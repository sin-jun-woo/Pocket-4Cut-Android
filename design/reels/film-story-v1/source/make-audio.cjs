#!/usr/bin/env node
'use strict';

// Pocket4Cut — "Four little moments", a deterministic procedural original.
// No external recordings, music, MIDI files, samples, or dependencies are used.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const RATE = 48000;
const SECONDS = 30;
const LENGTH = RATE * SECONDS;
const BPM = 120;
const BEAT = 60 / BPM;
const TAU = Math.PI * 2;
const outDir = path.resolve(__dirname, '../audio');
const bus = () => [new Float64Array(LENGTH), new Float64Array(LENGTH)];
const music = bus();
const effects = bus();
const freq = midi => 440 * 2 ** ((midi - 69) / 12);
const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
const db = value => 20 * Math.log10(Math.max(1e-15, value));

function seeded(seed) {
  let value = seed >>> 0;
  return () => {
    value ^= value << 13;
    value ^= value >>> 17;
    value ^= value << 5;
    return ((value >>> 0) / 4294967296) * 2 - 1;
  };
}

function voice(target, at, duration, amplitude, pan, synth) {
  const offset = Math.round(at * RATE);
  const count = Math.min(Math.round(duration * RATE), LENGTH - offset);
  const left = Math.cos((pan + 1) * Math.PI / 4);
  const right = Math.sin((pan + 1) * Math.PI / 4);
  for (let i = 0; i < count; i++) {
    const pos = offset + i;
    if (pos < 0) continue;
    const sample = synth(i / RATE, i) * amplitude;
    if (!Number.isFinite(sample)) throw new Error(`Non-finite synthesis at ${pos}`);
    target[0][pos] += sample * left;
    target[1][pos] += sample * right;
  }
}

function piano(at, midi, velocity = 0.1, pan = 0, sustain = 1.6) {
  const f = freq(midi);
  voice(music, at, sustain, velocity, pan, (t) => {
    const attack = 1 - Math.exp(-t / 0.004);
    const release = clamp((sustain - t) / 0.16, 0, 1);
    const body = Math.exp(-t / 0.66);
    const bell = 0.50 * Math.exp(-t / 0.18);
    const phase = TAU * f * t;
    const tine = Math.sin(phase + bell * Math.sin(phase * 3.003));
    const warm = 0.20 * Math.sin(phase * 2.001) * Math.exp(-t / 0.23);
    const detune = 0.18 * Math.sin(phase * 0.9985 + 0.10 * Math.sin(TAU * 0.7 * t));
    return (tine + warm + detune) * attack * body * release;
  });
}

function bass(at, midi, velocity = 0.18, duration = 0.34) {
  const f = freq(midi);
  voice(music, at, duration, velocity, 0, (t) => {
    const attack = 1 - Math.exp(-t / 0.009);
    const release = clamp((duration - t) / 0.08, 0, 1);
    const phase = TAU * f * t;
    const wood = 0.21 * Math.sin(phase * 2) * Math.exp(-t / 0.12);
    const hollow = 0.055 * Math.sin(phase * 3) * Math.exp(-t / 0.08);
    return (Math.sin(phase) + wood + hollow) * attack * release * Math.exp(-t / 0.5);
  });
}

function kick(at, velocity = 0.30) {
  const noise = seeded(Math.round(at * 100000) + 703);
  voice(music, at, 0.32, velocity, 0, (t) => {
    // Integrated downward pitch sweep; a short, rounded, non-clicking transient.
    const phase = TAU * (48 * t + 70 * 0.022 * (1 - Math.exp(-t / 0.022)));
    const attack = 1 - Math.exp(-t / 0.0014);
    const shell = Math.sin(phase) * Math.exp(-t / 0.080);
    const tap = noise() * Math.exp(-t / 0.002) * 0.10;
    return (shell + tap) * attack * clamp((0.32 - t) / 0.025, 0, 1);
  });
}

