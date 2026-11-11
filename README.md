# 도착한마디 (Arrival Talk)

> 주차 후, 내리기 전 1~3분. 오늘 쓸 영어 표현 하나를 듣고, 말해 보고, 다시 떠올리는 Pleos Connect 차량용 학습 앱.

기획안 `pleos_language_learning_plan.md` v1.0의 MVP 범위를 Kotlin / Jetpack Compose로 구현했습니다.
Pleos Connect v2.1.0 에뮬레이터(API 34) + Vehicle SDK 2.3.2 기준입니다.

## 무엇이 들어 있나

| 기획안 항목 | 구현 |
| --- | --- |
| FR-01 주차 전용 | 기어 P + 주행 제한 없음 + 2초 이내 확인값일 때만 시작·이어하기·음원 재생. DO 미선언 |
| FR-02 출근·퇴근 | 사용자가 직접 선택(위치 권한 없음). 한 모드만 써도 완결 |
| FR-03 1분·3분 | 활동 2개 / 4개. 카운트다운·시간 초과 오답 없음 |
| FR-04 듣기·선택·회상 | 20개 묶음 × (듣고 뜻 고르기, 상황 고르기, 떠올리기, 회상 확인, 변형 2종) |
| FR-05 말하기 연습 | 마이크 없이 따라 말하고 "말해 봤어요"로 기록(연습 참여) |
| FR-07 복습 추천 | 다음 학습일 → 1·3·7·14일 간격, 세션당 최대 2개, 힌트·오답·자가 확인 구분 |
| FR-08 오프라인 | 20개 묶음 전체가 앱에 포함. 네트워크를 쓰지 않음 |
| FR-09 저장·복구 | 활동을 확정할 때마다 저장. 강제 종료 후에도 중복 없이 이어하기 |
| FR-10 주간 목표·기록 | 학습일 / 연습한 표현 / 회상 확인 / 다시 익힐 표현을 따로 표시 |
| FR-11 창·주야간 | 창 크기에 맞춰 전체 비율 조정, 넓은 창 2단·좁은 창 1단, 주간·야간 색 |
| FR-12 기록 관리 | 이 차량에만 저장, 설정에서 모두 지우기 |

아직 넣지 않은 것: 음성 인식(STT) 평가(FR-06), 검수 음원 파일(지금은 기기 TTS), 분석 이벤트 전송(FR-13), 계정·동기화, Gleo 연동.

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

기어를 바꿔 주행 제한을 확인하려면 `adb forward tcp:8088 tcp:8088` 뒤 http://localhost:8088/ (Virtual Console)에서 P ↔ D를 바꿉니다.

## 코드 구조

```
app/src/main/java/com/arrivaltalk/app/
  MainActivity.kt          화면 표시/이탈에 맞춰 게이트·음원을 켜고 끈다
  domain/Models.kt         묶음, 세션, 시도, 숙련 상태
  domain/Content.kt        4주 · 20개 학습 묶음 (CONTENT_VERSION)
  domain/Engine.kt         추천, 세션 구성, 채점 반영, 복습 간격, 난이도, 주간 기록 — 순수 함수
  data/Codec.kt, Store.kt  JSON 저장 형식, 임시 파일 → 이름 바꾸기 저장
  vehicle/ParkingGate.kt   CarUxRestrictions + 기어 P 확인 (주차 전용 게이트)
  audio/Speaker.kt         TTS 재생, 오디오 포커스 요청·상실 처리
  ui/AppController.kt      화면 상태, 저장 시점, 일시정지·재개
  ui/Kit.kt                색·글자·버튼 등 공통 요소
  ui/HomeScreens.kt        첫 안내, 오늘, 표현 보관함, 내 기록, 설정
  ui/LessonScreens.kt      문제, 말하기, 피드백, 일시정지, 완료
app/src/test/              EngineTest(학습 규칙), CodecTest(저장·복구)
```

학습 규칙을 바꿀 때는 `Engine.kt`와 `EngineTest.kt`만 보면 됩니다. 문항을 고치면 `Content.CONTENT_VERSION`을 올립니다.

## 동작 원칙 (기획안에서 그대로 가져온 것)

- 하루 한 번의 유효 학습이면 그날 목표 달성. 연속 기록·순위·생명·광고 없음.
- 끝내기는 어느 단계에서나 한 번. 확인창 없음. 완료 후 다음 학습을 자동으로 시작하지 않음.
- 화면을 벗어나거나, 주차가 풀리거나, 다른 소리에 밀리면 멈추고, 돌아와도 사용자가 눌러야 이어감.
- 자가 확인(떠올렸어요)은 연습으로만 기록하고 "회상 확인"으로 세지 않음.
- 권한은 읽기용 `pleos.car.permission.CAR_DRIVE` 하나. 마이크·위치·네트워크 권한 없음.
