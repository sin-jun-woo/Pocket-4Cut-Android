# Illustration generation notes — themes 02–30

Generated on 2026-09-23. All 29 assigned themes received a separate built-in `image_gen` request. Original returned PNGs were copied unchanged into `masters/`; exact prompts and per-image visual observations are in `prompts/<id>.json`. No source atlas is silently reused for another theme. The built-in tool exposes neither model selection nor a quality selector, so no highest-model claim is made.

Scope: illustration masters only. No application, navigation, persistent state, UI, renderer, build or test files were changed by this contributor. No tests or builds were run.

## Visual review observations

- All 29 displayed outputs contain the requested six motif groups, in reading order, without visible lettering or branding. Fine painted internal shading is stronger in some metal, glass, toy and fabric objects than the flat shared prompt requested; this is documented in their prompt records.
- Several silhouettes extend beyond nominal 512px cells despite the margin instruction. In particular, `valentine` rose, `seollal` jeogori, `bridal-shower` ribbons, `teachers-day` carnation and `coming-of-age` rose need adaptive alpha-based extraction. Original objects are not visibly clipped at canvas edges. Root handles extraction and final placement; this is not a statement that a fixed-cell crop would be safe.
- `new-year` clock includes small winter berries/leaves and the ivory star is an ornament. Use firework/streamer/coupes as dominant motifs so the frame reads as New Year rather than Christmas.
- `white-day` pastel heart candies have faceted bright highlights. Prefer the wrapped sweet/lollipop as dominant motifs if a softer stationery tone is needed.
- `wedding`, `proposal` and `bridal-shower` have pale ivory elements. Preserve their warm contour contrast against the final paper and leave sufficient breathing room instead of clustering all six motifs.
- `hangeul-day` deliberately has no generated Hangul; typeset accurate Hangul in the final frame. `liberation-day` follows the catalog with hibiscus/dove/ribbons and deliberately does not generate an uncertain flag.

The tool's inline transparent-background presentation displays colored RGB bleed against dark areas. This log records visual subjects, not an independent neutral-background alpha-edge validation; root performs neutral compositing and normalization separately. No original master was repainted, flattened or destructively edited.
