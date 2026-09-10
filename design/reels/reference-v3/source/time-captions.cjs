#!/usr/bin/env node
'use strict';

// No forced transcript is fed to the recognizer. Never silently repair ASR words.
// Official whisper.cpp b4938: cli.cpp times_o emits t0/t1 * 10 as milliseconds;
// t_dtw is emitted unchanged. src/whisper.cpp sets it in 10ms units and prints /100.
// https://github.com/ggml-org/whisper.cpp/blob/b4938/examples/cli/cli.cpp
// https://github.com/ggml-org/whisper.cpp/blob/b4938/src/whisper.cpp
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const BASE = path.resolve(__dirname, '..');
const ROOT = path.resolve(__dirname, '../../../..');
const round = n => Number(n.toFixed(3));
const NUMBER_WORDS = { 여덟: '8', 여섯: '6', 네: '4', 두: '2', 팔: '8', 육: '6', 사: '4', 이: '2' };
const NUMBER_RE = /(?:여덟|여섯|네|두|팔|육|사|이)(?=\s*(?:컷|장))/gu;
const NORMALIZATION = [
  'Ignore whitespace and punctuation; Latin letter case is ignored.',
  'Only Korean 2/4/6/8 number forms immediately before 컷 or 장 are mapped to digits.',
  'No phonetic, brand-name, spelling, synonym or ASR-error corrections are applied.',
];

function normalizedWithMap(raw) {
  assert.equal(typeof raw, 'string');
  const replacements = [...raw.matchAll(NUMBER_RE)];
  const units = [];
  let cursor = 0;
  function literal(from, to) {
    for (let i = from; i < to;) {
      const char = String.fromCodePoint(raw.codePointAt(i));
      if (/[\p{L}\p{N}]/u.test(char)) units.push({ char: char.toLowerCase(), from: i, to: i + char.length });
      i += char.length;
    }
  }
  for (const match of replacements) {
    literal(cursor, match.index);
    units.push({ char: NUMBER_WORDS[match[0]], from: match.index, to: match.index + match[0].length });
    cursor = match.index + match[0].length;
  }
  literal(cursor, raw.length);
  return { text: units.map(x => x.char).join(''), units };
}
const normalize = text => normalizedWithMap(text).text;

