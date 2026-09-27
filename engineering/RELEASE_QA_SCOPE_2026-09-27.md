# Pocket 4Cut 출시 QA 범위 목록 — 2026-09-27

이 문서는 **2026-09-27 작성 시점의 작업 트리(미커밋 변경 포함)**에서 확인한 화면 경로·조작과 테스트 정의의 목록이다. 실행 결과 보고서가 아니다. 최종 검사 담당자가 동일 APK/소스 기준의 실행 기록을 별도로 연결해야 한다. 이 문서 작성 과정에서는 기기 조작·빌드·Git 변경을 수행하지 않았다.

## 범위와 판정 방식

- 현재 실제 기기 검증 범위는 **휴대전화 1대, Android 16/API 36**이다. 다른 제조사·기기·API의 성공으로 확장하지 않는다.
- 세로 화면과 기본/큰 글자 조작을 다룬다. **가로 전용 기능·검사·개선은 이번 요청에서 제외**한다. 세로 고정 정책 변경도 이 문서의 작업 범위가 아니다.
- **번들 폰트 라이선스 조사와 공개 개인정보 처리방침 페이지 검증은 제외**한다. 앱 내부 설정에서 해당 화면으로 이동하는 경로의 존재는 경로 목록에만 남긴다.
- **화면 자동 꺼짐 방지 설정은 변경하지 않는다.** 기존 설정의 존재를 이번 수정·통과 실적으로 기록하지 않는다.
- 최종 Gradle 정의는 사용자 버전업 요청을 반영한 `versionName 1.7`, `versionCode 8`, `minSdk 26`, `compileSdk/targetSdk 36`이다. 초기 QA의 1.6(7)과 변경 후 검사는 실행 보고서에서 구분한다. `debug/qaRelease`는 `com.pocket4cut.qa`, 배포 앱은 `com.pocket4cut`이다.
- 체크박스는 실행 결과 기입 대기이며 실패를 뜻하지 않는다. 소스 확인, 테스트 정의, 빌드/JVM/Lint, 계측, 수동 기기 조작, 결과 이미지 검수, 배포 검증을 서로 다른 증거로 취급한다.
- 재실행할 때 최종 커밋 또는 작업 트리 범위, variant, APK 식별값, 날짜·시간대, 명령, 실행/캐시/건너뜀/실패 수, 재현 조건과 남은 한계를 기록한다.
- 실제 사용자 사진·기기 serial·서명키·개인 경로·원시 로그는 이 문서에 넣지 않는다. 오류 주입과 삭제 검사는 격리된 QA fixture에 한정한다.

소스 기준: [앱 빌드 설정](../app/build.gradle.kts), [NavHost](../app/src/main/java/com/pocket4cut/presentation/navigation/PocketNavHost.kt), [Routes](../app/src/main/java/com/pocket4cut/presentation/navigation/Routes.kt), [프로젝트 지침](../AGENTS.md).

## 실제 화면 경로와 조작별 확인 항목

아래 항목은 **소스 검토로 확인한 경로에 대한 QA 체크리스트**이며 실제 조작 성공을 뜻하지 않는다. `Routes` 상수만 있는 이름과 실제 등록된 destination을 구분한다. 색상·계절·커스텀은 주로 `frameThemeSelect` 내부 단계이며, `occasionFramePick`은 별도 destination도 등록돼 있다.

### 1. 시작·홈·초안 재개

근거: [LaunchScreen](../app/src/main/java/com/pocket4cut/presentation/launch/LaunchScreen.kt), [HomeScreen](../app/src/main/java/com/pocket4cut/presentation/home/HomeScreen.kt), NavHost의 `launch → home`.

- [ ] 시작 화면 이후 홈 도착, 브랜드 `Pocket 4Cut` 표시, 뒤로가기 스택 확인.
- [ ] `카메라로 촬영`, `앨범에서 만들기`, `작업 보관함`, `설정 열기`의 목적 화면과 연타 동작 확인.
- [ ] 홈 전체 스크롤과 세로 큰 글자에서 주요 버튼 접근 확인.
- [ ] 재개 가능한 최근 초안의 입력 방식·컷 수 표시 및 해당 단계 재개.
- [ ] 초안 생성/수정 후 홈 재진입과 프로세스 재실행의 표시 갱신 확인.
- [ ] 복구 필요·읽기 불가·구형 이전 실패 안내와 보관함 진입 확인. 홈은 `NEEDS_RECOVERY`를 자동 재개 대상에서 제외하지만 보관함 진행 중 목록은 이를 포함하므로 두 숫자를 동일하게 해석하지 않는다.

### 2. 입력 방식·2/4/6컷 구성

근거: [FrameTypeSelectScreen](../app/src/main/java/com/pocket4cut/presentation/frameTypeSelect/FrameTypeSelectScreen.kt), `frameTypeSelect/{inputSource}`.

- [ ] 카메라와 앨범 진입에서 설명·다음 단계가 해당 입력 방식과 일치.
- [ ] 카메라 2/4/6컷은 각각 4/8/10장 촬영 후 2/4/6장 선택.
- [ ] 앨범 2/4/6컷은 완성에 필요한 2/4/6장 선택.
- [ ] 컷 구성 선택 상태, 스크롤, 닫기, 큰 글자와 48dp 조작 영역 확인.

### 3. 카메라·권한·일시정지

근거: [CaptureScreen](../app/src/main/java/com/pocket4cut/presentation/capture/CaptureScreen.kt), [CaptureViewModel](../app/src/main/java/com/pocket4cut/presentation/capture/CaptureViewModel.kt), `capture/{frameType}` 및 세션 ID가 포함된 재개 경로.

- [ ] 최초 권한 허용·거부, 재요청, 설정 열기, 설정에서 권한 변경 후 앱 복귀.
- [ ] 기본 전면 카메라와 전/후면 전환, 전환 연타·실패 후 원래 카메라 복귀.
- [ ] 촬영 시작, 카운트다운, 바로 촬영, 확대/축소, 완료 컷/전체 컷 표시.
- [ ] 설정 1–10초와 기본 3초 적용; 컷 처리 뒤 대기와 다음 컷 간격을 실제 시간으로 구분.
- [ ] 카운트다운·촬영 요청·파일 게시·컷 사이 대기 각각에서 홈/잠금/백그라운드 중단 후 명시적 재개.
- [ ] 늦은 콜백 중복 반영 금지, 원본 바이트 보존, 완료된 컷 유지, 재실행 후 연속된 미기록 게시 파일 한 번만 복구.
- [ ] 손상된 미기록 컷과 이미 기록된 원본 누락을 구분; 격리 확인·동일 슬롯 재촬영·정상 원본 보존.
- [ ] 실제 센서·카메라 연결·셔터 결과와 fixture JPEG 게시 검사를 별도 판정.

