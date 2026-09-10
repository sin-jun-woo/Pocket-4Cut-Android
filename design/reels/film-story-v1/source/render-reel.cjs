#!/usr/bin/env node
'use strict';

// Film Story: deterministic 30-second editorial animation of verified app renders.
// No app screen, photograph, icon, or final collage is fabricated by this renderer.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { once } = require('node:events');
const { spawn } = require('node:child_process');
const { createHash } = require('node:crypto');
const { createCanvas, loadImage, GlobalFonts } = require('@napi-rs/canvas');

const ROOT = path.resolve(__dirname, '../../../..');
const BASE = path.resolve(__dirname, '..');
const OUTPUT = path.join(BASE, 'deliverables');
const PROOF = path.join(BASE, 'verification');
const BUILD = path.join(ROOT, 'build/reels-render');
const W = 1080, H = 1920, FPS = 30, DURATION = 30;
const C = { paper: '#F3F0E8', white: '#FBFAF6', ink: '#1B1B19', red: '#C83D2D', line: '#D6D0C5', muted: '#746F67' };
const SHOTS = [
  { start: 0, end: 3, id: 'hook', title: ['네 컷 찍으러', '어디 가?'], subtitle: '사진관 감성, 내 손안에.' },
  { start: 3, end: 6, id: 'capture', title: ['내 폰이', '네 컷 부스.'], subtitle: '4컷 선택 → 8장 자동 촬영' },
  { start: 6, end: 10, id: 'selection', title: ['잘 나온 컷만,', '쏙.'], subtitle: '8장 중 마음에 드는 4장을 골라요.' },
  { start: 10, end: 13, id: 'layout', title: ['길게? 나란히?', '배치도 내 취향.'], subtitle: '클래식 · 그리드 · 가로 레이아웃' },
  { start: 13, end: 16, id: 'color', title: ['프레임 색도', '나답게.'], subtitle: '사진에 어울리는 색을 골라요.' },
  { start: 16, end: 20, id: 'filter', title: ['같은 사진,', '다른 분위기.'], subtitle: '원본 · 소프트 · 필름 · 흑백' },
  { start: 20, end: 24, id: 'save', title: ['오늘의 우리,', '한 장으로.'], subtitle: '완성한 사진은 저장하고 공유해요.' },
  { start: 24, end: 30, id: 'cta', title: ['다음 네 컷은', '지금 여기서.'], subtitle: 'Google Play에서 Pocket4Cut 검색' },
];
const ASSETS = {
  icon: 'design/play-store/film-strip-v4/app-icon-master-1024.png',
  count: 'design/play-store/print-booth-v1/raw-screenshots/frame_count.png',
  selection: 'design/play-store/print-booth-v1/raw-screenshots/selection.png',
  layout: 'design/play-store/print-booth-v1/raw-screenshots/layout.png',
  color: 'design/play-store/print-booth-v1/raw-screenshots/color.png',
  edit: 'design/play-store/print-booth-v1/raw-screenshots/edit.png',
  result: 'design/play-store/print-booth-v1/raw-screenshots/result.png',
  white: 'design/play-store/print-booth-v1/capture/fixtures/store-demo-result-white.jpg',
  black: 'design/play-store/print-booth-v1/capture/fixtures/store-demo-result-black.jpg',
  blush: 'design/play-store/print-booth-v1/capture/fixtures/store-demo-result-blush.jpg',
  sky: 'design/play-store/print-booth-v1/capture/fixtures/store-demo-result-skyblue.jpg',
  photo1: 'design/play-store/print-booth-v1/demo-photos/demo_01.jpg',
  photo2: 'design/play-store/print-booth-v1/demo-photos/demo_02.jpg',
  photo3: 'design/play-store/print-booth-v1/demo-photos/demo_03.jpg',
  photo4: 'design/play-store/print-booth-v1/demo-photos/demo_04.jpg',
};
const images = {};
const canvas = createCanvas(W, H), ctx = canvas.getContext('2d');
const textChecks = [];
let auditText = false;
const clamp = (n, a = 0, b = 1) => Math.max(a, Math.min(b, n));
const lerp = (a, b, t) => a + (b - a) * t;
const out = t => 1 - Math.pow(1 - clamp(t), 3);
const smooth = t => { t = clamp(t); return t * t * (3 - 2 * t); };

