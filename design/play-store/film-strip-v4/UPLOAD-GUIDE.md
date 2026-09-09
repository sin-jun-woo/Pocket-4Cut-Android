# Pocket4Cut — Film Strip v4 등록 파일

2026-09-10 · 네 컷 인화지 + 필름 가장자리 + 하단 `pocket4cut` 표기.

## 바로 등록할 두 파일

- `app-icon-512.png`: Play Console의 **앱 아이콘**. 512×512, RGBA8(32-bit) PNG, sRGB, 344,668 bytes. 알파 채널은 있으나 픽셀은 모두 불투명하다. 둥근 모서리를 미리 적용하지 않았다.
- `feature-graphic-1024x500.png`: Play Console의 **피처 그래픽/그래픽 이미지**. 1024×500, RGB8(24-bit) PNG, sRGB, 알파 없음, 793,055 bytes. 휴대전화 스크린샷 칸에 넣는 파일이 아니다.

`app-icon-master-1024.png`와 `feature-graphic-master.png`는 보관용 고해상도 사본이다. 등록 칸에는 위 두 규격 파일을 사용한다. ZIP 자체를 Play Console에 등록하지 말고 압축을 푼 뒤 각각 업로드한다.

Play Console → 사용자 늘리기/Grow users → 스토어 등록정보/Store presence → 기본 스토어 등록정보/Main store listing → 그래픽/Graphics. 화면 명칭은 콘솔 언어와 개편에 따라 달라질 수 있다.

기존 휴대전화 소개 이미지 8장과 이전 ZIP은 변경하지 않았다. 프로젝트의 `design/play-store/print-booth-v1/phone-screenshots/`에 그대로 있으며, 이번 ZIP에는 새 아이콘/피처 그래픽과 보관용 원본만 담았다.

## 제작 이력과 등록 시 확인

이미지와 인물은 내장 **image_gen**으로 생성·편집한 가상의 성인 사진이다. 실제 사용자 사진, 실제 촬영 캡처, 타사 로고를 가져오지 않았다. 사람이 촬영했거나 수작업으로 그린 이미지라고 표기하지 않는다. 최종 생성 프롬프트는 `PROMPTS.md`에 포함했다.

업로드 시 Play Console의 자산별 AI 생성/편집 콘텐츠 신고 항목을 확인하고 제작 사실에 맞게 처리한다. 피처 그래픽은 앱의 분위기를 소개하는 홍보 이미지이며 실제 UI 캡처나 실물 사진 인화 서비스 제공의 증거가 아니다.

하단 표기는 두 종류 이미지 모두 정확히 소문자 `pocket4cut`이다. 작은 런처 크기에서는 글자를 읽기 위한 문구가 아니라 인화지의 서명 디테일로 보인다. Android 테마 아이콘은 사진/글자를 뺀 네 컷·필름 단색 심볼로 표시된다.

## 검증 범위

- 이미지 치수·PNG 채널/bit depth·ICC/sRGB·용량·알파 검사 통과.
- 앱 adaptive 일반/원형 아이콘은 새 PNG 전경을 참조한다. 5종 밀도 PNG와 독립 monochrome 벡터를 적용했다.
- Android `assembleDebug` 성공: 36개 태스크 중 10개 실행/26개 UP-TO-DATE. 아이콘 리소스와 APK는 이번 입력으로 재생성했다.
- 제작 도구에서 48/72/96/144px, 원형/사각/둥근 사각 마스크를 시각 검사했다. 실기기 런처 검증은 아니며, 단위 테스트·Lint·실기기 E2E는 이번 이미지 교체에서 새로 실행하지 않았다. 앞선 Lint의 기존 CAMERA 오류를 해결한 작업도 아니다.
- 등록 파일 제작은 완료했으나 Play Console 업로드·출시 서명·스토어 게시·심사 승인은 수행하지 않았다.

## 공식 규격 참고

- [Google Play 아이콘 규격](https://developer.android.com/distribute/google-play/resources/icon-design-specifications?hl=en)
- [미리보기 자산과 피처 그래픽 규격](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
- [AI 생성 콘텐츠 신고 안내](https://support.google.com/googleplay/android-developer/answer/17262077?hl=en)

스토어 표면별로 그래픽 가장자리가 잘리거나 오버레이가 생길 수 있다. 업로드 후 콘솔의 실제 표시 상태도 확인한다. 파일 규격 검증과 심사 승인은 별개다.
