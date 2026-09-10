#!/usr/bin/env node
'use strict';

// Voice-led editorial demonstration, not a simulated live app recording.
// Existing Compose screenshots and existing renderer/photo assets stay unchanged.
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
const C = { paper: '#F3F0E8', ink: '#181816', white: '#FFFFFF', red: '#C83D2D', yellow: '#FFE580' };
const DEFAULT_STORY = {
  duration: 30,
  scenes: [
    { start: 0, end: 2.6, id: 'hook', lines: ['저, 네 컷', '만들었어요.'] },
    { start: 2.6, end: 4.8, id: 'reveal', lines: ['근데 사진관은', '안 갔어요.'] },
    { start: 4.8, end: 6.8, id: 'brand', lines: ['폰에서', '포켓네컷 켰어요.'] },
    { start: 6.8, end: 10.2, id: 'capture', lines: ['4컷 고르면', '8장 자동으로 찍어요.'] },
    { start: 10.2, end: 13.2, id: 'selection', lines: ['잘 나온 4장만', '골랐어요.'] },
    { start: 13.2, end: 16.0, id: 'layout', lines: ['배치도', '취향대로 골랐어요.'] },
    { start: 16.0, end: 19.0, id: 'filter', lines: ['흑백으로 하니까', '이 느낌이더라고요.'] },
    { start: 19.0, end: 22.0, id: 'save', lines: ['완성하면', '저장하고 공유해요.'] },
    { start: 22.0, end: 25.4, id: 'memory', lines: ['별일 없던 날도', '네 컷은 남겼어요.'] },
    { start: 25.4, end: 30, id: 'cta', lines: ['다음 네 컷,', '같이 만들어 봐요.'] },
  ],
};
const storyFile = process.env.NARRATION_STORY ? path.resolve(process.env.NARRATION_STORY) : path.join(__dirname, 'story-timed.json');
const STORY = fs.existsSync(storyFile)
  ? JSON.parse(fs.readFileSync(storyFile, 'utf8').replace(/^\uFEFF/, ''))
  : DEFAULT_STORY;
const SHOTS = STORY.scenes;
const DURATION = STORY.duration;
const IDS = new Set(DEFAULT_STORY.scenes.map(s => s.id));
assert(Number.isFinite(DURATION) && DURATION > 0 && DURATION <= 45, 'duration must be between 0 and 45 seconds');
assert(Array.isArray(SHOTS) && SHOTS.length > 0, 'scenes must be nonempty');
SHOTS.forEach((s, i) => {
  assert(IDS.has(s.id), 'Unsupported scene id: ' + s.id);
  assert(Number.isFinite(s.start) && Number.isFinite(s.end) && s.end > s.start, 'Invalid scene timing');
  assert(Math.abs(s.start - (i ? SHOTS[i - 1].end : 0)) < 0.00001, 'Scenes must be contiguous and start at zero');
  assert(Array.isArray(s.lines) && s.lines.length > 0 && s.lines.length <= 2 && s.lines.every(l => typeof l === 'string' && l.length > 0), 'Use one or two subtitle lines');
});
assert(Math.abs(SHOTS.at(-1).end - DURATION) < 0.00001, 'Last scene must end at duration');
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
const images = {};
const canvas = createCanvas(W, H), ctx = canvas.getContext('2d');
const checks = [];
let audit = false;
const clamp = (n, a = 0, b = 1) => Math.max(a, Math.min(b, n));
const ease = t => 1 - Math.pow(1 - clamp(t), 3);

