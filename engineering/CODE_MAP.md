# 소스 파일 지도

기준: 2026-09-09 작업 트리, main Kotlin **85개**. 아래 파일 경로는 모두 `app/src/main/java/com/pocket4cut/` 기준이다. 빈 파일·미사용 코드도 포함한다. 개별 기능 수정 시 이 지도에서 담당 파일을 찾고 [KNOWN_ISSUES](KNOWN_ISSUES.md)의 경계를 함께 확인한다.

## 진입점과 화면 — 29파일

| 파일 | 책임·변경 시 확인할 연결 |
|---|---|
| MainActivity.kt | ThemeManager/AppSettings 초기화, edge-to-edge, MaterialTheme, Scaffold, NavHost 진입 |
| presentation/navigation/Routes.kt | route/argument 상수, FrameType의 촬영·선택 장수, 인덱스/경로 codec. frame 계층도 FrameType에 의존 |
| presentation/navigation/PocketNavHost.kt | 14 destination, 화면 연결, 프레임 내부 분기, 이미지 디코드, pending 전달. 순서 유실의 핵심 연결점 |
| presentation/launch/LaunchScreen.kt | 시작 시각 화면, 1,150ms 지연 후 홈 |
| presentation/home/HomeScreen.kt | 촬영·보관함·설정 진입. galleryCount는 실제 연결 안 됨 |
| presentation/frameTypeSelect/FrameTypeSelectScreen.kt | 2/4/6컷 선택; 기본 4컷 |
| presentation/capture/CaptureScreen.kt | CAMERA 권한, PreviewView, 촬영 입력·줌 제스처·렌즈 전환·카운트다운 UI·화면 켜짐 |
| presentation/capture/CaptureViewModel.kt | CapturePhase, CameraX 연결, 타이머/수동 셔터 Channel, 세션 UUID, 촬영 파일 정규화, stop |
| presentation/selection/SelectionScreen.kt | 촬영본 선택 그리드, 선택 순서 표시, 장수 제한, 종료 확인 |
| presentation/selection/SelectionViewModel.kt | 파일 목록 로드, 선택 인덱스와 개수 관리. load마다 선택 초기화 |
| presentation/layoutSelection/LayoutSelectionScreen.kt | 컷 수별 배치, 선택 미리보기, 스크롤. 자체 이미지 로딩 |
| presentation/frameFlow/FrameFlowCoordinatorScreen.kt | 색/계절/커스텀 제작 방식 선택 |
| presentation/frameFlow/ColorFramePalettePickScreen.kt | 기본색+카탈로그색 팔레트, 프레임 미리보기, 선택 결과 콜백 |
| presentation/frameFlow/SeasonBackgroundFramePickScreen.kt | 4계절 프레임 선택, 기본 봄 |
| presentation/frameFlow/CustomFrameEditorScreen.kt | 장식 편집 상태·추가/삭제·텍스트/폰트·이동/확대/회전; 약 816줄의 큰 화면 |
| presentation/frameThemeSelect/FrameThemeSelectScreen.kt | 이전 카탈로그 테마 선택 UI. 현재 NavHost에서 호출되지 않음 |
| presentation/edit/EditScreen.kt | 전체 편집 UI, 930줄. 필터·색·문구·날짜·폰트·사진 배열·상세 진입 |
| presentation/edit/EditViewModel.kt | 이미지/필터 preview, 정렬, PendingCollageParams 작성. 이전 출력 함수도 남아 있음 |
| presentation/edit/PendingCollageStore.kt | frame_selection/pending_collage JSON 입출력, 장식 직렬화; delete 호출 없음 |
| presentation/edit/CollageFinalize.kt | 이전 단순 출력 경로. 현재 UI 미호출; 활성 상세 출력과 전달 옵션·해상도·frameId 의미 다름 |
| presentation/detailEdit/DetailEditScreen.kt | 실제 DetailEditViewModel 사용; 슬롯별 조절, 최종 결과 생성 요청, 오류 표시 누락 |
| presentation/detailEdit/DetailEditViewModel.kt | PhotoSlotAdjustment 모델, 사진별 처리 Job, 전체 필터+개별 보정, 실제 최종 합성/저장/세션 등록 |
| presentation/result/ResultScreen.kt | JPEG 표시, MediaStore 저장·자동 저장, FileProvider 공유, 홈 이동. 영속 export 상태 없음 |
| presentation/gallery/GalleryScreen.kt | 완성본 그룹/목록/빈 화면/삭제 확인. VM 오류 미표시 |
| presentation/gallery/GalleryViewModel.kt | 세션 로드, 날짜/컷 수 그룹, 개별 삭제; repository와 storage 직접 생성 |
| presentation/settings/SettingsScreen.kt | 환경설정 UI, 보관함 개수, 전체 삭제, 개인정보·문의 진입 |
| presentation/settings/AppSettings.kt | SharedPreferences와 Compose state를 가진 전역 설정 객체 |
| presentation/settings/PrivacyPolicyScreen.kt | 앱 내 개인정보 본문 표시 |
| presentation/settings/ContactFeedbackScreen.kt | 문의 주소 복사 및 mailto Intent; 직접 메일 발송 기능 없음 |