function compareText(expected, recognized) {
  const a = Array.from(normalize(expected)), b = Array.from(normalize(recognized));
  assert(a.length <= 2000 && b.length <= 2000, 'Unexpectedly long narration; review input');
  const matrix = Array.from({ length: a.length + 1 }, () => new Uint16Array(b.length + 1));
  for (let i = 0; i <= a.length; i++) matrix[i][0] = i;
  for (let j = 0; j <= b.length; j++) matrix[0][j] = j;
  for (let i = 1; i <= a.length; i++) for (let j = 1; j <= b.length; j++) {
    matrix[i][j] = Math.min(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1, matrix[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
  }
  const differences = []; let i = a.length, j = b.length;
  while (i || j) {
    if (i && j && a[i - 1] === b[j - 1] && matrix[i][j] === matrix[i - 1][j - 1]) { i--; j--; }
    else if (i && j && matrix[i][j] === matrix[i - 1][j - 1] + 1) {
      differences.push({ type: 'substitution', expectedIndex: i - 1, recognizedIndex: j - 1, expected: a[--i], recognized: b[--j] });
    } else if (i && matrix[i][j] === matrix[i - 1][j] + 1) {
      differences.push({ type: 'missing', expectedIndex: i - 1, recognizedIndex: j, expected: a[--i], recognized: '' });
    } else {
      differences.push({ type: 'extra', expectedIndex: i, recognizedIndex: j - 1, expected: '', recognized: b[--j] });
    }
  }
  return {
    normalizedTranscriptEqualsExpected: matrix[a.length][b.length] === 0,
    expectedNormalized: a.join(''), recognizedNormalized: b.join(''),
    expectedCharacterCount: a.length, recognizedCharacterCount: b.length,
    editDistance: matrix[a.length][b.length], differences: differences.reverse(), normalization: NORMALIZATION,
  };
}

function offsetSeconds(value) { return Number.isFinite(value) && value >= 0 ? value / 1000 : null; }
function dtwSeconds(value) { return Number.isFinite(value) && value >= 0 ? value / 100 : null; }
function isTextToken(token) {
  return typeof token.text === 'string' && !(Number.isFinite(token.id) && token.id >= 50257) && !/^\[_.*_\]$/.test(token.text);
}
function cleanAsr(asr) {
  assert(Array.isArray(asr.transcription) && asr.transcription.length > 0, 'Missing Whisper transcription');
  return asr.transcription.map((segment, index) => {
    assert.equal(typeof segment.text, 'string', 'Missing segment text');
    return {
      index, text: segment.text,
      startSeconds: offsetSeconds(segment.offsets?.from), endSeconds: offsetSeconds(segment.offsets?.to),
      tokens: (segment.tokens || []).filter(isTextToken).map(token => ({
        text: token.text, id: Number.isFinite(token.id) ? token.id : null,
        probability: Number.isFinite(token.p) ? token.p : null,
        startSeconds: offsetSeconds(token.offsets?.from), endSeconds: offsetSeconds(token.offsets?.to),
        dtwCentiseconds: Number.isFinite(token.t_dtw) ? token.t_dtw : null,
        dtwSeconds: dtwSeconds(token.t_dtw),
      })),
    };
  });
}

function buildTiming(asr, script, master, options = {}) {
  const lead = options.captionLeadSeconds ?? .05;
  const duration = master.durationSeconds, offset = master.voiceTimeOffsetSeconds;
  assert(Number.isFinite(duration) && duration > 0 && duration <= 45, 'Invalid master durationSeconds');
  assert(Number.isFinite(offset), 'Missing master voiceTimeOffsetSeconds; do not guess an offset');
  assert(Number.isFinite(lead) && lead >= 0 && lead <= .2, 'Invalid caption lead');
  assert(Array.isArray(script.phrases) && script.phrases.length > 0, 'Missing script phrases');
  assert.equal(normalize(script.phrases.map(x => x.text).join(' ')), normalize(script.text), 'Script phrases and full text differ');
  const segments = cleanAsr(asr), transcript = segments.map(x => x.text).join('');
  const comparison = compareText(script.text, transcript);
  const issues = [], warnings = [];
  const report = {
    provider: 'Local whisper.cpp ASR, not supplied-text forced transcription',
    modelType: asr.model?.type || null, language: asr.result?.language || asr.params?.language || null,
    transcript, expectedTranscript: script.text, comparison,
    units: { jsonOffsets: 'milliseconds / 1000', t_dtw: 'centiseconds / 100' },
    limitations: [
      'Recognition agreement is not a subjective listening or voice-similarity assessment.',
      'DTW token moments and inferred phrase ends are estimates, not sample-accurate word boundaries.',
      'A nonzero exit status must stop downstream rendering; an older story file is not updated on failure.',
    ],
    master: { durationSeconds: duration, voiceTimeOffsetSeconds: offset },
    segments, issues, warnings, readyForRender: false,
  };
  if (!comparison.normalizedTranscriptEqualsExpected) issues.push('Recognized narration differs from the expected script; inspect differences without replacing the original transcript.');
  if (transcript.includes('\uFFFD')) issues.push('Segment text contains Unicode replacement characters.');
  const allTokens = [];
  let tokenCursor = 0;
  for (const segment of segments) {
    const joined = segment.tokens.map(x => x.text).join('');
    segment.joinedTokenText = joined;
    segment.tokenTextExactlyMatchesSegment = joined === segment.text;
    segment.normalizedTokenTextMatchesSegment = normalize(joined) === normalize(segment.text);
    if (!segment.normalizedTokenTextMatchesSegment || joined.includes('\uFFFD')) issues.push(`Segment ${segment.index}: token text is incomplete or differs from its segment text; do not invent byte-fallback mappings.`);
    else if (!segment.tokenTextExactlyMatchesSegment) warnings.push(`Segment ${segment.index}: token/segment formatting differs; normalized content agrees.`);
    for (const token of segment.tokens) {
      allTokens.push({ ...token, segmentIndex: segment.index, from: tokenCursor, to: tokenCursor + token.text.length, segmentEndSeconds: segment.endSeconds });
      tokenCursor += token.text.length;
    }
  }
  const tokenText = allTokens.map(x => x.text).join('');
  report.joinedTokenTranscript = tokenText;
  const mapped = normalizedWithMap(tokenText);
  if (mapped.text !== comparison.recognizedNormalized) issues.push('Full token text does not map one-to-one to the recognized normalized transcript.');
  const contentTokens = allTokens.filter(token => normalize(token.text).length > 0);
  let previous = -1;
  for (const token of contentTokens) {
    if (token.dtwSeconds === null) issues.push(`Missing DTW timestamp for token ${token.id} in segment ${token.segmentIndex}. Use -nfa -dtw small and inspect the recognizer log.`);
    else {
      if (token.dtwSeconds < previous) issues.push(`Non-monotonic DTW timestamp in segment ${token.segmentIndex}.`);
      if (token.dtwSeconds + offset < -.05 || token.dtwSeconds + offset >= duration) issues.push(`DTW timestamp outside the master timeline in segment ${token.segmentIndex}.`);
      previous = token.dtwSeconds;
    }
  }
  if (contentTokens.length === 0) issues.push('No timed content tokens.');
  report.issues = [...new Set(issues)];
  if (report.issues.length) return { report, story: null };

  function tokenForUnit(unitIndex, end = false) {
    const unit = mapped.units[unitIndex];
    assert(unit, 'Missing normalized character mapping');
    const sourceIndex = end ? unit.to - 1 : unit.from;
    const token = allTokens.find(x => sourceIndex >= x.from && sourceIndex < x.to);
    assert(token && token.dtwSeconds !== null, 'Missing usable mapped token timestamp');
    return token;
  }
  const phrases = []; let normalizedCursor = 0;
  script.phrases.forEach((phrase, index) => {
    const normalized = normalize(phrase.text), first = tokenForUnit(normalizedCursor), last = tokenForUnit(normalizedCursor + normalized.length - 1, true);
    const next = contentTokens.find(x => x.from > last.from);
    let end = last.endSeconds;
    if (end === null || end <= last.dtwSeconds || (next && end > next.dtwSeconds)) end = next?.dtwSeconds ?? last.segmentEndSeconds;
    if (end === null || end <= last.dtwSeconds) issues.push(`Phrase ${index}: inferred end does not follow its last DTW token; inspect alignment.`);
    if (end !== null && end + offset > duration + .05) issues.push(`Phrase ${index}: inferred spoken end exceeds the master duration; inspect audio trimming.`);
    phrases.push({ ...phrase, phraseIndex: index, normalizedStart: normalizedCursor, normalizedEnd: normalizedCursor + normalized.length,
      rawVoiceStart: first.dtwSeconds, rawVoiceEnd: end,
      voiceStart: round(Math.max(0, first.dtwSeconds + offset)), voiceEnd: end === null ? null : round(Math.min(duration, end + offset)),
      timingBasis: 'First DTW token moment; end from compatible token offset, next token moment, or segment end.',
    });
    normalizedCursor += normalized.length;
  });
  assert.equal(normalizedCursor, mapped.units.length, 'Not every recognized character is assigned');
  report.captionPhrases = phrases;
  report.issues = [...new Set(issues)];
  if (report.issues.length) return { report, story: null };
  const scenes = phrases.map((phrase, i) => ({
    start: i ? round(Math.max(0, phrase.voiceStart - lead)) : 0,
    end: i < phrases.length - 1 ? round(Math.max(0, phrases[i + 1].voiceStart - lead)) : round(duration),
    id: phrase.id, lines: phrase.lines, ...(phrase.variant ? { variant: phrase.variant } : {}),
    voiceStart: phrase.voiceStart, voiceEnd: phrase.voiceEnd, phraseIndex: phrase.phraseIndex,
  }));
  for (let i = scenes.length - 1; i >= 0; i--) {
    const shot = scenes[i], phrase = phrases[shot.phraseIndex];
    if (shot.id === 'layout' && phrase.text.includes('필터')) {
      const cut = round(shot.start + (shot.end - shot.start) * .425);
      scenes.splice(i, 1, { ...shot, end: cut }, { ...shot, start: cut, id: 'filter', variant: 'original' });
    } else if (shot.id === 'occasions') {
      const localIndex = normalize(phrase.text).indexOf(normalize('집에서'));
      const dtw = localIndex >= 0 ? tokenForUnit(phrase.normalizedStart + localIndex).dtwSeconds + offset - lead : NaN;
      const candidate = Number.isFinite(dtw) ? dtw : (shot.start + shot.end) / 2;
      const cut = round(Math.max(shot.start + (shot.end - shot.start) * .3, Math.min(shot.start + (shot.end - shot.start) * .7, candidate)));
      scenes.splice(i, 1, { ...shot, end: cut, variant: 'one' }, { ...shot, start: cut, variant: 'two' });
    }
  }
  for (let i = 0; i < scenes.length; i++) {
    const shot = scenes[i];
    if (shot.start !== (i ? scenes[i - 1].end : 0) || shot.end <= shot.start || shot.end > duration + .001) issues.push(`Scene ${i} has an invalid or non-contiguous interval.`);
    if (shot.end - shot.start < .25) issues.push(`Scene ${i} is shorter than 250ms; inspect token boundaries before rendering.`);
  }
  report.captionPhrases = phrases;
  report.issues = [...new Set(issues)];
  if (report.issues.length) return { report, story: null };
  report.readyForRender = true;
  return { report, story: {
    duration: round(duration), source: 'Local Whisper ASR transcript comparison and experimental DTW token timing',
    voiceOffsetSeconds: offset, captionLeadSeconds: lead,
    note: 'Estimated phrase timing. Editorial layout/filter and occasion cuts share their complete spoken phrase caption. No exact voice-similarity claim.',
    scenes,
  } };
}

function main() {
  const args = process.argv.slice(2), value = (flag, fallback) => {
    const index = args.indexOf(flag); return index < 0 ? fallback : path.resolve(args[index + 1] || assert.fail('Missing ' + flag));
  };
  const asrFile = value('--asr', path.join(ROOT, 'build/reels-reference-v3/narration-whisper-local.json'));
  const scriptFile = value('--script', path.join(__dirname, 'narration-script.json'));
  const masterFile = value('--master', path.join(BASE, 'verification/narration-master.json'));
  const read = file => JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
  const master = read(masterFile), inputProof = read(path.join(BASE, 'verification/asr-input.json'));
  const currentVoiceSha = createHash('sha256').update(fs.readFileSync(path.join(BASE, 'audio/narration-original.wav'))).digest('hex');
  assert.equal(inputProof.sourceSha256, currentVoiceSha, 'ASR belongs to a different voice source.');
  assert.equal(inputProof.asrJsonSha256, createHash('sha256').update(fs.readFileSync(asrFile)).digest('hex'), 'ASR data has changed since recognition.');
  assert.equal(master.sourceSha256, currentVoiceSha, 'Master belongs to a different voice source.');
  const result = buildTiming(read(asrFile), read(scriptFile), master);
  result.report.inputVoiceSha256 = currentVoiceSha;
  result.report.master.sha256 = master.sha256;
  result.report.inputSha256 = createHash('sha256').update(fs.readFileSync(asrFile)).digest('hex');
  const reportFile = path.join(BASE, 'verification/asr-result.json');
  fs.mkdirSync(path.dirname(reportFile), { recursive: true });
  fs.writeFileSync(reportFile, JSON.stringify(result.report, null, 2) + '\n');
  if (!result.story) {
    console.error(`Timing blocked: ${result.report.issues.join(' | ')} No story file was written. Stop downstream rendering.`);
    process.exitCode = 2; return;
  }
  fs.writeFileSync(path.join(__dirname, 'story-timed.json'), JSON.stringify(result.story, null, 2) + '\n');
  console.log(`Compared ${result.report.comparison.expectedCharacterCount} normalized characters; ${result.story.scenes.length} estimated timed scenes. Subjective listening is not verified.`);
}
module.exports = { normalize, normalizedWithMap, compareText, offsetSeconds, dtwSeconds, cleanAsr, buildTiming };
if (require.main === module) {
  try { main(); } catch (error) { console.error(error.message); process.exitCode = 2; }
}
