# Pocket 4Cut Android 전면 감사 — 2026-09-22

## 결론과 증거 수준

현재 버전 `1.4 (5)`, 시작 HEAD `434584ec502e8349ebec0f6500b4d857ac26344e`, 시작 작업 트리 clean을 기준으로 **기능 코드를 수정하지 않고** 다시 조사했다. `Pixel_10_Pro` Android 17/API 37 에뮬레이터에 현재 debug APK를 새로 설치하여 2·4·6컷 촬영과 편집·결과·보관함을 실제 조작했다. 아래 **기기 재현**은 이번 실행에서 관찰했고, **진단 재현**은 이번에 실행한 계측 assertion, **코드 확인**은 현재 호출 경로의 의미, **조건부 위험**은 아직 장애를 재현하지 않은 결함 가능성이다. 이전 감사 문서의 실행 결과를 이번 결과로 합산하지 않았다.

가장 먼저 고칠 것은 미리보기와 최종 JPEG의 불일치, 선택·커스텀 편집 상태 유실, 자동 사진첩 저장 중복, 촬영 원본 보존이다. 리팩터링 전 세션 데이터 형식과 삭제 소유권을 확정해야 한다. 한 대의 가상 기기와 유한한 입력으로 가능한 모든 OS·기기·사진·중단 타이밍의 조합을 전수 검사했다고 주장할 수 없다. 아래 마지막 절에 미검증 축과 재검사 조건을 명시했다.

우선순위는 P1=결과 정확성·촬영 자료 보존·진행 상태에 직접 영향, P2=정상 사용의 신뢰성·접근성, P3=품질·구조 개선이다. 이 문서의 화면 증거 15개는 **에뮬레이터의 합성 카메라 장면**과 앱 UI만 담는다. 원시 로그와 나머지 임시 화면은 커밋하지 않았다.

## 실제 구조와 검증 기준

- Gradle 모듈은 `:app` 하나다. `MainActivity` → Compose `PocketNavHost`의 14 destination이 17 Screen 함수를 연결한다. CameraX 촬영, JSON 세션·편집 초안, 앱 전용 JPEG, SharedPreferences, MediaStore 사본이 실제 저장 경로다. 활성 최종 렌더는 `DetailEditViewModel` → `CollageRenderer` → `FileImageStorage`/`SessionRepository`이며 이전 `CollageFinalize`는 현재 UI 경로가 아니다. 자세한 기존 지도는 [현재 아키텍처](../docs/ARCHITECTURE.md)와 [코드 지도](CODE_MAP.md)를 참고한다.
- minSdk 26, target/compileSdk 36. 이번 에뮬레이터는 API 37, 1280×2856px, 밀도 480dpi의 전·후면 가상 카메라를 사용했다. 최초 설치 시 이 앱의 보관함과 MediaStore 항목은 비어 있었다. 설정 기본값은 전면 카메라 ON, 카운트다운 3초, 자동 사진첩 저장 OFF, 날짜 기본 표시 OFF다. 촬영 시간 실험에서 카운트다운을 1초로 바꾸었으며 큰 글자/다크모드/회전은 검증 뒤 원래 시스템값(1.0/라이트/자동 회전)으로 복원했다.
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest`, `:app:lintDebug`를 `--rerun-tasks --continue --offline`로 한 번에 실행했다. APK 및 androidTest APK 생성 성공, JVM 테스트 **1개 실제 실행·통과**. `lintDebug`는 **1 error, 77 warnings, 2 hints**로 실패했다. 차단 오류는 `AndroidManifest.xml:6`의 `PermissionImpliesUnsupportedChromeOsHardware`다. 결합 명령의 exit code는 1이며 이를 빌드 성공 전체로 표현하지 않는다.
- 기본 `:app:connectedDebugAndroidTest`는 에뮬레이터에서 **3개 통과**했다. 선택 실행 `RenderContractDiagnosticTest` 7개는 **3개 통과·4개 실패**했다. 통과는 8 배치×4 필터의 합성 사진 출력, 역순 Renderer 입력, 이전 UI가 보유한 Bitmap 수명이다. 실패는 8 배치×문구/날짜 3조건의 공간 계약, 카탈로그 색 ID 왕복, 스티커 도형 출력, `SIX_COLLAGE`와 3×2 그리드 기하 구분이다. 해당 테스트는 작은 합성 Bitmap이므로 실제 카메라/큰 사진의 메모리·크롭까지 입증하지 않는다. 계측 작업은 테스트 앱을 제거했으므로 UI 확인 뒤 동일 APK를 재설치했다. 이번 조작 구간 `logcat`의 앱 관련 `FATAL EXCEPTION` 패턴은 0건이었다. 크래시 0건이 모든 경합 안전성을 보증하지는 않는다.
- 자동 저장을 켜기 전 수동 저장된 2컷 JPEG는 MediaStore에서 **1280×1154px / 251,249 bytes**, 자동 저장 4컷은 **1280×1800px**, 6컷은 **1280×1275px**로 확인했다. 이는 이번 에뮬레이터 결과물의 측정값이다. 활성 일반 배치 출력 폭은 화면 픽셀 폭을 720–2160px로 제한하며, 클래식 세로 4컷만 1650×4920px이다([출력 폭](../app/src/main/java/com/pocket4cut/core/util/CollageExportMetrics.kt), [배치 수식](../app/src/main/java/com/pocket4cut/frame/CollageLayoutMath.kt)). 해상도 품질의 적정성은 인쇄 목표를 정해 추가 평가해야 한다.

재실행 명령은 아래와 같다. 현재 기기에 맞는 JDK/Android SDK를 설정해야 하며 `--offline`은 의존성이 이미 캐시에 있을 때만 사용한다. 계측 명령은 앱 설치·프로세스 상태에 영향을 주므로 진행 중인 편집을 먼저 마쳐야 한다.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --rerun-tasks --continue --offline --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain
.\gradlew.bat -I scripts/device-diagnostics.init.gradle :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.pocket4cut.diagnostic.RenderContractDiagnosticTest' --offline --console=plain
```

