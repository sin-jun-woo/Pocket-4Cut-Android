# Pocket 4Cut — UI 스펙 (구현 기준 전수 정리)

> **목적**: 현재 앱에 존재하는 화면·컴포넌트·상태·카피를 빠짐없이 나열한다.  
> **기준**: `app/src/main/java/com/pocket4cut/presentation/**` 및 `MainActivity`, `PocketNavHost`, `FrameDefinitions` 기준 (코드와 동기화).  
> **스택**: Jetpack Compose, Material 3, `Pocket4CutTheme` (다이내믹 컬러 기본 ON).

---

## 1. 앱 셸 · 전역

### 1.1 `MainActivity`

| 항목 | 내용 |
|------|------|
| 레이아웃 | `enableEdgeToEdge()`, 최상위 `Pocket4CutTheme` → `Scaffold(fillMaxSize)` → `innerPadding`을 `PocketNavHost`에 전달 |
| 시스템 UI | 상태바/내비게이션 영역은 Scaffold 패딩으로 회피 (별도 TopBar 통일은 없음—화면마다 상단 패딩이 제각각) |

### 1.2 테마 (`ui/theme`)

| 항목 | 내용 |
|------|------|
| `Pocket4CutTheme` | 시스템 다크/라이트, Android 12+ 시 `dynamicDarkColorScheme` / `dynamicLightColorScheme`, 아니면 기본 Purple 계열 `lightColorScheme` / `darkColorScheme` |
| 타이포 | `Typography` (`Type.kt`) — Material3 기본 확장 |
| **디자인 시스템** | 앱 전용 브랜드 컬러·컴포넌트 토큰은 **아직 미정의** (M3 기본 + 일부 화면에서 하드코딩 색) |

### 1.3 내비게이션 플로우 (UI 관점)

```
홈
 → 프레임 타입 선택
 → 촬영
 → 사진 선택
 → 프레임 테마
 → 편집 (콜라주 단위)
 → 상세 편집 (컷 단위)
 → 결과

보관함 (홈에서) → 결과 보기 (단일 이미지)
```

---

## 2. 화면별 UI 상세

### 2.1 홈 (`HomeScreen`)

| 영역 | 스펙 |
|------|------|
| 레이아웃 | `Column`, `fillMaxSize`, `padding(24.dp)`, 수직 `Center`, 수평 `CenterHorizontally` |
| 타이틀 | `"Pocket 4Cut"`, `MaterialTheme.typography.headlineMedium` |
| 서브텍스트 | `"부스처럼 연속 촬영하고 콜라주 만들기"`, `bodyMedium`, 상하 패딩 `top 8.dp`, `bottom 24.dp` |
| 액션 Row | `Button` **시작하기** \| `OutlinedButton` **보관함** (`start 12.dp` 간격) |

**상태**: 로딩/에러 없음 (정적 화면).

---

### 2.2 프레임 타입 선택 (`FrameTypeSelectScreen`)

| 영역 | 스펙 |
|------|------|
| 상단 | `Scaffold` + `TopAppBar`: 제목 `"프레임 타입 선택"`, `navigationIcon` = `TextButton` **뒤로** |
| 본문 | `Column` `padding(paddingValues) + 24.dp`, 중앙 정렬 |
| 질문 문구 | `"어떤 프레임으로 촬영하시겠어요?"`, `headlineSmall` |
| 선택 버튼 Row | `padding(top 24.dp)`, `spacedBy(12.dp)` |
| 버튼 3개 | `Button` **2컷 (4장 촬영)** / **4컷 (8장 촬영)** / **6컷 (10장 촬영)** → 각각 `FrameType` 매핑 |

**상태**: 없음.

---

### 2.3 촬영 (`CaptureScreen`)

#### 2.3.1 카메라 권한 없음

| 영역 | 스펙 |
|------|------|
| 레이아웃 | `Column` 전체, `padding(24.dp)`, 중앙 정렬 |
| 문구 | `"카메라 권한이 필요합니다."` |
| 버튼 Row | `OutlinedButton` **뒤로** \| `Button` **권한 허용** (`CAMERA` 요청) |

