# Pocket 4Cut 1.5(6) 최종 출시 검증 보고서

## 1. 판정과 검증 기준

2026-09-24에 추가한 앨범 가져오기, 사진별 비파괴 자르기, Everyday Editions 88종 런타임 프레임과 신뢰성·접근성 보강을 합친 뒤 최종 출시 검증을 처음부터 다시 수행했다.

- 애플리케이션 코드 기준: `f970d34a210884431d103f134eea4e88054c32a4`
- 작업 브랜치: `codex/album-import-crop`
- 배포 패키지: `com.pocket4cut`
- QA 패키지: `com.pocket4cut.qa`
- 버전: `1.5`, `versionCode 6` — 사용자 요청대로 변경하지 않음
- 최소/대상/컴파일 SDK: 26 / 36 / 36
- 실기기: Samsung Android 16 / API 36 한 대
- 실기기 앱: 데이터가 분리된 QA 패키지만 설치·검사했으며 기존 배포 앱과 자료는 교체하거나 초기화하지 않음. 최종 확인 뒤 QA 패키지는 제거함.
- 에뮬레이터: 사용자 지시에 따라 미실행

정의한 자동 검사와 한 대의 실기기 수동 흐름은 출시 후보 기준으로 통과했다. 최종 AAB도 같은 애플리케이션 코드에서 다시 생성해 구조·자산·서명을 검사했다. 아래 한계에 적은 Play Console 및 다중 기기 항목은 로컬 통과로 간주하지 않는다.

## 2. 이번 출시 후보의 구현 내용

### 2.1 앨범 가져오기

- 홈에서 `카메라로 촬영`과 `앨범에서 만들기`를 선택할 수 있다.
- 앨범에서도 2·4·6컷을 지원하며 각각 정확히 2·4·6장을 확정한다.
- 시스템 Photo Picker를 사용하고 전체 사진 읽기 권한을 선언하지 않는다.
- 선택한 사진은 앱 전용 import 영역에 원본 바이트를 유지한 채 복사한다. 외부 원본을 덮어쓰거나 삭제하지 않는다.
- 확인 화면에서 추가, 제거, 순서 변경과 접근성용 앞/뒤 이동을 제공한다.
- URI 중복, 부족 선택, picker 취소, 지원하지 않거나 손상된 형식, 부분 가져오기와 재실행 복구를 구분한다.
- import journal은 중단 뒤 완료된 파일을 중복 없이 세션에 반영하고 삭제 의도도 재개한다.

### 2.2 사진별 비파괴 자르기

- 카메라와 앨범 사진 모두 상세 편집에서 슬롯 비율에 맞춰 이동하고 1–4배 확대할 수 있다.
- crop은 원본을 새로 저장하지 않고 `PhotoId`에 연결된 편집값으로 보존한다.
- EXIF 정규화, 사용자 회전·반전, 필터·색 보정, crop, 프레임·장식 순서로 렌더한다.
- 미리보기와 최종 JPEG가 공통 `CropMath`와 슬롯 기하를 사용한다.
- neutral crop은 기존 중앙 aspect-fill과 같고 사진 순서를 바꿔도 crop은 같은 사진을 따라간다.

### 2.3 Everyday Editions 88종

- 10개 카테고리, 일반 occasion 77개, 직접 기록 special 11개, 총 88개를 앱에서 선택할 수 있다.
- 목록은 240×160 WebP 썸네일을 사용하고, 실제 렌더는 테마별 1536×1024 q95 alpha WebP 아틀라스를 동적으로 조합한다.
- 앱에 1,584개 완성 PNG를 넣지 않는다.
- schema v3가 `occasionThemeId`와 `occasionDesignVersion`을 저장한다.
- 미리보기와 최종 내보내기가 같은 카탈로그 항목과 painter를 사용한다.
- 아틀라스 로더의 강한 참조 캐시는 최대 2장이다. eviction된 sheet를 즉시 recycle하지 않아 UI나 내보내기가 보유한 유효 Bitmap을 깨뜨리지 않는다.
- 직접 기록 11종은 날씨·위치·D-Day·횟수를 자동으로 삽입하지 않는다. 기존 문구와 날짜 편집에서 사용자가 직접 입력한다.