## 촬영·파일·모델 — 8파일

| 파일 | 책임·주의 |
|---|---|
| camera/CaptureEngine.kt | CameraX provider, lifecycle/viewport binding, ImageCapture, zoom, suspend callback 연결 |
| data/storage/ImageStorage.kt | 앱 전용 촬영/결과 파일 저장·조회·삭제 인터페이스. MediaStore export는 없음 |
| data/storage/FileImageStorage.kt | cap_XX.jpg와 result.jpg 생성, 파일명순 조회, JPEG98 저장, 세션 사진 삭제 |
| data/local/SessionRepository.kt | sessions.json 전체 읽기/쓰기, upsert/getAll/getById/delete. 도메인 인터페이스 미구현 |
| data/local/PlaceholderLocal.kt | Room/DataStore TODO. 실제 저장 동작을 하는 파일이 아님 |
| domain/model/PhotoSession.kt | 완성 세션의 8개 필드. imagePaths/selectedIndexes/frameId 의미 주의 |
| domain/repository/SessionRepository.kt | 구현체에 연결되지 않은 save/getAll/getById/delete 계약 |
| domain/usecase/PlaceholderUseCase.kt | UseCase TODO. 실제 유스케이스 계층 없음 |

## 프레임·합성 — 18파일

| 파일 | 책임·주의 |
|---|---|
| frame/FrameLayouts.kt | FrameLayoutId 8개, FrameStyle, 장수별 목록. padding/gap 모델과 실제 수식 불일치 |
| frame/FrameTheme.kt | 구조 테마 FrameTheme/FrameCatalog. 컷 수별 배경·외곽선·여백·간격 |
| frame/FrameColors.kt | 45 기본 색, gradient 메타데이터, 카탈로그 색 확장. byId가 catalog ID 복구 못함 |
| frame/FilterDefs.kt | ORIGINAL/SOFT/FILM/BW 색 행렬 |
| frame/CustomFrameDecoration.kt | 정규화 위치·크기·회전·폰트, Text/Emoji/Sticker kind, CustomFrameDesign와 계절/색 해석 |
| frame/EmojiPicklist.kt | 이모지 선택 목록 |
| frame/StickerPalette.kt | 스티커 ID·이름·Compose ImageVector 25종 |
| frame/FrameBackgroundSelection.kt | 테마/단색/이미지/장식 배경 sealed model. 현재 호출부 없는 대안 모델 |
| frame/CollageLayoutMath.kt | 공유 캔버스·셀·상단·문구 영역 수식. FOUR_VERTICAL 특례와 preview adapter |
| frame/CollagePreview.kt | Compose 사진/프레임/장식/문구/계절 합성. Renderer와 차이를 함께 수정해야 함 |
| frame/CollageRenderer.kt | Android Canvas 최종 Bitmap 생성. Input API와 이전 overload, 사진 크롭·장식·문구 그리기 |
| frame/SeasonHTMLFrameStyle.kt | 계절 색/gradient/점선 스타일. 이름과 달리 HTML 실행 없음 |
| frame/SeasonBackgroundFrameFactory.kt | season ID와 색을 CustomFrameDesign으로 변환 |
| frame/rendering/SeasonDecorAnchors.kt | 계절 장식의 사진 영역/여백/하단 위치 계산 |
| frame/rendering/SpringFrameVectorDecor.kt | 봄 Canvas 벡터 장식. 기준 공간 uniform scale+가운데 정렬 |
| frame/rendering/SummerFrameVectorDecor.kt | 여름 Canvas 벡터. 전역 mutable Paint 공유 주의 |
| frame/rendering/AutumnFrameVectorDecor.kt | 가을 Canvas 벡터. 가로/세로 독립 배율 |
| frame/rendering/WinterFrameVectorDecor.kt | 겨울 Canvas 벡터. 독립 배율, 고정 seed의 장식점 |