## 이번 에뮬레이터 실행 범위

- **첫 실행·설정·권한:** 빈 보관함, 설정값 표시, CAMERA 첫 거부 → 시스템 설정에서 허용 → 앱 복귀, 렌즈 전환과 줌 조작을 확인했다.
- **촬영·선택:** 2컷 4장/2장, 4컷 8장/4장, 6컷 10장/6장으로 실제 진행했다. 2컷에서 선택 한도 초과, 회전, 레이아웃에서 뒤로 가기; 4컷은 촬영 중 Home 전환/복귀를 조작했다.
- **배치·프레임:** 2컷 가로, 4컷 그리드, 6컷 ‘콜라주’ 선택 후 편집까지 진행했다. 색 카탈로그 ‘필름 블랙’, 커스텀 배경·스티커, 여름 계절 프레임을 사용했다. 다른 배치/계절은 목록과 진단 범위에서만 확인했으며 모든 조합을 눈으로 비교한 것은 아니다.
- **편집·결과:** 흑백 필터, `QA2026` 문구, 날짜 ON, 번호로 사진 순서 교체, 사진별 90° 회전, 결과 저장, 공유 시트, 보관함 재진입과 자동 저장 중복을 확인했다. Android 공유 시트는 열렸지만 외부 앱에 실제 전송하지 않았다.
- **시스템 상태:** 선택·커스텀·결과 화면 회전, 4컷 촬영 중 Home 왕복, 시스템 다크모드와 글자 크기 1.5배 조합을 확인했다. 홈의 큰 글자는 버튼이 보였지만 다크모드 상태 아이콘 문제가 나타났다.

## 기기에서 재현한 문제

### E01 · P1 · 커스텀 스티커가 최종 이미지에서 이름 글자로 바뀜

4컷 그리드의 커스텀 프레임에서 분홍 장식을 추가하면 커스텀/일반 편집 미리보기에는 도형이 보인다. ‘사진별 상세 편집’ → ‘적용’ 후 완성 화면에는 중앙에 큰 분홍 **‘무당벌레’ 글자**가 출력됐다. [편집 미리보기](audit-evidence/2026-09-22/55-custom-to-edit.png) / [결과](audit-evidence/2026-09-22/56-result-custom-sticker.png). 미리보기는 [StickerPalette.icon](../app/src/main/java/com/pocket4cut/frame/CollagePreview.kt)을 그리고, [최종 렌더러](../app/src/main/java/com/pocket4cut/frame/CollageRenderer.kt)는 `displayName`을 `drawText`한다. 실제 선택한 모든 스티커에 같은 종류의 오류가 발생할 구조다. **수정 기준:** 같은 벡터 경로·색·위치·크기·회전을 preview/export가 공유하고, 스티커 ID별 픽셀 비교 회귀 검사를 통과해야 한다.

### E02 · P1 · 문구·날짜가 인화지 내부 미리보기와 최종 결과에서 다른 위치

