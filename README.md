# 포켓 네 컷 (Pocket 4Cut)

Android에서 연속 촬영한 사진을 골라 2·4·6컷 콜라주로 편집하고 저장·공유하는 앱입니다. Kotlin, Jetpack Compose, CameraX를 사용합니다.

이 문서는 **2026-09-22 신뢰성 개편**, 후속 **Paper Seasons**, `codex/release-readiness`의 출시 준비 변경을 반영합니다. 구현된 경로와 검증 완료 범위는 다릅니다. 실행한 검사·남은 문제·커밋 기준은 [출시 준비 보고서](engineering/RELEASE_READINESS_2026-09-22.md)와 [작업 기록](docs/WORKLOG.md)을 확인하세요. 초기 기획 문서의 기술 스택이나 일정은 현재 구현의 근거가 아닙니다.

## 앱 사용 흐름

1. 홈에서 촬영을 시작하고 2·4·6컷을 선택합니다.
2. 각각 4·8·10장을 촬영한 뒤 2·4·6장을 선택합니다.
3. 레이아웃과 색상·계절·커스텀 프레임을 고릅니다.
4. 필터, 문구, 날짜, 사진 순서를 편집하고 사진별 회전·반전·밝기·대비·채도를 조절합니다.
5. 완성한 JPEG를 앱 보관함에서 열거나 사진첩에 내보내고 Android 공유 시트로 공유합니다.

계절 프레임 4종은 **Paper Seasons** 일러스트 컬렉션으로 새로 구성했습니다. 봄의 벚꽃·튤립, 여름의 바다 소품, 가을의 낙엽·커피, 겨울의 니트·눈꽃을 2·4·6컷 전체 8배치의 실제 여백에 맞춰 그립니다. 기존 계절 ID와 구형 6컷 배치는 유지하며 이미 저장한 사진은 변경하지 않습니다. [프레임 이미지·홍보 자료](design/seasonal-frames/paper-seasons-v1/README.md)와 [자산 제작 기록](docs/IMAGE_ASSET_GUIDE.md)을 참고하세요.

카운트다운은 **매 컷마다** 적용하며 기본 3초, 설정 범위는 1–10초입니다. 촬영 처리 뒤 300ms와 다음 컷 전 2초 대기도 있어 실제 셔터 간격은 촬영·파일 처리 시간에 따라 달라집니다. 최초 한 번만 10초를 기다리는 방식은 현재 동작이 아닙니다.

촬영 중 앱을 벗어나면 일시정지하고 완료된 컷을 보존합니다. 돌아온 뒤 **이어서 촬영**을 누르면 남은 컷을 촬영합니다. 촬영 요청이 이미 전달된 한 컷은 중단 시점에 완료될 수 있습니다.

진행 중인 작업은 초안으로 보존합니다. 홈에서 최근 작업을 이어가거나 보관함의 진행 중인 작업 목록에서 재개·삭제할 수 있습니다. 손상된 미완료 결과와 세션 문서는 보관함에서 구분해 보여 주며, 확인한 결과 후보만 격리하거나 이전 정상본 복구를 시도할 수 있습니다. 정상본이 없어 읽을 수 없는 세션은 원본 파일을 보존한 채 보관함에서 숨길 수 있고, 숨긴 기록 관리에서 다시 표시할 수 있습니다. 이때 파일 소유권을 확인할 수 없으므로 숨긴 기록이 있는 동안 다른 세션의 실제 파일 삭제는 중단됩니다. 기록된 촬영 원본이 누락·손상됐을 때는 자동 재촬영하지 않고 작업을 멈춥니다. 작업을 버리는 행동과 일반 뒤로가기는 구분합니다.

## 설정과 데이터

- **화면 자동 꺼짐 방지:** 기본 OFF입니다. ON이면 앱을 사용하는 동안 화면을 유지하고, OFF이면 휴대폰의 화면 꺼짐 시간을 따릅니다.
- **전면 카메라 기본:** 기본 ON입니다. 카메라 권한이 필요합니다.
- **자동 사진첩 저장 / 날짜 기본 표시:** 모두 기본 OFF입니다.
- **보관함 삭제:** 앱이 소유한 세션·초안·사진 파일을 정리합니다. 이미 사진첩에 내보낸 공용 사본은 유지합니다. 전체 삭제는 진행 중인 작업도 포함하며 확인창에서 범위를 표시합니다.
- **Android 백업:** 설정과 선택한 계절 테마만 포함하도록 규칙을 지정했습니다. 사진·초안·결과·내보내기 기록은 백업 대상에서 제외합니다. 실제 클라우드 백업·기기 이전의 검증 여부는 작업 기록을 확인하세요.