#### 2.3.2 카메라 활성 (전체 화면)

| 레이어 | 스펙 |
|--------|------|
| 배경 | `PreviewView` `FILL_CENTER`, `AndroidView` `fillMaxSize` |
| 플래시 피드백 | `uiState.flash == true` 시 전체 `Box` + `Color.White` `alpha 0.65f` 오버레이 |
| 상단 Row | `fillMaxWidth`, `padding(16.dp)`, `SpaceBetween` |
| 좌측 | `OutlinedButton` **뒤로** + `OutlinedButton` **전면/후면** (문구: 전면일 때 `"후면"`, 후면일 때 `"전면"`). 비활성: `READY`·`IDLE`·`FAILED` 가 아닐 때 |
| 중앙 상단 | 촬영 중일 때만 `"현재/전체"` (예: `3/8`), `titleMedium`, **흰색** |
| 우측 | `COUNTDOWN` 또는 `CAPTURING` 일 때만 활성: `OutlinedButton` **길게 눌러 종료** — `combinedClickable`으로 **롱클릭 시** `viewModel.stop()` (일반 클릭은 빈 동작) |
| 중간 | `Spacer` `weight(1f)` |
| 하단 Column | `fillMaxWidth`, `bottom 24.dp`, 하단 정렬 중앙 |
| 줌 안내 | `maxZoom > minZoom + 0.01` 일 때만: 라벨 `"확대/축소 (핀치 또는 슬라이더)"`, `labelMedium`, 흰색, `bottom 4.dp` |
| 줌 슬라이더 | `Slider`, `valueRange = minZoom..maxZoom`, 좌우 `horizontal 8.dp` |
| 에러 | `errorMessage` 있으면 `Text`, `colorScheme.error`, `bottom 12.dp` |
| 메인 CTA | `Button` **촬영 시작 (N장)** — `N = frameType.captureCount`. 활성: `READY`·`IDLE`·`FAILED` |

#### 2.3.3 카운트다운 오버레이 (`COUNTDOWN`)

| 영역 | 스펙 |
|------|------|
| 배경 | 전체 `Box` + `Black` `alpha 0.25f` |
| 중앙 숫자 | `countdownRemaining` 문자열, `displayLarge`, 흰색 |
| 하단 셔터 | 하단 중앙 `padding(bottom 48.dp)`, 원형 **바깥 테두리** `76.dp`, 흰색 링 `4.dp`, 안쪽 흰 원 `58.dp` — 탭 시 `onManualShutter()` (남은 초 스킵 후 촬영) |

#### 2.3.4 제스처

- **핀치 줌**: `PreviewView` 터치를 `ScaleGestureDetector`로 소비 (`setOnTouchListener` → `true`).
- **화면 켜짐 유지**: 액티비티 `FLAG_KEEP_SCREEN_ON` (Composable dispose 시 해제).

#### 2.3.5 완료 시

- `COMPLETED` + 유효 `sessionId` 시 자동 `onCompleted` (별도 완료 UI 없음).

---

### 2.4 사진 선택 (`SelectionScreen`)

| 영역 | 스펙 |
|------|------|
| 상단 Row | `padding(16.dp)`, `SpaceBetween` |
| 좌 | `OutlinedButton` **뒤로** |
| 중앙 | `"선택된 수/최대"`, 형식 `${selectedIndexes.size}/$max 선택`, `titleMedium` (`max = frameType.selectCount`) |
| 우 | `Button` **선택 완료** — `selectedIndexes.size == max` 일 때만 활성 |

#### 그리드 (로딩 아님 · 에러 아님)

