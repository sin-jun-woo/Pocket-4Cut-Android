# Pocket 4Cut 이미지·그래픽 자산 가이드

> 확인일: 2026-09-10 · 기준: 기존 HEAD `95140a0`와 미커밋 로컬 코드 변경을 포함한 작업 트리.
> 최초 문서 조사에서는 자산을 변경하지 않았다. 이후 런처 아이콘 변경은 바로 아래 갱신 기록을 따른다.

## 2026-09-10 최신 갱신 — Film Strip v4 런처/스토어 자산

HEAD `ac42903`과 미커밋 UI 변경을 유지한 작업 트리에서 사용자가 네 컷/필름 사진 중심 아이콘 및 피처 그래픽 제작·적용을 요청했다. 후속 요청에 따라 인화지 하단에 소문자 `pocket4cut`을 추가했다. 아래 Print Booth v1과 1~2절의 자산 규격 설명은 각각 이전 시점 기록이다.

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

HEAD `ac42903` 및 기존 미커밋 UI 변경을 포함한 작업 트리에서, 사용자의 앱 아이콘 제작·적용 요청으로 런처 자산을 교체했다. 아래 1~2절의 이전 그라데이션·동일 monochrome 설명은 교체 전 조사 기록이다. 프레임·저장 JPEG 렌더링 계약은 이 작업에서 변경하지 않았다.

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

## 1. 현행 자산의 구분

현재 앱의 프레임과 장식은 대부분 파일 이미지가 아닌 Kotlin 코드로 그린다. 아래 파일 경로는 저장소 루트 기준이며, 개발자 컴퓨터의 절대 경로나 사용자 사진을 뜻하지 않는다.

- **프레임 배치·색:** `app/src/main/java/com/pocket4cut/frame/FrameLayouts.kt`, `FrameTheme.kt`, `FrameColors.kt`에 정의되어 있다. 프레임 전체를 담은 PNG 묶음을 읽는 구조가 아니다.
- **계절 배경·패턴:** 같은 `frame/` 아래 `SeasonHTMLFrameStyle.kt`, `SeasonBackgroundFrameFactory.kt`와 `rendering/*FrameVectorDecor.kt`, `SeasonDecorAnchors.kt`를 사용한다. 이름에 HTML이 있어도 HTML/WebView를 실행하지 않는다. 배경색, 그라데이션, 곡선·도형·글자를 Compose와 Android Canvas로 그린다.
- **사용자 장식:** `CustomFrameDecoration.kt`의 Text/Emoji/Sticker 모델이다. 텍스트와 이모지는 문자열이며, 스티커 25종은 `StickerPalette.kt`에 연결된 Compose Material `ImageVector`다. 스티커 PNG/SVG 파일을 자동 검색하는 로더는 없다.
- **앱 UI 아이콘:** 화면과 디자인 시스템 컴포넌트가 주로 Material `Icons`를 직접 사용한다. UI 아이콘과 최종 콜라주에 들어가는 장식은 용도와 렌더 경로가 다르다.
- **런처 아이콘:** `app/src/main/res/`의 density별 PNG, adaptive-icon XML, 배경 shape XML을 사용한다. 실제 파일 규격은 아래에 기록했다.
- **브랜드 SVG:** `app/src/main/assets/branding/pocket_4cut_app_icon.svg` 한 개가 있다. SVG의 `width`/`height`는 1024, `viewBox`는 `0 0 256 256`이다. 현재 앱 코드에서 이 SVG를 읽는 호출이나 SVG decoder 의존성은 확인되지 않았다. 이 파일의 존재는 런타임 SVG 지원을 의미하지 않는다.
- **글꼴:** `app/src/main/assets/fonts/`에 TTF 13개가 있다. `core/util/AppFontCatalog.kt`가 파일명을 정확히 지정해 Compose `FontFamily`와 Android `Typeface`를 읽는다. 시스템 기본 글꼴도 선택할 수 있다.
- **실제 촬영본·완성 JPEG:** 실행 중 앱 전용 저장소에 생성한다. 앱에 번들된 디자인 자산이 아니다.

브랜드 SVG에는 글자와 이모지 요소도 있다. 어떤 도구·글꼴로 PNG를 재생성했는지 확인하지 않고 기존 런처 PNG를 SVG의 자동 변환 결과로 단정하지 않는다. 현재 저장소에서 검증된 자동 재생성 경로는 확인하지 못했다.

## 2. 실제 런처 PNG 규격과 알파

PNG 15개를 직접 읽어 IHDR의 치수·bit depth·color type을 확인하고, 디코딩한 모든 픽셀의 알파 최솟값·최댓값을 검사했다.

