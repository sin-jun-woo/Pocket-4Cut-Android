# Pocket 4Cut 1.7(8) 출시 후보 검증 — 2026-09-27

검사일은 Asia/Seoul 기준이다. 기준 커밋은 `25d825fd4ca540f5c2daebf95eb996eb516b852e`, 작업 브랜치는 `codex/release-qa-20260927`이다. 사용자 후속 요청에 따라 `app/build.gradle.kts`의 버전을 **1.7(8)**로 변경했다. 이 보고서는 버전 변경 전 **1.6(7)의 실행 기록**과 **1.7(8)의 최종 재검사**를 구분한다. 1.6(7)과 9월 22~24일의 1.5(6) 성공 결과를 새 버전의 통과로 대체하지 않는다.

## 1. 판정과 읽는 방법

**1.7(8)의 전체 빌드·실기기 계측 174개·R8 설치 및 주요 진입 화면 검사·AAB 검증을 완료했다. 실행한 검사에서는 남은 실패가 없다.** 사용자 승인으로 versionCode를 7에서 8로 올렸다. 다만 **Play Console에서 코드 8이 미사용인지 확인하거나 새 AAB를 업로드하지 않았다.** 연결된 기기 한 대에서 확인하지 못한 OS·실제 TalkBack 전체 완주 등의 범위는 10절에 남긴다. 모든 입력·기기의 무결함 또는 Play 출시 완료 판정은 아니다.

버전 변경 전 1.6(7)은 명시한 범위에서 계측 174개·JVM 25개와 R8 카메라·앨범 흐름이 통과했고 서명된 AAB를 생성했다. 당시에는 연결 기기의 Play 앱도 1.6(7)이어서 동일 코드 후보의 새 업데이트 업로드를 BLOCKED로 기록했다. 이 과거 결과와 산출물 값은 3~8절에 보존하고, **1.7(8)의 새 결과는 9절에만 기록한다.** 지원 OS 전체·TalkBack 음성 완주 등의 미검증 범위는 버전 상승만으로 해소되지 않는다.

- **PASS**: 명시한 환경·입력·검사에서 실제 실행한 assertion 또는 수동 동작이 성공했다.
- **PARTIAL**: 일부 경로를 실행했지만 같은 범주의 모든 조건을 확인한 것은 아니다.
- **NOT TESTED**: 실행하지 않았다. 소스 검토나 테스트 정의만으로 통과시킬 수 없다.
- **EXCLUDED**: 사용자가 이번 범위에서 제외했다.
- **BLOCKED**: 필요한 기기·계정·환경 또는 외부 서비스 검증이 없어 해당 판정을 완료할 수 없다.

모든 입력 조합에 무결함이라고 선언하는 보고서가 아니다. 실제 화면 경로 74개 점검 항목과 테스트 메서드 목록은 [QA 범위 목록](RELEASE_QA_SCOPE_2026-09-27.md)에 별도로 보존한다. 그 문서는 정의 목록이며 이 문서의 실행 결과와 구분한다.

## 2. 환경과 보존 범위

아래 기기·상태 값은 1.6(7) 검사에서 확인한 기준 환경이다. 동일 범위의 1.7(8) 재검사 결과와 최종 상태 대조는 9절에 별도로 기록한다.

- 실제 기기: Samsung SM-S942N 한 대, Android 16/API 36, 256MiB 메모리 클래스. 기본 물리 화면 1080×2340, 설정 밀도 420dpi. 화면 크기를 강제로 바꾸지 않았다.
- 세로 화면을 기준으로 검사했다. 큰 글자는 실제 시스템 2.0배와 Compose의 360×640dp 세로 fixture를 사용했다. 최종 글자 크기를 원래 0.8배로 복구하고 값을 확인했다.
- 사용자의 요청 이후 가로 전용 계측 2개를 명시적으로 제외했다. 세로 화면 안의 `가로 2컷/가로 4컷/가로 그리드 6컷`은 사진 배치 이름이며 기기 가로 회전 검사가 아니다.
- 현재 앱 소스 버전: `versionName=1.7`, `versionCode=8`, minSdk 26, target/compileSdk 36. 사용자 후속 요청으로 버전을 올렸다. 아래 과거 실행 당시 버전은 1.6(7)이며 새 APK/AAB 내부 버전 대조는 9절에 기록했다.
- debug와 R8 QA 패키지는 `com.pocket4cut.qa`; 배포 앱 `com.pocket4cut`을 대상으로 설치·초기화 명령을 실행하지 않았다. 최종 패키지 조회에서는 배포 앱이 **1.6(7)**, 설치자·업데이트 소유자가 `com.android.vending`, 마지막 업데이트가 2026-09-27 14:51:54로 확인됐다. 이 Play 설치 상태를 관찰한 것이며 이번 후보 AAB의 생산 업데이트를 직접 검증한 것은 아니다. 과거 1.5(6) 기록을 현재 설치 상태로 사용하지 않는다.
- 촬영·앨범·삭제 장애 검사는 별도 QA 앱과 격리 fixture를 사용했다. 앨범 수동 검사는 직접 만든 1600×1200 번호/색상 JPEG 6장을 사용했다. 사용자 앨범 사진을 가져오거나 삭제하지 않았다.
- 기존 휴대전화 화면 켜짐 설정과 앱의 `화면 자동 꺼짐 방지` 항목은 조작하지 않았다. USB 전원 유지 값 15, 화면 타임아웃 15000이 최종에도 동일했다.
- 폰트 **라이선스 조사**와 **공개 개인정보 페이지 검증**은 EXCLUDED다. 앱 내부 문구·날짜의 실제 렌더 동작은 기능 검사에 포함한다.
- 원시 로그, 기기 식별자, 카메라 사진, 시스템 갤러리·공유 시트 화면은 로컬 ignored `build/release-qa-20260927/`에만 둔다. 공개 문서/Git에는 포함하지 않는다.