### 2.4 신뢰성·접근성 보강

- occasion ID·디자인 버전의 저장, 앱 재실행 복원과 알 수 없는 값의 복구 오류 처리를 연결했다.
- 앨범 가져오기 안내와 실제 저장 실패를 구분하고, 저장·공유 연타를 막았다.
- 공유 intent에 `EXTRA_STREAM`, `ClipData`, 읽기 URI grant를 함께 부여했다.
- 같은 결과의 자동·수동 사진첩 저장이 중복 사본을 만들지 않도록 유지했다.
- 카메라, 설정, 프레임·색상·필터, 작업 보관함과 알림의 주요 조작 영역을 48dp 이상으로 맞추고 역할·선택 상태·TalkBack 이름을 보강했다.
- 앱 이름은 사용자 지정 표기인 `Pocket 4Cut`으로 통일했다.

## 3. 자동 검증

### 3.1 런타임 자산

```powershell
cd design\occasion-frames\everyday-editions-v1\source
npm run validate:android
```

- 성공
- themes 88, categories 10, groups 77+11
- art 88, thumbnails 88
- 검증한 이미지 바이트 33,687,596
- 흰색·검정색 합성 배경 최소 PSNR 42.01dB
- alpha 값 차이 0
- 카탈로그·파일·해시 변조 음성 검사 14건 통과

### 3.2 빌드·JVM·Lint·R8·AAB

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebugAndroidTest :app:assembleQaRelease :app:bundleRelease --rerun-tasks --console=plain
```

- `BUILD SUCCESSFUL`, 5분 55초
- 173개 task 실행
- JVM: 25/25, 실패 0, 오류 0, 건너뜀 0
- Debug Lint: 오류 0, 경고 56, 힌트 6
- Release Lint: 오류 0, 경고 55, 힌트 6
- qaRelease와 release 모두 R8/resource shrinking 경로 생성 성공

남은 Lint는 오류가 아니며 이번 기준보다 증가하지 않았다. 주된 분류는 KTX·의존성 업데이트 제안, 미사용 리소스, Compose modifier 순서, state boxing, 런처 아이콘 형태와 버전 안내다. 기존 문제를 숨기기 위한 새 suppress는 넣지 않았다.

### 3.3 실기기 계측

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest `
  '-Pandroid.testInstrumentationRunnerArguments.renderMemoryStress=true' `
  '-Pandroid.testInstrumentationRunnerArguments.seasonalExport=true' `
  '-Pandroid.testInstrumentationRunnerArguments.seasonalSourceRevision=f970d34a210884431d103f134eea4e88054c32a4' `
  --rerun-tasks --console=plain
```

- Samsung Android 16/API 36 실제 기기에서 146/146 통과
- 실패 0, 오류 0, 건너뜀 0
- XML 합계 192.403초, Gradle 3분 52초
- 세션 schema v1/v2/v3 이전·검증, import journal·형식·삭제·복구, crop·EXIF·회전·반전, Bitmap 수명, 오래된 미리보기, 결과·공유 URI, 접근성 semantics, occasion 렌더와 메모리 소유권을 포함

88종 렌더 매트릭스는 `88개 테마 × 9개 레이아웃/버전 × 캡션 2상태 = 1,584개` 조합이다. 캡션 상태는 문구·날짜가 둘 다 없거나 둘 다 있는 경우다. 390px preview 렌더에서 슬롯 수, 사진 순서, 불투명하고 비어 있지 않은 결과, 캡션 영역의 사진 침범 방지를 검사했다. preview/export의 비례 기하와 정규화 렌더 일치는 12개 아트 프로필 대표 × 2개 레이아웃 × 같은 캡션 2상태인 별도 48개 조합으로 검사했다. crop 계약은 이 1,584개 matrix가 아니라 별도 crop·EXIF·회전·반전 테스트가 담당한다. 각 조합마다 16MP JPEG를 파일로 인코딩한 검사는 아니다.