function snare(at, velocity = 0.10, ghost = false) {
  const noise = seeded(Math.round(at * 100000) + 9901);
  let low = 0;
  let smooth = 0;
  voice(music, at, ghost ? 0.105 : 0.19, velocity, -0.025, (t) => {
    const n = noise();
    low += 0.045 * (n - low);
    smooth += 0.42 * (n - low - smooth);
    const body = Math.sin(TAU * 184 * t) * Math.exp(-t / 0.032) * 0.37;
    const clap = Math.exp(-t / 0.036) + 0.38 * Math.exp(-Math.abs(t - 0.013) / 0.005);
    return (smooth * clap + body) * (1 - Math.exp(-t / 0.0009));
  });
}

function hat(at, velocity = 0.045, open = false, pan = 0.2) {
  const noise = seeded(Math.round(at * 100000) + 53447);
  let low = 0;
  let dark = 0;
  const duration = open ? 0.20 : 0.062;
  voice(music, at, duration, velocity, pan, (t) => {
    const n = noise();
    low += 0.20 * (n - low);
    dark += 0.55 * (n - low - dark);
    const env = (1 - Math.exp(-t / 0.0008)) * Math.exp(-t / (open ? 0.054 : 0.018));
    return dark * env * clamp((duration - t) / 0.014, 0, 1);
  });
}

function rim(at, velocity = 0.035) {
  voice(music, at, 0.065, velocity, -0.26, (t) => {
    const tones = Math.sin(TAU * 780 * t) + 0.46 * Math.sin(TAU * 1275 * t);
    return tones * (1 - Math.exp(-t / 0.0005)) * Math.exp(-t / 0.006);
  });
}

function shutter(at, velocity = 0.095, pan = 0) {
  const noise = seeded(Math.round(at * 100000) + 0x341a);
  let low = 0;
  let band = 0;
  voice(effects, at, 0.15, velocity, pan, (t) => {
    const n = noise();
    low += 0.065 * (n - low);
    band += 0.28 * (n - low - band);
    const first = Math.exp(-t / 0.008);
    const second = t >= 0.036 ? 0.78 * Math.exp(-(t - 0.036) / 0.011) : 0;
    const spring = 0.15 * Math.sin(TAU * 650 * t) * Math.exp(-t / 0.019);
    const envelope = (1 - Math.exp(-t / 0.0008)) * clamp((0.15 - t) / 0.02, 0, 1);
    return (band * (first + second) + spring) * envelope;
  });
}

function paper(at, velocity = 0.06, pan = 0) {
  const noise = seeded(Math.round(at * 100000) + 0x552b);
  let low = 0;
  let band = 0;
  voice(effects, at, 0.27, velocity, pan, (t) => {
    const n = noise();
    low += 0.014 * (n - low);
    band += 0.20 * (n - low - band);
    const rise = Math.min(1, t / 0.038);
    const env = rise * Math.exp(-t / 0.058) * clamp((0.27 - t) / 0.04, 0, 1);
    return band * env * (0.8 + 0.2 * Math.sin(TAU * 52 * t));
  });
}

// Open ninth voicings: the upper notes leave space above the low, monophonic bass.
const harmony = [
  { name: 'Dmaj9', notes: [54, 61, 64, 69], root: 38 },
  { name: 'Bm9', notes: [54, 57, 61, 66], root: 35 },
  { name: 'Em9', notes: [55, 62, 66, 71], root: 40 },
  { name: 'A13', notes: [55, 61, 66, 71], root: 33 },
];

