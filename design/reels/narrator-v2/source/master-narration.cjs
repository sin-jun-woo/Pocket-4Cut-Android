#!/usr/bin/env node
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const { createHash } = require('node:crypto');
const BASE = path.resolve(__dirname, '..');
const input = path.join(BASE, 'audio/narration-original.wav');
const output = path.join(BASE, 'audio/narration-master-30s.wav');
const ffmpeg = process.env.FFMPEG_PATH;
assert(ffmpeg && fs.existsSync(ffmpeg), 'Set FFMPEG_PATH.');
const preprocess = 'highpass=f=70,acompressor=threshold=0.2:ratio=2:attack=5:release=65:makeup=1,aformat=sample_rates=48000:channel_layouts=stereo';
function run(args) {
  const r = spawnSync(ffmpeg, ['-hide_banner','-nostdin', ...args], { encoding: 'utf8', windowsHide: true, maxBuffer: 8e6 });
  if (r.error) throw r.error;
  assert.equal(r.status, 0, r.stderr);
  return r.stderr;
}
const first = run(['-i', input, '-af', `${preprocess},loudnorm=I=-16:TP=-1.8:LRA=7:print_format=json`, '-f', 'null', '-']);
const match = first.match(/\{\s*"input_i"[\s\S]*?\}/);
assert(match, 'Loudnorm measurement missing');
const m = JSON.parse(match[0]);
const normalize = `loudnorm=I=-16:TP=-1.8:LRA=7:measured_I=${m.input_i}:measured_TP=${m.input_tp}:measured_LRA=${m.input_lra}:measured_thresh=${m.input_thresh}:offset=${m.target_offset}:linear=true:print_format=json`;
const second = run(['-y', '-i', input, '-af', `${preprocess},${normalize},aresample=48000,afade=t=in:st=0:d=0.012,adelay=150:all=1,apad,atrim=duration=30`,
  '-ar','48000','-ac','2','-c:a','pcm_s24le',output]);
const report = { source: 'audio/narration-original.wav', output: 'audio/narration-master-30s.wav', music: false, addedSoundEffects: false,
  voiceTimeOffsetSeconds: 0.15, speechTimeStretch: 1, filters: [preprocess, normalize, '12ms entrance fade; 150ms start offset; silence pad to 30s'],
  target: { integratedLUFS: -16, truePeakDbTP: -1.8, durationSeconds: 30, sampleRate: 48000, channels: 2, codec: 'PCM24' },
  measuredBeforeNormalization: m, normalizationResult: JSON.parse(second.match(/\{\s*"input_i"[\s\S]*?\}/)[0]),
  bytes: fs.statSync(output).size, sha256: createHash('sha256').update(fs.readFileSync(output)).digest('hex') };
fs.mkdirSync(path.join(BASE, 'verification'), { recursive: true });
fs.writeFileSync(path.join(BASE, 'verification/narration-master.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report, null, 2));
