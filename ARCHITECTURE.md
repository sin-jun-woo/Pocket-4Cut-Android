# Pocket 4Cut - 안드로이드 아키텍처 설계

## 🏛️ 전체 아키텍처 개요

**MVVM + 레이어 분리**를 기반으로 한 구조다.  
(필요하면 Clean Architecture 스타일로 Domain/Data 경계를 더 명확히 가져가도 된다.)

```text
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│   (Jetpack Compose UI / ViewModel)      │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│         Domain Layer                    │
│   (Use Cases / Domain Models)           │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│         Data Layer                      │
│   (Repository / Local Storage)          │
└─────────────────────────────────────────┘

┌─────────────┐  ┌─────────────┐
│   Camera    │  │   Frame     │
│  (CameraX)  │  │ (Collage)   │
└─────────────┘  └─────────────┘
```

---

## 📦 프로젝트 구조 (안드로이드)

패키지/모듈 구성은 대략 아래 형태를 기준으로 한다. (단일 모듈 기준, 추후 multi-module로 쪼개도 됨)

```text
app/
├── presentation/
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── capture/
│   ├── selection/
│   ├── frame/
│   ├── edit/
│   ├── result/
│   ├── gallery/
│   ├── components/          # 재사용 Compose 컴포넌트
│   ├── navigation/          # NavHost, Destinations
│   └── designsystem/        # Color, Theme, Typography
│
├── domain/
│   ├── model/
│   │   └── PhotoSession.kt
│   ├── repository/          # 인터페이스
│   │   ├── SessionRepository.kt
│   │   └── ImageStorage.kt
│   └── usecase/
│       ├── capture/
│       ├── selection/
│       ├── frame/
│       └── saveshare/
│
├── data/
│   ├── storage/             # 로컬 저장 (Room / 파일)
│   │   ├── ImageStorageImpl.kt
│   │   └── SessionRepositoryImpl.kt
│   ├── local/               # Room DAO, DataStore 등
│   └── mapper/
│
├── camera/
│   └── CaptureEngine.kt     # CameraX 연속 촬영
│
├── frame/
│   ├── FrameDefinitions.kt
│   └── CollageRenderer.kt   # 비트맵 합성
│
├── core/
│   ├── util/
│   ├── extensions/
│   └── constants/
│
└── di/
    └── AppModule.kt         # Hilt DI 설정
```

---

## 🎯 각 레이어의 역할

### 1. Presentation Layer

- **역할**: UI 렌더링, 사용자 입력 처리, ViewModel을 통한 UseCase 호출
- **구성**: Compose `@Composable` 화면, `ViewModel`, `UiState`
- **흐름**: 사용자 액션 → ViewModel → UseCase → 상태 업데이트(StateFlow/MutableState) → UI recomposition

### 2. Domain Layer

- **역할**: 비즈니스 규칙, 촬영/선택/프레임/저장 로직의 추상화
- **구성**: `PhotoSession` 등 도메인 모델, `Repository` 인터페이스, UseCase
- **원칙**: Android 프레임워크/라이프사이클에 의존하지 않는 **순수 Kotlin 로직**

### 3. Data Layer

- **역할**: 이미지 파일 저장, 세션 메타데이터 저장/조회
- **구성**: `ImageStorageImpl`, `SessionRepositoryImpl`, Room DAO, 파일 시스템 접근
- **주의**: Domain 레이어에는 구현체 노출 안 하고, 인터페이스만 주입

### 4. Camera 모듈

- **역할**: CameraX 기반 연속 촬영, 타이머, 프리뷰
- **구성**: `CaptureEngine` (또는 `CameraService`)
- **연동**: ViewModel이 `CaptureEngine`을 호출하고 촬영 결과(`Bitmap` 리스트 또는 파일 경로 리스트) 수신

### 5. Frame 모듈

- **역할**: 프레임 레이아웃 정의, 이미지 합성(콜라주)
- **구성**: `FrameDefinitions`, `CollageRenderer`
- **연동**: 선택된 이미지 경로 + 프레임 ID + 편집 옵션 → 최종 `Bitmap` 생성

---

## 🔄 데이터 흐름 예시 (오프라인 부스 경험 관점)

### 촬영 → 선택 → 결과 저장 플로우 (타이밍 포함)