2컷 가로 편집에서 `QA2026`과 날짜를 켜면 문구는 **인화지 아래 별도 화면 요소**로만 보인다. 사진 영역 아래의 흰 인화지 내부에는 문구가 없다. 최종 결과 JPEG에는 인화지 안의 문구 영역이 생긴다. [편집](audit-evidence/2026-09-22/33-edit-preview-top.png) / [결과](audit-evidence/2026-09-22/36-after-detail-apply.png). [Preview 배치 어댑터](../app/src/main/java/com/pocket4cut/frame/CollageLayoutMath.kt)는 실제 캡션 대신 공백을 전달하고 공백을 없는 문자열로 처리한다. 별도 텍스트는 [EditScreen](../app/src/main/java/com/pocket4cut/presentation/edit/EditScreen.kt)이 표시한다. 진단은 **8 배치×3 문구 조건 24개**에서 preview/export 공간 계약 실패를 냈다. 클래식 세로 4컷에서는 문구 유무에 따라 사진 슬롯 높이도 달라진다. **수정 기준:** 프레임 내부 문구/날짜, 사진 크롭, 캔버스 크기를 같은 배치 계산에서 얻고 모든 배치에서 정규화 좌표가 일치해야 한다.

### E03 · P1 · 카탈로그 ‘필름 블랙’이 편집 화면에서 흰색으로 바뀜

2컷 색 프레임에서 카탈로그 **‘필름 블랙’**을 선택하면 해당 화면의 인화지는 검정인데, 다음 편집 화면은 흰 인화지와 ‘화이트’ 색상명이 표시된다. [선택](audit-evidence/2026-09-22/24-catalog-black-preview.png) / [편집](audit-evidence/2026-09-22/25-edit-after-catalog-black.png). [FrameColors](../app/src/main/java/com/pocket4cut/frame/FrameColors.kt)가 만든 `catalog_<theme.id>`를 `byId()`가 기본 색 목록에서 찾지 못해 흰색으로 되돌린다. 기본 팔레트의 별도 `black` 항목까지 실패했다는 뜻은 아니다. 진단에서 2·4·6컷의 catalog 블랙과 4컷 민트 **4개 ID**가 같은 문제를 보였다. **수정 기준:** 선택 가능한 모든 색 ID의 저장→복원 RGB가 같고 편집·결과도 같은 색을 사용해야 한다.

### E04 · P1 · 사진 선택이 회전·레이아웃 뒤로 가기에서 초기화

2컷에서 2/2장을 고른 뒤 가로 회전하면 같은 선택 화면이 **0/2**로 돌아온다. 두 장을 다시 선택해 레이아웃으로 갔다가 시스템 뒤로 돌아와도 **0/2**였다. [회전 전](audit-evidence/2026-09-22/18-two-selected.png) / [회전 후](audit-evidence/2026-09-22/19-selected-after-landscape.png) / [뒤로 간 후](audit-evidence/2026-09-22/21-selection-after-layout-back.png). [SelectionScreen](../app/src/main/java/com/pocket4cut/presentation/selection/SelectionScreen.kt)의 `load()` 재호출 때 [SelectionViewModel](../app/src/main/java/com/pocket4cut/presentation/selection/SelectionViewModel.kt)이 `selectedIndexes`를 비운다. 가로 화면의 썸네일도 매우 얇게 보였으나 스크롤·터치 가능성을 전부 측정하지 않았으므로 별도 레이아웃 점검 대상으로 둔다. **수정 기준:** 같은 sessionId 재로드·회전·뒤로 복귀는 선택 집합과 순서를 보존해야 한다.

### E05 · P1 · 회전하면 작성 중인 커스텀 프레임 단계·내용 소실

4컷 커스텀 프레임에서 검정 배경과 분홍 스티커를 만든 후 가로→세로 회전하자 ‘프레임 방식’으로 되돌아갔다. 커스텀에 다시 진입하면 흰 배경·스티커 없음으로 초기화됐다. [회전 전](audit-evidence/2026-09-22/48-custom-black-sticker.png) / [재진입](audit-evidence/2026-09-22/50-custom-reentered-after-rotate.png). [NavHost `flowStep`](../app/src/main/java/com/pocket4cut/presentation/navigation/PocketNavHost.kt)과 [커스텀 편집 상태](../app/src/main/java/com/pocket4cut/presentation/frameFlow/CustomFrameEditorScreen.kt)가 저장되지 않는 `remember`이고, 완료 콜백 전 초안 기록도 없다. **수정 기준:** 화면 회전·프로세스 재생성·뒤로 복귀 뒤 작성 중인 배경·장식·배치가 남아야 한다.

