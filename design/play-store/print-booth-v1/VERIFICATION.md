# 검증 메모

기준: 2026-09-10 KST, HEAD `ac42903` + 기존 미커밋 UI 변경을 포함한 작업 트리. 아이콘과 등록 이미지를 생성한 작업이며 스토어 업로드·심사·release 배포는 하지 않았다.

## 앱 아이콘과 일반 빌드

- Manifest의 일반/원형 런처 아이콘이 새 adaptive 전경·배경·monochrome에 연결된다.
- 아이콘 512px/1024px 원본, 밀도별 PNG 15개, 벡터 구조와 안전 영역을 검사했다. 세부 결과는 `../../branding/print-booth-v1/icon-validation.json`에 있다.
- 스토어 512px 아이콘은 512×512 RGBA8, sRGB ICC, 14,882 bytes, alpha 255이며 앱 벡터 원본과 같은 디자인이다. 바깥 모서리를 미리 자르지 않았다.

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain
```

- `assembleDebug`: 성공. `packageDebug`가 실제 실행되어 새 아이콘이 포함된 APK를 생성했다. 리소스 컴파일 등 일부 입력은 앞선 화면 렌더 빌드의 결과를 재사용했다.
- `testDebugUnitTest`: 실제 실행 성공. 테스트 1개, 실패/오류/건너뜀 0개. `ExampleUnitTest.addition_isCorrect`이며 앱 사용 흐름 테스트는 아니다. 실행 시각 KST 2026-09-10 00:33:15.947.
- `lintDebug`: 실패. 기존 Manifest의 `PermissionImpliesUnsupportedChromeOsHardware` 오류 1개, 경고 91개, 힌트 2개. CAMERA에 대응하는 `uses-feature`가 없는 문제이며 이 이미지 작업에서 숨기거나 수정하지 않았다.
- 결합 명령은 exit code 1, 52개 태스크 중 15개 실행/37개 UP-TO-DATE였다. Lint 실패를 빌드·단위 테스트 실패 또는 전체 검증 성공으로 합치지 않는다.

## 화면 이미지

화면은 기존 Compose Screen을 테스트용 생성 사진과 상태로 렌더한다. 파일 I/O와 비동기 작업의 호스트 환경 차이는 캡처 도구 안에서만 처리한다. 앱 화면을 HTML로 다시 그리거나, 생성 모델로 UI를 만들어 실제 실행 화면으로 표기하지 않는다.

최종 크기·채널·출처 해시·문구/화면 경계 검사는 `asset-validation.json`, 실제 렌더 실행 기록은 `capture/capture-manifest.json`에 저장한다. 렌더 테스트 통과와 별개로 빈 사진, 로딩 표시, 사진 개수, 버튼, 필터 노출을 이미지에서 직접 확인한다.

```powershell
./gradlew.bat -I scripts/store-screenshots.init.gradle :app:testDebugUnitTest --tests com.pocket4cut.storecapture.StoreScreenshotsTest --offline --console=plain
node design/play-store/print-booth-v1/source/render-listing.cjs
```

- Compose 렌더 테스트 9개 성공, 실패/오류/건너뜀 0개. 원본은 1080×2400이며, Home을 제외한 8종을 등록 이미지에 사용한다.
- 등록 PNG 8장 모두 1080×1920, 3채널 RGB, sRGB, 알파 없음. 헤드라인·설명·푸터와 이미지 외곽이 캔버스를 벗어나지 않음을 검사했다.
- 초안에서 비동기 로딩 전 빈 사진이 캡처된 문제를 발견해 캡처 도구의 테스트 상태·파일 경로·I/O를 보완하고 다시 렌더했다. 빈 상태 초안은 최종 이미지로 사용하지 않았다.
- 레이아웃·필터·상세 편집은 실제 스크롤 상태를 사용해 선택지와 조작부를 노출했다. 이 때문에 레이아웃/상세 화면의 긴 콜라주 일부가 화면 위로 스크롤되어 있다. 사진을 임의로 자르거나 실제 UI의 구조를 바꾸어 합성한 것은 아니다.
- 보관함 예시는 실제 `CollageRenderer`로 생성한 화이트·블랙·스카이블루·블러쉬 프레임과 원본·흑백·소프트·필름 조합 4개다. 기본 결과 화면은 화이트 프레임, 캡션/날짜 OFF, 원래 사진 순서다.
- 최종 렌더 실행 시각은 KST 00:44:51.172 시작, 마지막 원본 생성은 00:45:13이다. 결과 화면을 120px 실제 스크롤해 인화지 하단·홈·저장·공유가 모두 보이는 것을 확인했다.

## 다운로드 묶음

`../Pocket4Cut-PlayStore-Assets.zip`은 4,290,390 bytes이며 파일 13개를 담는다. 휴대전화 PNG 8장, 512/1024 아이콘, 대체 텍스트, 업로드 안내, 전체 미리보기다. 제작용 원본·테스트 코드·예제 사진은 ZIP에서 제외했다.

ZIP을 다시 열어 내부 13개 파일의 SHA-256이 원본 파일과 모두 일치함을 검사했다. ZIP 자체 SHA-256은 `E4C58EA11D55A6FB3CB971E76ECAFA8BA4191B1A33230F934117E1812D26D5A5`다.

## 미검증 범위

- 연결된 기기와 에뮬레이터가 없어 실기기 런처, 카메라 촬영, 사진첩 저장·공유, Android E2E를 실행하지 않았다.
- 화면 안의 인물은 생성한 가상의 성인 예시다. 실제 이용자 후기, 카메라 화질 성능 또는 앱의 이미지 생성 기능을 주장하는 자료가 아니다. 앱에 번들하지 않는다.
- 현재 코드의 사진 재정렬 최종 출력, 커스텀 스티커 내보내기 등 기존 문제의 수정·검증은 이번 작업에 포함하지 않았다.
- 이미지 규격 준수가 Google Play 심사 승인을 보장하지 않는다. 실제 배포 버전의 화면·기능과 일치하는지 등록 전에 확인해야 한다.
