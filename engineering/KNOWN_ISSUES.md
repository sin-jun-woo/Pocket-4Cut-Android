# 현재 구현의 확인된 문제와 후속 검증

기준: 2026-09-09 작업 트리. 이 문서는 수정 완료 목록이 아니다. **앱 코드를 바꾸지 않고 조사한 결과**다.

근거 경로는 별도 표시가 없으면 `app/src/main/java/com/pocket4cut/` 아래다. `코드 확정`은 실제 호출 경로와 연산을 확인했다는 뜻이며 실기기 재현과 구분한다. `JVM 재현`은 실제 컴파일된 기하 함수를 실행한 결과다. P1은 핵심 결과·데이터 정확성, P2는 일반 기능/신뢰성, P3는 보조 기능·정리 우선순위다.

## 결과 이미지의 정확성

### R01 · P1 · 사진 재정렬이 상세 편집과 최종 결과에 전달되지 않음 — 코드 확정

- 근거: `presentation/edit/EditViewModel.kt:198`은 `order`를 JSON에 저장한다. `presentation/navigation/PocketNavHost.kt:352`는 원래 selectedIndexes로 경로와 Bitmap을 다시 만들고 pending.order를 적용하지 않는다. `presentation/detailEdit/DetailEditViewModel.kt:192`는 전달된 경로순으로 렌더한다.
- 재현: A/B/C/D 선택 → 일반 편집에서 D/A/B/C로 이동 → 상세 편집 → 원래 순서로 돌아온다. 최종 이미지와 컷별 보정 대상도 원래 순서를 따른다.
- 수정 시 기준: 사진의 원래 ID, 선택 순서, 사용자 배치 순서, 보정 슬롯이 일치해야 한다. 선택 인덱스와 정렬 인덱스를 같은 의미로 섞지 않는다.

### R02 · P1 · 문구와 날짜가 미리보기에 표시되지 않음 — 코드 확정·JVM 재현

- 근거: `frame/CollageLayoutMath.kt:49`가 문구 존재 시 실제 문자열 대신 공백 `" "`을 compute에 전달한다. `:38`은 isNullOrBlank로 검사하므로 textArea가 null이다. `frame/CollagePreview.kt:219`의 하단 그리기가 실행되지 않는다.
- 실제 export는 비어 있지 않은 문자열로 계산한다. 일반 배치는 하단 40 기준 단위만큼 높이가 늘고, 클래식 4컷은 고정 높이 안에서 사진 셀이 줄어든다.
- 재현: 날짜 ON 또는 문구 입력 → preview와 저장 JPEG 비교. 이번 JVM probe에서 8개 배치 모두 previewCaption=false / exportCaption=true.
- 수정 시 기준: 동일 문구 유무에 대해 preview와 export의 정규화된 캔버스·셀·텍스트 영역이 같아야 한다.

### R03 · P1 · 스티커 아이콘이 결과물에서 한글 이름으로 바뀜 — 코드 확정

- 근거: `frame/CollagePreview.kt:368`은 StickerPalette.icon을 그리지만 `frame/CollageRenderer.kt:261`은 palette.displayName을 drawText한다.
- 재현: 커스텀 프레임에 하트 추가 → 저장 → 아이콘 대신 “하트”라는 글자가 그려진다.
- 수정 시 기준: 동일한 스티커 자산과 색·크기·회전을 두 렌더 경로가 사용해야 한다.

### R04 · P1 · 카탈로그 색상 선택이 다음 화면에서 흰색으로 바뀜 — 코드 확정

- 근거: `frame/FrameColors.kt:78`은 `catalog_<theme.id>`를 만든다. `presentation/frameFlow/ColorFramePalettePickScreen.kt:68`이 이 목록을 표시하고 NavHost `:249`가 선택 ID를 저장한다. `EditViewModel.kt:84`에서 사용하는 `FrameColors.byId():76`은 기본 all만 찾아 catalog ID를 복구하지 못한다.
- 재현: “필름 블랙” 카탈로그 항목 선택 → 프레임 화면은 검정 → 일반 편집 진입 → 흰색. 기본 목록의 “블랙”과 구분해서 확인해야 한다.
- 수정 시 기준: 저장 가능한 모든 색 ID에 대해 직렬화/역직렬화가 같은 색을 돌려줘야 한다.