### E06 · P2 · 권한을 시스템 설정에서 허용하고 돌아와도 거부 화면 유지

카메라 권한을 처음 거부한 뒤 ‘설정에서 허용’으로 이동해 Android 앱 정보에서 CAMERA를 허용했다. 앱으로 돌아왔을 때 권한 거부 화면 그대로였다. 앱의 ‘권한 허용’을 다시 누르면 카메라가 열렸다. [복귀 화면](audit-evidence/2026-09-22/14-return-after-external-grant.png). [CaptureScreen](../app/src/main/java/com/pocket4cut/presentation/capture/CaptureScreen.kt)은 최초 진입과 권한 요청 콜백에서만 상태를 갱신하고 Settings 복귀 `ON_RESUME` 재검사가 없다. **수정 기준:** 외부 설정 복귀 즉시 실제 permission을 다시 읽고 카메라 준비 또는 명확한 거부 상태를 표시해야 한다.

### E07 · P2 · 자동 사진첩 저장이 결과 재열기·회전마다 중복 생성

처음 결과를 수동 저장해 MediaStore 1개가 된 뒤 자동 저장을 ON으로 하고 보관함의 **같은 2컷 1개**를 두 차례 열었다. 공용 사진첩에 동일 이미지 이름 `..._result.jpg`, `..._result (1).jpg`, `..._result (2).jpg`로 **3개**가 됐다. 6컷 결과 화면에서 가로→세로 회전만 했을 때는 **5→7개**로 늘었다. 보관함 세션 수는 각각의 경우 그대로였다. [ResultScreen](../app/src/main/java/com/pocket4cut/presentation/result/ResultScreen.kt)의 저장 여부는 `remember`이고 재진입마다 MediaStore에 새 행을 삽입한다. **수정 기준:** 세션별 내보낸 URI/완료 여부를 영속화하고 같은 결과 재열기·회전·프로세스 재생성에 대해 멱등해야 한다. 명시적인 ‘다시 사본 저장’은 별도 사용자 행동으로 정의한다.

### E08 · P2 · 계절 프레임의 사진 경계 점선이 미리보기와 결과에서 다름

6컷 ‘콜라주’+여름 프레임 편집 미리보기에는 사진 둘레 점선이 거의 보이지 않지만, 최종 결과에는 **6개 슬롯을 둘러싼 파란 점선**이 나타난다. [편집](audit-evidence/2026-09-22/62-six-summer-edit.png) / [결과](audit-evidence/2026-09-22/63-six-summer-result.png). [Preview](../app/src/main/java/com/pocket4cut/frame/CollagePreview.kt)는 점선을 사진 뒤에 그려 사진/clip에 가리고, [Renderer](../app/src/main/java/com/pocket4cut/frame/CollageRenderer.kt)는 사진 뒤에 점선을 위에 그린다. 이 경로는 다른 계절·배치에도 적용되어 확대 점검이 필요하다. **수정 기준:** 장식 레이어 순서와 clip 계약을 두 경로에서 동일하게 하고 계절별 screenshot/bitmap 비교를 통과해야 한다.

### E09 · P2 · 권한 안내의 낮은 대비와 다크모드 상태 아이콘 불일치

검정 카메라 권한 화면의 제목이 짙은 글씨여서 거의 읽히지 않았다([화면](audit-evidence/2026-09-22/14-return-after-external-grant.png)). 현재 색 토큰으로 계산한 제목 대비는 약 **1.22:1**, 설명도 약 **3.90:1**이다. 시스템 다크모드+1.5배 글자에서 홈 본문·버튼은 보였지만 **밝은 화면 위 상태바 시계·통신 아이콘이 흰색**으로 표시됐다([화면](audit-evidence/2026-09-22/67-home-large-font-dark.png)). 앱의 밝은 화면 테마와 [MainActivity](../app/src/main/java/com/pocket4cut/MainActivity.kt)의 자동 시스템바 스타일 선택이 따로 동작한다. **수정 기준:** 권한 화면 대비와 TalkBack 레이블, 밝은/검정 화면별 시스템바 아이콘 색을 기기 모드 양쪽에서 검사한다.

### E10 · P2 · 촬영 중 Home 왕복에도 촬영 수가 계속 진행

