#!/usr/bin/env node
'use strict';

// Everyday Editions 88-frame version of the seasonal-v4 editorial film.
// Current runtime art/catalog are used in prepared promotional composites;
// these are not Android screenshots or a live device recording.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { once } = require('node:events');
const { spawn } = require('node:child_process');
const { createHash } = require('node:crypto');
const { createCanvas, loadImage, GlobalFonts } = require('@napi-rs/canvas');

const ROOT = path.resolve(__dirname, '../../../..');
const BASE = path.resolve(__dirname, '..');
const PROOF = path.join(BASE, 'visual-stills');
const OUTPUT = path.join(BASE, 'deliverables');
const BUILD = path.join(ROOT, 'build/reels-render');
const W = 1080, H = 1920, FPS = 30;
const C = { paper: '#F0ECE3', ink: '#252520', red: '#C83D2D', white: '#FFFFFF' };
const storyFile = process.env.NARRATION_STORY ? path.resolve(process.env.NARRATION_STORY) : path.join(__dirname, 'story-timed.json');
assert(fs.existsSync(storyFile), 'Run verified speech alignment before rendering; no untimed draft fallback.');
const STORY = JSON.parse(fs.readFileSync(storyFile, 'utf8').replace(/^\uFEFF/, ''));
const speechCheck = JSON.parse(fs.readFileSync(path.join(BASE, 'verification/asr-result.json'), 'utf8'));
assert(speechCheck.readyForRender === true, 'Speech verification has unresolved issues; do not render a stale story.');
assert.equal(speechCheck.master.durationSeconds, STORY.duration, 'Master and story durations differ.');
const SHOTS = STORY.scenes, DURATION = STORY.duration;
const IDS = new Set(['hook', 'solution', 'birthday', 'couple', 'graduation', 'travel', 'layout', 'save', 'cta']);
assert(Number.isFinite(DURATION) && DURATION > 0 && DURATION <= 45, 'Invalid duration');
assert(Array.isArray(SHOTS) && SHOTS.length > 0, 'Missing scenes');
SHOTS.forEach((s, i) => {
  assert(IDS.has(s.id), 'Unsupported scene ' + s.id);
  assert(Number.isFinite(s.start) && Number.isFinite(s.end) && s.end > s.start, 'Invalid timing');
  assert(Math.abs(s.start - (i ? SHOTS[i - 1].end : 0)) < 0.00001, 'Scenes must be contiguous');
  assert(Array.isArray(s.lines) && s.lines.length > 0 && s.lines.length <= 2 && s.lines.every(x => typeof x === 'string' && x.length), 'Use short subtitles');
  assert(s.lines.every(line => !/(?:광고|사용\s*예시|AI\s*나[래레]이션|생성\s*예시)/i.test(line)), 'Remove excluded overlay wording');
  if (s.id === 'layout') assert(['two', 'four', 'six', 'gallery'].includes(s.variant || 'gallery'), 'Unsupported layout variant');
});
assert(Math.abs(SHOTS.at(-1).end - DURATION) < 0.00001, 'Incorrect story duration');

const ASSETS = {
  icon: 'design/play-store/film-strip-v4/app-icon-master-1024.png',
  birthday: 'design/reels/occasion-v5/visual-assets/birthday.jpg',
  couple: 'design/reels/occasion-v5/visual-assets/couple.jpg',
  graduation: 'design/reels/occasion-v5/visual-assets/graduation.jpg',
  travel: 'design/reels/occasion-v5/visual-assets/travel.jpg',
  two: 'design/reels/occasion-v5/visual-assets/two.jpg',
  four: 'design/reels/occasion-v5/visual-assets/four.jpg',
  six: 'design/reels/occasion-v5/visual-assets/six.jpg',
  all88: 'design/reels/occasion-v5/visual-assets/overview-88.jpg',
  photo2: 'design/play-store/print-booth-v1/demo-photos/demo_02.jpg',
};
const images = {}, checks = [], layoutChecks = [];
const OCCASIONS = new Set(['birthday', 'couple', 'graduation', 'travel']);
const OCCASION_PAPER = { birthday: '#FFF4E6', couple: '#FCE8E7', graduation: '#F0ECD9', travel: '#F1E9D8' };
const canvas = createCanvas(W, H), ctx = canvas.getContext('2d');
let audit = false;
const clamp = (x, a = 0, b = 1) => Math.max(a, Math.min(b, x));
const ease = x => 1 - Math.pow(1 - clamp(x), 3);

