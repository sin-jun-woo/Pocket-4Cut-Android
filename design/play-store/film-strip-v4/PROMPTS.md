# Film Strip v4 — Image generation prompts

Mode: built-in image_gen. No API/CLI fallback. All people are fictional generated adults, not user photographs. The first checkerboard draft is not an alpha asset and is not for release. Final wordmark edits are the selected masters.

Only production resizing, color/alpha encoding, Android safe-area padding and mask previews are performed by the export script. Creative generation and editing were done with image_gen.

## Initial icon (checkerboard defect; rejected)

```text
Use case: logo-brand.
Asset type: production Android app icon foreground artwork, square 1024 x 1024, genuinely transparent background.
Primary request: Create an original distinctive icon for Pocket4Cut, a Korean four-cut self-photo booth app. The icon must be about a real four-picture photo strip, not a camera and not a numeral. Restrained analog photographic identity.
Subject: one continuous warm ivory rectangular photo-booth print, subtly tilted counterclockwise about 10 degrees. Exactly FOUR separate landscape black-and-white photographic frames arranged vertically on that single print, equal size, slim ivory dividers and generous lower paper margin. Within all four frames show the same two fictional East Asian young adults in their twenties, woman with bobbed dark hair and man with short dark hair, close faces at different natural playful poses: quiet smile, spontaneous laugh, leaning together, looking warmly into the lens. Candid monochrome contrast, photographic rather than cartoon, no beauty-retouching look. Faces large and simple enough to suggest people when reduced to 48 pixels; do not rely on tiny detail.
Behind the print, a narrow dark sepia film negative strip peeks out on its RIGHT side, angled slightly clockwise, with a disciplined row of small rectangular sprocket holes, distinctly secondary to the four-cut print. Do not create a second dominant photo strip. Add one small flat brick-red rectangular printed registration mark at the bottom ivory margin, the only color accent.
Style: considered independent photo-lab visual identity, graphic cut-paper arrangement using real-looking photo content; straight clean paper edges, almost flat front view, natural print detail very restrained, tight balanced proportions. No glossy 3D modeling, no bent curled paper, no elaborate shadows, no lens, no camera body, no numbers, no letters, no words, no brand logos, no stars/hearts/sparkles, no stickers, no badges, no outer rounded square.
Composition: a SINGLE icon artwork isolated centered on true transparent canvas, all visible artwork inside the central 80 percent of the square; no cut-off corners. Photo strip should be visually substantial not an ultra-thin ribbon; overall cluster about half canvas width and four fifths canvas height. Alpha must be truly transparent outside the artwork, including sprocket holes; NOT a checkerboard printed into the image. Preserve clean antialiased edges.
Palette: warm paper #F7F3E9, deep ink black and grayscale photographs, dark warm sepia film edge, brick red #C83D2D registration mark. Intended final background is flat charcoal #1B1B19 but DO NOT render that background here.
Output: one final asset only, no presentation board, no comparison grid, no device mockup.
```

## Charcoal background correction

```text
Use case: precise-object-edit. Input image 1 is the edit target: the approved photo-strip artwork, but its checkerboard background is an export defect.
Keep the EXACT single ivory four-cut photo strip, all four photos and faces, film edge, red rectangle, rotation and relative composition unchanged. Correct ONLY the background and outer canvas framing:
Replace EVERY part of the checkerboard, INCLUDING inside each film sprocket hole, with one perfectly uniform flat opaque charcoal color RGB(27,27,25) / #1B1B19. No checkerboard may remain. No texture, noise, gradient, lighting or shadow on the background.
Scale the entire unchanged photo-strip artwork cluster down uniformly around the center so the TOPMOST and BOTTOMMOST corners fit between y=22% and y=78% of the complete square, centered horizontally. This enlarged plain margin is important for Android adaptive-icon safe area. All the artwork should fit within a central circle of radius 29% of the square. This is NOT the final store crop; keep these margins.
Square image 1024x1024 or higher. Full square opaque background; no rounded outer tile, no external shadow, no extra icon frame, no words, no extra graphics. Preserve four photographic frames, not three or five.
```

## Feature graphic