4컷 연속 촬영에서 화면 1/8일 때 Home으로 나갔다가 약 3초 뒤 복귀하니 촬영 진행이 **6/8**이었고 이후 8장 선택 화면에 도달했다. [복귀 화면은 로컬 원시 증거로만 보존]. 중단·재개 안내는 없었다. 이것만으로 백그라운드의 8장 모두가 정상 카메라 프레임이라고 단정할 수 없다. [CaptureViewModel](../app/src/main/java/com/pocket4cut/presentation/capture/CaptureViewModel.kt)의 반복 job은 ViewModel 수명으로 돌고 [CaptureEngine](../app/src/main/java/com/pocket4cut/camera/CaptureEngine.kt)의 카메라는 lifecycle에 묶여 있다. **수정 기준:** `ON_STOP`과 복귀 시의 정책(일시정지/중단/재시작)을 제품 요구에 맞게 결정하고 각 컷 파일의 유효성 및 누락·중복 처리를 검증한다.

## 현재 코드와 진단에서 확인한 추가 결함·위험

### C01 · P1 · 편집의 사진 순서가 상세 편집/최종 렌더로 전달되지 않음 — 코드 확인

[EditViewModel](../app/src/main/java/com/pocket4cut/presentation/edit/EditViewModel.kt)은 바꾼 `order`를 pending JSON에 저장하지만 [PocketNavHost](../app/src/main/java/com/pocket4cut/presentation/navigation/PocketNavHost.kt)는 상세 진입 시 원래 `selectedIndexes` 순서로 경로/Bitmap을 다시 만든다. 이번에는 번호 두 칸 교체 UI를 조작했으나 합성 장면의 사진들이 유사해 최종 픽셀의 순서 차이를 단말에서 독립적으로 입증하지 못했다. Renderer 자체는 역순 입력을 그대로 출력하는 진단이 통과했다. **수정 기준:** 안정적인 photo ID와 표시 순서·상세 보정 슬롯을 하나의 순서 모델로 묶고 서로 다른 숫자/색 fixture로 E2E 검증한다.

### C02 · P1 · 촬영 원본 JPEG를 같은 경로에서 축소·재압축 — 코드 확인

[CaptureViewModel](../app/src/main/java/com/pocket4cut/presentation/capture/CaptureViewModel.kt)이 CameraX JPEG 직후 [BitmapDecoding](../app/src/main/java/com/pocket4cut/core/util/BitmapDecoding.kt)의 `rewriteJpegMaxLongEdge()`로 **원본 파일을 덮어쓴다**. 상수는 긴 변 최대 2048px/JPEG 92이며 별도 원본이 없다. `compress()` 반환값과 정규화 실패도 호출부에서 확인하지 않는다. 실제 파일 손상·용량 부족은 주입하지 않았으므로 실패 사고는 조건부 위험이다. **수정 기준:** 원본 불변 보관, 편집용 proxy 별도 생성, 임시 파일→검증→원자적 교체 또는 실패 복구, 내보내기는 원본 기반으로 구현한다.

### C03 · P1 · 세션 JSON의 직접 전체 재쓰기와 복구 부재 — 조건부 데이터 위험

[SessionRepository](../app/src/main/java/com/pocket4cut/data/local/SessionRepository.kt)의 upsert/delete는 잠금 없는 `sessions.json` 전체 read-modify-write이며 `writeText()`가 같은 파일에 직접 기록한다. 개별 레코드 파싱 오류는 누락되고 전체 파싱 오류는 전파된다. 저장 중 종료, 공간 부족, 동시 저장/삭제가 일어나면 업데이트 손실이나 전체 인덱스 손상 위험이 있다. 이번 테스트는 기존 사용자 데이터에 고장 주입을 하지 않았다. **수정 기준:** 격리 fixture로 중단·부분 파일·경합을 재현하고 원자적 쓰기, 백업/복구, 스키마 버전을 설계한다.

### C04 · P2 · 초안·촬영 파일의 삭제 소유권 불명확 — 코드 확인

[PendingCollageStore](../app/src/main/java/com/pocket4cut/presentation/edit/PendingCollageStore.kt)는 `pending_collage_*.json`과 `frame_selection_*.json`을 만들지만 삭제 함수는 활성 코드에서 호출하지 않는다. [SelectionScreen](../app/src/main/java/com/pocket4cut/presentation/selection/SelectionScreen.kt)의 ‘모두 삭제’ 안내는 실제로 navigation back만 실행한다. [FileImageStorage](../app/src/main/java/com/pocket4cut/data/storage/FileImageStorage.kt)의 완성 세션 삭제는 초안을 지우지 않고 파일 삭제 성공도 확인하지 않는다. 이번 에뮬레이터의 완성된 2컷 세션에도 두 JSON이 남아 있는 것을 확인했지만 사용자 자료 삭제 동작은 수행하지 않았다. **수정 기준:** 촬영 원본/초안/결과/MediaStore 사본의 소유권과 취소·개별삭제·전체삭제 범위를 명시한 뒤 각 삭제 결과를 확인한다.