| 항목 | 스펙 |
|------|------|
| 컨테이너 | `LazyVerticalGrid`, `GridCells.Fixed(3)`, 좌우 `horizontal 12.dp`, 셀 간격 `8.dp` |
| 셀 | `aspectRatio(1f)`, `RoundedCornerShape(12.dp)`, 배경 `Black alpha 0.08f`, 클릭 시 토글 |
| 이미지 | `AsyncImage` `fillMaxSize` |
| 선택 시 | 전체 어두운 오버레이 `Black alpha 0.25f` + 좌상단 근처 배지: `CircleShape` + `primary` 배경, `(선택 순서+1)` 숫자, `labelLarge`, `onPrimary` |

#### 상태

| 상태 | UI |
|------|-----|
| 로딩 | 전체 `Box` 중앙 `CircularProgressIndicator` |
| 에러 | 중앙 `Text` (메시지 또는 기본 `"오류가 발생했습니다."`) |

---

### 2.5 프레임 테마 (`FrameThemeScreen`)

| 영역 | 스펙 |
|------|------|
| 상단 Row | `padding(16.dp)`: **뒤로** \| 제목 `"프레임 테마"` `titleMedium` \| **다음** (`selectedThemeId` 비어 있으면 비활성) |

#### 로딩 / 에러

| 상태 | UI |
|------|-----|
| 로딩 | 전체 중앙 `CircularProgressIndicator` |
| 에러 | 중앙 `Text` |

#### 정상 (본문 2단: 미리보기 + 리스트)

| 영역 | 스펙 |
|------|------|
| 미리보기 블록 | 상단 `Column` `weight(0.6f)`, 좌우 `horizontal 16.dp` |
| 라벨 | `"미리보기"`, `titleSmall`, `bottom 8.dp` |
| 프리뷰 박스 | `fillMaxWidth`, `weight(1f)`, `RoundedCornerShape(18.dp)`, 배경 = 선택 테마 `background` 또는 `Black alpha 0.05f`, 내부 `padding(6.dp)` |
| 프리뷰 내부 | 로딩 중 `CircularProgressIndicator` / 비트맵 있으면 `Image` `ContentScale.Fit` / 없으면 `"미리보기를 준비 중입니다."` |
| 테마 리스트 | `LazyColumn` `weight(0.4f)`, `top 16.dp`, 아이템 간격 `10.dp` |
| **ThemeRow** (행) | 전체 너비, 좌우 `16.dp`, `RoundedCornerShape(14.dp)`, 배경: 선택 시 `theme.accent alpha 0.12f`, 아니면 `Black alpha 0.04f`, `padding(14.dp)`, 클릭으로 선택 |
| 행 내용 | 좌: 테마 `name` `titleMedium`, 아래 `id` `bodySmall` `Gray` / 우: 색 스와치 `28.dp` 정사각, `RoundedCornerShape(8.dp)`, `theme.accent` 채움 |

**데이터**: `FrameDefinitions.themesFor(frameType)` — 2·4·6컷별 노출 테마 개수가 다름 (코드 기준: Classic White/Black 공통, Soft Pink는 4컷만, Mint는 6컷만).

---

### 2.6 편집 — 콜라주 단위 (`EditScreen`)

| 영역 | 스펙 |
|------|------|
| 전체 | `Column` + `verticalScroll`, `padding(24.dp)`, 세로 간격 `14.dp` |
| 상단 Row | **뒤로** \| 제목 `"편집"` `titleLarge` \| **다음** (`errorMessage == null` 이고 `imagePaths` 비어 있지 않을 때만 활성) |

#### 미리보기 박스

| 상태 | UI |
|------|-----|
| 로딩·미리보기 없음 | `CircularProgressIndicator` |
| 성공 | 콜라주 `Image` `9:16` `aspectRatio`, `ContentScale.Fit` |
| 에러 | 에러 문자열 |
| 그 외 | `"미리보기를 준비 중입니다."` |

#### 구분선

- `HorizontalDivider` 후 **필터** 섹션.

#### 필터

| 항목 | 스펙 |
|------|------|
| 제목 | `"필터"`, `titleMedium` |
| 칩 | `TextButton` 스타일 커스텀 `FilterChip`: Soft / Film / B&W — 선택 시 라벨 앞에 `[` `]` (예: `[Soft]`) |

#### 텍스트

