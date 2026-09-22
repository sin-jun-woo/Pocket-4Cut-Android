# Pocket 4Cut 개발 지침

이 지침은 저장소 전체에 적용한다. 사용자의 현재 요청과 명시한 작업 범위를 우선한다.

## 먼저 확인할 것

- 사용자에게 한국어로 설명하고 코드·API 식별자는 원문을 유지한다. 핵심 결과, 근거, 검증 한계를 명확히 말한다.
- 시작할 때 `git status --short`, `git branch --show-current`, `git diff --cached --name-status`로 기존 변경과 작업 범위를 확인한다.
- [README](README.md), [실제 아키텍처](docs/ARCHITECTURE.md), [최근 작업 기록](docs/WORKLOG.md), 이미지 작업이면 [자산 가이드](docs/IMAGE_ASSET_GUIDE.md)를 읽고 관련 코드를 확인한다.
- 사실의 기준은 현재 작업 트리와 실제 호출 관계다. 초기 설계 문서, TODO, 감사 당시의 결함 목록을 현재 구현·검증 결과로 대신하지 않는다.
- 구조·버전·검증 기록에는 기준 시점/커밋과 미커밋 변경 포함 여부를 적고, **확인된 구현 / 향후 제안 / 미검증**을 구분한다.

## 현재 코드의 핵심 경계

- 단일 `:app` Android 모듈, `MainActivity` + Compose Navigation이다. `presentation`, `camera`, `frame`, `data`, `domain`, `core`, `ui`는 패키지다. Room/Hilt/완성된 UseCase 계층 또는 AWS 서버가 있다고 가정하지 않는다.
- 신뢰성 개편은 `SessionDocumentRepository`와 세션별 JSON을 사용한다. `SessionDraft.selectedPhotoIdsInOrder`가 선택·표시 순서이며 사진별 보정은 PhotoId에 귀속된다. stage·revision·결과·내보내기 기록과 기존 데이터 가져오기 경로를 함께 확인한다.
- 활성 결과 생성은 `DetailEditViewModel → RenderSnapshot / CollageRenderer → FileImageStorage / SessionDocumentRepository`다. 미리보기와 내보내기 계약, 순서·배치 버전·출력 크기를 함께 검증한다.
- 앱 전용 촬영 원본·결과 JPEG, 세션 JSON, SharedPreferences, MediaStore 사본은 소유권이 다르다. `GalleryExporter`가 내보내기 작업 기록을 관리한다.
- 이전 `PhotoSession`/`SessionRepository`/`PendingCollageStore`/`CollageFinalize`가 남아 있다. 새 구조가 모든 옛 경로를 제거했다고 가정하지 말고 호출부를 확인한다. NavHost·화면 I/O도 아직 존재한다.

## 확정된 제품 동작

- 2·4·6컷은 각각 4·8·10장 중 2·4·6장을 선택한다.
- 카운트다운은 매 컷 기본 3초, 설정 범위 1–10초다. 촬영 뒤 300ms와 다음 컷 전 2초를 유지한다. 초기 문서의 최초 10초 준비 방식으로 임의 변경하지 않는다.
- 촬영 중단은 일시정지이며 완료된 사진을 보존한다. 사용자가 재개해야 다음 컷의 카운트다운을 시작한다. 진행 중이던 CameraX 요청의 늦은 콜백·파일을 중복 반영하지 않는다.
- 초안은 자동 보존하고 홈/보관함에서 이어간다. 일반 뒤로가기로 삭제하지 않으며 작업 삭제는 사용자 행동과 명확한 삭제 범위에 연결한다.
- 새 비대칭 6컷은 첫 사진의 큰 슬롯과 나머지 다섯 슬롯을 갖는다. 기존 레이아웃은 버전을 구분하여 호환한다.
- 출력 해상도는 기기 화면 크기에 의존하지 않는다. 원본을 보존하고 원본 기반으로 별도 결과를 렌더한다.
- Android 백업·기기 이전 규칙은 설정/계절 테마만 포함한다. 사진·초안·결과·내보내기 기록을 자동 백업에 포함시키지 않는다.

## 변경 원칙

