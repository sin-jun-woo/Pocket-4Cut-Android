# Pocket 4Cut — Print Booth 아이콘

2026-09-10(Asia/Seoul), 기준 HEAD `ac42903`과 기존 미커밋 UI 변경을 포함한 작업 트리를 참고해 제작했다.

UI의 인쇄 빨강 `#C83D2D`, 아이보리 `#FBFAF6`, 잉크색 `#1B1B19`을 사용한 네 컷 사진 스트립이다. 글꼴·이모지·사진·외부 이미지 없이 벡터로 직접 구성했다.

- `play-store-icon-512.png`: Play Console의 앱 아이콘에 등록하는 **512×512, 32bit RGBA PNG, sRGB**. 전체 불투명 정사각형이고 외곽 라운드·그림자를 포함하지 않는다.
- `app-icon-master-1024.png`: 1024×1024 편집·홍보용 고해상도 출력.
- `app-icon-source.svg`: 앱 원본과 동일한 편집 가능한 벡터 사본.
- `adaptive-foreground-432.png`: 108dp 레이어의 xxxhdpi 참조 출력. 배경은 투명하다.
- `adaptive-monochrome-432.png`: 단색/테마 아이콘 참조 출력. 사진 네 칸은 실제 투명 구멍이다.
- `icon-preview-grid.png`: 사각형·원형·둥근 사각형·squircle, 48px, 테마 적용과 66dp 안전영역 확인용. **실기기 캡처가 아니다.**
- `icon-validation.json`: 실제 치수, PNG 채널/알파/색 공간, 벡터 기하 일치와 안전영역 검증 결과.
- `previous-icon/`: 교체 전 19개 원본의 경로별 백업. 앱 런타임에서 읽지 않는다.

앱의 원본은 `app/src/main/assets/branding/pocket_4cut_app_icon.svg`이며 APK 런처는 SVG를 읽는 대신 Android vector XML 두 개를 사용한다. `ic_launcher_print_foreground.xml`과 `ic_launcher_monochrome.xml`의 기하를 원본과 자동 대조한다. normal/round adaptive XML은 새 전경과 별도 monochrome을 참조한다. 기존 foreground PNG도 같은 디자인으로 교체해 오래된 리소스가 남아 보이지 않게 했다.

런처 호환 출력은 mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi의 48/72/96/144/192px이고 adaptive 전경은 108/162/216/324/432px다. Play용 아이콘과 런처 전경을 서로 바꿔 등록하지 않는다.

재생성: `sharp`가 설치된 Node 환경에서 저장소 루트 기준 `node design/branding/print-booth-v1/render-icons.cjs`를 실행한다. 별도 설치 위치를 사용하는 환경에서는 `NODE_PATH`로 해당 `node_modules`를 지정한다. 개인 절대 경로는 프로젝트 설정에 저장하지 않는다. 실행 시 현재 아이콘 PNG 산출물만 다시 생성하고 이전 원본 백업은 변경하지 않는다.

규격 근거: [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), 확인일 2026-09-10. Play 등록 심사 통과나 기기별 런처 표시를 자동 보장하는 자료는 아니다.
