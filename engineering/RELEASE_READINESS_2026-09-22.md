# Pocket 4Cut Android 출시 준비 검증 보고서 — 2026-09-22

## 판정 원칙과 범위

이 보고서는 [수정 전 에뮬레이터 감사](EMULATOR_AUDIT_2026-09-22.md)의 E01–E10·C01–C15와 [이전 개편 검증](REFACTOR_VERIFICATION_2026-09-22.md)의 잔여 조건을 **현재 Android 저장소의 실제 코드와 새 실행 결과**에 대조한다. 이전 보고서는 기준 기록으로 보존했다. 시작 HEAD는 `6c815cb2ce3c665989e80512ffde1e71f268f86a`, 시작 브랜치는 `codex/seasonal-frames`, 작업 트리는 clean이었다. 이 변경은 `codex/release-readiness`에서 작성했다. 시작 HEAD에는 Paper Seasons 프레임·홍보 자산과 이전 신뢰성 개편이 이미 포함돼 있다. 따라서 이전 보고서의 38개 계측 결과를 이번 실행 결과로 합산하지 않는다.

여기서 **코드 구현**, **자동 검사 통과**, **API 37 기기에서 확인**, **외부 확인 필요**는 서로 다른 증거다. 한 대의 에뮬레이터로 실물 카메라·모든 Android 버전·Play Console 상태를 검증했다고 판정하지 않는다. 출시 후보의 로컬 기술 검증과 실제 Google Play 게시 승인은 별도 관문이다. 새 이미지 에셋은 만들지 않았고, 기존 벡터 스티커와 Paper Seasons atlas를 사용했다.

## 이번 작업의 구현 결과

### 촬영·복구와 원본 보존

- 촬영 중 회전·화면 재생성에서 오래된 `LifecycleOwner`와 `PreviewView`를 ViewModel이 강하게 붙잡는 경계를 줄였다. 시스템 뒤로가기는 촬영을 멈추고, 늦은 컷은 기존 저장 계약에 따라 처리한다.
- 기록되지 않은 게시 JPEG가 손상되면 해당 슬롯을 점유한 것으로 처리하지 않는다. 사용자에게 손상 상태를 보이고, **명시적으로 격리를 선택한 경우**에만 그 파일을 앱 소유 `.quarantine`에 원본 바이트 그대로 옮긴 뒤 같은 컷부터 다시 촬영한다.
- 이미 세션에 기록된 JPEG가 누락·손상되거나 게시 파일 번호가 비연속이면 자동 append·재촬영을 막는다. 남아 있는 정상 사진과 문서는 보존하고 수동 확인 상태로 남긴다. 유효한 연속 게시 파일은 재실행 시 한 번만 세션에 반영한다.
- 카메라 프리뷰의 접근성 클릭 경로를 연결했다. 회전 중 새 CameraX 화면 바인딩 경합은 별도 코드·기기 검증 결과에 따라 아래 미완료 조건을 판정한다.

### 편집·결과·보관함

- 일반/상세 편집의 시스템 뒤로가기를 저장 완료 경로에 연결했다. 지연 문구 입력을 즉시 이탈 시 flush하고, 빠른 중복 화면 이동을 막는다.
- 커스텀 프레임 화면은 장식을 Compose 오버레이로 따로 그리지 않고 결과 JPEG와 같은 `drawScene`에 전달한다. 문구는 논리 폭·최대 2행·끝 말줄임으로 preview/export가 동일하게 처리한다. 커스텀 화면에서 뒤로간 후 재진입할 때 방금 저장한 디자인을 다시 사용한다.
- 결과 게시의 손상·빈 journal 또는 잘못된 JPEG는 기존 완료본/원본과 분리해 감지한다. 보관함은 정상 작업을 계속 보여 주며, 사용자가 확인한 **미완료 결과 후보만** 안전한 격리 위치로 옮긴다. 격리 중 종료된 작업은 intent로 재실행할 수 있다. 손상된 본문 JSON은 이전 정상본 복구와 구분한다.
- 서명된 출시 번들을 새로 빌드하고 Manifest의 배포 ID·버전·권한을 확인한다. 원격 CI는 `codex/**` push에서도 시작하도록 하고 API 26/28/29/33/36 계측 매트릭스를 정의했다.

### 배포 자료의 정합성

