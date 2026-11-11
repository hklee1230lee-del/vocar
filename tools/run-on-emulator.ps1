# Builds the app, installs it on the Pleos Connect emulator and launches it.
#   run-on-emulator.bat            build + unit tests + install + launch
#   tools\smoke-test.bat           same, then walks through the screens listed in tools\smoke-steps.txt
# Output: build\run\ (run.log, gradle.log, status.txt, screenshots)
param(
    [switch]$SkipBuild,
    [switch]$Smoke,
    [string]$Steps = "tools\smoke-steps.txt",
    [string]$Avd = ""
)

$ErrorActionPreference = "Continue"
$package = "com.arrivaltalk.app"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$out = Join-Path $root "build\run"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$log = Join-Path $out "run.log"
$statusFile = Join-Path $out "status.txt"
Set-Content -Path $log -Value "" -Encoding UTF8
Set-Content -Path $statusFile -Value "RUNNING" -Encoding ASCII

function Say($message) {
    $line = "[{0}] {1}" -f (Get-Date -Format "HH:mm:ss"), $message
    Write-Host $line
    Add-Content -Path $log -Value $line -Encoding UTF8
}

function Fail($message) {
    Say "FAILED: $message"
    Set-Content -Path $statusFile -Value "FAILED: $message" -Encoding UTF8
    exit 1
}

# Reads a key from local.properties (Java properties escapes such as \: \\ \uXXXX are decoded).
function Prop($name) {
    $file = Join-Path $root "local.properties"
    if (-not (Test-Path $file)) { return $null }
    foreach ($line in Get-Content $file) {
        if ($line -match ("^\s*" + [regex]::Escape($name) + "\s*=(.*)$")) {
            $raw = $Matches[1].Trim()
            try { return [regex]::Unescape($raw) } catch { return $raw }
        }
    }
    return $null
}