### C05 · P2 · API 26–28 사진첩 저장 권한 경로 공백 — 코드 확인, 구버전 미실행

minSdk 26이고 [Manifest](../app/src/main/AndroidManifest.xml)에 `WRITE_EXTERNAL_STORAGE`를 `maxSdkVersion=28`로 선언하지만 runtime 요청은 CAMERA뿐이다([CaptureScreen](../app/src/main/java/com/pocket4cut/presentation/capture/CaptureScreen.kt)). [ResultScreen](../app/src/main/java/com/pocket4cut/presentation/result/ResultScreen.kt)은 공용 MediaStore에 쓰므로 해당 구버전의 새 설치·권한 미허용에서 실패할 조건이다. 이번 API 37 성공을 API 26–28 성공으로 확장하지 않는다. **수정 기준:** API 26/28 에뮬레이터의 권한 거부·허용·재시도·앱 전용 저장을 분리해 검증한다.

### C06 · P2 · 사진 처리 작업의 취소·오래된 결과·메모리 경계 — 코드 확인, 압박 미실행

[EditViewModel](../app/src/main/java/com/pocket4cut/presentation/edit/EditViewModel.kt)과 [DetailEditViewModel](../app/src/main/java/com/pocket4cut/presentation/detailEdit/DetailEditViewModel.kt)은 필터/보정 Job을 교체하지만 완료 순서가 바뀌는 경우의 결과 수락 계약이 약하다. NavHost·편집 초기화의 이미지 디코드와 다수 Bitmap 소유권은 큰 사진·저메모리에서 별도 측정이 필요하다. 이전 UI가 보유한 Bitmap을 너무 일찍 `recycle`하는 **과거 문제는 현재 진단 통과**로 이번 결함에 포함하지 않는다. **수정 기준:** job 버전/취소, Bitmap 수명 소유자, 원본/저해상도 미리보기 메모리 예산을 명시하고 스트레스 검사한다.

### C07 · P2 · 장식 크기·회전 단위가 두 렌더러에서 어긋남 — 코드 확인

[CustomFrameEditorScreen](../app/src/main/java/com/pocket4cut/presentation/frameFlow/CustomFrameEditorScreen.kt)의 제스처 `rotationChange`(도)를 `rotationRadians`에 그대로 더한다. [CollagePreview](../app/src/main/java/com/pocket4cut/frame/CollagePreview.kt)는 layer scale과 실제 글자/아이콘 size에 scale을 중복 적용하는 반면 [CollageRenderer](../app/src/main/java/com/pocket4cut/frame/CollageRenderer.kt)는 한 번 적용한다. 제스처 2손가락 정량 측정은 에뮬레이터에서 하지 않았으므로 보이는 오류의 크기는 코드 근거다. **수정 기준:** 저장 회전 단위를 하나로 정하고 위치·크기·회전의 preview/export 정규화 좌표를 fixture로 비교한다.

### C08 · P2 · ‘자유로운 콜라주 6컷’이 3×2 그리드와 같은 기하 — 진단 재현

6컷 화면의 ‘가로 그리드 6컷’과 ‘콜라주 6컷’은 설명이 다르지만 [FrameLayouts](../app/src/main/java/com/pocket4cut/frame/FrameLayouts.kt)의 행·열·비율이 같고 [CollageLayoutMath](../app/src/main/java/com/pocket4cut/frame/CollageLayoutMath.kt)는 차이 필드를 쓰지 않는다. 이번 계측은 모든 셀 기하가 동일해 실패했다. **수정 기준:** 실제로 다른 레이아웃을 구현하거나 중복 선택지를 정리하되 저장된 layout ID 호환성을 유지한다.

### C09 · P2 · 오류를 화면에 전달하지 않는 경로와 취소 경합 — 코드 확인