function label(text, x, y, size = 28, color = C.muted, bold = false, maxWidth = 900 - x, audit = true) {
  ctx.save();
  ctx.fillStyle = color; ctx.textBaseline = 'alphabetic';
  ctx.font = `${size}px "${bold ? 'Pocket Bold' : 'Pocket Regular'}"`;
  while (ctx.measureText(text).width > maxWidth && size > 22) {
    size -= 1; ctx.font = `${size}px "${bold ? 'Pocket Bold' : 'Pocket Regular'}"`;
  }
  const m = ctx.measureText(text);
  if (auditText && audit) {
    const bounds = { text, x, y, size, right: x + m.width, top: y - m.actualBoundingBoxAscent, bottom: y + m.actualBoundingBoxDescent };
    assert(bounds.x >= 68 && bounds.right <= 920.5, 'Critical text outside horizontal safe area: ' + text);
    assert(bounds.top >= 150 && bounds.bottom <= 1510, 'Critical text outside vertical safe area: ' + text);
    textChecks.push(bounds);
  }
  ctx.fillText(text, x, y); ctx.restore();
}

function paperRect(x, y, w, h, fill, shadow = false) {
  ctx.save();
  if (shadow) { ctx.shadowColor = '#1B1B1925'; ctx.shadowBlur = 24; ctx.shadowOffsetY = 14; }
  ctx.fillStyle = fill; ctx.fillRect(x, y, w, h); ctx.restore();
}

function drawPrint(name, cx, cy, h, angle = 0, alpha = 1) {
  const im = images[name], w = h * im.width / im.height;
  ctx.save(); ctx.globalAlpha *= alpha;
  ctx.translate(cx, cy); ctx.rotate(angle * Math.PI / 180);
  ctx.shadowColor = '#17171335'; ctx.shadowBlur = 30; ctx.shadowOffsetY = 20;
  ctx.drawImage(im, -w / 2, -h / 2, w, h); ctx.restore();
}

function photoCard(name, cx, cy, size, angle, alpha = 1) {
  ctx.save(); ctx.globalAlpha *= alpha; ctx.translate(cx, cy); ctx.rotate(angle * Math.PI / 180);
  paperRect(-size / 2 - 12, -size / 2 - 12, size + 24, size + 24, C.white, true);
  ctx.drawImage(images[name], -size / 2, -size / 2, size, size); ctx.restore();
}

function crop(name, source, dest, shadow = true) {
  const im = images[name];
  assert(source.x >= 0 && source.y >= 0 && source.x + source.w <= im.width && source.y + source.h <= im.height, 'Invalid crop ' + name);
  ctx.save();
  paperRect(dest.x - 3, dest.y - 3, dest.w + 6, dest.h + 6, C.line, shadow);
  ctx.drawImage(im, source.x, source.y, source.w, source.h, dest.x, dest.y, dest.w, dest.h);
  ctx.restore();
}

function ring(x, y, t, start, r = 40, color = C.red) {
  const p = clamp((t - start) / 0.6);
  if (t < start || p >= 1) return;
  ctx.save(); ctx.globalAlpha = (1 - p) * 0.9; ctx.strokeStyle = color;
  ctx.lineWidth = 5 * (1 - p) + 1; ctx.beginPath(); ctx.arc(x, y, r + p * 24, 0, Math.PI * 2); ctx.stroke(); ctx.restore();
}

function topbar(dark, step = '') {
  const fg = dark ? C.white : C.ink;
  label('POCKET / 4CUT', 80, 203, 27, fg, true);
  if (step) label(step, 668, 203, 23, dark ? '#F2C2B7' : C.muted, false, 230);
  ctx.strokeStyle = dark ? '#E57E6C' : C.line; ctx.lineWidth = 1;
  ctx.beginPath(); ctx.moveTo(80, 239); ctx.lineTo(900, 239); ctx.stroke();
}

function footer(t, dark) {
  // Progress is decorative; core captions remain above the bottom/right platform UI.
  paperRect(80, 1580, 820, 3, dark ? '#B23427' : C.line);
  paperRect(80, 1580, 820 * clamp(t / DURATION), 3, dark ? C.white : C.red);
  label('앱 화면 시연 · AI 생성 예시 사진', 80, 1560, 22, dark ? '#F8D7CE' : '#79736B', false, 820, false);
}

