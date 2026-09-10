# 실제 Compose 화면 렌더 하네스

이 폴더는 스토어 에셋 제작에 명시적으로 사용하는 테스트 소스다. 기존 앱의 `HomeScreen`, `FrameTypeSelectScreen`, `SelectionScreen`, `LayoutSelectionScreen`, `ColorFramePalettePickScreen`, `EditScreen`, `DetailEditScreen`, `ResultScreen`, `GalleryScreen`을 호출한다. 화면을 HTML이나 별도 Compose 구현으로 다시 그리지 않는다.

## 재생성

저장소 루트에서 로컬 JDK를 설정한 뒤 실행한다. 첫 실행에는 Paparazzi와 Windows layoutlib 의존성 다운로드가 필요하다.

```powershell
./gradlew.bat -I scripts/store-screenshots.init.gradle :app:testDebugUnitTest --tests com.pocket4cut.storecapture.StoreScreenshotsTest --console=plain
```

의존성이 캐시된 이후 실행은 `--offline`을 추가할 수 있다. 실제 검증에는 `testDebugUnitTest`를 사용했으며 `recordPaparazziDebug`를 사용하지 않았다.

`exportStoreScreenshots`가 마지막 실행의 최종 프레임을 `../raw-screenshots/`의 이름별 PNG로 복사하고 `capture-manifest.json`에 생성 시점·크기·실행 테스트를 기록한다. 모든 화면은 1080×2400px다. `warmup-*` 프레임은 납품 PNG에 포함하지 않는다.

## 상태와 데이터

- `../demo-photos/`의 생성 예제 사진 4장을 사용한다. 실제 사용자 사진이나 연결 기기 데이터는 읽지 않는다.
- 사진 선택은 8장 중 처음 4장 선택 상태다. 사진 순서는 바꾸지 않는다.
- 주요 흐름은 클래식 세로 4컷·화이트 프레임·원본 필터·문구/날짜 OFF다.
- 보관함의 화이트/블랙/스카이블루/블러쉬 결과는 앱의 `CollageRenderer`와 `FilterDefs`를 사용해 생성했다. 별도 이미지 편집으로 결과를 꾸미지 않는다.
- 결과는 120px, 편집은 650px, 레이아웃은 1000px, 상세 편집은 스크롤 끝의 실제 `ScrollState`를 공급한다. 결과는 인화지 전체와 홈/저장/공유 컨트롤이 함께 보이는 위치이며, 상세 편집은 네 번째 사진을 선택한 중립 보정 상태다.

## 테스트 환경 보완과 한계

현재 재생성 하네스는 Paparazzi 2.0.0-alpha05 / Android layoutlib 16.2.1로 호스트에서 실제 앱 Composable을 렌더한다. 기존 `capture-manifest.json`과 납품 PNG는 2.0.0-alpha02 / layoutlib 15.2.3으로 만든 산출물이며, 이번 빌드 도구 갱신에서는 다시 생성하지 않았다. 기기에서 실행한 캡처나 촬영·저장·공유 E2E 검증은 아니다.

Windows 호스트에서 Android `fstat`과 `FileProvider`의 기기 경로 처리를 사용할 수 없어 테스트 프로세스 안에서만 입력 바이트 디코딩·파일 URI로 보완한다. 예제 사진용 Coil fetcher는 동일한 Bitmap을 반환하고 사진 배치·크롭·UI 그리기는 앱 코드가 수행한다. VM에는 고정 예제 state를 주입하고 로딩 I/O와 IO dispatcher를 테스트에서 제어해 로딩 전 프레임이 저장되지 않도록 한다.

Byte Buddy 계측, 별도 sourceSet, Paparazzi 의존성은 `-I scripts/store-screenshots.init.gradle`을 명시했을 때만 적용한다. 일반 빌드와 출시 APK에는 이 테스트 소스·의존성·fixture가 들어가지 않는다. 저장·공유·삭제 콜백은 실행하지 않는다.

`fixtures/`는 예제 파일만 가진 로컬 제작용 저장소이고 Play Console 업로드 대상이 아니다. 스토어에는 상위 `phone-screenshots/`의 최종 홍보 이미지 8장을 사용한다.