function rect(x, y, w, h, color) {
  ctx.fillStyle = color; ctx.fillRect(x, y, w, h);
}
function font(size, bold = true) { ctx.font = `${size}px "${bold ? 'Pocket Bold' : 'Pocket Regular'}"`; }
function label(text, x, y, size, color = C.white, maxWidth = 820, bold = true, critical = true) {
  ctx.save(); font(size, bold);
  while (ctx.measureText(text).width > maxWidth && size > 26) font(--size, bold);
  const m = ctx.measureText(text);
  if (audit && critical) {
    const box = { text, x, y, size, right: x + m.width, top: y - m.actualBoundingBoxAscent, bottom: y + m.actualBoundingBoxDescent };
    assert(box.x >= 80 && box.right <= 900.5 && box.top >= 240 && box.bottom <= 1480.5, 'Unsafe critical text: ' + JSON.stringify(box));
    checks.push(box);
  }
  ctx.fillStyle = color; ctx.textBaseline = 'alphabetic'; ctx.fillText(text, x, y); ctx.restore();
}
function crop(name, x, y, w, h, dx, dy, dw, dh) {
  const im = images[name];
  assert(x >= 0 && y >= 0 && x + w <= im.width && y + h <= im.height, 'Invalid source crop ' + name);
  ctx.drawImage(im, x, y, w, h, dx, dy, dw, dh);
}
function print(name, x, y, height, angle = 0) {
  const im = images[name], width = height * im.width / im.height;
  ctx.save(); ctx.translate(x, y); ctx.rotate(angle * Math.PI / 180);
  ctx.shadowColor = '#00000050'; ctx.shadowBlur = 28; ctx.shadowOffsetY = 15;
  ctx.drawImage(im, -width / 2, -height / 2, width, height); ctx.restore();
}
function photo(name, x, y, size, angle = 0, border = 0) {
  ctx.save(); ctx.translate(x, y); ctx.rotate(angle * Math.PI / 180);
  if (border) rect(-size / 2 - border, -size / 2 - border, size + border * 2, size + border * 2, C.white);
  ctx.drawImage(images[name], -size / 2, -size / 2, size, size); ctx.restore();
}
function photoBackground(name, p = 0) {
  // Defocused duplicate is purely an editorial backdrop, not a new sample photo.
  ctx.save(); ctx.filter = 'blur(24px)';
  photo(name, 540, 960, 2040 + p * 30); ctx.restore();
  rect(0, 0, W, H, '#17171388');
}
function disclosures(dark = false) {
  rect(80, 239, 244, 46, '#111111DA');
  label('광고 · 사용 예시', 96, 271, 25, C.white, 211, false);
  rect(80, 1512, 654, 46, '#111111DB');
  label('AI 내레이션 · AI 생성 예시 사진', 96, 1545, 25, C.white, 620, false, false);
}
function subtitle(shot) {
  const sizeLimit = 57;
  const lines = shot.lines;
  let size = sizeLimit;
  font(size);
  while (lines.some(s => ctx.measureText(s).width > 750) && size > 35) font(--size);
  assert(lines.every(s => ctx.measureText(s).width <= 750), 'Subtitle is too long; split wording into two shorter lines');
  const lineH = 78, bottom = shot.id === 'cta' ? 1362 : 1458;
  const first = bottom - (lines.length - 1) * lineH;
  lines.forEach((line, i) => {
    const y = first + i * lineH;
    const width = ctx.measureText(line).width;
    rect(80, y - size - 13, width + 40, size + 29, '#111111F2');
    const parts = line.split(/(8장|4장|4컷|흑백|포켓네컷|폰)/g);
    let x = 100;
    parts.forEach(part => {
      const highlight = /^(8장|4장|4컷|흑백|포켓네컷|폰)$/.test(part);
      label(part, x, y, size, highlight ? C.yellow : C.white, 800 - (x - 100));
      font(size); x += ctx.measureText(part).width;
    });
  });
}
function appIdentity(y = 350) {
  ctx.drawImage(images.icon, 80, y, 170, 170);
  rect(273, y + 21, 610, 113, '#111111EC');
  label('Pocket4Cut', 293, y + 95, 63, C.white, 569);
}