| 항목 | 스펙 |
|------|------|
| `TextField` | 라벨 `"텍스트"`, 한 줄, `ImeAction.Done`, `fillMaxWidth` |

#### 날짜

| 항목 | 스펙 |
|------|------|
| Row | `"날짜 표시"` `titleMedium` + `Switch` (전체 너비 `SpaceBetween`) |

#### 순서 스왑

| 항목 | 스펙 |
|------|------|
| 구분선 | `HorizontalDivider` |
| 제목 | `"순서 변경(간단 스왑)"`, `titleMedium` |
| 버튼 | `TextButton` **1↔2** (`order.size >= 2`), **2↔3** (`>= 3`), **3↔4** (`>= 4`) — 2컷은 3↔4 비활성, 6컷도 현재 UI는 4슬롯 스왑만 노출 (코드 한계 명시) |

---

### 2.7 상세 편집 — 컷 단위 (`DetailEditScreen`)

| 영역 | 스펙 |
|------|------|
| 전체 | `verticalScroll`, `padding(24.dp)`, 간격 `14.dp` |
| 상단 Row | **뒤로** \| `"상세 편집"` `titleLarge` \| **완료** (저장 중 `"생성 중"`) — `errorMessage == null`, `orderedPaths` 비어 있지 않음, `!isLoading`, `!isSaving` 일 때 활성 |

#### 슬롯 탭

| 항목 | 스펙 |
|------|------|
| 레이아웃 | 가로 스크롤 `Row`, 간격 `8.dp` |
| 버튼 | `TextButton` — 슬롯 인덱스 `1,2,3…`, 현재 선택에 `"▶ "` 접두사 |

#### 미리보기

| 상태 | UI |
|------|-----|
| 로딩·이미지 없음 | `CircularProgressIndicator` |
| 성공 | 단일 컷 미리보기 `9:16`, `ContentScale.Fit` |
| 에러 | 메시지 `Text` |

#### 조절 (선택 슬롯·경로 유효 시)

| 컨트롤 | 스펙 |
|--------|------|
| 밝기 | 라벨 `"밝기"` `titleSmall` + `Slider` `-0.35f..0.35f` |
| 대비 | `"대비"` + `Slider` `0.7f..1.5f` |
| 채도 | `"채도"` + `Slider` `0f..2f` |
| 회전 | `TextButton` **90° 회전** (누를 때마다 90° 누적, 모듈로 4) |

---

### 2.8 결과 (`ResultScreen`)

| 영역 | 스펙 |
|------|------|
| 전체 | `verticalScroll`, `padding(16.dp)`, 세로 간격 `12.dp` |
| 상단 Row | **뒤로** \| `"결과"` `titleLarge` \| **메인** `Button` |
| 이미지 | `9:16` `Box`, `AsyncImage` + `FileUris` Content URI, `ContentScale.Fit` |
| 하단 Row | 동일 행에 3개: **저장** `Button` `weight(1)` \| **공유** `Button` `weight(1)` \| **보관함** `OutlinedButton` `weight(1)` |
| 피드백 | 저장 후/실패 시 `bodyMedium` 메시지 (성공: `"갤러리에 저장되었습니다."`, 실패 시 예외 메시지 포함) |

**비고**: 과거 있던 **결과 편집**(콜라주 통째 보정) 화면은 제거됨.

---

### 2.9 보관함 (`GalleryScreen`)

| 영역 | 스펙 |
|------|------|
| 상단 Row | `padding(16.dp)`: **뒤로** \| `"보관함"` `titleLarge` \| **새로고침** `OutlinedButton` |

#### 상태별 본문

| 상태 | UI |
|------|-----|
| 로딩 | 중앙 `CircularProgressIndicator` |
| 에러 | 중앙 `Text` |
| 빈 목록 | `"아직 결과물이 없습니다."` |
| 목록 있음 | `LazyVerticalGrid` 2열,우 `12.dp`, 간격 `10.dp` |

#### 그리드 셀

