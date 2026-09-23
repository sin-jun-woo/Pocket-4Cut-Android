# Pocket 4Cut 작업 로그

작업 단위로 최신 기록을 위에 추가한다. 날짜는 별도 표기가 없으면 Asia/Seoul(KST) 기준이다. 실제 수행 결과와 향후 계획을 구분하고, 실패·캐시 재사용·미실행을 성공으로 합치지 않는다.

`docs/`는 GitHub Pages 배포 대상이므로 공개 가능한 요약만 기록한다. 비밀값, 사용자 사진, 기기 serial, 개인 로컬 경로, 원시 실행 로그를 넣지 않는다. 세부 실행 산출물은 로컬 build/캐시 영역에 두고 필요한 명령·결과만 남긴다.

## 2026-09-23 — Everyday Editions 기념일 88종 이미지 제작 (Asia/Seoul)

- 요청/범위: 표의 기념일 77종과 특수 11종을 모두 독립 디자인하고 2·4·6컷 전체 배치와 다운로드·인스타 이미지 1종을 제작한다. 후속 지시인 **“테스트하지 말고 프레임부터”**를 우선해 앱 코드·런타임 리소스·선택 UI·빌드·테스트는 변경/실행하지 않았다. 기존 계절 4종도 보존했다. 분위기·색감·소품·여백·글자가 장식 개수보다 중요하다는 추가 요청을 타이포그래피·장식 밀도에 반영했다.
- 기준: 시작 HEAD `b51ea8bb0655bc3498e435236dedd4ba696929c7`, 시작 `codex/release-readiness` clean. 새 작업 브랜치 `codex/occasion-frames`. 아래는 이번 디자인 작업 트리 기준이며 앱 배포 결과가 아니다.
- 제작: 내장 전용 이미지 생성 도구로 주제별 별도 원본 88장과 홍보용 종이 배경 1장을 생성했다. 모델명·최고 품질 선택 옵션은 도구에 노출되지 않는다. 마스터·정확한 프롬프트·출처를 보존하고, 생성물을 사람이 직접 그린 것으로 표시하지 않는다. 광복절 태극기만 행정안전부 공식 도안을 썼다.
- 디자인: `source/catalog.json`의 88개 고유 ID와 10개 카테고리, `art-direction.json`의 주제별 방향·서체·패턴·장식 밀도를 분리했다. 웨딩/졸업의 단정한 비대칭 제목, 기록의 모노스페이스, 스포츠의 굵은 글자, 추억의 절제된 여백 등으로 구분했다. UI 구분과 동적 문구는 `NEXT-APP-INTEGRATION.md`의 후속 제안이다.
- 출력: 보존된 Paper Seasons manifest의 실제 앱 사진 칸 좌표로 정적 합성했다. 주제당 현재 8배치 + 문구 공간 8변형 + 이전 6컷 2변형 = 18장, **총 1,584 RGBA PNG**다. 88개 생성/정리 아틀라스는 각각 1536×1024, 완성 프레임 폭 1248–4992px / 높이 1878–4971px다. 88개 테마 모아보기, 10개 카테고리 및 전체 모아보기, 가상 성인 사진 비율 예시 88장, Instagram 1080×1350 및 2160×2700 마스터를 제공한다.
- 실행: `node design/occasion-frames/everyday-editions-v1/source/render-collection.cjs`(범위별 실행 및 필요한 테마 재출력), `render-promo.cjs`, `package-collection.cjs`로 미디어를 출력한다. 리본/줄기/지도 등이 균등 셀을 넘는 6종은 자동 크롭을 중단하고 중립 배경 육안 확인 후 별도 ROI/다각형으로 분리했다. 원본을 잘라 덮어쓰지 않았다. 이 미디어 제작 명령과 Git diff 확인을 앱 테스트/빌드 통과로 표현하지 않는다.
- 육안 범위: 모든 원본을 생성 직후와 중립 배경 시트로 확인했다. 10개 분류 대표 배치의 글자·장식·사진 칸, 대표 2·4·6컷 모아보기, 웨딩/추석 원본 PNG와 졸업/출국의 caption 출력, 위치 기록·광복절 전체 8배치, 홍보 이미지를 확인했다. **1,584장 모두를 개별 확대 검사하거나 실제 Android에서 실행한 것은 아니다.**
- 전달/Git: `design/occasion-frames/everyday-editions-v1/`에 원본·프롬프트·PNG·좌표/해시·스크립트·안내를 둔다. 전체 ZIP과 85 MiB 이하 카테고리 ZIP은 로컬 다운로드이며 중복 대용량 파일이라 하위 `.gitignore`로 제외한다. Git에는 원본 PNG와 재생성 도구를 보존한다. 카테고리 ZIP은 한 테마의 18장+모아보기+예시를 쪼개지 않는다. 실제 파일 용량·목록은 납품 `download-manifest.json`을 따른다.
- 최종 패키지 실행: `package-collection.cjs` exit 0, `themes=88 / frames=1584 / examples=88`; 전체 ZIP 698,471,379 bytes, 1,775개 파일과 카테고리 ZIP 13개를 생성했다. SHA-256은 manifest에 기록했다. 실제 ZIP 전체를 재해제/전수 디코딩하는 별도 검사는 실행하지 않았다. 최초 읽기 전용 개수 표시 명령은 괄호 오타로 한 번 실패했으나 수정한 명령에서 88개 기록·1,584개 출력·동일한 최종 renderer hash를 확인했다. 이미지 원본 메타데이터는 88장 모두 1536×1024 sRGB/alpha=true였다.
- 변경 파일: 위 디자인 폴더, `docs/IMAGE_ASSET_GUIDE.md`, 이 작업 로그. 새 앱 의존성·버전 변경·기기 설치·게시 없음.
- 미구현/미실행: 앱 적용과 분류 UI, 검색/즐겨찾기, 날짜/D-Day/위치/날씨 자동 입력, Android preview/export 일치, 실기기 메모리·인쇄·Instagram 실제 게시. 날짜·온도·횟수는 디자인 예시다. 공통 성인 사진은 비율 확인용이며 아기/반려동물 주제의 실제 활용 사진으로 홍보하지 않는다. **Gradle 빌드, Lint, JVM/계측/실기기 테스트 모두 사용자 요청으로 실행하지 않았다.**

## 2026-09-22 — Android 16 실기기 최종 출시 후보 검사 (Asia/Seoul)

- 요청/범위: 연결된 실제 휴대전화에서 2·4·6컷 촬영→선택→편집→저장과 중단·복원·설정을 검사하고 발견한 문제를 수정한 뒤 AAB를 생성한다. 화면 켜짐 유지 설정은 변경·시험하지 않았다. 사용자가 제외한 글꼴 라이선스와 공개 개인정보 웹페이지 및 추가 에뮬레이터 검사도 이번 결과에 합치지 않는다.
- 기준: `codex/release-readiness`, 시작 HEAD `1b705cb3cd813efc062eefeed807f595e4b7adf2`, 시작 작업 트리 clean. 아래 결과는 이번 앱·테스트 수정이 반영된 미커밋 작업 트리 기준이다. 배포 앱 `com.pocket4cut` 대신 데이터가 분리된 QA 앱 `com.pocket4cut.qa`를 설치·실행했고 기존 배포 앱 자료는 초기화하지 않았다. 버전은 `1.4 (5)`로 유지했다.
- 수정: 빠른 전·후면 카메라 전환 시 CameraX 바인딩을 직렬화하고 중복 전환/촬영 시작을 막았다. 선택 화면 시스템 뒤로를 닫기 확인창에 연결하고, 보관함 복원 선택 화면에서 `홈으로`를 누르면 저장 후 실제 Home route로 이동하도록 보완했다. 선택 저장 중 중복 이탈 입력도 막았다. 정상 결과 조회가 관계없는 손상 세션별 문서 때문에 실패하지 않게 하고, 사진첩 저장 상태는 실제 MediaStore 사본을 확인하도록 바꿨다. 기록 URI가 없는 부분 pending 행은 소유가 확인된 경우 같은 작업으로 복사 재개하도록 보완했다. 구형 단일 `sessions.json` 가져오기가 실패해도 정상 세션별 문서의 보관함 스캔은 계속하고 Home/보관함에 경고하며, 구형 소유 관계를 판정할 수 없으면 삭제를 보류한다. 설정 카운트다운 슬라이더의 표시·저장값을 통일하고 설정 닫기 연타가 홈까지 pop하지 않게 했다. 사용자 후속 요청으로 홈 상단과 홈 프린트 미리보기의 앱 이름을 `Pocket 4Cut`으로 통일했다. 마지막 문자열 변경의 재검증 수치는 아래 명령의 후속 실행 결과로 구분한다.
- 변경 파일: 앱 소스 `CaptureViewModel.kt`, `SelectionScreen.kt`, `SelectionViewModel.kt`, `ResultScreen.kt`, `GalleryExporter.kt`, `SessionDocumentRepository.kt`, `GalleryScreen.kt`, `GalleryViewModel.kt`, `HomeScreen.kt`, `AppSettings.kt`, `SettingsScreen.kt`, `PocketNavHost.kt`, 신규 `NavigationGuard.kt`; 계측 `GalleryExporterInstrumentedTest.kt`, 신규 `ResultLinkInstrumentedTest.kt`, `CountdownSettingInstrumentedTest.kt`, `NavigationGuardInstrumentedTest.kt` 및 구형 오류 회귀 테스트; [실기기 보고서](../engineering/PHYSICAL_RELEASE_VERIFICATION_2026-09-22.md), 이 작업 로그, `ARCHITECTURE.md`. 새 이미지 자산·Gradle 버전 변경은 없다.
- 실기기 수동: Android 16/API 36 실물 카메라에서 QA 앱으로 2컷 4장, 4컷 8장, 6컷 10장을 각각 촬영하고 필요한 사진을 선택해 결과 저장까지 진행했다. 2컷은 선택 순서를 바꾼 뒤 커스텀 색·문구·필터·사진별 보정·반복 저장·공유 시트·보관함 재열기를 확인했고, 4컷은 겨울 프레임·흑백 필터, 6컷은 첫 사진이 큰 비대칭 배치를 확인했다. 첫 권한 거부 후 허용·카메라 진입, 전/후면 전환과 줌, Home/force-stop 뒤 이어하기를 수행했다.
- 수정 뒤 수동 회귀: 최종 R8 QA 앱에서 1초·10초 카운트다운 설정이 빠른 이탈·재진입 뒤에도 유지됐다. 설정 닫기 연타 뒤 홈이 표시됐다. 10초 카운트다운 약 3초 뒤 Home으로 나가 11초 이상 기다린 뒤 복귀해도 `0/4`와 `이어서 촬영`이었다. 화면 켜짐 유지 설정은 손대지 않았다.
- 추가 수동 확인: 자동 사진첩 저장 ON·날짜 기본 ON은 설정 재진입 뒤 유지됐고, 자동 저장 결과는 수동 저장 전 `저장 완료`를 표시했다. 이미 만든 초안에는 날짜 기본값을 소급 적용하지 않는다. 검사 뒤 전면 카메라 ON·3초·자동 저장 OFF·날짜 OFF로 복원했다. 글자 배율 2.0과 임시 가로 회전에서 각각 홈의 이어하기·촬영·보관함 버튼이 화면 안에 있음을 확인하고 시스템 글자 배율·자동 회전·방향을 원래 값으로 돌렸다. 이 두 화면 조건은 홈 한정이며 전체 앱 완주는 아니다.
- 선택 수동 회귀: R8 QA에서 2컷 4장 중 2장을 선택하고 닫기→`홈으로`를 연타해도 초안 이어하기가 있는 Home에 도착했다. 보관함의 진행 중 작업에서 선택 화면을 이어 연 뒤 같은 `홈으로`를 눌러도 보관함 대신 Home으로 이동했다.
- 홈 문구 재검사: 사용자 후속 요청을 반영한 QA APK를 최종 결합 빌드 뒤 실기기에 다시 설치했다. Home UI 트리에서 상단 라벨·프린트 미리보기의 `Pocket 4Cut` 두 곳을 확인했고 이전 대문자·슬래시 표기는 없었다. 별도 이미지 자산과 결과 JPEG 문구는 변경하지 않았다.
- 홈 브랜드 문자열 변경 전 검증: `./gradlew.bat :app:assembleDebug :app:assembleQaRelease :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:bundleRelease --offline --console=plain` **BUILD SUCCESSFUL**, 172 task 중 42 executed/130 up-to-date. JVM 1/1 통과, Debug Lint error 0/warning 56/hint 2, Release Lint error 0/warning 41/hint 2. 연결 계측 XML은 67개 중 65 통과·2 opt-in skip·실패/오류 0이며, 같은 2개를 옵션형 실행으로 별도 2/2 통과시켰다. 따라서 67개 고유 검사를 각각 실행·통과했으며 두 실행을 69개의 다른 테스트로 세지 않는다. 후속 세 코드 경계의 관련 계측도 이 실행에 포함된다.
- 홈 문자열 변경 후 빌드·계측: `./gradlew.bat :app:assembleDebug :app:assembleQaRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:bundleRelease --offline --console=plain` **BUILD SUCCESSFUL**, 5분 15초, 144 task 중 35 executed/109 up-to-date. 새 APK의 실기기 Home에서 `Pocket 4Cut` 두 곳을 확인했다. 이어 `:app:connectedDebugAndroidTest` 재실행은 **BUILD SUCCESSFUL**, 39초. 최종 XML은 67건 중 65 통과·2 opt-in skip·실패/오류 0이다. opt-in 두 건은 브랜드 문자열 변경 전 별도 실행에서 2/2 통과했고 테스트 소스는 이후 변경되지 않았다. Gradle CLI의 69/67 표기 대신 XML 67건을 기준으로 한다.
- 홈 문자열 변경 후 최종 번들: `app/build/outputs/bundle/release/app-release.aab` 38,281,110 bytes, SHA-256 `914D9DF8CB66F7E09FF62C9ABFC105D0FA35C810697023EB22FE568A56D2BB48`, `jarsigner -verify`에서 `jar verified`. 자체 서명 인증서 경고가 있으며 기존 Play 업로드 키 일치는 로컬 검사만으로 확인되지 않는다. 버전은 `1.4 (5)` 그대로다.
- 남은 범위: 한 대의 API 36 실기기로 다른 API/제조사, API 26–28 사진첩 권한, 저메모리·저장공간 고갈, 모든 촬영 중단 타이밍, TalkBack·2배 글자/가로 전체 흐름, 45색·25스티커·13글꼴의 전 조합, 공유 수신 앱의 실제 바이트 읽기와 클라우드 복원을 통과로 표시하지 않는다. 구형 단일 `sessions.json` 손상 분리 계측은 통과했으나 원문 자동 복구는 아니다. 실제 사진을 PC로 복사하는 행위는 개인정보 반출 자동 승인 심사에서 거절돼 원본/결과 파일 해시·EXIF 직접 대조를 하지 않았다. 출시 판정과 3종 Store 문구는 실기기 보고서에 상세히 기록했다.

## 2026-09-22 — Android 에뮬레이터 전면 감사 재실행 (Asia/Seoul)

- 요청/범위: 현재 저장소를 처음부터 다시 조사하고 앱 코드 수정 없이 에뮬레이터의 촬영·선택·프레임·편집·저장·공유·보관함 문제와 리팩터링 기준을 상세 보고서로 기록했다. 기준은 `codex/reels-promo`의 시작 HEAD `434584ec502e8349ebec0f6500b4d857ac26344e`, 시작 작업 트리 clean이다. 이전 감사의 기기 결과를 이번 실행 성공으로 합치지 않았다.
- 변경 파일: `engineering/EMULATOR_AUDIT_2026-09-22.md`, `engineering/audit-evidence/2026-09-22/`의 합성 카메라 장면·앱 화면 증거 15개, 이 작업 로그. 앱 기능 코드·Manifest·Gradle·에셋은 바꾸지 않았다.
- 빌드/테스트: `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --rerun-tasks --continue --offline --console=plain` 실행. 두 APK 생성 성공, 기본 JVM 테스트 1개 실제 실행·통과. Lint는 기존 `PermissionImpliesUnsupportedChromeOsHardware`로 실패(1 error/77 warnings/2 hints), 따라서 결합 명령 exit 1. `:app:connectedDebugAndroidTest`는 API 37 에뮬레이터에서 기본 3개 통과. 선택 `-I scripts/device-diagnostics.init.gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pocket4cut.diagnostic.RenderContractDiagnosticTest --offline --console=plain`은 7개 중 3개 통과·4개 실패(문구/날짜, 카탈로그 색, 스티커, 6컷 기하 중복).
- 기기 확인: 새 설치에서 2컷 4장→2장, 4컷 8장→4장, 6컷 10장→6장 선택까지 진행했다. 권한 설정 복귀 UI 미갱신, 선택·커스텀 상태의 회전 소실, 카탈로그 검정의 흰색 복원, 문구/날짜 및 스티커·계절 프레임의 preview/export 차이, 자동 사진첩 저장 중복을 재현했다. 수동 저장 JPEG와 Android 공유 시트, 촬영 중 Home 왕복, 큰 글자·다크모드도 확인했다. `logcat`에서 이번 앱 관련 FATAL 패턴은 발견하지 못했다.
- 보존/한계: 에뮬레이터의 글자 크기·시스템 모드·회전을 원래 값으로 돌렸다. 테스트 앱의 임시 세션은 계측 설치 과정 뒤 재설치되었고, 합성 결과의 공용 사진첩 사본은 남아 있다. 사용자 사진 삭제·저장소 손상 주입은 하지 않았다. API 26–36, 다른 기기·실기기, 메모리/공간 부족, 모든 프레임·글꼴·스티커 조합, TalkBack, 최적화 release는 이번 새 검사 범위 밖이다. 상세한 재현·수정 기준은 [보고서](../engineering/EMULATOR_AUDIT_2026-09-22.md)에 있다.

