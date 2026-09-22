# Pocket 4Cut 이미지·그래픽 자산 가이드

> **Paper Seasons 업데이트:** 아래 신뢰성 개편 기준 이후, 2026-09-22 계절 프레임 4종을 새 일러스트로 교체했다. 현행 추가 규격은 마지막 Paper Seasons 절을 참고한다.

> 현행 코드 확인일: 2026-09-22 · 기준: `codex/refactor-reliability`의 앱 소스 커밋 `155ced0b44b381bb8d274d2652d6d2fd421b2973`(시작 기준 `a0eff04`).
> 위 커밋은 신뢰성 문서 정리 당시 기준이다. 1~6절의 계절 자산 설명은 이후 Paper Seasons 구현에 맞췄으며, 실제 이미지 교체·납품·검증 내역은 13절에 기록했다. 날짜가 있는 아이콘·영상 절은 해당 제작 당시의 이력이다.

## 2026-09-10 적용 기록 — Film Strip v4 런처/스토어 자산

HEAD `ac42903`과 미커밋 UI 변경을 유지한 작업 트리에서 사용자가 네 컷/필름 사진 중심 아이콘 및 피처 그래픽 제작·적용을 요청했다. 후속 요청에 따라 인화지 하단에 소문자 `pocket4cut`을 추가했다. 아래 Print Booth v1은 이전 시점 기록이며, 현행 리소스 규격은 1~2절에서 확인한다.

- 현재 컬러 원본은 `design/branding/film-strip-v4/source/icon-with-wordmark.png`. 네 컷 인물 사진·인화지·필름 표현을 위해 image_gen으로 제작한 래스터를 사용한다. 인물은 가상의 성인이며 사용자 사진을 사용하지 않았다.
- `mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml`은 실제 새 density PNG `@drawable/ic_launcher_foreground`를 참조한다. 기존 전경 XML 이름은 새 PNG를 가리키는 호환 alias로 갱신했다. 배경은 `#1B1B19`다.
- density 전경5개(108/162/216/324/432), 일반/원형5쌍(48/72/96/144/192)을 새 이미지로 재생성했다. **컬러 전경에는 차콜 backing이 포함되며 알파가 모두255다.** 투명 cutout으로 설명하지 않는다. 원형 legacy PNG에는 마스크 투명도가 있다.
- `ic_launcher_monochrome.xml`은 별도 단색 네 컷·필름 벡터다. 사진창4개·천공6개는 실제 투명이며, 사진의 인물·하단 글씨는 단색 테마 변형에서 생략한다. 전체 외곽 반경30.017dp.
- 컬러 전경은108dp, 표시용 중심 crop은72dp다. 차콜 대비>20인 픽셀의 최대 반경31.283dp 미만을 확인했고 마스크·축소판도 시각 검사했다. 낮은 대비의 가장자리를 놓칠 수 있는 보조 검사이므로 완전한 알파 실루엣 검증 또는 실기기 검증으로 표현하지 않는다.
- `assets/branding/pocket_4cut_app_icon.png`는 현재1024px master 사본이고, 같은 이름 SVG는 옆 PNG를 읽는 preview wrapper다. 자체 완결형 벡터도 런타임 SVG 로더도 아니다.
- 등록 파일: `design/play-store/film-strip-v4/app-icon-512.png` (512×512 RGBA8/sRGB/344,668B), `feature-graphic-1024x500.png` (1024×500 RGB8/sRGB/알파 없음/793,055B). 내장 생성 도구의 프롬프트와 AI 생성 사실을 안내 파일에 기록했다.
- 재생성: sharp가 준비된 Node에서 `node design/branding/film-strip-v4/export-assets.cjs`. 생성 이미지를 다시 그리는 스크립트가 아니라 크기·색상 형식·안전영역·리소스 출력을 만드는 도구다.
- 이전 v1 파일21개는 `design/branding/film-strip-v4/previous-icon-v1/`에 보존했다. 기존 휴대전화 소개 이미지8장과 이전 배포 ZIP은 변경하지 않았다.

시각 검토판·출력23개 해시·파일검사는 `design/branding/film-strip-v4/asset-validation.json` 및 README를 따른다. 앱 빌드 결과와 미실행 검사는 WORKLOG에 별도로 기록했다. 프레임·사용자 결과 JPEG·UI 코드의 렌더 계약은 이번 작업에서 변경하지 않았다.

## 2026-09-10 이전 갱신 — Print Booth v1 런처 아이콘

HEAD `ac42903` 및 기존 미커밋 UI 변경을 포함한 작업 트리에서, 사용자의 앱 아이콘 제작·적용 요청으로 런처 자산을 교체했다. 이 절은 Film Strip v4로 교체되기 전 이력이며 현행 파일 규격은 1~2절을 따른다. 프레임·저장 JPEG 렌더링 계약은 이 작업에서 변경하지 않았다.

- 브랜드 원본: `app/src/main/assets/branding/pocket_4cut_app_icon.svg`, 1024×1024, `viewBox="18 18 72 72"`. 앱은 여전히 SVG를 직접 읽지 않는다.
- 적응형 아이콘의 배경: 기존 `ic_launcher_background.xml`을 인쇄 빨강 `#C83D2D` 단색으로 교체.
- 적응형 전경: 신규 `res/drawable/ic_launcher_print_foreground.xml`, 108dp 벡터. 아이보리·잉크색 네 컷 스트립.
- 테마 아이콘: 신규 `res/drawable/ic_launcher_monochrome.xml`. 사진 네 칸이 실제 투명 구멍인 독립 단색 벡터.
- `mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml`을 위 전경·단색 리소스에 연결했다.
- 기존 density별 PNG 15개의 이름과 크기는 유지하고 새 디자인으로 재생성했다. 일반 launcher는 전체 불투명, round launcher와 foreground에는 투명 영역이 있다.
- 제작 원본·512px Play 아이콘·1024px master·마스크 검토·검증 JSON은 `design/branding/print-booth-v1/`에 있다. 교체 전 원본 19개도 이 폴더의 `previous-icon/`에 보존했다.
- 재생성: `sharp`를 사용할 수 있는 Node 환경에서 `node design/branding/print-booth-v1/render-icons.cjs`.
- 확인: Play PNG 512×512 RGBA8/sRGB/14,882B/알파255, 5종 density 치수, XML 5개 파싱, 원본·벡터 기하 일치, 66dp 안전원 안 마크(최대 반경32.45dp), 단색 투명창4개.
- 마스크 검토 이미지는 제작 도구 출력이며 실기기 런처 화면 캡처가 아니다. 빌드와 기기 검증 범위는 해당 WORKLOG 기록을 따른다.