| 항목 | 스펙 |
|------|------|
| 비율 | `aspectRatio(9f/16f)` |
| 모양 | `RoundedCornerShape(14.dp)`, 배경 `Black alpha 0.06f` |
| 탭 | 짧게 누르면 해당 결과 **결과 화면**으로 이동 |
| 롱프레스 | 삭제 확인 다이얼로그 트리거 |

#### 삭제 다이얼로그 (`AlertDialog`)

| 항목 | 스펙 |
|------|------|
| 제목 | `"삭제하시겠어요?"` |
| 본문 | `"이 결과물을 삭제합니다: {파일명}"` |
| 확인 | `Button` **삭제** |
| 취소 | `OutlinedButton` **취소** |

---

## 3. 공통 UI 패턴 정리

| 패턴 | 사용 화면 |
|------|-----------|
| 상단 3분할 Row: 뒤로(Outlined) · 제목 · 주요 CTA(Button) | 선택, 테마, 편집, 상세편집, 결과, 보관함 (보관함 우측은 새로고침) |
| 콜라주/결과 미리보기 가로폭 전체 + **세로 비율 9:16** | 편집, 상세편집, 결과, 보관함 썸네일 |
| 로딩 | 대부분 `CircularProgressIndicator` 단독 중앙 |
| 에러 | 단순 `Text` 중앙 (스낵바/재시도 버튼은 없음) |
| 필터·칩 | M3 `FilterChip`이 아니라 `TextButton` + `[선택]` 텍스트 패턴 |
| 스크롤 | 홈 제외 다수 화면에서 긴 폼은 `verticalScroll` |

---

## 4. 하드코딩 색상 · 오버레이 (디자인 시스템 이슈)

| 위치 | 색 / 용도 |
|------|-----------|
| 촬영 UI | 흰/검 오버레이, 라벨 흰색 (다크 카메라 배경 가정) |
| 선택·테마·보관함 | `Color.Black` 알파 배경, `Gray` 보조 텍스트 |
| 테마 행 | `theme.accent` 기반 선택 하이라이트 |

→ 다크 모드·브랜드 통일 작업 시 **한 번에 토큰화**하는 것이 좋음.

---

## 5. 접근성 · 카피 (현재 구현 기준)

| 항목 | 상태 |
|------|------|
| `contentDescription` | 결과·촬영 일부·선택 그리드에만 부분 적용 |
| 터치 영역 | Material 기본 최소 크기 이상이 아닌 곳 있을 수 있음 (특히 TextButton 칩) |
| TalkBack / 라벨 | 슬라이더에 값 읽기용 레이블 없음 (개선 여지) |

---

## 6. 네비게이션에 대응하는 “화면 ID” 요약

| Route / 화면 | 사용자에게 보이는 제목/역할 |
|--------------|---------------------------|
| `home` | Pocket 4Cut 랜딩 |
| `frameTypeSelect` | 프레임 타입 선택 |
| `capture` | 촬영 |
| `selection` | N장 선택 |
| `frameTheme` | 프레임 테마 |
| `edit` | 편집 |
| `detailEdit` | 상세 편집 |
| `result` | 결과 |
| `gallery` | 보관함 |

---

## 7. (참고) 기획 확장 시 UI에 추가로 잡을 만한 것들

아래는 **현재 코드에 없음** — 나중에 만들 경우 체크리스트로 쓰기 좋음.

- 스플래시 / 온보딩
- 설정 화면 (화질, 카운트다운 초, 저장 경로 안내)
- 촬영 중 **일시정지** 전용 UI (현재는 롱프레스 종료 위주)
- 6컷 **5↔6** 등 순서 스왑 노출
- 상세 편집: 슬라이더 옆 **숫자 표시**, **되돌리기(리셋)**, **직선 가이드(그리드)**
- 에러 공통 컴포넌트 (아이콘 + 재시도 + 뒤로가기)
- 빈 상태 일러스트
- 큰 글씨 / 고대비 모드

---

이 파일은 **구현 변경 시 함께 갱신**하는 걸 권장한다.