### R05 · P2 · 커스텀 장식의 회전 입력 단위 오류 — 코드 확정

- 근거: `presentation/frameFlow/CustomFrameEditorScreen.kt:240`의 detectTransformGestures rotationChange는 degrees인데 `:250`에서 rotationRadians에 그대로 누적한다. Preview `:331`, Renderer `:236`에서 다시 toDegrees한다.
- 콜백 단위는 로컬 Compose foundation 1.7.0 및 1.9.2 sources.jar의 TransformGestureDetector 설명에서도 확인했다.
- 재현: 두 손가락으로 장식을 조금 회전 → 입력 대비 약 57.3배의 각도 변환이 일어날 수 있다.
- 수정 시 기준: 저장 단위를 하나로 정하고 입력과 그리기 경계에서만 변환한다.

### R06 · P2 · 장식 크기와 위치의 preview/export 계약 불일치 — 코드 확정

- `CollagePreview.kt:332`의 graphicsLayer scale에 더해 `:340/:356/:364`의 글자/아이콘 크기에도 scale을 곱한다. Renderer는 한 번만 적용한다. scale=2는 미리보기 4배, 결과 2배가 된다.
- 커스텀 편집기의 초기 크기는 고정 sp/dp, 일반 미리보기는 canvasWidth 비례, 결과는 min(width,height) 비례다. 가로 배치에서는 scale=1에서도 차이가 생긴다.
- 근거: `CustomFrameEditorScreen.kt:752`, `CollagePreview.kt:340`, `CollageRenderer.kt:239`.
- 재현: 가로 4컷에서 텍스트·이모지·스티커를 추가하고 0.5/1/2배로 조절 → 편집기, 일반 미리보기, 결과를 같은 폭으로 비교.

### R07 · P2 · 색상 UI와 실제 프레임 적용의 누락 — 코드 확정

- 커스텀 배경은 색 편집을 허용하지만 setFrameColor는 selectedFrameColor만 바꾼다. 미리보기·최종 출력은 기존 customFrameDesign.resolvedFillColor를 우선하므로 색이 유지된다.
- 근거: `EditViewModel.kt:85/:135`, `EditScreen.kt:140`, `DetailEditScreen.kt:208`, `DetailEditViewModel.kt:200`.
- hologram/sunset/aurora의 gradientBrush는 일부 팔레트 칩에만 사용된다. 실제 프레임 입력은 Color뿐이라 그라데이션이 단색으로 나온다.
- 재현: 커스텀 프레임 생성 후 일반 편집에서 배경색 변경; 특수 색 팔레트 선택 후 결과 확인.

### R08 · P2 · 콜라주 6컷과 가로 그리드 6컷이 동일 — 코드 확정·JVM 재현

- 근거: `frame/FrameLayouts.kt:48`의 SIX_GRID_3X2와 SIX_COLLAGE는 rows/columns/aspect가 같다. 차이인 style.padding/gap을 `CollageLayoutMath`가 소비하지 않는다.
- 동일 theme과 문구에서 캔버스 높이 및 모든 셀 좌표가 같음을 실제 compiled class로 재현했다.
- 수정 전 결정: 두 선택지를 실제로 다르게 만들지, 하나로 정리할지 제품 의도를 명확히 해야 한다. 기존 layout ID를 쓰는 세션의 호환성도 필요하다.

## 저장과 데이터 보존

### D01 · P1 · 취소/삭제 후 촬영본과 편집 JSON 잔존 — 코드 확정

- `CaptureViewModel.kt:220`의 stop은 job/state만 정리한다. `SelectionScreen.kt:89`의 “모두 삭제” 확인은 onBack만 호출한다.
- `FileImageStorage.deleteSessionFiles`는 GalleryViewModel과 SettingsScreen에서 완성 세션에 대해 호출된다. 결과를 만들지 않은 세션은 sessions.json에 없어 정리 대상에서 빠진다.
- `PendingCollageStore.kt:123`의 delete는 호출부가 없다. 완성 세션을 지워도 문구·장식 JSON이 남는다.
- 재현: 촬영 중 취소 / 선택 중 나가기 / 편집 중 나가기 / 완성 후 보관함 삭제를 각각 수행하고 해당 session 폴더와 내부 JSON 잔존 여부 확인.
- 수정 시 기준: “보관함 항목 삭제”, “현재 촬영 취소”, “앱 데이터 전체 삭제”, “공용 사진첩 사본”의 범위를 UI와 저장 서비스에서 일치시킨다.