# --- JDK (Gradle needs one to start; Android Studio ships one in its jbr folder) ---
$jdk = $null
$candidates = @(
    (Prop "java.home"),
    $env:JAVA_HOME,
    (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
    (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr")
)
foreach ($candidate in $candidates) {
    if ($candidate -and (Test-Path (Join-Path $candidate "bin\java.exe"))) { $jdk = $candidate; break }
}
if (-not $jdk) { Fail "JDK not found. Add a line java.home=<Android Studio folder>\\jbr to local.properties." }
$env:JAVA_HOME = $jdk
Say "JDK: $jdk"

# --- Android SDK ---
$sdk = Prop "sdk.dir"
if (-not $sdk) { $sdk = $env:ANDROID_HOME }
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (-not (Test-Path $adb)) { Fail "adb not found under $sdk. Check sdk.dir in local.properties." }
Say "SDK: $sdk"

function Adb {
    & $adb @args 2>&1 | ForEach-Object { "$_" }
}

# --- Build ---
if (-not $SkipBuild) {
    Say "Building: unit tests + debug APK (first run can take several minutes)..."
    $gradleOut = Join-Path $out "gradle.log"
    $gradleErr = Join-Path $out "gradle-errors.log"
    $process = Start-Process -FilePath (Join-Path $root "gradlew.bat") `
        -ArgumentList "--console=plain", "testDebugUnitTest", "assembleDebug" `
        -WorkingDirectory $root -NoNewWindow -PassThru `
        -RedirectStandardOutput $gradleOut -RedirectStandardError $gradleErr
    $null = $process.Handle
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { Fail "Gradle build failed. See build\run\gradle.log and gradle-errors.log" }
    Say "Build OK"
}
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { Fail "APK not found: $apk" }

# --- Emulator ---
function Booted {
    $state = (Adb get-state) -join " "
    if ($state -notmatch "device") { return $false }
    $boot = (Adb shell getprop sys.boot_completed) -join ""
    return $boot.Trim() -eq "1"
}

if (-not (Booted)) {
    $emulator = Join-Path $sdk "emulator\emulator.exe"
    if (-not (Test-Path $emulator)) { Fail "No running emulator and emulator.exe not found under $sdk" }
    if (-not $Avd) {
        $Avd = & $emulator -list-avds 2>$null | Where-Object { $_ -match "Pleos" } | Select-Object -First 1
    }
    if (-not $Avd) { Fail "No Pleos AVD found. Start the Pleos Connect emulator from Android Studio and run again." }
    Say "Starting emulator: $Avd"
    Start-Process -FilePath $emulator -ArgumentList "-avd", $Avd | Out-Null
    $deadline = (Get-Date).AddMinutes(6)
    while (-not (Booted)) {
        if ((Get-Date) -gt $deadline) { Fail "Emulator did not finish booting within 6 minutes." }
        Start-Sleep -Seconds 4
    }
    Start-Sleep -Seconds 15
}
Say "Emulator is ready"

# --- Install + launch ---
$install = (Adb install -r $apk) -join " "
if ($install -notmatch "Success") { Fail "Install failed: $install" }
Say "Installed $package"
$user = ((Adb shell am get-current-user) | Select-Object -Last 1).Trim()

function Launch {
    Adb shell am start -n "$package/.MainActivity" | Out-Null
}

function Shot($name) {
    $remote = "/data/local/tmp/arrival-talk-shot.png"
    Adb shell screencap -p $remote | Out-Null
    Adb pull $remote (Join-Path $out "$name.png") | Out-Null
}

Adb logcat -c | Out-Null
Launch
Start-Sleep -Seconds 5
Shot "launch"
$appPid = ((Adb shell pidof $package) -join "").Trim()
if (-not $appPid) {
    Adb logcat -d -t 400 "AndroidRuntime:E" "ArrivalTalk:V" "*:S" | Set-Content -Path (Join-Path $out "logcat.txt") -Encoding UTF8
    Fail "The app is not running after launch. See build\run\logcat.txt"
}
Say "App is running (pid $appPid, user $user). Screenshot: build\run\launch.png"

# --- Optional walk-through ---
$failures = 0
if ($Smoke) {
    $stepsFile = Join-Path $root $Steps
    if (-not (Test-Path $stepsFile)) { Fail "Steps file not found: $stepsFile" }

    function Dump($allWindows) {
        $remote = "/data/local/tmp/arrival-talk-ui.xml"
        if ($allWindows) { Adb shell uiautomator dump --windows $remote | Out-Null }
        else { Adb shell uiautomator dump $remote | Out-Null }
        $local = Join-Path $out "ui.xml"
        Remove-Item $local -ErrorAction SilentlyContinue
        Adb pull $remote $local | Out-Null
        if (-not (Test-Path $local)) { return $null }
        try { return [xml](Get-Content -Path $local -Raw -Encoding UTF8) } catch { return $null }
    }

    # Exact text match first (so "next" does not hit a sentence that merely contains it), then substring.
    function FindNode($text) {
        foreach ($allWindows in @($false, $true)) {
            $xml = Dump $allWindows
            if (-not $xml) { continue }
            $nodes = $xml.SelectNodes("//node")
            foreach ($exact in @($true, $false)) {
                foreach ($node in $nodes) {
                    foreach ($value in @($node.GetAttribute("text"), $node.GetAttribute("content-desc"))) {
                        if (-not $value) { continue }
                        if ($exact -and $value -eq $text) { return $node }
                        if (-not $exact -and $value.Contains($text)) { return $node }
                    }
                }
            }
        }
        return $null
    }

    function WaitNode($text) {
        for ($i = 0; $i -lt 5; $i++) {
            $node = FindNode $text
            if ($node) { return $node }
            Start-Sleep -Milliseconds 600
        }
        return $null
    }

    $index = 0
    foreach ($raw in Get-Content -Path $stepsFile -Encoding UTF8) {
        $line = $raw.Trim()
        if (-not $line -or $line.StartsWith("#")) { continue }
        $parts = $line.Split(" ", 2)
        $command = $parts[0]
        $arg = if ($parts.Count -gt 1) { $parts[1].Trim() } else { "" }
        switch ($command) {
            "clear" { Adb shell pm clear --user $user $package | Out-Null; Say "clear data" }
            "stop" { Adb shell am force-stop --user $user $package | Out-Null; Say "force-stop" }
            "launch" { Launch; Start-Sleep -Seconds 4; Say "launch" }
            "home" { Adb shell input keyevent KEYCODE_HOME | Out-Null; Start-Sleep -Seconds 2; Say "home key" }
            "back" { Adb shell input keyevent KEYCODE_BACK | Out-Null; Start-Sleep -Milliseconds 800; Say "back key" }
            "wait" { Start-Sleep -Milliseconds ([int]$arg) }
            "night" { Adb shell cmd uimode night $arg | Out-Null; Start-Sleep -Seconds 2; Say "night mode $arg" }
            "shot" {
                $index++
                $name = "{0:D2}-{1}" -f $index, $arg
                Shot $name
                Say "shot $name"
            }
            "tap" {
                $node = WaitNode $arg
                if ($node -and $node.GetAttribute("bounds") -match "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
                    $x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
                    $y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
                    Adb shell input tap $x $y | Out-Null
                    Start-Sleep -Milliseconds 900
                    Say "tap '$arg' at $x,$y"
                } else {
                    $failures++
                    Say "MISSING tap target '$arg'"
                }
            }
            "expect" {
                if (WaitNode $arg) { Say "PASS expect '$arg'" }
                else { $failures++; Say "MISSING expect '$arg'" }
            }
            "absent" {
                if (FindNode $arg) { $failures++; Say "UNEXPECTED '$arg' is on screen" }
                else { Say "PASS absent '$arg'" }
            }
            default { Say "unknown step: $line" }
        }
    }
}

Adb logcat -d -t 600 "AndroidRuntime:E" "ArrivalTalk:V" "*:S" | Set-Content -Path (Join-Path $out "logcat.txt") -Encoding UTF8
$crashed = Select-String -Path (Join-Path $out "logcat.txt") -SimpleMatch -Pattern "Process: $package" -Quiet
if ($crashed) { Fail "The app crashed. See build\run\logcat.txt" }
if ($failures -gt 0) { Fail "$failures walk-through step(s) did not match. See build\run\run.log" }
Say "DONE"
Set-Content -Path $statusFile -Value "OK" -Encoding ASCII
exit 0