[DetailEditScreen](../app/src/main/java/com/pocket4cut/presentation/detailEdit/DetailEditScreen.kt)은 최종 저장 실패 상태를 충분히 표시하지 않고, [GalleryViewModel](../app/src/main/java/com/pocket4cut/presentation/gallery/GalleryViewModel.kt)·[SettingsScreen](../app/src/main/java/com/pocket4cut/presentation/settings/SettingsScreen.kt)도 파일/파싱 실패의 사용자 복구 안내가 부족하다. 촬영에서는 `catch(Throwable)`이 취소를 실패로 취급할 수 있고 CameraX callback을 감싼 `suspendCoroutine`은 취소 전달 계약이 없다([CaptureViewModel](../app/src/main/java/com/pocket4cut/presentation/capture/CaptureViewModel.kt), [CaptureEngine](../app/src/main/java/com/pocket4cut/camera/CaptureEngine.kt)). 실제 실패 주입은 하지 않았다. **수정 기준:** 저장/읽기/촬영 실패를 상태·재시도·원본 보존 결과와 함께 UI에 드러내고 cancellation을 별도로 처리한다.

### C10 · P2 · 편집·촬영의 프로세스 재생성 복원 부족 — 코드 확인, 강제 종료 미실행

촬영 ViewModel은 초기 IDLE/`sessionId=null`이며 새 촬영에 UUID를 만든다. Edit는 pending 문구·필터·순서를 전부 복원하지 않고, Detail의 사진별 회전·밝기 등도 다시 중립값으로 시작한다([CaptureViewModel](../app/src/main/java/com/pocket4cut/presentation/capture/CaptureViewModel.kt), [EditViewModel](../app/src/main/java/com/pocket4cut/presentation/edit/EditViewModel.kt), [DetailEditViewModel](../app/src/main/java/com/pocket4cut/presentation/detailEdit/DetailEditViewModel.kt)). 회전에서 E04/E05를 재현했지만 저메모리 프로세스 종료 시각의 복원은 테스트하지 않았다. **수정 기준:** sessionId·현재 단계·선택/배열·장식·보정값을 복원 가능한 단일 초안으로 저장하고 `SavedStateHandle`/영속 저장의 책임을 분리한다.

### C11 · P2 · 백업에 사진이 포함될 수 있는데 정책 경계가 불명확 — 코드 확인, 실제 백업 미실행

[Manifest](../app/src/main/AndroidManifest.xml)는 `allowBackup=true`이고 [backup_rules](../app/src/main/res/xml/backup_rules.xml), [data_extraction_rules](../app/src/main/res/xml/data_extraction_rules.xml)에는 활성 사진 제외 규칙이 없다. 내부 JSON·설정과 앱 전용 사진의 백업 여부를 제품 정책·개인정보 문구와 맞춰야 한다. 실제 클라우드 업로드, 유출, 복원 실패를 확인한 것은 아니다. **수정 기준:** 백업 범위·크기·복원 시 절대 경로 재해석을 정의하고 테스트 계정의 가상 사진으로 백업/복원을 검사한다.

### C12 · P3 · 설정·접근성·제품 문구의 기준 정리 필요

[AppSettings](../app/src/main/java/com/pocket4cut/presentation/settings/AppSettings.kt)의 기본 카운트다운은 3초이고 사용자가 제공한 핵심 흐름은 10초 준비를 요구한다. 어떤 값을 제품 기본으로 할지 결정이 필요하다. 커스텀 장식 크기 슬라이더와 일부 번호/토글은 48dp 터치·스크린리더 설명을 전수 검증하지 않았다. UI는 다크모드에서 색 테마를 고정해 쓰므로 시스템바 문제 외 전 화면 대비 회귀도 필요하다. 기본 JVM 테스트는 `2+2=4` 수준이고 GitHub Actions에는 앱 빌드/린트/계측 CI가 없어 현재의 제품 계약을 지속적으로 보호하지 못한다. **수정 기준:** 요구 기본값, 접근성 스펙, 화면/저장 계약 회귀 테스트를 출시 기준으로 명문화한다.

### C13 · P2 · 커스텀 프레임의 후속 색상 선택과 특수 색상 표시가 출력에 반영되지 않음 — 코드 확인

[EditViewModel](../app/src/main/java/com/pocket4cut/presentation/edit/EditViewModel.kt)은 프레임 색상 선택을 바꾸지만 커스텀 프레임이 있으면 [EditScreen](../app/src/main/java/com/pocket4cut/presentation/edit/EditScreen.kt) 미리보기와 [CollageRenderer](../app/src/main/java/com/pocket4cut/frame/CollageRenderer.kt)가 `customFrameDesign.resolvedFillColor`를 우선한다. 따라서 색상 칩·표시명은 바뀌어도 실제 커스텀 배경은 그대로일 수 있다. 일부 그라데이션은 선택 칩에만 `gradientBrush`가 있고 Renderer 입력은 단색이다. 이번에는 커스텀 프레임 후 색상 칩과 모든 특수 색상의 결과를 기기 비교하지 않았으므로 코드 확인으로 분류한다. **수정 기준:** 편집 화면에서 제공하는 선택지만 preview/export의 동일 결과로 연결하거나 적용 불가능한 제어는 숨기고, 그라데이션 데이터는 출력까지 전달한다.