## 3. 이번에 확인하고 수정한 문제

수정 내용은 1.7(8) 작업 트리에도 포함되며, 이 절의 실제 실행 근거는 버전 변경 전 1.6(7)의 검사다. 1.7(8)의 재검사 결과는 9절과 구분한다.

### Q01. 불러오기 실패가 기존 사진 선택을 지울 수 있는 경로

촬영 사진 선택의 초기 읽기에 실패했을 때 빈 UI 선택을 저장하며 이탈할 수 있었고, 저장 실패는 사진 목록 자체를 오류 화면으로 치환했다. `hasLoaded`와 마지막 저장 선택을 구분했다. 초기 읽기 실패에서는 쓰기 없이 홈으로 나가고, 다시 불러오기를 제공한다. 저장 실패에서는 사진·최신 선택을 유지하고 저장을 재시도한다. 화면 재생성도 저장하지 못한 선택을 덮지 않는다.

근거: `SelectionRecoveryInstrumentedTest`의 원본 누락, 읽기 재시도, 저장 실패/복구, dirty 선택 유지 검사. 파일: `SelectionViewModel.kt`, `SelectionScreen.kt`.

### Q02. ‘다음’ 저장 중 선택 변경·연속 탭 경쟁

선택 A를 저장하고 레이아웃으로 이동하는 동안 늦은 선택 B 저장이 뒤따라 선택 순서/단계를 다시 덮을 수 있었다. 완료 요청 시 coroutine 대기 전에 입력을 잠그고, 사진 선택·중복 완료·이탈·불러오기를 차단한다. 실패하면 잠금을 해제하고 선택과 오류를 보존한다. 성공한 outgoing 화면은 잠금을 유지하고 재진입 시 저장 상태를 읽는다.

근거: 저장을 gate로 지연한 `completingSelectionBlocksLateToggleDuplicateCompletionAndReload`, `completionWriteFailureUnlocksSelectionAndAllowsEditedRetry`. revision 1회 증가, FRAME 단계, callback 1회, 실패 후 다른 선택으로 재시도까지 검사한다. 단순 빠른 탭 타이밍에 의존하지 않는다.

### Q03. 레이아웃 진입/저장 실패의 복구 UI 누락

문서 읽기·선택 원본 누락·저장 실패를 잡아 재시도 또는 보관함 이동을 제공한다. 선택 장수, 복구 필요 상태, 실제 원본 파일을 확인하며 저장 성공 전에 프레임 화면으로 이동하지 않는다. 저장 중 중복 실행도 막는다.

근거: `LayoutSelectionRouteInstrumentedTest` 3개. 손상 문서, 저장 실패, 원본 누락을 각각 주입하고 다른 사진/문서의 보존을 확인한다. 파일: `PocketNavHost.kt`의 `LayoutSelectionRoute`.

### Q04. 초과 가져오기 안내가 기존 복구 실패를 숨김

Photo Picker 초과 반환을 거절하는 조기 반환에서 기존 journal 재생 실패 목록을 잃었다. 초과 선택 오류와 기존 복구 오류를 함께 유지하도록 수정했다. 초과 장수를 임의로 잘라 가져오지 않는다.

근거: `overSelectionRetainsRecoveryFailureWithoutChangingImportedPhotos`. 문서·기존 import 해시가 변하지 않는지도 검사한다. 파일: `PhotoImportRepository.kt`.

### Q05. 앨범 확인 화면의 큰 글자/긴 안내에서 조작 접근성

큰 글자 또는 좁은 높이에서는 화면 전체가 스크롤되도록 하고, 일반 높이에서는 기존 하단 CTA와 사진 목록 스크롤을 유지한다. 최대 6장 목록은 key를 가진 Column으로 표시하여 무한 높이의 중첩 Lazy 스크롤을 피했다. 제거·드래그·접근성 순서 변경은 유지한다.

근거: 360×640dp 세로·2배 글자의 취소 후 재열기, 긴 오류 안내 아래 사진/추가/다음 접근 계측. 실기기 앨범 4컷에서도 2배 글자로 다음 단계에 실제 진입했다.

### Q06. 커스텀 프레임 tray를 열면 계속 버튼에 도달하지 못함

`verticalScroll`과 `heightIn(max=380.dp)` 순서 때문에 emoji/sticker 패널의 스크롤 제한이 잘못 적용됐다. 높이 제한을 scroll 바깥으로 옮겼다.

수정 전 실제 기기 Compose 검사 **4/4 실패**, 수정 후 같은 검사 **4/4 통과**. 360×640dp 세로의 기본/2배 글자에서 이모지와 스티커를 실제 추가하고 스크롤해 계속 버튼을 누른다. 수동 앨범 4컷에서도 별 스티커 이동·확대·회전을 적용한 결과를 저장했다.

### Q07. 자르기 확대 슬라이더의 접근성 식별

확대 슬라이더에 `사진 확대 비율` 레이블을 추가했다. `CropEditorGestureInstrumentedTest`가 실제 터치 pinch/drag, 1–4배 clamp, 빈 가장자리 방지, slider, 네 방향 버튼, 가운데 맞춤, 완료 callback을 검사한다. 포인터마다 원본을 새로 디코드하는 변경은 없다.

### Q08. 날짜 기본 표시 ON이 새 작업에서 무시됨

실제 설정 파일은 ON인데 이후 만든 카메라·앨범 초안의 `showDate`가 false인 것을 확인했다. 새 `SessionDocument`가 `SessionDraft`의 false 기본값을 사용하고, 일반 편집이 그 저장값으로 UI 기본값을 덮는 것이 원인이었다.