## 공통 유틸 — 10파일

| 파일 | 책임·주의 |
|---|---|
| core/util/Constants.kt | 4/8/10 촬영, 2/4/6 선택, 간격2초, 긴 변2048, JPEG92/98. countdown10/출력2560은 활성 기본값과 구분 |
| core/util/BitmapDecoding.kt | bounds 기반 sampling, EXIF 회전·반사, JPEG 긴 변 정규화/덮어쓰기 |
| core/util/BitmapAdjustments.kt | 90도 회전·반전, brightness/contrast/saturation 행렬 처리 |
| core/util/CollageExportMetrics.kt | 시스템 화면 폭으로 일반 레이아웃 출력 폭720–2160 결정 |
| core/util/AppFontCatalog.kt | 번들 Typeface/FontFamily 로딩·캐시·이름 조회, 기본 글꼴 fallback |
| core/util/ColorRGB.kt | Compose Color와 24비트 RGB Long 변환 |
| core/util/FileUris.kt | applicationId.fileprovider content URI 생성 |
| core/extensions/ContextExtensions.kt | ContextWrapper에서 Activity 찾기; 촬영 화면 Window flag에 사용 |
| core/extensions/DateExtensions.kt | epoch 날짜 문자열/현재 시간 유틸. 주요 화면은 별도 날짜 포맷도 사용 |
| core/designsystem/DesignSystem.kt | TODO stub. 실사용 디자인 시스템은 ui/designsystem |

## UI·테마 — 19파일

| 파일 | 책임·주의 |
|---|---|
| ui/designsystem/AppColors.kt | 현재 ThemeManager에서 동적으로 읽는 배경·텍스트·강조·의미 색 |
| ui/designsystem/AppTypography.kt | 화면용 타이포그래피 토큰 |
| ui/designsystem/AppSpacing.kt | 화면/컴포넌트 간격 |
| ui/designsystem/AppLayout.kt | 반경2–16dp, 높이·선·터치 영역 토큰 |
| ui/designsystem/AppAnimation.kt | 애니메이션 시간·효과 기본값 |
| ui/designsystem/components/Buttons.kt | Primary/Secondary/IconCircleButton/ScaleOnPress. 현재 인화지 UI, 역할 semantics 보완 대상 |
| ui/designsystem/components/Dialogs.kt | 확인·안내 다이얼로그 |
| ui/designsystem/components/SharedComponents.kt | Toast, 로딩, 빈 상태 등. overlay가 입력 차단을 보장하지 않음 |
| ui/designsystem/components/PinkToggle.kt | 자체 토글 UI. 색은 계절 강조색 |
| ui/designsystem/components/PinkGradientSlider.kt | 자체 pointer 기반 슬라이더. 현재 단색 스타일 |
| ui/designsystem/components/InAppColorPaletteSheet.kt | 색 선택 bottom sheet |
| ui/designsystem/components/InAppFontPickerSheet.kt | 번들/기본 폰트 선택 sheet |
| ui/designsystem/theme/AppTheme.kt | AppThemeData와 색상 그룹 모델 |
| ui/designsystem/theme/Themes.kt | 종이/잉크 셸 위에 4계절 강조색 정의 |
| ui/designsystem/theme/Season.kt | SPRING/SUMMER/AUTUMN/WINTER enum |
| ui/designsystem/theme/ThemeManager.kt | 계절 state와 SharedPreferences 저장 |
| ui/theme/Theme.kt | 실제 MainActivity에서 사용하는 MaterialTheme adapter. colorScheme은 최상위 val |
| ui/theme/Type.kt | Material typography 기본 템플릿 |
| ui/theme/Color.kt | package 선언만 남은 빈 파일 |

## 개인정보 — 1파일

