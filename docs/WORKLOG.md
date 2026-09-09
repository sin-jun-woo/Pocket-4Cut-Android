# Pocket 4Cut 작업 로그

작업 단위로 최신 기록을 위에 추가한다. 날짜는 별도 표기가 없으면 Asia/Seoul(KST) 기준이다. 실제 수행 결과와 향후 계획을 구분하고, 실패·캐시 재사용·미실행을 성공으로 합치지 않는다.

`docs/`는 GitHub Pages 배포 대상이므로 공개 가능한 요약만 기록한다. 비밀값, 사용자 사진, 기기 serial, 개인 로컬 경로, 원시 실행 로그를 넣지 않는다. 세부 실행 산출물은 로컬 build/캐시 영역에 두고 필요한 명령·결과만 남긴다.

## 2026-09-10 — Windows JAVA_HOME 오류 해결 (Asia/Seoul)

- 요청: 터미널의 `JAVA_HOME is not set` 및 Java PATH 미발견 오류 해결.
- 기준: `codex/setup-project-guidance`, HEAD `ac42903` 및 기존 미커밋 UI·버전·이미지·문서 변경이 있는 작업 트리. 기존 변경은 보존하고 이번 안내와 로그 추가분만 커밋 대상으로 구분했다.
- 확인: Android Studio 번들 JBR 21.0.10의 `java`와 `javac`는 직접 실행 가능했지만, 기존 터미널 환경에서 Java를 찾지 못했다. IDE의 로컬 Gradle JDK 지정과 wrapper의 `JAVA_HOME`/`PATH` 조회가 별개임을 확인했다.
- 조치: 이 PC의 사용자 환경 변수 `JAVA_HOME`을 확인된 JBR로 영구 등록하고, 기존 사용자 `Path` 항목을 보존하며 JDK `bin`을 추가했다. Windows에 환경 변경 알림을 보냈다. 앱 소스·Gradle 설정·이미지 에셋은 수정하지 않았다.
- 변경 파일: `QUICK_START.md`에 설정·터미널 재시작·현재 PowerShell 갱신 방법을 추가하고, `docs/WORKLOG.md`에 이번 기록을 추가했다. OS 환경 변수 자체는 Git 커밋에 포함되지 않는다.
- 검증: 저장된 사용자 환경 변수를 별도 PowerShell 실행에서 다시 읽고, `PATH`의 JDK 항목이 한 개이며 `java`가 해당 JDK로 해석되는 것을 확인했다. `java -version`과 `javac -version`은 21.0.10, `./gradlew.bat --version --console=plain`은 Gradle 8.13 / Launcher JVM 21.0.10으로 성공했다.
- `./gradlew.bat :app:assembleDebug --offline --console=plain`: **성공**, 36개 태스크 중 8개 실행 / 28개 UP-TO-DATE. 기존 사용자 Gradle 캐시를 이번 검증 프로세스에서 지정해 사용했다. 완전한 재컴파일이나 새 기능 테스트로 간주하지 않는다.
- 범위/잔여 확인: Java 실행 환경 수정이므로 단위 테스트·Lint·실기기 검사는 재실행하지 않았다. 이전 CAMERA Lint 오류를 수정한 작업이 아니다. 이미 실행 중인 터미널/IDE는 재시작하거나 문서의 PowerShell 갱신 명령을 적용해야 한다.

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
