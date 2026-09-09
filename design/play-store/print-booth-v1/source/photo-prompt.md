# 예시 사진 제작 기록

Built-in image generation 사용. 실제 이용자, 후기 또는 촬영 성능의 증거가 아닌 가상의 성인 인물 예시다. 사진은 제작 전용이며 앱 런타임에 번들하지 않는다.

## 제작 프롬프트

Use case: photorealistic-natural. Asset type: demo photos for Pocket 4Cut, a Korean self-photo-booth collage Android app store listing. Generate ONE square 2048x2048 contact sheet divided into an exact 2-by-2 grid of four equal square photographs, edge-to-edge with NO gutters, NO borders, NO lettering, NO numbers, NO logos, NO watermark. Each quadrant is a different photograph of the SAME TWO fictional adult Korean friends in their mid-twenties in a simple self-photo booth, shoulder-up, both entire heads comfortably inside every quadrant, facing the camera, casual ivory knit and charcoal cotton outfits. Top left: relaxed smiles side by side. Top right: spontaneous laughing with eyes open. Bottom left: one small V-sign beside a cheek, playful but natural. Bottom right: leaning heads gently together, warm smiles. Backdrop identical warm muted beige plaster in every photograph. Soft frontal flash and diffused warm daylight, realistic pores, natural individual facial features, slightly imperfect candid expressions, real cotton and knit texture. Composition practical for cropping to a horizontal or vertical photobooth frame; keep people centered with generous headroom. Clean contemporary editorial photography, restrained warm film color, no beauty retouch look, no plastic skin, no props, no device, no accessories that have visible brands. The pictures are sample app content, not testimonials. Critical: exactly four distinct square pictures occupying exact quadrants of a square canvas; preserve the same identities and clothing throughout.

`prepare-photos.cjs`는 생성된 단일 시트의 네 사분면을 JPEG 파일 네 개로 분리하며 실제 출력 치수를 기록한다. 인물·표정의 추가 합성이나 보정은 하지 않는다.

## 실제 받은 출력

- 프롬프트의 요청 크기와 달리 반환된 시트는 **1254×1254 PNG**였다. 보관 파일은 `../demo-photos/generated-contact-sheet.png`다.
- 사분면을 분리한 `../demo-photos/demo_01.jpg`부터 `demo_04.jpg`는 각각 **627×627**이다.
- 이 사진들은 앱의 기존 사진 배치·필터·저장 화면에 들어가는 예시 콘텐츠로만 사용했다. 홍보 문구와 UI는 이미지 생성 모델로 그리지 않았다.