## 2026-09-11 — Play Console 앱 최적화 경고 대응 (Asia/Seoul)

- 요청/범위: Play Console의 ‘앱 최적화 낮음 / 난독화 0% / R8 구성 없음’ 표시를 해결할 release 빌드 구성을 적용한다. 앱 기능 결함과 기존 Lint 오류는 이번 변경에 섞지 않는다.
- 기준/보존: `codex/reels-promo`, 시작 HEAD `022fc33c`, 시작 작업 트리 clean 및 `origin/codex/reels-promo`와 0/0. 사용자의 최종 지시에 따라 버전은 `1.4 (5)`를 유지한다.
- 빌드 도구: AGP 8.13.2→9.0.1, Gradle 8.13→9.1.0, Kotlin/Compose compiler 2.0.21→2.2.10으로 갱신했다. AGP 9 내장 Kotlin에 맞춰 `org.jetbrains.kotlin.android` 적용과 `kotlinOptions`를 제거하고, 선택 진단 Kotlin source set을 `.kotlin.srcDir`로 등록했다. 설치된 SDK를 재사용하도록 Build Tools 36.1.0을 명시했다. wrapper 배포 SHA-256과 wrapper JAR SHA-256을 공식 값으로 검증했다.
- release 최적화: `isMinifyEnabled=true`, `isShrinkResources=true`, `proguard-android-optimize.txt`를 적용했다. 앱 전체 keep, 전역 `dontwarn`, `dontoptimize`, `dontshrink`, `dontobfuscate`는 추가하지 않았다. 선택 실행 Paparazzi 도구는 AGP 9 호환 수정이 반영된 2.0.0-alpha05/layoutlib 16.2.1로 갱신했고 test Kotlin 컴파일이 성공했다. 실제 스크린샷 생성·기존 산출물 교체는 수행하지 않았다.
- 빌드/테스트: ignored 격리 buildDir에서 `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:bundleRelease`를 최초 116 task 모두 실행해 성공했다. 최종 1.4(5) 재검증도 성공(18 executed/98 up-to-date)했고 `:app:assembleRelease`도 성공했다. `:app:testReleaseUnitTest` task는 이 구성에서 생성되지 않아 실행하지 못했고, 최적화 release는 아래의 별도 실기기 패키지로 검증했다. 최초 AGP 9 dry-run은 기본 Build Tools 36.0.0 미설치로 실패했으며 36.1.0 명시 후 전체 task graph와 R8 task가 정상 구성됐다.
- AAB 확인: 최종 AAB는 31,135,845 bytes, SHA-256 `3B7C4CD629C12F077FF78073D3BA73B4BA24E776CC8129E571CE22157D967DB7`, `jarsigner -verify` exit 0이다. 비최적화 기준 AAB 39,240,879 bytes보다 20.65% 작고, 압축 해제 DEX는 3개/46,858,388 bytes에서 1개/3,394,376 bytes로 92.76% 감소했다. 13개 폰트는 모두 유지됐다.
- R8 증거: AAB의 `BUNDLE-METADATA/com.android.tools/r8.json`은 R8 9.0.32, 난독화 97.88%, 최적화 97.04%, 축소 97.98%, optimized resource shrinking=true를 기록한다. AAB 내부 `proguard.map`과 외부 `mapping.txt` 44,923,761 bytes가 생성됐다. production release APK는 `com.pocket4cut`, versionName 1.4, versionCode 5, minSdk 26, targetSdk 36으로 확인했다.
- Lint: `:app:lintDebug`는 **실패 — Error 1, Warning 91, Hint 2**. 차단 오류는 기존 `AndroidManifest.xml:6`의 `PermissionImpliesUnsupportedChromeOsHardware`이며 이번 최적화 작업에서 suppress하지 않았다. release의 `lintVitalRelease`는 성공했다.
- 실기기 release: Android 16/API 36 기기에 R8·resource shrink를 그대로 둔 별도 applicationId/debug 서명 QA APK를 설치했다. cold start, 홈, 설정 ON/OFF의 실제 `FLAG_KEEP_SCREEN_ON` 반영과 재실행 복원, 2컷 선택, CameraX 권한·전면 카메라 준비, 4장 연속 촬영, 2장 선택, 가로 레이아웃, 커스텀 색/스티커, 필름 필터, 상세 편집 회전·반전·보정 반복, 최종 결과 생성, 공유 chooser, 보관함 생성·재실행 복원을 확인했다. 반복 조작 전후 PID가 유지됐고 새 `AndroidRuntime` crash가 없었다.
- 계측/기존 결함: 별도 debug QA 패키지 `com.pocket4cut.agp9qa`에서 Bitmap 소유권 회귀 계측 2개는 통과했다. 선택 진단 포함 10개 전체 실행은 5 pass/5 fail이었다. 실패 1개는 임시 applicationId suffix와 하드코딩된 packageName 기대의 차이이고, 4개는 이미 문서화된 R02 문구/날짜 preview, R03 스티커 export, R04 카탈로그 색 복원, R08 6컷 배치 중복을 그대로 검출했다. 이 계측은 최적화된 release의 계측 검증이 아니며 이번 범위에서 해당 기능 코드를 바꾸지 않았다.
- 정리/한계: QA package 제거 후 원래 `com.pocket4cut`만 남겼고 QA 촬영·세션 파일도 함께 제거했다. 사용자 사진을 공용 영역에 남기지 않기 위해 MediaStore 저장 버튼은 누르지 않았다. 한 기기의 release 검증이 모든 기기·OS 조합을 보장하지 않으며 Play Console 서버 표시는 새 AAB 업로드·처리 뒤 확인해야 한다.
- Git: 이번 build/toolchain/R8/문서 파일만 명시적으로 stage·검토해 Conventional Commit으로 일반 push한다. Play Console 경고 해소는 이 AAB를 업로드하고 처리가 끝난 뒤 같은 ‘앱 최적화’ 항목에서 최종 확인한다.

## 2026-09-10 — UI 게시 Bitmap 조기 해제 충돌 수정 (Asia/Seoul)

- 요청/범위: 당일 업데이트를 위해 실기기 감사의 최우선 QA-01, `Canvas: trying to use a recycled bitmap` 강제 종료 원인만 수정한다. 다른 P1/P2 결함과 Lint 기존 오류는 이번 범위에서 고치지 않았다.
- 기준/보존: `codex/reels-promo`, 시작 HEAD `5410405`. 시작 전부터 있던 `app/build.gradle.kts`의 1.3(4)→1.4(5) 버전 변경은 사용자 작업으로 보존하고 이번 변경에 포함하지 않았다.
- 원인/수정: `asImageBitmap()`은 별도 복사본을 만들지 않는데 ViewModel이 StateFlow로 게시한 Bitmap을 Compose의 이전 프레임이 놓기 전에 recycle했다. `DetailEditViewModel`의 전체 preview 교체·단일 슬롯 교체·ViewModel 제거와 `EditViewModel`의 ViewModel 제거, 총 4개 수동 recycle 경로를 제거했다. 미게시 중간 산출물·취소 결과·최종 렌더 임시 Bitmap 해제는 유지했다.
- 회귀 테스트: `BitmapOwnershipInstrumentedTest` 2개를 추가해 Detail의 전체/단일 교체 및 두 ViewModel 제거 뒤 보관한 UI 참조가 recycled되지 않고 Canvas에 그려짐을 확인했다. 필터 thumbnail 작업 완료까지 기다려 테스트 자체의 해제 경합도 피했다. 기존 진단의 조기 recycle 테스트 1개도 assertion 변경 없이 통과했다.
- 빌드/단위 검사: 편집기 프로세스가 기본 `app/build`의 `R.jar`를 점유해 해당 출력 경로 빌드는 두 번 실패했고 프로세스를 강제 종료하지 않았다. ignored 격리 buildDir로 `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`를 실행해 69개 task 모두 실제 실행·성공, 기본 단위 테스트 1개 통과. 마지막 테스트 보강 후 androidTest APK 재빌드도 성공했다. sandbox의 기본/cache-only wrapper 재시도 2회는 캐시 쓰기·오프라인 plugin 부재로 실패한 뒤 기존 사용자 Gradle 캐시와 격리 buildDir 조합으로 최종 성공했다.
- Lint: 격리 buildDir의 `:app:lintDebug`는 **실패 — Error 1, Warning 77, Hint 2**. 차단 오류는 기존 `AndroidManifest.xml:6`의 `PermissionImpliesUnsupportedChromeOsHardware`이며 suppress하거나 이번 충돌 수정에 섞지 않았다.
- 실기기 자동/수동 검사: Android 16/API 36 기기에서 신규 계측 2개와 기존 표적 진단 1개가 통과했다. 새 2컷 세션으로 회전·반전 36회, 보정 slider 왕복 36회, 상세 편집 재진입 3회, 일반 편집 이탈 2회를 수행했고 전후 PID가 유지됐으며 새 crash가 없었다. 조작 후 1회 PSS 309,500KiB/RSS 417,060KiB는 최고값이나 장기 누수 검증이 아니다.
- 데이터/환경 정리: 신규 테스트 세션과 임시 test APK를 제거했고, 검사 전후 기존 세션 metadata가 동일함을 확인했다. 앱은 데이터 유지 설치 후 홈에서 실행 중이며 기존 설정의 `keepScreenOn=true`를 유지했다. 설치 APK는 별도 사용자 버전 변경을 포함해 1.4(5)이지만 그 버전 파일은 이번 커밋 대상이 아니다.
- 변경 파일/자산: production ViewModel 2개, 기본 계측 테스트 1개, QA·known issues·진단 README·이 로그를 변경했다. 의존성, Manifest, 사용자 UI, 이미지 에셋은 변경하지 않았다.
- 한계: 한 기기의 반복 실행으로 모든 프레임 경합·OS·메모리 압박에서의 무충돌을 보장할 수 없다. UI 참조가 사라질 때까지 Bitmap 회수가 늦어질 수 있으므로 별도 메모리 최고 사용량 위험은 유지한다. 다른 감사 항목은 후속 작업으로 남긴다.
- Git: 위 충돌 수정 관련 파일만 명시적으로 stage·검토해 Conventional Commit으로 일반 push한다. 기존 버전 변경은 working tree에 남긴다.

## 2026-09-10 — 남은 파일 전체 커밋·푸시 (Asia/Seoul)

- 요청/범위: 현재 저장소에 남아 있는 변경을 모두 커밋하고 GitHub에 push한다. 브랜치 병합이나 main 변경은 요청 범위에 포함하지 않는다.
- 기준: `codex/reels-promo`, HEAD `b9f2254779c99015f0fc8d434fae10df6a934570`. `git fetch origin` 후 현재 브랜치의 원격 대비 ahead/behind는 0/0이었다. 미커밋 항목은 `design/reels/reference-v3/Pocket4Cut-Reference-Reels-Package/`에 풀어 놓은 파일 7개뿐이었다.
- 변경: 압축 해제된 MP4·MP3·커버 JPEG·SRT·대본·게시글·업로드 안내 7개와 이 작업 기록을 추가한다. 기존 `deliverables/`의 동명 파일과 SHA-256이 모두 일치하며 합계 6,874,060 bytes다. 새 이미지 생성이나 앱 코드 변경은 없다.
- 검증: 숨김 파일·하위 폴더·추가 참조 원본·비밀정보 파일 혼입이 없고, 7개 파일의 바이트 동일성을 확인했다. 기존 파일의 동일 사본을 추가하는 작업이므로 Android 빌드·앱 테스트·Lint 및 영상 재렌더는 실행하지 않았다. `.gitignore`로 제외된 캐시·서명 설정은 강제로 추가하지 않는다.
- Git: 명시한 7파일과 이 로그만 검토·stage하고 `chore: track unpacked reel delivery package`로 커밋한다. 일반 push 후 원격 SHA 일치와 미커밋 변경이 없는지 확인해 완료 응답에 보고한다.

## 2026-09-10 — 화면 자동 꺼짐 방지 설정 (Asia/Seoul)

