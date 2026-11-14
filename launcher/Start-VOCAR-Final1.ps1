$ErrorActionPreference = 'Stop'
$adb = 'C:\Android\Sdk\platform-tools\adb.exe'
$avd = 'Pleos_Connect_v2.1.0_ASCII'
$pkg = 'com.vocar.app'
$apk = 'C:\Projects\VOCAR\integration\econ-multilingual\app\build\outputs\apk\debug\app-debug.apk'
$hash = 'DB9087573A49F94D2C5604AC03E0DF514E87FDED1C0587E12DA34999E68AF72A'
$mutex = New-Object Threading.Mutex($false, 'Local\VOCARFinal1Launcher')
$locked = $false
function Invoke-AdbChecked([string[]]$Arguments) {
    # Read stderr as text, not PowerShell ErrorRecord (Windows PowerShell 5.1).
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $adb
    $info.Arguments = ($Arguments | ForEach-Object { '"' + $_ + '"' }) -join ' '
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    try {
        [void]$process.Start()
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $stdout = $stdoutTask.Result
        $stderr = $stderrTask.Result
        $code = $process.ExitCode
        Write-Host "ADB exit code: $code"
        if ($stdout) { Write-Host "stdout: $stdout" }
        if ($stderr) { Write-Host "stderr: $stderr" }
        if ($code -ne 0) { throw "ADB failed (exit $code). stdout: $stdout stderr: $stderr" }
        return $stdout
    } finally { $process.Dispose() }
}
try {
    $locked = $mutex.WaitOne(0)
    if (!$locked) { throw 'Another launcher is already working.' }
    function FindDevice {
        $found = @()
        foreach ($line in (& $adb devices)) {
            if ($line -match '^(emulator-\d+)\s+device$') {
                $id = $Matches[1]
                if ((& $adb -s $id emu avd name 2>$null) -contains $avd) { $found += $id }
            }
        }
        if ($found.Count -gt 1) { throw 'Multiple Pleos devices detected.' }
        if ($found.Count -eq 1) { $found[0] }
    }
    $device = FindDevice
    if (!$device -and !(Get-Process emulator,qemu-system-x86_64,qemu-system-x86_64-headless -ErrorAction SilentlyContinue)) {
        $env:ANDROID_AVD_HOME = 'C:\Projects\VOCAR\.android\avd'
        $env:TEMP = 'C:\Projects\VOCAR\.android\emulator-temp'
        $env:TMP = $env:TEMP
        if (!(Test-Path "$env:ANDROID_AVD_HOME\$avd.ini")) { throw 'Existing AVD not found.' }
        Start-Process 'C:\Android\Sdk\emulator\emulator.exe' -ArgumentList "-avd $avd -no-snapshot -gpu host" -WindowStyle Hidden
    }
    Write-Host 'Waiting for Pleos (maximum 5 minutes); no duplicate will be started...'
    $deadline = (Get-Date).AddMinutes(5)
    $boot = ''
    while ((Get-Date) -lt $deadline) {
        if (!$device) { $device = FindDevice }
        if ($device) { $boot = ((& $adb -s $device shell getprop sys.boot_completed) -join '').Trim() }
        if ($boot -eq '1') { break }
        Start-Sleep 2
    }
    if ($boot -ne '1') { throw 'Pleos not ready. No reset performed.' }
    $userId = ((& $adb -s $device shell am get-current-user) -join '').Trim()
    if ($userId -notmatch '^\d+$') { throw 'Cannot determine Android user.' }
    $packages = Invoke-AdbChecked @('-s', $device, 'shell', 'pm', 'list', 'packages', '--user', $userId, $pkg)
    $installed = ($packages -split '\r?\n').Trim() -contains "package:$pkg"
    $installedHash = ''
    if ($installed) {
        $paths = Invoke-AdbChecked @('-s', $device, 'shell', 'pm', 'path', '--user', $userId, $pkg)
        $base = ($paths -split '\r?\n') | Where-Object { $_ -match '^package:.*/base\.apk\s*$' } | Select-Object -First 1
        if (!$base) { throw 'Installed APK path unavailable; update skipped.' }
        $remote = ($base -replace '^package:', '').Trim()
        $sum = Invoke-AdbChecked @('-s', $device, 'shell', 'sha256sum', $remote)
        if ($sum -notmatch '^([a-fA-F0-9]{64})\s') { throw 'Installed APK hash unavailable; update skipped.' }
        $installedHash = $Matches[1]
    }
    if (!$installed -or $installedHash -ne $hash) {
        if (!(Test-Path $apk)) { throw 'Build-verified APK missing; install skipped.' }
        if ((Get-FileHash $apk).Hash -ne $hash) { throw 'APK changed since verification; install skipped.' }
        Write-Host 'Installing latest build-verified APK once, preserving existing data...'
        $output = Invoke-AdbChecked @('-s', $device, 'install', '--no-streaming', '-r', '--user', $userId, $apk)
        if ($output -notmatch '(?m)^Success\s*$') { throw 'Installation did not report Success; no app was removed.' }
        $packages = Invoke-AdbChecked @('-s', $device, 'shell', 'pm', 'list', 'packages', '--user', $userId, $pkg)
        if (!(($packages -split '\r?\n').Trim() -contains "package:$pkg")) { throw 'Package not available after installation; launch skipped.' }
    } else { Write-Host 'Latest verified APK already installed; installation/update skipped.' }
    $output = Invoke-AdbChecked @('-s', $device, 'shell', 'am', 'start', '--user', $userId, '-n', "$pkg/com.vocar.app.MainActivity")
    if ($output -match 'Error:|Exception') { throw "Launch failed: $output" }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host 'Press Enter to close'
    exit 1
} finally {
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