### 4. 앨범 가져오기·순서 변경

근거: [PhotoImportScreen](../app/src/main/java/com/pocket4cut/presentation/photoImport/PhotoImportScreen.kt), [PhotoImportViewModel](../app/src/main/java/com/pocket4cut/presentation/photoImport/PhotoImportViewModel.kt), `photoImport/{frameType}` 및 세션 재개 경로.

- [ ] 시스템 Photo Picker 자동 열기, 취소 후 `앨범 열기`, 필요한 장수만 추가, 전체 사진 읽기 권한 미요청.
- [ ] 정확한 장수에서만 `레이아웃 선택` 활성화, 부족·초과·중복 선택 안내.
- [ ] 가져온 사진 표시/번호, 드래그 및 접근성 `앞으로 이동/뒤로 이동`, 사진 제거와 외부 원본 보존.
- [ ] 긴 오류 안내 뒤에도 세로 큰 글자에서 목록·추가·닫기·다음 조작 접근.
- [ ] 일부 실패·중단·프로세스 재실행 후 완료 사본/순서 보존과 journal 한 번만 재생.
- [ ] JPEG/PNG/정적 WebP/기기 지원 HEIF 입력, EXIF, 큰 이미지·초광각 비율; 지원하지 않는 형식·빈/절단 픽셀 데이터·한도 초과 거부.
- [ ] 안내 메시지 해제가 차단 복구 오류를 없애지 않는지, 저장 실패 시 오래된 목록으로 계속하지 않는지 확인.

### 5. 촬영 사진 선택·레이아웃 선택

근거: [SelectionScreen](../app/src/main/java/com/pocket4cut/presentation/selection/SelectionScreen.kt), [SelectionViewModel](../app/src/main/java/com/pocket4cut/presentation/selection/SelectionViewModel.kt), [LayoutSelectionScreen](../app/src/main/java/com/pocket4cut/presentation/layoutSelection/LayoutSelectionScreen.kt), NavHost의 `selection`, `layoutSelection`.

- [ ] 선택 한도·선택 취소·배치 순서 숫자·완료 버튼과 저장된 PhotoId 순서가 일치.
- [ ] 초기 문서/사진 불러오기 실패 시 재시도·홈 이탈; 빈 UI 선택으로 기존 저장 순서를 덮어쓰지 않음.
- [ ] 선택 저장 실패 후 목록과 최신 선택 유지, 저장 재시도, 성공 시 오류 해제; 실패한 이탈은 화면에 남음.
- [ ] 저장되지 않은 선택이 있는 화면 재생성에서 dirty 선택 유지; 정상 재진입은 저장된 상태 재조회.
- [ ] 닫기와 시스템 뒤로가기의 동일한 보존 정책, 보관함에서 재개한 선택 화면의 홈 복귀.
- [ ] 2/4/6컷별 레이아웃 전환·실제 사진 미리보기·선택 확정 및 뒤로가기.
- [ ] 레이아웃 초기 읽기/선택 원본 누락/저장 실패는 오류·다시 시도·보관함 이동을 제공하고 성공 전에 다음 화면으로 가지 않음.
- [ ] 기존 6컷 배치 버전과 새 비대칭 6컷의 첫 사진 큰 슬롯 유지.

### 6. 색상·계절·OCCASION 88·커스텀 프레임

근거: [FrameFlowCoordinatorScreen](../app/src/main/java/com/pocket4cut/presentation/frameFlow/FrameFlowCoordinatorScreen.kt), [색상](../app/src/main/java/com/pocket4cut/presentation/frameFlow/ColorFramePalettePickScreen.kt), [계절](../app/src/main/java/com/pocket4cut/presentation/frameFlow/SeasonBackgroundFramePickScreen.kt), [Occasion](../app/src/main/java/com/pocket4cut/presentation/frameFlow/OccasionFramePickScreen.kt), [Custom](../app/src/main/java/com/pocket4cut/presentation/frameFlow/CustomFrameEditorScreen.kt).

- [ ] 네 프레임 방식 선택, 뒤로/홈/닫기와 초안의 현재 단계·적용된 값 복원.
- [ ] 모든 색상/그라데이션 이름·선택 상태·현재 사진 미리보기·저장 후 색 유지.
- [ ] 계절 4종 선택과 atlas 교체, 신구 6컷 배치·사진 슬롯·문구 영역을 보존한 미리보기.
- [ ] OCCASION 10개 카테고리·88개 테마의 접근, 각 카드 이름/썸네일/선택 상태와 해당 ID의 현재 사진 미리보기.
- [ ] 카테고리 탐색만으로 저장값 변경 금지; 명시 적용·적용 중 중복 이동 방지·같은 카드 재선택·뒤로 재진입.
- [ ] 11개 직접 기록 항목의 수동 입력 안내; 날씨·위치·D-Day·촬영 횟수를 자동 생성한 사실처럼 표시하지 않음.
- [ ] 프레임 로딩/atlas decode 실패, 잘못된 ID·버전, 메모리 부족에서 적용 제한·재시도 및 이전 값 보존.
- [ ] 실제 사진/문구 위로 프레임이 번지거나 화면 헤더를 덮지 않음; 미리보기와 최종 JPEG의 clip·사진 순서·장식·footer 일치.
- [ ] 커스텀 배경·스티커·이모지·텍스트 추가/선택/삭제, 이동·배율·회전 및 버튼 대체 조작.
- [ ] 커스텀 패널 스크롤, 글꼴/색상 시트, 키보드 표시 상태, 세로 큰 글자에서 `이 프레임으로 계속` 접근 및 저장 후 재개.

### 7. 일반 편집·상세 보정·사진별 자르기

근거: [EditScreen](../app/src/main/java/com/pocket4cut/presentation/edit/EditScreen.kt), [DetailEditScreen](../app/src/main/java/com/pocket4cut/presentation/detailEdit/DetailEditScreen.kt), [CropEditorScreen](../app/src/main/java/com/pocket4cut/presentation/detailEdit/CropEditorScreen.kt), `edit`, `detailEdit`. 자르기는 상세 편집 안의 화면이다.