`app/src/main/res/mipmap-<density>/`에는 `ic_launcher.png`와 `ic_launcher_round.png`가 각각 있다. 두 이름 모두 같은 density에서 아래 치수를 사용한다.

- `mipmap-mdpi`: 각각 **48×48px**.
- `mipmap-hdpi`: 각각 **72×72px**.
- `mipmap-xhdpi`: 각각 **96×96px**.
- `mipmap-xxhdpi`: 각각 **144×144px**.
- `mipmap-xxxhdpi`: 각각 **192×192px**.

`app/src/main/res/drawable-<density>/ic_launcher_foreground.png`의 치수는 다음과 같다.

- `drawable-mdpi`: **108×108px**.
- `drawable-hdpi`: **162×162px**.
- `drawable-xhdpi`: **216×216px**.
- `drawable-xxhdpi`: **324×324px**.
- `drawable-xxxhdpi`: **432×432px**.

15개 모두 **PNG color type 6, 채널당 8bit RGBA**다. 실제 픽셀 검사에서도 각 파일에 alpha=0과 alpha=255가 모두 존재했다. 즉 알파 채널만 선언된 불투명 파일이 아니라 완전 투명 영역을 포함한다. 이 검사는 이미지 품질·런처 마스크·테마 아이콘의 실기기 표시 검증을 대체하지 않는다.

`res/mipmap-anydpi/ic_launcher.xml`과 `ic_launcher_round.xml`은 모두 다음 리소스를 참조한다.

- background: `@drawable/ic_launcher_background`.
- foreground: `@drawable/ic_launcher_foreground`.
- monochrome: 동일한 `@drawable/ic_launcher_foreground`.

배경은 `res/drawable/ic_launcher_background.xml`의 불투명 선형 그라데이션 shape다. 전경 PNG의 투명 영역을 통해 이 배경이 보이는 구조다. 현재 monochrome도 같은 전경을 재사용하므로 별도 단색 원본이 있다고 가정하지 않는다.

## 3. 프레임 기하와 최종 해상도

실제 좌표의 기준은 `frame/CollageLayoutMath.kt`다. 참고 PNG에서 셀 위치를 눈대중으로 추출하는 방식보다 코드의 캔버스·셀·문구 영역을 먼저 확인해야 한다.

- 레이아웃 ID는 `TWO_VERTICAL`, `TWO_HORIZONTAL`, `FOUR_VERTICAL`, `FOUR_GRID`, `FOUR_HORIZONTAL`, `SIX_GRID_2X3`, `SIX_GRID_3X2`, `SIX_COLLAGE`의 8개다.
- **클래식 `FOUR_VERTICAL`의 최종 크기는 1650×4920px 고정**이다. 문구 또는 날짜가 있으면 고정 높이 안에서 사진 셀 높이가 줄어든다.
- **그 외 활성 최종 출력은 시스템 화면 폭을 720~2160px로 제한**한다. `core/util/CollageExportMetrics.kt`에서 화면 폭이 320px 미만으로 보고되면 먼저 1170px를 대체값으로 사용한다. 일반 출력은 기기별 해상도가 같다고 보장되지 않는다.
- 일반 레이아웃의 높이는 행·열, 셀 비율, 여백·간격, 브랜드와 문구/날짜 영역으로 계산한다. 문구 또는 날짜가 생기면 전체 높이가 증가한다. 모든 프레임에 적용할 고정 높이 하나는 없다.
- `FrameStyle.padding/gap`은 현재 실제 기하 계산에 쓰이지 않는다. `FrameTheme.outerPadding/cellSpacing`을 사용하고, 2컷 가로만 20/10 기준값을 별도로 적용한다. 현재 `SIX_COLLAGE`와 `SIX_GRID_3X2`는 실제 기하가 같다.
- 사진은 셀에 중앙 aspect-fill로 그려 가장자리가 잘릴 수 있다. 원본의 모든 영역이 결과에 들어가는 fit 방식이 아니다.

활성 출력은 `presentation/detailEdit/DetailEditViewModel.kt`에서 `CollageRenderer.Input`을 만든다. `Constants.RESULT_IMAGE_MAX_WIDTH = 2560`은 이전 `presentation/edit/CollageFinalize.kt` 경로에서 쓰는 값이며 현재 공통 출력 규격으로 사용하면 안 된다.