- 앱 내·GitHub Pages HTML·Markdown 개인정보 처리방침의 세션별 JSON, 원본/결과, 사진첩 사본, API 26–28 저장소 쓰기 권한, 설정만 포함하는 백업 범위를 현재 코드에 맞췄다.
- Play 출시 문구를 `1.4 (versionCode 5)`와 현행 기능 기준으로 고쳤다. 실제 Console 업로드·게시 여부, 데이터 안전성 신고, 기존 서명/버전 충돌은 저장소에서 확인할 수 없으므로 출시 완료 문구로 쓰지 않았다.

## 출시 판정에 필요한 증거의 경계

Android 배포 설정은 `minSdk 26`, `targetSdk 36`, `compileSdk 36`, 배포 ID `com.pocket4cut`, 버전 `1.4 (5)`다. Google Play의 [2026년 8월 31일 이후 앱·업데이트 대상 API 규칙](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)은 API 36 이상을 요구하므로 현재 코드상의 `targetSdk` 값은 이 요구를 충족한다. 이 사실은 다른 Play 심사 요건이나 기존 앱과의 서명 호환을 보증하지 않는다.

`debug`와 R8 `qaRelease`는 `com.pocket4cut.qa`로 설치해 배포 앱 데이터를 건드리지 않는다. 릴리스 AAB는 빌드·서명·Manifest 확인 대상이며, 실제 스토어 업로드/게시를 이 보고서의 로컬 테스트로 대체하지 않는다. 로컬에는 API 37 `Pixel_10_Pro` AVD 하나만 확인했다. 나머지 API와 물리 전·후면 카메라, TalkBack 실사용, 실제 클라우드 백업·기기 이전은 별도의 실행 증거가 필요하다.