- [ ] 필터 4종·프레임 색·문구 30자 제한·글자 크기·글꼴/글씨 색·날짜/날짜 크기 조작과 미리보기.
- [ ] 문구 입력 직후 닫기/시스템 뒤로가기에서 debounce 저장 완료, 중복 화면 이동 금지.
- [ ] 사진 순서 변경과 접근성 앞/뒤 이동, PhotoId별 보정/crop 귀속 유지.
- [ ] 상세 사진 선택·회전·좌우 반전·밝기·대비·채도·초기화, 빠른 변경 후 최신값만 표시.
- [ ] 자르기 미리보기·드래그·두 손가락 확대·±·확대 슬라이더·4방향 버튼·가운데 맞춤·완료·닫기/뒤로가기.
- [ ] 세로 큰 글자에서 충분한 사진 영역, 저해상도 안내·48dp 조작·완료 버튼 접근. 가로 배치는 이번 판정에서 제외.
- [ ] 확대 1–4배, 슬롯 바깥 빈 영역 없음, 회전/반전 후 초점 방향 일치, 원본 비파괴·저장 후 재개.
- [ ] 적용 진행 상태·중복 적용 제한·렌더 실패 안내와 원본/이전 결과 보존.

### 8. 최종 이미지·결과 화면·사진첩·공유

근거: [ResultScreen](../app/src/main/java/com/pocket4cut/presentation/result/ResultScreen.kt), [GalleryExporter](../app/src/main/java/com/pocket4cut/data/export/GalleryExporter.kt), [CollageRenderer](../app/src/main/java/com/pocket4cut/frame/CollageRenderer.kt), `result/{resultPath}?auto={auto}`.

- [ ] 원본 기반 별도 렌더, 출력 크기/배치 버전/사진 순서/필터/보정/crop/문구·날짜/장식 일치; UI 스크린샷을 최종 결과로 쓰지 않음.
- [ ] 결과 준비 중·저장 중·공유 중·저장 완료의 버튼 상태, 중복 탭 제한.
- [ ] 사진첩 수동/자동 저장, 같은 결과 재열기·앱 복귀 시 중복 사본 없음.
- [ ] 사진첩 사본 삭제·변경·잘못된 URI·pending 행·중단 journal의 복구 계약 및 기존 공용 사본 덮어쓰기 금지.
- [ ] Android 공유 시트 열기·취소·외부 수신 앱 읽기, FileProvider가 완성 결과 외 원본/세션을 노출하지 않음.
- [ ] 정상 결과가 무관한 손상 세션 때문에 막히지 않음, 결과 연결 실패·내보내기 실패 메시지와 재시도.
- [ ] 홈 복귀, 세로 큰 글자, 최종 이미지 종횡비와 실제 JPEG 열람.
- [ ] API 26/28 저장 권한 분기는 현재 API 36 한 대의 실기기 성공으로 판정하지 않음.

### 9. 보관함·손상 복구·삭제 소유권

근거: [GalleryScreen](../app/src/main/java/com/pocket4cut/presentation/gallery/GalleryScreen.kt), [GalleryViewModel](../app/src/main/java/com/pocket4cut/presentation/gallery/GalleryViewModel.kt), [SessionDocumentRepository](../app/src/main/java/com/pocket4cut/data/local/SessionDocumentRepository.kt), `gallery`.

- [ ] 완료/진행 중 탭, 날짜/컷별 보기, 빈 상태, 항목 열기·진행 중 작업 재개, 읽기 오류 다시 불러오기.
- [ ] 항목 길게 누르기/삭제, 작업 버리기 확인·취소, 세션/촬영 원본/앨범 앱 사본의 삭제 범위.
- [ ] 외부 앨범 원본·공용 사진첩 사본·다른 세션이 참조하는 파일 보존.
- [ ] 손상 문서와 미완료 결과 후보 구분, 정상 완료본 계속 표시, 이전 정상본 복구와 결과 후보 격리를 구분.
- [ ] 읽기 불가 기록 숨기기·숨긴 기록 관리·다시 표시, 소유권을 확정할 수 없는 상태에서 물리 삭제 차단.
- [ ] 구형 자료 이전 실패 안내, 새 정상 세션 병행 표시, tombstone 후 중단 삭제 재개와 삭제 세션 재수입 방지.
- [ ] 실제 사용자 데이터 손상/삭제 없이 격리 fixture로 오류·중단 검증.

### 10. 설정·문의·공통 접근성

근거: [SettingsScreen](../app/src/main/java/com/pocket4cut/presentation/settings/SettingsScreen.kt), [ContactFeedbackScreen](../app/src/main/java/com/pocket4cut/presentation/settings/ContactFeedbackScreen.kt), [공통 조작](../app/src/main/java/com/pocket4cut/ui/designsystem/components/Buttons.kt), `settings`, `contactFeedback`.

- [ ] 계절 앱 테마, 전면 카메라 기본, 카운트다운, 자동 사진첩 저장, 날짜 기본 표시의 값과 재진입 유지.
- [ ] 보관함 전체 삭제 확인 범위·취소·오류 처리; 실제 사용자 자료에는 수행하지 않음.
- [ ] 버전 표시, 설정 닫기 연타 후 홈 유지.
- [ ] 문의 화면 진입·뒤로가기·주소 복사·외부 메일 작성 화면 연결. 실제 메일 발송은 하지 않음.
- [ ] 앱 내부 개인정보 화면의 진입/닫기 경로만 목록화. 공개 페이지 내용·배포 검증은 제외.
- [ ] TalkBack 레이블·선택 상태·순서·오류 안내, 슬라이더 접근성/키보드 조작, 장식/사진 순서의 제스처 대체 조작.
- [ ] 세로 작은 화면/큰 글자/키보드, 버튼·다이얼로그·시트의 잘림, 48dp·대비·시스템바 가독성.
- [ ] 화면 자동 꺼짐 방지 설정은 수정·이번 QA 판정 범위에서 제외하고 기존 상태 유지.

## 자동 검사 정의 전체 목록

작성 시 `app/src/test`와 `app/src/androidTest`의 `*Test.kt`를 검색하고 `@Test` 메서드를 대조했다. 최종 대조에는 작성 중 추가된 커스텀 세로 조작, occasion 최종 JPEG, crop 제스처, Selection 완료 저장 중 입력 차단·실패 재시도와 카메라·앨범 날짜 기본값 검사도 포함했다. **JVM 6개 파일·25개 메서드, Android 계측 38개 파일·176개 메서드, 합계 44개 파일·201개 정의**다. 아래 모든 메서드는 이름 그대로 나열했으며 각 파일의 annotation 수와 추출 수가 일치했다. 파일/메서드 추가·삭제 후 이 목록은 갱신해야 한다.