- 요청/범위: 오늘은 설정의 ‘화면 자동 꺼짐 방지’ 옵션 하나만 추가한다. ON은 앱 화면 사용 중 유지, OFF는 휴대폰 설정을 따른다. 기존 감사에서 발견한 다른 결함은 수정하지 않았다.
- 기준/보존: 구현 검토 기준 `bab7ec6`, 작업 브랜치 `codex/reels-promo`. 시작 당시 병행 릴스 변경과 WORKLOG 변경이 있었으며 해당 작업의 별도 커밋·미추적 산출물을 보존했다.
- 변경 파일: `MainActivity.kt`는 앱 전체 window flag의 단일 소유자, `CaptureScreen.kt`는 기존 무조건 화면 유지 제거, `AppSettings.kt`는 기본 false인 `keepScreenOn` 저장·복원, `SettingsScreen.kt`는 맨 위 ‘화면’ 그룹/스위치/접근성 이름 추가. README에 동작을 설명하고 이 로그를 추가했다. 총 6개 파일이다.
- 동작: 설정 즉시 적용, 앱 재실행 후 유지. 촬영 화면에서도 OFF면 기기 설정을 따르며 ON 상태 카메라 이탈로 flag가 해제되지 않는다. WakeLock·추가 권한·의존성·시스템 화면 꺼짐 시간 변경은 없다. 새 이미지 자산도 없으며 기존 Material 아이콘을 사용했다.
- 빌드/테스트: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain` 실행. 최종 APK 빌드 성공, 단위 테스트 1개 실제 실행/통과. Lint는 기존 Manifest의 `PermissionImpliesUnsupportedChromeOsHardware` 오류로 실패(Error 1/Warning 91/Hint 2). 이번 추가 코드에서 발생한 경고 2개는 해소 후 재검사했으며 기존 오류를 suppress하지 않았다. 최종 결합 명령은 exit 1, 16개 task 실행·36개 캐시 재사용이다.
- 실기기: Android16/API36 연결 기기에 최종 APK를 데이터 유지 설치했다. 휴대폰 제한 15초·충전 중 화면 유지 OFF 상태에서 옵션 ON의 설정/카메라 대기 화면은 25초 무입력 후에도 Awake, 앱을 벗어나거나 설정 OFF에서는 25초 후 Dozing이었다. 카메라 진입→이탈의 ON 유지, 설치/재실행 후 ON/OFF 값 보존과 스위치 이름·체크 상태를 확인했다.
- OFF 카메라 대기 화면도 25초 무입력 후 Dozing으로 전환되어 기존 강제 화면 유지와의 충돌이 해소됐음을 확인했다. 검사에서 새 사진은 촬영하지 않았다.
- 한계: 기본 산술 단위 테스트 통과를 기능 테스트로 대신하지 않았다. 다른 기종·멀티윈도우·모든 화면 조합은 별도 검사하지 않았으며 기존 촬영 중단/Bitmap 충돌 등의 수정은 이번 범위 밖이다. 실제 사진·기기 식별자·원시 로그는 커밋하지 않는다.
- Git: 이번 6개 파일의 변경만 검토·커밋·일반 push한다. 공유 WORKLOG는 이번 절만 패치로 stage하며, 최종 SHA와 원격 일치는 완료 응답에서 보고한다.

## 2026-09-10 — 연결 Android 실기기 기능·충돌 감사 (Asia/Seoul)

- 요청/범위: 실제 기기에 debug 빌드를 설치하고 촬영·편집·저장·보관함·중단 상황을 검사, 추가 제공된 RuntimeException 스택 2개를 포함해 문제를 정리했다. 앱 기능 수정은 포함하지 않는다.
- 기준: 시작 `codex/reels-promo`, HEAD `f79b2fd389fdb56a0a39b21ca5223f5f0bc0979b`, 당시 기존 변경 없음. 병행 릴스 작업의 커밋과 미커밋 파일은 보존했고 검사 중 앱 소스 변경은 없었다.
- 변경 파일: `engineering/DEVICE_QA_2026-09-10.md`의 29개 항목·재현 단계·근거·한계, `engineering/device-diagnostics/RenderContractDiagnosticTest.kt`와 README, `scripts/device-diagnostics.init.gradle`, 이 로그. 진단 소스는 init script를 명시한 경우에만 androidTest에 추가된다. 의존성·Manifest·이미지 자산은 바꾸지 않았다.
- 실제 기능 검사: Android 16/API36 기기에서 2/4/6컷 촬영과 저장, 전후면·줌, 선택/재정렬, 색/계절/커스텀, 필터·문구·날짜·상세 보정, 공유 chooser, 보관함 종류·테스트 결과 삭제, 설정·권한·큰 글자·회전·백그라운드·프로세스 복원을 실행했다. 수신자 전송과 기존 사용자 자료 삭제는 하지 않았다.
- 주요 결과: 기존 recycled Bitmap 충돌 로그와 같은 계열의 수명 결함, 백그라운드 촬영 실패, 사진 순서 인계 누락, 편집 프로세스 복원 손실, preview/export 불일치, 중복 자동 저장, 삭제 후 JSON 잔존을 확인했다. 실제 UI 재현/계측/정적 코드 확인을 구분했다. 이번 수동 검사에서 새 FATAL은 관찰되지 않았다.
- 검증: `:app:assembleDebug` 성공 및 기기 설치·실행, 최종 기준 빌드는 UP-TO-DATE. `:app:testDebugUnitTest --rerun` 실제 실행 1개 통과. 기본 `ExampleInstrumentedTest` 1개 통과. `:app:lintDebug`는 기존 `PermissionImpliesUnsupportedChromeOsHardware`로 실패(Error 1/Warning 91/Hint 2).
- 진단: `-I scripts/device-diagnostics.init.gradle :app:assembleDebugAndroidTest --offline --console=plain` 성공. 실제 기기에서 `RenderContractDiagnosticTest` 7개 실행·2개 통과·5개 실패. 32개 렌더 조합과 역순 Renderer 입력은 통과, Bitmap 수명·24문구 조건·4색 ID·6컷 기하 중복·하트 스티커 출력은 실패. 재실행 명령과 해상도 범위는 진단 README에 기록했다.
- 보존/복원: 기존 세션 metadata 일치와 기존 선택 사진·결과·미완성 초안 존재 확인. 화면 제한·글자 크기·회전·카메라 권한·앱 타이머/테마/자동 저장을 원래 값으로 복원했다. 테스트 APK는 제거했으며 신규 테스트 결과 2개와 일부 테스트 촬영/사진첩 사본은 기기에 남는다. 실제 사진·식별자·원시 로그는 ignored 로컬 영역에만 보관한다.
- 미검증: 다른 기종/API26–35, 실제 저장공간 고갈·저메모리/전화/열 압박, 모든 장식·글꼴 조합, TalkBack 전체 완주, release/Play 배포. 한 기기 검사와 전체 경우의 수 전수 검증을 혼동하지 않는다.
- Git: 위 5개 파일만 검토·stage·Conventional Commit 대상으로 삼는다. 병행 변경을 포함하지 않는다. 최종 SHA와 일반 push 결과는 완료 응답과 Git 이력에 기록한다.

## 2026-09-10 — 릴스 v3 상·하단 설명 문구 제거 (Asia/Seoul)

- 요청/범위: 본편과 커버의 `광고 · 사용 예시`, `AI 내레이션 · 생성 예시` 오버레이를 제거했다. 앱 소개 자막·음성·대본·컷 전환·브랜드는 유지한다.
- 기준: `codex/reels-promo`, HEAD `4fac7a2cb52559eebf8b59ebd88288a888d8d6b0`. 기존 별도 기기 진단 파일과 병행 WORKLOG 변경은 보존하고 이번 커밋에 섞지 않는다.
- 변경: `reference-v3/source/render-reference.cjs`의 공통 표시 함수와 호출 제거, 본편 MP4·커버 JPEG·28개 시점 스틸·14장면 검수 이미지·ZIP 재출력. 관련 README/업로드 안내/편집 설명/자산 가이드에서 표시 유지 안내를 갱신했다. 제작 도구와 원본 재료에 관한 사실 기록은 지우지 않았다.
- 검증: `node --check`, render/export/package 스크립트 실행 성공. 23초/1080×1920/30 fps/690프레임 유지, 전체 영상·음성 디코딩/동기/faststart 통과, 검은 구간 0, AAC −16.0 LUFS/−3.4 dBTP. 28시점·핵심 텍스트 경계 41개 검사, 최종 추출 14장면·시작·끝·커버 시각 검수에서 요청 문구 제거 확인.
- 보존: WAV 2개·MP3·소개 자막 SRT·새 대본 TXT·대본 JSON·타이밍 JSON 총 7파일과 MP4 내 AAC 스트림 SHA-256이 수정 전과 정확히 일치했다. 앱·기존 v1/v2·음성 생성·전사 결과는 변경하지 않았다.
- 납품: MP4 5,909,714 bytes, SHA-256 `6e5df09d817b513d6f321893b22488a5018768a15b3cfc91a37187fd0ab0273c`. ZIP 6,715,952 bytes, 7개 항목 모두 원본 해시 일치. 커버는 1080×1920 JPEG이며 새 이미지 생성은 없다.
- 미실행/한계: 자산 표시 제거 작업이므로 Android 빌드/앱 테스트/Lint는 재실행하지 않았다. 음성은 동일 비트 보존을 검증했으며 새로운 청취·Instagram 업로드 검증은 하지 않았다. 이전 문구 포함본은 이전 Git 커밋에서 복구할 수 있다.
- Git: 병행 기기 진단은 먼저 별도 커밋 `06a1903`으로 완료됐다. 그 뒤 이번 변경과 이 로그의 해당 절만 변경분에 포함해 `fix: remove reel overlay labels`로 별도 커밋하고 일반 push한다. SHA/원격 결과는 완료 응답에서 보고한다.

## 2026-09-10 — 참조 음색과 자체 존댓말 대본의 릴스 v3 (Asia/Seoul)

### 요청과 변경 범위

- 요청: 사용자 제공 영상의 AI 음색을 참조하고, 원본의 이야기·반말을 따르지 않는 포켓네컷 자체 앱 소개 광고를 제작. 음악을 넣지 않고 참조 계정/출처 크레딧을 광고에 삽입하지 않는다. 추출 음성과 새 대본의 Qwen/Hugging Face 전송은 사용자에게 별도 명시 승인을 받았다.
- 기준: `codex/reels-promo`, HEAD `f79b2fd389fdb56a0a39b21ca5223f5f0bc0979b`, 시작 시 미커밋/스테이징 변경 없음. 신규 `design/reels/reference-v3/`, 이 로그와 자산 가이드만 작업 범위다. 기존 v1/v2, 앱 소스·Manifest·Gradle·아이콘·원본 이미지와 사용자 제공 영상은 보존한다.
- 대본: “내 폰이 네 컷 사진관이 된다면요?” → 앱 이름 → 4컷/8장 촬영 → 4장 선택 → 배치/필터/흑백 → 저장/공유 → 사용할 상황 → 브랜드 검색. 친근한 존댓말 12문장으로 구성하고 실제 후기를 가장하지 않는다.
- 화면: 기존 Compose 호스트 렌더, CollageRenderer 결과, Film Strip v4 아이콘, 가상 성인 생성 사진을 재사용한다. 새 사진·아이콘을 생성하거나 앱 기능을 변경하지 않는다. 광고·AI 사용 예시 표시는 출처 크레딧과 별개로 유지한다.

### 음성 경로와 보존 원칙

- 공식 공개 ASR/TTS는 승인된 각 1회 요청에서 상세 원인 없는 `error:null`을 반환했다. 원인을 할당량·계정 문제로 단정하거나 성공으로 처리하지 않았다. 동일 요청 반복이나 다른 기본 음색으로 대체하지 않고 로컬 ASR·참조 조건 음성 생성으로 전환했다.
- 공식 whisper.cpp b4938와 다국어 small-q5_1로 참조 음성을 로컬 전사했다. 6초 참조의 철자 일부는 영상 자막과 대조했다. 사용자 원본 영상, 참조 클립, 원본 전체 전사는 ignored `build/`에만 보관하며 Git·납품 ZIP에 포함하지 않는다. 실제로 듣고 참조 음색을 검수했다는 주장은 하지 않는다.
- 공식 Qwen3-TTS 1.7B Base와 토크나이저를 pinned revision으로 내려받아 두 대형 가중치의 공개 SHA-256 일치를 확인했다. 모델과 Python 패키지는 ignored `build/`의 독립 환경에 설치했다. 앱/공유 런타임/시스템 PATH를 변경하지 않으며 계정 자격증명·유료 API를 사용하지 않는다.

### 검증 기록

- 타이밍 도구의 합성 fixture 9개 테스트와 PowerShell 패키지 스크립트 구문 검사를 통과했다. 이는 실제 새 음성의 대본 일치 또는 음색 평가가 아니다.
- JavaScript 7개 `node --check`, Python 생성 스크립트 문법 검사 통과. 공개 자료에서 개인 원본 파일명·참조 계정 URL·자격증명 패턴을 검색해 발견하지 못했다.
- 실제 새 음성을 로컬 CPU에서 1회 생성했다. 20.160초/24 kHz/mono/float32 WAV, 생성 209.125초, 전체 220.281초. 모델에 실제 6초 참조와 참조 대본을 함께 전달했으며 배속·발화 삭제·음악·효과음 추가는 없다.
- 로컬 Whisper ASR + DTW는 정답 프롬프트 없이 성공했다. 숫자·공백·구두점 정규화 후 136문자가 새 대본과 일치했다(숫자를 한글로 풀면 137음절). 원문과 토큰 시각은 검증 자료에 보존했다. 마지막 ASR 끝 시각이 원본보다 20 ms 늦은 한계도 기록했다.
- `master-narration.cjs`, `time-captions.cjs`, `render-reference.cjs`, `export-reference.cjs` 실제 실행. 새 음성에 0.12초 시작 여유와 후반 검색 여유를 더해 23초/48 kHz/stereo PCM24로 정리하고 14개 연속 편집 구간을 생성했다. 초안 스틸은 ignored 영역으로 이동해 보존하고 최종 구간 스틸과 혼합하지 않았다.
- 인코딩 결과는 1080×1920, 30 fps, 690프레임, 23초, H.264 High/yuv420p/BT.709, AAC stereo 48 kHz. 전체 디코딩·길이 동기·faststart 검사 통과, 검은 구간 0, 최종 AAC −16.0 LUFS / −3.4 dBTP. 음성 원본·ASR·마스터·story·visual의 해시를 연결해 이전 출력의 혼입/잘림을 차단했다.
- 28개 장면 시점과 핵심 텍스트 경계 70개 검사. 최종 MP4에서 추출한 14장면 모아보기/시작/끝과 커버를 실제 시각 검토했다. 흑백 결과가 저장/공유까지 이어지는지 확인했고, CTA 상단 광고 표기와 필름 인쇄 문자의 겹침은 헤더 배경을 정리한 뒤 재출력했다. 자동 보고서의 프레임 추출과 실제 시각 검수도 구분했다.
- 앱 변경이 없어 Android 빌드·단위 테스트·Lint·기기 E2E는 재실행하지 않았다. 기존 앱 문제의 해결이나 Play/Instagram 실제 배포를 주장하지 않는다.

### 산출물과 남은 확인

- 납품: 23초 본편 MP4, 동일 음성 MP3, 1080×1920 JPEG 커버, 화면 자막 SRT, 새 대본, 게시글, 업로드 안내와 ZIP. 원본 새 음성, 제작 스크립트, 출처/라이선스·한계 문서, 해시/검증 자료는 별도 제작 폴더에 보존한다. 참조 원본은 포함하지 않는다.
- `package-reference.ps1` 실행 완료: ZIP 6,834,141 bytes, 7개 항목 모두 원본 SHA-256 일치. 최종 MP4 SHA-256은 `21d42e7eb026b001d77572cf345329ce369d5fa6b2ec615a89cb7595622bf2f4`다.
- 직접 청취와 독립 음색 비교는 수행하지 못했다. 참조 조건 적용과 자동 전사 일치는 원본 음색의 완전한 동일성·자연스러움 보증이 아니다. 휴대전화 청취와 실제 Instagram UI/커버 자르기, 스토어 검색/설치 가능 여부는 게시 전 확인해야 한다.
- 완료 커밋은 `feat: add reference-voice Pocket4Cut reel`로 식별한다. SHA를 문서 자체에 넣어 amend하지 않으며, 일반 push 이후 원격 SHA/작업 트리 상태를 최종 응답에서 보고한다.

## 2026-09-10 — 음악 없는 남성 나레이션 릴스 추가 (Asia/Seoul)

### 요청과 변경 범위

- 요청: 기존 영상과 별개로 “저 ~했어요” 계열의 남성 나레이션을 넣고, 음악을 빼고, 최근 릴스 광고 표현을 조사해 한 편 더 제작.
- 기준: 시작/작업 브랜치 `codex/reels-promo`, HEAD `c33b83a68a192a8aab8dde4f90b78c2e3357146e`, 기존 미커밋 변경 없음. `design/reels/narrator-v2/` 및 이 로그/자산 가이드만 변경했다. 기존 `film-story-v1`과 앱 소스·Manifest·Gradle·아이콘은 보존했다.
- 조사: 곰랩 2026-03-03의 국내 남성 AI 일상 나레이션 설명, Later 2026-09-04 및 New Engen 본문 2026-09-01의 상황 설정/반전/1인칭 표현, Meta Blueprint 제작 기본기를 구분해 참고했다. 한국 전체의 실시간 유행 순위나 전환 성과를 확인했다는 주장은 하지 않는다.
- 제작: “저, 네 컷 만들었어요. 근데 사진관은 안 갔어요.”를 도입으로, 실제 음성에 맞춘 15개 편집 구간에 결과·사용법·흑백 감상·저장/공유·친구와의 추억·검색 안내를 배치했다. 기존 앱 코드 기반 렌더와 가상 성인 생성 사진을 재사용했다. 실제 사용자 후기나 실기기 녹화가 아니며 영상/커버/게시글에 광고·생성 예시 고지를 넣었다.
- 음성: 전용 음성 제작 수단을 조사한 뒤 Qwen 공식 공개 VoiceDesign 데모로 참조 음성 없는 원본 한국어 남성 음색을 지정해 생성했다. 짧은 시험 1회, 본편 1회만 요청했으며 별도 유료 API/자격증명/플러그인 설치/음성 복제는 사용하지 않았다. 음악과 효과음은 없다.
- 산출물: 30초 세로 MP4, 30초 음성 MP3, 1080×1920 JPEG 커버, SRT, 대본, 게시글, 업로드 안내 및 ZIP. 원본 WAV, 음색 지시, 재출력 코드, 공식 생성/전사 출처와 검증 자료도 별도 폴더에 보존했다.

### 실제 실행한 검증과 보완

- 공식 Qwen3-ASR/ForcedAligner로 생성 원본을 전사했다. 공백/문장부호를 제외한 대본의 119개 한글 음절이 모두 일치했고 음성 구간 13개를 추출했다. 원본 25.953750초를 배속 없이 처리하고 시작 0.15초 여유/끝 무음을 더했다. 발화 시각에 맞춰 화면 자막을 구성했다.
- `node --check` JavaScript 5개 통과, PowerShell Parser 1개 오류 0. `master-narration.cjs`, `time-captions.cjs`, `render-narrator.cjs`, `export-narrator.cjs` 실행 완료. 공개 음성 생성 스크립트도 본편 생성에 실제 사용했다.
- 독립 검토에서 흑백 다음 저장 화면이 컬러로 되돌아가는 연속성 문제와 커버의 생성 예시 고지 누락을 발견해 수정하고 재출력했다. “흑백” 대사에는 흑백 결과를, 이어지는 저장/공유에도 같은 결과를 유지한다. 기존 필터 UI의 원본 선택 상태가 흑백 결과와 함께 보이지 않도록 편집했다.
- 최종 영상 1080×1920, 30 fps, 900프레임, 정확히 30초, H.264 High/yuv420p/BT.709, AAC stereo 48 kHz 확인. 전체 영상/음성 디코딩 오류 0, 길이 동기/faststart 통과, 예상치 못한 검은 구간 0. 최종 AAC −15.8 LUFS / −2.7 dBTP로 측정했다.
- 30개 시점의 장면 스틸과 핵심 텍스트 경계 123개 검사. 최종 MP4에서 15장면/첫 프레임/끝 프레임을 추출했고, 최종 장면 모아보기·흑백 저장 결과·커버를 시각 검토했다. 원본 자산 경로/치수/SHA-256을 기록했다.
- `package-narrator.ps1`: 납품 파일 7개, ZIP 7,298,978 bytes. 모든 ZIP 항목의 SHA-256이 원본과 일치했다. MP4 SHA-256은 `360731d52fa0f7adc6b7c968c7a82a8ee461d3d5f4410fcc7a96f8e2fbd8bcfd`다.
- 앱 변경이 없어 Gradle 빌드/앱 단위 테스트/Lint는 재실행하지 않았다. 기존 앱 문제나 이전 Lint 결과를 해결했다고 주장하지 않는다.

### 남은 확인

- 파일/자동 전사 검증과 주관적 음성 청취는 다르다. 이번 환경에서는 음색·억양·자연스러움을 실제로 듣고 평가하지 못했다. 제공한 MP4/MP3를 휴대전화에서 재생해 확인해야 한다.
- Instagram의 실제 UI 겹침/커버 자르기, 스토어 검색·설치 가능 여부와 최신 배포 UI 일치는 게시 전에 확인해야 한다. 계정 게시·광고 집행·심사 확인은 수행하지 않았다. 생성 음성의 모델 라이선스를 생성물 독점권이나 자동 광고 권리 보증으로 확대하지 않는다.

## 2026-09-10 — 30초 Instagram 릴스 홍보 영상 (Asia/Seoul)

### 요청과 변경 범위

- 요청: 현재 릴스 트렌드를 조사하고 Pocket4Cut의 사용법과 장점을 소개하는 약 30초 홍보 영상을 제작.
- 기준: 시작 `main`, HEAD `4527a05`, 미커밋/스테이징 변경 없음. `codex/reels-promo`에서 별도 홍보 자산을 작성했다. 앱 1.3 (4)의 소스·Manifest·Gradle·런처 리소스는 변경하지 않았다.
- 조사: 2026-09-04 Later 트렌드, 2026-09-01 New Engen, Meta Blueprint의 세로 영상/초반 관심 유도/소리/안전 영역 안내를 확인했다. 한국 전체의 실시간 인기 순위나 광고 전환 보장으로 해석하지 않는다. 참고 링크와 편집 판단은 제작물의 `source/CREATIVE-BRIEF.md`에 기록했다.
- 제작: “네 컷 찍으러 어디 가? → 내 폰이 네 컷 부스.” 콘셉트로 결과 → 4컷/8장 촬영 안내 → 4장 선택 → 레이아웃/색/필터 → 저장/공유 → 검색 CTA의 8장면을 구성했다. 실제 Compose 호스트 렌더·CollageRenderer 결과·현행 Film Strip v4 아이콘과 기존 가상 성인 생성 사진을 재사용했다. 실기기 녹화 또는 고객 후기라고 표현하지 않는다.
- 산출물: `design/reels/film-story-v1/`에 음악 포함 MP4, 효과음 전용 MP4, 1080×1920 JPEG 커버, 게시글, SRT, 업로드 안내, 원본 WAV 3개, 제작/검증 코드와 기록. 앱 기능이 아닌 별도 마케팅 자산이다. 이 섹션과 `IMAGE_ASSET_GUIDE.md`에 새 자산을 문서화했다.
- 현재 사용 가능한 전용 영상 생성 도구가 없어 native Canvas/FFmpeg로 시간축 모션 편집했다. 외부 곡·음성·샘플 없이 120 BPM의 음악과 셔터/종이 효과음을 절차적으로 제작했다. 플러그인 설치·전역 PATH 수정·모델 전환·계정 게시·광고 집행은 하지 않았다.

### 실행한 검증과 발견/보완

- 오디오/렌더/내보내기 JavaScript 구문 검사 및 실행. WAV 3개는 48 kHz stereo PCM16, 30.000초, 믹스 sample peak −3.100 dBFS, clipping 0. MP4 AAC 변환 후 완성본 −15.3 LUFS / −2.1 dBTP, 효과음 전용본 −42.0 LUFS / −20.6 dBTP를 측정했다.
- `render-reel.cjs`: 1080×1920, 30 fps, 900프레임 출력. 핵심 한글 자막의 사용자 정의 여백 검사. 최초 시안의 글자/사진 겹침과 색 선택 crop을 수정한 뒤 8장면/커버를 재검토했다.
- 독립 read-only 검토에서 native `canvas.data()` 버퍼 공유를 재현했다. 실제 인코더 write가 큐에 남았을 때의 위험을 없애도록 `Buffer.from()` 소유 복사 및 backpressure 대기를 적용했다. 장면 모아보기는 live Canvas 참조 대신 immutable 이미지 스냅샷을 쓴다.
- 최초 MP4의 BT.709 transfer/primaries 태그 누락을 자동 검사에서 발견했다. 검사 조건을 낮추지 않고 scale 뒤 `setparams`를 적용해 재출력했다. 최종 RGB 디코딩 배경 `[200, 59, 44]`는 원본 `[200, 61, 45]` 대비 허용 오차 안이다. JPEG 검증 스냅샷은 BT.709→BT.601 변환으로 일반 JPEG 뷰어의 색 해석에 맞췄다.
- `export-reel.cjs`: 두 MP4 각각 H.264 High/yuv420p/BT.709, 1080×1920, 900프레임, 30 fps, 영상/오디오/컨테이너 30초, AAC stereo 48 kHz 확인. faststart 통과, 전체 영상·오디오 디코딩 오류 0, 예상치 못한 검은 구간 0. 최종 인코딩 8장면/첫 프레임/마지막 프레임도 추출하여 검토했다.
- `package-reel.ps1`: 정확한 납품 파일 6개를 8,357,126 bytes ZIP에 포함했다. 각 항목을 압축 해제 없이 읽어 원본 SHA-256과 비교해 6개 모두 일치했다. PowerShell Parser 구문 오류 0, JavaScript 3개 구문 검사 통과, 변경 문서의 `git diff --check` 통과.
- 앱을 변경하지 않아 Gradle 빌드/앱 단위 테스트/Lint는 이번에 재실행하지 않았다. 이전 `PermissionImpliesUnsupportedChromeOsHardware` Lint 실패와 기존 제품 문제는 해결 범위가 아니다.

### 남은 확인

- 실제 휴대전화 스피커 청취·연속 재생, Instagram의 우측/하단 UI와 커버 썸네일 자르기, 실제 스토어 노출과 배포 화면 일치를 게시 전에 확인해야 한다. 수치 검증을 청취·실기기 촬영·업로드 심사 통과로 과장하지 않는다.
- 생성 예시 고지를 유지한다. 유행 음악으로 교체하면 실제 브랜드 홍보/광고 사용 범위의 권리를 별도로 확인해야 한다. 음악의 독자 제작은 자동 권리 판정/법적 독점성의 보증이 아니다.
- 앱에서 미해결로 기록된 재정렬/스티커 출력/캡션 위치 등은 홍보 기능에서 제외했다. 프레임 색과 필터 예시는 조합된 스타일링이며 단일 변수 A/B 테스트가 아니다.

## 2026-09-10 — 미커밋 전체 작업 통합 및 main 반영 (Asia/Seoul)

### 요청과 반영 범위

- 요청: GitHub에 반영하지 않은 작업 전부의 내용을 정리하여 커밋하고 `main`으로 push. 사용자가 기존 미커밋 작업 전체의 포함을 명시적으로 승인했다.
- 기준: 시작 브랜치 `codex/setup-project-guidance`, HEAD `317b711`; fetch 후 `origin/main`은 `95140a0`이며 기존 개발 지침·Java 안내 커밋 2개가 앞서 있었다. 사용자 이력을 재작성하지 않는 일반 커밋과 fast-forward 통합을 사용한다.
- 앱 버전은 현재 실제 설정인 **1.3 (versionCode 4)**를 반영한다. 초기 조사와 이전 출시 초안의 1.2 표기는 역사적 기록으로 구분했다.
- UI: Compose 화면 16개·디자인 시스템 10개를 종이색/잉크색, 계절 강조색, 간격·글꼴·작은 모서리로 정리했다. 홈·컷 수·프레임 선택·배치·보관함 구성을 갱신하고 결과 저장/공유를 가로 배치했다. 시작 지연을 1.15초로 줄이고 결과 액션의 지연·confetti를 제거한 기존 로컬 작업을 포함한다.
- 브랜드/스토어: Film Strip v4 런처 PNG·adaptive/monochrome XML·브랜드 PNG/SVG, Play 아이콘 **512×512 RGBA**, 피처 그래픽 **1024×500 RGB**, 기존 휴대전화 소개 이미지 **1080×1920 8장**을 포함한다. `design/`의 최종 원본, 프롬프트, 마스크 검토, 제작 스크립트, 가상 성인 사진 fixture, 미채택 초안·이전 아이콘 백업·ZIP도 제작 이력으로 함께 보존한다. 최종본과 미채택 자료는 각 폴더 README로 구분한다. 이번 Git 통합에서 새 이미지를 생성하지 않았다.
- 문서: 프로젝트 인수 분석과 파일 지도·알려진 문제·검증 기록, UI/자산 작업 로그, 이미지 가이드, 기존 출시 준비 초안 및 실제 아키텍처의 현재 반영 상태를 포함한다.
- 검증 도구: ADB smoke 스크립트와 명시적으로 실행할 때만 사용하는 Paparazzi init script/fixture를 포함한다. 공개 전에 ADB 스크립트의 개인 기기 serial 기본값과 로컬 경로를 제거하고, 세션 JSON·사진 파일 목록 출력은 개수/읽기 가능 여부로 바꿨다. 검증 문서의 개인 설치 경로는 환경 변수 기반 재실행 예시로 일반화했다.
- `.gitignore`에 Kotlin 컴파일러의 로컬 세션 캐시 `/.kotlin/`을 추가했다. 기존 규칙으로 제외된 서명키·서명 암호 파일·SDK 로컬 설정·빌드 캐시는 포함하지 않는다. 자격증명 패턴 검사에서 발견한 Gradle 항목은 속성 조회 코드였고 실제 비밀값은 아니었다.

### 실행한 검증

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --continue --offline --console=plain
```

