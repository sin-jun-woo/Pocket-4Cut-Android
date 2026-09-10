#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const { createHash } = require('node:crypto');
const { createCanvas, loadImage, GlobalFonts } = require('@napi-rs/canvas');

const ROOT = path.resolve(__dirname, '../../../..');
const BASE = path.resolve(__dirname, '..');
const OUT = path.join(BASE, 'deliverables');
const PROOF = path.join(BASE, 'verification');
const VISUAL = path.join(ROOT, 'build/reels-render/visual-master.mp4');
const FFMPEG = process.env.FFMPEG_PATH;
assert(FFMPEG && fs.existsSync(FFMPEG), 'Set FFMPEG_PATH.');
const FFPROBE = process.env.FFPROBE_PATH || path.join(path.dirname(FFMPEG), process.platform === 'win32' ? 'ffprobe.exe' : 'ffprobe');

function run(program, args) {
  const r = spawnSync(program, args, { encoding: 'utf8', windowsHide: true, maxBuffer: 32 * 1024 * 1024 });
  if (r.error) throw r.error;
  assert.equal(r.status, 0, `${path.basename(program)} failed: ${r.stderr}`);
  return r;
}
function ff(args) { return run(FFMPEG, ['-hide_banner', '-nostdin', ...args]); }
function hash(file) { return createHash('sha256').update(fs.readFileSync(file)).digest('hex'); }
function atoms(file) {
  const b = fs.readFileSync(file), result = [];
  for (let p = 0; p + 8 <= b.length;) {
    let size = b.readUInt32BE(p);
    const type = b.toString('ascii', p + 4, p + 8);
    if (size === 1) size = Number(b.readBigUInt64BE(p + 8));
    if (size === 0) size = b.length - p;
    assert(size >= 8 && p + size <= b.length, 'Invalid MP4 box');
    result.push({ type, position: p, size }); p += size;
  }
  return result;
}