- 이는 실행/통과 수가 아니다. 반복 실행 횟수·테스트 내부 조합 수·실제 UI E2E 수와도 다르다.
- 가로 전용 계측 2개는 소스에 존재하지만 이번 요청의 실행·판정 범위에서 제외한다. 이를 뺀 정의는 계측 174개와 JVM 25개, 합계 199개이며 아래 조건부 검사도 포함한 수다. 실행 수나 통과 수가 아니다.
- 대형 메모리, 계절 정적 산출물, 88종 최종 JPEG 검사는 각각 opt-in 조건이 있다. 기본 실행의 건너뜀을 통과로 기록하지 않는다.
- `GalleryExporterInstrumentedTest` 6개는 API 29 이상을 전제로 한다. API 36 결과는 API 26/28 분기를 증명하지 않는다.
- Compose 의미/크기 검사는 TalkBack 실제 음성 탐색 완주와 다르고, 작은 합성 Bitmap·가상 실패는 실제 카메라/고해상도 메모리/저장 공간 부족·OS 프로세스 종료 전부를 대신하지 않는다.

### 입력·사진 선택·초안 복구

#### [CaptureDateDefaultInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CaptureDateDefaultInstrumentedTest.kt) — 3개

Android 계측 정의. UUID별 설정·파일 저장소에서 실제 CaptureViewModel의 새 작업 생성과 즉시 일시정지를 사용해 날짜 기본값 ON/OFF를 저장하는지 확인한다. 기존 카메라 초안 재개는 현재 기본값으로 날짜 선택·문자열·revision을 덮어쓰지 않아야 한다. 실제 카메라 요청과 전역 AppSettings 변경은 하지 않는다.

- [ ] `newCameraDraftUsesEnabledDateDefaultBeforeFirstCapture`
- [ ] `newCameraDraftUsesDisabledDateDefaultBeforeFirstCapture`
- [ ] `restoredCameraDraftKeepsItsDateChoiceAfterDefaultChanges`

#### [AlbumDateDefaultInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/AlbumDateDefaultInstrumentedTest.kt) — 3개

Android 계측 정의. 격리 설정과 합성 JPEG를 실제 PhotoImportViewModel로 가져와 새 앨범 초안의 날짜 기본값 ON/OFF와 추가 가져오기 시 기존 선택 보존을 확인한다. 최초 사진 게시 뒤 세션 쓰기 실패를 주입해 저널에 기록한 날짜 기본값의 복구와 필드가 없는 구형 저널의 OFF 호환도 검사한다. 실제 Settings UI·Photo Picker·프로세스 종료 검사는 아니다.

- [ ] `newAlbumDraftUsesSavedDateDefaultAndFurtherImportsKeepItsChoice`
- [ ] `firstPhotoPublicationRecoveryKeepsRecordedDateDefault`
- [ ] `legacyImportJournalWithoutDateDefaultKeepsPreviousOffBehavior`

#### [PhotoSelectionPolicyTest](../app/src/test/java/com/pocket4cut/presentation/photoImport/PhotoSelectionPolicyTest.kt) — 4개

JVM 정의. Photo Picker 초과 선택·중복·부족 장수 정책.

- [ ] `rejectsWholeCallbackWhenFallbackReturnsMoreThanConfiguredMaximum`
- [ ] `removesDuplicatesWithoutSpendingAnotherSlot`
- [ ] `acceptsFewerPhotosSoUserCanAddTheRestLater`
- [ ] `leavesExistingDraftDuplicateDecisionToRepository`

#### [PhotoImportUiStateTest](../app/src/test/java/com/pocket4cut/presentation/photoImport/PhotoImportUiStateTest.kt) — 3개

JVM 정의. 안내 메시지와 차단 오류, 오래된 가져오기 상태의 다음 단계 제한.

- [ ] `dismissibleErrorNoticeDoesNotBlockACompleteSelection`
- [ ] `dismissingPresentationStateDoesNotClearARecoveryBlock`
- [ ] `persistedEditFailureBlocksContinuingWithStalePhotoState`

#### [PhotoImportRepositoryInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/PhotoImportRepositoryInstrumentedTest.kt) — 24개

Android 계측 정의. 앨범 바이트 보존·형식 검증·journal 재생·PhotoId crop·이전·삭제 소유권.

- [ ] `schemaV1IsStrictlyUpgradedAndWrittenAsCurrentSchemaOnNextUpdate`
- [ ] `invalidV1CountsBecomeRecoveryInsteadOfGuessingFourCut`
- [ ] `schemaV1FourAndSixCutPairsMigrateWithoutChangingOrder`
- [ ] `futureSchemaIsRejectedWithoutRewritingItsDocument`
- [ ] `invalidCropIsRejectedAsCorruptSession`
- [ ] `importPreservesExternalBytesReplaysWithoutDuplicatesAndRemovesOnlyCopy`
- [ ] `damagedLegacyIndexDoesNotBlockFirstAlbumImportOrResume`
- [ ] `damagedLegacyPendingDoesNotBlockFirstAlbumImportOrResume`
- [ ] `currentAlbumDocumentCorruptionIsNotHiddenByLegacyIndependentLookup`
- [ ] `cropRemainsAttachedToPhotoIdAfterReorderAndRepositoryRecreation`
- [ ] `preparedJournalWithAlreadyRenamedFileRecoversExactlyOnce`
- [ ] `filePublishedJournalWithoutSessionCommitRecoversExactlyOnce`
- [ ] `removalIntentAfterReferenceRemovalDeletesOnlyItsOwnedImportCopy`
- [ ] `mismatchedRemovalJournalCannotDeleteAnotherSessionsImport`
- [ ] `failedRemovalReplayDoesNotResurrectPhotoFromCommittedImportJournal`
- [ ] `duplicateAndFallbackOverSelectionNeverSilentlyTruncate`
- [ ] `overSelectionRetainsRecoveryFailureWithoutChangingImportedPhotos`
- [ ] `unsupportedImageDoesNotCreateSessionOrLeavePendingCopy`
- [ ] `animatedWebpAvifAndBmpSignaturesAreExplicitlyRejected`
- [ ] `pngAndStaticWebpAreImportedWithoutReencoding`
- [ ] `jpegWithMotionPhotoPayloadAfterEoiIsImportedByteForByte`
- [ ] `emptyAndCorruptImagesAreRejectedWithoutDraft`
- [ ] `boundsReadableButTruncatedPixelPayloadIsRejected`
- [ ] `deletingAlbumSessionPreservesExternalSourcesAndOtherSessionCopies`

#### [PhotoImportAccessibilityInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/PhotoImportAccessibilityInstrumentedTest.kt) — 6개