for (let bar = 0; bar < 15; bar++) {
  const start = bar * 2;
  const finalBar = bar === 14;
  const cta = bar >= 12;
  const chord = finalBar ? harmony[0] : harmony[bar % 4];
  const chordTimes = finalBar ? [0.04] : cta ? [0.05, 1.30] : [0.04, 0.82, 1.53];
  chordTimes.forEach((position, chordIndex) => {
    const vel = (cta ? 0.083 : 0.077) * (chordIndex === 1 ? 0.75 : 1);
    chord.notes.forEach((note, j) => {
      piano(start + position + j * 0.008, note, vel * (1 - j * 0.065), (j - 1.5) * 0.23, finalBar ? 1.96 : 1.4);
    });
  });
  if (finalBar) {
    bass(start + 0.04, 38, 0.105, 0.70);
    piano(start + 0.07, 74, 0.062, 0.13, 1.86);
    continue;
  }

  const kicks = cta ? [0] : (bar % 2 === 0 ? [0, 1.75, 2.50] : [0, 1.50, 2.75]);
  kicks.forEach((beat, j) => {
    const at = start + beat * BEAT + (bar === 0 && j === 0 ? 0.04 : 0);
    kick(at, (j === 0 ? 0.30 : 0.25) * (cta ? 0.72 : 1));
  });
  const basses = cta ? [0.04, 1.34] : [0.04, 0.85, 1.32, 1.79];
  basses.forEach((position, j) => {
    const lastPickup = !cta && j === 3;
    const note = lastPickup && bar % 4 === 3 ? 37 : chord.root + (lastPickup ? 12 : 0);
    bass(start + position, note, (cta ? 0.13 : 0.19) * (lastPickup ? 0.65 : 1), lastPickup ? 0.17 : 0.34);
  });
  if (cta) {
    snare(start + 0.51, 0.061);
    hat(start + 0.28, 0.024, false, 0.16);
    hat(start + 1.28, 0.022, false, -0.16);
    continue;
  }
  snare(start + 0.503, 0.116);
  snare(start + 1.505, 0.110);
  if (bar > 0 && bar % 2 === 1) snare(start + 1.36, 0.032, true);
  for (let eighth = 0; eighth < 8; eighth++) {
    const offbeat = eighth % 2 === 1;
    const at = start + eighth * 0.25 + (offbeat ? 0.029 : 0.006);
    const accented = eighth === 3 || eighth === 7;
    hat(at, offbeat ? 0.057 : 0.034, accented && bar > 0, offbeat ? 0.27 : -0.18);
  }
  if (bar % 2 === 0) rim(start + 1.145, 0.043);
  if (bar % 4 === 3) {
    hat(start + 1.88, 0.026, false, -0.25);
    rim(start + 1.90, 0.024);
  }
}

// A small answering motif, not a continuously busy lead over the instructional text.
const motif = [
  [2.28, 73, 0.057], [2.80, 69, 0.062], [3.56, 66, 0.050],
  [6.29, 71, 0.056], [6.81, 69, 0.051], [7.56, 66, 0.044],
  [10.28, 73, 0.060], [10.79, 74, 0.051], [11.54, 69, 0.048],
  [14.28, 71, 0.055], [14.80, 69, 0.050], [15.56, 66, 0.044],
  [18.28, 73, 0.057], [18.80, 69, 0.059], [19.55, 66, 0.046],
  [22.29, 71, 0.047], [22.81, 69, 0.043], [23.52, 66, 0.037],
];
motif.forEach(([at, note, velocity], i) => piano(at, note, velocity, i % 2 === 0 ? -0.13 : 0.18, 1.10));

// Edit points: the first four frames are short mechanical shutters; later cuts
// alternate paper movement and shutter punctuation, without licensed samples.
[0.5, 1, 1.5, 3, 10, 16, 24].forEach((at, i) => shutter(at, i < 3 ? 0.135 : 0.11, i % 2 === 0 ? -0.10 : 0.10));
[6, 13, 20].forEach((at, i) => paper(at, 0.115, i % 2 === 0 ? 0.12 : -0.12));

function ambience(target) {
  // Four damped feedback lines. No convolution impulse or other recorded input.
  const lengths = [0.0437, 0.0593, 0.0797, 0.1019].map(t => Math.round(t * RATE));
  const lines = lengths.map(n => new Float64Array(n));
  const states = [0, 0, 0, 0];
  for (let i = 0; i < LENGTH; i++) {
    const input = (target[0][i] + target[1][i]) * 0.5;
    let left = 0;
    let right = 0;
    for (let k = 0; k < lines.length; k++) {
      const line = lines[k];
      const position = i % line.length;
      const delayed = line[position];
      states[k] += 0.18 * (delayed - states[k]);
      line[position] = input * 0.17 + states[k] * 0.46;
      if (k % 2 === 0) left += delayed;
      else right += delayed;
    }
    target[0][i] += left * 0.18;
    target[1][i] += right * 0.18;
  }
}
ambience(music);