- 승인된 작업은 조사·수정·검증까지 진행하고 같은 승인을 반복 요청하지 않는다. 결과에 영향을 주는 불확실성만 질문한다.
- 문서만 요청받은 작업에서는 앱 소스·Manifest·Gradle·UI·자산을 수정하지 않는다. 발견한 결함은 근거와 후속 검증으로 기록한다.
- 기존 수정·미추적 파일을 보존한다. 작업 정리를 위해 `reset --hard`, `git clean`, 일괄 restore/stash를 사용하지 않는다.
- 변경은 작은 검증 가능한 단위로 진행한다. 무관한 파일 이동, 대규모 포맷 변경, 의존성 업데이트를 끼워 넣지 않는다.
- sessionId/PhotoId, 선택 순서, layout/theme/color ID, JSON schema/revision, 파일 경로를 바꿀 때 기존 저장 데이터의 이전·복구 계획을 마련한다. 과거 사진 인덱스를 이미 선택된 경로 목록에 다시 적용하지 않는다.
- 저장소의 revision 검사·프로세스 공유 잠금·원자적 쓰기를 우회해 UI가 세션 JSON을 직접 덮어쓰지 않는다. 오류를 빈 보관함이나 기본 프레임으로 숨기지 않는다.
- 촬영 원본, 초안, 결과, 공용 사진첩 사본의 삭제 범위를 구분한다. 검증을 위해 사용자 사진을 삭제하거나 앱 데이터를 초기화하지 않는다.
- 비트맵의 Main 스레드 부담, UI가 참조하는 Bitmap 수명, coroutine 취소, 오래된 작업 결과를 확인한다. UI가 보유한 Bitmap을 임의 recycle하지 않는다.
- 이미지 변경은 preview/export의 사진 순서·크롭·좌표·색·글꼴·장식·출력 크기를 함께 검증한다. HTML 시안과 Android 실행 화면을 구분한다.
- 기존 코드 벡터를 이유 없이 래스터로 교체하지 않는다. 신규 자산 규격·출처는 IMAGE_ASSET_GUIDE에 기록한다.
- 시스템/화면 뒤로가기를 같은 정책에 연결하고 회전·프로세스 복원, 권한 거부/복귀, TalkBack, 48dp 조작 영역, 큰 글자·가로 화면·시스템 바 대비를 확인한다.

## 검증과 기록

- 버전은 Gradle 파일이 기준이다. 현재 CI는 JDK 21, SDK 36, Build Tools 36.1.0을 사용한다. 개인 JDK/SDK 절대 경로를 커밋하지 않으며 `--offline`은 의존성이 캐시되어 있을 때만 사용한다.
- 기준 검사는 Windows에서 `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`이다. macOS/Linux는 `./gradlew`를 사용한다.
- 계측 APK 생성은 `:app:assembleDebugAndroidTest`, 실행은 `:app:connectedDebugAndroidTest`다. 최적화 QA 빌드는 `:app:assembleQaRelease`, 배포 AAB는 `:app:bundleRelease`다.
- debug/qaRelease는 `com.pocket4cut.qa`, 배포 앱은 `com.pocket4cut`이다. QA variant끼리는 같은 데이터를 공유한다. 설치·계측 전에 대상 패키지/기기를 확인하고 사용자 배포 앱을 대체하지 않는다.
- 기본 androidTest에는 Bitmap 소유권, 렌더 계약, 출력 크기/배치 버전, 세션 저장·복구·이전·삭제 테스트가 있다. 옛 `scripts/device-diagnostics.init.gradle`로 같은 진단 클래스를 중복 추가하지 않는다. JVM 기본 테스트는 산술 검사이므로 전체 기능의 근거로 삼지 않는다.
- 빌드·JVM 테스트·Lint·계측·수동 UI·release 검증을 각각 보고한다. `UP-TO-DATE`는 캐시 재사용이며 테스트 소스/CI 정의 존재를 실제 실행·통과로 표현하지 않는다.
- 회전 재생성 검사와 실제 프로세스 종료 복원은 다른 검사다. 작은 합성 Bitmap 검사가 실제 센서·고해상도 메모리·모든 기기 검증을 대신하지 않는다.
- 실패를 숨기거나 통과를 위해 기존 결함을 임의 suppress하지 않는다. 관련 검사가 충분히 통과하면 이유 없이 반복하지 않는다.
- 작업 단위로 WORKLOG에 날짜·시간대, 목적/범위, 변경 파일, 기준 커밋, 명령/결과, 미검증·남은 문제를 기록한다. 분담 작업에서는 지정된 기록 담당이 합치고 다른 담당 파일을 수정하지 않는다.
- 사용자 동작·데이터·빌드 절차가 달라지면 README/ARCHITECTURE와 연결 문서를 갱신한다. 과거 검증 기록을 새 성공 결과로 덮어쓰지 않는다.

## Git과 공개 자료

- commit/push는 사용자가 요청한 범위에서 수행한다. main/master에서 작업 커밋이 필요하면 `codex/` 브랜치를 만들고 사용자가 지정한 이름을 우선한다.
- 정확한 파일 경로로 stage하고 `git diff --cached --check`와 staged diff를 검토한다. `git add .`/`git add -A`/`git commit -a`로 기존 변경을 섞지 않는다.
- 일반 push와 upstream 설정을 사용한다. 요청 없이 force push, amend, 기존 이력 재작성을 하지 않는다. 완료 시 변경 파일·검증·브랜치·SHA·push 결과를 보고한다.
- keystore, 서명 암호, 토큰, 실제 자격증명 파일을 출력하거나 커밋하지 않는다. 검증과 무관한 비밀 파일은 열람하지 않는다.
- `docs/` 전체는 GitHub Pages 배포 대상이다. 공개 자료에는 사용자 사진·기기 serial·비밀값·원시 로그·개인 로컬 경로를 넣지 않는다.
