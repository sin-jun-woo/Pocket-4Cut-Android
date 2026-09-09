# Pocket 4Cut — Google Play 등록용 이미지

제작 기준: 2026-09-10 (Asia/Seoul), `codex/setup-project-guidance`의 HEAD `ac42903`과 기존 미커밋 앱 UI 변경을 포함한 작업 트리.

## 등록 파일

- `../Pocket4Cut-PlayStore-Assets.zip`: 업로드 파일을 모은 다운로드용 묶음. 개별 PNG 8장과 아이콘, 대체 텍스트, 업로드 안내, 확인용 전체 미리보기가 들어 있다.
- `app-icon-512.png`: 스토어의 **앱 아이콘** 항목에 사용. 512×512, 32-bit PNG, sRGB, 풀스퀘어.
- `phone-screenshots/01_*.png`부터 `08_*.png`: 스토어의 **휴대전화 스크린샷** 항목에 번호순으로 사용. 각각 1080×1920, 24-bit RGB PNG, 투명도 없음.
- `overview.png`: 8장 전체 확인용. 스토어에 올리는 파일이 아님.
- `alt-text-ko.txt`: 각 스크린샷에 함께 입력할 한국어 대체 텍스트.

휴대전화 기기 유형에는 최대 8장을 등록할 수 있으므로 8장으로 구성했다. 다른 기기 유형, 동영상 또는 사용자 리뷰 이미지가 아니다. 등록·게시·계정 로그인은 이 작업에 포함하지 않는다.

## 구성

1. **오늘의 표정, 한 장의 추억.** 완성 예시와 저장·공유.
2. **두 컷, 네 컷, 여섯 컷.** 촬영 구성 선택.
3. **마음에 드는 표정만 골라요.** 여러 촬영본에서 사진 선택.
4. **사진이 달라지는 배치의 차이.** 세로·격자·가로 배치.
5. **오늘의 분위기에 어울리는 색.** 프레임 색 선택.
6. **같은 순간도, 다른 느낌으로.** 네 가지 사진 필터.
7. **한 컷씩, 조금 더 내 취향.** 사진별 밝기·대비·채도·회전·반전.
8. **완성한 순간을 차곡차곡.** 완성 콜라주 보관함.

헤드라인은 화면 상단 약 20% 안에 배치한다. 순위·이용자 수·후기·수상·가격 주장이나 설치를 재촉하는 문구를 넣지 않았다. 실제 UI의 글자·버튼을 홍보 문구로 덮지 않고, 추가 문구는 화면 밖에 배치한다.

## 제작 원본과 출처

- `raw-screenshots/`: 현행 앱의 Compose Screen 함수를 호출해 만든 렌더 이미지. 실제 기기 카메라로 촬영한 사진이나 실기기 E2E 검증 영상이 아니다.
- `capture/` 및 저장소의 `scripts/store-screenshots.init.gradle`: 명시적으로 실행할 때만 사용하는 화면 렌더 도구. 일반 앱 빌드와 release 앱에는 포함하지 않는다.
- `source/story.json`: 헤드라인·설명·대체 텍스트.
- `source/render-listing.cjs`: 실제 화면 이미지의 비율을 유지하면서 등록용 PNG로 출력하는 제작 코드.
- `demo-photos/`: 화면 예시에만 사용한 생성 사진. 가상의 성인 인물이며 실제 이용자 후기·촬영 성능의 증거가 아니다. 앱에 번들하지 않는다.
- `source/photo-prompt.md`: Built-in image generation 사용 여부와 실제 프롬프트 기록.

이전 `design/mockups/print-booth-v1`의 HTML 재현 화면을 실제 Android 캡처로 바꿔 표기하지 않는다. 출시 앱이 이번 작업 트리와 달라지면 등록 전 화면을 다시 생성해야 한다. 확인된 코드상의 기존 문제인 사진 재정렬의 최종 출력, 커스텀 스티커의 저장 결과 등은 이 구성의 홍보 소재로 사용하지 않는다.

## 다시 만들기

Node.js에서 `playwright`, `sharp`가 필요하며, 기본 브라우저는 Windows Chrome이다. `CHROME_PATH` 환경변수로 실행 파일 위치를 바꿀 수 있다.

```powershell
# 프로젝트 루트에서 실제 화면 렌더 — JDK 21, Android SDK 필요
./gradlew.bat -I scripts/store-screenshots.init.gradle :app:testDebugUnitTest --tests com.pocket4cut.storecapture.StoreScreenshotsTest --offline --console=plain

# 원본 화면으로 휴대전화 등록 이미지 생성
node design/play-store/print-booth-v1/source/render-listing.cjs
```

`asset-validation.json`에 각 이미지의 크기·채널·파일 크기와 잘림 검사를 기록한다. 빌드 검증과 실기기 검증은 별개이며 상세 결과는 함께 제공되는 검증 메모를 따른다.

최초 실행 환경에 Paparazzi/Byte Buddy 의존성이 캐시되어 있지 않다면 `--offline`을 빼고 내려받아야 한다. 캡처 태스크가 끝나면 `exportStoreScreenshots`가 자동 실행되어 원본 PNG와 실행 기록을 내보낸다. 이 렌더 확인은 실제 기기의 촬영·저장·공유 테스트를 대체하지 않는다.

## 확인한 공식 규격

- [Google Play 미리보기 애셋 안내](https://support.google.com/googleplay/android-developer/answer/9866151?hl=ko): 기기 유형별 최대 8장, JPEG 또는 알파 없는 24-bit PNG, 기본 크기/비율 및 콘텐츠 지침.
- [Google Play 아이콘 규격](https://developer.android.com/distribute/google-play/resources/icon-design-specifications): 512×512, 32-bit PNG, sRGB, 1024KB 이하, 바깥 모서리 마스크와 그림자는 스토어에서 적용.
- [Android Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive): 런처의 전경·배경·단색 레이어와 안전 영역.

이미지 규격을 충족하는 것과 Google Play 심사 승인은 별개다. 이 작업에서는 스토어 업로드나 심사를 실행하지 않았다.