function finish(target) {
  // DC rejection, a gently darkened top, short start fade and a genuine tail.
  for (let channel = 0; channel < 2; channel++) {
    let lastInput = 0;
    let highpass = 0;
    let lowpass = 0;
    for (let i = 0; i < LENGTH; i++) {
      const t = i / RATE;
      const sample = target[channel][i];
      highpass = 0.9978 * (highpass + sample - lastInput);
      lastInput = sample;
      lowpass += 0.72 * (highpass - lowpass);
      const intro = Math.sin(Math.min(1, t / 0.075) * Math.PI / 2);
      const ending = t > 29.15 ? Math.cos((t - 29.15) / 0.85 * Math.PI / 2) ** 2 : 1;
      target[channel][i] = lowpass * intro * ending;
    }
    target[channel][LENGTH - 1] = 0;
  }
}
finish(music);
finish(effects);

let rawPeak = 0;
for (let i = 0; i < LENGTH; i++) {
  for (let channel = 0; channel < 2; channel++) {
    rawPeak = Math.max(rawPeak, Math.abs(music[channel][i] + effects[channel][i]));
  }
}
const gain = 10 ** (-3.1 / 20) / rawPeak;
const fullMix = bus();
for (let i = 0; i < LENGTH; i++) {
  for (let channel = 0; channel < 2; channel++) {
    music[channel][i] *= gain;
    effects[channel][i] *= gain;
    fullMix[channel][i] = music[channel][i] + effects[channel][i];
  }
}

function wav(target, filename) {
  const dataBytes = LENGTH * 4;
  const buffer = Buffer.alloc(44 + dataBytes);
  buffer.write('RIFF', 0);
  buffer.writeUInt32LE(36 + dataBytes, 4);
  buffer.write('WAVEfmt ', 8);
  buffer.writeUInt32LE(16, 16);
  buffer.writeUInt16LE(1, 20);
  buffer.writeUInt16LE(2, 22);
  buffer.writeUInt32LE(RATE, 24);
  buffer.writeUInt32LE(RATE * 4, 28);
  buffer.writeUInt16LE(4, 32);
  buffer.writeUInt16LE(16, 34);
  buffer.write('data', 36);
  buffer.writeUInt32LE(dataBytes, 40);
  let peak = 0;
  let squares = 0;
  let clipped = 0;
  let dc = 0;
  let correlation = 0;
  let leftEnergy = 0;
  let rightEnergy = 0;
  for (let i = 0; i < LENGTH; i++) {
    for (let channel = 0; channel < 2; channel++) {
      const sample = target[channel][i];
      if (!Number.isFinite(sample)) throw new Error(`Non-finite master sample ${i}`);
      if (Math.abs(sample) >= 1) clipped++;
      const pcm = Math.round(clamp(sample, -1, 1) * 32767);
      buffer.writeInt16LE(pcm, 44 + i * 4 + channel * 2);
      const normalized = pcm / 32768;
      peak = Math.max(peak, Math.abs(normalized));
      squares += normalized ** 2;
      dc += normalized;
    }
    correlation += target[0][i] * target[1][i];
    leftEnergy += target[0][i] ** 2;
    rightEnergy += target[1][i] ** 2;
  }
  if (clipped) throw new Error(`${filename}: ${clipped} clipped samples`);
  fs.writeFileSync(path.join(outDir, filename), buffer);
  const rms = Math.sqrt(squares / (LENGTH * 2));
  return {
    filename, bytes: buffer.length, seconds: LENGTH / RATE, sampleRate: RATE,
    channels: 2, pcmBits: 16, sampleFrames: LENGTH,
    peakDbfs: +db(peak).toFixed(3), rmsDbfs: +db(rms).toFixed(3),
    dcOffset: +(dc / (LENGTH * 2)).toFixed(9), clippedSamples: clipped,
    stereoCorrelation: +(correlation / Math.sqrt(leftEnergy * rightEnergy)).toFixed(6),
    sha256: crypto.createHash('sha256').update(buffer).digest('hex'),
  };
}

fs.mkdirSync(outDir, { recursive: true });
const result = [
  wav(fullMix, 'pocket4cut-original-30s.wav'),
  wav(music, 'music-only.wav'),
  wav(effects, 'sfx-only.wav'),
];
console.log(JSON.stringify({
  title: 'Four little moments', provenance: 'Deterministic procedural original, no external samples',
  bpm: BPM, beats: BPM * SECONDS / 60, output: result,
  limitation: 'Sample peak/RMS checks do not establish integrated LUFS, true peak, or subjective listening quality.',
}, null, 2));