계절 벡터의 **600×1800**은 그리기용 기준 좌표 공간이다. 현재 PNG 파일 크기나 최종 출력 해상도가 아니다. 봄·여름은 균일 배율과 가운데 정렬을, 가을·겨울은 가로·세로 독립 배율을 사용한다. 가로형·격자형 프레임에서 동일한 모양 비율이 보장된다고 가정하지 않는다. 반복 점·눈꽃 등의 패턴도 코드로 그리며, 현재 재사용하는 raster tile 파일은 없다.

## 4. 사진 파일명·보관·내보내기의 현행 규칙

`data/storage/FileImageStorage.kt`가 사용하는 논리적 경로는 다음과 같다. `<sessionId>`는 촬영 시작 때 생성한 UUID다.

```text
getExternalFilesDir(Pictures)/Pocket4Cut/
  captures/<sessionId>/cap_01.jpg
  captures/<sessionId>/cap_02.jpg
  ...
  results/<sessionId>_result.jpg
```

- 촬영 파일 번호는 1부터 시작하며 두 자리로 채운다. 예: `cap_01.jpg`, `cap_08.jpg`, `cap_10.jpg`.
- 촬영 목록은 파일명순으로 읽고 선택 인덱스가 이 목록을 가리킨다. 기존 촬영 파일명을 임의로 바꾸거나 폴더에 무관한 파일을 섞으면 대응 관계가 깨질 수 있다. 현재 목록 로더에는 이미지 확장자 필터도 없다.
- 매 컷 촬영 파일 저장 직후 `BitmapDecoding.rewriteJpegMaxLongEdge()`가 EXIF 회전·반사를 실제 픽셀에 반영해 **긴 변 최대 2048px, JPEG 품질 92**로 덮어쓴다. 이것이 정상 완료된 세션 원본 규격이다. 현재 호출부는 실패 반환값을 확인하지 않으므로 모든 파일이 반드시 정규화되었다고 단정할 수는 없다.
- 기본 편집은 `decodeSampled(..., 720)`, 프레임 선택은 512, 상세 편집 진입은 2048, 최종 상세 처리는 3072를 요청한다. 이 요청치는 bitmap의 긴 변 상한이 아니다. 양쪽 치수에 따른 sampling 때문에 더 큰 bitmap이 반환될 수 있다. 3072로 다시 읽어도 이미 2048로 축소된 원본의 세부 정보가 복원되지는 않는다.
- 결과는 **JPEG 품질 98**로 `<sessionId>_result.jpg`에 저장한다. 동일 세션의 재생성은 같은 경로를 사용한다. 결과는 PNG나 투명 이미지가 아니다.
- `presentation/result/ResultScreen.kt`는 완성 JPEG를 공용 사진첩으로 복사한다. API29+에서는 MediaStore의 `Pictures/Pocket4Cut` 경로와 `image/jpeg` MIME을 지정한다. 이 단계는 기존 JPEG의 byte copy이며 재렌더링·재압축 과정이 아니다.
- 공유도 완성 파일을 FileProvider URI로 전달하는 방식이다. 앱 전용 결과 파일과 공용 사진첩 사본은 별개다.

촬영본·완성본·세션 JSON은 사용자의 실행 데이터다. 제작용 자산 폴더나 문서 예시에 실제 사진, 실제 세션 UUID, 기기 절대 경로를 복사하지 않는다.

## 5. 색·투명도·장식 변환의 현행 계약

중간 합성 bitmap은 `ARGB_8888`이므로 메모리 안에서는 알파 채널을 가진다. 그러나 최종 JPEG는 알파를 보존하지 않는다. 투명 PNG를 별도 완성본으로 내보내는 사용자 기능도 현재 없다.

`CustomFrameDecoration.position`은 캔버스 전체 기준 정규화 중심 좌표 `(x, y)`다. `scale`은 크기 배율이고 저장 필드는 `rotationRadians`다. 최종 장식 크기의 기준은 `min(canvasWidth, canvasHeight)`이며, 텍스트·이모지·스티커가 각각 다른 비율을 사용한다. 사진 셀 내부 좌표와 혼동하지 않는다.

스티커 `colorRGB`, caption 색, `ColorRGB` 유틸은 24bit RGB를 사용하고 렌더 때 불투명 alpha를 붙인다. `textColorARGB`라는 이름의 텍스트 장식 값도 현재 Renderer는 하위 RGB만 취하고 alpha=255로 그린다. 따라서 모델에 ARGB 값이 있다고 해서 장식의 사용자 지정 반투명도를 지원한다고 설명하면 안 된다. 계절 벡터에 코드로 지정된 Paint alpha는 별도다.