```text
[HomeScreen] "촬영 시작"
    ↓
[FrameTypeSelectScreen] 4컷/6컷 선택
    ↓
[CaptureScreen / CaptureViewModel]
    ↓ CaptureEngine.captureSequence(count: 8 or 10)
[CaptureEngine] 타이머 기반 연속 촬영 (기본 2초 간격, 준비 카운트다운 10초)
    ↓ List<Bitmap> 또는 파일 경로 리스트
[CaptureViewModel] 이미지/경로 저장
    ↓ 네비게이션
[SelectionScreen] 4장 또는 6장 선택, selectedIndexes (정확한 개수 강제)
    ↓
[FrameThemeScreen] 프레임 테마 선택
    ↓
[EditScreen] 필터/텍스트/순서
    ↓
[CollageRenderer] 최종 콜라주 Bitmap 생성
    ↓
[ResultScreen] 저장 / 공유
    ↓ ImageStorage, SessionRepository, Android 공유 시트(Intent.ACTION_SEND)
[갤러리 저장 & 공유 완료]
```

---

## 🎛 촬영 세션 상태 정의 (State Machine 개요)

오프라인 부스의 흐름을 최대한 비슷하게 재현하기 위해, 촬영 세션은 다음과 같은 상태로 정의한다.

```text
Idle → Ready → Countdown → Capturing → Selecting → Editing → Rendering → Completed
                                     ↘ Failed
```

- **Idle**: 홈/보관함 등, 아직 촬영을 시작하지 않은 상태
- **Ready**: `촬영 시작` 버튼을 누르고 카메라가 준비된 상태
- **Countdown**: 10초 준비 카운트다운(10→9→…→1)
- **Capturing**: 8장 또는 10장의 연속 촬영 진행 (`currentShotIndex` 관리)
- **Selecting**: 촬영본에서 4장 또는 6장 선택 (정확한 개수 선택 시만 다음 단계)
- **Editing**: 프레임/필터/텍스트/날짜/순서 편집
- **Rendering**: 고해상도 콜라주 렌더링 및 저장
- **Completed**: 결과 화면에서 저장/공유/닫기만 남은 상태
- **Failed**: 카메라/렌더링/저장 등의 치명적 실패 시 진입, 에러 표시 후 `Idle`로 복귀

상태 전이는 `CaptureViewModel` 과 이후 화면 ViewModel 들에서 명시적으로 관리해서  
촬영 UX가 꼬이지 않도록 한다.

---

## 🧩 의존성 방향

- **presentation** → **domain** (UseCase, Model)
- **domain** → Repository 인터페이스만 의존 (구현체는 DI로 주입)
- **data** → Domain Model, Room, 파일 시스템, MediaStore
- **camera** → CameraX (ViewModel/UseCase가 Camera 모듈 사용)
- **frame** → 비트맵 처리 라이브러리, Domain Model (선택된 이미지 경로 등)

DI는 Hilt(Koin 등)로 처리하는 걸 기본 가정:

- ViewModel ↔ UseCase ↔ Repository 인터페이스 ↔ 구현체 의존성 그래프를 Hilt 모듈에서 구성.

---

## 🔒 핵심 비즈니스 규칙 구현 위치

| 규칙 | 구현 위치 |
|------|-----------|
| 8장/10장만 촬영 | `CaptureEngine`, `CaptureViewModel` |
| 4장/6장만 선택 | `SelectionViewModel`, `ValidateSelectionUseCase` |
| 선택 순서 유지 | `PhotoSession.selectedIndexes` |
| 프레임 종류 제한 | `FrameDefinitions` |
| 고해상도 결과물 | `CollageRenderer` |

---

## 📱 화면별 책임 요약 (Android 기준)

| 화면 | ViewModel | 주요 UseCase / 의존성 |
|------|-----------|------------------------|
| Home | `HomeViewModel` | 최근 결과 미리보기, 촬영 시작 네비게이션 |
| FrameTypeSelect | (옵션) `FrameTypeViewModel` | 4컷/6컷 타입 선택, 촬영 옵션 전달 |
| Capture | `CaptureViewModel` | `CaptureEngine`, 촬영 옵션(8/10, 타이머), 세션 ID 생성 |
| Selection | `SelectionViewModel` | 선택 개수 검증, `ValidateSelectionUseCase`, `BuildSessionUseCase` |
| FrameTheme | `FrameThemeViewModel` | `FrameDefinitions`, 미리보기, 선택된 테마 유지 |
| Edit | `EditViewModel` | 필터, 텍스트, 순서, `RenderCollageUseCase` |
| Result | `ResultViewModel` | `SaveResultUseCase`, 갤러리 저장, 공유 시트 호출 |
| Gallery | `GalleryViewModel` | `GetGallerySessionsUseCase`, `DeleteSessionUseCase` |

---

**문서 버전**: 2.0.0 (Android 기준으로 재작성)  
**문서 위치**: `/Pocket-4Cut-Android/ARCHITECTURE.md`