function scene(shot, t) {
  const local = t - shot.start, p = clamp(local / (shot.end - shot.start));
  ctx.resetTransform(); ctx.globalAlpha = 1; ctx.filter = 'none';
  rect(0, 0, W, H, C.paper);
  if (shot.id === 'hook') {
    photoBackground('photo2', p);
    // The first frame already contains the outcome. The second scale is a hard cut.
    const close = p > 0.56;
    print('black', 823, 1095, close ? 1645 : 1520, 10);
    print('white', 481, close ? 970 : 1005, close ? 1610 : 1465 + ease(p * 2) * 45, -5);
  } else if (shot.id === 'reveal') {
    // Full-size crop of a genuine static Compose render; no fake device chrome.
    crop('result', 0, 520, 1080, 1750, -12 - p * 9, 260, 1104 + p * 18, 1789 + p * 30);
    rect(80, 330, 281, 60, C.red);
    label('폰으로 만든 네 컷', 96, 372, 28, C.white, 250);
  } else if (shot.id === 'brand') {
    photoBackground('photo3', p);
    photo('photo3', 525, 930, 1140 + p * 24, -2);
    appIdentity(337);
  } else if (shot.id === 'capture') {
    if (p < 0.52 && shot.variant !== 'detail' && !shot.lines.join(' ').includes('8장')) {
      crop('count', 0, 370, 1080, 1090, 0, 350, 1080, 1090);
    } else {
      // Crop is a camera-edit punch-in on existing 4 CUT state, never a tap animation.
      crop('count', 35, 750, 1010, 350, 32, 441, 1010, 350);
      photo('photo2', 385, 1069, 556, -5, 12);
      photo('photo4', 801, 1102, 501, 7, 12);
    }
  } else if (shot.id === 'selection') {
    if (p < 0.54) {
      crop('selection', 30, 335, 1020, 1480, 12, 291, 1010, 1465.49);
    } else {
      crop('selection', 30, 525, 1020, 1275, 0, 290, 1050, 1312.5);
    }
  } else if (shot.id === 'layout') {
    if (p < 0.56) {
      crop('layout', 30, 730, 1020, 1330, 12, 294, 1020, 1330);
    } else {
      crop('layout', 40, 872, 995, 590, 24, 401, 995, 590);
      // Exports stay original; showing choices does not pretend the grid is selected.
      rect(80, 1075, 666, 75, C.red);
      label('클래식 · 그리드 · 가로', 102, 1128, 43, C.white, 619);
    }
  } else if (shot.id === 'filter') {
    const bw = shot.variant === 'bw' || shot.lines.join(' ').includes('흑백');
    rect(0, 0, W, H, bw ? '#20201E' : C.paper);
    print(bw ? 'black' : 'white', 506, bw ? 924 : 662, bw ? 1510 + p * 20 : 1290, bw ? 4 : -5);
    if (!bw) {
      // Do not leave an "Original selected" control panel beneath a B&W export.
      rect(80, 985, 812, 330, C.paper);
      crop('edit', 35, 1390, 1010, 410, 80, 985, 812, 329.62);
    }
    rect(80, 321, 380, 49, '#111111E5');
    label('필터·프레임 적용 예시', 96, 355, 26, C.white, 347);
  } else if (shot.id === 'save') {
    // Keep the B&W result from the preceding filter beat through save/share.
    print('black', 525, 566, 970, -2);
    // Preserve the real save/share buttons, enlarged as an editorial detail insert.
    crop('result', 36, 1940, 1008, 260, 48, 1074, 936, 241.43);
  } else if (shot.id === 'memory') {
    const n = p < 0.30 ? 2 : p < 0.63 ? 3 : 4;
    photoBackground('photo' + n, p);
    photo('photo' + n, 513, 925, 1135 + (p % 0.3) * 26, n === 3 ? 1.5 : -1.4, 18);
  } else if (shot.id === 'cta') {
    rect(0, 0, W, H, '#D6D0C3');
    print('black', 882, 1114, 1382, 14);
    print('white', 483, 979, 1429 + p * 22, -6);
    appIdentity(333);
    rect(80, 1401, 813, 81, C.red);
    label('Google Play에서 Pocket4Cut 검색', 102, 1458, 38, C.white, 768);
  }
  subtitle(shot);
  disclosures();
}