현재 코드에서는 프레임 색 45개와 카탈로그 색을 제공한다. hologram/sunset/aurora의 `gradientBrush`는 팔레트 표시 일부에만 쓰이며 실제 프레임은 단색 `color`로 처리한다. 사진용 필터는 ORIGINAL/SOFT/FILM/BW의 ColorMatrix 처리이며 프레임·장식 전체에 동일 필터를 씌우는 구조는 아니다.

## 6. 자산 제작·교체 전에 알아야 할 구현 제약

아래는 확인된 코드상의 차이다. 새 이미지를 만들어도 이 차이가 자동으로 해결되지는 않으며, 이번 문서 작성에서는 수정하지 않았다.

1. `CollagePreview`는 스티커 `ImageVector`를 보여 주지만 `CollageRenderer`는 스티커의 `displayName`을 글자로 그린다. 현재 스티커를 완성 JPEG와 동일한 아이콘으로 내보낸다고 보장할 수 없다.
2. 일반 미리보기는 장식 크기와 레이어에 scale을 중복 적용한다. 커스텀 편집기는 고정 sp/dp, 일반 미리보기는 캔버스 폭, 최종 출력은 짧은 변을 크기 기준으로 사용한다.
3. 커스텀 편집 제스처의 회전값은 degree인데 현재 `rotationRadians`에 그대로 더한다. 디자인 파일에 보정 각도를 넣어 이 코드 오류를 상쇄하는 방식은 기준을 더 혼란스럽게 한다.
4. `CollageLayoutMath.computeForPreview()`가 문구 존재 표시로 공백을 넘겨 문구 영역을 만들지 못한다. 최종 출력에는 실제 문구·날짜 영역이 생기므로 사진 셀 또는 캔버스 높이가 달라진다.
5. Renderer API에는 `overrideBackgroundImage: Bitmap?`가 있지만 활성 제작 흐름에 이미지 파일 배경 선택 기능이 연결되어 있지 않다. Preview에도 인자는 있으나 실제 배경 bitmap을 그리지 않는다. `assets/`에 프레임 PNG나 SVG를 추가하기만 하면 앱이 사용하는 구조가 아니다.
6. 계절 점선의 그리기 순서, footer의 pixel offset, 계절 anchor 변환에도 preview/export 차이가 있다. 프레임은 최종 저장 JPEG까지 비교해야 한다.

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
- 일반 레이아웃용 전체 이미지는 최대 폭 2160px만 알고 제작하지 않는다. 대상 레이아웃·문구 상태별 실제 높이와 셀 좌표를 먼저 산출한다. device-dependent 출력 폭을 바꾸는 작업은 별도 렌더 정책 변경이다.
- 제작 후보의 색 공간은 sRGB로 통일하는 방향을 검토한다. 현재 코드가 모든 입력에 명시적으로 색 공간 변환·ICC 검증을 수행하는 것은 아니다.
- 투명 가장자리는 흰색·검정·밝은 색·어두운 색 바탕에서 확인한다. 축소 후 번짐, 색 테두리, 잘린 그림자, 불필요하게 큰 투명 여백을 점검한다. 투명 제작 PNG를 JPEG로 변환해 납품하지 않는다.
- 새 자산을 최종 JPEG에 합칠 때는 먼저 의도한 불투명 프레임 배경에 합성한다. JPEG 저장만으로 투명 영역의 최종 배경색이 원하는 색이 된다고 기대하지 않는다.
- 납품 메모에 용도, 논리 ID, 원본/출력 파일 구분, 실제 pixel 치수, 알파 유무, 색 공간, 기준 레이아웃, 출처와 사용 조건을 함께 기록한다. 자동 내보내기 도구를 새로 만들면 버전과 재생성 명령도 기록한다.

## 8. 디자인 시안과 공개 문서의 경계

확인 시점에 `design/`는 Git 미추적 별도 작업물이었다. `design/mockups/print-booth-v1/`에는 HTML로 만든 화면 PNG·스타일 보드·전체 보기와 제작 원본이 있다. 해당 시안 문서는 화면 PNG를 1080×2400px로 설명하지만, 이는 Android 기기 실행 캡처나 최종 콜라주의 출력 규격이 아니다. 이번 문서 변경에 시안 파일을 함께 포함하거나 재생성하지 않는다. 새 checkout에 이 폴더가 없어도 이 가이드의 현행 코드 설명은 독립적으로 읽을 수 있다.

`.github/workflows/github-pages.yml`은 main의 `docs/**` 변경을 계기로 `docs/` 전체를 Pages artifact로 업로드한다. 이 문서 및 이 폴더에는 공개 가능한 설명만 넣는다. 개인 사진, 세션 초안, 서명 자료, 인증 정보, 개발자 절대 경로를 자산 예시나 검증 첨부물로 추가하지 않는다.