function headings(shot, local, dark, size = 94) {
  // Keep text immediately legible; only a small settling movement, no blank intro.
  const dy = 15 * (1 - out(local / 0.35));
  shot.title.forEach((s, i) => label(s, 80, 355 + 112 * i + dy, size, dark ? C.white : C.ink, true, 820));
  if (shot.id !== 'hook' && shot.id !== 'cta') label(shot.subtitle, 83, 535 + dy, 33, C.muted, false, 817);
}

function scene(shot, t) {
  const local = t - shot.start, p = clamp(local / (shot.end - shot.start));
  const dark = shot.id === 'hook' || shot.id === 'cta';
  paperRect(0, 0, W, H, dark ? C.red : C.paper);
  if (!dark) {
    paperRect(977, 0, 103, H, '#EAE5DA');
    // Restrained print-shop register line, not an imitation film frame around the UI.
    paperRect(43, 298, 7, 170, C.red);
  }
  topbar(dark, ({capture:'01 / 찍고',selection:'02 / 고르고',layout:'03 / 꾸미고',color:'03 / 꾸미고',filter:'03 / 꾸미고',save:'04 / 간직하고'})[shot.id] || '');
  headings(shot, local, dark, shot.id === 'layout' ? 86 : 94);

  if (shot.id === 'hook') {
    // Actual CollageRenderer exports lead from frame zero; photo bursts follow the beat.
    drawPrint('black', 704 + 14 * Math.sin(local), 997, 714, 10 - 2 * p);
    drawPrint('white', 439 + 14 * Math.sin(local * 0.8), 988, 828 + 20 * out(local / 0.7), -8 + p * 3);
    if (local > 0.45 && local < 2.05) {
      const n = Math.min(3, Math.floor((local - 0.45) / 0.5));
      const k = ((local - 0.45) % 0.5) / 0.5;
      photoCard('photo' + (n + 1), 745, 780 + 12 * out(k), 230, 7, 0.9 * (1 - smooth((k - 0.65) / 0.35)));
    }
    label(shot.subtitle, 80, 1490, 35, C.white, true, 820);
  } else if (shot.id === 'capture') {
    const d = { x: 86, y: 603 + 14 * (1 - out(local / 0.5)), w: 808, h: 840 };
    crop('count', { x: 40, y: 425, w: 1000, h: 1040 }, d);
    ring(492, 938, local, 0.6, 122); ring(492, 938, local, 1.4, 122);
    label('표정만 준비하면 돼요.', 83, 1500, 31, C.ink, true, 817);
  } else if (shot.id === 'selection') {
    const d = { x: 128, y: 572, w: 734, h: 939.26 };
    crop('selection', { x: 0, y: 335, w: 1080, h: 1382 }, d);
    const points = [[448,596],[942,596],[448,1257],[942,1257]];
    points.forEach(([x,y], i) => ring(d.x+x/1080*d.w,d.y+(y-335)/1382*d.h,local,0.35+i*0.6,29));
  } else if (shot.id === 'layout') {
    // Match-cut from four selected faces into the existing three-layout selector.
    crop('layout', { x: 35, y: 710, w: 1010, h: 1340 }, { x: 146, y: 578, w: 700, h: 928.71 });
    ring(397, 827, local, 0.55, 110);
  } else if (shot.id === 'color') {
    drawPrint('sky', 303, 835 + 8 * Math.sin(local * 1.4), 545, -7);
    drawPrint('blush', 583, 837 - 8 * Math.sin(local * 1.4), 568, 7);
    crop('color', { x: 40, y: 1840, w: 995, h: 255 }, { x: 83, y: 1180, w: 811, h: 207.84 });
    label('프레임 색상 선택 화면', 83, 1430, 28, C.muted, false, 817);
    label('취향이 달라도, 같이 예쁘게.', 83, 1495, 31, C.ink, true, 817);
  } else if (shot.id === 'filter') {
    const shift = 12 * Math.sin(local * 0.65);
    drawPrint('white', 314 + shift, 812, 507, -5);
    drawPrint('black', 618 - shift, 812, 507, 5);
    label('원본', 252, 1110, 26, C.muted, false, 200);
    label('흑백', 566, 1110, 26, C.muted, false, 200);
    crop('edit', { x: 35, y: 1370, w: 1010, h: 430 }, { x: 83, y: 1163, w: 811, h: 345.28 });
  } else if (shot.id === 'save') {
    drawPrint('blush', 660, 883, 591, 10);
    drawPrint('white', 418, 880 + 9 * Math.sin(local), 677, -5);
    crop('result', { x: 36, y: 1940, w: 1008, h: 260 }, { x: 83, y: 1271, w: 811, h: 209.19 });
  } else if (shot.id === 'cta') {
    drawPrint('black', 717, 832, 570, 12);
    drawPrint('white', 436, 829 + 10 * Math.sin(local * 0.8), 610, -7);
    ctx.save(); ctx.beginPath(); ctx.roundRect(80, 1198, 148, 148, 22); ctx.clip();
    ctx.drawImage(images.icon, 80, 1198, 148, 148); ctx.restore();
    label('Pocket4Cut', 257, 1255, 70, C.white, true, 643);
    label('내 손안의 네 컷 사진관', 259, 1321, 32, C.white, false, 641);
    paperRect(80, 1400, 820, 100, C.white);
    label(shot.subtitle, 110, 1465, 36, C.ink, true, 760);
  }

  footer(t, dark);

  // A quick paper-edge wipe between editorial scenes, never a repeated strobe.
  if (local < 0.20 && shot.start > 0) {
    ctx.save(); ctx.fillStyle = C.white;
    const x = W * out(local / 0.20);
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x + 110, H); ctx.lineTo(W + 130, H); ctx.lineTo(W + 130, 0); ctx.closePath(); ctx.fill(); ctx.restore();
  }
}