- 저장된 사용자 JBR 21.0.10 및 기존 Gradle 캐시로 실행했다. 전체 52개 태스크 중 **17개 실행 / 35개 UP-TO-DATE**, 소요 2분 12초. 결합 명령의 종료 코드는 Lint 실패로 **1**이다.
- `assembleDebug`: **성공**. 리소스 처리와 APK 패키징을 실행했으며 출력 metadata의 버전 **1.3 (4)**가 현재 설정과 일치한다. Kotlin 앱 컴파일 등은 캐시를 재사용했다.
- `testDebugUnitTest`: **실제 실행 성공**, 1개 / 실패 0 / 오류 0 / 건너뜀 0. XML 시각은 `2026-09-09T16:46:08.589Z`. 기본 산술 테스트이므로 제품 기능 커버리지를 의미하지 않는다.
- `lintDebug`: **실패**, Error 1 / Warning 91 / Hint 2. 기존 `AndroidManifest.xml:6`의 `PermissionImpliesUnsupportedChromeOsHardware`가 원인이다. CAMERA 권한에 대응하는 hardware feature 선언 문제이며, 이번 통합에서는 Manifest 변경이나 오류 억제를 하지 않았다.
- JavaScript 제작 도구 6개는 `node --check` 구문 검사 통과. ADB smoke 스크립트는 PowerShell Parser 구문 오류 0개. 실제 ADB·촬영·앱 설치·기기 데이터 변경이나 새 Paparazzi 렌더는 실행하지 않았다.
- `design/`은 203개 파일 / 54,869,311 bytes이며 100 MB 초과 파일이 없다. 변경된 파일 목록과 staged diff를 검토하고 Git에서 추적할 대상만 정확한 경로 목록으로 stage한다.
- staged 공백 검사에서 출시 초안의 Markdown 줄바꿈은 동일한 의미의 명시적 줄바꿈으로 정리했다. 보관된 이전 브랜드 SVG의 마지막 공백/빈 줄은 기존 원본 보존을 위해 유지했다. 전체 `git diff --cached --check`는 이 백업 파일 한 개 때문에 종료 코드 2이며, 해당 파일을 제외한 나머지 변경은 공백 검사를 통과했다. 디자인 ZIP 3개의 44개 항목은 대응 원본과 SHA-256이 일치한다.

### 남은 확인과 배포 경계

- 기존 선택 순서 전달·캡션 위치·초안 저장·복원 문제는 [알려진 문제](../engineering/KNOWN_ISSUES.md)에 남아 있다. 이번 UI/자산 통합을 이 결함들의 해결로 간주하지 않는다.
- 실제 기기의 카메라/저장/공유, 다양한 화면·접근성·런처 마스크는 추가 확인 대상이다. 좌표 기반 ADB smoke는 ADB 실패·시간초과·결과 미생성을 모두 실패로 판정하지 않으므로 성공 표시만으로 제품 E2E 통과를 주장할 수 없다. Paparazzi export도 전체 화면 수 검증을 보강할 여지가 있다.
- `main`의 `docs/**` 변경은 저장소의 GitHub Pages 워크플로 대상이다. Git push 성공, Pages 배포 성공, Play Store 게시/심사는 별개이며 스토어 업로드는 이 작업에 포함하지 않는다. 최종 커밋 SHA·원격 반영 결과는 완료 보고와 Git 이력에서 확인한다.

## 2026-09-10 — Windows JAVA_HOME 오류 해결 (Asia/Seoul)

- 요청: 터미널의 `JAVA_HOME is not set` 및 Java PATH 미발견 오류 해결.
- 기준: `codex/setup-project-guidance`, HEAD `ac42903` 및 기존 미커밋 UI·버전·이미지·문서 변경이 있는 작업 트리. 기존 변경은 보존하고 이번 안내와 로그 추가분만 커밋 대상으로 구분했다.
- 확인: Android Studio 번들 JBR 21.0.10의 `java`와 `javac`는 직접 실행 가능했지만, 기존 터미널 환경에서 Java를 찾지 못했다. IDE의 로컬 Gradle JDK 지정과 wrapper의 `JAVA_HOME`/`PATH` 조회가 별개임을 확인했다.
- 조치: 이 PC의 사용자 환경 변수 `JAVA_HOME`을 확인된 JBR로 영구 등록하고, 기존 사용자 `Path` 항목을 보존하며 JDK `bin`을 추가했다. Windows에 환경 변경 알림을 보냈다. 앱 소스·Gradle 설정·이미지 에셋은 수정하지 않았다.
- 변경 파일: `QUICK_START.md`에 설정·터미널 재시작·현재 PowerShell 갱신 방법을 추가하고, `docs/WORKLOG.md`에 이번 기록을 추가했다. OS 환경 변수 자체는 Git 커밋에 포함되지 않는다.
- 검증: 저장된 사용자 환경 변수를 별도 PowerShell 실행에서 다시 읽고, `PATH`의 JDK 항목이 한 개이며 `java`가 해당 JDK로 해석되는 것을 확인했다. `java -version`과 `javac -version`은 21.0.10, `./gradlew.bat --version --console=plain`은 Gradle 8.13 / Launcher JVM 21.0.10으로 성공했다.
- `./gradlew.bat :app:assembleDebug --offline --console=plain`: **성공**, 36개 태스크 중 8개 실행 / 28개 UP-TO-DATE. 기존 사용자 Gradle 캐시를 이번 검증 프로세스에서 지정해 사용했다. 완전한 재컴파일이나 새 기능 테스트로 간주하지 않는다.
- 범위/잔여 확인: Java 실행 환경 수정이므로 단위 테스트·Lint·실기기 검사는 재실행하지 않았다. 이전 CAMERA Lint 오류를 수정한 작업이 아니다. 이미 실행 중인 터미널/IDE는 재시작하거나 문서의 PowerShell 갱신 명령을 적용해야 한다.

## 2026-09-10 — Film Strip v4 아이콘 적용·피처 그래픽 제작 (Asia/Seoul)