function verify(file, name) {
  const info = JSON.parse(run(FFPROBE, ['-v', 'error', '-show_streams', '-show_format', '-of', 'json', file]).stdout);
  const v = info.streams.find(s => s.codec_type === 'video');
  const a = info.streams.find(s => s.codec_type === 'audio');
  assert.equal(v.codec_name, 'h264'); assert.equal(v.width, 1080); assert.equal(v.height, 1920);
  assert.equal(v.pix_fmt, 'yuv420p'); assert.equal(v.avg_frame_rate, '30/1'); assert.equal(Number(v.nb_frames), 900);
  assert.equal(v.color_space, 'bt709'); assert.equal(v.color_transfer, 'bt709'); assert.equal(v.color_primaries, 'bt709');
  assert.equal(a.codec_name, 'aac'); assert.equal(a.sample_rate, '48000'); assert.equal(a.channels, 2);
  assert(Math.abs(Number(info.format.duration) - 30) < 0.05, 'Container duration');
  assert(Math.abs(Number(v.duration) - Number(a.duration)) < 0.05, 'A/V duration mismatch');
  const boxes = atoms(file);
  assert(boxes.find(b => b.type === 'moov').position < boxes.find(b => b.type === 'mdat').position, 'Missing faststart');
  const decode = ff(['-v', 'error', '-xerror', '-i', file, '-map', '0:v:0', '-map', '0:a:0', '-f', 'null', '-']);
  assert.equal(decode.stderr.trim(), '', 'Decode warning/error');
  const loudness = ff(['-nostats', '-i', file, '-vn', '-af', 'ebur128=peak=true', '-f', 'null', '-']).stderr;
  const summary = loudness.slice(loudness.lastIndexOf('Summary:'));
  const lufs = Number(summary.match(/I:\s+(-?[\d.]+) LUFS/)[1]);
  const truePeak = Number(summary.match(/Peak:\s+(-?[\d.]+) dBFS/)[1]);
  assert(truePeak < -1, 'AAC true peak headroom');
  const black = ff(['-nostats', '-i', file, '-an', '-vf', 'blackdetect=d=0.1:pix_th=0.10:pic_th=0.98', '-f', 'null', '-']).stderr;
  assert(!/black_start:/.test(black), 'Unexpected black interval');
  const first = spawnSync(FFMPEG, ['-v', 'error', '-i', file, '-frames:v', '1', '-f', 'rawvideo', '-pix_fmt', 'rgb24', 'pipe:1'],
    { windowsHide: true, maxBuffer: 16 * 1024 * 1024 });
  assert.equal(first.status, 0); assert.equal(first.stdout.length, 1080 * 1920 * 3);
  const rgb = [...first.stdout.subarray(0, 3)];
  assert(rgb.every((n, i) => Math.abs(n - [200, 61, 45][i]) <= 4), 'BT.709 decoded brand color mismatch');
  return { file: name, bytes: fs.statSync(file).size, sha256: hash(file), duration: Number(info.format.duration),
    video: { codec: v.codec_name, width: v.width, height: v.height, frames: Number(v.nb_frames), fps: v.avg_frame_rate, pixelFormat: v.pix_fmt,
      colorSpace: v.color_space, colorTransfer: v.color_transfer, colorPrimaries: v.color_primaries, profile: v.profile },
    audio: { codec: a.codec_name, sampleRate: Number(a.sample_rate), channels: a.channels, duration: Number(a.duration), integratedLUFS: lufs, truePeakDbTP: truePeak },
    checks: { fullDecode: 'PASS', faststart: 'PASS', durationSync: 'PASS', unexpectedBlackIntervals: 0, firstFrameBackgroundRGB: rgb,
      containerBoxes: boxes.map(b => b.type) } };
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true }); fs.mkdirSync(PROOF, { recursive: true });
  const variants = [
    ['Pocket4Cut-Reels-30s.mp4', 'pocket4cut-original-30s.wav'],
    ['Pocket4Cut-Reels-30s-NoMusic.mp4', 'sfx-only.wav'],
  ];
  const report = [];
  for (const [name, wav] of variants) {
    const file = path.join(OUT, name);
    ff(['-v', 'warning', '-y', '-i', VISUAL, '-i', path.join(BASE, 'audio', wav), '-map', '0:v:0', '-map', '1:a:0',
      '-c:v', 'copy', '-c:a', 'aac', '-b:a', '192k', '-ar', '48000', '-ac', '2', '-af', 'volume=1dB',
      '-t', '30', '-map_metadata', '-1', '-movflags', '+faststart', file]);
    const result = verify(file, name); report.push(result);
    console.log(`${name}: ${result.duration}s / ${result.video.frames} frames / ${result.audio.integratedLUFS} LUFS / ${result.audio.truePeakDbTP} dBTP / decode PASS`);
  }
  const regular = process.env.REEL_FONT_REGULAR || path.join(process.env.WINDIR || 'C:/Windows', 'Fonts/malgun.ttf');
  assert(GlobalFonts.registerFromPath(regular, 'Pocket Regular'));
  const sheet = createCanvas(1440, 1510), ctx = sheet.getContext('2d');
  ctx.fillStyle = '#F3F0E8'; ctx.fillRect(0, 0, 1440, 1510);
  const times = [1.2, 4.2, 7.2, 11.2, 14.2, 17.2, 21.2, 25.2];
  for (let i = 0; i < times.length; i++) {
    const file = path.join(PROOF, `encoded-${String(i+1).padStart(2, '0')}.jpg`);
    // JPEG viewers assume a BT.601 YCbCr matrix; convert from the video's BT.709.
    ff(['-v', 'error', '-y', '-ss', String(times[i]), '-i', path.join(OUT, variants[0][0]), '-frames:v', '1',
      '-vf', 'scale=in_color_matrix=bt709:out_color_matrix=bt601:out_range=pc', '-q:v', '2', file]);
    ctx.drawImage(await loadImage(file), (i%4)*360+10, Math.floor(i/4)*750+10, 340, 604.44);
    ctx.fillStyle = '#1B1B19'; ctx.font = '23px "Pocket Regular"';
    ctx.fillText(`${times[i].toFixed(1)}s / encoded MP4`, (i%4)*360+12, Math.floor(i/4)*750+660);
  }
  fs.writeFileSync(path.join(PROOF, 'encoded-contact-sheet.jpg'), sheet.toBuffer('image/jpeg', 94));
  // Boundary frames catch a bad first/last picture independently of storyboard rendering.
  for (const [name, t] of [['first', 0], ['last', 29.966667]]) {
    const file = path.join(PROOF, `encoded-${name}.jpg`);
    ff(['-v', 'error', '-y', '-ss', String(t), '-i', path.join(OUT, variants[0][0]), '-frames:v', '1',
      '-vf', 'scale=in_color_matrix=bt709:out_color_matrix=bt601:out_range=pc', '-q:v', '2', file]);
    assert(fs.existsSync(file) && fs.statSync(file).size > 1000, 'Missing boundary image');
  }
  fs.writeFileSync(path.join(PROOF, 'export-validation.json'), JSON.stringify({ checkedAt: new Date().toISOString(),
    sourceRevision: '4527a05', ffmpeg: run(FFMPEG, ['-version']).stdout.split('\n')[0].trim(),
    scope: 'File-level automated checks. Not a device test, subjective listening test or Instagram upload acceptance test.', files: report }, null, 2)+'\n');
  console.log('Encoded frames and export validation saved.');
}
main().catch(e => { console.error(e); process.exitCode = 1; });