순차 아틀라스 회귀 검사는 프로세스 전체 native allocator의 GC 시점 값을 128MiB 렌더러 소유량으로 오인하지 않도록 고쳤다. 실제 로더가 강하게 소유한 cache entry 수, `allocationByteCount`, 진행 요청 수를 검사하고 eviction 뒤 기존 sheet가 계속 그려지는지도 확인했다. 단일 검사 5회, occasion 클래스 12/12, Bitmap pipeline 7/7, Bitmap ownership 클래스 5회 15/15를 각각 재확인한 뒤 전체 146개를 통과시켰다.

## 4. 실기기 수동 전체 흐름

### 4.1 카메라 2컷

- 첫 카메라 권한 거부 화면, 시스템 권한 거부, 설정에서 허용 후 복귀 재확인
- 실제 4장 촬영 후 서로 떨어진 2장을 순서대로 선택
- `TWO_HORIZONTAL`, occasion `couple-100` 적용
- 사진별 90도 회전, 반전, crop 1.2배와 위치 이동
- 결과 2350×1878 생성과 사진첩 저장 확인

### 4.2 카메라 4컷

- 촬영 중 전·후면 카메라 전환과 줌 확인
- 실제 8장 촬영 후 4장 선택
- `FOUR_GRID`, 겨울 프레임 적용
- ORIGINAL/SOFT/FILM/BW 필터 전환
- 문구 `FINAL_QA_4CUT`, 날짜 on/off, 사진 재정렬
- 결과 2350×3545 생성 확인

### 4.3 카메라 6컷과 중단 복구

- 카운트다운 10초 설정
- 첫 컷 저장 뒤 두 번째 카운트다운 중 Home 이동
- 백그라운드에서 새 촬영이나 자동 재개가 일어나지 않음 확인
- 앱 강제 종료·재실행 후 홈의 이어하기 확인
- 사용자가 재개한 뒤 기존 1장을 유지하고 총 10장까지 촬영
- 6장 선택, `SIX_COLLAGE`, 커스텀 검정 프레임 적용
- 첫 사진이 큰 대표 슬롯에 들어가는 비대칭 6컷 확인
- 결과 3383×4728 생성과 사진첩 저장 확인

### 4.4 앨범 2·4·6컷

번호와 색을 구분한 합성 JPEG로 세 흐름을 새로 완주했다.

- 2컷: 정확히 2장 선택, 한 장 제거 후 1/2에서 계속 버튼 비활성 계약 확인, 같은 URI 재선택 시 기존 항목을 유지한 중복 안내, 다른 사진 추가 후 2/2와 계속 버튼 복구
- 4컷: 4장 선택·순서 확정·레이아웃·프레임·편집·결과까지 완료
- 6컷: 6장 선택·순서 확정·비대칭 배치·편집·결과까지 완료
- 시스템 Photo Picker가 열리고 앨범 경로에서 카메라 권한이나 전체 사진 읽기 권한을 요구하지 않음 확인
- 기존 완주 결과의 크기와 구성도 다시 확인: 앨범 2컷 1248×3307, 앨범 4컷 2350×3304, 앨범 6컷 3383×4728

### 4.5 편집·저장·공유·보관함

- 사진별 crop, 회전, 반전과 필터가 화면 이동·재실행 뒤 같은 PhotoId에 유지됨 확인
- 같은 결과의 저장을 반복했을 때 MediaStore 사본 수가 24에서 증가하지 않음 확인
- 새 카메라 6컷 저장에서만 사본 수가 24에서 25로 증가
- 완성 결과를 다시 열 때 자동 저장이 재실행되지 않음 확인
- Android 공유 chooser가 표시됨 확인
- 계측 테스트에서 공유 intent의 URI grant 계약과 JPEG SOI/EOI 표식이 든 테스트 파일 바이트 읽기를 확인
- 작업 보관함에서 완성·진행 중 항목, 입력 종류와 재열기 확인