### C14 · P2 · MediaStore 부분 실패의 rollback·결과 확인 부재 — 코드 확인

[ResultScreen](../app/src/main/java/com/pocket4cut/presentation/result/ResultScreen.kt)은 MediaStore에 행을 `insert`한 후 stream 열기·복사에 실패해도 이미 만든 URI를 삭제하지 않는다. API 29+ `IS_PENDING=0`의 `update()` 결과도 확인하지 않는다. 이번 저장은 정상 완료했으며 실패 중 빈 파일·pending 항목 잔존은 주입하지 않았다. **수정 기준:** URI를 확보한 뒤 실패 시 삭제/재시도하고 stream 복사·pending 해제 결과를 확인하며, 정상·부분쓰기·공간부족 fixture로 검증한다.

### C15 · P3 · 계절 장식 좌표·폰트의 다양한 비율 검사가 부족 — 코드 확인, 시각 검증 필요

[SeasonDecorAnchors](../app/src/main/java/com/pocket4cut/frame/rendering/SeasonDecorAnchors.kt)와 봄·여름 벡터 장식의 기준 좌표 변환은 배치 비율에 따라 서로 다른 스케일/기준점을 사용한다. 가로 4컷처럼 사진 영역이 넓은 조건에서 장식이 의도한 여백 대신 사진 위에 들어갈 수 있다. 이번 단말에서는 여름 6컷만 눈으로 비교했으므로 다른 계절의 위치 오류를 확정하지 않는다. **수정 기준:** 8개 레이아웃×4계절의 기준 앵커가 슬롯/안전영역 안에 드는지 좌표 assertion과 실제 출력 이미지 검수로 확인한다.

## 아직 검증하지 못한 조합과 다음 검사

이번 감사에서 **실제로 하지 않은 것**은 API 26–36/다른 화면 비율/실기기 전·후면 센서, 4계절·45색·25스티커·모든 글꼴/필터/문구 길이의 전체 조합, TalkBack 완주, 저저장공간·OOM·전화 수신·화면 잠금·권한 촬영 중 회수, 실제 프로세스 kill/백업 복원, 사용자 데이터가 있는 삭제/마이그레이션, release/R8 실행 및 Play 배포다. 진단의 8배치×4필터 통과는 작은 합성 Bitmap의 렌더 계약이고 위 조건을 대신하지 않는다. 개인정보 보호를 위해 기존 사용자 사진 삭제나 저장소 손상 주입은 수행하지 않았다.

리팩터링은 다음 순서로 묶는 편이 회귀를 추적하기 쉽다.

1. **데이터 경계부터 고정:** 촬영 원본·proxy·초안·결과·사진첩 사본의 소유권, 스키마·마이그레이션, 원자적 저장·실패 복구와 삭제 범위를 명세하고 테스트한다.
2. **한 렌더 계약:** 사진 ID/순서, 슬롯 기하, caption, 색 ID, 벡터 장식, 계절 레이어를 preview/export가 공유하도록 구현하고 현 진단 4실패를 회귀 검사로 모두 통과시킨다. 2/4/6컷의 대표 JPEG를 실제 화면에서 재비교한다.
3. **화면 상태·수명:** 권한 복귀, 촬영 백그라운드/취소, 선택·커스텀·상세 편집 회전/프로세스 복원, 멱등 자동 저장을 통합 시나리오로 검증한다.
4. **출시 검증:** API 26/28/현재 API, 작은·큰 화면, 밝음/어두움, 글자 1.0/1.5/2.0, TalkBack, 저메모리·공간 부족, 최적화 release와 공유 대상 앱을 가상 데이터로 검사한다. Lint 오류를 원인에 맞게 수정하고 앱 빌드·테스트·Lint CI를 추가한다.

이번 보고서는 수정 완료 선언이 아니다. 각 P1/P2를 고친 후 동일 재현 절차·계측 진단·출력 JPEG를 다시 비교하고, 통과/미실행/기존 실패를 분리해 기록해야 한다.
