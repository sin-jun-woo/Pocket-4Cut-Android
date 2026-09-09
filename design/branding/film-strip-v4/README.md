# Film Strip v4 — 현재 적용 아이콘

사용자가 승인한 네 컷/필름 방향과 후속 요청한 하단 소문자 `pocket4cut`을 반영했다. 아이콘과 피처 그래픽은 내장 image_gen으로 제작했다. 사진 표현을 위해 이번 컬러 아이콘은 래스터로 변경했으며 앱 UI/프레임/콜라주 렌더 코드는 수정하지 않았다.

## 파일과 재생성

- 최종 생성 원본: `source/icon-with-wordmark.png`, `source/feature-with-wordmark.png`.
- 생성/편집 프롬프트: `source/PROMPTS.md`.
- 테마 아이콘: `adaptive-monochrome-source.svg`, Android `ic_launcher_monochrome.xml`과 같은 두 path. 네 사진창·천공 6개는 투명 구멍. 사진과 작은 글씨는 단색 테마 변형에서 생략했다.
- 등록 파일: `../../play-store/film-strip-v4/`.
- 이전 v1 런처 파일 21개: `previous-icon-v1/`. 이전 사용자 변경을 되돌릴 때 전체 작업 트리를 reset하지 말고 정확한 아이콘 파일만 비교한다.
- 재생성: sharp가 준비된 Node 환경에서 `node design/branding/film-strip-v4/export-assets.cjs` (저장소 루트 기준). Node의 패키지 탐색 경로는 실행 환경에서 설정하며 개인 경로를 코드에 고정하지 않았다.

초기 `draft-checkerboard-not-for-use.png`는 모델이 투명 체크무늬를 그린 미채택 초안이다. 진짜 알파가 아니며 어떤 앱/등록 출력에도 쓰지 않는다. 서명 추가 전 두 원본도 제작 이력용이다. 최종 선택된 두 원본만 exporter가 읽는다.

## Android 연결과 안전영역

두 adaptive XML의 foreground가 실제 density PNG `@drawable/ic_launcher_foreground`를 가리킨다. 배경은 `#1B1B19`; 이전 `ic_launcher_print_foreground.xml`은 새 PNG로 연결되는 호환 alias다. 기존 이름만 남은 옛 벡터가 활성화되지 않도록 했다.

컬러 생성 원본은 차콜 backing이 포함된 **불투명 이미지**다. 전경도 alpha=255의 RGBA PNG이며 투명 오브젝트 cutout으로 설명하지 않는다. Android adaptive drawable은 이 전체 사진 레이어를 마스킹한다. 레이어 간 독립적인 피사체 parallax는 검증하지 않았다.

108dp를 1296px로 내보내고 생성 이미지 전체를 1104px로 축소해 가운데 배치한다. 실제 사진/종이/필름의 차콜 대비 픽셀(채널 차이>20) 최대 반경은 31.283dp 미만으로 66dp 안전원 안이다. 이 검사는 명암 대비 기반 보조 검사이지 알파 실루엣의 수학적 증명은 아니다. 원형/사각/둥근 사각 마스크와 축소판을 사람이 볼 수 있는 출력으로 검토했다. 단색 심볼 외곽 꼭짓점 반경은 30.017dp다.

`icon-preview-grid.png`는 오프라인 마스크/크기 검토판이며 실기기 화면이 아니다. 기본 명암/회색조에서 사진의 미세 디테일과 글자는 축소 시 읽히지 않는다. 네 칸 인화지의 실루엣을 식별 기준으로 삼는다.

브랜드 `assets/branding/pocket_4cut_app_icon.svg`는 이제 같은 폴더의 현재 PNG를 가리키는 래스터 미리보기 wrapper다. 자체 완결형 벡터나 런타임 SVG decoder를 뜻하지 않는다. PNG 사본은 등록용 1024 master와 같다.

치수·색 공간·알파·파일 해시는 `asset-validation.json`. APK 빌드와 검사 결과는 `docs/WORKLOG.md`를 따른다. Git commit/push나 스토어 업로드는 하지 않았다.
