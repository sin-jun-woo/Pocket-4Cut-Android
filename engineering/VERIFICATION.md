# 개발 인수 검증 기록

실행일: **2026-09-09**, 저장소 HEAD `95140a0`, 기존 미커밋 변경을 포함한 작업 트리. 개발 인수 분석 중 앱 소스를 수정하지 않았다. 아래는 이번 실행과 이전 기록을 구분한 결과다.

## 이번에 직접 실행한 것

| 확인 | 결과 | 의미·한계 |
|---|---|---|
| Git 상태·파일 지도 | main Kotlin 85파일, 15,224줄; 기존 수정27파일 | 현재 변경을 포함해 분석. 보존 대상 파악 |
| Gradle/JDK 확인 | Gradle8.13 / JBR21.0.10 / Windows11 | JVM 코드 타깃11과 Gradle 실행 JDK는 구분 |
| :app:assembleDebug | **통과** | 현재 작업 트리 APK 생성. 기기 기능 동작을 증명하지 않음 |
| :app:testDebugUnitTest | **통과, 1개** | addition_isCorrect: 2+2=4. 제품 로직 커버리지는 없음 |
| :app:lintDebug | **실패** | Error1 / Warning95 / Hint2 |
| 실제 레이아웃 class JVM probe | **두 결함 재현** | 8/8 문구 영역 불일치, 두6컷 좌표 동일. Android UI/pixel 렌더 테스트는 아님 |
| ADB 연결 확인 | 기기1대, 모델 SM_S942N, API36 | 기기 식별 serial은 보고서에 저장하지 않음 |
| 설치 앱 메타데이터 | com.pocket4cut 1.2(3), min26/target36, debuggable | 동일 버전명은 최신 빌드와 동일 바이너리라는 증거가 아님 |
| 계층·API 사용 검색 | UseCase/DI/Room 부재, 파일 JSON/SharedPreferences 확인 | 소스 호출 관계와 의존성 선언 기준 |
| 자격증명 현재 추적 여부 | keystore/서명 속성/평문 자격증명 파일 Git 추적 안 됨 | 내용 열람 없음. 과거 이력 전체 감사는 아님 |

이번에는 연결 기기에 앱을 설치하거나 데이터를 삭제하거나 촬영하지 않았다. Android instrumentation/E2E 테스트도 실행하지 않았다. 기존 기기 데이터와 테스트 데이터가 섞이는 방식의 검증은 수행하지 않았다.

### 당시 검증 명령의 재실행 형태

아래는 당시 명령에서 개인 설치 경로를 제외한 재실행 형태다. 사용자 환경 변수 `JAVA_HOME`이 설치된 JDK를 가리키는지 먼저 확인한다. 아래 명령 형태로 다시 실행했다는 뜻은 아니며, 당시 실행 결과는 그대로 보존한다.

```powershell
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:GRADLE_USER_HOME = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.gradle'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain
```

결합 명령은 마지막 lintDebug 실패 때문에 exit1이었다. 앞서 assembleDebug와 testDebugUnitTest는 완료되었다. `clean`이나 의존성 버전 변경은 하지 않았다. 다른 환경에서는 JDK/SDK 위치와 Gradle 캐시를 해당 환경에 맞춘다. 캐시에 의존성이 없으면 offline 빌드를 재현할 수 없다.

처음 제한 환경의 기본 Gradle 홈은 시스템 드라이브 루트의 `.gradle`로 해석되어 lock 디렉터리를 만들지 못했다. 작업공간 `.gradle`의 로컬 배포본으로는 Gradle 버전은 확인했지만 오프라인 AGP plugin marker를 해석하지 못했다. 이후 기존 사용자 Gradle 캐시를 사용해 실제 앱 빌드·테스트·Lint를 완료했다. 이는 실행 환경 문제와 소스 검증 실패를 구분하기 위한 기록이다.

### 결과 파일

- APK: [app-debug.apk](../app/build/outputs/apk/debug/app-debug.apk)
- 단위 테스트 XML: [TEST-com.pocket4cut.ExampleUnitTest.xml](../app/build/test-results/testDebugUnitTest/TEST-com.pocket4cut.ExampleUnitTest.xml)
- 단위 테스트 HTML: [index.html](../app/build/reports/tests/testDebugUnitTest/index.html)
- Lint: [HTML](../app/build/reports/lint-results-debug.html), [XML](../app/build/reports/lint-results-debug.xml)