Android 계측 정의. 앨범 계속 조건·안내 해제·접근성 순서 변경·세로 큰 글자 조작.

- [ ] `exactCountControlsContinuation`
- [ ] `recoveryErrorBlocksContinuationEvenAtExactCount`
- [ ] `dismissibleErrorNoticeDoesNotBlockExactCount`
- [ ] `photoRowsOfferMoveAndRemoveActionsWithLargeTouchTarget`
- [ ] `cancelledPickerCanBeReopenedInCompactPortraitWithDoubleFontScale`
- [ ] `photoActionsRemainReachableBelowLongNoticeInCompactPortraitWithDoubleFontScale`

#### [SelectionRecoveryInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/SelectionRecoveryInstrumentedTest.kt) — 6개

Android 계측 정의. 촬영 사진 선택의 초기 불러오기 실패·저장 실패·dirty 선택 보존, 완료 저장 중 늦은 선택·중복 완료·재로딩 차단과 완료 저장 실패 후 편집·재시도.

- [ ] `missingPhotoLoadLeavesWithoutErasingStoredSelection`
- [ ] `loadRetryRestoresStoredOrderAfterSourceBecomesAvailable`
- [ ] `saveFailureKeepsPhotosAndDirtyOrderUntilRetrySucceeds`
- [ ] `repeatedLoadDoesNotDiscardUnsavedSelectionAndLaterSaveClearsError`
- [ ] `completingSelectionBlocksLateToggleDuplicateCompletionAndReload`
- [ ] `completionWriteFailureUnlocksSelectionAndAllowsEditedRetry`

#### [LayoutSelectionRouteInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/presentation/navigation/LayoutSelectionRouteInstrumentedTest.kt) — 3개

Android 계측 정의. 레이아웃 진입 읽기·원본 누락·저장 실패의 재시도와 원본 보존.

- [ ] `corruptDocumentShowsRetryAndReloadsWithoutChangingOriginals`
- [ ] `saveFailureBlocksNavigationAndCanBeRetriedWithoutChangingOriginals`
- [ ] `missingSelectedPhotoShowsErrorWithoutRewritingSessionOrOtherPhoto`

#### [DraftExitPersistenceInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/DraftExitPersistenceInstrumentedTest.kt) — 1개

Android 계측 정의. 문구 debounce 직후 뒤로가기의 저장 완료와 단일 이동.

- [ ] `immediateBackFlushesDebouncedCaptionAndNavigatesOnce`

### 카메라 파일 게시·중단 복구

#### [CaptureDamageRecoveryInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CaptureDamageRecoveryInstrumentedTest.kt) — 1개

Android 계측 정의. 손상된 미기록 촬영 파일의 보존·격리 후 같은 슬롯 재촬영.

- [ ] `damagedUnrecordedCaptureIsPreservedWhileItsSlotCanBeRetaken`

#### [CapturePublicationInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CapturePublicationInstrumentedTest.kt) — 1개

Android 계측 정의. 잘못된 JPEG의 촬영 슬롯 게시 거부.

- [ ] `invalidJpegNeverOccupiesCaptureSlotAndNextAttemptCanPublish`

#### [CaptureRecoveryOrderInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CaptureRecoveryOrderInstrumentedTest.kt) — 3개

Android 계측 정의. 기록된 원본 누락·비연속 게시 파일·연속 게시 재생의 순서 보호.

- [ ] `missingRecordedSourceStopsRecoveryWithoutReplacingOtherPhotos`
- [ ] `outOfOrderUnrecordedFileIsPreservedAndNeverAppended`
- [ ] `contiguousPublishedFileAfterCrashIsAppendedOnlyOnce`

### 일반 편집·자르기·렌더·Bitmap 수명

#### [CustomFrameAccessibilityInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CustomFrameAccessibilityInstrumentedTest.kt) — 4개

Android 계측 정의. 360×640dp 세로 화면의 기본/2배 글자에서 이모지·스티커를 실제로 추가하고 열린 tray 아래의 계속 버튼까지 스크롤해 조작한다.

- [ ] `emojiTrayCanAddAndContinueInPortrait`
- [ ] `emojiTrayCanAddAndContinueInPortraitWithDoubleFont`
- [ ] `stickerTrayCanAddAndContinueInPortrait`
- [ ] `stickerTrayCanAddAndContinueInPortraitWithDoubleFont`

#### [CropEditorGestureInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CropEditorGestureInstrumentedTest.kt) — 2개

Android 계측 정의. 합성 Bitmap과 360×640dp 세로·기본 글자 화면에서 실제 pointer pinch/drag, 확대 slider, 네 방향 버튼, 가운데 맞춤·완료를 조작한다. preview/commit 콜백의 확대 범위·유효 crop·빈 가장자리 방지와 dismiss 전달을 확인하는 검사이며, 세션 영속화·전체 내비게이션·실제 원본 내보내기 검사는 아니다.

- [ ] `pinchAndDragCommitFiniteClampedCropsWithoutExposingBlankEdges`
- [ ] `labelledSliderDirectionButtonsCenterAndDoneCommitThroughTheUi`

#### [CropMathTest](../app/src/test/java/com/pocket4cut/frame/CropMathTest.kt) — 8개

JVM 정의. crop 좌표·회전·반전·clamp·중립 crop 수학 계약.

- [ ] `mapsStoredFocusForEveryQuarterTurnAndHorizontalFlip`
- [ ] `displayAndStoredFocusConversionsRoundTripForAllTransforms`
- [ ] `neutralCropIsExactlyTheLegacyCenteredAspectFill`
- [ ] `translatedViewportWithFloatRoundTripGapStillProducesADrawRect`
- [ ] `everySupportedZoomAndTransformCoversViewportWithoutBlankEdges`
- [ ] `drawRectUsesRotatedThenMirroredFocusOnTransformedBitmap`
- [ ] `clampMovesOnlyFocusAndKeepsZoom`
- [ ] `rejectsInvalidCropAndDimensionsInsteadOfSilentlyChangingThem`

#### [BitmapOwnershipInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/BitmapOwnershipInstrumentedTest.kt) — 3개

Android 계측 정의. UI가 보유한 Bitmap 수명과 오래된 필터 작업 게시 억제.

- [ ] `detailPreviewRemainsDrawableAfterReplacementResetAndViewModelClear`
- [ ] `editFilterSwitchKeepsRetainedPreviewDrawableAndPublishesLatestFilter`
- [ ] `editReinitRejectsStaleFilterAndThumbnailPublishes`

#### [BitmapPipelineInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/BitmapPipelineInstrumentedTest.kt) — 7개