## 9. 다음 자산 변경 때의 확인 순서 제안

1. 변경할 논리 ID와 현재 사용처를 검색하고 제작 원본·런타임 자산·사용자 출력물 중 무엇을 다루는지 정한다.
2. 현행 파일과 renderer를 보존한 상태에서 새 자산 또는 변경안을 준비한다. 기존 자산의 자동 변환·이름 변경을 문서 정리 작업에 섞지 않는다.
3. 파일 치수·알파·density·경로 연결을 확인하고, 코드 벡터라면 Preview와 최종 Canvas가 같은 형상을 사용하는지 확인한다.
4. 2·4·6컷과 가로/세로/격자, 문구·날짜 ON/OFF, 밝고 어두운 배경, 최소/최대 장식 scale에서 비교한다.
5. 화면 preview뿐 아니라 실제 저장 JPEG를 열어 사진 슬롯, 장식 모양·색·위치·회전·크기, caption과 알파 경계를 확인한다.
6. 검증한 범위와 미검증 범위를 작업 기록에 남긴다. 이 가이드의 현재 규격을 바꿨다면 해당 근거 코드와 설명을 함께 갱신한다.

이번 확인은 코드·자산 목록·PNG 헤더 및 픽셀 알파 검사다. 런처 마스크와 themed icon, 실제 촬영부터 저장까지의 기기 시각 검증, 새로운 파일 자산 로더 또는 변환 파이프라인 검증은 수행하지 않았다.

## 10. 2026-09-10 릴스 홍보 자산 — Film Story v1

기준은 `4527a05`의 앱 1.3 (4)와 기존 스토어 제작물이다. `design/reels/film-story-v1/`에 30초 홍보 영상 2종, 1080×1920 JPEG 커버, 게시글/편집용 SRT/업로드 안내, 제작 코드와 검증 자료를 추가했다. 앱 내 리소스·아이콘·UI·Manifest·Gradle은 변경하지 않았다.

- `deliverables/Pocket4Cut-Reels-30s.mp4`: 1080×1920 / 9:16 / 30 fps / 900프레임 / 30초. H.264 High, yuv420p, BT.709, faststart. AAC 48 kHz stereo 음악+효과음.
- `deliverables/Pocket4Cut-Reels-30s-NoMusic.mp4`: 동일한 영상에 합성 효과음만 포함. 새 음악 추가용이며 완전 무음본은 아니다.
- `deliverables/Pocket4Cut-Reels-Cover.jpg`: 1080×1920 불투명 JPEG. 인화지·앱 이름·큰 한글 타이포그래피로 구성한 릴스 커버이며 앱 아이콘 교체본이 아니다.
- `audio/`: 외부 음원·샘플 없이 절차적으로 합성한 30초 / 48 kHz / stereo / PCM16 WAV 세 파일. 최종 MP4의 음악 포함본은 −15.3 LUFS / −2.1 dBTP로 측정했다.

새 사진/로고를 생성하지 않았다. Film Strip v4 아이콘, 기존 프로덕션 Compose 호스트 렌더, CollageRenderer의 네 컷 JPEG, 가상 성인 생성 사진을 재사용했다. 사진 이동·강조·전환은 영상 편집 효과이며 실제 디바이스 연속 녹화가 아니다. 사용자 촬영 사진이나 실제 고객 후기로 소개하지 않는다. 프레임 색 예시는 필터도 적용된 스타일링 조합이다.

재료의 경로·치수·해시는 `verification/input-manifest.json`, 편집 판단·트렌드 참고 링크는 `source/CREATIVE-BRIEF.md`, 음악 제작/권리 한계는 `source/AUDIO-NOTES.md`, 실행 방법과 도구 출처는 해당 폴더 README에 기록했다. 기존 이미지의 생성 프롬프트는 원래 스토어 자산 패키지에 남겨 두었다. FFmpeg 실행 파일과 한글 폰트 파일을 앱 또는 납품 ZIP에 포함하지 않는다.

두 MP4를 전체 디코딩하고 규격·길이·동기·오디오 true peak·예상치 못한 검은 구간을 검사했다. 주요 자막 영역, 시안/최종 디코딩 8장면, 시작/끝 프레임, 커버를 시각 검토했다. 기본 색상 태그가 누락된 최초 출력은 납품하지 않고 태그 설정을 보완해 재출력했다. 실제 휴대전화 음량, Instagram 업로드 자르기/심사, 앱 E2E는 미검증이다.