- 기준: `codex/setup-project-guidance`, HEAD `ac42903`과 기존 미커밋 UI/버전 변경을 포함한 작업 트리. 사용자는 앞선 웹 조사 방향의 이미지 제작·새 아이콘 적용·Play 아이콘/그래픽 파일을 요청했고, 작업 중 인화지 하단에 소문자 `pocket4cut` 추가를 요청했다.
- 내장 image_gen으로 가상의 성인 두 사람이 담긴 네 컷 인화지, 좁은 필름 가장자리와 빨간 표식을 제작했다. 최초 투명 요청은 RGB 체크무늬로 출력되어 검사에서 제외하고 도구로 차콜 배경으로 수정했다. 아이콘과 피처 그래픽 모두 후속 도구 편집으로 하단 `pocket4cut`을 넣었다. 프롬프트와 미채택 원본은 `design/branding/film-strip-v4/source/`에 구분 보관했다. 사용자 사진·타사 이미지 파일을 가져오지 않았다.
- 실제 적용: adaptive 일반/원형 XML의 foreground를 새 density PNG로 전환, 배경차콜, 호환 전경alias, 단색벡터, density PNG15개, 브랜드PNG/SVG wrapper 갱신. 사진 표현을 위한 래스터 전환이며 앱 UI/Manifest/Gradle/촬영·콜라주 코드는 수정하지 않았다. 이전 v1 파일21개는 별도 backup으로 보존했다.
- 스토어 등록 출력: `design/play-store/film-strip-v4/`의 512px RGBA8 아이콘344,668B와 1024×500 RGB8 피처그래픽793,055B. 모두sRGB ICC 및8bit, 아이콘픽셀알파255/피처알파없음. 보관용master, 한국어대체텍스트, 업로드안내, 전체프롬프트도 제공한다. 등록 가이드에는 내장 AI 생성/편집 사실과 현재 Play의 자산별 신고 안내를 포함했다.
- `node design/branding/film-strip-v4/export-assets.cjs`: 출력23개 생성/검사 성공. 처음 시스템Node에서sharp탐색실패는 제공된 런타임 패키지경로 설정으로 해결했다. 마스크검토판 렌더링의 fontconfig 캐시 쓰기 경고는 있었으나 출력 생성과 글자 표시를 직접 확인했다. 등록문구는 생성 이미지에 포함되어 별도 시스템폰트 렌더가 아니다.
- 시각검사: 각 인화지4프레임, 하단표기철자, 체크무늬제거, 사진/문구잘림없음 확인. 원형·사각·둥근사각·squircle 및48/72/96/144px 축소 검토. 작은크기에서는얼굴·글씨·천공세부식별이제한된다. 독립 검토에서도 중요 수정 문제는 발견하지 못했다.
- 컬러전경은 차콜backing을 포함한 불투명RGBA다. 대비>20 픽셀의 최대반경31.2824dp<33dp를 보조검사했고, 낮은대비의 실루엣을포함하는 완전한알파경계검사로 주장하지 않는다. 단색벡터는 SVG/XML path일치, 사진창4개·천공6개투명, 외곽반경30.017dp 확인.
- `./gradlew.bat :app:assembleDebug --offline --console=plain`: 성공, 36태스크 중10실행/26UP-TO-DATE. 리소스컴파일과packageDebug를 실행해 새APK 생성. APK ZIP에서 브랜드PNG/SVG와15개런처PNG,5개아이콘XML이 포함된 것을 확인했다. 배포release서명이나기기설치는하지 않았다.
- 최종 패키지 `design/play-store/Pocket4Cut-PlayStore-Film-v4.zip`: 4,616,601 bytes, 파일 7개. ZIP 내부 7개 항목이 원본과 SHA-256 일치한다. APK에 포함된 런처 PNG 15개도 현재 리소스와 해시가 일치한다. 기존 휴대전화 소개 이미지 8장과 이전 ZIP의 해시가 변경되지 않은 것을 확인했다.
- 단위 테스트·Lint·실기기 런처·촬영/저장/공유 E2E는 이번 이미지 교체에서 재실행하지 않았다. 앞선 CAMERA Lint 오류를 해결한 작업이 아니다. 스토어 등록 규격 검증과 심사 승인은 별개다. 새 아이콘/피처 ZIP만 추가했으며 commit/push/스토어 업로드는 하지 않았다.

## 2026-09-10 — 아이콘 방향 재검토와 필름 레퍼런스 조사 (Asia/Seoul)