function renderFrame(t, audit = false) {
  t = Math.min(DURATION - 1 / FPS, Math.max(0, t));
  const shot = SHOTS.find(s => t >= s.start && t < s.end);
  assert(shot); auditText = audit; ctx.resetTransform(); ctx.globalAlpha = 1;
  ctx.imageSmoothingEnabled = true; ctx.imageSmoothingQuality = 'high';
  scene(shot, t); auditText = false;
  return canvas;
}

async function init() {
  fs.mkdirSync(OUTPUT, { recursive: true }); fs.mkdirSync(PROOF, { recursive: true }); fs.mkdirSync(BUILD, { recursive: true });
  const regular = process.env.REEL_FONT_REGULAR || path.join(process.env.WINDIR || 'C:/Windows', 'Fonts/malgun.ttf');
  const bold = process.env.REEL_FONT_BOLD || path.join(process.env.WINDIR || 'C:/Windows', 'Fonts/malgunbd.ttf');
  assert(GlobalFonts.registerFromPath(regular, 'Pocket Regular'), 'Missing Korean regular font; set REEL_FONT_REGULAR');
  assert(GlobalFonts.registerFromPath(bold, 'Pocket Bold'), 'Missing Korean bold font; set REEL_FONT_BOLD');
  for (const [id, relative] of Object.entries(ASSETS)) images[id] = await loadImage(path.join(ROOT, relative));
  const manifest = Object.entries(ASSETS).map(([id, file]) => ({ id, file, width: images[id].width, height: images[id].height,
    sha256: createHash('sha256').update(fs.readFileSync(path.join(ROOT, file))).digest('hex') }));
  fs.writeFileSync(path.join(PROOF, 'input-manifest.json'), JSON.stringify({ reuse: 'Existing image_gen sample photos and production Compose/CollageRenderer outputs', assets: manifest }, null, 2) + '\n');
}