새 카메라 세션과 첫 유효 앨범 가져오기는 현재 저장 설정을 초안에 복사한다. **기존 초안은 설정 변경으로 덮지 않는다.** 앨범 import journal에도 생성 당시 값을 남겨 파일 게시 후 중단 복구 시 유지한다. 해당 필드가 없는 과거 journal은 기존 OFF 동작을 유지한다. 날짜 문자열은 계속 작업 생성일 기준이다.

근거: `CaptureDateDefaultInstrumentedTest` 3개, `AlbumDateDefaultInstrumentedTest` 3개. UUID별 사진·문서·SharedPreferences로 격리했고 실제 QA/배포 앱 설정을 테스트 fixture가 바꾸지 않는다. 최종 R8 QA에서 날짜 기본 표시 ON으로 카메라·앨범 2컷을 각각 새로 만들고 화면과 실제 JPEG의 날짜를 확인했다.

### Q09. 요청한 상단 번호 브랜드 제거

작업 시작 전부터 존재하던 승인된 `OccasionFramePainter.kt` 변경을 보존했다. 88종 공통 상단의 `Pocket 4Cut / NN`만 제거하며 테마 제목과 하단 브랜드는 유지한다. 새 이미지 파일이나 atlas를 생성하지 않았다. 과거 완료 JPEG는 다시 렌더하거나 변경하지 않는다.

### 이번 작업의 기준 커밋에 이미 포함된 두 수정의 재검증

- 업데이트 후 앨범 추가: 새 UUID의 가져오기를 전체 legacy 이전 실패와 분리하고 초기 복구와 picker callback 순서를 보호한 `25d825f` 수정. 손상 legacy index/pending을 만든 격리 계측과 QA 앱 데이터 유지 업데이트로 재검증했다. 실제 사용자 배포 앱의 모든 기존 데이터 상태를 읽거나 생산 패키지를 업데이트한 검사는 아니다.
- 88종 상단 메뉴 가림: 공통 Canvas의 save/clip/finally restore와 preview clip 수정. 88개 카드를 실제로 선택·로드하며 상단 제목/뒤로/닫기/카테고리 픽셀이 남는지 검사했고 밝은/어두운 대표 테마는 실기기 화면으로도 확인했다.

## 4. 버전 변경 전 1.6(7) 자동 검증과 실패 이력

### 1.6(7) 최종 실행 명령

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest `
  :app:testDebugUnitTest :app:lintDebug :app:lintRelease `
  :app:assembleQaRelease :app:bundleRelease --console=plain
```

기존 Gradle 연결 검사는 UTP가 QA 앱을 제거하고 연결이 끊기는 초기 실행이 있어, 이후 같은 AndroidJUnitRunner를 직접 실행했다. 배포 앱에는 영향이 없다.

```text
adb shell am instrument -w -r
  -e renderMemoryStress true
  -e seasonalExport true
  -e seasonalSourceRevision 25d825f-final-date-and-completion
  -e occasionFinalExports true
  -e notClass com.pocket4cut.OccasionFramePickerInstrumentedTest#applyRemainsReachableOnLandscapeWithDoubleFontScale,com.pocket4cut.OccasionFramePickerInstrumentedTest#recoveryErrorActionsRemainReachableInCompactLandscapeWithDoubleFontScale
  com.pocket4cut.qa.test/androidx.test.runner.AndroidJUnitRunner
