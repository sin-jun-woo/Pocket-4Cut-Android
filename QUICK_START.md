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

### Windows 터미널에서 `JAVA_HOME is not set` 오류가 날 때

Android Studio의 Gradle JDK 설정과 터미널의 Java 환경 변수는 별개다. 이 저장소의 `gradlew.bat`는 `JAVA_HOME`이 있으면 그 경로의 Java를, 없으면 `PATH`의 Java를 찾는다.

1. Android Studio 설치 폴더의 `jbr` 아래에 `bin/java.exe`와 `bin/javac.exe`가 있는지 확인한다. 2026-09-10 로컬 검증에서는 번들 JBR 21.0.10으로 Gradle 8.13 실행과 디버그 빌드가 성공했다. 앱의 Java/Kotlin 타깃 11은 빌드 실행 JDK와 구분한다.
2. Windows **사용자 환경 변수**의 `JAVA_HOME`을 확인한 `jbr` 폴더로 지정한다. 값에 따옴표나 마지막 `bin`을 넣지 않는다.
3. 사용자 `Path`의 기존 항목을 유지하며 해당 `jbr/bin` 폴더의 절대 경로를 한 번만 추가한다.
4. 터미널을 다시 연다. IDE 내부 터미널에서 오류가 계속되면 해당 IDE도 종료 후 다시 실행한다. 이미 실행 중인 프로세스에는 변경된 환경 변수가 자동 적용되지 않을 수 있다.

사용자 환경 변수 등록을 마쳤다면 현재 PowerShell에는 아래와 같이 반영하고 확인할 수 있다.

```powershell
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) { throw '사용자 JAVA_HOME을 먼저 설정하세요.' }
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
java -version
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug --console=plain
```

개인 JDK 절대 경로를 저장소의 `gradle.properties`에 커밋하지 않는다. `org.gradle.java.home`만 설정해도 wrapper가 처음 실행할 Java를 찾지 못하는 이 오류는 해결되지 않는다.

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