스토어 휴대전화 소개 이미지는 `design/play-store/print-booth-v1/`에 제작하며, 등록용 이미지·실제 Compose 화면 렌더·예시 생성 사진·검증용 전체 보기를 구분한다. `docs/`에는 예시 사진이나 원시 캡처를 복사하지 않는다.

## 1. 현행 자산과 렌더 경로

프레임 기하는 [FrameLayouts](../app/src/main/java/com/pocket4cut/frame/FrameLayouts.kt), [FrameTheme](../app/src/main/java/com/pocket4cut/frame/FrameTheme.kt), [FrameColors](../app/src/main/java/com/pocket4cut/frame/FrameColors.kt)의 데이터와 Kotlin 그리기 코드로 구성된다. 2026-09-22 Paper Seasons 교체 후 계절 프레임은 `SeasonHTMLFrameStyle`의 종이색·기하 패턴과 `SeasonalStickerArt`의 새 RGBA 일러스트를 합성한다. 구형 계절 벡터 4개와 전용 anchor 코드는 제거했으며 WebView를 실행하지 않는다. 아래 역사 기록과 현행 교체 기록을 구분한다.

- 사용자 장식은 `CustomFrameDecoration`의 Text/Emoji/Sticker 모델이다. 스티커 25종은 `StickerPalette`의 Material ImageVector이며 PNG/SVG 파일 자동 검색 로더는 없다.
- [CollagePreview](../app/src/main/java/com/pocket4cut/frame/CollagePreview.kt)와 최종 JPEG는 [CollageRenderer.drawScene](../app/src/main/java/com/pocket4cut/frame/CollageRenderer.kt)을 공유한다. 미리보기와 결과에서 스티커 이름 대신 같은 vector path를 그린다.
- [StickerVectorPainter](../app/src/main/java/com/pocket4cut/frame/StickerVectorPainter.kt)는 현재 카탈로그 vector의 group 변환과 path를 Android Canvas로 옮긴다. 임의 SVG의 모든 stroke·clip·gradient·alpha 기능을 지원하는 범용 벡터 로더는 아니다.
- UI 기능 아이콘은 주로 Compose Material Icons다. 장식의 저장 ID와 UI 아이콘의 컴포넌트 식별자를 혼동하지 않는다.
- 브랜드 `app/src/main/assets/branding/pocket_4cut_app_icon.svg`는 1024×1024, `viewBox="0 0 1024 1024"`인 PNG 참조 wrapper다. 인접 `pocket_4cut_app_icon.png`가 필요하며 자체 완결 벡터나 Android 런타임 SVG 지원을 뜻하지 않는다.
- 글꼴은 `assets/fonts/`의 TTF 13개와 시스템 글꼴을 사용한다. `AppFontCatalog`가 Compose FontFamily와 Android Typeface를 연결한다.
- 실제 촬영본·완성 JPEG는 앱 실행 데이터다. 사용자 사진을 번들 자산이나 공개 문서 예시로 복사하지 않는다.

신뢰성 리팩터링 자체는 코드 렌더 계약만 변경했다. 이후 Paper Seasons 작업에서 새 계절 일러스트 PNG 4개를 추가·연결했으며 아이콘과 다른 색상/사용자 스티커 카탈로그는 유지했다.

## 2. 현재 런처 PNG 규격과 검증 근거

현재 launcher는 위 Film Strip v4다. 2026-09-22에 리소스 PNG 15개의 SHA-256을 `design/branding/film-strip-v4/asset-validation.json`의 출력 기록과 대조해 모두 일치함을 확인했다. 아래 치수·알파는 그 동일 파일의 기존 검증 기록이다. 이번 문서 작업에서 전체 픽셀 검사나 실기기 런처 시각 검수를 다시 수행한 것은 아니다.

- `mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi`의 `ic_launcher.png`, `ic_launcher_round.png`: 각각 48, 72, 96, 144, 192px 정사각형.
- `drawable-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi/ic_launcher_foreground.png`: 각각 108, 162, 216, 324, 432px 정사각형.
- 15개 모두 8bit RGBA PNG다. 일반 launcher와 컬러 foreground는 알파 255인 불투명 이미지다. round PNG는 알파 0~255의 마스크 투명도를 가진다.
- adaptive icon XML은 background `ic_launcher_background`, foreground `ic_launcher_foreground`, monochrome `ic_launcher_monochrome`을 각각 참조한다.
- 현재 배경은 `#1B1B19` 단색이다. monochrome은 독립 단색 벡터이며 컬러 사진 foreground를 재사용하지 않는다.
- 원본 생성·권리/출처·export script·안전영역 검사 기준은 Film Strip v4 제작 기록에 남아 있다. 이전 Print Booth의 파일 규격·시각 결과는 역사 기록으로만 읽는다.

## 3. 프레임 기하와 기기 독립 출력

[CollageLayoutMath](../app/src/main/java/com/pocket4cut/frame/CollageLayoutMath.kt)가 사진 셀·캔버스·브랜드·문구/날짜 영역을 계산한다. 이미지 시안에서 슬롯을 눈대중으로 재구성하지 않는다.

- ID는 `TWO_VERTICAL`, `TWO_HORIZONTAL`, `FOUR_VERTICAL`, `FOUR_GRID`, `FOUR_HORIZONTAL`, `SIX_GRID_2X3`, `SIX_GRID_3X2`, `SIX_COLLAGE`의 8개다.
- 클래식 `FOUR_VERTICAL`은 1650×4920px 고정이다. 문구/날짜 영역은 고정 높이 안의 사진 영역에 영향을 준다.
- 그 외 신규 출력은 [CollageOutputSize](../app/src/main/java/com/pocket4cut/frame/CollageOutputSize.kt)가 기하를 기준으로 결정한다. 가장 작은 슬롯 짧은 변 1024px를 목표로 하며 16,000,000 pixels·각 변 8192px를 상한으로 둔다. 상한에 걸리면 슬롯 목표보다 작아질 수 있다.
- 문구·날짜·theme·배치가 같으면 기기 화면 폭이 출력 치수를 바꾸지 않는다. 모든 배치가 같은 가로/세로 치수를 사용하는 것은 아니다.
- `SIX_COLLAGE` layoutVersion 2는 첫 사진이 큰 슬롯을 차지하는 비대칭 6컷이다. legacy layoutVersion 1은 이전 3×2 기하를 유지한다. 기존 완료 JPEG는 새 기하로 자동 변환하지 않는다.
- 일반 grid는 theme의 outerPadding/cellSpacing을 사용한다. 2컷 가로 및 새 6컷 콜라주에는 별도 기하 규칙이 있으므로 FrameStyle.padding/gap만 읽어 슬롯을 추정하지 않는다.
- 사진은 셀에 aspect-fill로 들어가므로 가장자리가 잘릴 수 있다. 원본 전체를 항상 fit하는 계약이 아니다.