- 기준: `codex/setup-project-guidance`, HEAD `ac42903` 및 기존 미커밋 변경 포함. 사용자 피드백은 단순 인화지 아이콘 → 카메라 일러스트도 부적합 → 숫자 4 중심도 부적합 → 실제 네 컷/필름 사진을 웹에서 참고하라는 순서로 추가되었다.
- `design/branding/print-booth-v2/`에 카메라와 인화지 벡터 초안·PNG·검증 코드를 만들었다. 512px PNG 33,316B, RGBA8/sRGB ICC, 66dp 안전원 검사 및 Android 리소스 빌드는 통과했지만 **사용자가 방향을 거절해 최종 채택하지 않았다.**
- `design/branding/print-booth-v3/`의 숫자 4 심볼 SVG/PNG도 **미채택 비교용 초안**이다. v3 밀도별 PNG/배포 ZIP 생성은 중단했다. 두 폴더의 README에 현행 아이콘이 아님을 표시했다.
- 거절된 초안이 SVG/XML/PNG에 혼재하지 않도록 앱 아이콘을 마지막 납품본 v1으로 복구했다. `v2/previous-icon/` 기준 PNG 15개 SHA-256 일치, SVG/XML 6개 줄바꿈 정규화 후 내용 일치를 확인했다. 최초 바이트 비교의 배경 XML 불일치는 줄바꿈 차이였으며 전체 파일이 바이트 단위로 동일하다고 보고하지 않는다.
- 복구 후 `./gradlew.bat :app:assembleDebug --offline --console=plain` 성공: 36개 태스크 중 10개 실행/26개 UP-TO-DATE. 새 단위 테스트·Lint·실기기 검증은 실행하지 않았다. 앞선 Lint 오류를 해결한 작업도 아니다.
- 스토어 아이콘 사본, 기존 소개 PNG 8장, 기존 다운로드 ZIP은 변경하지 않았다. 초안에 실제 사용자 사진을 쓰거나 기기 데이터를 변경하지 않았다.
- 웹 조사: [Photo Pronto 실제 네 컷 인화지](https://www.photo-pronto.com/gallery), [Photoautomat 아날로그 사진 스트립](https://photoautomat.de/vermietung), [Kodak 필름 참고서](https://www.kodak.com/content/products-brochures/Film/kodak-essential-reference-guide-for-filmmakers.pdf), [ILFORD 흑백 필름 가이드](https://www.ilfordphoto.com/wp/wp-content/uploads/2017/04/Processing-your-first-black-and-white-film.pdf). 인생네컷 공식 클래식 프레임 페이지는 검색 색인에서 확인했으나 직접 열기는 403이므로 세부 이미지 확인 자료로 사용하지 않았다.
- 도출한 방향은 제작 제안이며 아직 구현하지 않았다: 세로로 이어진 네 사진과 인화지 여백을 중심에 두고, 실제 필름의 반복 천공·프레임 경계를 절제해서 참조한다. 포토부스 인화지와 네거티브 필름은 서로 다른 매체임을 구분하며 타사 로고/프레임을 복제하지 않는다.
- commit/push/스토어 업로드는 하지 않았다.

## 2026-09-10 — Print Booth 앱 아이콘 및 Play 휴대전화 등록 이미지 (Asia/Seoul)

### 범위와 기준

- 요청: 기존 UI 디자인에 맞춘 새 앱 아이콘 적용, 스토어 아이콘 파일, 앱 소개와 사용법을 설명하는 휴대전화 스크린샷 10장 이내 제작.
- 기준: 브랜치 `codex/setup-project-guidance`, HEAD `ac42903`과 이전부터 존재한 미커밋 UI·버전 변경을 포함한 로컬 작업 트리.
- Google Play의 기기 유형별 최대 8장 규격을 확인해 등록 이미지 8장으로 구성했다. 앱 아이콘은 별도 파일이다. 등록정보 업로드·게시·심사·release 서명 작업은 하지 않았다.
- 기존 README, 앱 버전, UI 재설계, `engineering/`, 이전 HTML 시안과 이미지, 출시 메모는 유지했다. 이번 아이콘/스토어 이미지 작업에서는 프로덕션 화면 코드와 기존 Gradle 설정을 추가 변경하지 않았다.

### 변경 파일과 산출물

- `app/src/main/assets/branding/pocket_4cut_app_icon.svg`: 종이색 네 컷 인화지, 잉크색 네 칸, 빨간 배경을 사용한 단순 벡터 아이콘.
- `app/src/main/res/`: 런처 전경/배경/monochrome XML, 일반/원형 adaptive 연결, 밀도별 PNG 15개. Manifest의 기존 아이콘 참조를 유지하며 새 자산으로 교체했다.
- `design/branding/print-booth-v1/`: SVG 원본, 512px 스토어 아이콘, 1024px 마스터, 마스크 확인 이미지와 검증 JSON, 재생성 스크립트. 교체 전 기존 아이콘 파일 19개는 `previous-icon/`에 보관했다.
- `design/play-store/print-booth-v1/`: `phone-screenshots/`에 번호순 개별 PNG 8장, 앱 아이콘, 전체 미리보기, 한국어 대체 텍스트, 업로드 안내와 검증 메모. 생성 사진·실제 Compose 원본·제작 코드는 하위 폴더로 분리했다.
- `scripts/store-screenshots.init.gradle`와 `design/play-store/print-booth-v1/capture/`: 명시적인 init script 실행에서만 붙는 Paparazzi 화면 렌더 도구. 앱의 기존 Screen 함수에 예시 상태를 공급하며 일반/release 앱에는 포함하지 않는다.
- `docs/IMAGE_ASSET_GUIDE.md`: 최초 조사 당시 아이콘 정보와 이번 갱신을 구분해 현행 아이콘 자산·안전 영역·검증 정보를 추가했다.

### 디자인과 이미지 검증

- 아이콘은 512×512 RGBA8, sRGB ICC, 14,882 bytes, 알파 255의 정사각형 PNG다. 미리 둥근 외곽/그림자를 넣지 않았다. 원본 SVG와 스토어 사본의 일치, 전경/단색 레이어, 밀도별 크기, 네 칸의 단색 투명 구멍을 확인했다.
- 화면 구성: 완성 예시 → 컷 수 → 사진 선택 → 레이아웃 → 프레임 색 → 필터 → 사진별 상세 편집 → 보관함.
- 등록 PNG는 8장 모두 1080×1920, 알파 없는 24-bit RGB PNG다. 추가 문구를 실제 앱 UI 밖에 배치하고 크기·색 채널·경계·SHA-256을 `asset-validation.json`에 기록했다.
- 실제 앱 Compose 함수를 1080×2400으로 렌더한 원본을 사용했다. 이전 HTML 시안을 Android 실행 캡처로 바꾸어 표기하지 않았다. 이미지 생성 도구는 가상의 성인 예시 사진에만 사용했고 UI나 문구는 생성 모델로 그리지 않았다.
- 초기 캡처의 빈 사진·로딩 상태를 시각 검사에서 발견해 테스트 전용 파일 경로/I/O와 예시 상태를 수정한 뒤 재캡처했다. 필터·레이아웃·사진별 선택지는 실제 스크롤 상태로 노출했다. 스크롤 위쪽의 긴 미리보기가 부분적으로 보이는 상태와 이미지 제작 과정의 잘림을 구분한다.
- 사진 선택 번호 1–4, 레이아웃 3종, 필터 4종, 사진별 썸네일과 보정 조작부, 보관함의 실제 Renderer 결과 4개를 확인했다. 생성 사진과 테스트 fixture는 앱에 번들하지 않았다.

### 실행한 검사와 결과

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain
```

- `assembleDebug`: 성공. `packageDebug`를 실제 실행해 새 아이콘이 포함된 APK를 생성했다. 리소스 컴파일 등 일부 입력은 앞선 렌더 빌드의 결과를 재사용했다.
- 기본 `testDebugUnitTest`: 실제 실행 성공, tests=1/failures=0/errors=0/skipped=0. 기본 산술 테스트이며 앱 사용 흐름 테스트는 아니다. 실행 시각 KST 00:33:15.947.
- `lintDebug`: 실패, 오류 1개/경고 91개/힌트 2개. 기존 CAMERA의 `PermissionImpliesUnsupportedChromeOsHardware`이며 이 작업에서 Manifest를 수정하거나 suppress하지 않았다.
- 결합 명령 exit code 1, 52개 태스크 중 15개 실행/37개 UP-TO-DATE. Lint 실패와 APK·단위 테스트 결과를 구분한다.

```powershell
./gradlew.bat -I scripts/store-screenshots.init.gradle :app:testDebugUnitTest --tests com.pocket4cut.storecapture.StoreScreenshotsTest --offline --console=plain
node design/play-store/print-booth-v1/source/render-listing.cjs
```

- Compose 화면 렌더 테스트 9개 성공, 실패/오류/건너뜀 0개. Home 1장을 추가로 렌더했지만 스토어 등록 구성은 요청한 8종이다.
- PNG 제작 스크립트 성공, 등록 이미지 8개 출력 및 크기·알파·색 채널·문구/이미지 경계 검사 통과. 최종 화면은 별도 시각 검사를 수행했다.
- 최종 원본 생성 완료 KST 00:45:13. 결과 화면은 실제 120px 스크롤로 인화지 하단·홈·저장·공유가 모두 보이도록 확인했다.
- `design/play-store/Pocket4Cut-PlayStore-Assets.zip` 생성: 4,290,390 bytes, 파일 13개. 휴대전화 PNG 8개와 아이콘 2개·대체 텍스트·업로드 안내·전체 미리보기로 구성했다. ZIP을 다시 열어 내부 파일 13개가 원본과 SHA-256 일치함을 확인했고 제작용 원본/fixture/코드는 포함하지 않았다.
- 정확한 렌더 실행 시각·화면별 테스트는 `capture/capture-manifest.json`, 이미지 파일 해시는 `asset-validation.json`, 상세 한계는 `VERIFICATION.md`에서 확인한다.

### 미검증과 남은 사항

- 연결된 기기/AVD가 없어 실기기 런처, 카메라 촬영, 사진첩 저장·공유, Android E2E는 미실행이다. 호스트 렌더 성공을 이 기능들의 성공으로 해석하지 않는다.
- 사진 재정렬 최종 출력, 커스텀 스티커 내보내기 등 기존 결함은 이번 소개 소재로 삼지 않았으며 수정하지 않았다.
- 스토어 이미지 규격 준수와 Play 심사 승인은 별개다. 실제 배포할 앱이 변경되면 등록 이미지를 해당 버전으로 다시 생성해야 한다.
- 사용자 사진 삭제, 앱 데이터 초기화, stage/commit/push는 수행하지 않았다.

## 2026-09-10 — 프로젝트 개발 지침과 기준 문서 구성

### 요청과 작업 범위

기존 README·기획 문서·실제 코드 구조·빌드 설정·테스트·Git 상태를 조사하고 프로젝트 맞춤 Codex 지침을 적용했다. 기능 코드는 변경하지 않고 다음 네 파일을 새로 작성하는 작업이다.

- [AGENTS.md](../AGENTS.md): 한국어 소통, 조사 순서, 변경 범위, 기존 작업/사진 보존, 이미지 처리 검증, Git·작업 로그 운영 지침.
- [ARCHITECTURE.md](ARCHITECTURE.md): 실제 단일 모듈·화면·상태·이미지·저장 구조와 미연결 코드, 향후 제안.
- [IMAGE_ASSET_GUIDE.md](IMAGE_ASSET_GUIDE.md): 현재 프레임·스티커·아이콘·패턴 구현, 파일명·해상도·투명 배경·내보내기 기준과 신규 자산 제안.
- `WORKLOG.md`: 이번 조사·검증 기준과 이후 기록 방식.

루트 `AGENTS.md`와 위 docs 파일은 작업 시작 시 존재하지 않았다. 사용자 지정 경로를 사용했으며 전역 Codex 설정이나 별도 규칙 디렉터리를 만들지 않았다. AGENTS.md의 프로젝트 지침 역할은 [OpenAI 공식 안내](https://learn.chatgpt.com/docs/agent-configuration/agents-md)로 확인했다.

### 조사 기준과 기존 작업 보호

- 시작 HEAD: `95140a0`, 시작 브랜치: `main`.
- 작업 브랜치: `codex/setup-project-guidance`를 생성했다.
- origin은 `sin-jun-woo/Pocket-4Cut-Android` GitHub 저장소다. 이번 작업은 main 병합이나 강제 push를 포함하지 않는다.
- 시작 시 staged 변경은 없었다. 기존 tracked 변경은 README 1개와 app 관련 27개였으며, 이전 분석 문서·디자인·출시노트·smoke script 등 미추적 파일도 있었다.
- 기존 추적/미추적 일반 파일 190개의 SHA-256 기준을 작업 전 로컬 캐시에 보관했고, 문서 작성 후 **190/190개 불변**을 확인했다. 비밀 파일과 ignored 빌드 산출물은 이 비교 대상이 아니다.
- 기존 앱 변경, README 변경, `PROJECT_UNDERSTANDING.md`, `engineering/`, `design/`, 기존 출시노트와 smoke script는 이번 stage/commit 대상에서 제외한다.
- **아래 빌드 결과와 구조 문서는 미커밋 앱 변경을 포함한 로컬 작업 트리 기준이다.** 문서만 커밋하므로 원격 브랜치의 앱 소스까지 같은 상태가 되는 것은 아니다.

### 확인된 프로젝트 구성

- Gradle 모듈은 `:app` 하나, applicationId/namespace는 `com.pocket4cut`.
- compileSdk/targetSdk 36, minSdk 26, Gradle 8.13, AGP 8.13.2, Kotlin 2.0.21, Java/Kotlin 코드 타깃 11.
- 이번 실행 환경은 Windows, Gradle 실행 JVM은 JBR 21.0.10이다. 개인 JDK/SDK 경로를 문서나 빌드 설정에 추가하지 않았다.
- 로컬 앱 버전은 `1.2 (versionCode 3)`이나 시작 HEAD의 앱 버전은 `1.0 (versionCode 1)`이다. 버전 변경은 기존 미커밋 작업이며 이번에 반영하지 않는다.
- main Kotlin 85파일, 15,224줄. CameraX·Compose·Canvas, JSON 세션 저장소, SharedPreferences를 사용하는 로컬 앱이다.
- 기존 루트 README/ARCHITECTURE/API/DATA_STORAGE/ERROR_HANDLING/PROJECT_PLAN에는 과거 상태나 미래 설계가 포함되어 있다. Room/Hilt/완성된 UseCase/AWS를 현재 구현으로 옮겨 적지 않았다.
- GitHub Actions workflow는 main의 docs 변경 또는 수동 실행으로 docs 전체를 Pages에 게시한다. 현재 Android build/test/lint CI는 없다.

### 이번에 실행한 검증

저장소 루트에서 실행했다. 의존성이 있는 기존 Gradle 캐시와 로컬 JDK를 사용했고, 소스·의존성·Lint 설정은 바꾸지 않았다.

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain
```

- `:app:assembleDebug`: 통과, **UP-TO-DATE**. 기존 APK 산출물과 입력 상태를 재사용했다.
- `:app:testDebugUnitTest`: 첫 결합 명령에서는 **UP-TO-DATE**였으므로 아래 명령으로 테스트 태스크만 실제 재실행했다.
- `:app:lintDebug`: 실패. 기존 분석 결과를 재사용한 검사에서 **오류 1개, 경고 95개, 힌트 2개**를 보고했다.
- 결합 명령 exit code는 1이며, 52개 actionable task 중 1개 실행·51개 UP-TO-DATE였다. 빌드/단위 테스트 통과와 Lint 실패를 구분한다.

```powershell
./gradlew.bat :app:testDebugUnitTest --rerun --offline --console=plain
```

- 실제 테스트 태스크 재실행: **성공**, exit code 0. 의존 태스크는 재사용했다.
- 결과: tests=1, failures=0, errors=0, skipped=0.
- 테스트는 `ExampleUnitTest.addition_isCorrect`의 기본 산술 확인이며 촬영·편집·저장 회귀 검증이 아니다.
- 결과 XML 실행 시각: `2026-09-09T15:04:12.439Z` = KST `2026-09-10 00:04:12.439`.
- 산출물 경로: `app/build/outputs/apk/debug/app-debug.apk`, `app/build/test-results/testDebugUnitTest/TEST-com.pocket4cut.ExampleUnitTest.xml`, `app/build/reports/lint-results-debug.html` 및 `.xml`. 이 파일은 이번 커밋에 포함하지 않는다.

Lint 차단 사유는 `app/src/main/AndroidManifest.xml:6`의 `PermissionImpliesUnsupportedChromeOsHardware`다. CAMERA permission에 대응하는 하드웨어 uses-feature 선언이 없다. 같은 선언은 시작 HEAD에도 있으며 이번 문서 작업에서 수정하거나 suppress하지 않았다.

경고 95개에는 UseKtx 49, GradleDependency 11, ModifierParameter 7, UnusedResources 7 등이 포함된다. 업데이트 경고를 실제 기능 실패 또는 일괄 업데이트 필요성으로 단정하지 않는다.

### 검토와 미검증 범위

- 새 문서의 소스 링크, 현재 구현/제안 구분, 미커밋 작업 기준, 공개될 정보 범위를 검토했다. 링크·코드 블록·UTF-8 문자·공백 검사 오류는 0개였다.
- Git diff와 문서 네 파일만 포함된 staged 목록을 검토했고, staged 내용이 검토한 파일과 일치함을 확인했다. `git diff --cached --check`도 통과했다. 기존 파일 190개의 해시 보존을 확인했다.
- Android instrumentation 테스트는 새로 실행하지 않았다. 기본 packageName 테스트는 있으나 이번에는 기기 앱 설치·촬영·데이터 초기화를 수행하지 않았다.
- 실기기 전체 흐름, 프로세스 복원, 저메모리, API26–28 사진첩 저장, 백업 복원, 접근성, release 서명과 Play 공개 상태는 이번 검증 범위 밖이다.
- 문서에서 확인한 사진 순서·문구 영역·색/스티커 출력·삭제 수명 문제는 후속 기능 작업 대상으로 남긴다.

### 커밋과 원격 확인 방식

이번 커밋의 파일 범위는 위 네 문서이며 메시지는 `docs: Pocket 4Cut 개발 지침 및 작업 로그 추가`다. 커밋 SHA는 자기 자신을 파일 본문에 넣어 반복 amend하지 않고 Git 이력과 최종 완료 보고에서 확인한다.

```powershell
git log -1 --format='%H %s' -- docs/WORKLOG.md
git status --short --branch
git rev-parse --abbrev-ref '@{upstream}'
git ls-remote --heads origin codex/setup-project-guidance
```

문서 검토 후 `git push --set-upstream origin codex/setup-project-guidance`를 수행하고 원격 SHA·upstream 설정 결과를 최종 보고에 남긴다. 이 항목의 존재만으로 원격 반영 성공을 추정하지 않는다.

## 이후 작업 기록 양식

```markdown
## YYYY-MM-DD — 작업 제목 (Asia/Seoul)
- 요청/범위:
- 기준 브랜치·커밋 / 미커밋 변경 포함 여부:
- 변경 파일과 목적:
- 현재 구현에서 확인한 사실:
- 실행 명령과 결과(통과/실패/캐시 재사용):
- 미실행 항목과 이유:
- 기존 변경·사용자 데이터 보존 확인:
- 남은 문제 / 향후 제안:
- commit/push를 수행한 경우 식별 방법과 결과:
```

## 2026-09-22 — 신뢰성·이미지 품질 개편 (Asia/Seoul)

- 요청/범위: [전면 감사](../engineering/EMULATOR_AUDIT_2026-09-22.md)의 E01–E10·C01–C15를 근거로 사진 원본 보존, 편집 초안·복구, 미리보기/출력 통합, 내보내기 중복 방지, QA 격리, 접근성/문서/CI를 개선한다. 원래 감사 기록은 수정하지 않았다.
- 시작 기준: `a0eff0498d50fc160938138ed6cfb701f89721a4`, 시작 브랜치 `codex/reels-promo`, 작업 트리 clean. 작업 브랜치는 `codex/refactor-reliability`다. 아래는 이 브랜치의 변경 코드를 포함하며 시작 HEAD의 기능이라고 주장하지 않는다.
- 데이터: `SessionDocument`/codec/repository, 세션별 schema·revision, 프로세스 Mutex·AtomicFile·직전 정상본, 기존 JSON 이전·tombstone·소유권 확인 삭제, 결과·내보내기 기록을 추가했다. 새 캡처는 pending JPEG를 게시하고 원본을 재압축하지 않는다. 결과는 ID별 고유 파일이며 선기록 AtomicFile 저널로 파일 게시 후 문서 기록 전 중단을 재생한다. 세션 삭제는 해당 결과 저널·손상 원문도 확인 후 정리한다.
- 흐름: 촬영 일시정지/사용자 재개·늦은 callback 방어, 세션 생성 중 Home 일시정지, 손상 pending 촬영 파일 차단, 선택/일반/상세 편집의 PhotoId 기반 순서·보정 자동 저장, 홈·보관함 초안 이어하기, 오류 표시, 카메라 권한 재확인, Android 구버전 사진첩 쓰기 권한 경로를 연결했다.
- 이미지: `RenderSnapshot`, 공통 Canvas scene, vector 스티커, color ID/gradient, 문구·날짜, 레이어 순서, 비대칭 6컷 배치 버전, 기기 독립 출력 크기를 추가했다. 일반 편집 필터는 원본을 공통 Canvas에서 직접 그려 전체 사진의 반복 Bitmap 복제를 제거했다. 6컷의 계절 footer 간격, 커스텀 장식 위치, 날짜만 켠 문구의 중복도 수정했다. 원본은 컷별로 디코드하며 preview 취소/교체와 EXIF 8방향을 검사한다.
- 앱/검사 경계: `com.pocket4cut.qa` debug/qaRelease, 설정만 백업하는 두 XML, optional camera 선언, Android CI, 기본 렌더 진단·출력·저장·비트맵 계측 테스트를 추가했다. README/AGENTS/ARCHITECTURE/IMAGE_ASSET_GUIDE를 현행 작업 트리에 맞췄다.
- 사용자 자료 보존: 시작할 때 미커밋 작업이 없었다. UI·계측은 분리된 QA 패키지만 대상으로 했고 배포 패키지 초기화·사진 삭제를 실행하지 않았다. QA 결과는 가상 카메라 합성 장면이다.
- 수동 API 37: QA 2컷 4장 촬영 후 프로세스 종료·재실행, 홈 이어하기, 선택, 필름 블랙 프레임 편집, 1248×3179px JPEG 생성·사진첩 저장·보관함 재열기를 확인했다. [검증 기록과 화면](../engineering/REFACTOR_VERIFICATION_2026-09-22.md)에 실행 범위와 남은 항목을 분리했다.
- 실행 명령: `.\gradlew.bat :app:assembleDebug :app:assembleQaRelease :app:testDebugUnitTest :app:lintDebug --offline --console=plain`과 `.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain`. 처음에는 촬영 반복문 `SuspiciousIndentation`, 마지막 통합 직전에는 API 29 `MediaStore.setIncludePending` 호출의 버전 주석 누락으로 Lint 오류가 각각 1개 있었다. 해당 코드만 수정한 뒤 같은 빌드·JVM·Lint 명령을 재실행해 **BUILD SUCCESSFUL**을 확인했다. 중간 실패를 성공으로 합치지 않는다.
- 최종 Lint: 오류 0, 경고 77, 힌트 2. 수정 전 감사의 경고 총수 77과 같다. 현재 경고 종류는 UseKtx 51, UnusedResources 8, ModifierParameter 6, IconLauncherShape 5, IconXmlAndPng 2, 그 밖의 단일 항목 5개다. 경고 전체의 기준 커밋 대비 항목별 동일성은 대조하지 않았다. `StaticFieldLeak`과 `ClickableViewAccessibility` 등 남은 경고는 별도 검토 대상이다.
- 선택 실행: 기존 렌더 진단 7/7과 출력 크기/배치 버전 2/2 통과. 마지막 전체 `.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain`은 API 37 QA 패키지에서 **38/38 통과, 실패·오류·건너뜀 0**이었다. 접근성 5개, pending MediaStore 재개, 결과 journal 손상, 촬영 JPEG 검증, 비대칭 6컷 footer, 필터 Bitmap 수명 검사를 포함한다. JVM 기본 테스트는 1개 통과했으나 앱 흐름 증거가 아니다. Gradle의 `UP-TO-DATE` 표시와 기기 계측의 실제 38개 실행을 구분한다.
- 최적화 APK: `:app:assembleQaRelease`는 R8·리소스 축소를 적용해 성공했고 `app-qaRelease.apk`를 API 37에 `com.pocket4cut.qa`로 설치해 홈, [권한 화면](../engineering/refactor-evidence/2026-09-22/qa-r8-permission.png), 허용 뒤 [CameraX 미리보기](../engineering/refactor-evidence/2026-09-22/qa-r8-camera.png)를 확인했다. [최적화 QA 홈](../engineering/refactor-evidence/2026-09-22/qa-r8-home.png). 이는 최적화 빌드의 전 화면 기능·실제 배포 서명 검사가 아니다.
- 미검증/위험: API 26/28/29/33/36 AVD와 실기기, 실제 cloud/device backup 복원, 모든 컷 타이밍의 Home·잠금·권한 상실, 저장/삭제 각 단계 fault injection, TalkBack/큰 글자/가로 화면, 256MiB 최대 출력 메모리 실측, 모든 색·장식·글꼴 조합, 공유 수신 앱은 완료로 표시하지 않는다. 초안 입력 debounce·NavHost I/O·촬영/삭제의 포괄적 transaction journal·원인별 복구 UI 등도 후속 작업이다.
- 커밋/push 식별: 앱 소스·테스트·CI 커밋은 `155ced0b44b381bb8d274d2652d6d2fd421b2973`이다. 이 문서의 자체 SHA는 본문에 넣지 않는다. 최종 원격 반영은 `git log -1`, `git status --short --branch`, `git ls-remote --heads origin codex/refactor-reliability`와 최종 보고에서 확인한다.

## 2026-09-22 — Paper Seasons 사계절 프레임 전면 교체 (Asia/Seoul)

- 요청/범위: 봄·여름·가을·겨울만 새 일러스트와 종이 질감의 디자인으로 교체하고 2·4·6컷의 모든 배치에 적용한다. 개별 프레임 이미지와 인스타그램 홍보 이미지도 제공한다. 다른 프레임 카테고리·사용자 스티커·앱 아이콘·앱 버전·촬영 정책은 변경하지 않았다.
- 시작 기준: `d249a01`, `codex/refactor-reliability`, 작업 트리 clean. 새 작업 브랜치 `codex/seasonal-frames`의 미커밋 변경을 포함해 검증했다. 기록 시점은 2026-09-22 18:41 KST 이후다.
- 디자인: 전용 이미지 생성 도구로 계절별 6개씩 독창적인 다색 모티프를 생성했다. 봄은 꽃·리본·딸기, 여름은 바다·조개·레몬, 가을은 낙엽·책·커피, 겨울은 장갑·눈사람·코코아를 중심으로 구성했다. 도구가 모델/품질 선택 옵션을 노출하지 않아 특정 최고 모델을 선택했다고 주장하지 않는다. 슬롯·브랜드·문구·패턴·안전 영역은 코드로 제어한다.
- 변경 파일: `frame/rendering/SeasonalStickerArt.kt`, `SeasonHTMLFrameStyle.kt`, `CollageRenderer.kt`, `CollagePreview.kt`, `DetailEditViewModel.kt`에 로딩·캐시·공통 렌더 연결과 실패/재시도를 구현했다. `EditScreen/EditViewModel`, `PocketNavHost`, 배치 선택 및 색/계절/커스텀 프레임 선택 화면에는 기존 6컷의 `layoutVersion` 전달을 보완했다. JSON schema·계절 ID·레이아웃 ID를 바꾸지 않았다.
- 구형 자산 제거: 전용 `Spring/Summer/Autumn/WinterFrameVectorDecor.kt`와 `SeasonDecorAnchors.kt` 5개를 제거했다. Git 이력에서 복구할 수 있다. 저장된 결과를 다시 렌더하거나 사용자 사진·초안을 삭제하지 않았다.
- 자산: `app/src/main/assets/seasonal/`에 1536×1024 RGBA/sRGB atlas 4개(총 약 6.95MB), `design/seasonal-frames/paper-seasons-v1/`에 생성 원본·프롬프트·가공/포장 스크립트·검증 자료를 추가했다. 캐시 참조는 최대 2장/약 12MiB이며 UI/진행 중 출력이 보유하는 이미지나 콜라주 메모리까지 포함한 상한은 아니다. 캐시 제거 시 수동 recycle하지 않는다. 새 앱 의존성은 추가하지 않았다.
- 납품: 현행 8배치×4계절 32종, 문구 공간 변형 32종, 구형 비대칭 개편 전 6컷 호환 8종으로 투명 사진창 PNG **72장**. 가상 성인 사진을 넣은 실제 Android 렌더 JPEG **32장**, 모아보기 **6장**, **1080×1350** 홍보 PNG 1장. 클래식 세로 4컷은 **1650×4920**, 다른 배치는 기존 출력 정책을 따르며 정확한 크기는 `manifest.json`에 있다. 실제 사용자/고객 사진은 사용하지 않았다. 앱의 일반 저장은 기존처럼 JPEG다.
- 패키지: [전체 ZIP](../design/seasonal-frames/paper-seasons-v1/deliverables/pocket4cut-paper-seasons-v1.zip), 113항목/103,096,843 bytes. ZIP을 재개봉하여 모든 항목의 길이·SHA-256이 원본과 일치함을 확인했다. archive SHA-256: `ecf1a7d655da6aec96656fb331052a1491cf844d6b1927b40323a955550044e2`. 파일 크기·해시는 `deliverables/package-validation.json`, 치수·슬롯은 `deliverables/manifest.json`, atlas 알파 검사는 `asset-validation.json`에 있다. 납품 PNG/JPEG의 형식·치수·알파는 `source/package-deliverables.cjs`가 실제 이미지와 대조해 검사했다.
- 새 검사: `SeasonalFrameContractInstrumentedTest`의 144종 사진/브랜드/문구 보호 영역과 72종 preview/export 투영 계약, atlas·ID 저장 호환, `SeasonalLegacyPreviewInstrumentedTest`의 구형 6컷 복원, `SeasonalFramePreviewInstrumentedTest`의 실제 Compose 시즌 전환 및 V1/V2 배치 표시를 추가했다. 기존 Bitmap/출력 계약 테스트는 새 구현을 대상으로 갱신했다. 테스트 fixture 4장은 기존 가상 성인 생성 사진을 재사용했다.

### 최종 실행 결과

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleQaRelease :app:testDebugUnitTest :app:lintDebug --offline --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.seasonalExport=true' '-Pandroid.testInstrumentationRunnerArguments.seasonalSourceRevision=d249a01+paper-seasons-v1-final-worktree' --offline --console=plain
```

- 최종 빌드: **BUILD SUCCESSFUL**, 3분 28초, 96 actionable tasks 중 17 실행/79 UP-TO-DATE. Debug 및 R8/리소스 축소 QA APK 생성 성공. 이번 최종 QA APK의 최적화 런타임은 설치하여 검증하지 않았다.
- JVM: 실제 테스트 태스크 실행, **1/1 통과**, 실패·오류·건너뜀 0. 기본 산술 검사이며 앱 기능 전체의 근거가 아니다.
- Lint: **오류 0, 경고 44, 힌트 2**. 기존 경고를 임의 suppress하지 않았다. 모든 경고가 기준 커밋과 항목별 동일하다고 주장하지 않는다.
- 계측: API 37 에뮬레이터, 실제 대상이 `com.pocket4cut.qa`/`.qa.test`임을 확인 후 **45/45 통과**, 실패·오류·건너뜀 0. opt-in exporter까지 실제 실행해 최종 72 PNG/32 JPEG를 얻었다. 별도 프로덕션 패키지를 설치·초기화하거나 사진을 삭제하지 않았다.
- 시각 검수: 4계절 밝은/어두운 배경의 atlas 가장자리, 실제 클래식 4컷 예시 4장, 전체 32종 및 각 시즌 8종 모아보기, 1080×1350 홍보 문구/사진 수/잘림을 확인했다. 중간 출력의 측면 스티커 잘림을 발견해 회전 경계까지 여백에 맞춘 후 전체 계측·출력을 다시 수행했다. 납품은 수정 후 최종 실행만 포함한다.
- 실제 UI 증거: [가을 선택 화면](../design/seasonal-frames/paper-seasons-v1/verification/autumn-picker-compose.png), 1280×2856. 실제 Compose 자동 UI 검사에서 얻은 화면이며 HTML 시안이나 전체 앱 수동 E2E가 아니다. 현재 도구 환경에서 클라우드 전용 녹화/업로드 대신 로컬 증거 파일을 제공했다.
- 실행 이슈와 해결: 초기 Gradle 캐시 위치/권한 오류는 기존 사용자 캐시를 지정해 해결했다. 초기 exporter는 QA 앱 삭제 후 파일을 가져올 수 없어 AGP additional test output 디렉터리로 바꾸고 재실행했다. 정책 검토의 대상 패키지 우려는 앱/계측 APK metadata와 `.qa` suffix를 대조해 해소했다. 최종 검사를 통과한 사실과 중간 환경 오류를 구분한다.
- 미검증/남은 위험: 물리 카메라/실기기, API 26–36 전 기종, TalkBack/큰 글자/가로 모드 전체 조합, 실제 최대 해상도 저메모리 스트레스, 실제 인쇄, release 서명/AAB/Play·Instagram 게시 및 심사는 수행하지 않았다. 기존 비계절 프레임 및 일반 저장 정책은 유지했으며 새 계절 프레임에 대한 자동 계약 검사와 수동 이미지 검수 범위만 완료로 기록한다.
- 문서: README, ARCHITECTURE, IMAGE_ASSET_GUIDE, 이 WORKLOG 및 자산 패키지 README를 갱신했다. 비밀 파일·원시 기기 로그·개인 로컬 경로는 공개 문서에 추가하지 않았다.
- commit/push: 검토된 이번 작업 파일만 명시적으로 stage하여 `feat: 사계절 포토 프레임 전면 교체`로 커밋하고 `origin/codex/seasonal-frames`에 일반 push한다. 자기 자신을 가리키는 SHA는 이 문서에 반복 기록하지 않는다. 실제 성공 여부와 SHA는 최종 `git log -1`, `git status --short --branch`, `git ls-remote --heads origin codex/seasonal-frames` 및 완료 보고로 확인한다.

## 2026-09-22 — 전체 계절/레이아웃 홍보 이미지 추가 (Asia/Seoul)

- 요청/범위: 사용자가 지정한 `design/seasonal-frames/paper-seasons-v1/deliverables/overview-all-seasons.png`를 바탕으로 Instagram 홍보 이미지 1장을 추가한다. 기준 `0e5212e`, `codex/seasonal-frames`, 시작 작업 트리 clean.
- 변경: 내장 전용 이미지 도구로 4계절×8배치를 모은 컬렉션 포스터를 생성했다. `instagram-layouts-promo-v2-master.png`(1122×1402)와 `instagram-layouts-promo-v2.png`(1080×1350, 불투명 sRGB PNG, 2,045,862 bytes)를 원본/납품으로 분리했다. 앱 코드·런타임 자산·기존 overview·첫 홍보 이미지·ZIP은 변경하지 않았다.
- 관련 파일: 자산 폴더의 `source/INSTAGRAM-LAYOUTS-V2-PROMPT.md`, `source/export-instagram-layouts-v2.cjs`, `verification/instagram-layouts-v2-validation.json`, README, IMAGE_ASSET_GUIDE 및 이 작업 기록. 새 이미지가 기존 ZIP에 들어 있다고 표기하지 않는다.
- 생성/검수: 전용 이미지 도구에 원본 overview를 참조로 제공했다. 큰 제목과 32조합 안내, 4계절 라벨, 2·4·6컷 안내, 4행×8개의 완전한 프레임 외곽을 시각 확인했다. 가로 4컷처럼 작은 창은 확대 검수했다. 실제 Android 스크린샷이나 픽셀 동일 렌더가 아니라 홍보 시안으로 구분하며 원래 프레임 파일이 정확한 제품 기준이다.
- 실행: `node --check design/seasonal-frames/paper-seasons-v1/source/export-instagram-layouts-v2.cjs` 및 `node design/seasonal-frames/paper-seasons-v1/source/export-instagram-layouts-v2.cjs` 성공. source/reference/delivery를 전체 디코드하고 비업스케일·4:5 비율·1080×1350·PNG·sRGB·알파 없음 및 SHA-256을 확인했다. 산출물 SHA-256은 `c21811c03568a409d57600f298d3657fd146711a9b4aa4c059afa7f3fddac64e`다.
- 미실행: 앱 파일 변경이 없어 Gradle 빌드/단위/계측을 반복하지 않았다. 이전 45/45 결과를 이번에 새로 실행한 것으로 계산하지 않는다. Instagram 실제 게시·압축·자르기·심사는 하지 않았다.
- 보존/권리: 사용자 사진이나 타사 캐릭터를 넣지 않았다. 이미지 생성 도구의 실제 요청을 남겼으며 사람이 직접 그렸다고 표시하지 않는다. 기존 ZIP은 그대로 유지하고 새 PNG를 별도 제공한다.
- Git: 위 이미지·제작 기록·문서만 명시적으로 stage하여 `design: 전체 계절 레이아웃 홍보 이미지 추가`로 커밋 후 현재 작업 브랜치에 일반 push한다. 실제 SHA/원격 일치/작업 트리 상태는 최종 명령과 완료 보고에서 확인한다.

## 2026-09-22 — 마지막 릴스 틀을 유지한 사계절 소개 영상 (Asia/Seoul)

- 요청/범위: 마지막 제작 릴스의 기본 틀을 유지하고 내용만 새 봄·여름·가을·겨울 프레임 소개로 바꾼다. 기준 `6d0c151d473d942ae5da70c8c745cda5f8f4377b`, `codex/seasonal-frames`, 시작 작업 트리 clean. 납품 검증/포장은 2026-09-22 19:30 KST에 완료했다.
- 확인한 원본: 설명 오버레이 제거까지 반영된 `reference-v3/deliverables/Pocket4Cut-Reels-Reference-Voice.mp4` 23초 버전이다. 원본 코드·음성·스토리·encoded contact sheet를 대조하고 v1/v2로 되돌리지 않았다.
- 변경 파일: 새 `design/reels/seasonal-v4/`의 제작 코드·대본·WAV·스틸·검증 JSON·MP4/MP3/커버/자막·ZIP·README, 이 WORKLOG와 IMAGE_ASSET_GUIDE. 기존 앱 소스·리소스·Gradle·이전 릴스·사용자 사진·사계절 프레임 원본을 변경하지 않았다.
- 영상: 이전 종이색·타이포그래피·인화사진·전환·검색 안내를 유지하고 실제 사계절 Android 렌더 JPEG를 사용했다. 네 시즌 전체 프레임/장식 확대, 대표 2·4·6컷, 8배치 안내, 저장/공유를 14장면으로 구성했다. 저장→공유는 같은 겨울 프레임/사진 순서를 유지한다. 음악·효과음과 이전에 제거한 설명 오버레이를 추가하지 않았다. 가상 성인 사진을 재사용했고 실제 기기 연속 녹화로 표시하지 않는다.
- 음성: 기존 Qwen3-TTS 1.7B Base, 같은 승인된 참조·Korean/seed 91026/CPU float32 SDPA/6스레드 설정을 오프라인으로 사용했다. 모델/tokenizer/참조 해시와 기존 Python 환경을 확인했고 `pip check` 오류 0이었다. 추가 설치·다운로드·외부 전송은 없다. SoX/flash-attn 미설치 경고는 비치명적이었고 기존 SDPA 경로가 완료됐다.
- 중간 실패: 첫 21.92초 음성에서 두 표현의 ASR 결과가 대본과 달라 타이밍/렌더 가드가 중단했다. 정답 프롬프트 없는 추가 탐욕 디코딩에서도 차이가 남았고 음성 오류인지 인식 오류인지 단정하지 않았다. 두 문구만 간결하게 바꿔 동일 조건으로 재생성했다. 첫 WAV·마스터·전사·생성 기록은 ignored `build/reels-seasonal-v4/attempt-01`에 보존했으며 참조와 사용자 자료를 삭제하지 않았다. ASR 원문을 정답으로 대체하거나 발음 오차 허용을 늘리지 않았다.
- 최종 음성: 20.960초 / 24 kHz / mono / float32 WAV, SHA-256 `a03ecd67aec028c403b2b0e68bfc4a380d471f82eb5f62f5b25803a443ff4c92`. 생성 206.672초, 명령 전체 221.188초, exit 0. 배속·발화 잘라내기 없이 0.12초 시작 여유와 끝 안내 시간을 둔 23초 / 48 kHz / stereo / PCM24 마스터로 처리했다.
- 실행: `node --check`로 제작 JS 문법을 확인했다. `node --test design/reels/seasonal-v4/source/test-time-captions.cjs` **10/10 통과**. `master-narration.cjs` → `transcribe-narration.cjs` → `time-captions.cjs`가 최종 성공했다. 로컬 whisper.cpp b4938/small-q5_1의 정답 프롬프트 없는 전사를 보존하고 명시한 공백/문장부호/개수 표기만 정규화해 **136자 일치, 편집 거리 0**, 실제 토큰 시각으로 14장면을 생성했다.
- 시각 실행: `render-seasons.cjs --stills`에서 **28스틸·41텍스트 경계·12프레임 배치 경계**를 확인했다. 이어 `render-seasons.cjs`로 실제 690프레임을 인코딩했다. 계절별 장식/문구, 사진창 수, 비대칭 6컷, 저장/공유 연속성, 커버/CTA 잘림을 독립 정지화면 검수에서도 확인했다. 최종 MP4에서 추출한 모아보기와 계절 확대 장면도 검수했다. 의도한 배경 사진의 화면 밖 배치와 주요 프레임 누락을 구분했다.
- 최종 내보내기: `export-seasons.cjs` 성공. **23.000초 / 1080×1920 / 30 fps / 690프레임 / H.264 BT.709 / AAC 48 kHz stereo**, 9,984,514 bytes. 전체 영상·음성 디코딩 PASS, A/V 길이 일치 PASS, faststart PASS, 검은 구간 0, 커버 크기 PASS, 최종 −16.0 LUFS / −2.4 dBTP. MP4 SHA-256 `4583fc67fbf61dd9fe2f65c393b14e8f67864daf8f225ef94eb659d6cfa96dfb`.
- 전달: `Pocket4Cut-Seasons-Cover.jpg` 1080×1920, 23초 내레이션 MP3, SRT·대본·게시글·업로드 안내. `powershell -NoProfile -ExecutionPolicy Bypass -File design/reels/seasonal-v4/source/package-seasons.ps1` 성공, **7항목 모두 원본 해시 일치**, ZIP 10,939,270 bytes, SHA-256 `52ef98e2d0c58048cd9caac1a6f3dcee6ae70fc9ee6229ae9573ed510e90e0c2`. 사용자 원본 영상·참조 음성/대사·개인 경로·비밀값은 납품 및 공개 문서에 포함하지 않는다.
- 미실행/한계: 앱 파일 변경이 없어 Android 빌드·JVM·계측 테스트는 반복하지 않았다. 이전 앱 검사 결과를 이번 실행으로 계산하지 않는다. 직접 청취/주관적 자연스러움/음색 동일성, 휴대전화 재생, Instagram 게시·압축·자르기·심사, Play 검색·배포 상태는 미검증이다. 게시 전에 배포 앱의 새 계절 프레임 적용 여부와 음성을 확인해야 한다.
- Git: 위 작업 파일만 정확한 경로로 stage하고 staged diff/공백/공개 정보 범위를 검사한 뒤 `feat: 사계절 프레임 릴스 홍보 영상 추가`로 커밋하고 현재 원격 브랜치에 일반 push한다. 자체 SHA는 반복 amend 없이 최종 `git log -1`, `git status --short --branch`, `git ls-remote --heads origin codex/seasonal-frames` 및 완료 보고에서 확인한다.

## 2026-09-22 — 출시 준비 추가 구현 및 로컬 최종 검증 (Asia/Seoul)

- 요청/기준: [이전 개편 검증](../engineering/REFACTOR_VERIFICATION_2026-09-22.md)에 남은 항목을 개선하고 최종 출시 후보를 검사한다. 시작 `6c815cb2ce3c665989e80512ffde1e71f268f86a`, `codex/seasonal-frames`, 시작 작업 트리 clean. `codex/release-readiness`에서 작업했다. 상세한 ID별 근거와 출시 결정은 [출시 준비 보고서](../engineering/RELEASE_READINESS_2026-09-22.md)에 둔다.
- 변경: 촬영 JPEG의 순서·손상 복구, 결과 게시 journal/JPEG/`.tmp`의 선택적 격리와 중단 재생, 손상 세션 숨김·다시 표시 및 안전한 삭제 차단을 추가했다. 촬영 중 화면 재생성의 CameraX 바인딩을 보완했다. 일반/상세/커스텀 편집에서 뒤로가기 직전 저장, 최신 커스텀 디자인 재사용, preview/export의 공통 그리기와 문구 폭·2행 규칙을 연결했다. 개인정보 문구·README/ARCHITECTURE/이미지 가이드/Play 출시 초안을 현행 코드에 맞췄다.
- 관련 소스/검사: `SessionDocumentRepository`, `FileImageStorage`, Capture/Gallery/Edit/DetailEdit/CustomFrameEditor/NavHost, `CollagePreview`, `CollageRenderer`, 앱 내 개인정보 문구, 결과 게시·세션·촬영 복구·편집 이탈·렌더 계약 계측, CI 워크플로. `.github/workflows/android-ci.yml`은 `codex/**` push와 API 26/28/29/33/36 매트릭스를 정의한다. 워크플로 정의만으로 원격 통과를 주장하지 않는다.
- 빌드/JVM/Lint/AAB: `:app:assembleDebug :app:assembleQaRelease :app:testDebugUnitTest :app:lintDebug :app:bundleRelease` 결합 실행 **BUILD SUCCESSFUL**, 142 task 중 32 실행/110 up-to-date. JVM 1/1. Debug Lint 오류 0·경고 56·힌트 2. 별도 `:app:lintRelease` **BUILD SUCCESSFUL**, 오류 0·경고 41·힌트 2. 둘 다 `--offline --console=plain`으로 실행했다. 기존 경고를 suppress하지 않았고 이전 Paper Seasons 기록 44개와 항목별 동일성을 주장하지 않는다.
- 계측: API 37 Pixel_10_Pro AVD의 기본 전체 실행 XML **59개 중 57 통과·2 opt-in 건너뜀**, 실패/오류 0. `renderMemoryStress=true`, `seasonalExport=true`를 켠 별도 전체 실행은 **59/59 통과**, 실패·오류·건너뜀 0. 실제 계절 출력 72 PNG/32 JPEG는 `app/build`의 무추적 검사 산출물이며 새 앱 이미지 자산은 없다. 3265×4898 최대 결과 검사에서 renderer 소유 Bitmap 87,967,880 bytes, 프로세스 PSS 표본 283,263KiB, emulator memoryClass 192MiB를 기록했다. 이 표본은 256MiB 실기기 peak 보증이 아니다.
- R8 QA 실제 조작: `com.pocket4cut.qa` 최적화 APK를 API 37에 설치했다. 가상 카메라 2컷 4장 촬영→2장 선택→프로세스 종료·재실행→선택 복원→세로 배치/필름 블랙/필름 필터/문구/날짜/첫 사진 회전→결과 JPEG→사진첩 저장→보관함 재열기를 확인했다. 결과 1248×3307px, 126,145 bytes. `Pocket4Cut_` MediaStore 행은 저장 전 1, 저장 후 2, 반복 저장과 재열기 후에도 2였다. 공유 시트는 열렸으나 수신 앱의 URI 바이트 읽기는 미검증이다. 별도 10초 카운트다운에서 시작 0.5초 후 Home 전환, 복귀/프로세스 재시작 시 명시적 `이어서 촬영` 대기를 확인했다. [합성 사진 결과](../engineering/release-evidence/2026-09-22/qa-r8-2cut-result.jpg), [재열기 화면](../engineering/release-evidence/2026-09-22/qa-r8-result-reopened.png), [중단 화면](../engineering/release-evidence/2026-09-22/qa-r8-capture-paused.png).
- 번들: 최종 `app-release.aab` 38,276,058 bytes, SHA-256 `5a9ddea0a3375c8a4e383b9168db30011994c2a430205119441c83c79d39f502`; `jarsigner -verify`는 `jar verified`를 반환했다. release Manifest는 ID `com.pocket4cut`, 1.4(5), min26/target36, CAMERA와 API 28 상한 쓰기 권한, 설정 허용 목록 백업이다. 업로드 키가 기존 Play Console 등록 키와 같은지는 확인하지 못했다. 서명키·암호 파일은 열람/커밋하지 않았다.
- 출시 제한: 로컬 출시 후보 및 API 37 제한 검증은 통과했지만, 원격 API 26/28/29/33/36 CI 결과, 실제 전·후면 카메라·공유 수신·TalkBack/2배 글자/가로·저메모리/저장 공간 부족·실제 백업 복원·중단 지점별 장애 주입, 13개 TTF 파일별 출처/고지, Play Console 서명·버전·데이터 안전성·심사는 미완료다. 따라서 프로덕션 출시 승인으로 기록하지 않는다. 새 이미지 에셋은 없으며 기존 벡터·계절 atlas만 재사용했다.
- Git: 작업 관련 파일과 합성 QA 증거만 정확한 경로로 stage하여 검토 후 원자적 커밋·현재 브랜치 일반 push를 수행한다. 자체 SHA와 원격 결과는 최종 `git log -1`, `git status --short --branch`, `git ls-remote --heads origin codex/release-readiness` 및 완료 보고에서 확인한다.

## 2026-09-22 — 출시 차단 항목 재확인 및 개인정보 링크 보정 (Asia/Seoul)

- 기준: 첫 출시 준비 커밋 `157334b8d59bfae500f50aa942dd2953ebe63c1e`은 `origin/codex/release-readiness`에 push됐다. 원격 GitHub Actions [실행 35724117933](https://github.com/sin-jun-woo/Pocket-4Cut-Android/actions/runs/35724117933)은 build/API 29·33·36 성공, API 26·28 실패다. 공개 annotation에는 exit 1만 있어 설치·부팅·계측 중 원인을 확정할 수 없다. 구형 사진첩 권한 거부·재허용·재시도는 현재 계측이 API 29 미만에서 건너뛰므로 별도 미검증이다.
- 수정: 공개 개인정보 URL의 익명 HTTP 응답이 404여서 앱 내 방침 전문 끝의 도달 불가능한 링크 한 줄을 제거했다. 앱 내 전문은 유지하며 `docs/privacy-policy.html` 공개 배포는 별도 출시 관문으로 남겼다. Play 출시 초안과 출시 보고서에 현재 404를 명시했다.
- 권리: 번들 TTF 13개를 파일명으로 4개 게시자 계열에 분류했으나 각 바이너리 공식 원본·해시·고지 전문이 저장소에 없어 출시 권리 확인은 열려 있다. IMAGE_ASSET_GUIDE에 근거와 한계를 보강했다.
- Lint 비교: 같은 JDK/Gradle cache 및 격리 buildDir에서 시작 `6c815cb` 44경고/2힌트, 첫 출시 커밋 `157334b` 42경고/2힌트로 새 코드 경고 증가가 없었다. 작업 트리 실행의 56경고와 격리 실행의 42경고 차이는 버전 알림 항목 14개이며 정확한 환경 원인은 미확정이다. 기존 경고 분류·처리는 남았다.
- 최종 소스 검증: `:app:assembleDebug :app:assembleQaRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:bundleRelease --offline --console=plain` **BUILD SUCCESSFUL** (4분 5초, 144 task 중 37 executed/107 up-to-date). JVM 1/1, Debug Lint 오류 0/경고 56/힌트 2, Release Lint 오류 0/경고 41/힌트 2. 새 AAB 38,276,019 bytes, SHA-256 `5bcb4411ba8b9a0d97bc977f4ccf49c3fa079b17b2055a9b8d0a92a6f43931e1`; `jarsigner -verify` exit 0/`jar verified`이며 자기 서명·타임스탬프 관련 경고도 남는다. 인수를 인용해 재실행한 API 37 전체 계측은 대형 렌더·계절 출력 포함 XML **59/59 통과**, 2분 19초(65 task 중 1 executed/64 up-to-date)였다. 최초 명령은 PowerShell의 `-P` 인수 분리로 테스트 전 실패했으며 수정해 완료했다.
- 배포 상태: Play 업로드/게시와 `main` 반영은 수행하지 않았다. 공개 정책 URL 404·폰트 권리·실기기 검증 및 Console 확인이 남아 있어 출시 완료로 판정하지 않는다. API 26/28 원격 실패는 아래 사용자 범위 변경에 따라 추가 에뮬레이터 검사의 관문으로 요구하지 않되 미해결 사실을 보존한다.
- 후속 사용자 범위 변경: 에뮬레이터 추가 테스트를 제외한다. 이미 실행한 API 37 결과와 원격 API 26/28 실패는 삭제하거나 통과로 바꾸지 않았다. `.github/workflows/android-ci.yml`의 계측 매트릭스는 `workflow_dispatch` 수동 실행으로 남기고 push/PR은 빌드·JVM·Lint·R8 QA만 수행하도록 했다. 원격 실패의 원인과 API 26/28 사진첩 권한 경로는 여전히 미검증이다. 이 변경은 검사 통과가 아니라 범위 조정이다.
- 글꼴 공개 배포본 추가 대조: GC컴퍼니 공식 ZIP의 `Jalnan2TTF.ttf`와 앱 파일 SHA-256 `bd0028223d1e69c0bf76266bee52931be26a5f8a9fce517bb8765371d57836a3`이 일치했다. 공식 사용 가이드 3쪽은 앱 임베딩/번들 시 저작권 안내와 라이선스 전문 포함 조건을 제시한다. 현재 고지가 없어 이 1개도 출시 승인하지 않으며, 나머지 12개 원본 일치는 미확인이다. 상세 출처는 IMAGE_ASSET_GUIDE와 출시 보고서에 기록했다.
- 범위 변경 뒤 원격 CI: `263c2fea6f7db8e05c5024706565c2896db495ea`의 [실행 35727793916](https://github.com/sin-jun-woo/Pocket-4Cut-Android/actions/runs/35727793916)은 build **success**, instrumented **skipped**로 완료됐다. build job은 Debug 빌드·JVM·Debug Lint·R8 QA APK를 포함한다. 앱 소스 변경은 이전 `9ebe100` 이후 없으며 skipped를 계측 통과로 해석하지 않는다.

## 2026-09-23 — Notion 프로젝트 위키 및 전체 이력 아카이브 구성 (Asia/Seoul)

- 요청/범위: 지정된 Pocket 4Cut Notion 워크스페이스에 최신 프로젝트 명세·아키텍처·디자인·개발·릴리스 문서, 전체 커밋, 날짜별 검증 보고서, 원문 문서, 이전 홍보물과 결과물을 포함한 날짜별 업데이트를 구성했다. 앱 소스·설정·버전·자산은 수정하지 않았고 저장소 변경은 이 작업 기록뿐이다.
- 기준: 시작 분석 및 원문 이관 기준은 `78194b8b452e63ad84418a706864f3f8b393aa52`, `codex/release-readiness`, 1.5(6)이다. 기존 사용자 변경이 커밋된 뒤 clean 상태를 재확인했다. `main`의 `4527a05`와 최신 앱 소스 브랜치를 구별했고 원격 6개 브랜치·비 shallow 이력을 확인했다.
- 구성: 제품·사용자 경험, 아키텍처·데이터, 디자인·브랜드, 개발·품질, 릴리스·운영, Git 커밋 기록, 검증 보고서, 원문 문서 라이브러리, 디자인·홍보 아카이브, 업데이트 내역의 10개 카테고리다. 현재 동작 설명과 초기 설계, 과거 검증 결과, 확인되지 않은 출시 조건을 분리했다.
- 원문: Markdown 64개·TXT 12개·SRT 5개·HTML 3개의 84개 경로를 조사했다. 83개는 본문을 수록했고 화면 구현 HTML 1개는 시안 이미지와 원본 링크로 연결했다. 문서성 JSON 55개는 전문 46개·동일 본문 대응 3개·서비스 요청/작업 로그 제외 6개로 분류했다. 문서 원장은 총 130개 고유 경로이며 Notion 재조회로 누락·중복이 없음을 확인했다. 개인 로컬 경로 1곳과 서비스 작업 ID 1곳은 공유용 사본에서 가렸다.
- 작업/검증 이력: 최초 수집한 102개 커밋의 SHA·날짜·파일 수·추가/삭제 줄 수를 전체 대조해 차이 0건을 확인했다. 최초·최신·264파일 최대 변경 커밋의 메시지·시각·전체 파일 목록도 표본 대조했다. 커밋 날짜 요약 10개와 별도로 종합 업데이트 12개 작업일, 검증 보고서 22건을 구성하고 기존 WORKLOG의 날짜별 23개 작업 구역을 보존했다. 이 문서화 커밋은 최초 수집 102개에 추가되는 별도 기록이다.
- 결과물: 디자인·홍보 14개 제작 묶음에 현재 파일 588개와 덮어쓰기 이전 파일 버전 58개를 연결했다. 앱 이미지·글꼴 38개, 삭제된 초기 런처 WebP 10개, engineering QA 증거 이미지 25개도 구분해 보존했다. 현재 비코드 확장자 파일 614개의 문서/자산 목록 합집합을 대조했고 빠진 경로가 없음을 확인했다. 각 범위에는 중복이 있으므로 개별 수를 합산해 고유 파일 수로 해석하지 않는다. 새 이미지·영상·음성·폰트는 생성하거나 변경하지 않았다.
- 실행한 확인: `git status --short`, 브랜치/최근 커밋/원격 참조/전체 이력 조회, 파일별 원문·해시·경로 대조, Notion DB 페이지네이션 전수 조회, 문서 84개 및 디자인 14개 본문 재조회, 긴 JSON과 보고서 표본 검사, 실제 Notion 홈·홍보 갤러리 화면 검수를 수행했다. 문서 재조회에서 잘림·미지원 블록이 없고 제목·끝 문장·출처를 확인했다. 긴 JSON은 Notion의 공통 들여쓰기 정규화와 데이터 값·키·순서를 구분해 검증했다.
- 검사 경계: Android 앱 변경이 없어 Gradle 빌드·JVM·Lint·기기 계측을 재실행하지 않았다. 기존 1.4(5) QA/기기/영상 검사 기록을 현재 1.5(6)의 새 검증이나 Play 배포 완료로 표현하지 않았다. Notion 날짜 속성은 분 단위이며 커밋 본문에 초 단위 원본 ISO 시각을 보존했다. 작업일·제작일·Git 기록일의 차이를 표시했고 자동 동기화는 설정하지 않았다.
- Git: 이 WORKLOG만 명시적으로 stage하여 공백 검사와 diff를 검토한 뒤 현재 작업 브랜치에 일반 커밋·push한다. 최종 SHA·원격 반영 여부는 완료 보고에서 확인하며, 자체 SHA를 넣기 위한 amend는 하지 않는다.

## 2026-09-23 — Notion 문서 전수 재대조와 제작 기록 보강 (Asia/Seoul)

- 요청/범위: 저장소 전체 문서와 Notion의 수록 상태를 다시 확인하고 부족한 원문·분류·날짜별 연결을 보강했다. 기준 HEAD는 `ab0751eee5214bfe08ffea46572325c0ad545f49`, 브랜치는 `codex/release-readiness`, 앱 버전은 1.5(6)이다. 작업 시작 시 tracked/untracked/staged 변경은 없었다. 앱 소스·설정·자산은 변경하지 않았다.
- 원문 전수: Markdown 64개·TXT 12개·SRT 5개·HTML 3개는 기존 84개 경로와 대조해 누락 0건이다. 문서성 JSON 55개 중 기존 46개 전문에 음성 제작 요청·ASR·생성/로컬 검증 6개를 추가해 52개 전문을 수록했다. 나머지 3개는 전문이 동일한 복제 경로로 원본 페이지에 연결했다. 따라서 139개 문서성 경로가 Notion 원문 136개와 복제 링크 3개로 대응한다. 서비스 작업 ID 3곳과 임시 결과 URL 1곳만 가린 공유용 사본이며, 원본 JSON 키·값·순서와 끝부분을 재조회했다.
- 탐색 보강: 원문 라이브러리의 누락 안내·집계와 문서 지도의 26개 내부 페이지 링크를 갱신했다. 9월 10일 업데이트에 음성 JSON 5개, 9월 22일에 1개를 직접 연결하고 9월 23일 업데이트에 재대조 기록을 덧붙였다. 개발·품질 영역에는 계측 테스트 21개 파일, JVM 기본 검사, 개발 스크립트와 CI 워크플로의 위치·목적·실행 범위·한계를 정리한 테스트·도구 실행 지도를 추가했다. 기존 날짜별 보고서 22건과 홍보·디자인 자산 목록은 보존했다.
- 검증: `git status --short`, `git branch --show-current`, `git diff --cached --name-status`, 저장소 파일·Git 이력·Notion 원장 경로 대조, 추가 JSON 6개와 날짜별 링크·문서 지도·홈을 다시 조회했다. 기존 출시·검증 원문 46경로와 관련 Markdown 28개 1,719행도 별도로 대조해 누락 0건을 확인했다. 앱 코드 변경이 없어 Android 빌드·JVM·Lint·계측·실기기 검사를 이번 작업 결과로 새로 주장하지 않는다.
- 경계: 초기 설계와 현재 구현을 분리하고, 과거 1.4(5) 검증을 현행 1.5(6) 검증으로 올려 적지 않았다. 저장소 README와 `docs/ARCHITECTURE.md`에 남은 1.4(5) 표기는 해당 원문에 현재 버전과의 차이를 표시했다. Notion은 이 시점의 수동 스냅샷이며 후속 Git 변경의 자동 동기화는 설정하지 않았다. 새 이미지·영상·음성·글꼴은 제작하지 않았다.

## 2026-09-23 — Notion 저장소 전수 지도 보강 (Asia/Seoul)

- 기준/목적: `429e48b08c099f891c8c5a824ba5b806256e263a`에서 문서 원문뿐 아니라 Git 추적 파일 전체가 Notion의 설명·원장·고정 소스에 대응하는지 다시 확인했다. 앱 코드 기준 1.5(6) · `78194b8`은 바꾸지 않았다.
- 보강: 개발·품질 영역에 런타임 Kotlin 91개, Manifest/리소스 27개, 테스트·입력 26개, 앱 자산 19개, 앱 설정 3개, 디자인 제작 도구 37개, 빌드·CI·도구 설정을 연결한 소스 코드·설정·저장소 전수 지도를 추가했다. 테스트 지도에는 기본 소스셋 밖의 제작·진단 Kotlin 2개와 잘못된 도구 링크를 보완했다.
- 전수 대조: `git ls-files` 871개를 `app` 166, `design` 631, `engineering` 35, `docs` 10, 그 밖의 루트·Gradle·CI·scripts 29로 분류했다. 다시 문서 139경로, 디자인 제작 도구 37개, 디자인 미디어·패키지 485개, engineering 증거 25개를 분리했으며 분류 합계와 고유 경로가 모두 871개로 일치했다.
- Notion 확인: 문서 원장 136행과 동일 본문 대응 3개, 커밋 원장 104행, 디자인·홍보 14묶음/결과물 수 588, 검증 보고서 22건, 업데이트 12일을 조회했다. 새 지도에는 런타임 소스 91개와 디자인 제작 도구 37개의 고유 링크가 모두 있고, 잘림·미지원 블록이 없음을 재조회와 화면으로 확인했다.
- 검증 경계: 외부 문서화와 경로 대조 작업이므로 Android 빌드·JVM·Lint·계측·실기기 검사를 새로 실행하지 않았다. Git이 추적하지 않는 `build/`, 캐시, 비밀 서명 정보, 사용자 사진과 원시 로그는 프로젝트 스냅샷 871개에 포함하지 않았다.
