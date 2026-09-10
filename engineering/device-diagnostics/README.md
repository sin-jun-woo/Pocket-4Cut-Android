# 실기기 렌더·상태 계약 진단

2026-09-10, HEAD `f79b2fd`의 production 코드로 Samsung SM-S942N / Android16에서 **7개 실행, 2개 통과, 5개 실패**했다. [전체 실기기 보고서](../DEVICE_QA_2026-09-10.md)를 함께 읽는다.

기존 앱/의존성은 바꾸지 않는다. 명시적으로 init script를 전달할 때만 이 소스 폴더가 androidTest에 추가된다.

```powershell
./gradlew.bat -I scripts/device-diagnostics.init.gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pocket4cut.diagnostic.RenderContractDiagnosticTest --console=plain
```

한 대의 연결·인증된 테스트 기기와 프로젝트에 맞는 JDK/SDK가 필요하다. 의존성이 캐시에 있을 때만 `--offline`을 추가한다. connected 태스크는 debug/test APK 설치 및 앱 프로세스 종료를 수반하므로 편집 중인 작업을 마친 뒤 실행한다.

테스트 본문은 180×240 ARGB_8888 색상·번호 Bitmap을 메모리에서 만들고 해제한다. 카메라, 기존 사진·세션 조회, MediaStore, 결과 저장·사용자 자료 삭제를 호출하지 않는다.

검사와 당시 결과:

1. 8레이아웃×4필터 32렌더, 크기·사진 슬롯 중앙색·입력순서: 통과. 클래식 4컷은 1650×4920, 나머지는 진단용 출력 폭 390px이다. 앱 기본 출력 크기의 전체 32조합 검증은 아니다.
2. 역순 사진 입력의 출력 순서: 통과. NavHost가 pending.order를 인계하는지는 별도 결함이다.
3. 8레이아웃×3문구 조건의 preview/export 공간: 실패(24조건).
4. 선택 가능한 색 ID→RGB 왕복: 실패(비흰색 카탈로그 ID 4개).
5. 콜라주6컷과 가로그리드6컷 차이: 실패(기하가 동일).
6. 하트 스티커와 이름 문자열 출력 차이: 실패(픽셀 동일).
7. UI가 이전 상태를 보유할 때 Bitmap 수명: 실패(조기 recycle).

마지막 검사는 수명 계약 위반을 검증한다. 실제 Compose draw 경합의 타이밍이나 사용자의 정확한 튕김 조작을 재현하는 테스트는 아니다. 출력 중앙색 검사는 실제 인물의 크롭·계절 장식·모든 글꼴/이모지 품질 검증을 대체하지 않는다.

실패 assertion을 반대로 바꾸거나 suppress하여 통과시키지 않는다. 제품 수정 후 같은 테스트와 관련 기기 UI 회귀 검사를 실행한다.