공유의 수동 확인은 chooser 표시까지다. Gmail·메신저 등 독립된 실제 수신 앱에서 전송을 완료하거나 별도 cross-UID 수신 앱이 읽는 검사는 아니다.

### 4.6 설정·화면 조건·접근성

- 앱 테마, 기본 카메라, 카운트다운 1/10초와 기본 3초 복원, 자동 저장, 날짜 기본값 확인
- 인앱 개인정보 설명과 피드백 화면 진입 확인. 외부 메시지는 보내지 않음
- 720×1280 작은 화면, 잠금 가로 화면, 글자 배율 1.0/1.5/2.0, 밝음/어두움에서 주요 행동 접근 확인
- 원래 시스템 상태인 1080×2340, 글자 배율 0.8, 자동 회전 on, 사용자 회전 0, Night custom schedule로 복원
- QA 앱 설정은 전면 카메라 on, 카운트다운 3초, 자동 저장 off, 날짜 off로 복원
- 휴대전화 화면 켜짐 유지 값은 전체 검사 전후 `15`였고 변경하지 않음
- 정확한 앱 이름 `Pocket 4Cut`이 홈과 R8 QA 화면에 표시됨 확인

## 5. R8 QA와 출시 AAB

### 5.1 R8 QA APK

- 경로: `app/build/outputs/apk/qaRelease/app-qaRelease.apk`
- 크기: 68,990,775 bytes
- SHA-256: `744A3548CFE2C1C7A5670EA5F1C2475E11D550E0E4B297DF90060159E3453DBD`
- 실제 기기에 설치한 뒤 cold start 성공: `Status ok`, `LaunchState COLD`, `TotalTime 116ms`
- 홈의 정확한 이름, 카메라·앨범·작업 보관함·설정 진입 확인
- 설정 화면의 `1.5-qa-r8 (6)` 확인
- 앨범 2·4·6컷 선택 화면과 시스템 Photo Picker 열기·취소 복귀 확인
- 관련 앱 crash buffer 기록 없음
- 검사 중 기존 배포 앱 `com.pocket4cut`은 설치 상태와 데이터를 유지

### 5.2 기기 정리

- 이번 검사에서 만든 번호·색상 합성 원본 12개를 정확한 경로로 제거했다.
- 격리된 `com.pocket4cut.qa`만 제거하고 배포 앱 `com.pocket4cut`은 설치 상태로 유지했다.
- 화면 켜짐 유지 값은 정리 뒤에도 `15`였으며 변경하지 않았다.
- 사진첩 저장 검증으로 만들어진 결과 사본은 기존 사용자 결과와 안전하게 구분할 기록이 없어 일괄 삭제하지 않았다.

### 5.3 release AAB

- 경로: `app/build/outputs/bundle/release/app-release.aab`
- 크기: 72,284,509 bytes
- SHA-256: `E9C97A953067B0BACEDD18E2AE18F02AD44A7A762354D1B01529CDB2C36E2826`
- `jarsigner -verify` exit 0, `jar verified`
- 서명 경고: 로컬 인증서는 self-signed이며 timestamp와 공개 인증서 체인이 없다. JDK는 AAB에 대해 `JarFile`/`JarInputStream` 관련 내부 불일치 경고도 출력했다. 로컬 무결성 검사는 통과했지만 Play Console에 등록된 upload certificate와의 일치는 로컬에서 증명하지 못했다.

bundletool 기반 manifest 확인:

- package `com.pocket4cut`
- versionName `1.5`, versionCode `6`
- compileSdk 36, minSdk 26, targetSdk 36
- `CAMERA`
- `WRITE_EXTERNAL_STORAGE`는 maxSdk 28 한정
- `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED` 없음
- 카메라 feature는 `required=false`
- 설정 파일만 포함하는 backup/data extraction 규칙 참조
- FileProvider `exported=false`, `grantUriPermissions=true`
- Photo Picker backport metadata 포함

AAB 내부 확인 결과:

- themes 88, categories 10, groups 77+11
- art WebP 88개, thumbnail WebP 88개
- manifest에 등록된 176개 WebP의 크기·SHA-256 전부 일치
- 검증한 이미지 바이트 33,687,596
- occasion PNG와 incomplete marker 없음
- R8 metadata 존재
- AAB에 포함된 `proguard.map`이 같은 빌드의 mapping 파일과 일치

### 5.4 난독화 매핑

- 경로: `app/build/outputs/mapping/release/mapping.txt`
- 크기: 50,691,035 bytes
- SHA-256: `2DE50D2566BFDD04DAF957688AC1B20C74F7EACCE3DB6B2BC4B0F0B3FCCB551B`

Play Console에 AAB를 올릴 때 이 mapping을 같은 출시의 deobfuscation 파일로 보관한다.

## 6. 제외 및 남은 출시 외부 확인

사용자 지시로 검증하지 않은 항목:

- 에뮬레이터
- 글꼴 라이선스
- 공개 개인정보 웹페이지

한 대의 Android 16/API 36 실기기와 로컬 환경으로 확정할 수 없는 항목:

- API 26·28 실제 저장소 권한 UI와 API 29·33의 실제 OS별 전체 흐름
- 다른 제조사, 화면 크기, 카메라 HAL, 전·후면 센서 방향과 발열 특성
- 실제 저장 공간 고갈, OS 저메모리 kill, 256MiB급 실기기의 장시간 최대 출력
- cloud provider와 실제 HEIF/HEIC 공급자의 수동 선택 흐름
- 실제 Android 백업·복원과 기기 간 이전
- Gmail·메신저 등 독립 실제 수신 앱을 통한 공유 완료
- 88개 프레임 카드를 각각 수동으로 선택·저장·보관함 재열기한 전수 순회. 88개 카탈로그와 렌더 전수 범위는 자동 검사로 확인했다.
- 실제 TalkBack 서비스를 켠 상태의 전체 흐름 음성·탐색 완주. 이번 범위는 계측 semantics와 화면 크기·글자 배율별 조작 가능성 확인이다.
- 45색·25스티커·현재 글꼴을 서로 조합한 모든 경우의 실기기 수동 순회
- Play Console에서 versionCode 6을 사용할 수 있는지 여부
- 로컬 서명 인증서와 등록된 upload certificate의 일치
- 데이터 안전성 양식, 스토어 등록정보, 실제 AAB 업로드·자동 검사·심사·게시

위 항목을 통과로 표시하지 않는다. 현재 AAB는 저장소에서 수행 가능한 정의된 검증을 통과한 출시 후보이며, Play Console 외부 게이트를 완료한 뒤 게시할 수 있다.

## 7. Google Play 업데이트 내용

### 자세한 버전

```text
Pocket 4Cut 1.5

• 카메라뿐 아니라 휴대전화 앨범의 사진으로도 2·4·6컷을 만들 수 있습니다.
• 각 사진의 위치와 확대 비율을 조절하는 비파괴 자르기 기능을 추가했습니다.
• 생일, 여행, 우정, 기념일 등 10개 분류의 Everyday Editions 프레임 88종을 추가했습니다.
• 앨범 사진의 추가·제거·순서 변경과 중단 후 이어서 작업을 지원합니다.
• 미리보기와 고해상도 저장 결과의 사진 순서, 자르기, 필터, 프레임, 문구와 날짜 일치를 개선했습니다.
• 중복 저장 방지, 공유 안정성, 작업 복원과 주요 화면의 접근성을 개선했습니다.
```

### 권장 사용자용 버전

```text
앨범 사진으로도 2·4·6컷을 만들고, 사진마다 원하는 위치와 확대 비율을 조절할 수 있습니다. 생일·여행·우정 등 88종의 새 프레임을 추가했으며, 작업 복원과 고해상도 저장·공유 안정성도 개선했습니다.
```

### 간략한 버전

```text
앨범 가져오기, 사진별 자르기, 새 프레임 88종을 추가하고 저장·공유 안정성을 개선했습니다.
```