### D02 · P2 · 결과 재진입마다 공용 사진첩 자동 저장 반복 — 코드 확정

- 근거: `ResultScreen.kt:86/:161`에서 isSaved/didAttemptAutoSave는 remember이고 저장 시 `:135`에서 항상 MediaStore.insert한다.
- 재현: 자동 저장 ON → 완성 → Home → Gallery → 같은 결과 열기 반복. 새 항목을 계속 생성할 수 있다.
- 수정 시 기준: 완성본 생성 이벤트와 단순 보기 진입을 구분하고 내보낸 URI 또는 동일 결과의 저장 상태를 관리한다.

### D03 · P2 · API26–28 사진첩 저장의 런타임 권한 처리 공백 — 코드+플랫폼 요건 확인

- minSdk26, WRITE_EXTERNAL_STORAGE maxSdk28 선언은 있지만 실제 런타임 요청은 CAMERA뿐이다.
- 근거: `app/src/main/AndroidManifest.xml:16`, `ResultScreen.kt:113`, CaptureScreen 권한 launcher.
- 재현 대기: API26 또는 28 새 설치에서 저장 권한이 없는 상태로 결과 저장.
- API29+에서 자체 생성 미디어를 쓰는 데 읽기 권한은 필요하지 않다. [Android 공유 미디어 문서](https://developer.android.com/training/data-storage/shared/media)

### D04 · P1 · JSON 손상·동시 수정·중간 실패에 대한 복구 계약 부재 — 코드 확정, 사고 미재현

- `data/local/SessionRepository.kt:16/:38/:48`은 잠금 없는 전체 read-modify-write와 직접 writeText다. 원자적 교체/버전/백업본이 없다. 전체 JSON 파싱 실패는 예외, 개별 행 실패는 조용한 누락이다.
- 메타데이터 저장 전 JPEG가 만들어져 있으면 고아 결과가 남을 수 있다. 개별 삭제는 metadata→파일, 전체 삭제는 파일→metadata로 순서가 다르다.
- `FileImageStorage.kt:40/:57`은 compress/delete 반환값을 확인하지 않는다. ResultScreen은 MediaStore insert 후 복사 실패 시 새 URI를 삭제하지 않는다.
- 확인할 조건: 저장 중 앱 종료, 저장 공간 부족, JSON 일부 손상, 두 저장/삭제 작업 경합. 개인 실데이터가 아닌 격리 테스트 데이터로 검증해야 한다.

### D05 · P2 · 영속 필드 의미 혼용 — 코드 확정; 복원 경로 위험 — 미검증

- imagePaths는 선택된 사진 목록, selectedIndexes는 전체 촬영 목록에 대한 인덱스다. 두 값을 직접 배열 인덱싱으로 연결하면 잘못된 사진 또는 범위 밖 접근이 된다.
- 활성 DetailEdit는 frameId에 layout ID, 이전 CollageFinalize는 theme ID를 기록한다.
- createdAt은 저장 시각이며 경로는 절대 경로다. 기기 복원/저장 볼륨 변경 시 경로 재해석이 필요할 수 있다.
- 근거: `domain/model/PhotoSession.kt`, `DetailEditViewModel.kt:225`, `CollageFinalize.kt:57`.
- 후속 재편집 기능이나 DB 이전을 시작하기 전에 기존 데이터의 양쪽 의미를 수용하는 migration 계약을 만든다.

## 화면·수명·오류

### U01 · P2 · 편집 초안과 시스템 뒤로 처리 불완전 — 코드 확정

- SavedStateHandle/rememberSaveable/BackHandler가 없다. flowStep, 커스텀 장식, 선택 UI는 재생성에 취약하다. Edit의 새 VM은 pending 편집값을 복원하지 않는다.
- Selection load가 선택을 초기화한다. Capture UI의 이중 누름 취소와 Selection 확인 다이얼로그는 시스템 뒤로에 적용되지 않는다.
- FrameFlow 하위 화면의 UI 뒤로는 방식 선택으로 가지만 시스템 뒤로는 배치 선택으로 빠진다.
- 근거: `SelectionViewModel.kt:26`, `EditViewModel.kt:71`, `PocketNavHost.kt:216`, `CaptureScreen.kt:184`.
- 재현 대기: 단계별 화면 회전·백그라운드 프로세스 종료·시스템 뒤로와 화면 닫기 비교.

### U02 · P2 · 오류가 빈 화면·빈 목록 또는 무응답으로 나타남 — 코드 확정

- 상세 결과 생성은 `DetailEditScreen.kt:411`에서 onSuccess만 처리한다.
- GalleryViewModel의 errorMessage를 GalleryScreen이 표시하지 않는다. 손상된 목록이 빈 보관함처럼 보일 수 있다.
- Edit 오류가 화면에 제대로 표시되지 않고 상세 진입 액션이 남아 있다.
- 상세 진입에서 모든 bitmap decode 실패 시 `PocketNavHost.kt:361`의 if 조건 때문에 화면 자체가 구성되지 않는다. 일부만 mapNotNull로 빠지면 경로/슬롯 대응도 어긋날 수 있다.
- 공통 오류 종류, 재시도 가능 여부, 버튼 활성 조건, 오류 복구 경로가 필요하다.

### U03 · P2 · Material 테마 색이 계절 변경과 동기화되지 않을 수 있음 — 코드 확정, 시각 영향 미검증

- `ui/theme/Theme.kt:8`의 colorScheme은 최상위 val이다. 직접 ThemeManager를 getter로 읽는 AppColors와 갱신 시점이 다르다.
- 재현: 시작 계절을 바꾼 뒤 기본 Material 컴포넌트와 커스텀 컴포넌트의 강조색 비교.

### U04 · P2 · 접근성·현지화 공백 — 코드 확정, TalkBack 미검증

- 다수 아이콘 버튼의 contentDescription=null; Row/Box.clickable로 버튼을 만들고 역할을 명시하지 않는다.
- 커스텀 PinkToggle/슬라이더는 토글 상태·범위 조작 semantics를 제공하지 않는다. 선택 상태의 의미도 빠진 곳이 있다.
- 문자열은 대부분 코드에 직접 작성되어 있고 작은 터치 영역이 남아 있다.
- 근거: `ui/designsystem/components/Buttons.kt:79`, `PinkToggle.kt:45`, `PinkGradientSlider.kt:90`, `DetailEditScreen.kt:180`.
- 재현 대기: TalkBack으로 전체 제작, 큰 글자 크기, 스위치 액세스, 작은 화면/가로 화면.

### U05 · P3 · 보조 액션과 표기 미연결 — 코드 확정

- Home.galleryCount 기본 0에 실제 목록 개수를 전달하지 않는다(`HomeScreen.kt:48`, `PocketNavHost.kt:67`).
- 빈 보관함의 “촬영하러 가기”는 onBack만 호출해 홈으로 돌아간다(`GalleryScreen.kt:118`).
- 이름만 남은 route·FrameThemeSelectScreen·FrameBackgroundSelection·이전 출력 경로와 TODO 파일은 정리 후보다. 호출 관계 확인 후 정리한다.

## 성능·카메라·벡터 엔진의 추가 위험

다음은 위험한 코드 구조가 확인된 항목이다. OOM·ANR·화면 깨짐이 기기에서 발생했다고 보고하는 것은 아니다.

| 항목 | 근거·다음 검증 |
|---|---|
| UI 스레드 디코드/필터 | NavHost `:219/:352`가 Main에서 사진을 decode. EditViewModel `:254`는 순서 변경 시 필터 이미지를 Main에서 재생성. 기기 trace 필요 |
| 메모리 최고 사용량 | 원본·필터 preview·상세 preview·최종 처리 사진·출력 bitmap이 겹친다. 2048² ARGB 한 장 약 16MiB, 클래식 출력 약 31MiB. 실제 사용량 측정 필요 |
| decodeSampled 요청 크기 의미 | BitmapDecoding `:29`는 양쪽 치수를 기준으로 sampling. reqSize=720이 긴 변 720 이하를 보장하지 않음 |
| bitmap 소유권 | 해결(2026-09-10): Edit 원본과 Detail preview처럼 UI에 게시한 Bitmap의 수동 recycle 4곳을 제거하고 도달 가능성에 따른 회수로 통일. 신규 수명 계측 2개, 기존 조기 recycle 진단 1개, 실기기 반복 편집 통과. 일시적 메모리 중첩은 위의 최고 사용량 위험으로 계속 추적 |
| 필터/보정 작업 경합 | EditViewModel `:263`의 오래된 Job 결과가 최신 선택을 덮을 수 있음. Detail의 전체 rebuild Job은 slotJobs에서 관리하지 않음 |
| 취소와 예외 정리 | Detail의 처리 목록 구성 중 실패하면 앞서 생성한 bitmap이 finally 범위 밖. withContext 취소 시 이후 recycle 분기에 도달하지 않을 수 있음 |
| 촬영 취소 | CaptureEngine suspendCoroutine은 요청 취소를 연결하지 않음. CaptureViewModel catch(Throwable)은 취소도 FAILED로 변환할 수 있음 |
| 촬영 파일 정규화 실패 | CaptureViewModel `:198`은 rewriteJpegMaxLongEdge 반환값 무시. 원본 직접 덮어쓰기 실패에 대한 복구 없음 |
| 전면 반사 | OutputFileOptions에 명시적 반사 metadata 없음. 글자/비대칭 표적을 전면으로 찍어 preview·JPEG 방향 비교 |
| 카메라 준비/줌 | awaitLaidOut은 post 한 번만 기다림. zoom Future 실패와 비동기 반영을 무시. 회전·재바인딩·렌즈 부재 검증 |
| 권한 복귀 | CaptureScreen `:81`은 최초 remember/permission launcher에서 권한을 갱신하고 설정 앱에서 돌아올 때 재확인하지 않음. 설정에서 권한 변경 후 복귀 검증 |
| 렌더 중 입력 | SharedComponents `:101`의 LoadingOverlay는 입력 차단을 보장하지 않음. 상세 화면 뒤로/초기화도 렌더 상태로 비활성화되지 않아 경합 검증 필요 |
| 여름 Paint 공유 | SummerFrameVectorDecor `:37`의 mutable Paint를 UI/IO draw가 공유. 동시 렌더 race 가능 |
| 계절 anchor 변환 | 봄/여름 uniform scale+중앙 정렬과 anchor 역변환이 다름. 가을/겨울은 비균일 scale. 넓은 레이아웃에서 배치/왜곡 비교 필요 |
| 계절 footer/점선 | anchor의 고정 px 오프셋, baseline/top 해석 차이, preview와 export의 사진·점선 그리기 순서 차이 |
| 긴 문구 | Preview는 ellipsis/줄 수 제한, Renderer는 단일 drawText. 긴 문구 clipping 비교 필요 |

## 배포와 문서 정합성

- **현재 Lint 차단:** `app/src/main/AndroidManifest.xml:6`, PermissionImpliesUnsupportedChromeOsHardware. 카메라 권한과 대응 하드웨어 선언이 없다. 지원 기기 정책을 정하고 수정해야 하며, 이번 감사에서는 단순 suppress하지 않았다.
- **자동 백업 범위:** allowBackup=true + 제외 규칙 없음. 설정뿐 아니라 filesDir/externalFilesDir의 사진·편집 자료도 대상이 될 수 있다. [공식 기본 포함 범위](https://developer.android.com/identity/data/autobackup). 실제 백업 발생 여부는 미검증이다.
- **개인정보 본문 중복:** Kotlin/Markdown/HTML 별도 관리. 삭제 잔존과 백업 범위를 구현과 함께 맞춘다.
- **CI 공백:** 현재 workflow는 docs Pages 배포뿐이며 Android build/test/lint CI가 없다.
- **테스트 공백:** 기본 산술·패키지명 테스트만 있다. 기존 ADB smoke script는 기기 경로/좌표가 고정이고 최종 파일 생성 assertion이 없다. FATAL과 패키지 문자열을 같은 줄에서 찾는 방식도 crash 누락 가능성이 있어 품질 보증 수단으로 충분하지 않다.
- **릴리스/자산:** version 1.2/3은 확인했지만 Play 공개 상태와 폰트별 고지·LICENSE 파일은 추가 확인 대상이다. 현재 분석은 법률 검토나 게시 승인 검사가 아니다.