저장은 서로 다른 세 경로를 사용합니다.

- 앱 내부 JSON: 세션별 `SessionDocument`에 사진 ID, 편집 초안, revision, 결과와 내보내기 작업 기록을 저장합니다.
- 앱 전용 Pictures: 촬영 JPEG 원본과 완성 결과를 저장합니다. 새 촬영 경로는 원본을 축소·재압축해 덮어쓰지 않으며 결과는 별도 파일로 만듭니다.
- 공용 사진첩: `GalleryExporter`가 MediaStore에 사본을 만들고 결과 ID별 저장 기록을 확인하여 같은 결과의 재열기·회전으로 새 사본을 만들지 않도록 처리합니다. Android 8/9에서는 쓰기 권한이 필요합니다.

출력 크기는 화면 픽셀 수 대신 프레임 기하로 정합니다. 현재 계산은 사진 슬롯의 짧은 변 1024px을 목표로 하며 전체 1600만 픽셀·한 변 8192px을 상한으로 둡니다. 입력 사진의 해상도와 프레임 구성에 따라 실제 인쇄 품질은 달라집니다. 새 비대칭 6컷은 첫 사진을 큰 슬롯에 배치하고, 기존 배치 데이터는 버전으로 구분합니다.

## 실제 코드 구조

Gradle 모듈은 `:app` 하나이며 `MainActivity`와 Compose Navigation으로 화면을 연결합니다. Room, Hilt, 로그인 또는 AWS 백엔드는 현재 구현에 없습니다.

- [presentation](app/src/main/java/com/pocket4cut/presentation): 화면·ViewModel·내비게이션·화면 사이 작업 인계.
- [camera](app/src/main/java/com/pocket4cut/camera): CameraX 바인딩과 촬영 콜백.
- [SessionDocument](app/src/main/java/com/pocket4cut/domain/model/SessionDocument.kt): 사진 ID, 선택 순서, 사진별 보정, 초안·결과·내보내기 모델.
- [SessionDocumentRepository](app/src/main/java/com/pocket4cut/data/local/SessionDocumentRepository.kt): 세션 JSON, revision 검사, 원자적 쓰기·복구, 이전 데이터 가져오기와 삭제.
- [GalleryExporter](app/src/main/java/com/pocket4cut/data/export/GalleryExporter.kt): MediaStore 내보내기와 공유 URI 검증.
- [frame](app/src/main/java/com/pocket4cut/frame): 배치·필터·스티커, 미리보기, 렌더 입력 스냅샷, 최종 Canvas 합성.
- [ui/designsystem](app/src/main/java/com/pocket4cut/ui/designsystem), [ui/theme](app/src/main/java/com/pocket4cut/ui/theme): 공통 UI와 실제 MaterialTheme 연결.
- [core](app/src/main/java/com/pocket4cut/core): Bitmap 처리, 폰트, 파일 URI 등 공통 코드.

활성 결과 생성은 `DetailEditViewModel → RenderSnapshot / CollageRenderer → FileImageStorage / SessionDocumentRepository`입니다. 이전 `PhotoSession`, `SessionRepository`, `PendingCollageStore`, `CollageFinalize`도 호환·잔여 경로에 존재하므로 이름만으로 현재 저장 경로를 판단하지 마세요. NavHost와 화면에도 일부 I/O가 남아 있습니다. 자세한 경계는 [현재 아키텍처](docs/ARCHITECTURE.md)를 참고하세요.

## 개발 환경

- JDK 21을 권장합니다. 저장소 CI도 JDK 21을 사용합니다. Gradle 실행 JVM과 소스의 Java 11 호환 설정은 별개입니다.
- Android SDK Platform 36, Build Tools 36.1.0, Platform Tools가 필요합니다.
- Gradle Wrapper 9.1.0, Android Gradle Plugin 9.0.1을 사용합니다. AGP의 내장 Kotlin과 Compose 컴파일러 플러그인을 사용하며 버전은 [버전 카탈로그](gradle/libs.versions.toml)에 있습니다.
- 앱의 최소 OS는 Android 8/API 26, compile/target SDK는 36입니다. 현재 버전은 1.4(5)이며 실제 값은 [app/build.gradle.kts](app/build.gradle.kts)가 기준입니다.