활성 출력은 `DetailEditViewModel → RenderSnapshot → CollageRenderer`다. 과거 `CollageExportMetrics`의 화면 폭 계산과 `Constants.RESULT_IMAGE_MAX_WIDTH`/이전 `CollageFinalize`를 현행 출력 규격으로 사용하지 않는다.

새 계절 atlas는 1536×1024, 512px 셀 6개이며 프레임 전체를 늘리는 배경 이미지가 아니다. 장식을 실제 header·side·gutter·footer 영역에 배치하고 사진 슬롯·브랜드·문구 영역에서는 clip한다. 기본 8배치와 구형 비대칭 이전 배치, 문구 유무를 별도로 검사한다.

## 4. 원본·미리보기·결과 파일

[FileImageStorage](../app/src/main/java/com/pocket4cut/data/storage/FileImageStorage.kt)의 활성 파일 구조는 다음과 같다.

```text
getExternalFilesDir(Pictures)/Pocket4Cut/
  captures/<sessionId>/.pending/<uuid>.jpg
  captures/<sessionId>/cap_01.jpg
  captures/<sessionId>/cap_02.jpg
  results/<sessionId>_<resultId>.jpg
```

- 새 촬영은 CameraX JPEG를 임시 경로에서 촬영 슬롯으로 게시하고 photo ID를 세션 문서에 연결한다. 기존 슬롯이 있으면 덮어쓰지 않는다.
- 활성 촬영 경로는 `rewriteJpegMaxLongEdge()`를 호출하지 않는다. 원본을 2048px/JPEG 92로 덮어쓰는 이전 동작과 구분한다. 촬영 JPEG의 실제 치수는 CameraX/기기 입력에 따르며 모든 촬영본이 같은 고정 치수라고 가정하지 않는다.
- 이전 버전에 이미 축소된 사진은 그 바이트를 보존한다. 마이그레이션이나 큰 해상도 재렌더링이 소실된 세부 정보를 되살리지는 않는다.
- 기본 편집은 720, 프레임 선택은 512, 상세 편집 진입은 2048, 최종 상세 처리는 3072px 크기를 요청하여 디코드한다. sampling 요청은 이미지 긴 변의 엄격한 상한이 아니다. 별도 디스크 proxy 캐시는 아직 없고 미리보기 Bitmap을 메모리에 보유한다.
- 최종 렌더의 imageProvider는 필요한 원본을 한 장씩 보정해 반환하고 슬롯을 그린 뒤 그 Bitmap을 회수한다. UI Bitmap의 수명과 구분한다.
- 새 결과는 JPEG 품질 98로 임시 인코딩·sync 후 고유 `<sessionId>_<resultId>.jpg`를 게시한다. 같은 세션의 다음 적용은 기존 결과를 교체하지 않고 새 결과를 만든다.
- legacy `results/<sessionId>_result.jpg`는 이전 결과로 보존한다. 활성 촬영 파일 목록 helper는 `cap_[숫자].jpg` 패턴만 읽는다.
- 사진 선택·순서·보정의 기준은 [SessionDocument](../app/src/main/java/com/pocket4cut/domain/model/SessionDocument.kt)의 photo ID다. 파일명이나 배열 위치를 논리 ID 대신 사용하지 않는다.

[GalleryExporter](../app/src/main/java/com/pocket4cut/data/export/GalleryExporter.kt)는 결과 JPEG를 공용 `Pictures/Pocket4Cut/`에 byte copy한다. 표시 파일명은 결과 ID와 export operation ID를 포함한다. 저장 상태·URI를 기록해 기존 정상 결과 저장을 재사용하며 복사 과정에 SHA-256 검사를 사용한다. 공유는 파일 검증 후 FileProvider URI를 전달한다. 앱 내부 결과와 공용 사본의 삭제 범위는 다르다.

사진·초안·세션/export 기록은 Android 클라우드와 기기 이전의 백업 허용 대상에서 제외되어 있다. 설정 SharedPreferences만 백업하며, 미내보낸 촬영 자료의 다른 기기 자동 복원을 보장하지 않는다. 삭제·legacy 이전·실패 복구의 상세 범위는 [ARCHITECTURE](ARCHITECTURE.md)를 따른다.

## 5. 색·투명도·장식 변환

합성 Bitmap은 ARGB_8888이지만 최종 JPEG는 알파를 보존하지 않는다. 투명 PNG 완성본 내보내기 기능은 없다. 원본 제작 자산의 투명도와 최종 불투명 JPEG를 구분한다.

`CustomFrameDecoration.position`은 캔버스 전체 기준 정규화 중심 좌표다. `scale`은 배율, `rotationRadians`는 라디안이며 커스텀 편집기의 degree 제스처를 라디안으로 변환한다. 커스텀 편집 화면도 장식을 Compose 글자·아이콘 오버레이로 따로 그리지 않고 일반 미리보기·최종 출력과 같은 `drawScene`에 전달한다. 커스텀 텍스트는 논리 폭과 최대 2행 말줄임 규칙을 공유한다. 선택 표시·터치 영역까지 모든 화면 크기에서 픽셀 정합이 검증된 것은 아니다.