```

현재 정의: JVM 25개 + 계측 176개. 가로 전용 2개를 제외한 실행 대상 계측은 174개다. 3개 opt-in을 모두 활성화했으며, runner에서 174개 시작과 174개 성공 종료 및 최종 OK를 대조했다.

### 확인된 중간 결과와 최종 결과 구분

- 날짜 수정 전 전체 실기기 실행: **166/166 PASS**, 실패/건너뜀 0, 297.846초.
- 선택 완료 guard의 관련 재검사: **14/14 PASS**, 14.432초. 선택 복구 6개, navigation guard 2개, MainFlow 접근성 6개. 앞선 166개와 중복되므로 합산하여 180개 통과라고 하지 않는다.
- 최종 날짜 수정 후 전체 계측: **174/174 PASS**, 실패·오류·건너뜀 0, 295.333초. `instrumentation-release-candidate.log`의 최종 runner 결과다.
- 최종 Gradle: **BUILD SUCCESSFUL**, 5분 17초, 173개 task 중 41개 실행/132개 UP-TO-DATE. `build-date-default-final.log`. 모든 task가 새로 실행됐다고 표현하지 않는다.
- JVM: **25/25 PASS**, 실패·오류·건너뜀 0. 최종 `testDebugUnitTest` XML 6개를 합산했다.
- Debug Lint: 오류 0, 경고 56, 힌트 6. Release Lint: 오류 0, 경고 55, 힌트 6. suppression/baseline을 추가해 통과시키지 않았다. 최종 보고 XML에서 건수를 다시 대조했다.
- Debug 경고 분류: UseKtx 18, GradleDependency 11, UnusedResources 8, ModifierParameter 7, IconLauncherShape 5, IconXmlAndPng 2, NewerVersionAvailable 2, AndroidGradlePluginVersion 1, RedundantLabel 1, UseOfNonLambdaOffsetOverload 1. 의존성 일괄 업데이트·아이콘 교체·대규모 코드 스타일 변경은 이번 버그 수정에 섞지 않았다. 경고 0건이라고 보고하지 않는다.
- 런타임 자산: `npm.cmd run validate:android` PASS. 88개 테마, 10분류, 77+11 그룹, atlas 88개와 thumbnail 88개, 33,687,596 검증 바이트. 최소 합성 PSNR 42.01dB, 알파 차이 0, 변조 거부 14건.
- `git diff --check` 통과. 최종 관련 파일을 명시적으로 stage하고 staged 공백 검사·diff를 별도로 검토한다.

실패/미완료 실행도 보존한다. 초기 `connectedDebugAndroidTest`는 연결 종료와 불완전 수집으로 PASS가 아니다. CustomFrame 4개는 수정 전 실패했다. Selection 신규 검사의 첫 실행은 Kotlin 반환형 때문에 JUnit의 `should be void` 초기화 오류가 있었고, 같은 명령의 잘못된 접근성 클래스명도 오류였다. 반환형을 Unit으로 명시하고 실제 클래스명으로 재실행하여 위 14개가 통과했다. 이 오류를 앱 자체 crash로 기록하지 않는다.

## 5. 버전 변경 전 1.6(7) 실제 기기 전체 사용 흐름

아래 6개는 시스템 Photo Picker 또는 실제 CameraX 센서에서 시작해 최종 이미지 생성·사진첩 저장까지 수동 조작했다. 모든 조합을 같은 경로로 6회 곱해 실행했다는 뜻은 아니다.

### 카메라 2컷 — PASS

4장 실제 촬영 → 두 장을 2,1 순서로 선택 → 세로 2컷 → 검정 프레임 → 소프트/흑백/필름 전환 → 문구/날짜 → 상세 회전·반전·색 보정 → 결과/수동 저장.

영문 30자 제한과 Samsung 한글 IME 입력을 확인했다. 편집 중 실제 force-stop 후 홈 이어하기에서 문구 `가`, 필름 필터, 검정 프레임, 날짜를 복원했다. 결과 JPEG **1248×3307**.

### 카메라 4컷 — PASS

8장 실제 촬영 중 첫 사진 이후 Home에 머물렀다가 돌아오면 일시정지 상태다. 실제 프로세스 종료 후에도 사용자 재개 전 자동 촬영하지 않는다. 남은 7장만 촬영하여 8장이 보존됐다. 네 장 선택, 4컷 3개 배치 선택, 계절 프레임 4종 전환, 가을/클래식 세로 4컷 결과 저장. JPEG **1650×4920**.

아주 짧게 Home을 눌렀다 즉시 복귀한 첫 시도는 Activity STOP에 도달하지 않아 중단 검사 근거로 삼지 않았다. 실제로 백그라운드에 머문 별도 시도와 프로세스 종료 검사로 판정했다.

### 카메라 6컷 — PASS

10초 설정 확인, 후면 시작, 바로 촬영과 중단/재개, 총 10장 → 6장 선택 → 6컷 3개 배치 선택 → 비대칭 콜라주 → Halloween 프레임 → 두 번째 사진 crop 약 3.8배/이동 → 자동 사진첩 저장. 첫 대표 슬롯과 다른 사진의 순서를 보존했다. JPEG **3383×4728**.

실제 후면/전면 프리뷰와 촬영은 수행했다. 실제 문자를 같은 방향 표준 타깃으로 찍어 모든 렌즈/센서 미러링을 독립 계측한 것은 아니므로 그 범위는 PARTIAL이다.

### 앨범 2컷 — PASS

Picker 취소 → 다시 열기 → 1장만 가져오기 → 같은 사진 중복 선택 안내 → 나머지 1장 추가 → 가로 2컷 → 커플 100일 프레임 → crop 1.1배/이동 → 결과 저장/보관함 재열기/공유. JPEG **2350×1878**. 카메라나 전체 사진 읽기 권한 없이 앨범 경로가 동작했다.

### 앨범 4컷 — PASS

4장 제한에서 다섯 번째를 추가하지 못하는 실제 picker 동작 확인 → 앱 내부 드래그 순서 변경 → 1장 제거/재추가 → **2,1,4,3** 순서 확정 → 시스템 글자 2배에서 레이아웃 진입 → 2×2 → 커스텀 ivory/별 스티커 → 오른쪽 이동·10% 확대·15도 회전 → 저장. JPEG **2350×3304**. 최종 JPEG를 직접 열어 번호 순서와 스티커를 확인했다.

### 앨범 6컷 — PASS

3장만 가져온 후 실제 force-stop → 홈 `앨범 6컷 이어서 작업하기` → 기존 3장 유지 → 나머지 3장 추가 → 3×2 배치 → `오늘의 날씨` 직접 기록 프레임 → 소프트 필터 → 회전/반전 후 현재 사진 초기화 → 자동 저장. JPEG **3631×3615**. 최종 번호는 1~6 순서이며 자동 날씨/위치 예시는 삽입되지 않았다.

### 추가 수동 경계 검사

- 최초 카메라 권한 거부/재요청/허용 PASS.
- QA 카메라 권한 회수 → 권한 안내 → 실제 설정 앱 링크 → 앱 사용 중 허용 → 앱 복귀 후 카메라 미리보기 재연결 PASS.
- 카운트다운 1/3/10초 설정값 확인 및 실제 촬영 실행 PASS. 6컷에서는 바로 촬영을 함께 사용했으므로 모든 컷이 10초를 끝까지 기다렸다는 시간 정확도 검사로 확장하지 않는다.
- 4개 앱 계절 테마 전환, 자동 사진첩 저장 ON/OFF, 전면 기본 선택, 날짜 기본 표시 UI 조작 실행. 날짜 기능은 Q08 결함을 발견해 별도 수정/재검사했다.
- 초기 설정 전체 삭제 확인창에서 완료본 6개와 사진첩 사본 보존 안내 확인, 취소 후 기존 작업 보존 PASS. 마지막에는 아래 R8 단계에서 이번 QA가 만든 초안·완료본만 실제 삭제하고 사진첩 사본/외부 원본 보존을 검사했다. 생산 데이터 삭제는 실행하지 않았다.
- 문의 화면 2배 글자 주요 버튼과 메일 앱 선택 시트 열기/취소 PASS. 메일을 보내지 않았다. 클립보드 교체/주소 복사는 NOT TESTED.
- 시스템 dark에서 세로 홈과 시스템 바를 실제 화면으로 확인하고 원래 `custom_schedule` 상태로 복원했다. 앱 전체 모든 화면의 light/dark 조합은 PARTIAL.
- 실제 TalkBack 서비스 활성화는 확인했지만 첫 실행 전화 접근 안내가 개입했다. 음성 피드백을 들으며 전체 앱을 완주한 검사는 **NOT TESTED**다. 전화 권한을 부여하지 않았고 서비스는 원래 비활성 상태로 복구했다. Compose 의미/48dp assertion 통과와 TalkBack 완주를 혼동하지 않는다.

## 6. 버전 변경 전 1.6(7) 렌더·자산·원본·공유 검사 범위

### 이미지 조합

- 8배치×4필터×4문구/날짜 상태의 장면 투영 검사: 128조합. 260/520 논리 너비에서 두 크기의 `drawScene` 투영 대응과 pixel/좌표 assertion을 사용한다. 이 검사를 실제 최종 JPEG 생성과 구분한다.
- 88종×9레이아웃/버전×2문구 조건 = **1,584개 소형 렌더 조합**. 현재 8개 배치와 구형 6컷, 문구·날짜 둘 다 없음/둘 다 있음의 두 상태다. 1,584장 모두를 16MP JPEG로 저장했다는 뜻이 아니다.
- 실제 원본 기반 고해상도 JPEG는 **88종 각각 1장**, 8배치를 테마당 순환하여 **배치당 11장** 생성했다. `DetailEditViewModel.renderFinalCollage`의 region decode→보정/crop→렌더→결과 게시 경로를 사용했다. PhotoId 역순·crop·회전·반전, 결과 크기, 원본 SHA, 저장 문서 재열기를 확인했다. 이 검사는 MediaStore나 화면 내비게이션 88회 완주를 대신하지 않는다.
- 88개 카드 UI 순회는 각 테마 로딩 완료, header 픽셀 유지, 적용 callback ID를 검사한다. 카테고리와 재생성 별도 검사도 있다.
- 45개 색상, 25개 스티커, 계절 4종, EXIF 8방향, 사용자 회전 4종/반전 2종, neutral crop과 기존 aspect-fill pixel 동일성, 영역 decode, 극단 비율/할당 제한을 계측했다.
- 렌더러 소유 메모리 상한 검사와 atlas 2장 캐시 검사를 실행했다. 앱 프로세스 전체 PSS가 128MiB 이하라는 주장은 하지 않는다. 한 측정 시점 전체 PSS는 약 332MiB였으며 플랫폼/UI/디코더 메모리를 포함한다.

### 실제 파일 확인

Debug 수동 6개와 최종 R8 수동 2개, 총 **8개 완료 JPEG**의 실제 크기와 파일을 검사했다. 모두 JPEG이며 GPS EXIF가 없었다. R8 설치 이후에도 먼저 만든 6개 결과의 SHA-256가 유지됐다. 외부 앨범 fixture 6개의 SHA-256는 처음 만든 파일과 동일했다. import 사본도 해당 fixture 바이트와 동일하며 4컷의 PhotoId 순서가 2,1,4,3이었다. 8개 결과 각각 공용 사진첩 사본이 한 개씩이고 내부 JPEG와 해시가 같았다. 예전에 완성한 사용자 JPEG를 수정하지 않았다.

### 실제 공유 수신 — PASS

앱과 다른 UID의 최소 테스트 수신 앱을 만들어 Android 공유 시트에서 선택했다. 사진/저장소 읽기 권한 없는 수신 앱이 FileProvider read grant로 JPEG를 실제 읽었다.

- 읽은 크기: 289,144 bytes, 2350×1878.
- 원본 결과/수신 바이트 SHA-256: `520f991eb4442a891d1c68208fece9cbd37ad4e01586402f94bc4bc86e68b307`.
- 서로 일치했다. 공유 Intent만 만든 단위 검사보다 넓은 확인이다.
- KakaoTalk/Instagram/모든 외부 앱의 재압축·게시·계정 호환은 NOT TESTED다. 실제 메시지를 전송하지 않았다.

## 7. 버전 변경 전 1.6(7) 저장·이전·장애 범위

계측에서는 세션 revision/원자 게시/이전 정상본, 손상 JSON/미래 schema/누락 원본, legacy selectedIndexes 재적용 방지, tombstone/삭제 재생, 파일 게시 후 중단, import/removal journal과 외부 원본 소유권, MediaStore pending/실패/중복 내보내기, 결과 파일 보존을 fixture로 검사했다. 세부 메서드와 조건은 QA 범위 목록에 연결돼 있다.

실제 작업에서는 Home·force-stop·재실행, 데이터 유지 debug APK 업데이트, 일부 앨범 추가 후 재개, 반복 결과 열기와 공용 사진첩 저장을 검사했다. **fixture 단계별 오류 주입은 실제 디스크 전체를 가득 채우거나 OS가 임의로 프로세스를 종료한 시험과 다르다.** 장치 전체 저장공간 소진/저메모리 강제 압박/전원 차단은 NOT TESTED다. 실제 사용자 사진에 위험한 장애를 주입하지 않았다.

백업 allowlist·FileProvider 범위·공개 권한 계약은 소스/계측으로 확인했다. 실제 Google 백업 복원/기기 간 이전은 NOT TESTED다. 지원 API 26~35의 권한 분기는 API 36 성공으로 대체하지 않는다.

## 8. 버전 변경 전 1.6(7) R8와 배포 산출물

이 절의 크기·해시·서명·번들 검증은 **1.6(7) 후보의 역사 기록**이다. 새 빌드가 같은 출력 경로를 사용하므로 아래 경로만으로 현재 파일이 과거 후보와 같다고 판단하지 않는다. 1.7(8)의 새 크기와 해시는 9절에 기록한다.

최종 `qaRelease`를 기존 QA 데이터 위에 설치했다. 패키지 `com.pocket4cut.qa`, `1.6-qa-r8 (7)`, debuggable 플래그 없음으로 확인했다. release와 같은 최적화 설정이지만 별도 applicationId·QA 서명이므로 Play 배포본 설치 검사를 대신하지 않는다.

- 설치 전 완료본 6개와 미완성 작업 1개가 설치 후에도 보였다. 완료본 6개 원본 결과 해시는 모두 같았다.
- 날짜 기본 표시 ON → 앨범 2장 → 세로 2컷/흰 프레임 → 저장을 완주했다. 새 1248×3307 JPEG를 열어 `2026.09.27` 날짜를 확인했다.
- 날짜 기본 표시 ON → 실제 카메라 1초 카운트다운으로 4장 촬영 → 2장 선택 → 세로 2컷 → 자동 저장을 완주했다. 새 1248×3307 JPEG의 날짜와 사진첩 사본을 확인했다.
- 총 8개 완료 결과·사본을 검증한 다음, 이번 QA에서 만든 미완성 촬영 작업을 확인창으로 버렸다. 완료본 8개가 유지됐다. 이어 QA 앱의 전체 삭제 확인창에서 실제 삭제했다. QA 내부 사진 파일은 0개가 됐지만 공용 사진첩 사본 8개와 외부 합성 사진 6개는 각각 삭제 전 해시와 같았다.
- QA 앱은 빈 홈 화면으로 남겼다. 테스트용 공용 사진 8개와 합성 사진 6개는 삭제 범위 계약에 따라 남아 있다. QA 앱 설정은 카운트다운 3초·전면 기본 ON·자동 저장 OFF·날짜 기본 OFF로 복구했다. 기기 글자 0.8, 회전 값 0/0, TalkBack 원래 비활성, 야간 모드 `custom_schedule`도 대조했다. 화면 켜짐 유지 값은 바꾸지 않았다.
- 마지막 crash buffer에는 QA 시작 전 04:46의 시스템/타 앱 종료 기록만 있었고 `com.pocket4cut.qa` 종료 기록은 없었다. 한 번 읽은 buffer가 모든 기간·모든 native/ANR 상황의 부재를 증명하지는 않는다.

- 산출물: `app/build/outputs/bundle/release/app-release.aab`.
- 크기: **72,293,846 bytes (68.94MiB)**.
- SHA-256: `0a76bde679c0be09f6a0c65bae3b3f7985378e715fe1d397b60bf3cf08d195d9`.
- bundletool 1.18.3 `dump manifest` 및 `validate`: 각각 exit 0. package `com.pocket4cut`, versionName `1.6`, versionCode `7` 확인.
- ZIP entry 530개, 중복 0. artwork 88개와 thumbnail 88개의 길이·SHA-256가 내장 manifest/소스와 전부 일치했다. 검증 자산 33,687,596 bytes. 1,584개 정적 완성 PNG를 AAB에 넣지 않았다.
- `jarsigner -verify -verbose -certs`: exit 0, `jar verified`. 자체서명·인증서 체인 신뢰 실패·timestamp 없음·POSIX 속성·JarInputStream manifest 및 528개 entry 해석 차이 경고가 있다. 따라서 무경고/strict 검증 통과라고 표현하지 않는다. 인증서 만료는 2053-10-19이며 keystore/암호 파일은 출력하지 않았다.
- 검증 시각: 2026-09-27 16:23 KST. 최종 Gradle 성공 이후 실제 산출물에 대해 검사했다. Play 업로드 키 승인과 배포 설치는 이 검사에 포함되지 않는다.

**당시 1.6(7) AAB는 동일 코드로 새 Play 업데이트 업로드가 가능한 파일로 판정하지 않았다.** 당시 기기의 Play 앱도 `com.pocket4cut` 1.6(7)이었고, 더 큰 미사용 versionCode가 필요했다. 이 단계에서는 당시 사용자 지시에 따라 버전을 유지했다. 이후 사용자 승인을 받아 소스를 1.7(8)로 올렸다. 새 산출물 검증은 9절에 기록했으며 Play Console 코드 8의 사용 이력 확인은 별도로 남아 있다. [Android 버전 관리 공식 문서](https://developer.android.com/studio/publish/versioning)

Play Console 업로드·심사·내부 테스트 배포는 실행하지 않았다. 서명된 AAB 생성 성공은 Play의 서명키 등록/버전 수락/기기 호환 승인과 별개다.

## 9. 1.7(8) 최종 재검사 — 실행 범위 PASS

### 기준과 실행 검증

- 소스 버전: `app/build.gradle.kts`의 `versionName=1.7`, `versionCode=8` 확인. 이번 버전 요청으로 이미지 자산이나 화면 방향 정책을 변경하지 않는다.
- 전체 Gradle 빌드: **BUILD SUCCESSFUL, 6분 42초, 173개 task 중 46 실행/127 UP-TO-DATE**. 명령은 4절과 같은 debug/계측 APK·JVM·Debug/Release Lint·R8 QA·release AAB 전체 명령이다. 새 로그는 `build-1.7-final.log`이다.
- JVM 및 Lint: `testDebugUnitTest`는 이번 버전 변경에서 **UP-TO-DATE**로 앞선 동일 테스트 코드의 XML을 재사용했다. 6개 XML의 25개 통과·실패/오류/건너뜀 0을 다시 대조했지만 새 JVM 실행으로 합산하지 않는다. Debug/Release Lint analysis/report는 새로 실행했고 각각 오류0/경고56/힌트6, 오류0/경고55/힌트6이다. `aab-1.7-jvm-lint.json`에 task 상태와 XML 시각을 보존했다.
- 실기기 AndroidJUnitRunner: **174/174 PASS, 실패·오류·건너뜀0, 327.454초**. 1.7 Debug APK를 실제 설치한 뒤 4절과 같은 명령에서 `seasonalSourceRevision=25d825f-final-1.7-code8`로 실행했다. 가로 전용2개 제외와 renderMemoryStress/seasonalExport/occasionFinalExports opt-in을 유지했다. 로그 `instrumentation-1.7-final.log`의 `OK (174 tests)`와 종료코드를 확인했다. 88종 실제 고해상도 JPEG·카드 UI·1,584개 소형 렌더 조합도 포함한다.
- R8 QA 설치·실행: 데이터 초기화 없이 `com.pocket4cut.qa`에 `1.7-qa-r8 (8)`을 설치했다. debuggable 플래그 없음, `am start -W`의 COLD 시작667ms/Status ok. 실제 홈→설정의 버전표시→카메라2컷 구성→CameraX 전면 미리보기→뒤로→앨범4장 Photo Picker→취소→빈 확인화면→홈을 확인했다. 설정 화면과 미리보기를 캡처해 열어 확인했다. 버전 변경 후 수동으로 8개 결과를 다시 만들지는 않았으며 1.6에서의 8개 수동 결과와 1.7의 88개 자동 최종 결과를 구분한다. QA 서명 설치는 Play 배포본 설치와 다르다.
- 기기 상태·보존 확인: `after-1.7-state-verification.json`에서 공용 사본8개와 외부 합성원본6개 해시가 그대로이고 QA 소유사진0개임을 재확인했다. 최종 QA는 빈 홈, 글자0.8·회전0/0·TalkBack비활성·야간custom_schedule이다. 화면 타임아웃15000·전원유지15를 읽기만 했으며 변경하지 않았다. 최종 crash buffer에서 Pocket 4Cut 프로세스 종료 기록은 발견되지 않았지만 전체 기간·모든 ANR 부재를 보장하지 않는다.
- 1.7(8) 판정: **실행한 빌드·계측·R8 진입·번들 검사 PASS**. 외부 배포와 다른 기기·미실행 수동 조합은 아래 제한을 유지한다.

### 새 1.7(8) AAB와 패키지 검증

- 대상 출력 경로: `app/build/outputs/bundle/release/app-release.aab`.
- 새 AAB 크기: **72,293,844 bytes (68.94MiB)**.
- 새 AAB SHA-256: `338817a4cca22298155c139dd37f54d320faccb0f270aaa5c2a101c4a5c3ea92`.
- bundletool 1.18.3 `dump manifest`/`validate`: 각각 exit0. `com.pocket4cut`, `versionName=1.7`, `versionCode=8` 확인.
- ZIP·번들 자산: entry530개/중복0, atlas88개와 thumbnail88개, 총176개의 길이·SHA-256가 모두 일치했다(33,687,596bytes). catalog와 source asset manifest 해시도 일치했다.
- `jarsigner -verify -verbose -certs`: exit0, `jar verified`. 자체서명/PKIX 체인/timestamp 없음/POSIX 속성/JarInputStream Manifest 및 528entry 해석 차이 경고는 남아 있다. 인증서 만료2053-10-19. 검사 시각은 **2026-09-27 16:53:47 KST**, 새 증거는 `aab-1.7-*`에 보존했다. 무경고/strict 검사 또는 Play 승인을 의미하지 않는다.

### 외부 배포 상태

- 동일 코드 7 재사용 문제: 사용자 승인에 따라 소스 코드를 8로 변경했다. 1.6(7) 당시 차단 판정을 현재 코드의 동일 문제로 반복하지 않는다.
- Play Console 코드 8 미사용 확인: **NOT TESTED**.
- 1.7(8) AAB 업로드·서명키 수락·내부 테스트 배포·심사/게시: **NOT TESTED**.
- 폰트 라이선스 조사·공개 개인정보 페이지 검증·가로 화면 검사는 **EXCLUDED**를 유지한다. 화면 켜짐 유지 설정은 변경·시험하지 않는다.

## 10. 남은 출시 확인 사항

- **NOT TESTED — Play Console 코드 8 사용 이력과 업로드**: 코드 7 재사용 문제에 대응해 소스를 8로 올렸지만, 코드 8이 Console에서 미사용인지와 새 AAB가 실제 수락되는지는 확인하지 않았다.
- **BLOCKED — 다른 OS/기기**: 연결된 한 대 이외의 API 26/28 fallback·구형 저장 권한, API 29 전환, API 33 권한 경계, 다른 OEM과 더 낮은 메모리 기기는 이번에 실행하지 않았다.
- **NOT TESTED — 이번 후보의 실제 배포 업데이트**: 기기에서 확인한 Play 앱 버전과 이번 새 후보의 설치/기능 검증은 다르다. 배포 패키지를 이번 AAB로 업데이트해 사용자 자료를 검사하지 않았다. 격리 legacy 이전과 QA 데이터 유지 업데이트 PASS는 별도 증거다.
- **NOT TESTED — 클라우드 제공자/HEIF 실사진 전 범위**: 실제 cloud 다운로드, 권한 만료, HEIF 기기별 decoder, 최대64MiB/250MP 경계 전수 실사진은 완료하지 않았다. JPEG/PNG/staticWebP·거부 형식·절단 데이터의 계측 범위는 별도다.
- **PARTIAL — 접근성과 화면**: 세로 360×640dp fixture, 실제 기본/2배 글자와 주요 의미 계약은 검사했지만 TalkBack 음성 전체 완주, 외부 키보드 전체 경로, 모든 화면×글자1/1.5/2×테마의 Cartesian product는 완료하지 않았다.
- **PARTIAL — 실제 카메라/장시간 성능**: 전후면·2/4/6컷·Home/프로세스 복원은 실행했다. 표준 타깃을 이용한 센서별 방향/미러링 전수, 통화 중 카메라 선점, 잠금/강제 종료를 모든 촬영 단계에 주입, 장시간 발열/배터리/FPS 정량 평가는 남았다.
- **NOT TESTED — 외부 시스템**: 실제 백업/기기 이전, 모든 공유 수신 앱, Play pre-launch report/정책·AAB 수락은 실행하지 않았다.
- **EXCLUDED**: 가로 화면 검사, 폰트 라이선스 확인, 공개 개인정보 페이지, 화면 켜짐 유지 설정 변경/시험.

이 항목들을 PASS로 바꾸지 않는다. 실행된 범위에서 확인한 결함은 수정·재검사하고, 지원 OS 전체에 대한 최종 출시 판단은 남은 환경의 증거와 함께 내려야 한다.

## 11. 변경 파일·Git·증거 위치

- 제품 코드: `PhotoImportRepository`, `OccasionFramePainter`, `CaptureViewModel`, `CropEditorScreen`, `CustomFrameEditorScreen`, `PocketNavHost`, `PhotoImportScreen`, `PhotoImportViewModel`, `SelectionScreen`, `SelectionViewModel`, `AppSettings`.
- 버전 설정: `app/build.gradle.kts`의 versionName 1.6→1.7, versionCode 7→8. 사용자 후속 요청으로 변경했다.
- 보강한 기존 계측: `OccasionFramePickerInstrumentedTest`, `PhotoImportAccessibilityInstrumentedTest`, `PhotoImportRepositoryInstrumentedTest`.
- 새 계측: `SelectionRecoveryInstrumentedTest`, `LayoutSelectionRouteInstrumentedTest`, `CustomFrameAccessibilityInstrumentedTest`, `CropEditorGestureInstrumentedTest`, `OccasionFinalResultInstrumentedTest`, `CaptureDateDefaultInstrumentedTest`, `AlbumDateDefaultInstrumentedTest`.
- 문서: README, ARCHITECTURE, IMAGE_ASSET_GUIDE, WORKLOG, 이 보고서와 QA 범위 목록.
- 새 이미지 자산 없음. 기존 atlas 1536×1024와 thumbnail 240×160을 그대로 사용한다. 검사용 번호 JPEG와 실제 결과/스크린샷은 앱 배포 자산이 아니다.
- 로컬 증거: `build/release-qa-20260927/`의 실행 로그, XML/PNG, `pre-r8-result-audit.json`, `r8-result-audit.json`, `after-delete-verification.json`, 수신 앱 결과, 서명/번들 검증 로그. 공개하지 않는다.
- Git 식별: 이 보고서와 변경 파일을 포함한 커밋을 `codex/release-qa-20260927`에서 보존한다. 자체 커밋 SHA를 문서에 다시 삽입하려고 amend하지 않는다. 최종 응답에서 실제 SHA와 원격 push 결과를 함께 제공한다.
- Git 통합 계획: 기존 사용자 승인에 따라 작업 브랜치의 검증·커밋·일반 push 뒤 `main`에 `--ff-only`로 통합한다. 이는 실행 예정이며 실제 통합·push 결과는 마감 시 기록한다. force push나 기존 이력 재작성은 하지 않는다.

## 12. Play Console 출시 내역 제안 — 세 가지 길이

아래는 이번 수정 내용을 사용자 관점으로 정리한 문안이다. 내부 테스트 수·기기 모델·개발 용어나 확인하지 않은 전 기기 보장을 넣지 않았다.

### 자세한 버전

- 앱 업데이트 후 앨범에서 선택한 사진이 추가되지 않던 일부 상황을 수정했어요.
- 88종 프레임을 고를 때 상단 메뉴가 가려지거나 깜박이던 문제를 해결했어요.
- 프레임 위쪽의 공통 번호 표기를 정리하고, 아래쪽 브랜드는 유지했어요.
- 큰 글자에서도 앨범 확인과 커스텀 프레임의 다음 버튼에 쉽게 접근할 수 있도록 개선했어요.
- 사진 선택을 저장하는 동안 연속 입력으로 작업이 꼬이지 않도록 보강했어요.
- 사진이나 편집 내용을 불러오거나 저장하지 못했을 때, 작업을 유지한 채 다시 시도할 수 있도록 개선했어요.
- 새 카메라·앨범 작업에 ‘날짜 기본 표시’ 설정이 올바르게 적용되도록 수정했어요. 기존 작업의 날짜 선택은 유지돼요.
- 사진 자르기의 확대 조작과 저장·복원 안정성을 점검하고 개선했어요.

### 간략한 버전

앨범 사진 추가 오류와 88종 프레임 메뉴 가림 문제를 수정했어요. 큰 글자 화면, 사진 선택·저장, 날짜 기본 표시와 자르기 사용성을 개선했어요.

### 이해하기 쉬운 일반 버전

앨범에서 사진을 골랐는데 추가되지 않거나, 프레임을 고를 때 위쪽 메뉴가 사라지던 문제를 고쳤어요. 글자를 크게 사용해도 다음 단계로 이동하기 편해졌고, 사진 선택과 저장 중 오류가 나면 작업을 유지하면서 다시 시도할 수 있어요. 새 작업에는 설정한 날짜 표시도 제대로 적용돼요.