앱에 포함된 TTF 13개의 파일별 공식 원본·라이선스 전문은 저장소에서 찾지 못했다. [네이버 글꼴 정책](https://hangeul.naver.com/font)은 번들 시 저작권 안내와 라이선스 전문을 포함하도록 안내하고, [카페24 안내](https://cafe24.zendesk.com/hc/ko/articles/18299360301593-%EC%B9%B4%ED%8E%9824%EC%97%90%EC%84%9C-%EC%A0%9C%EA%B3%B5%EB%90%98%EB%8A%94-%ED%8F%B0%ED%8A%B8%EB%A5%BC-%EC%82%AC%EC%9A%A9%ED%95%A0-%EC%88%98-%EC%9E%88%EB%82%98%EC%9A%94)는 카페24 폰트의 소프트웨어 번들을 허용한다고 설명한다. 이 일반 정책이 현재 파일 13개 각각의 출처·수정 여부·고지 충족을 증명하지는 않는다. 파일을 임의로 제거하거나 허가를 추정하지 않았다.

## E01–E10 재검토

| ID | 이번 코드·검사 근거 | 아직 통과로 표시하지 않는 범위 |
| --- | --- | --- |
| E01 스티커 이름 출력 | 기존 공통 vector painter를 유지하고 25개 ID를 새 렌더 계약 검사에 포함했다. | 각 기기·폰트 배율에서 25종 최종 JPEG 육안 검수. |
| E02 문구·날짜 위치 | caption/date가 실제 입력에 따라 같은 공간을 예약하고 8배치×4필터×4문구 조건 128개 투영 계약을 검사했다. | 모든 글꼴·이모지·최대 글자 크기의 시각 검수. |
| E03 필름 블랙 색 | 모든 `FrameColors` ID 45개의 왕복과 색 구별 렌더 검사를 추가했다. | 실제 화면 색 관리·인쇄물 일치. |
| E04 선택 소실 | 이전 PhotoId 저장·복원 계약을 유지하고 일반 편집 즉시 이탈의 문구 flush 계측을 추가했다. | 선택 직후 실제 프로세스 종료·화면 회전의 전 타이밍. |
| E05 커스텀 프레임 | 커스텀 화면을 결과와 같은 painter로 전환하고, 이탈 시 현재 디자인을 저장하며, 같은 route 재진입 시 최신 디자인을 사용한다. | 다중 제스처 직후 회전/프로세스 종료, 2배 글자/최소 화면의 선택 표시. |
| E06 권한 복귀 | `ON_RESUME` 권한 재확인 코드를 유지한다. | 설정 앱에서 거부→허용→복귀하는 최신 APK 수동 재현. |
| E07 사진첩 중복 | 결과 ID별 MediaStore 작업/URI 계약과 기존 3개 exporter 계측을 유지한다. | insert·복사·게시 순간의 실제 프로세스 종료와 사진첩 행 수 검사. |
| E08 계절 경계 | 공통 painter와 Paper Seasons의 144개 safe-area/72개 투영 계약을 유지한다. | 모든 최종 JPEG의 육안 경계 검수와 저메모리 동시 렌더. |
| E09 권한·시스템바 | 권한 대비/시스템바 설정과 접근성 의미 계측을 유지하고 Camera Preview 터치 접근성 경고를 고쳤다. | TalkBack 완주, 가로/작은 화면/2배 글자 전체 UI. |
| E10 Home 중 촬영 | 일시정지 상태 머신, 늦은 callback 보호, 게시/기록 재개 검사를 유지·확장했다. | 카운트다운·셔터 콜백·컷 간 대기 각각의 Home/잠금/회전과 물리 카메라. |

## C01–C15 재검토

| ID | 이번 코드·검사 근거 | 아직 통과로 표시하지 않는 범위 |
| --- | --- | --- |
| C01 사진 순서 | PhotoId 순서와 `RenderSnapshot` 계약, 숫자 합성 사진/렌더 계약을 유지한다. | 실제 촬영 숫자 사진의 선택→재정렬→저장 E2E. |
| C02 원본 재압축 | pending→검증→게시의 원본 byte 보존 계약과 손상 컷 격리/재촬영 계측을 추가했다. | 실기기 EXIF/센서별 원본 해시와 모든 중단 지점. |
| C03 세션 손상 | AtomicFile/last-good와 결과 journal 재생을 유지하고 손상 후보별 격리·재시작 intent, 정상 작업 병행 표시를 추가했다. | 전체 저장 공간 부족·다중 프로세스·모든 파일 쓰기 단계 실패 주입. |
| C04 삭제 소유권 | tombstone/앱 소유 경로 검사와 격리 자료 정리를 확장한다. | 소유 관계를 읽을 수 없는 손상 기록의 물리 삭제는 데이터 보존을 우선하므로 자동 성공으로 간주하지 않는다. |
| C05 구형 저장 권한 | Manifest에 API 28 상한의 `WRITE_EXTERNAL_STORAGE`만 있고 UI 요청 경로를 유지한다. | API 26/28 실기기·에뮬레이터에서 거부/재허용/재시도. |
| C06 취소·메모리 | preview revision/Bitmap 수명 검사를 유지하고 16MP급 결과 소유 메모리를 opt-in 계측으로 측정했다. | 256MiB 물리 기기와 UI·플랫폼 decoder/encoder/계절 atlas 동시 peak, 낮은 메모리에서 안전 실패. |
| C07 장식 회전·배율 | 라디안 저장, Canvas 공통 장식 렌더, 커스텀 텍스트 2행/폭 계약을 확인했다. | 다중 손가락 제스처의 정량 픽셀 비교와 모든 글꼴. |
| C08 비대칭 6컷 | version 2 대표 2×2 슬롯과 구형 version 1 복원 검사를 유지한다. | 실제 10장 촬영→6장 선택→최종 JPEG 기기 E2E. |
| C09 오류·취소 | 촬영 미기록 손상/순서 불일치/기록 원본 누락을 구분하고 보존·중단·격리 경로를 추가했다. | CameraX 장치 연결 실패, 실제 디스크 부족, OS 강제 종료 모든 단계. |
| C10 프로세스 복원 | 세션 ID·문서 복원과 편집 이탈 저장을 유지·확장했다. | debounce 직후 강제 종료, 커스텀·상세 편집 각 단계 실제 프로세스 종료. |
| C11 백업 범위 | legacy/cloud/device-transfer 규칙이 설정 두 파일만 포함하는 패키지 계측 2개를 유지한다. | 실제 계정 백업·기기 이전 후 복원. |
| C12 설정·접근성·CI | 카운트다운 설정·QA ID를 유지하고 CI를 API 26/28/29/33/36까지 넓혔다. | 원격 실행 결과, TalkBack/2배 글자/가로 전체 조작. |
| C13 후속 색·그라데이션 | catalog 45색과 필터 4종, 공통 배경 입력을 검사했다. | 커스텀 디자인 이후 모든 색의 저장·재열기 UI. |
| C14 MediaStore 부분 실패 | 기존 상태 journal/URI 검증과 pending 재개 계측을 유지한다. | API 26–29 provider 실패 및 실제 insert 직후/게시 직후 프로세스 종료. |
| C15 계절 좌표 | Paper Seasons safe-area/투영 계약과 대표 결과물 검토 기록을 유지한다. | 모든 장식·글꼴 조합과 물리 화면의 경계 검수. |

위 표에서 **검사 코드가 존재한다**는 사실과 **이번 빌드에서 실행·통과했다**는 사실은 아래 실행 기록으로 다시 구분한다. 조합 수는 scene의 투영/기하 assertion이며 128개 독립 기기 E2E가 아니다. 원본 감사의 25개 항목을 무조건 `완료`로 바꾸지 않는다.

## 최종 로컬 실행 기록

검사는 2026-09-22 KST에 `codex/release-readiness`의 모든 앱 소스 변경을 반영한 작업 트리에서 실행했다. Windows/JDK 21, SDK 36, API 37 `Pixel_10_Pro` AVD 한 대를 사용했다. Gradle은 이미 내려받은 의존성으로 `--offline` 실행했다. 테스트 APK와 최적화 QA APK는 `com.pocket4cut.qa`이며 배포 앱 `com.pocket4cut`과 데이터가 분리된다. 아래의 Gradle `UP-TO-DATE`는 기존 산출물 재사용이고, 계측 XML과 수동 조작은 이번에 새로 실행한 결과다.

| 검증 | 실행 명령 또는 증거 | 결과와 해석 |
| --- | --- | --- |
| Debug·R8 QA·JVM·Debug Lint·release AAB | `./gradlew.bat :app:assembleDebug :app:assembleQaRelease :app:testDebugUnitTest :app:lintDebug :app:bundleRelease --offline --console=plain` | **BUILD SUCCESSFUL**, 4분 44초. 142 task 중 32 executed/110 up-to-date. JVM XML은 1/1 통과. Debug Lint XML은 오류 0, 경고 56, 힌트 2. |
| 기본 전체 계측 | `./gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain` | **BUILD SUCCESSFUL**, API 37 결과 XML 59개 중 57 통과·2 opt-in 건너뜀·실패/오류 0. Gradle 출력의 총계 문구 대신 결과 XML의 `tests=59`를 기준으로 삼았다. |
| 대형 렌더·계절 내보내기 포함 전체 계측 | `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.renderMemoryStress=true -Pandroid.testInstrumentationRunnerArguments.seasonalExport=true -Pandroid.testInstrumentationRunnerArguments.seasonalSourceRevision=release-readiness-worktree --offline --console=plain` | **BUILD SUCCESSFUL**, 결과 XML **59/59 통과**, 실패·오류·건너뜀 0. 계절 출력은 실제 renderer에서 투명 PNG 72장과 가상 성인 사진 합성 JPEG 32장, manifest를 생성했다. 이들은 `app/build/outputs/connected_android_test_additional_output/`의 검사 산출물이며 이번 커밋의 신규 앱 자산이 아니다. |
| 릴리스 변형 Lint | `./gradlew.bat :app:lintRelease --offline --console=plain` | **BUILD SUCCESSFUL**, 1분 32초. Release Lint XML 오류 0, 경고 41, 힌트 2. Debug의 56개와 variant별 검사 범위가 달라 동일 건수로 해석하지 않는다. |

전체 계측 XML에는 백업 규칙 2, Bitmap 소유권 2·파이프라인 4, 촬영 손상/게시/순서 복구 5, 출력 기하 3, 기존 진단 7, 초안 즉시 이탈 1, MediaStore 3, 접근성 5, 렌더 출시 계약 4, 결과 게시 4, 계절 계약·미리보기·구형 배치 7, 세션 저장소 11, 기본 패키지 확인 1이 포함된다. UI에서 실제 촬영을 59번 수행했다는 뜻이 아니다. 새 저장소 시험은 숨긴 손상 기록의 재표시와 삭제 차단, 완료본/원본 보존, 중단된 결과 격리 재생을 포함한다.

최대 출력 opt-in 검사는 `SIX_COLLAGE` **3265×4898px** 결과와 한 번에 한 장의 3000×2000 입력 Bitmap을 사용했다. 테스트가 계산한 renderer 소유 `결과 + 입력 1장` 최대치는 **87,967,880 bytes**로 정한 128MiB 계약 아래다. API 37 에뮬레이터의 앱 `memoryClass`는 192MiB이고, 검사 중 읽은 전체 프로세스 PSS 표본 최대치는 **283,263KiB**였다. PSS에는 Android 런타임·UI·디코더·atlas 등이 섞이며 순간 peak가 아니다. 따라서 이 숫자로 256MiB 실기기의 안전성, OOM 부재, 최저 메모리 환경의 실패 복구를 합격 처리하지 않는다.

### R8 QA 앱 수동 흐름

[QA 설정 화면](release-evidence/2026-09-22/qa-r8-settings.png)에는 검사 APK의 버전 `1.4-qa-r8 (5)`와 카운트다운 기본 3초가 표시된다. 중단 시험을 위해 QA 설정에서만 10초로 바꾼 뒤 시험 종료 시 3초로 복원했다.

최종 `app-qaRelease.apk`를 API 37에 재설치했다. 앱 설정 화면에서 `1.4-qa-r8 (5)`를 확인했다. `2컷`에서 가상 카메라 합성 장면 **4장 촬영 → 2장 선택**을 완료했다. 선택 직후 `am force-stop`으로 실제 앱 프로세스를 끝내고 재실행한 뒤 홈의 `2컷 이어서 작업하기`를 눌렀을 때 선택 화면은 `선택 완료!`와 `다음` 상태를 유지했다. 세로 2컷, 필름 블랙, 필름 필터, `RELEASEQA` 문구, 날짜 표시, 첫 사진 90° 회전을 적용해 결과를 열었다. [실제 QA JPEG](release-evidence/2026-09-22/qa-r8-2cut-result.jpg)는 **1248×3307px / 126,145 bytes**, SHA-256 `58b021d0067e432ed0ea0d3600b72e0b818de644907b49960d2a6cf6ed0923`이다. 가상 장면이므로 피부색·센서 화질 검사가 아니다. 문구·날짜·검은 프레임·두 슬롯의 잘림은 이 대표 결과에서 육안 확인했다.

사진첩의 `Pocket4Cut_` 행은 저장 전 **1개**, 새 결과 저장 후 **2개**였다. 같은 결과에서 저장을 다시 눌러도 **2개**, 앱 프로세스 종료·재실행 후 보관함에서 완료본을 다시 열어도 **2개**였다. 재열기 화면은 `저장 완료`로 표시됐다. [재열기 화면](release-evidence/2026-09-22/qa-r8-result-reopened.png). 공유 버튼은 Android `Sharing image` 시트를 열었다. 수신 앱이 URI를 열어 JPEG 바이트를 읽는 통합 검사는 수행하지 않았다. 이 행 수 검사는 결과 하나의 정상 경로에 한정하며 insert/게시 순간 강제 종료 전체를 포함하지 않는다.

별도의 2컷 초안에서 카운트다운을 **10초**로 설정하고 촬영 시작 0.5초 뒤 Home으로 이동했다. 복귀 시 `이어서 촬영` 버튼이 보였고 이미 경과한 카운트다운 때문에 자동 재개되지 않았다. [일시정지 화면](release-evidence/2026-09-22/qa-r8-capture-paused.png). 다시 `am force-stop`으로 종료하고 홈의 이어하기를 거쳐 촬영 화면을 열어도 `이어서 촬영`이었다. 카운트다운 중단 한 시점만 확인했으며 촬영 콜백·컷 간 대기·잠금·회전의 타이밍 매트릭스는 통과로 표시하지 않는다. 색상 칩 검수 중 텍스트 라벨을 터치했을 때 선택이 바뀌지 않았지만, 실제 원형 칩을 터치하자 필름 블랙이 선택되고 다음 편집 화면에도 유지됐다. 이는 최초 조작 좌표 오류로 판정했으며 색 저장 결함으로 보고하지 않는다. [칩 화면](release-evidence/2026-09-22/qa-color-selection.png).

### 배포 번들과 Manifest

최종 `app/build/outputs/bundle/release/app-release.aab`는 **38,276,058 bytes**, SHA-256 `5a9ddea0a3375c8a4e383b9168db30011994c2a430205119441c83c79d39f502`이다. JDK `jarsigner -verify`는 `jar verified`를 반환했고 공개 서명 인증서 SHA-256 fingerprint는 `93:DF:82:15:B0:1C:28:3F:81:65:FE:79:4D:C5:1A:23:E6:FC:F6:DD:A8:07:B2:D8:6C:01:4E:20:2D:A4:A5:E5`다. 자기 서명·타임스탬프 없음과 JAR/stream 해석 경고도 출력되었다. 로컬 서명 검증은 Play Console의 기존 업로드 키 fingerprint와 일치함을 증명하지 않는다. 개인 keystore 파일이나 비밀번호는 읽거나 문서·커밋에 넣지 않았다.

AGP가 생성한 release merged Manifest는 `com.pocket4cut`, `versionCode=5`, `versionName=1.4`, `minSdk=26`, `targetSdk=36`이었다. 앱 권한은 `CAMERA`와 API 28 상한 `WRITE_EXTERNAL_STORAGE`이고, 의존성의 내부 signature permission 외에 기존 사진 전체 읽기 권한은 없다. 카메라 hardware 선언은 optional이며 백업 규칙 두 파일이 Manifest에 연결된다. AAB가 Play에서 허용되는 기존 버전/서명 조합인지, 정책·데이터 안전성 양식이 맞는지는 이 파일만으로 판정할 수 없다.

## 릴리스 결정과 아직 닫히지 않은 관문

**이번 결과는 로컬 출시 후보 빌드와 API 37 제한 검증 통과이며, 최종 프로덕션 출시 승인은 보류한다.** 앱의 사진 손실·복구 가능성에 관한 주요 경계를 개선했고 정상 흐름을 실제 R8 QA 앱으로 수행했으나, 다음 조건은 계획의 완료 요건이면서 현재 증거가 없다.

1. **OS·물리 기기:** API 26/28/29/33/36의 실행 결과, 전·후면 물리 카메라 방향/미러링·발열·연속 촬영, 잠금·회전·권한 회수 각 촬영 단계, 실제 공유 수신 앱 JPEG 읽기. CI 파일에 매트릭스를 정의한 것과 CI가 통과한 것은 다르다.
2. **중단·리소스:** MediaStore insert/복사/게시의 강제 종료, 촬영·삭제·JSON 쓰기의 단계별 장애 및 공간 부족 주입, 백업/기기 이전 후 설정만 복원, 256MiB 및 더 낮은 메모리 기기의 앱 전체 peak/안전 실패. 최대 출력 단일 계측만으로 이를 대체하지 않는다.
3. **화면·접근성·이미지:** 실제 TalkBack 완주, 2배 글자/가로/작은 화면 전 흐름, 25개 스티커·45색·13개 글꼴·모든 계절 결과 JPEG의 저장/재열기 육안 검수. 자동 scene 투영 128조합과 대표 결과 한 장을 전체 수동 조합의 대체로 세지 않는다.
4. **배포·권리:** 원격 CI 전체 실행, Play Console의 패키지·기존 `versionCode`·업로드 키 일치, 앱 서명·데이터 안전성·스토어 설명·개인정보 URL·심사 트랙, 포함된 TTF 13개의 파일별 공식 원본·라이선스 전문·고지 확인이 필요하다. 글꼴은 파일명과 일반 배포 정책만으로 각 바이너리의 권리를 확정할 수 없다.
5. **Lint 경고:** 오류는 0이지만 Debug 기준 경고 **56**개를 남겼다. 분포는 `UseKtx` 18, 의존성/버전 알림 15, 미사용 리소스 8, `ModifierParameter` 6, 아이콘 관련 7, 기타 2다. 이전 Paper Seasons 기록의 경고 44개와 수가 다르다. 동일 Lint 기준으로 항목별 기준 대비 증가가 없는지 증명하거나 관련 경고를 처리하기 전에는 원 계획의 `새 경고 0`을 달성했다고 말하지 않는다.

요청한 25개 감사 항목은 위 표에 각각 코드·실행 근거와 미완료 범위를 남겼다. 실기기·원격 서비스·권리 자료가 확보되기 전에는 이 보고서를 출시 완료 확인서나 무결함 보증서로 사용하지 않는다. 현재 변경에는 새 프레임·스티커·아이콘·패턴 이미지 파일이 없고, 기존 Paper Seasons 자산을 재사용했다.