```text
Use case: ads-marketing.
Asset type: Google Play FEATURE GRAPHIC for the Korean self-photo booth app Pocket4Cut. A FINISHED landscape banner, approximately 2048 x 1000 pixels, aspect ratio 2.048:1.
Input image 1 is a STYLE AND SUBJECT REFERENCE ONLY, not an edit target. Use its warm ivory photo print, black-and-white fictional adult couple, and restrained brick red analog photo-lab identity. Do not display the app icon as a giant square.
Design a confident, warm editorial photographic banner with exceptional Korean typography, inspired by an independent analog photo lab's printed poster. Warm matte ivory #F3F0E8 full-bleed background. A broad brick-red #C83D2D rectangular color field occupies roughly the RIGHT third behind the photographic still life; no gradients, no rounded card UI.
Left-center contains beautifully typeset dark ink Korean headline with these EXACT two lines:
"오늘의 우리,"
"네 컷으로."
Set it LARGE in a crisp heavy Korean sans-serif, controlled spacing, sophisticated rather than cute. Above it, much smaller brick-red brand wordmark exactly "Pocket4Cut". Below the headline, small but readable dark ink text exactly "찍고, 꾸미고, 간직해요."
Right-center contains TWO physical straight-edged four-cut photo-booth prints lying at opposing slight angles on the ivory/red color fields. One is black-and-white, the other subtly warm muted color. Each print has EXACTLY FOUR landscape pictures of the same two fictional East Asian adults in their twenties smiling, laughing, leaning close and posing naturally; consistent identities across all frames, real candid photo-print feel, no cartoon. Use those strips as actual photo keepsakes, not as app icon badges. A little dark sepia film edge with rectangular perforations peeks from behind one print as a minor detail. No hands outside photographs. Paper contact shadows can be very slight, no extruded 3D. Keep all print corners visible.
Composition: balanced asymmetric editorial layout with substantial negative space. Put the brand, Korean headline and complete four-frame print objects within central 80% width and central 80% height, away from crop-prone edges. The headline must not overlap the photographs. The headline should be the strongest element alongside the photos. Important content no closer than 10% of canvas height from the top/bottom. Only background may meet the edges. Avoid tiny text, avoid decorative labels.
Constraints: EXACT text only as listed, no other text, no fake UI, no phones, no store badges, no price, no awards, no claims about printing physical products, no third-party marks, no stars/hearts/sparkles, no tape/stickers, no artificial light leaks or heavy grunge, no rounded cards, no collage of design alternatives, no watermark. Return one complete final banner image.
```

## User-requested lowercase signature — icon

```text
Use case: precise-object-edit. Image 1 is the edit target. Make ONLY this requested addition: On the empty ivory WHITE LOWER MARGIN of the photo-booth paper strip, directly below the fourth photograph, print the word "pocket4cut" exactly, all LOWERCASE, spelled p-o-c-k-e-t-4-c-u-t. Use a clean restrained dark ink sans-serif wordmark, medium weight, aligned parallel to the bottom paper edge. Make it legible and centered in the available white space to the left of the existing small red rectangle, comfortably padded; approximately half the paper width. Keep the small red rectangle. This looks like a photo lab's printed signature, not a sticker or a title floating outside the print.
Preserve ALL FOUR photos, both people's exact faces and poses, film edge and sprocket holes, white paper margins, proportions, positioning, rotation, square dimensions, and charcoal background. No other changes. Keep exactly four frames. No other text additions.
```

## User-requested lowercase signature — feature

```text
Use case: precise-object-edit. Image 1 is the edit target, a completed landscape feature graphic. Make ONLY this requested addition: On the empty ivory WHITE LOWER MARGIN of EACH of the TWO photo-booth paper prints, below its fourth photograph, print the word "pocket4cut" exactly, all LOWERCASE, spelled p-o-c-k-e-t-4-c-u-t. There must be exactly two new "pocket4cut" signatures, one on each paper strip. Use a clean restrained dark ink sans-serif wordmark, medium weight, aligned parallel to the paper's bottom edge, centered in the available space to the left of its existing small red rectangle. Keep both red rectangles. Signatures must be legible and naturally printed on the paper, not outside it.
Preserve absolutely everything else: the existing title "Pocket4Cut", the exact Korean text "오늘의 우리," / "네 컷으로." and "찍고, 꾸미고, 간직해요.", all photographs and people's identities, exactly four frames per strip, rotations, film edge, background colors, size and landscape composition. Do not move or replace any existing text. No other changes.
```