Android 계측 정의. 필터/보정 순서·EXIF·region decode·큰 입력 제한·최신 작업·atlas 수명.

- [ ] `sharedPhotoPipelineAppliesFilterBeforePerPhotoColorAdjustments`
- [ ] `allExifOrientationsPreserveTheExpectedPixelPositions`
- [ ] `regionDecoderMapsTopLeftCropAcrossAllExifOrientations`
- [ ] `regionDecoderSupportsPngAndStaticWebpAndRejectsUnknownBytes`
- [ ] `decoderBoundsLargeAndPanoramicSourcesBeforeAllocating`
- [ ] `immediateAndRapidDetailChangesPublishTheLatestAdjustmentForEverySlot`
- [ ] `retainedSeasonalSheetsSurviveCacheEvictionAndParallelPaintsMatchSerialPaints`

#### [CropRendererInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CropRendererInstrumentedTest.kt) — 4개

Android 계측 정의. 슬롯 순서·빈 픽셀 방지·EXIF/사용자 보정 crop·원본 영역 decode.

- [ ] `explicitNeutralCropIsPixelIdenticalToLegacyDefaultForEveryLayout`
- [ ] `cropDataIsAppliedBySlotOrderAndNeverExposesBlankPixels`
- [ ] `regionDecodedExportMatchesFullSourceCropAfterExifRotationAndUserTransform`
- [ ] `allExifOrientationsQuarterTurnsAndMirrorsRetainTheSameVisibleCrop`

#### [CollageOutputContractInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CollageOutputContractInstrumentedTest.kt) — 3개

Android 계측 정의. 기기 독립 출력 상한·6컷 신구 레이아웃·사진 밖 footer.

- [ ] `everyLayoutUsesDeviceIndependentBoundedOutput`
- [ ] `newSixCutFirstPhotoOwnsHeroSlotAndOldLayoutStaysGrid`
- [ ] `sixCutFooterAndCaptionStayOutsideThePhotoSlots`

#### [RenderContractDiagnosticTest](../app/src/androidTest/java/com/pocket4cut/RenderContractDiagnosticTest.kt) — 7개

Android 계측 정의. 사진 순서·필터·문구/날짜·색상·스티커·미리보기 수명 진단.

- [ ] `allEightLayoutsAndFourFiltersRenderEveryNumberedPhotoInInputOrder`
- [ ] `rendererHonorsDeliberatelyReversedPhotoInputOrder`
- [ ] `captionAndDateReserveTheSameAreaInPreviewAndExportForAllLayouts`
- [ ] `everySelectableFramePaletteIdRestoresTheChosenRgb`
- [ ] `freelyArrangedSixCollageIsGeometricallyDifferentFromSixHorizontalGrid`
- [ ] `exportedHeartStickerIsNotIdenticalToPlainTextOfItsDisplayName`
- [ ] `replacingDetailPreviewDoesNotRecycleBitmapStillRetainedByUiConsumer`

#### [RenderReleaseContractInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/RenderReleaseContractInstrumentedTest.kt) — 4개

Android 계측 정의. 모든 배치/필터/문구 투영·색상/스티커·커스텀 텍스트·옵션형 메모리 검사.

- [ ] `allLayoutsFiltersAndCaptionConditionsProjectTheSameScene`
- [ ] `everyFrameColorAndStickerPaintsItsSelectedAsset`
- [ ] `customTextWrapsWithinTwoLineLogicalWidth`
- [ ] `measuredMaxOutputRetainsOneSourceBitmapAtATime` — **조건부 정의: renderMemoryStress=true**

#### [SeasonalFrameContractInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/SeasonalFrameContractInstrumentedTest.kt) — 4개

Android 계측 정의. 4계절 safe area·preview/export 좌표·자산 제한·옵션형 정적 산출물.

- [ ] `seasonalArtPreservesEveryPhotoAndReservedTextAreaAcrossAllLayouts`
- [ ] `previewAndFinalCoordinateSystemsPaintTheSameSeasonalComposition`
- [ ] `seasonalAssetsAreBoundedAndExistingSavedSeasonIdentifiersStillRoundTrip`
- [ ] `exportDownloadableFramesAndFictionalPhotoCompositesWhenExplicitlyRequested` — **조건부 정의: seasonalExport=true**

#### [SeasonalFramePreviewInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/SeasonalFramePreviewInstrumentedTest.kt) — 2개

Android 계측 정의. 계절 선택 UI의 atlas 교체·실제 그리기·6컷 버전.

- [ ] `pickerLoadsAndSwitchesAllFourAtlasesAndCapturesActualAutumnScreen`
- [ ] `pickerActuallyDrawsLegacyAndModernSixCutLayoutsWithoutLosingTheVersion`

#### [SeasonalLegacyPreviewInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/SeasonalLegacyPreviewInstrumentedTest.kt) — 1개

Android 계측 정의. 구형 계절 6컷 초안의 편집/내보내기 배치 유지.

- [ ] `restoringSeasonalSixCutDraftKeepsLegacyGeometryInGeneralEditorAndExport`

### OCCASION 88 카탈로그·선택·렌더

#### [OccasionFinalResultInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/OccasionFinalResultInstrumentedTest.kt) — 1개

Android 계측 정의. 실제 상세 편집의 원본 decode·고해상도 렌더·결과 게시·재열기 경로로 테마마다 JPEG 하나를 생성한다. 88개 테마에 8개 배치를 순환 배정하며 88×8 전체 조합, Compose 내비게이션, Photo Picker, MediaStore 저장 검사는 아니다.

- [ ] `everyOccasionPublishesAndReopensAnOriginalBasedHighResolutionJpeg` — **조건부 정의: occasionFinalExports=true**

#### [OccasionSelectionContractTest](../app/src/test/java/com/pocket4cut/frame/occasion/OccasionSelectionContractTest.kt) — 6개

JVM 정의. occasion ID/버전/기본 theme 및 다른 배경 payload와의 계약.

- [ ] `legacyAndBasicFramesHaveNoOccasionSelection`
- [ ] `completeKnownSelectionIsValid`
- [ ] `applyingOccasionPersistsTheSelectedBaseFrameTheme`
- [ ] `renderThemeCompatibilityOnlyDefaultsBlankCurrentOccasionDrafts`
- [ ] `occasionSelectionRejectsSeasonAndCustomPayloads`
- [ ] `partialUnknownAndFutureSelectionsNeedRecovery`

#### [OccasionCatalogInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/OccasionCatalogInstrumentedTest.kt) — 13개

