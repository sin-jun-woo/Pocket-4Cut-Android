# Pocket 4Cut Android - 빠른 시작

## 🚀 5분 안에 실행하기

### 1. 프로젝트 열기

- 저장소 클론 후 Android Studio에서 **Open** → `Pocket-4Cut-Android` 폴더 선택
- 또는 `File > Open > Pocket-4Cut-Android` 선택

### 2. 설정 확인

- **PROJECT_SETUP_GUIDE.md** 에 따라 프로젝트 구조와 `AndroidManifest.xml` 권한이 설정되어 있는지 확인한다.
- 카메라/저장 권한이 없으면 실제 기기에서 촬영·저장 기능이 동작하지 않는다.

### 3. 빌드 및 실행

- **Ctrl+F9** (Windows/Linux) / **⌘F9** (Mac): 빌드
- **Shift+F10** / **⌃R**: 실행
- 에뮬레이터보다 **실제 기기**에서 카메라 테스트를 권장한다.

---

## 📁 프로젝트 구조 (요약)

```
app/src/main/java/com.pocket4cut/
├── presentation/   # 화면 (Home, Capture, Selection, Frame, Edit, Result, Gallery)
├── domain/         # 모델, Repository 인터페이스, UseCases
├── data/           # 저장소 구현 (Storage, SessionRepository)
├── camera/         # CameraX 연속 촬영
├── frame/          # 프레임 정의 및 콜라주 렌더링
└── core/           # 공통 유틸, DesignSystem
```

---

## 📚 문서 목록

| 문서 | 설명 |
|------|------|
| **README.md** | 프로젝트 소개, MVP 범위, 기술 스택 |
| **PROJECT_PLAN.md** | 6주 개발 계획, Phase별 상세 작업 |
| **PROJECT_SETUP_GUIDE.md** | Android Studio 프로젝트 생성, 폴더 구조, 권한 |
| **ARCHITECTURE.md** | 레이어 구조, 모듈 역할, 데이터 흐름 |
| **DATA_STORAGE.md** | 이미지/메타데이터 저장 구조 |
| **ERROR_HANDLING.md** | 에러 종류, 사용자 메시지, UI 처리 |
| **API_SPECIFICATION.md** | Repository / UseCase 인터페이스 |

---

## ⚠️ 현재 상태 및 이후 계획

- **현재**: 기획 완료 / 개발 준비 단계
- **다음 단계**: Phase 0 (프로젝트 초기 설정) → Phase 1 (카메라 & 촬영)부터 MVP 기능 구현
- **MVP 이후**: 로그인, 피드, AI 보정, 프레임 마켓, QR 공유 등 확장 기능을 별도 로드맵에 따라 순차 추가 예정

---

## 🔧 기술 스택 요약 (Android)

- **언어**: Kotlin 1.9+
- **UI**: Jetpack Compose
- **카메라**: CameraX
- **이미지 처리**: Android Graphics / Bitmap, 필요 시 RenderScript 등
- **저장**: 앱 전용 디렉터리 + Room 또는 DataStore, MediaStore(갤러리)
- **공유**: Intent.ACTION_SEND (공유 시트)

---

**문서 위치**: `/Pocket-4Cut-Android/QUICK_START.md`
