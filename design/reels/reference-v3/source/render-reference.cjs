#!/usr/bin/env node
'use strict';

// Product-first editorial film. Existing production Compose/CollageRenderer
// assets are animated here; this is not a live device recording.
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
const IDS = new Set(['hook', 'solution', 'capture', 'selection', 'layout', 'filter', 'save', 'occasions', 'cta']);
assert(Number.isFinite(DURATION) && DURATION > 0 && DURATION <= 45, 'Invalid duration');
assert(Array.isArray(SHOTS) && SHOTS.length > 0, 'Missing scenes');
SHOTS.forEach((s, i) => {
  assert(IDS.has(s.id), 'Unsupported scene ' + s.id);
  assert(Number.isFinite(s.start) && Number.isFinite(s.end) && s.end > s.start, 'Invalid timing');
  assert(Math.abs(s.start - (i ? SHOTS[i - 1].end : 0)) < 0.00001, 'Scenes must be contiguous');
  assert(Array.isArray(s.lines) && s.lines.length > 0 && s.lines.length <= 2 && s.lines.every(x => typeof x === 'string' && x.length), 'Use short subtitles');
});
assert(Math.abs(SHOTS.at(-1).end - DURATION) < 0.00001, 'Incorrect story duration');

const ASSETS = {
  icon: 'design/play-store/film-strip-v4/app-icon-master-1024.png',
  count: 'design/play-store/print-booth-v1/raw-screenshots/frame_count.png',
  selection: 'design/play-store/print-booth-v1/raw-screenshots/selection.png',
  layout: 'design/play-store/print-booth-v1/raw-screenshots/layout.png',
  edit: 'design/play-store/print-booth-v1/raw-screenshots/edit.png',
  result: 'design/play-store/print-booth-v1/raw-screenshots/result.png',
  white: 'design/play-store/print-booth-v1/capture/fixtures/store-demo-result-white.jpg',
  black: 'design/play-store/print-booth-v1/capture/fixtures/store-demo-result-black.jpg',
  blush: 'design/play-store/print-booth-v1/capture/fixtures/store-demo-result-blush.jpg',
  photo1: 'design/play-store/print-booth-v1/demo-photos/demo_01.jpg',
  photo2: 'design/play-store/print-booth-v1/demo-photos/demo_02.jpg',
  photo3: 'design/play-store/print-booth-v1/demo-photos/demo_03.jpg',
  photo4: 'design/play-store/print-booth-v1/demo-photos/demo_04.jpg',
};
const images = {}, checks = [];
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
function crop(name, sx, sy, sw, sh, dx, dy, dw, dh) {
  const im = images[name];
  assert(sx >= 0 && sy >= 0 && sx + sw <= im.width && sy + sh <= im.height, 'Invalid crop ' + name);
  ctx.drawImage(im, sx, sy, sw, sh, dx, dy, dw, dh);
}
function print(name, x, y, height, angle = 0, shadow = true) {
  const im = images[name], width = height * im.width / im.height;
  ctx.save(); ctx.translate(x, y); ctx.rotate(angle * Math.PI / 180);
  if (shadow) { ctx.shadowColor = '#25252040'; ctx.shadowBlur = 27; ctx.shadowOffsetY = 16; }
  ctx.drawImage(im, -width / 2, -height / 2, width, height); ctx.restore();
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
    // Outcome in the first frame. Film stays dominant; no title-card preamble.
    darkPhoto('photo2', p); light = true;
    const tight = shot.variant === 'tight';
    print('blush', tight ? 885 : 858, 1155, tight ? 1790 : 1620, 12);
    print('white', tight ? 443 : 458, tight ? 958 : 960, tight ? 1875 + p * 24 : 1658 + ease(p) * 25, -4);
    captionY = 1114;
  } else if (shot.id === 'solution') {
    rect(0, 0, W, H, '#D7D5C8');
    print('white', 488, 1122, 1455 + p * 28, 0);
    identity(319); captionY = 1129; light = true;
  } else if (shot.id === 'capture') {
    if (shot.variant === 'detail' || /8장|여덟/.test(shot.lines.join(' '))) {
      rect(0, 0, W, H, '#E5E1D7');
      photo('photo2', 331, 620, 732 + p * 12, -5, 14);
      photo('photo4', 778, 910, 730 + p * 12, 7, 14);
      crop('count', 35, 750, 1010, 350, 66, 1153, 866, 300.1);
      light = true; captionY = 1055;
    } else {
      crop('count', 0, 370, 1080, 1090, 54, 359, 972, 981);
      captionY = 1430;
    }
  } else if (shot.id === 'selection') {
    // Punch-in on the real selected state; no fabricated taps or progress.
    const zoom = 1 + .035 * p;
    crop('selection', 30, 525, 1020, 1275, 20 - p * 17, 301, 1020 * zoom, 1275 * zoom);
    captionY = 1075; light = true;
  } else if (shot.id === 'layout') {
    // Real three layout choices. The selected state is left untouched.
    crop('layout', 30, 730, 1020, 1330, 22 - p * 10, 440, 1020 + p * 20, 1330 + p * 26);
    captionY = 358;
  } else if (shot.id === 'filter') {
    const bw = shot.variant === 'bw' || /흑백/.test(shot.lines.join(' '));
    if (bw) {
      rect(0, 0, W, H, '#252622'); light = true;
      print('black', 492, 953, 1710 + p * 28, 2);
      captionY = 1083;
    } else {
      rect(0, 0, W, H, '#E3DACF');
      print('blush', 783, 662, 1275, 9);
      print('white', 412, 663, 1360, -4);
      // The real Original-selected row appears only with a color result.
      crop('edit', 35, 1390, 1010, 410, 68, 1148, 866, 351.54);
      light = true; captionY = 1039;
    }
  } else if (shot.id === 'save') {
    rect(0, 0, W, H, '#D9DACF');
    // Continue the immediately preceding monochrome export into both actions.
    print('black', 495, 696, 1295 + p * 12, -2);
    crop('result', 36, 1940, 1008, 260, 63, 1228, 887, 228.79);
    captionY = 1119; light = true;
  } else if (shot.id === 'occasions') {
    const n = shot.variant === 'one' ? 2 : shot.variant === 'two' ? 3 : 4;
    darkPhoto('photo' + n, p); light = true;
    photo('photo' + n, 529, 859, 1250 + p * 20, n === 3 ? 1 : -1);
    // Small secondary completed print gives the square original a product context.
    print(n === 3 ? 'black' : 'white', 773, 1427, 664, 7);
    captionY = 1064;
  } else if (shot.id === 'cta') {
    rect(0, 0, W, H, '#D7D3C6');
    print('blush', 859, 1164, 1580, 12);
    print('black', 702, 1067, 1570, 4);
    print('white', 382, 1000, 1530 + p * 22, -7);
    // Keep a clear brand header above the large photo prints.
    rect(0, 0, W, 436, '#D7D3C6');
    identity(321);
    // CTA uses the actual brand, not a copied reference account or fake offer.
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
    note: 'Only existing AI sample photos, production Compose/CollageRenderer outputs, and the current brand icon are used. Editorial animation, not a device recording.',
    assets: Object.entries(ASSETS).map(([id, file]) => ({ id, file, width: images[id].width, height: images[id].height, sha256: createHash('sha256').update(fs.readFileSync(path.join(ROOT, file))).digest('hex') })),
  }, null, 2) + '\n');
}
async function stills() {
  checks.length = 0;
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
  print('black', 875, 1490, 1430, 10);
  print('white', 425, 1455, 1420, -6);
  text('내 폰이', 82, 453, 87, C.ink, { maxWidth: 780 });
  text('네 컷 사진관으로', 82, 566, 84, C.red, { maxWidth: 804 });
  identity(632);
  audit = false;
  fs.writeFileSync(path.join(OUTPUT, 'Pocket4Cut-Reference-Cover.jpg'), canvas.toBuffer('image/jpeg', 96));
  fs.writeFileSync(path.join(PROOF, 'text-safe-area-checks.json'), JSON.stringify({
    note: 'Conservative project critical-text bounds, not a universal Instagram specification. Existing app UI can extend outside the caption region.',
    bounds: { left: 80, right: 900, top: 240, bottom: 1480 }, checks,
  }, null, 2) + '\n');
  fs.writeFileSync(path.join(PROOF, 'story-used.json'), JSON.stringify(STORY, null, 2) + '\n');
  console.log(`Verified ${SHOTS.length * 2} stills and ${checks.length} text bounds. Cover: 1080x1920.`);
}
async function video() {
  const ffmpeg = process.env.FFMPEG_PATH;
  assert(ffmpeg && fs.existsSync(ffmpeg), 'Set FFMPEG_PATH to the verified executable');
  const target = path.join(BUILD, 'reference-v3-visual.mp4');
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
    duration: frames / FPS, requestedDuration: DURATION, fps: FPS, frames, width: W, height: H,
    video: 'H.264 high / yuv420p / BT.709 / faststart', elapsedSeconds: (Date.now() - started) / 1000,
    shots: SHOTS, deviceCapture: false, audio: false, errors,
  }, null, 2) + '\n');
  console.log('Visual master complete: ' + target);
}
async function main() { await init(); await stills(); if (!process.argv.includes('--stills')) await video(); }
main().catch(e => { console.error(e); process.exitCode = 1; });
