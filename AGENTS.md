# Pocket 4Cut 개발 지침

이 지침은 저장소 전체에 적용한다. 사용자의 현재 요청과 명시한 작업 범위를 우선한다.

## 먼저 확인할 것

- 사용자에게 한국어로 설명하고 코드·API 식별자는 원문을 유지한다. 핵심 결과, 근거, 검증 한계를 명확히 말한다.
- 시작할 때 `git status --short`, `git branch --show-current`, `git diff --cached --name-status`로 기존 변경과 작업 범위를 확인한다.
- [실제 아키텍처](docs/ARCHITECTURE.md), [최근 작업 기록](docs/WORKLOG.md), 이미지 작업이면 [자산 가이드](docs/IMAGE_ASSET_GUIDE.md)를 읽고 관련 코드를 확인한다.
- 사실의 기준은 현재 작업 트리와 실제 호출 관계다. 루트의 초기 설계 문서, TODO, 출시 문구를 구현·검증 완료의 증거로 쓰지 않는다.
- 구조·버전·검증 기록에는 기준 시점/커밋과 미커밋 변경 포함 여부를 적고, **확인된 구현 / 향후 제안 / 미검증**을 구분한다.

## 현재 코드의 핵심 경계

- 단일 `:app` Android 모듈, `MainActivity` + Compose Navigation이다. `presentation`, `camera`, `frame`, `data`, `domain`, `core`, `ui`는 패키지이며 독립 Gradle 모듈이 아니다.
- 실제 상태는 5개 ViewModel과 화면 state에 분산되어 있다. NavHost와 화면에도 I/O가 있다. Room/Hilt/완성된 UseCase 계층 또는 AWS 서버가 있다고 가정하지 않는다.
- 실제 저장은 JSON 세션/편집 초안, 앱 전용 JPEG, SharedPreferences다. MediaStore에 내보낸 사진첩 사본은 별도 데이터다.
- 실제 결과 생성은 `DetailEditViewModel → CollageRenderer → FileImageStorage/SessionRepository`다. 이전 `CollageFinalize` 경로와 혼동하지 않는다.

## 변경 원칙

- 승인된 작업은 조사·수정·검증까지 진행하고 같은 승인을 반복 요청하지 않는다. 요구사항이 모호하면 독립적인 조사를 진행하고 결과에 영향을 주는 불확실성만 확인한다.
- 문서만 요청받은 작업에서는 앱 소스·Manifest·Gradle·UI·자산을 수정하지 않는다. 발견한 결함은 근거와 후속 검증으로 기록한다.
- 기존 수정·미추적 파일을 보존한다. 작업 정리를 위해 `reset --hard`, `git clean`, 일괄 restore/stash를 사용하지 않는다.
- 기능 변경은 작은 검증 가능한 단위로 진행한다. 요청과 무관한 파일 이동, 대규모 포맷 변경, 의존성 업데이트를 끼워 넣지 않는다.
- sessionId, 선택/정렬 인덱스, layout/theme/color ID, JSON 필드, 파일 경로를 바꿀 때 기존 저장 데이터의 이전·복구 계획을 함께 마련한다.
- 촬영 JPEG, 편집 JSON, 보관함 metadata, 결과 JPEG, 공용 사본의 소유권과 삭제 범위를 구분한다. 사용자 사진 삭제·앱 데이터 초기화로 테스트하지 않는다.
- 이미지 변경은 preview와 export의 사진 순서·좌표·색·글꼴·장식·출력 크기를 함께 검증한다. HTML 시안과 Android 실행 화면을 구분한다.
- 비트맵 연산은 Main 스레드 부담, 메모리 소유권, recycle 시점, coroutine 취소와 오래된 작업 결과를 확인한다.
- 현재 코드 벡터 자산을 이유 없이 래스터로 교체하지 않는다. 신규 자산 규격은 IMAGE_ASSET_GUIDE의 현재 사실과 제안을 구분해 적용한다.

## 검증과 기록

- 변경에 맞는 검사를 실행한다. 기준 검증을 요청받으면 저장소 루트에서 Windows는 `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`, 그 외는 `./gradlew`로 실행한다.
- JDK/SDK/Gradle 캐시는 현재 환경에 맞춰 사용하고 개인 절대 경로를 프로젝트 설정에 커밋하지 않는다. `--offline`은 의존성이 캐시되어 있을 때만 사용한다.
- 빌드, 단위 테스트, Lint를 각각 보고한다. `UP-TO-DATE`는 캐시 재사용으로 표시하고 새 테스트 실행·실기기 검증으로 표현하지 않는다.
- 현재 테스트는 기본 산술/패키지 확인 수준이다. 통과만으로 촬영·편집·저장·공유가 검증됐다고 말하지 않는다. 실패·미실행·기존 문제를 숨기지 않는다.
- 검사 통과를 위해 기존 실패를 임의 suppress하거나 문서 작업 중 기능 코드를 고치지 않는다. 관련 변경 후 검사 한 번이 충분하면 이유 없이 반복하지 않는다.
- 작업 단위로 WORKLOG에 날짜·시간대, 목적/범위, 변경 파일, 기준 커밋, 명령/결과, 미검증·남은 문제를 기록한다. 과거 기록을 새 성공 결과로 덮어쓰지 않는다.

## Git과 공개 자료

- commit/push는 사용자가 요청한 범위에서 수행한다. main/master에서 작업 커밋이 필요하면 `codex/` 브랜치를 만들고 사용자가 지정한 이름을 우선한다.
- 정확한 파일 경로로 stage하고 `git diff --cached --check`와 staged diff를 검토한다. `git add .`/`git add -A`/`git commit -a`로 기존 변경을 섞지 않는다.
- 일반 push와 upstream 설정을 사용한다. 요청 없이 force push, amend, 기존 이력 재작성을 하지 않는다. 완료 시 변경 파일·검증·브랜치·SHA·push 결과를 보고한다.
- keystore, 서명 암호, 토큰, 실제 자격증명 파일을 출력하거나 커밋하지 않는다. 검증과 무관한 비밀 파일은 열람하지 않는다.
- `docs/` 전체는 GitHub Pages 배포 대상이다. WORKLOG에는 공개 가능한 작업 요약만 적고 사용자 사진·기기 serial·비밀값·원시 로그·개인 로컬 경로는 제외한다.