Android Studio에서 저장소 루트를 열고 Gradle JDK와 Android SDK 위치를 지정한 뒤 동기화하세요. CLI에서는 `JAVA_HOME`과 `PATH`가 같은 유효한 JDK를 가리키도록 설정하고 확인합니다. 개인 JDK/SDK 절대 경로는 커밋하지 않습니다.

```powershell
java -version
.\gradlew.bat --version
```

## 빌드와 테스트

저장소 루트에서 실행합니다. macOS/Linux에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용합니다. `--offline`은 필요한 의존성이 이미 캐시에 있을 때만 추가합니다.

```powershell
# 앱 빌드, JVM 테스트, Android Lint
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain

# 계측 APK 생성 / 연결된 QA 기기에서 계측 실행
.\gradlew.bat :app:assembleDebugAndroidTest --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain

# R8 최적화를 적용한 QA APK
.\gradlew.bat :app:assembleQaRelease --console=plain

# 배포용 App Bundle
.\gradlew.bat :app:bundleRelease --console=plain
```

`debug`와 `qaRelease`의 applicationId는 `com.pocket4cut.qa`입니다. 배포 앱 `com.pocket4cut`과 데이터를 분리하지만 **두 QA variant끼리는 같은 앱 데이터를 사용**합니다. 계측은 QA 앱 설치·프로세스 상태에 영향을 주므로 진행 중인 QA 작업을 먼저 마칩니다. `qaRelease`는 디버그 키로 서명한 검사 전용 APK이며 배포용이 아닙니다.

배포 서명은 로컬 `keystore.properties`가 있을 때 구성합니다. 서명 파일·암호·키는 저장소에 추가하지 않습니다. 서명 정보 없이 생성한 결과를 바로 배포 가능한 AAB로 취급하지 마세요.

[계측 테스트](app/src/androidTest/java/com/pocket4cut)는 Bitmap 수명, 렌더 순서·캡션·색상·스티커 계약, 출력 크기·6컷 기하, 세션 revision·복구·이전 데이터·삭제 경계를 다룹니다. [JVM 테스트](app/src/test/java/com/pocket4cut)는 아직 기본 산술 검사입니다. 파일의 존재가 실행·통과를 뜻하지 않으며 계측 통과도 실제 카메라와 전체 UI 검증을 대신하지 않습니다.

`RenderContractDiagnosticTest`는 기본 androidTest에 포함되어 있습니다. 과거 `scripts/device-diagnostics.init.gradle`로 별도 진단 소스를 추가하는 명령과 혼용하지 않습니다.

[Android CI](.github/workflows/android-ci.yml)는 빌드·JVM 테스트·Lint·최적화 QA APK와 API 26/28/29/33/36 에뮬레이터 계측을 정의합니다. `main`·`codex/**` push와 PR에서 시작하며 워크플로 정의와 실제 원격 실행 성공은 구분해야 합니다.

## 검증 기록과 남은 확인

- [WORKLOG](docs/WORKLOG.md): 변경별 실행 명령, 실제 결과, 기준 커밋과 남은 위험.
- [2026-09-22 감사](engineering/EMULATOR_AUDIT_2026-09-22.md): 신뢰성 개편 **이전** 버전에서 재현한 기준 결함과 증거. 현재 코드의 수정 완료 판정은 후속 재검사 기록으로 확인합니다.
- [신뢰성 개편 검증 보고서](engineering/REFACTOR_VERIFICATION_2026-09-22.md): 감사 25개 항목의 현재 코드 근거, API 37 검사, 남은 완료 조건과 우선순위.
- [출시 준비 검증 보고서](engineering/RELEASE_READINESS_2026-09-22.md): 이번 작업의 변경, 실제 빌드·기기 결과, 출시 판정과 외부 확인 조건.
- [ARCHITECTURE](docs/ARCHITECTURE.md): 실제 호출 관계와 데이터 경계.
- [AGENTS](AGENTS.md): 기존 작업 보존, 테스트, Git, 공개 자료 작성 규칙.
- [이미지 자산 가이드](docs/IMAGE_ASSET_GUIDE.md): 자산 출처와 생성·수정 기준.

루트의 초기 기획·설계 문서는 역사 자료이며 현재 API나 설치 절차를 보장하지 않습니다. 실제 센서별 촬영·중단 복귀, OS에 의한 프로세스 종료, 낮은 저장 공간·메모리, API 26을 포함한 OS 차이, TalkBack, 가로·분할 화면·큰 글자, 백업 복원, 최적화 release 실행과 Play 배포는 각각 수행한 증거가 있어야 검증 완료로 표시합니다.