function rect(x, y, w, h, color) { ctx.fillStyle = color; ctx.fillRect(x, y, w, h); }
function font(size, bold = true) { ctx.font = `${size}px "${bold ? 'Pocket Bold' : 'Pocket Regular'}"`; }
function text(str, x, y, size = 44, color = C.ink, options = {}) {
  const { align = 'left', maxWidth = 820, stroke = false, bold = true, critical = true } = options;
  ctx.save(); font(size, bold);
  while (ctx.measureText(str).width > maxWidth && size > 25) font(--size, bold);
  const m = ctx.measureText(str), left = align === 'center' ? x - m.width / 2 : x;
  assert(m.width <= maxWidth + .1, 'Text cannot fit: ' + str);
  if (audit && critical) {
    const box = { text: str, size, left, right: left + m.width, top: y - m.actualBoundingBoxAscent, bottom: y + m.actualBoundingBoxDescent };
    assert(box.left >= 80 && box.right <= 900.5 && box.top >= 240 && box.bottom <= 1480.5, 'Unsafe text ' + JSON.stringify(box));
    checks.push(box);
  }
  ctx.textAlign = align; ctx.textBaseline = 'alphabetic';
  if (stroke) {
    ctx.strokeStyle = '#00000090'; ctx.lineWidth = 5; ctx.lineJoin = 'round';
    ctx.shadowColor = '#0000008A'; ctx.shadowBlur = 7; ctx.shadowOffsetY = 2;
    ctx.strokeText(str, x, y); ctx.shadowBlur = 0; ctx.shadowOffsetY = 0;
  }
  ctx.fillStyle = color; ctx.fillText(str, x, y); ctx.restore();
}
function print(name, x, y, height, angle = 0, shadow = true) {
  const im = images[name], width = height * im.width / im.height;
  ctx.save(); ctx.translate(x, y); ctx.rotate(angle * Math.PI / 180);
  if (shadow) { ctx.shadowColor = '#25252040'; ctx.shadowBlur = 27; ctx.shadowOffsetY = 16; }
  ctx.drawImage(im, -width / 2, -height / 2, width, height); ctx.restore();
}
// Fit the entire rotated prepared composition into a reserved, unobstructed box.
// Never use cover/crop scaling here: every 2/4/6 photo window must remain visible.
function fitPrint(name, left, top, width, height, angle = 0, p = 0) {
  const im = images[name], ratio = im.width / im.height;
  const radians = Math.abs(angle) * Math.PI / 180;
  const rotatedWidth = ratio * Math.cos(radians) + Math.sin(radians);
  const rotatedHeight = Math.cos(radians) + ratio * Math.sin(radians);
  const drawHeight = Math.min(width / rotatedWidth, height / rotatedHeight) * (.986 + .014 * ease(p));
  const x = left + width / 2, y = top + height / 2;
  print(name, x, y, drawHeight, angle);
  if (audit) {
    const box = {
      asset: name, sourceWidth: im.width, sourceHeight: im.height,
      left: x - drawHeight * rotatedWidth / 2, right: x + drawHeight * rotatedWidth / 2,
      top: y - drawHeight * rotatedHeight / 2, bottom: y + drawHeight * rotatedHeight / 2,
      reserved: { left, top, right: left + width, bottom: top + height },
    };
    assert(box.left >= left - .01 && box.right <= left + width + .01 &&
      box.top >= top - .01 && box.bottom <= top + height + .01, 'Clipped layout ' + name);
    assert(box.left >= 60 && box.right <= 1000 && box.top >= 420 && box.bottom <= 1700, 'Unsafe layout placement ' + name);
    layoutChecks.push(box);
  }
}
function photo(name, x, y, size, angle = 0, border = 0) {
  ctx.save(); ctx.translate(x, y); ctx.rotate(angle * Math.PI / 180);
  if (border) rect(-size / 2 - border, -size / 2 - border, size + border * 2, size + border * 2, C.paper);
  ctx.drawImage(images[name], -size / 2, -size / 2, size, size); ctx.restore();
}
function darkPhoto(name, p = 0) {
  ctx.save(); ctx.filter = 'blur(18px)'; photo(name, 540, 960, 2030 + p * 20); ctx.restore();
  rect(0, 0, W, H, '#1A211CDD');
}
function identity(y, light = false) {
  ctx.drawImage(images.icon, 80, y, 93, 93);
  text('Pocket4Cut', 191, y + 63, 47, light ? C.white : C.ink, { maxWidth: 550 });
}
function caption(shot, light = true, y = 1062) {
  const start = y - (shot.lines.length - 1) * 29;
  shot.lines.forEach((line, i) => text(line, 490, start + i * 59, 45, light ? C.white : C.ink, {
    align: 'center', maxWidth: 804, stroke: light,
  }));
}