build 폴더 산출물은 재빌드로 바뀌거나 정리될 수 있다. 보존할 사실은 이 문서에 적었다. 이번 단위 테스트 XML은 tests=1, failures=0, errors=0, skipped=0, timestamp=2026-09-09T14:33:45.678Z였다.

### Lint 내용

차단 오류는 `app/src/main/AndroidManifest.xml:6`의 **PermissionImpliesUnsupportedChromeOsHardware**다. CAMERA permission에 대응하는 하드웨어 uses-feature 선언이 없다. 이 선언은 이번 분석에서 수정한 부분이 아니다. 지원 기기 정책을 정한 후 해결할 사안으로 기록했다.

| 종류 | 개수 |
|---|---:|
| UseKtx | 49 |
| GradleDependency | 11 |
| ModifierParameter | 7 |
| UnusedResources | 7 |
| IconDuplicates | 5 |
| IconLauncherShape | 5 |
| NewerVersionAvailable | 3 |
| IconXmlAndPng | 2 |
| AndroidGradlePluginVersion | 1 |
| ClickableViewAccessibility | 1 |
| RedundantLabel | 1 |
| SelectedPhotoAccess | 1 |
| StaticFieldLeak | 1 |
| UseOfNonLambdaOffsetOverload | 1 |
| **경고 합계** | **95** |
| AutoboxingStateCreation 힌트 | 2 |

업데이트 알림을 곧바로 버그나 일괄 업데이트 지시로 해석하지 않는다. 실제 동작 정확성·저장 안전성 문제는 Lint가 잡지 못하므로 [KNOWN_ISSUES](KNOWN_ISSUES.md)와 별개다.

## 실제 기하 함수 검증

`app/build/tmp/kotlin-classes/debug`의 실제 FrameLayouts/FrameStyle/FrameTheme/CollageLayoutMath/CollageLayoutDimensions를 JVM에서 호출했다. Android의 `RectF`만 필드4개와 생성자를 가진 좌표 holder로 대체했다. Kotlin 표준 라이브러리를 로드하고 색상은 수식에 사용되지 않는 fixture 값으로 넣었다. 공통 theme의 outerPadding=20, cellSpacing=10을 사용했다.

이 방식은 **프로덕션 기하 수식을 복제하지 않고 실행**하며 Canvas, Compose, 카메라, Bitmap 렌더 품질은 검증하지 않는다. 아래 숫자는 동일 fixture의 비교값이며 모든 실제 테마의 화면 크기 표가 아니다.

| 레이아웃 | preview 문구 영역 | export 문구 영역 | preview 높이@폭390 | export 높이@폭390 |
|---|---|---|---:|---:|
| TWO_VERTICAL | 없음 | 있음 | 1028.33 | 1068.33 |
| TWO_HORIZONTAL | 없음 | 있음 | 311.67 | 351.67 |
| FOUR_VERTICAL | 없음 | 있음 | 1162.91 | 1162.91 |
| FOUR_GRID | 없음 | 있음 | 548.33 | 588.33 |
| FOUR_HORIZONTAL | 없음 | 있음 | 191.67 | 231.67 |
| SIX_GRID_2X3 | 없음 | 있음 | 785.00 | 825.00 |
| SIX_GRID_3X2 | 없음 | 있음 | 388.33 | 428.33 |
| SIX_COLLAGE | 없음 | 있음 | 388.33 | 428.33 |

클래식 4컷은 높이가 고정이므로 높이만 비교하면 결함을 놓친다. textArea와 사진 셀 높이를 함께 비교해야 한다. SIX_GRID_3X2와 SIX_COLLAGE는 캔버스 높이와 모든 사진 셀의 left/top/right/bottom이 같았다.

임시 검증 Java 소스와 class는 Git 제외 영역 `.gradle/project-audit/`에 두었다. 일반 제품 회귀 테스트를 추가한 것은 아니다. 관찰 요약 출력은 `captionMismatches=8/8 sixCollageEqualsGrid3x2=true`였다. 수정 후에는 문구 영역의 정규화 일치와 제품 의도에 따른6컷 차이를 검증하는 지속 가능한 테스트로 대체할 수 있다.