async function stills() {
  const sheet = createCanvas(1440, 1490), sc = sheet.getContext('2d');
  sc.fillStyle = C.paper; sc.fillRect(0, 0, sheet.width, sheet.height);
  for (let i = 0; i < SHOTS.length; i++) {
    const t = SHOTS[i].start + Math.min(1.2, (SHOTS[i].end-SHOTS[i].start)/2);
    renderFrame(t, true);
    const jpeg = canvas.toBuffer('image/jpeg', 93);
    fs.writeFileSync(path.join(PROOF, `scene-${String(i+1).padStart(2,'0')}-${SHOTS[i].id}.jpg`), jpeg);
    // Use an immutable snapshot: another canvas can retain a live drawing surface.
    sc.drawImage(await loadImage(jpeg), (i%4)*360+10, Math.floor(i/4)*730+12, 340, 604.44);
    sc.font = '24px "Pocket Bold"'; sc.fillStyle = C.ink;
    sc.fillText(`${String(SHOTS[i].start).padStart(2,'0')}–${SHOTS[i].end}s / ${SHOTS[i].id}`, (i%4)*360+14, Math.floor(i/4)*730+660);
  }
  fs.writeFileSync(path.join(PROOF, 'storyboard-contact-sheet.jpg'), sheet.toBuffer('image/jpeg', 93));
  // Cover uses the same existing artwork with a grid-safe title area.
  paperRect(0,0,W,H,C.red);
  drawPrint('black',723,1100,650,11);
  drawPrint('white',440,1100,705,-7);
  label('내 폰이',80,557,103,C.white,true,820,false);
  label('네 컷 부스.',80,682,103,C.white,true,820,false);
  label('Pocket4Cut',80,1550,62,C.white,true,820,false);
  fs.writeFileSync(path.join(OUTPUT,'Pocket4Cut-Reels-Cover.jpg'),canvas.toBuffer('image/jpeg',96));
  fs.writeFileSync(path.join(PROOF,'text-safe-area-checks.json'),JSON.stringify({ note:'Custom conservative title box, not a claimed universal Instagram safe zone. Editorial images may extend beyond it.', bounds:{left:68,right:920,top:150,bottom:1510}, checks:textChecks },null,2)+'\n');
  console.log('Storyboard, cover and text bounds verified.');
}

async function video() {
  const ffmpeg = process.env.FFMPEG_PATH;
  assert(ffmpeg && fs.existsSync(ffmpeg),'Set FFMPEG_PATH to a verified FFmpeg executable.');
  const target = path.join(BUILD,'visual-master.mp4');
  const args = ['-hide_banner','-loglevel','warning','-y','-f','rawvideo','-pixel_format','rgba','-video_size',`${W}x${H}`,'-framerate',String(FPS),'-i','pipe:0',
    '-an','-c:v','libx264','-preset','medium','-crf','18','-pix_fmt','yuv420p','-vf','scale=in_range=pc:out_range=tv:out_color_matrix=bt709,setparams=range=limited:color_primaries=bt709:color_trc=bt709:colorspace=bt709',
    '-colorspace','bt709','-color_primaries','bt709','-color_trc','bt709','-color_range','tv','-profile:v','high','-level:v','4.2','-threads','6','-movflags','+faststart',target];
  const encoder = spawn(ffmpeg,args,{stdio:['pipe','ignore','pipe'],windowsHide:true});
  let errors=''; encoder.stderr.on('data',b=>{errors=(errors+b.toString()).slice(-20000);});
  const finished = new Promise((resolve,reject)=>{encoder.once('error',reject);encoder.once('close',code=>code===0?resolve():reject(new Error(`FFmpeg exit ${code}: ${errors}`)));});
  const start = Date.now();
  for(let f=0;f<DURATION*FPS;f++) {
    renderFrame(f/FPS);
    // Native data() aliases the drawing surface; own every frame until pipe flush.
    const frame = Buffer.from(canvas.data());
    if(!encoder.stdin.write(frame)) await once(encoder.stdin,'drain');
    if(f%90===0)console.log(`Rendered ${f}/${DURATION*FPS} frames (${((Date.now()-start)/1000).toFixed(1)}s)`);
  }
  encoder.stdin.end(); await finished;
  fs.writeFileSync(path.join(PROOF,'render-manifest.json'),JSON.stringify({duration:DURATION,fps:FPS,frames:DURATION*FPS,width:W,height:H,
    video:'H.264 high / yuv420p / BT.709 / faststart',elapsedSeconds:(Date.now()-start)/1000,shots:SHOTS,sourceRevision:'4527a05',deviceCapture:false,errors},null,2)+'\n');
  console.log('Visual master complete: '+target);
}

async function main(){await init();await stills();if(!process.argv.includes('--stills'))await video();}
main().catch(e=>{console.error(e);process.exitCode=1;});
