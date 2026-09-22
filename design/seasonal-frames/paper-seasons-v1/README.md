# Pocket 4Cut — Paper Seasons

봄·여름·가을·겨울을 색연필/종이 스티커 질감으로 새로 구성한 계절 프레임입니다. 앱의 현재 2·4·6컷 전체 8배치와 구형 6컷 배치를 지원합니다.

## 파일

- [전체 다운로드 ZIP](deliverables/pocket4cut-paper-seasons-v1.zip): 113개 파일, 103,096,843 bytes(약 98.32 MiB). 해제한 모든 파일의 길이·SHA-256을 원본과 대조했다.
- [전체 32종 모아보기](deliverables/overview-all-seasons.png), [클래식 4컷 모아보기](deliverables/overview-classic-four.png).
- [인스타그램 홍보 이미지](deliverables/instagram-promo.png): 1080×1350, 4:5, 불투명 sRGB PNG. 실제 프레임을 참고한 인화지 포스터 시안이며 앱 화면 캡처가 아니다.
- [실제 Compose 화면 검증 캡처](verification/autumn-picker-compose.png): API 37 QA 계절 선택 화면, 1280×2856. 자동 UI 테스트 중 캡처했으며 전체 촬영 흐름의 수동 검증을 뜻하지 않는다.
- `deliverables/frames/`: 사진창이 실제 투명한 프레임 PNG 72개. 기본 32개, 문구 공간 변형 32개, 구형 배치 8개.
- `deliverables/examples/`: 기존의 가상 성인 사진을 넣은 실제 Android 렌더 JPEG 32개. 사용자가 촬영한 사진이 아닙니다.
- `deliverables/manifest.json`: 정확한 픽셀 크기, 사진 슬롯 좌표, 계절, 배치 버전, 문구 공간.
- `deliverables/README-USAGE.txt`: 프레임 사용 안내.
- `source/*-atlas.png`: 수정하지 않은 생성 원본 4개.
- `source/GENERATION-PROMPTS.md`: 내장 이미지 생성 도구의 실제 프롬프트와 출처.
- `asset-validation.json`: 런타임 atlas의 크기·알파·해시 검증.
- `deliverables/package-validation.json`: ZIP 113개 항목의 파일 크기·해시 대조 결과. ZIP 자체의 SHA-256은 `ecf1a7d655da6aec96656fb331052a1491cf844d6b1927b40323a955550044e2`다. 프레임/예시의 형식·치수·알파 검사는 `source/package-deliverables.cjs`가 `manifest.json` 및 실제 이미지와 대조해 수행한다.
- `source/instagram-promo-master.png`, `source/INSTAGRAM-PROMPT.md`: 홍보 이미지의 생성 원본(1122×1402)과 실제 요청·참조 자료 기록.

앱의 일반 저장은 기존처럼 JPEG입니다. 투명 프레임 PNG는 외부 디자인/보관을 위한 납품 자료이며 앱에 PNG 저장 기능을 추가한 것은 아닙니다. 이미 저장한 사용자 결과와 원본은 변경하지 않습니다.

## 다시 만들기

1. 기존 Node 환경의 `sharp`로 `node design/seasonal-frames/paper-seasons-v1/source/prepare-assets.cjs`를 실행합니다.
2. QA 기기에서 `SeasonalFrameContractInstrumentedTest`의 exporter를 `seasonalExport=true` 인자로 실행합니다. AGP `additionalTestOutputDir`로 생성된 자료가 PC의 connected Android test additional output에 복사됩니다.
3. 그 실행의 `frames`, `examples`, `manifest.json`을 `deliverables`로 복사합니다. 다른 실행의 변형을 섞지 않습니다.
4. `sharp`, `@napi-rs/canvas`, `jszip`가 있는 환경에서 `node design/seasonal-frames/paper-seasons-v1/source/package-deliverables.cjs`를 실행합니다. ZIP은 다시 열어 모든 파일 해시를 대조합니다.

슬롯/문구/브랜드는 코드로, 장식은 생성 일러스트로 제작했습니다. 생성물의 출처를 감추거나 사람이 직접 손으로 그렸다고 주장하지 않습니다. 기능·빌드·계측·실제 기기 검증의 범위는 저장소 `docs/WORKLOG.md`의 Paper Seasons 기록을 참고하세요.