출시 권리 검토에서 `assets/fonts/`의 TTF 13개에 대한 라이선스 전문·파일별 원본 URL/해시가 저장소에 없음을 확인했다. 파일명 기준으로 배민 4종(`BMHANNAPro`, `BMJUA_ttf`, `BMKkubulimTTF`, `BMYEONSUNG_ttf`), 카페24 3종, GC컴퍼니 `Jalnan2TTF` 1종, 네이버 나눔/클로바 5종이다. 이는 이름으로 분류한 것이며 바이너리 출처·허가 확인이 아니다. [우아한형제들](https://www.woowahan.com/fonts/license), [카페24](https://help.cafe24.com/faq/web-hosting/introduce/new-renewal-change/cafe24_free_fonts_usage/), [GC컴퍼니](https://gccompany.co.kr/font), [네이버](https://hangeul.naver.com/font)의 공식 배포 조건과 각 파일 원본을 대조하고, 필요한 저작권 표시·라이선스 전문을 배포물에 포함해야 한다. 특히 네이버·우아한형제들의 고지 조건을 확인해야 한다. 현재 파일을 임의로 재배포 가능하다고 판정하지 않는다.

스티커/문구 색은 24bit RGB를 불투명 색으로 그린다. `textColorARGB`라는 이름만 보고 사용자 지정 반투명 장식을 지원한다고 설명하지 않는다. 계절 Paint의 별도 alpha는 다른 기능이다.

[FrameColors](../app/src/main/java/com/pocket4cut/frame/FrameColors.kt)는 기본 색과 `catalog_<theme.id>`를 모두 조회한다. hologram/sunset/aurora는 gradientStops를 렌더러의 LinearGradient에 전달한다. 커스텀 프레임 후 색 변경도 디자인의 fillColorId에 반영한다. 사진 필터 ORIGINAL/SOFT/FILM/BW는 사진에 적용하며 프레임·장식 전체에 같은 필터를 씌우지 않는다.

## 6. 현행 개선과 남은 자산 검증

소스에서 연결된 개선은 스티커 path 출력, 미리보기/최종 drawScene 공유, 실제 문구 영역, 카탈로그 색 왕복, gradient stops, 라디안 회전, 버전별 6컷 기하, 고정된 출력 정책이다. 이전 감사에서 재현한 현상을 현재도 그대로 발생한다고 적지 않으며, 수정 코드가 있다는 이유로 모든 자산 조합 검증이 완료됐다고 적지도 않는다.

다음 사항은 여전히 확인해야 한다.

1. 현재 StickerVectorPainter는 한 색으로 path를 채우는 카탈로그 대응 코드다. 다중 색·stroke·clip·trim·path별 alpha가 필요한 새 벡터는 렌더 계약을 먼저 확장해야 한다.
2. 계절별 anchor, 긴 문구/날짜, 모든 글꼴·이모지, 장식 극단 크기/회전은 8배치에서 비교한다. 같은 painter 사용만으로 폰트 환경 차이·모든 크롭 품질까지 검증되지는 않는다.
3. Renderer/Preview API는 background Bitmap을 받을 수 있지만 사용자가 외부 PNG/SVG 배경을 고르는 흐름은 없다. 자산 파일 추가만으로 자동 연결되지 않는다.
4. 원본 사진의 ICC/광색역/HDR을 모두 명시적으로 변환·검증하는 파이프라인은 아니다. 출력 픽셀 상한은 전체 앱 메모리 상한도 아니다.
5. 계측 소스에 렌더 계약과 출력 정책 검사가 존재한다. 실행 횟수·통과 여부·합성 fixture/물리 카메라 범위는 해당 검증 기록에서 확인한다.


## 7. 신규 자산 제작 제안 — 아직 구현된 규칙이 아님

이 절은 이후 자산 작업을 맡을 때 사용할 **제안**이다. 현재 앱에 없는 로더, 파일명 규약, 자동 변환 파이프라인이 이미 있다고 해석하지 않는다. 기존 파일·ID를 이 제안에 맞춰 일괄 변환하거나 이름 변경하지 않는다.

### 형식과 구성 제안

- 단순 아이콘·계절 도형은 현재 코드 벡터 체계를 우선 검토한다. 새 벡터를 추가할 때는 Compose 미리보기와 Android Canvas 출력이 같은 자산을 그리는 방법부터 정한다.
- 사진·질감처럼 raster가 필요한 경우에만 PNG/JPEG 등 파일 자산을 도입한다. 파일 추가와 실제 읽기·캐시·그리기 구현은 별도 작업으로 취급한다.
- SVG를 제작 원본으로 보관할 수는 있으나 런타임에 바로 넣지 않는다. 사용 도구, 글꼴, gradient·mask 지원, 대상 renderer에 맞는 변환 결과를 함께 검토한다. 이번 작업에서는 변환하지 않는다.
- 투명 스티커·프레임 overlay·패턴 tile의 전달 후보는 RGBA PNG다. 투명 영역을 흰색이나 검정으로 미리 채우지 않는다. 완성 JPEG 출력과 편집 가능한 투명 제작 원본은 구분한다.
- 프레임 overlay에는 실제 사용자 사진을 넣지 않고 사진 슬롯을 분리한다. 슬롯 마스크가 필요하면 해당 레이아웃의 `CollageLayoutMath` 좌표와 일치하도록 별도 계약을 정한다. 현재 앱은 외부 PNG 마스크를 읽지 않는다.
- 반복 패턴을 새로 만들 경우 tile의 네 변 연결, 반복 주기와 스케일을 정의한다. 현재 코드 패턴을 승인 없이 raster 이미지로 대체하지 않는다.
- 폰트를 새로 추가하거나 교체한다면 파일과 `AppFontCatalog`의 명시적 파일명 연결을 함께 검토한다. 출처·사용 허가·배포 고지를 기록하고, 기존 폰트명을 자동 정규화하지 않는다.

### 파일명과 위치 제안

신규 Android resource 후보는 소문자 영문·숫자·밑줄로 용도와 대상을 표현한다. 아래는 예시일 뿐이며 현재 존재하거나 자동 인식되는 파일이 아니다.

- `sticker_spring_flower_01.png`: 개별 장식 후보.
- `pattern_winter_dots_01.png`: 반복 패턴 후보.
- `frame_four_vertical_spring_overlay.png`: 특정 레이아웃용 overlay 후보.
- `ic_frame_rotate.xml`: 별도 UI 아이콘 resource 후보.

실제 배치 위치는 로딩 방식에 맞춰 정한다. density에 따라 크기가 바뀌어야 하는 UI 이미지와, 정확한 pixel 치수가 중요한 합성 자산을 같은 방식으로 취급하지 않는다. 예를 들어 향후 pixel 기준 raster를 도입하면 `res/drawable-nodpi/` 또는 명시적 `assets/` 경로를 검토할 수 있으나 현재 프레임용 파일 로더를 뜻하지 않는다.

기존 `StickerPalette.assetId`, `FrameColor.id`, `FrameLayoutId`, 폰트 ID는 JSON 인계값과 연결되어 있다. 예를 들어 `balloon2`, `faceSmiling` 같은 기존 ID는 신규 파일명 제안에 맞추어 바꾸지 않는다. 식별자 변경은 읽기 호환성을 포함한 별도 코드 변경이다.

### 해상도·알파·내보내기 제안

- 신규 bitmap 크기는 최종 그리기 영역의 pixel 크기와 사용자가 허용받은 최대 scale을 기준으로 정한다. 모든 스티커를 임의의 512px로, 모든 프레임을 1024px로 통일하는 규칙은 없다.
- 클래식 전체 overlay를 검토한다면 기준 캔버스는 1650×4920px다. 다만 문구·날짜 유무로 셀이 달라지므로 외곽 크기가 같다는 것만으로 슬롯 정합성이 보장되지는 않는다.
- 일반 배치용 전체 이미지는 CollageOutputSize와 layoutVersion을 기준으로 실제 가로/세로 치수·슬롯 좌표를 산출한다. 1024px 슬롯 목표와 16MP/8192px 상한만으로 모든 overlay의 크기를 하나로 정하지 않는다.
- 제작 후보의 색 공간은 sRGB로 통일하는 방향을 검토한다. 현재 코드가 모든 입력에 명시적으로 색 공간 변환·ICC 검증을 수행하는 것은 아니다.
- 투명 가장자리는 흰색·검정·밝은 색·어두운 색 바탕에서 확인한다. 축소 후 번짐, 색 테두리, 잘린 그림자, 불필요하게 큰 투명 여백을 점검한다. 투명 제작 PNG를 JPEG로 변환해 납품하지 않는다.
- 새 자산을 최종 JPEG에 합칠 때는 먼저 의도한 불투명 프레임 배경에 합성한다. JPEG 저장만으로 투명 영역의 최종 배경색이 원하는 색이 된다고 기대하지 않는다.
- 납품 메모에 용도, 논리 ID, 원본/출력 파일 구분, 실제 pixel 치수, 알파 유무, 색 공간, 기준 레이아웃, 출처와 사용 조건을 함께 기록한다. 자동 내보내기 도구를 새로 만들면 버전과 재생성 명령도 기록한다.

## 8. 디자인 시안과 공개 문서의 경계

최초 2026-09-10 조사 당시 `design/`는 Git 미추적 작업물이었으나, 이를 현재 Git 상태로 일반화하지 않는다. `design/mockups/print-booth-v1/`에는 HTML로 만든 화면 PNG·스타일 보드·전체 보기와 제작 원본이 있다. 해당 시안 문서는 화면 PNG를 1080×2400px로 설명하지만, 이는 Android 기기 실행 캡처나 최종 콜라주의 출력 규격이 아니다. 이번 문서 변경에 시안 파일을 함께 포함하거나 재생성하지 않는다. 새 checkout에 이 폴더가 없어도 이 가이드의 현행 코드 설명은 독립적으로 읽을 수 있다.

`.github/workflows/github-pages.yml`은 main의 `docs/**` 변경을 계기로 `docs/` 전체를 Pages artifact로 업로드한다. 이 문서 및 이 폴더에는 공개 가능한 설명만 넣는다. 개인 사진, 세션 초안, 서명 자료, 인증 정보, 개발자 절대 경로를 자산 예시나 검증 첨부물로 추가하지 않는다.

## 9. 다음 자산 변경 때의 확인 순서 제안

1. 변경할 논리 ID와 현재 사용처를 검색하고 제작 원본·런타임 자산·사용자 출력물 중 무엇을 다루는지 정한다.
2. 현행 파일과 renderer를 보존한 상태에서 새 자산 또는 변경안을 준비한다. 기존 자산의 자동 변환·이름 변경을 문서 정리 작업에 섞지 않는다.
3. 파일 치수·알파·density·경로 연결을 확인하고, 코드 벡터라면 Preview와 최종 Canvas가 같은 형상을 사용하는지 확인한다.
4. 2·4·6컷과 가로/세로/격자, 문구·날짜 ON/OFF, 밝고 어두운 배경, 최소/최대 장식 scale에서 비교한다.
5. 화면 preview뿐 아니라 실제 저장 JPEG를 열어 사진 슬롯, 장식 모양·색·위치·회전·크기, caption과 알파 경계를 확인한다.
6. 검증한 범위와 미검증 범위를 작업 기록에 남긴다. 이 가이드의 현재 규격을 바꿨다면 해당 근거 코드와 설명을 함께 갱신한다.

2026-09-22 문서 갱신에서는 현재 코드·리소스 참조·기존 검증 기록과 PNG 해시 일치를 확인했다. 픽셀 알파·런처 마스크 검사를 이번에 재실행한 것은 아니다. 실제 촬영부터 저장까지의 시각 검증, 새로운 자산 로더와 변환 파이프라인 검증은 별도 실행 기록으로 구분한다.

## 10. 2026-09-10 릴스 홍보 자산 — Film Story v1

기준은 `4527a05`의 앱 1.3 (4)와 기존 스토어 제작물이다. `design/reels/film-story-v1/`에 30초 홍보 영상 2종, 1080×1920 JPEG 커버, 게시글/편집용 SRT/업로드 안내, 제작 코드와 검증 자료를 추가했다. 앱 내 리소스·아이콘·UI·Manifest·Gradle은 변경하지 않았다.

- `deliverables/Pocket4Cut-Reels-30s.mp4`: 1080×1920 / 9:16 / 30 fps / 900프레임 / 30초. H.264 High, yuv420p, BT.709, faststart. AAC 48 kHz stereo 음악+효과음.
- `deliverables/Pocket4Cut-Reels-30s-NoMusic.mp4`: 동일한 영상에 합성 효과음만 포함. 새 음악 추가용이며 완전 무음본은 아니다.
- `deliverables/Pocket4Cut-Reels-Cover.jpg`: 1080×1920 불투명 JPEG. 인화지·앱 이름·큰 한글 타이포그래피로 구성한 릴스 커버이며 앱 아이콘 교체본이 아니다.
- `audio/`: 외부 음원·샘플 없이 절차적으로 합성한 30초 / 48 kHz / stereo / PCM16 WAV 세 파일. 최종 MP4의 음악 포함본은 −15.3 LUFS / −2.1 dBTP로 측정했다.

새 사진/로고를 생성하지 않았다. Film Strip v4 아이콘, 기존 프로덕션 Compose 호스트 렌더, CollageRenderer의 네 컷 JPEG, 가상 성인 생성 사진을 재사용했다. 사진 이동·강조·전환은 영상 편집 효과이며 실제 디바이스 연속 녹화가 아니다. 사용자 촬영 사진이나 실제 고객 후기로 소개하지 않는다. 프레임 색 예시는 필터도 적용된 스타일링 조합이다.

재료의 경로·치수·해시는 `verification/input-manifest.json`, 편집 판단·트렌드 참고 링크는 `source/CREATIVE-BRIEF.md`, 음악 제작/권리 한계는 `source/AUDIO-NOTES.md`, 실행 방법과 도구 출처는 해당 폴더 README에 기록했다. 기존 이미지의 생성 프롬프트는 원래 스토어 자산 패키지에 남겨 두었다. FFmpeg 실행 파일과 한글 폰트 파일을 앱 또는 납품 ZIP에 포함하지 않는다.

두 MP4를 전체 디코딩하고 규격·길이·동기·오디오 true peak·예상치 못한 검은 구간을 검사했다. 주요 자막 영역, 시안/최종 디코딩 8장면, 시작/끝 프레임, 커버를 시각 검토했다. 기본 색상 태그가 누락된 최초 출력은 납품하지 않고 태그 설정을 보완해 재출력했다. 실제 휴대전화 음량, Instagram 업로드 자르기/심사, 앱 E2E는 미검증이다.

## 11. 2026-09-10 릴스 홍보 자산 — Narrator v2

기준은 `c33b83a`이며 `design/reels/narrator-v2/`에 기존 v1과 별개의 음성 중심 광고를 추가했다. 앱 및 기존 이미지 원본은 변경하지 않았다. 새 사진/아이콘 생성 없이 기존 Compose 호스트 렌더·CollageRenderer 결과·Film Strip v4 아이콘·AI 생성 가상 성인 사진으로 편집했다.

- `deliverables/Pocket4Cut-Reels-Male-Narration-30s.mp4`: 1080×1920 / 9:16 / 30 fps / 900프레임 / 30초. H.264 High/yuv420p/BT.709/faststart, AAC stereo 48 kHz. 한국어 남성 AI 나레이션만 포함하며 음악·효과음 없음.
- `deliverables/Pocket4Cut-Male-Narration.mp3`: 동일한 30초 음성 트랙, MP3 192 kbps 설정. 시작 여유 및 끝 무음 포함.
- `deliverables/Pocket4Cut-Narrator-Cover.jpg`: 1080×1920 불투명 JPEG. 큰 필름 사진과 한글 제목, 광고·AI 생성 예시 고지. 앱 아이콘 교체본이 아니다.
- `audio/narration-original.wav`: 공식 Qwen3-TTS VoiceDesign이 생성한 25.953750초 / 24 kHz / mono / PCM16. 원본 음성 및 텍스트 음색 지시를 보존했다.
- `audio/narration-master-30s.wav`: 30초 / 48 kHz / stereo / PCM24. 말속도 1.0배, 0.15초 시작 여유 및 끝 무음. 최종 AAC −15.8 LUFS / −2.7 dBTP 측정.
- `visual-stills/`: 실제 대사에 맞춘 15구간의 두 시점 JPEG와 모아보기. `verification/`에는 최종 MP4에서 추출한 15장면/시작/끝 JPEG 및 검증 JSON이 있다. 스틸은 영상 편집 검사용이며 앱 실기기 스크린샷으로 소개하지 않는다.

음성 생성은 공식 [Qwen3-TTS](https://github.com/QwenLM/Qwen3-TTS)의 [VoiceDesign 모델](https://huggingface.co/Qwen/Qwen3-TTS-12Hz-1.7B-VoiceDesign)과 [공개 데모](https://huggingface.co/spaces/Qwen/Qwen3-TTS)를 사용했다. 한국어 원본 남성 음색을 텍스트로 지정했고 참조 음성/실존 인물 복제를 사용하지 않았다. 모델/데모의 Apache-2.0 표기는 생성물 독점권·광고 심사 통과의 보증이 아니다. 원본 요청은 `source/voice-request.json`, 출처·후처리·한계는 `source/VOICE-NOTES.md`에 기록했다.

실제 발화의 자동 전사 119개 한글 음절이 대본과 일치했다. 흑백 감상과 저장/공유의 결과 연속성을 검토해 수정했다. 전체 MP4 디코딩·규격·길이·동기·음량·검은 구간 검사와 7개 ZIP 항목 원본 해시 비교를 통과했다. 생성 예시 고지를 유지하며 실제 청취·Instagram 업로드/자르기·스토어 배포 상태·앱 E2E는 별도 확인이 필요하다.

## 12. 2026-09-10 릴스 홍보 자산 — Reference v3

기준은 `f79b2fd`이며 `design/reels/reference-v3/`에 별도 광고를 추가했다. 기존 앱·아이콘·v1/v2 영상은 보존했다. 새 사진·프레임·로고 생성 없이 기존 앱 코드 기반 Compose 호스트 렌더·CollageRenderer 결과·Film Strip v4 아이콘·가상 성인 사진을 편집했다.

- `deliverables/Pocket4Cut-Reels-Reference-Voice.mp4`: 23초 / 1080×1920 / 9:16 / 30 fps / 690프레임. H.264 High/yuv420p/BT.709/faststart + AAC stereo 48 kHz. 음악·효과음 추가 없음. 최종 음량 −16.0 LUFS / −3.4 dBTP.
- `deliverables/Pocket4Cut-Reference-Narration.mp3`: 23초 / 48 kHz / stereo / MP3 192 kbps 설정. 동일한 내레이션 마스터에서 내보낸다.
- `deliverables/Pocket4Cut-Reference-Cover.jpg`: 1080×1920 불투명 JPEG. “내 폰이 / 네 컷 사진관으로”, 실제 브랜드와 큰 필름 사진을 배치한 릴스 커버다. 앱 아이콘 교체본이 아니다.
- `audio/narration-original.wav`: 실제 6초 참조를 조건으로 로컬 Qwen3-TTS 1.7B Base가 생성한 새 대본. 20.160초 / 24 kHz / mono / 32-bit float. `narration-master.wav`는 배속 없이 시작 0.12초와 후반 여유를 추가한 23초 / 48 kHz / stereo / PCM24다.
- `visual-stills/`: 14구간의 두 시점 JPEG와 모아보기, 자막 경계·자산 해시·렌더 기록. `verification/`에는 실제 인코딩 MP4의 14장면/시작/끝 프레임, 음성·파일·패키지 검증이 있다.

사용자 요청에 따라 자체 앱 소개 흐름과 친근한 존댓말을 사용하고, 참조 광고의 계정/원본 영상/원본 대본을 본편·커버·ZIP에 넣지 않았다. 초기 출력의 상·하단 설명 표시는 후속 요청으로 제거했으며, 현재 본편·커버·스틸·ZIP에 반영했다. 소개 자막과 음성은 그대로이고 제작 도구·재료의 사실 기록은 별도로 보존한다. 사용자 원본 영상과 음성은 ignored 작업 영역에만 보존한다.

색상은 종이색 `#F0ECE3`, 먹색 `#252520`, 적색 `#C83D2D` 중심이다. 생성 프롬프트가 필요한 새 사진 작업은 없었다. 새 음성 대본은 `source/narration-script.json`, 생성 설정·공식 Qwen/whisper.cpp 출처·Apache-2.0 모델 표기와 권리/검수 한계는 `source/VOICE-NOTES.md`, 재사용 이미지 경로·치수·해시는 `visual-stills/input-manifest.json`을 참조한다. 원본 이미지의 생성 프롬프트는 기존 스토어 제작물에 남겨 두었다.

실제 전사/DTW로 대본 내용을 확인했고 최종 파일의 전체 디코딩·규격·동기·음량·검은 구간을 검사했다. 선택 프레임·커버 시각 검수에서 흑백→저장/공유 연속성을 확인하고 CTA 상단 표기 겹침을 정리했다. 음색 완전 동일성·자연스러움의 직접 청취, 기기 UI/업로드 심사, 실제 Play 상태는 미검증이다.

## 13. 2026-09-22 — Paper Seasons 계절 프레임 교체

- 기준: `d249a01`에서 시작한 `codex/seasonal-frames` 변경. 다른 색상 프레임·사용자 스티커·앱 아이콘·촬영 사진은 교체하지 않는다.
- 제작: 내장 전용 이미지 생성 도구로 시즌마다 6개 모티프를 새로 생성했다. 도구는 모델명/품질 선택 옵션을 노출하지 않으므로 특정 최고 모델을 선택했다고 주장하지 않는다. 사진 슬롯·문구·브랜드·안전 영역은 생성 이미지에 맡기지 않고 기존 Android 기하를 사용한다.
- 봄: 벚꽃, 튤립, 리본, 나비, 딸기, 꽃. 종이 `#FFF6F1`, 잉크 `#9F3557`, 옅은 체크 패턴.
- 여름: 파라솔, 튜브, 조개, 레몬, 갈매기, 해. 종이 `#EFF8F7`, 잉크 `#245A77`, 가는 세로선.
- 가을: 은행/단풍잎, 커피, 도토리, 목도리, 책, 배. 종이 `#F5E9D5`, 잉크 `#78452D`, 가는 노트선.
- 겨울: 장갑, 눈사람, 솔가지, 코코아, 눈꽃, 리본. 종이 `#EFF2F5`, 잉크 `#3D556B`, 옅은 격자.
- 원본: `design/seasonal-frames/paper-seasons-v1/source/*-atlas.png` 4개, 각 1536×1024 RGBA. 원본 파일과 생성 provenance를 보존했다. 정확한 요청은 같은 폴더 `GENERATION-PROMPTS.md`에 있다.
- 앱: `app/src/main/assets/seasonal/{spring,summer,autumn,winter}.png` 4개, 각 1536×1024 RGBA/sRGB, 512px 셀 6개, 셀 경계 최소 14px 실제 투명 여백. crop/trim/downscale/atlas 포장만 했으며 원본 장식을 재도색하지 않았다. 해시·sourceRect·알파 검사는 `asset-validation.json`에 있다.
- 원본의 중심 알파는 최고 254이며 임의로 불투명화하지 않았다. 런타임 리샘플링 결과 일부 픽셀은 255가 될 수 있다. 밝고 어두운 배경에서 투명 가장자리와 셀 간 누출을 확인했다.
- `SeasonalStickerArt`는 IO 디코드·최대 2개 캐시를 사용한다. atlas 1장의 ARGB 디코드 크기는 약 6MiB이고 캐시 참조는 최대 약 12MiB다. 화면/진행 중 저장이 별도로 참조하는 이미지와 최종 콜라주 메모리는 이 상한에 포함되지 않는다. 캐시에서 빠진 Bitmap을 수동 recycle하지 않는다.
- `CollagePreview`와 최종 `DetailEditViewModel`은 같은 시즌 Sheet를 `drawScene`에 전달한다. 로딩 중/실패 시 상태를 표시하고 재시도한다. 잘못된 시즌 또는 누락된 Sheet를 단색 결과로 조용히 저장하지 않는다.
- 측면 장식은 회전을 포함한 전체 경계가 종이 여백 안에 들어오도록 제한했다. 사진 슬롯·브랜드·문구/날짜 영역은 추가 clip으로 보호한다. 기존 큰 모서리·점선 테두리·계절 그라데이션을 제거했다.
- 삭제: 구형 `Spring/Summer/Autumn/WinterFrameVectorDecor.kt`와 전용 `SeasonDecorAnchors.kt`. Git 이력에는 남아 있으며 사용자 완료본을 삭제하거나 다시 렌더하지 않는다.
- 호환: `sourceSeason` 문자열과 레이아웃 ID/버전은 유지한다. 구형 6컷의 레이아웃 선택·색/계절/커스텀 선택·일반 편집에서도 같은 버전을 전달한다.
- 전달: 현재 8배치×4계절 32종, 문구 공간 변형 32종, 구형 6컷 호환 8종으로 **투명 사진창 PNG 72장**을 출력한다. 가상 성인 사진을 넣은 예시 32장은 실제 앱과 같은 JPEG 품질 98로 제공한다. 실제 고객/사용자 사진은 사용하지 않았다.
- 재생성: `source/prepare-assets.cjs` → QA 계측 exporter(`seasonalExport=true`) → `source/package-deliverables.cjs`. exporter는 QA의 AGP `additionalTestOutputDir`에 쓰며 Gradle이 PC로 복사한다. 일반 테스트에서는 대용량 export를 건너뛴다.
- 앱의 최종 저장 형식은 JPEG다. 투명 프레임 PNG는 디자인 납품본이지 새로운 앱 PNG 저장 기능이 아니다. 정확한 파일별 치수·사진 칸 좌표·문구 공간·배치 버전은 납품 `manifest.json`을 따른다.
- Instagram: 실제 최종 4계절 클래식 JPEG를 참고하여 전용 이미지 도구로 종이 포스터 시안을 생성했다. `source/instagram-promo-master.png`는 1122×1402 RGB 원본, `deliverables/instagram-promo.png`는 1080×1350 불투명 sRGB PNG다. 가상 성인 사진을 사용하며 실제 앱 캡처와 구분한다. 정확한 요청은 `source/INSTAGRAM-PROMPT.md`에 있다. 플랫폼 게시를 실행하지 않았다.
- 패키지: `deliverables/pocket4cut-paper-seasons-v1.zip`은 103,096,843 bytes, 113개 파일(프레임 72 + 예시 32 + 모아보기 6 + manifest/README/홍보 이미지)이다. 다시 열어 전 항목의 길이·SHA-256을 대조했다. 검증 기록은 `deliverables/package-validation.json`, 별도 실제 Compose 화면은 `verification/autumn-picker-compose.png`다.
- 권리/출처: 전용 생성 도구로 만든 독창적 계절 일러스트이며 타사 캐릭터·로고를 참조하지 않았다. 사람이 직접 그렸다고 표시하지 않는다. 생성물의 독점권·인쇄 품질·플랫폼 심사 승인은 보장하지 않는다. 실제 수행한 검증과 남은 범위는 WORKLOG에 기록한다.

## 14. 2026-09-22 — 전체 레이아웃 Instagram 포스터 v2

- 기준: `0e5212e`, `codex/seasonal-frames`, 시작 작업 트리 clean. 사용자가 `overview-all-seasons.png`를 지정하여 새 홍보 이미지 1장을 요청했다.
- 제작: 내장 전용 이미지 생성 도구에 기존 3840×2920 모아보기를 제품 참조로 전달했다. 따뜻한 종이 바탕, 사계절 컬러 구분, 4행×8개 프레임, 큰 한글 제목의 컬렉션형 이미지다. 모델명·품질 선택 옵션은 노출되지 않는다.
- 파일: `design/seasonal-frames/paper-seasons-v1/source/instagram-layouts-promo-v2-master.png`(1122×1402, RGB/sRGB, 생성 원본), `deliverables/instagram-layouts-promo-v2.png`(1080×1350, 불투명 RGB/sRGB PNG, 2,045,862 bytes).
- 주 문구: “계절도, 프레임도 / 내 취향대로.”, “사계절 × 8가지 레이아웃, 총 32종”, “2컷 · 4컷 · 6컷으로 오늘을 남겨보세요.”. 32종은 네 계절과 여덟 배치의 조합 수이며 서로 다른 계절 일러스트가 32세트라는 뜻이 아니다.
- 출처·검증: `source/INSTAGRAM-LAYOUTS-V2-PROMPT.md`에 실제 생성 요청, `source/export-instagram-layouts-v2.cjs`에 크기/색 공간 변환, `verification/instagram-layouts-v2-validation.json`에 참조·원본·납품 이미지 규격과 SHA-256을 기록했다. 사진창은 비워 두었으며 사용자 사진은 사용하지 않았다.
- 구분: 생성 홍보 시안이므로 작은 브랜드 글자나 장식은 Android 렌더와 픽셀 단위로 같지 않다. 정확한 프레임 PNG와 overview는 기존 파일이 기준이다. 앱 코드·기존 프레임·첫 홍보 이미지·113항목 ZIP은 변경하지 않았으며 새 이미지는 별도 다운로드다. 실제 Instagram 게시/자동 자르기/압축 검사는 하지 않았다.

## 15. 2026-09-22 — Paper Seasons 릴스 v4

- 기준: `6d0c151`, `codex/seasonal-frames`, 시작 작업 트리 clean. 사용자는 마지막 완성된 `reference-v3` 릴스의 틀을 유지하면서 새 사계절 프레임을 소개하도록 요청했다.
- 결과: `design/reels/seasonal-v4/deliverables/Pocket4Cut-Reels-Seasons-Voice.mp4`, **23초 / 1080×1920 / 30 fps / 690프레임**, H.264 High/yuv420p/BT.709/faststart + AAC 48 kHz stereo. 최종 음량 −16.0 LUFS / −2.4 dBTP이며 음악·효과음은 없다.
- 커버: `Pocket4Cut-Seasons-Cover.jpg`, **1080×1920 불투명 JPEG**, “사계절을 / 네 컷에 담아요”. 동일한 브랜드 아이콘·인화지·색상·한글 타이포그래피를 재사용한 릴스 커버이며 앱 아이콘 교체본이 아니다.
- 디자인: 기존 종이색 `#F0ECE3`, 먹색 `#252520`, 적색 `#C83D2D`, 기울어진 인화사진·짧은 자막·미세한 확대·마지막 검색 안내를 유지했다. 계절별 전체 프레임과 장식 확대, 대표 2·4·6컷, 저장/공유를 보여 준다. 실제 지원하는 8배치는 자막으로 안내하며 영상에 모든 32조합이 등장한다고 주장하지 않는다.
- 재료: Paper Seasons의 실제 Android CollageRenderer JPEG와 기존 가상 성인 사진·브랜드 아이콘·결과 UI의 버튼 영역을 재사용했다. 새 이미지 생성 프롬프트는 없으며 기존 일러스트의 프롬프트·출처는 Paper Seasons 원본에 보존한다. 재료 경로·치수·SHA-256은 `visual-stills/input-manifest.json`에 있다. 편집 영상이지 실기기 연속 녹화나 실제 사용자 후기가 아니다.
- 음성: 이전에 사용을 승인받은 동일한 참조 및 로컬 Qwen3-TTS 1.7B Base 설정으로 새 존댓말 대본을 생성했다. 새 설치·다운로드·외부 음성 전송은 하지 않았다. 원본은 20.960초 / 24 kHz / mono / float32 WAV, 마스터는 배속 없이 시작 0.12초와 끝 여유를 포함한 23초 / 48 kHz / stereo / PCM24 WAV다. 납품 MP3도 23초다. 모델·참조와 권리/청취 한계는 `source/VOICE-NOTES.md`에 기록했다. 사용자 참조 파일은 ignored 영역에만 보존하며 Git/ZIP에 넣지 않는다.
- 검증: 첫 음성의 ASR 불일치를 감지해 렌더를 차단하고 두 표현을 간결하게 바꾼 뒤 재생성했다. 최종 실제 ASR 136자 정규화 일치·편집 거리 0, 자막 테스트 10/10, 스틸 28장·텍스트 경계 41건·프레임 배치 경계 12건을 확인했다. 최종 MP4 전체 디코딩·규격·길이 동기·음량·검은 구간 검사를 통과했고 실제 인코딩 프레임과 커버를 시각 검토했다.
- 전달: 본편·커버·음성·SRT·대본·게시글·안내 7개 파일의 ZIP은 10,939,270 bytes이며, 재개봉 후 각 항목 해시가 원본과 일치했다. 제작 코드·WAV·검증 자료와 패키지 README는 `design/reels/seasonal-v4/`에 분리했다. 앱 코드·런타임 자산·이전 릴스·계절 이미지 ZIP은 수정하지 않았다.
- 미검증: 자연스러움·발음·완전히 같은 음색의 직접 청취, 실제 휴대전화/Instagram 자르기·압축·게시·심사, Play 검색/배포 상태는 확인하지 않았다. 게시 전에 배포 앱에 새 프레임이 적용됐는지 확인한다. 앱 변경이 없어 이번에 Android 빌드·계측을 재실행하지 않았다.
