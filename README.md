# VOCAR (보카)

> 주차 후 1~3분 동안 외국어 표현을 연습하고 경제 퀴즈를 풀 수 있는 Pleos Connect 차량용 학습 앱입니다.

현대자동차그룹 차량용 앱 개발 공모전 출품작(팀 어울수레). Kotlin / Jetpack Compose로 구현했습니다.

| 항목 | 내용 |
| --- | --- |
| 실행 방법 | Pleos 에뮬레이터를 켜고 저장소 루트에서 `adb install -r ".\apk\어울수레_VOCAR_release.apk"`를 실행한 뒤 런처에서 VOCAR를 엽니다. 소스에서 빌드하고 실행하려면 `.\run-on-emulator.bat`를 사용합니다. [빌드와 실행](#빌드와-실행) |
| 테스트 환경 및 설정 | APK 설치에는 Android SDK Platform-Tools(`adb`)가 필요합니다. Virtual Console에서 기어를 P로 두고 주행 제한이 해제된 상태에서 테스트합니다. 소스 빌드에는 Android SDK Platform 37과 Gradle 데몬용 JDK 25를 사용합니다. |
| 개발에 사용한 에뮬레이터 | Pleos Connect v2.1.0 (Android API 34), 2560 × 1440 px |
| APK | [제출용 release APK](apk/어울수레_VOCAR_release.apk) · [새 아이콘 확인용 debug APK 릴리스](https://github.com/hklee1230lee-del/VOCAr/releases/tag/v1.0.1-icon) |
| 연동 SDK | Vehicle 2.3.2, NaviHelper 3.0.5, Pleos TTS·STT 3.3.1 |
| 패키지 | `com.vocar.app` |
| 안전 | 기어 P + 주행 제한 없음일 때만 학습·음원 재생. 권한은 읽기용 `CAR_DRIVE`·`NAVI_ROUTE`와 `TTS_SERVICE`·`STT_SERVICE` |
| 오프라인 | 영어 20개 + 일본어·중국어·스페인어 각 15개 학습 묶음과 경제 상식 퀴즈가 앱에 포함. 앱 자체는 네트워크 권한을 선언하지 않음(Pleos SDK 라이브러리가 INTERNET을 포함하고, TTS·STT는 HYBRID 모드) |

## 무엇이 들어 있나

| 기획안 항목 | 구현 |
| --- | --- |
| FR-01 주차 전용 | 기어 P + 주행 제한 없음 + 2초 이내 확인값일 때만 시작·이어하기·음원 재생. DO 미선언 |
| FR-02 출근·퇴근 | 사용자가 직접 선택(위치 권한 없음). 한 모드만 써도 완결 |
| FR-03 1분·3분 | 활동 2개 / 4개. 카운트다운·시간 초과 오답 없음 |
| FR-04 듣기·선택·회상 | 20개 묶음 × (듣고 뜻 고르기, 상황 고르기, 떠올리기, 회상 확인, 변형 2종) |
| FR-05 말하기 연습 | "마이크로 말하기"를 누를 때만 Pleos STT로 한 번 듣고, 정답 문장과 같으면 연습으로 기록. 마이크 없이 "말해 봤어요"로도 기록 가능 |
| FR-06 다국어 | 설정에서 학습 언어 선택: 영어, 일본어, 중국어(간체), 스페인어. TTS·STT 언어도 함께 바뀜 |
| 경제 상식 탭 | 하루 3문제 퀴즈 + 심화 퀴즈 팩, 경제 사전(개념·쉬운 설명·그림), 레벨·배지 도감. 문제 풀기는 주차 중에만. 연속 기록과 연속 보호권은 이 탭에만 있으며, 보호권 "구매"는 결제 연동 없는 시연용 |
| FR-07 복습 추천 | 다음 학습일 → 1·3·7·14일 간격, 세션당 최대 2개, 힌트·오답·자가 확인 구분 |
| FR-08 오프라인 | 20개 묶음 전체가 앱에 포함. 학습 콘텐츠는 네트워크 없이 동작 |
| FR-09 저장·복구 | 활동을 확정할 때마다 저장. 강제 종료 후에도 중복 없이 이어하기 |
| FR-10 주간 목표·기록 | 학습일 / 연습한 표현 / 회상 확인 / 다시 익힐 표현을 따로 표시 |
| FR-11 창·주야간 | 창 크기에 맞춰 전체 비율 조정, 넓은 창 2단·좁은 창 1단, 주간·야간 색 |
| FR-12 기록 관리 | 이 차량에만 저장, 설정에서 모두 지우기 |
| 도착 감지 | 주행이 끝난 시점을 기록하고 출근/퇴근을 추정해 오늘 학습을 바꿔 제안 (아래 참고) |

아직 넣지 않은 것: STT 기반 발음 점수, 실제 결제 연동, 검수 음원 파일(지금은 Pleos TTS, 초기화가 안 되면 기기 TTS), 분석 이벤트 전송(FR-13), 계정·동기화, Gleo 연동.

## 출근·퇴근이 끝난 시점을 아는 방법

Vehicle SDK 2.3.2의 읽기 API(권한 `pleos.car.permission.CAR_DRIVE`)와, 내비게이션 도착 이벤트(NaviHelper 3.0.5, 권한 `pleos.car.permission.NAVI_ROUTE`)를 **병행**합니다. 위치(GPS)는 쓰지 않습니다.

| 알아내는 것 | 쓰는 API | 비고 |
| --- | --- | --- |
| 주행이 끝난 시각 | `Gear.registerGearSelection` — P가 아닌 기어 뒤에 P가 되는 순간 | 콜백 방식. 앱이 화면에 없어도 프로세스가 살아 있으면 받음 |
| 놓친 주행 보완 | `Odometer.getCurrentDistanceDriven` (누적 km) | 앱이 주차 상태로 열릴 때 읽어, 지난 기록보다 0.3km 이상 늘었으면 도착으로 기록(시각은 앱을 연 시각) |
| 내비 목적지 도착 | `NaviHelper.onDestinationArrived` | 내비 안내를 켠 주행에서만 옴. 이벤트의 주행 거리·시간을 쓴다. 집/회사 판별은 (1) 사용자가 도착을 출근/퇴근으로 확인하면 그 목적지의 키(도착 이벤트의 이름+주소를 해시한 값)를 기억해 다음부터 바로 확정하고, (2) 목적지 이름이 `집`·`회사` 등과 정확히 같으면 그 값을 쓴다. 에뮬레이터 확인: 내비에 집으로 등록한 장소도 SDK는 `집`이 아니라 장소 이름(예: `더현대서울`)으로 주므로 (2)는 거의 동작하지 않고 (1)이 주된 방법이다 |
| 이번 주행 거리·시간 | `Odometer.getTripCurrentDrivingDistance` (m), `getTripCurrentDrivingTime` (분) | 없으면 누적 거리 차이로 대신 |

출근인지 퇴근인지는 `domain/Arrivals.kt`의 규칙으로 가립니다.

1. 평소 출퇴근 거리와 비슷한가 (확정된 도착들의 주행 거리 중앙값 ±25%)
2. 순서 — 오늘 회사 도착 뒤의 도착은 집, 집 다음 도착은 회사
3. 시간 — 배운 도착 시각 ±2시간, 아직 없으면 11시 이전은 출근·16시 이후는 퇴근

도착 뒤 45분 안에 사용자가 출근/퇴근 학습을 시작하면(또는 안내에서 바로잡으면) 그 값이 정답으로 저장되어 다음 추정에 쓰입니다.
거리와 순서·시간이 모두 맞으면 "도착했어요", 아니면 "도착한 것 같아요(추정)"로 표시합니다. 좌표나 주소는 저장하지 않습니다.

내비 도착은 기어가 P가 될 때까지 기다렸다가 확정합니다(주행 중 알림만으로 기록하지 않음. 이미 P이면 바로, 20분 안에 P가 안 되면 버림). 내비 도착과 기어 도착은 같은 주행의 두 신호라서, 20분 안에 서로 다른 신호가 오면 도착 한 건으로 합칩니다. 합칠 때는 내비가 준 주행 거리·시간을 우선합니다. 권한을 거부하거나 내비를 쓰지 않으면 기어·주행거리 방식만으로 동작합니다. 저장하는 것은 사용자가 집/회사로 확인한 목적지 키 최대 20개와 그 값뿐이며, 이름·주소·좌표는 저장하지 않습니다(키에서 되돌릴 수 없음). 출퇴근이 아니라고 답하면 그 키를 잊습니다. 목적지는 도착 이벤트 하나에서 정합니다. 앱이 백그라운드에서 OS에 의해 얼려 있는 동안(`isFrozen=true`, 에뮬레이터 확인)에는 목적지 푸시나 주기 조회가 멈추므로, 따로 추적한 목적지 상태에 기대지 않기 위해서입니다.

한계: 프로세스가 꺼져 있던 사이의 주행은 정확한 도착 시각을 알 수 없습니다(앱을 연 시각으로 기록). 내비 이벤트도 앱 프로세스가 살아 있을 때만 받습니다. 이벤트의 거리 단위(m)와 시간 단위(초)는 SDK 문서로 확인하지 못해 가정한 값이며, 말이 안 되는 값은 버립니다.
FusedLocation(위치)은 쓰지 않습니다.
기어 조회 API는 SDK에서 deprecated(구글 `GEAR_SELECTION` 속성 권장)로 표시되어 있습니다.

## 빌드와 실행

Android Studio에서 이 폴더를 열고 Pleos Connect 에뮬레이터를 고른 뒤 ▶ 를 누르면 됩니다.
명령줄에서는 아래 스크립트가 빌드 → 단위 테스트 → 에뮬레이터 설치 → 실행까지 합니다.

```powershell
.\run-on-emulator.bat          # 빌드 + 설치 + 실행
.\tools\smoke-test.bat         # 위 과정 뒤에 tools\smoke-steps.txt 순서대로 화면을 눌러 보고 스크린샷 저장
```

결과는 `build\run\`에 남습니다(`run.log`, `gradle.log`, `status.txt`, 스크린샷).
에뮬레이터가 꺼져 있으면 이름에 `Pleos`가 들어간 AVD를 찾아 켭니다.

`local.properties`(git에 올리지 않음)에 PC별 경로를 둡니다.

```properties
sdk.dir=C\:\\Users\\<이름>\\AppData\\Local\\Android\\Sdk
# JAVA_HOME이 없을 때만: Android Studio 폴더의 jbr (한글 경로는 \uXXXX로 적는다)
java.home=C\:\\Program Files\\Android\\Android Studio\\jbr
```

에뮬레이터에 차량(DDS) 연결이 없어 콘솔이 `emulator_inactive`를 돌려줄 때는, debug 빌드에서 기어 변화를 흉내 낼 수 있습니다. 실제 기어 콜백과 같은 경로(`TripMonitor.onGear`)로 들어가 도착 기록과 카드가 그대로 동작합니다. release 빌드에는 등록되지 않습니다.

```powershell
adb shell am broadcast -a com.vocar.app.DEBUG_GEAR --es gear D
adb shell am broadcast -a com.vocar.app.DEBUG_GEAR --es gear P --ef trip_km 12.4 --ei minutes 25 --ef odo_km 15000
```

내비 도착도 흉내 낼 수 있습니다. 주행 중(`D`)에 보내면 `P`가 될 때까지 기다리고, `dest`+`addr`가 이미 확인한 목적지면 바로 확정 안내합니다. 공백이 든 값은 작은따옴표로 한 인자로 묶어 `adb shell "am broadcast ..."`처럼 문자열 하나로 보냅니다.

```powershell
adb shell am broadcast -a com.vocar.app.DEBUG_NAVI --es dest 더현대서울 --es addr '서울특별시 영등포구 여의대로 108' --ef km 12.4 --ei minutes 25
```

`P`에 `trip_km`를 주면 SDK 값(에뮬레이터에서는 비어 있음) 대신 그 값으로 도착을 기록합니다. 차량 SDK가 값을 주는 경로는 이 방법으로 검증되지 않습니다.

기어를 바꿔 주행 제한을 확인하려면 `adb forward tcp:8088 tcp:8088` 뒤 http://localhost:8088/ (Virtual Console)에서 P ↔ D를 바꿉니다.

## 코드 구조

```
app/src/main/java/com/vocar/app/
  MainActivity.kt          화면 표시/이탈에 맞춰 게이트·음원을 켜고 끈다
  domain/Models.kt         묶음, 세션, 시도, 숙련 상태
  domain/Content.kt        4주 · 20개 영어 학습 묶음 (CONTENT_VERSION)
  domain/SentenceContent.kt 일본어·중국어·스페인어 문장 학습 묶음, 학습 언어 목록
  domain/EconQuiz.kt, Dictionary.kt  경제 상식 퀴즈·배지·사전 (data/EconStore.kt에 저장)
  audio/Mic.kt             말하기 단계의 Pleos STT(버튼을 누를 때만 인식)
  ui/EconScreens.kt        경제 상식 탭
  words/LanguageActivity.kt 학습 언어 선택
  domain/Engine.kt         추천, 세션 구성, 채점 반영, 복습 간격, 난이도, 주간 기록 — 순수 함수
  data/Codec.kt, Store.kt  JSON 저장 형식, 임시 파일 → 이름 바꾸기 저장
  vehicle/ParkingGate.kt   CarUxRestrictions + 기어 P 확인 (주차 전용 게이트)
  vehicle/TripMonitor.kt   기어·주행거리로 주행이 끝난 시점 감지
  vehicle/NaviArrivals.kt  내비 목적지 도착 이벤트로 도착 감지(NaviHelper, 기어 도착과 병행)
  domain/Arrivals.kt       도착 기록, 출근/퇴근 추정 규칙
  audio/Speaker.kt         TTS 재생(Pleos TTS → 기기 TTS), 오디오 포커스 요청·상실 처리
  ui/AppController.kt      화면 상태, 저장 시점, 일시정지·재개
  ui/Kit.kt                색·글자·버튼 등 공통 요소
  ui/HomeScreens.kt        첫 안내, 오늘, 표현 보관함, 내 기록, 설정
  ui/LessonScreens.kt      문제, 말하기, 피드백, 일시정지, 완료
app/src/test/              EngineTest(학습 규칙), ArrivalsTest(도착 추정), CodecTest(저장·복구), EconQuizTest·DictionaryTest(경제 상식)
```

학습 규칙을 바꿀 때는 `Engine.kt`와 `EngineTest.kt`만 보면 됩니다. 문항을 고치면 `Content.CONTENT_VERSION`을 올립니다.

## 동작 원칙 (기획안에서 그대로 가져온 것)

- 하루 한 번의 유효 학습이면 그날 목표 달성. 어학 학습에는 연속 기록·순위·생명·광고 없음(연속 기록은 경제 상식 탭에만).
- 끝내기는 어느 단계에서나 한 번. 확인창 없음. 완료 후 다음 학습을 자동으로 시작하지 않음.
- 화면을 벗어나거나, 주차가 풀리거나, 다른 소리에 밀리면 멈추고, 돌아와도 사용자가 눌러야 이어감.
- 자가 확인(떠올렸어요)은 연습으로만 기록하고 "회상 확인"으로 세지 않음.
- 권한은 읽기용 `pleos.car.permission.CAR_DRIVE`·`NAVI_ROUTE`와 `TTS_SERVICE`·`STT_SERVICE`. 앱 자체는 위치·네트워크 권한을 선언하지 않음(SDK 라이브러리가 INTERNET 포함). 마이크는 STT를 쓰는 말하기 단계에서만. 내비 목적지는 집/회사로 확인된 해시 키만 저장.