Android 계측 정의. 88종 자산·schema v2/v3·ID 재개·잘못된/미래 계약·재시도 가능한 카탈로그 오류.

- [ ] `bundledCatalogContainsValidatedAssetsForAllEightyEightThemes`
- [ ] `schemaV2MigratesWithoutInventingAnOccasionSelection`
- [ ] `schemaV3RequiresBothOccasionFieldsEvenWhenTheyAreNull`
- [ ] `malformedOrUnsafeCatalogContractsAreRejected`
- [ ] `validOccasionSelectionPersistsAsSchemaV3`
- [ ] `everyBundledOccasionIdPersistsAndReopensWithoutSubstitution`
- [ ] `unknownThemeAndVersionAreReturnedAsRecoveryWithoutRewritingSource`
- [ ] `catalogLoadFailureIsRetryableAndDoesNotRewriteAValidSessionAsRecovery`
- [ ] `corruptPrimaryWithValidOccasionLastGoodKeepsCatalogFailureRetryable`
- [ ] `persistedOccasionWithSeasonOrCustomPayloadNeedsRecoveryWithoutRewrite`
- [ ] `newInconsistentOccasionSelectionIsRejected`
- [ ] `updateRejectsOccasionWithSeasonOrCustomPayloadAndPreservesCurrentRevision`
- [ ] `framePickerStepCanChangeWithoutDiscardingTheLastAppliedSelection`

#### [OccasionFramePickerInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/OccasionFramePickerInstrumentedTest.kt) — 11개

Android 계측 정의. 10개 카테고리·88개 카드·현재 미리보기·명시 적용·저장상태 복원.

- [ ] `frameModeExposesOccasion88AsFourthAccessibleChoice`
- [ ] `pickerFiltersTenCategoriesAndKeepsAllEightyEightThemesReachable`
- [ ] `manualRecordThemeIsExplicitAndOnlyAppliesAfterCta`
- [ ] `selectedThemeUpdatesTheRealCurrentLayoutPreview`
- [ ] `everyThemeCardLoadsWithoutCoveringHeaderAndAppliesItsOwnId`
- [ ] `retappingReadySelectedThemeKeepsApplyEnabled`
- [ ] `dedicatedPickerExplorationDoesNotWriteAndApplyCommitsAtomically`
- [ ] `applyRemainsReachableOnLandscapeWithDoubleFontScale` — **가로 전용·이번 범위 제외**
- [ ] `applyStaysDisabledWhenSelectedArtworkCannotBeDecoded`
- [ ] `categoryAndThemeSelectionSurviveSavedStateRestoration`
- [ ] `recoveryErrorActionsRemainReachableInCompactLandscapeWithDoubleFontScale` — **가로 전용·이번 범위 제외**

#### [OccasionFrameRenderInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/OccasionFrameRenderInstrumentedTest.kt) — 13개

Android 계측 정의. Canvas clip·브랜드/기록문구·footer·preview 작업 취소/메모리·전체 렌더 조합.

- [ ] `translatedPreviewNeverPaintsOutsideSceneOrChangesCallerClip`
- [ ] `brandAndManualRecordTitlesNeverRenderExampleValues`
- [ ] `captionBandIsReservedForUserTextAcrossFooterThemesAndLayouts`
- [ ] `captionFreeFramesRenderFooterMotifsOutsideEveryPhotoSlot`
- [ ] `previewAtlasOutOfMemoryBecomesRetryableFailureAndEvictsTheLru`
- [ ] `concurrentRequestsForOneAtlasShareOnePublishedSheet`
- [ ] `aToBToAKeepsTheLatestAWaiterWhenTheOriginalOwnerIsObsolete`
- [ ] `rapidPreviewChangesCancelBeforeDecodeAndNeverPublishAStaleResult`
- [ ] `obsoleteDecodedBitmapIsRecycledBeforeCachePublication`
- [ ] `eightyEightRapidDifferentThemeDecodesStayWithinConcurrencyAndHeapLimits`
- [ ] `everyThemeAndLayoutVariantRendersAnOpaqueNonEmptyProtectedScene`
- [ ] `previewAndScaledExportUseTheSameCompositionForEveryArtProfile`
- [ ] `sequentialAtlasLoadingStaysBoundedAndRejectsAMismatchedTheme`

### 결과·사진첩·공유·세션 저장

#### [ResultActionStateTest](../app/src/test/java/com/pocket4cut/presentation/result/ResultActionStateTest.kt) — 3개

JVM 정의. 결과 연결/진행 상태별 저장·공유 활성화.

- [ ] `loadingAndUnavailableLinksDisableBothActions`
- [ ] `readyLinkAllowsOnlyIdleOperations`
- [ ] `verifiedExistingCopyDisablesSaveButKeepsShareAvailable`

#### [ResultActionControlsInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/ResultActionControlsInstrumentedTest.kt) — 4개

Android 계측 정의. 준비 중·저장 중·공유 중 실제 결과 조작 상태.

- [ ] `loadingLinkShowsPreparationAndDisablesShare`
- [ ] `readyIdleLinkEnablesSaveAndShare`
- [ ] `activeSaveDisablesShare`
- [ ] `activeShareDisablesSaveAndAnotherShare`

#### [ResultLinkInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/ResultLinkInstrumentedTest.kt) — 1개

Android 계측 정의. 무관한 손상 세션과 정상 결과 연결 분리.

- [ ] `unrelatedCorruptSessionDoesNotBlockHealthyResultLink`

#### [ResultPublicationJournalInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/ResultPublicationJournalInstrumentedTest.kt) — 4개

Android 계측 정의. 결과 게시/격리 중단 재생과 이전 정상 결과 보호.

- [ ] `damagedNewJpegRequiresRecoveryWithoutHidingExistingResult`
- [ ] `interruptedQuarantineResumesAndEmptyPublicationDoesNotBlockDraft`
- [ ] `replayRejectsValidJpegWithWrongRecordedDimensions`
- [ ] `publishedFileReplaysOnceAfterRestartAndPreservesPreviousResult`

#### [GalleryExporterInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/GalleryExporterInstrumentedTest.kt) — 6개

Android 계측 정의. MediaStore 작업 재생·행 검증·중복/덮어쓰기 방지(API 29 이상 조건).

- [ ] `deletedCompletedMediaRowCanBeRecreatedOnceWithSameOperation`
- [ ] `changedCompletedMediaRowIsNotDuplicatedOrOverwritten`
- [ ] `preparedExportWithoutMediaRowResumesAfterRestartOnlyOnce`
- [ ] `preparedExportReusesItsPendingMediaRowAfterRestart`
- [ ] `recoveryWithoutRecordedUriCompletesItsPartialPendingRow`
- [ ] `completedOperationRejectsUnrelatedOrPendingMediaRow`