function renderFrame(t, check = false) {
  const time = Math.min(DURATION - 0.000001, Math.max(0, t));
  const shot = SHOTS.find(s => time >= s.start && time < s.end);
  assert(shot, 'Missing scene at ' + time);
  audit = check; scene(shot, time); audit = false;
  return canvas;
}
async function init() {
  [PROOF, OUTPUT, BUILD].forEach(p => fs.mkdirSync(p, { recursive: true }));
  const regular = process.env.REEL_FONT_REGULAR || path.join(process.env.WINDIR || 'C:/Windows', 'Fonts/malgun.ttf');
  const bold = process.env.REEL_FONT_BOLD || path.join(process.env.WINDIR || 'C:/Windows', 'Fonts/malgunbd.ttf');
  assert(GlobalFonts.registerFromPath(regular, 'Pocket Regular'), 'Missing Korean font REEL_FONT_REGULAR');
  assert(GlobalFonts.registerFromPath(bold, 'Pocket Bold'), 'Missing Korean font REEL_FONT_BOLD');
  for (const [id, relative] of Object.entries(ASSETS)) images[id] = await loadImage(path.join(ROOT, relative));
  fs.writeFileSync(path.join(PROOF, 'input-manifest.json'), JSON.stringify({
    note: 'Existing AI sample photos and production Compose / CollageRenderer outputs. Editorial animation, not device capture.',
    assets: Object.entries(ASSETS).map(([id, file]) => ({ id, file, width: images[id].width, height: images[id].height, sha256: createHash('sha256').update(fs.readFileSync(path.join(ROOT, file))).digest('hex') })),
  }, null, 2) + '\n');
}
async function stills() {
  checks.length = 0;
  const columns = 5, cellW = 270, cellH = 532;
  const sheet = createCanvas(columns * cellW, Math.ceil(SHOTS.length / columns) * cellH);
  const sc = sheet.getContext('2d'); sc.fillStyle = C.paper; sc.fillRect(0, 0, sheet.width, sheet.height);
  for (let i = 0; i < SHOTS.length; i++) {
    const shot = SHOTS[i];
    for (const [suffix, portion] of [['a', 0.23], ['b', 0.73]]) {
      renderFrame(shot.start + (shot.end - shot.start) * portion, true);
      const jpeg = canvas.toBuffer('image/jpeg', 94);
      fs.writeFileSync(path.join(PROOF, `scene-${String(i + 1).padStart(2, '0')}-${shot.id}-${suffix}.jpg`), jpeg);
      if (suffix === 'a') {
        sc.drawImage(await loadImage(jpeg), (i % columns) * cellW + 5, Math.floor(i / columns) * cellH + 5, 260, 462.22);
        sc.font = '18px "Pocket Bold"'; sc.fillStyle = C.ink;
        sc.fillText(`${shot.start.toFixed(1)}–${shot.end.toFixed(1)}s ${shot.id}`, (i % columns) * cellW + 7, Math.floor(i / columns) * cellH + 500);
      }
    }
  }
  fs.writeFileSync(path.join(PROOF, 'storyboard-contact-sheet.jpg'), sheet.toBuffer('image/jpeg', 94));
  audit = false; rect(0, 0, W, H, '#CFC8B9');
  print('black', 859, 1057, 1522, 12);
  print('white', 491, 1045, 1480, -6);
  rect(80, 528, 768, 129, '#111111F5');
  label('사진관은 안 갔는데', 106, 618, 70, C.white, 712);
  rect(80, 671, 736, 129, '#111111F5');
  label('네 컷은 만들었어요', 106, 761, 68, C.yellow, 684);
  rect(80, 1380, 621, 100, '#111111F5');
  label('Pocket4Cut', 105, 1454, 68, C.white, 571);
  rect(80, 1512, 505, 46, '#111111E5');
  label('광고 · AI 생성 예시 사진', 96, 1544, 25, C.white, 472);
  fs.writeFileSync(path.join(OUTPUT, 'Pocket4Cut-Narrator-Cover.jpg'), canvas.toBuffer('image/jpeg', 96));
  fs.writeFileSync(path.join(PROOF, 'text-safe-area-checks.json'), JSON.stringify({
    note: 'Conservative project caption bounds, not a universal platform specification; source UI is intentionally cropped and may extend outside.',
    bounds: { left: 80, right: 900, top: 240, bottom: 1480 }, checks,
  }, null, 2) + '\n');
  fs.writeFileSync(path.join(PROOF, 'story-used.json'), JSON.stringify(STORY, null, 2) + '\n');
  console.log(`Verified ${SHOTS.length * 2} scene stills, ${checks.length} text bounds and cover.`);
}
async function video() {
  const ffmpeg = process.env.FFMPEG_PATH;
  assert(ffmpeg && fs.existsSync(ffmpeg), 'Set FFMPEG_PATH to the verified executable');
  const target = path.join(BUILD, 'narrator-v2-visual.mp4');
  const args = ['-hide_banner', '-loglevel', 'warning', '-y', '-f', 'rawvideo', '-pixel_format', 'rgba', '-video_size', `${W}x${H}`, '-framerate', String(FPS), '-i', 'pipe:0',
    '-an', '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', '-pix_fmt', 'yuv420p', '-vf', 'scale=in_range=pc:out_range=tv:out_color_matrix=bt709,setparams=range=limited:color_primaries=bt709:color_trc=bt709:colorspace=bt709',
    '-colorspace', 'bt709', '-color_primaries', 'bt709', '-color_trc', 'bt709', '-color_range', 'tv', '-profile:v', 'high', '-level:v', '4.2', '-threads', '6', '-movflags', '+faststart', target];
  const encoder = spawn(ffmpeg, args, { stdio: ['pipe', 'ignore', 'pipe'], windowsHide: true });
  let errors = ''; encoder.stderr.on('data', b => { errors = (errors + b.toString()).slice(-20000); });
  const finished = new Promise((resolve, reject) => {
    encoder.once('error', reject); encoder.once('close', code => code === 0 ? resolve() : reject(new Error(`FFmpeg exit ${code}: ${errors}`)));
  });
  const started = Date.now(), frames = Math.ceil(DURATION * FPS);
  for (let f = 0; f < frames; f++) {
    renderFrame(f / FPS);
    const frame = Buffer.from(canvas.data()); // Own the bytes until stdin flushes.
    if (!encoder.stdin.write(frame)) await once(encoder.stdin, 'drain');
    if (f % 90 === 0) console.log(`Rendered ${f}/${frames} frames (${((Date.now() - started) / 1000).toFixed(1)}s)`);
  }
  encoder.stdin.end(); await finished;
  fs.writeFileSync(path.join(PROOF, 'render-manifest.json'), JSON.stringify({
    duration: frames / FPS, requestedDuration: DURATION, fps: FPS, frames, width: W, height: H, video: 'H.264 high / yuv420p / BT.709 / faststart',
    elapsedSeconds: (Date.now() - started) / 1000, shots: SHOTS, deviceCapture: false, audio: false, errors,
  }, null, 2) + '\n');
  console.log('Visual master complete: ' + target);
}
async function main() { await init(); await stills(); if (!process.argv.includes('--stills')) await video(); }
main().catch(e => { console.error(e); process.exitCode = 1; });