function scene(shot, t) {
  const p = clamp((t - shot.start) / (shot.end - shot.start));
  ctx.resetTransform(); ctx.globalAlpha = 1; ctx.filter = 'none';
  rect(0, 0, W, H, C.paper);
  let light = false, captionY = 1062;
  if (shot.id === 'hook') {
    // Open with finished birthday/couple compositions in the same print-led hook.
    darkPhoto('photo2', p); light = true;
    const tight = shot.variant === 'tight';
    print('couple', tight ? 804 : 798, 1070, tight ? 1660 : 1560, 9);
    print('birthday', tight ? 365 : 354, 979, (tight ? 1615 : 1535) + ease(p) * 25, -6);
    captionY = 1114;
  } else if (shot.id === 'solution') {
    rect(0, 0, W, H, '#D7D5C8');
    if (p < .52) {
      // Keep the familiar four-print fan, then make a single editorial hard cut.
      print('travel', 862, 1098, 1210, 11);
      print('graduation', 665, 1019, 1300, 4);
      print('couple', 438, 1062, 1330 + p * 15, -4);
      print('birthday', 235, 1125, 1260 + p * 18, -10);
      rect(0, 0, W, 436, '#D7D5C8');
      identity(319);
      text('88가지 프레임', 626, 382, 31, C.red, { maxWidth: 274 });
      captionY = 1129; light = true;
    } else {
      // Show all 88 actual designs together, without cropping the outer rows.
      // Move the caption above the wall; the brand row is intentionally absent
      // during this beat so caption text and identity cannot occupy the same area.
      fitPrint('all88', 80, 455, 880, 1245, 0, p);
      text('10개 카테고리', 490, 410, 27, C.red, { align: 'center', maxWidth: 804 });
      captionY = 358;
    }
  } else if (OCCASIONS.has(shot.id)) {
    rect(0, 0, W, H, OCCASION_PAPER[shot.id]);
    // One narration beat can reveal both the complete frame and its paper detail.
    // Explicit full/detail variants are also available for separately timed shots.
    const detail = shot.variant === 'detail' || (shot.variant !== 'full' && p >= .52);
    if (detail) {
      // The enlarged background is the same prepared composition, not invented UI.
      // A complete four-window print remains visible in front of the detail.
      print(shot.id, 873, 1040, 2140 + p * 24, 8);
      print(shot.id, 381, 974, 1450 + ease(p) * 16, -4);
    } else {
      const angle = shot.id === 'couple' || shot.id === 'travel' ? 2 : -2;
      print(shot.id, 486, 993, 1500 + p * 18, angle);
    }
    light = true; captionY = 1114;
  } else if (shot.id === 'layout') {
    rect(0, 0, W, H, '#E5E1D7');
    const variant = shot.variant || 'gallery';
    if (variant === 'gallery') {
      // Non-overlapping cards: two above, four/six below, all original ratios.
      fitPrint('two', 170, 430, 720, 515, -2, p);
      fitPrint('four', 70, 1010, 430, 660, -3, p);
      fitPrint('six', 560, 1010, 430, 660, 3, p);
      text('10개 카테고리', 490, 410, 27, C.red, { align: 'center', maxWidth: 804 });
    } else {
      fitPrint(variant, 80, 450, 890, 1220, variant === 'four' ? 2 : -2, p);
    }
    captionY = 358;
  } else if (shot.id === 'save') {
    rect(0, 0, W, H, '#D9DACF');
    // Keep the same travel outcome and photo order for both save/share phrases.
    // This editorial label is not an old app screenshot or a simulated button.
    print('travel', 847, 720, 1200, 9);
    print('travel', 484, 694, 1115 + p * 12, -2);
    rect(80, 1280, 815, 104, C.ink);
    text('저장  ·  공유', 487, 1345, 37, C.white, { align: 'center', maxWidth: 763 });
    captionY = 1119; light = true;
  } else if (shot.id === 'cta') {
    rect(0, 0, W, H, '#D7D3C6');
    print('travel', 867, 1164, 1360, 12);
    print('graduation', 653, 1067, 1360, 4);
    print('couple', 454, 1035, 1380, -3);
    print('birthday', 250, 1060, 1300 + p * 22, -9);
    rect(0, 0, W, 436, '#D7D3C6');
    identity(321);
    rect(80, 1369, 815, 104, C.ink);
    text('Google Play에서 Pocket4Cut 검색', 487, 1434, 37, C.white, { align: 'center', maxWidth: 763 });
    captionY = 1120; light = true;
  }
  caption(shot, light, captionY);
}
function renderFrame(t, check = false) {
  const time = Math.max(0, Math.min(DURATION - .000001, t));
  const shot = SHOTS.find(s => time >= s.start && time < s.end);
  assert(shot, 'Missing scene at ' + time); audit = check; scene(shot, time); audit = false;
}
async function init() {
  [PROOF, OUTPUT, BUILD].forEach(p => fs.mkdirSync(p, { recursive: true }));
  const fonts = [
    [process.env.REEL_FONT_REGULAR || path.join(process.env.WINDIR || 'C:/Windows', 'Fonts/malgun.ttf'), 'Pocket Regular'],
    [process.env.REEL_FONT_BOLD || path.join(process.env.WINDIR || 'C:/Windows', 'Fonts/malgunbd.ttf'), 'Pocket Bold'],
  ];
  fonts.forEach(([file, family]) => assert(GlobalFonts.registerFromPath(file, family), 'Missing font ' + family));
  for (const [id, relative] of Object.entries(ASSETS)) images[id] = await loadImage(path.join(ROOT, relative));
  fs.writeFileSync(path.join(PROOF, 'input-manifest.json'), JSON.stringify({
    note: 'Existing fictional adult sample photos, prepared occasion frame composites based on the current runtime atlas/catalog without the removed Pocket/NN header, and the existing brand icon. Editorial animation, not Android-rendered output or a device recording. The save/share label is editorial typography, not an app screenshot.',
    assets: Object.entries(ASSETS).map(([id, file]) => ({ id, file, width: images[id].width, height: images[id].height, sha256: createHash('sha256').update(fs.readFileSync(path.join(ROOT, file))).digest('hex') })),
  }, null, 2) + '\n');
}
async function stills() {
  checks.length = 0; layoutChecks.length = 0;
  const columns = 5, cellW = 270, cellH = 532;
  const sheet = createCanvas(columns * cellW, Math.ceil(SHOTS.length / columns) * cellH), sc = sheet.getContext('2d');
  sc.fillStyle = C.paper; sc.fillRect(0, 0, sheet.width, sheet.height);
  for (let i = 0; i < SHOTS.length; i++) {
    const shot = SHOTS[i];
    for (const [suffix, portion] of [['a', .23], ['b', .73]]) {
      renderFrame(shot.start + (shot.end - shot.start) * portion, true);
      const jpeg = canvas.toBuffer('image/jpeg', 95);
      fs.writeFileSync(path.join(PROOF, `scene-${String(i + 1).padStart(2, '0')}-${shot.id}-${suffix}.jpg`), jpeg);
      if (suffix === 'a') {
        sc.drawImage(await loadImage(jpeg), (i % columns) * cellW + 5, Math.floor(i / columns) * cellH + 5, 260, 462.22);
        sc.font = '18px "Pocket Bold"'; sc.fillStyle = C.ink;
        sc.fillText(`${shot.start.toFixed(1)}–${shot.end.toFixed(1)}s ${shot.id}`, (i % columns) * cellW + 7, Math.floor(i / columns) * cellH + 500);
      }
    }
  }
  fs.writeFileSync(path.join(PROOF, 'storyboard-contact-sheet.jpg'), sheet.toBuffer('image/jpeg', 95));
  audit = true; rect(0, 0, W, H, C.paper);
  print('travel', 860, 1379, 1030, 10);
  print('graduation', 650, 1339, 1050, 4);
  print('couple', 440, 1345, 1050, -3);
  print('birthday', 231, 1384, 1010, -9);
  text('오늘을 위한', 82, 453, 87, C.ink, { maxWidth: 780 });
  text('88가지 프레임', 82, 566, 84, C.red, { maxWidth: 804 });
  identity(632);
  audit = false;
  fs.writeFileSync(path.join(OUTPUT, 'Pocket4Cut-88Frames-Cover.jpg'), canvas.toBuffer('image/jpeg', 96));
  fs.writeFileSync(path.join(PROOF, 'text-safe-area-checks.json'), JSON.stringify({
    note: 'Conservative project critical-text bounds, not a universal Instagram specification. Existing app UI can extend outside the caption region.',
    bounds: { left: 80, right: 900, top: 240, bottom: 1480 }, checks,
  }, null, 2) + '\n');
  fs.writeFileSync(path.join(PROOF, 'layout-safe-area-checks.json'), JSON.stringify({
    note: 'Full source images retain their aspect ratio and fit non-overlapping reserved boxes. No layout scene uses a source crop.',
    bounds: { left: 60, right: 1000, top: 420, bottom: 1700 }, checks: layoutChecks,
  }, null, 2) + '\n');
  fs.writeFileSync(path.join(PROOF, 'story-used.json'), JSON.stringify(STORY, null, 2) + '\n');
  console.log(`Verified ${SHOTS.length * 2} stills and ${checks.length} text bounds. Cover: 1080x1920.`);
}
async function video() {
  const ffmpeg = process.env.FFMPEG_PATH;
  assert(ffmpeg && fs.existsSync(ffmpeg), 'Set FFMPEG_PATH to the verified executable');
  const target = path.join(BUILD, 'occasion-v5-visual.mp4');
  const args = ['-hide_banner', '-loglevel', 'warning', '-y', '-f', 'rawvideo', '-pixel_format', 'rgba', '-video_size', `${W}x${H}`, '-framerate', String(FPS), '-i', 'pipe:0', '-an',
    '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', '-pix_fmt', 'yuv420p', '-vf', 'scale=in_range=pc:out_range=tv:out_color_matrix=bt709,setparams=range=limited:color_primaries=bt709:color_trc=bt709:colorspace=bt709',
    '-colorspace', 'bt709', '-color_primaries', 'bt709', '-color_trc', 'bt709', '-color_range', 'tv', '-profile:v', 'high', '-level:v', '4.2', '-threads', '6', '-movflags', '+faststart', target];
  const encoder = spawn(ffmpeg, args, { stdio: ['pipe', 'ignore', 'pipe'], windowsHide: true });
  let errors = ''; encoder.stderr.on('data', b => { errors = (errors + b.toString()).slice(-20000); });
  const finished = new Promise((resolve, reject) => {
    encoder.once('error', reject); encoder.once('close', code => code === 0 ? resolve() : reject(new Error(`FFmpeg exit ${code}: ${errors}`)));
  });
  const started = Date.now(), frames = Math.ceil(DURATION * FPS);
  for (let f = 0; f < frames; f++) {
    renderFrame(f / FPS);
    const frame = Buffer.from(canvas.data());
    if (!encoder.stdin.write(frame)) await once(encoder.stdin, 'drain');
    if (f % 90 === 0) console.log(`Rendered ${f}/${frames} frames (${((Date.now() - started) / 1000).toFixed(1)}s)`);
  }
  encoder.stdin.end(); await finished;
  fs.writeFileSync(path.join(PROOF, 'render-manifest.json'), JSON.stringify({
    storySha256: createHash('sha256').update(fs.readFileSync(storyFile)).digest('hex'),
    visualSha256: createHash('sha256').update(fs.readFileSync(target)).digest('hex'),
    inputManifestSha256: createHash('sha256').update(fs.readFileSync(path.join(PROOF, 'input-manifest.json'))).digest('hex'),
    rendererSha256: createHash('sha256').update(fs.readFileSync(__filename)).digest('hex'),
    asrResultSha256: createHash('sha256').update(fs.readFileSync(path.join(BASE, 'verification/asr-result.json'))).digest('hex'),
    duration: frames / FPS, requestedDuration: DURATION, fps: FPS, frames, width: W, height: H,
    video: 'H.264 high / yuv420p / BT.709 / faststart', elapsedSeconds: (Date.now() - started) / 1000,
    shots: SHOTS, deviceCapture: false, audio: false, errors,
  }, null, 2) + '\n');
  console.log('Visual master complete: ' + target);
}
async function main() { await init(); await stills(); if (!process.argv.includes('--stills')) await video(); }
main().catch(e => { console.error(e); process.exitCode = 1; });