#### [FileProviderExposureInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/FileProviderExposureInstrumentedTest.kt) — 2개

Android 계측 정의. 완성 결과만 공유하고 별도 수신 패키지가 읽을 수 있는 권한.

- [ ] `onlyCompletedResultsAreShareable`
- [ ] `grantedCompletedResultCanBeReadByIndependentReceiverPackage`

#### [ResultShareIntentInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/ResultShareIntentInstrumentedTest.kt) — 1개

Android 계측 정의. EXTRA_STREAM·ClipData·읽기 권한 전달.

- [ ] `imageUriIsGrantedThroughExtraStreamAndClipData`

#### [SessionDocumentRepositoryInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/SessionDocumentRepositoryInstrumentedTest.kt) — 12개

Android 계측 정의. revision·손상 복구·이전·보관함 부분 조회·삭제 소유권·tombstone·export 기록.

- [ ] `revisionRejectsStaleWritesAndOriginalIsUnchanged`
- [ ] `truncatedCaptureIsNotPublishedInSession`
- [ ] `corruptPrimaryNeedsRecoveryAndRestoresLastGoodRevision`
- [ ] `galleryScanExposesUnreadableSessionWithoutHidingHealthySessions`
- [ ] `corruptLegacyIndexDoesNotHideHealthyResultOrPermitUnsafeDeletion`
- [ ] `deletingSessionClearsOnlyKnownPendingResultFile`
- [ ] `deletingSessionRemovesOnlyItsRecoveryArtifacts`
- [ ] `migrationTreatsLegacyImagePathsAsAlreadySelectedAndDoesNotRepeat`
- [ ] `deletedLegacySessionDoesNotReappearAndOnlyOwnedFileIsRemoved`
- [ ] `deletionResumesFromDurableTombstoneAfterProcessRestart`
- [ ] `deletionPreservesFileReferencedByAnotherSessionAcrossPathFormats`
- [ ] `exportCopyFlagSurvivesDocumentReload`

### 설정·내비게이션·접근성·기본 환경

#### [CountdownSettingInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/CountdownSettingInstrumentedTest.kt) — 1개

Android 계측 정의. 카운트다운 설정 저장과 화면 재진입 표시.

- [ ] `countdownValuePersistsAndReappearsAfterLeavingSettings`

#### [NavigationGuardInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/presentation/navigation/NavigationGuardInstrumentedTest.kt) — 2개

Android 계측 정의. 설정 닫기 연타·보관함에서 재개한 선택 화면의 홈 복귀.

- [ ] `repeatedSettingsCloseLeavesHomeOnTheBackStack`
- [ ] `galleryResumedSelectionReturnsHomeAndRepeatedLeaveCannotPopIt`

#### [MainFlowAccessibilityInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/MainFlowAccessibilityInstrumentedTest.kt) — 6개

Android 계측 정의. 48dp/의미·선택 순서·날짜·슬라이더·순서/장식 대체 조작.

- [ ] `iconCircleButtonExposesMinimumTouchTargetLabelRoleAndEnabledState`
- [ ] `selectedPhotosAnnounceOrderAndKeepDeselectionAvailableAtLimit`
- [ ] `dateSwitchAnnouncesStateAndRevealsAdjustableDateSize`
- [ ] `customSlidersSupportScreenReaderRangeAndKeyboardWithoutDragging`
- [ ] `photoOrderOffersExplicitMoveActionsAndWholePhotoSwapTarget`
- [ ] `decorationCanMoveScaleAndRotateWithSingleTapControls`

#### [ReleaseAccessibilityInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/ReleaseAccessibilityInstrumentedTest.kt) — 4개

Android 계측 정의. 카메라·설정·알림·팔레트·프레임·보관함 선택 의미.

- [ ] `captureControlsExposeNamedButtonStateAndMinimumTouchTargets`
- [ ] `settingsToastAndPaletteExposeSinglePurposeSemantics`
- [ ] `frameLayoutSeasonAndColorChoicesExposeRadioSelection`
- [ ] `frameModesGalleryTabsAndEditFiltersExposeCorrectRoles`

#### [BackupRulesInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/BackupRulesInstrumentedTest.kt) — 2개

Android 계측 정의. 설정 두 preference 파일만 포함하는 백업/기기 이전 규칙.

- [ ] `legacyBackupUsesOnlyTheTwoSettingsPreferences`
- [ ] `cloudBackupAndDeviceTransferEachUseOnlyTheTwoSettingsPreferences`

#### [ExampleInstrumentedTest](../app/src/androidTest/java/com/pocket4cut/ExampleInstrumentedTest.kt) — 1개

Android 계측 정의. 대상 앱 패키지 기본 확인.

- [ ] `useAppContext`

#### [ExampleUnitTest](../app/src/test/java/com/pocket4cut/ExampleUnitTest.kt) — 1개

JVM 정의. 기본 산술 검사; 기능 QA 근거로 사용하지 않음.

- [ ] `addition_isCorrect`

## 실행 결과 연결

이 목록의 체크박스는 기능·테스트 정의를 보존하며 일괄 PASS 표시가 아니다. 아래 [최종 검증 보고서](FINAL_RELEASE_QA_2026-09-27.md)가 실제 실행 판정의 기준이다.

- 최종 소스·버전·보존 범위: 보고서 1–2절. `25d825f` 이후 이번 수정과 사용자 요청의 1.7(8)을 포함한다.
- 빌드·JVM·Lint·계측 명령, opt-in 및 가로 2개 제외, 실행/캐시 재사용과 실패 이력: 보고서 4·9절.
- API 36 휴대전화의 세로 수동 시나리오: 보고서 5절. 74개 정의를 모두 수동 통과했다고 합산하지 않는다.
- 원본·최종 JPEG·프레임 조합·사진첩·실제 공유 수신: 보고서 6–7절.
- R8 설치 및 AAB 식별값·서명 검사, 버전 변경 후 재검사: 보고서 8–9절.
- 재현 결함과 수정 근거: 보고서 3절. 미검증·부분 검증·환경 제한: 보고서 10절.
- 가로·폰트 라이선스·공개 개인정보 페이지 제외와 화면 자동 꺼짐 방지 미변경: 사용자 지정 범위를 그대로 유지했다.

작업 이력은 [WORKLOG](../docs/WORKLOG.md)에 남긴다. 과거 성공 수치나 이 문서의 검사 정의 수를 실제 실행 결과에 합산하지 않는다.