## 이전 기록과의 구분

`design/mockups/print-booth-v1/VERIFICATION.md`의 이전 작업도 debug build/기본 unit 성공과 동일 Lint 실패를 기록한다. 그때는 “ADB 연결 기기 없음”이었지만 **이번에는 API36 기기1대 연결을 직접 확인**했다. 두 시점의 결과를 합쳐 기기 E2E를 수행했다고 해석하면 안 된다.

기존 release AAB가 디스크에 있으나 이번에는 release bundle/서명 검증/Play Console 상태를 확인하지 않았다. 디자인 PNG와 render-validation.json은 HTML 시안 검증이며 Android UI 테스트가 아니다.

기존 `scripts/e2e-smoke-adb.ps1`은 이번에 실행하지 않았다. 경로·기기·좌표 가정이 고정되어 있고 timeout 뒤에도 계속 진행한다. 최종 파일/세션 값은 출력만 하며 생성 여부와 픽셀 품질을 assertion하지 않는다. 결과 메시지 “OK”만으로 전체 E2E 성공을 주장할 수 없다.

## 후속 기능 검증 표

아래 항목은 **미실행**이다. 사진·저장·삭제 테스트는 식별 가능한 테스트 이미지와 격리 데이터를 사용해야 한다.

| 영역 | 검증 시나리오 | 통과 기준 |
|---|---|---|
| 전체 흐름 | 2/4/6컷 자동 촬영부터 선택·편집·결과·보관함 | 구성별 장수 정확, 결과 존재·decode 가능, 세션 일치 |
| 선택/정렬 | 각 사진에 번호를 넣고 선택 순서와 재정렬 혼합 | 상세 보정 슬롯과 최종 순서까지 사진 ID 동일 |
| 프레임 | 레이아웃8개 × 색/계절/커스텀 대표 | 선택 상태와 결과 일치, 주요 조합 golden image 비교 |
| 문구 | 빈 문구, 날짜만, 한글/이모지, 긴 문구, 폰트13개 | 영역·글자·위치·잘림 정책 일치 |
| 장식 | 각 kind, 가로/세로 프레임, 확대/회전/이동 | preview/export의 정규화 위치·크기·색·회전 일치 |
| 이미지 보정 | 전체 필터4종 × 개별 회전·반전·보정 | 중복 필터 없음, 원본 비파괴, 슬롯 대응 유지 |
| 카메라 | 전후면 비대칭 표적, 회전, 줌, 권한 거부/재허용 | 방향·반사 정책 준수, 준비/재시도/취소 상태 일관 |
| 수명 | 각 편집 단계 회전·백그라운드·프로세스 종료 | 정의한 초안 복원 또는 명확한 복구 안내 |
| 저장 | API26/28/29/33/36, 권한 거부, 중복 진입 | 저장 성공 또는 명확한 오류, 중복 자동 저장 없음 |
| 실패 | 공간 부족, 중간 decode 실패, 손상 JSON | 원본/기존 보관함 보존, 정리·재시도 가능 |
| 삭제 | 촬영 취소/선택 종료/개별/전체 삭제 | 정의한 원본·결과·초안 삭제, 공용 사본 범위 명확 |
| 성능 |6컷 반복 편집, 빠른 필터/슬라이더, 동시 렌더 | OOM/ANR/오래된 결과 덮어쓰기/recycled bitmap 오류 없음 |
| UI | 작은 화면·가로·큰 글꼴·키보드·시스템 inset | CTA 접근 가능, 주요 콘텐츠/문구 안 잘림 |
| 접근성 | TalkBack으로 촬영·선택·수정·저장 | 모든 조작에 이름/역할/상태/값 제공 |
| 백업 | 사진·메타데이터·초안 포함/제외와 복원 | 명시한 정책과 실제 데이터 범위/경로 일치 |
| 배포 | release build, 서명, manifest, 정책/폰트 고지 | 현재 버전과 배포 자료 일치, 필요한 검증 증거 확보 |

초기 회귀 테스트 우선순위는 선택/정렬 계약, 기하·문구 영역, 프레임 ID round-trip, 장식 단위, 저장/삭제 수명이다. 현재 기본 산술 테스트의 성공은 이 항목을 대체하지 않는다.