| 파일 | 책임 |
|---|---|
| legal/PrivacyPolicyContent.kt | 앱 내 정적 개인정보 본문. 웹 HTML/Markdown과 별도 관리 |

## Kotlin 밖의 구성

| 경로 | 역할·확인 범위 |
|---|---|
| app/src/main/AndroidManifest.xml | 카메라·저장 권한, allowBackup, FileProvider, launcher Activity |
| app/src/main/res/xml/file_paths.xml | 공유 가능한 files/cache/external-files 범위 |
| app/src/main/res/xml/backup_rules.xml | 구버전 백업 설정; 실질 제외 없음 |
| app/src/main/res/xml/data_extraction_rules.xml | 신버전 백업/전송 규칙; 실질 제외 없음 |
| app/src/main/res/values | 앱 이름·XML 테마·색. 대부분 UI 문구는 Kotlin에 있음 |
| app/src/main/res/drawable* / mipmap* | 밀도별 런처 아이콘·adaptive icon |
| app/src/main/assets/fonts | 한국어 폰트 TTF 13개. 바이너리 내부의 라이선스 메타데이터 감사는 미실시 |
| app/src/main/assets/branding | 브랜드 아이콘 SVG |
| app/src/test / app/src/androidTest | 산술 기본 테스트 / packageName 기본 테스트 각1개 |
| app/build.gradle.kts | 앱 ID·SDK·버전·서명·의존성·빌드 타입 |
| build.gradle.kts / settings.gradle.kts | 플러그인 선언, 저장소, 단일 app 모듈 |
| gradle/libs.versions.toml / wrapper | 의존성 버전·Gradle 배포 버전 |
| gradle.properties / app/proguard-rules.pro | 빌드 JVM2GB·AndroidX 설정 / 미최적화 release 기본 규칙 |
| local.properties | 로컬 SDK 위치, Git 제외. 공유 문서에 복사하지 않음 |
| keystore.properties.example | 서명 속성 템플릿. 실제 자격증명 파일은 조사에서 제외 |
| scripts/generate-release-keystore.ps1 / .bat | 릴리스 키 생성 도구. 이번에는 실행하지 않음 |
| scripts/e2e-smoke-adb.ps1 | 특정 환경의 좌표 기반 smoke 보조 도구. 독립적인 E2E 성공 증명 아님 |
| .github/workflows/github-pages.yml | docs 전체 Pages 배포; Android CI는 아님 |
| design/mockups/print-booth-v1 | HTML/CJS 기반 시안·PNG·검사 JSON·이전 확인 기록 |
| design/Pocket4Cut-Print-Booth-v1.zip | 디자인 인계 패키지. 앱 런타임 자산이 아님 |
| docs/index.html / privacy-policy.html / PRIVACY_POLICY.md | 공개 Pages/정책 콘텐츠 |
| docs/RELEASE_SIGNING.md / PLAY_STORE_RELEASE_NOTES.md | 릴리스 운영 안내·스토어 문구; 실제 출시 여부 증거 아님 |
| 루트 기획 Markdown | 목표·과거 상태·UI 설계. 현재 구현 기준은 PROJECT_UNDERSTANDING부터 확인 |

## 변경 요청별 첫 진입점

| 요청 | 먼저 읽을 파일 |
|---|---|
| 촬영 장수/타이머/셔터 | Routes, Constants, AppSettings, CaptureViewModel, CaptureEngine |
| 선택·사진 배열 오류 | SelectionViewModel → EditViewModel → PendingCollageStore → PocketNavHost → DetailEditViewModel |
| 프레임/사진 크롭/출력 사이즈 | FrameLayouts, FrameTheme, CollageLayoutMath, CollageExportMetrics, Preview/Renderer |
| 문구·스티커·색·장식 오류 | CustomFrameDecoration, FrameColors, CustomFrameEditorScreen, Preview/Renderer |
| 저장/삭제/보관함 | FileImageStorage, SessionRepository, ResultScreen, GalleryViewModel, SettingsScreen |
| 복원/뒤로/화면 멈춤 | PocketNavHost, 해당 ViewModel, PendingCollageStore, coroutine와 Bitmap 수명 |
| 앱 전체 시각/테마 | ui/designsystem, ui/theme/Theme, design/README |
| 권한·개인정보·배포 | Manifest/res xml, 법적 본문3곳, app/build.gradle, docs, Pages workflow |
