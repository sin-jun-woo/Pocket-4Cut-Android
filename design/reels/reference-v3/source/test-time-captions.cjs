'use strict';
// Entirely SYNTHETIC fixtures. These tests do not verify any generated narration.
const test = require('node:test');
const assert = require('node:assert/strict');
const { normalize, compareText, offsetSeconds, dtwSeconds, cleanAsr, buildTiming } = require('./time-captions.cjs');
const phrases = [
  { id: 'hook', text: '네 컷 남겨요.', lines: ['4컷 남겨요.'] },
  { id: 'layout', text: '배치랑 필터도 취향대로 바꿔요.', lines: ['배치랑 필터도 취향대로.'] },
  { id: 'occasions', text: '데이트할 때도, 집에서 놀 때도.', lines: ['데이트할 때도, 집에서 놀 때도.'] },
  { id: 'cta', text: '포켓네컷.', lines: ['Pocket4Cut'] },
];
const script = { text: phrases.map(x => x.text).join(' '), phrases };
function fixture() {
  return {
    params: { model: 'C:\\private-user\\secret-folder\\model.bin', language: 'ko' }, model: { type: 'synthetic' }, result: { language: 'ko' },
    transcription: phrases.map((p, i) => {
      const words = p.text.split(' '), span = 3000, each = span / words.length;
      return { text: ' ' + p.text, offsets: { from: i * span, to: (i + 1) * span },
        tokens: [{ id: 50364, text: '[_BEG_]', t_dtw: -1 }, ...words.map((word, j) => ({
          id: 100 + j, text: ' ' + word, p: .9, t_dtw: (i * span + j * each) / 10,
          offsets: { from: i * span + j * each, to: i * span + (j + 1) * each },
        }))] };
    }),
  };
}
const master = { durationSeconds: 13, voiceTimeOffsetSeconds: .15 };
test('only contextual number spelling is normalized; brand errors remain visible', () => {
  assert.equal(normalize('네 컷, 여덟 장!'), '4컷8장');
  assert.equal(normalize('4컷 8장'), '4컷8장');
  assert.equal(normalize('사람이 네게'), '사람이네게');
  assert.equal(compareText('포켓네컷', '포켓네커').normalizedTranscriptEqualsExpected, false);
  assert.equal(compareText('네 컷', '4컷').normalizedTranscriptEqualsExpected, true);
});
test('DTW centiseconds and JSON milliseconds are not confused; missing times stay null', () => {
  assert.equal(dtwSeconds(123), 1.23); assert.equal(offsetSeconds(1230), 1.23);
  assert.equal(dtwSeconds(-1), null); assert.equal(offsetSeconds(undefined), null);
});
test('synthetic matching ASR yields contiguous scenes and editorial split variants', () => {
  const { report, story } = buildTiming(fixture(), script, master);
  assert.equal(report.readyForRender, true); assert.equal(report.comparison.editDistance, 0);
  assert.equal(story.scenes.length, 6); assert.equal(story.scenes[0].start, 0); assert.equal(story.scenes.at(-1).end, 13);
  assert.equal(story.scenes[1].start, 3.1); assert.equal(report.captionPhrases[0].voiceStart, .15);
  assert.equal(story.scenes[2].id, 'filter'); assert.equal(story.scenes[2].variant, 'original');
  assert.equal(story.scenes[3].variant, 'one'); assert.equal(story.scenes[4].variant, 'two');
  story.scenes.forEach((x, i) => assert.equal(x.start, i ? story.scenes[i - 1].end : 0));
  assert.equal(JSON.stringify(report).includes('private-user'), false);
});
test('brand mismatch blocks final story without overwriting recognized transcript', () => {
  const data = fixture(); data.transcription[3].text = ' 포켓네커.'; data.transcription[3].tokens[1].text = ' 포켓네커.';
  const { report, story } = buildTiming(data, script, master);
  assert.equal(story, null); assert.equal(report.readyForRender, false); assert.match(report.transcript, /포켓네커/);
  assert(report.comparison.differences.some(x => x.type === 'substitution'));
});
test('broken byte-fallback token text blocks alignment even when segment transcript matches', () => {
  const data = fixture(); data.transcription[0].tokens[1].text = ' \uFFFD';
  const { report, story } = buildTiming(data, script, master);
  assert.equal(report.comparison.normalizedTranscriptEqualsExpected, true); assert.equal(story, null);
  assert(report.issues.some(x => /token text is incomplete/.test(x)));
});
test('DTW disabled or non-monotonic token timestamps block output', () => {
  let data = fixture(); data.transcription[1].tokens[1].t_dtw = -1;
  assert.equal(buildTiming(data, script, master).story, null);
  data = fixture(); data.transcription[2].tokens[2].t_dtw = 10;
  assert(buildTiming(data, script, master).report.issues.some(x => /Non-monotonic/.test(x)));
});
test('ASR export keeps source words but excludes machine configuration and input paths', () => {
  const cleaned = cleanAsr(fixture());
  assert.equal(cleaned[0].text, ' 네 컷 남겨요.'); assert.equal(cleaned[0].tokens.length, 3);
  assert.equal(JSON.stringify(cleaned).includes('secret-folder'), false);
});
test('missing master offset and mismatched phrase inventories fail instead of guessing', () => {
  assert.throws(() => buildTiming(fixture(), script, { durationSeconds: 13 }), /voiceTimeOffsetSeconds/);
  assert.throws(() => buildTiming(fixture(), { ...script, text: '다른 대본' }, master), /Script phrases/);
});
test('inferred final speech beyond master duration is reported instead of silently clipped', () => {
  const result = buildTiming(fixture(), script, { durationSeconds: 11, voiceTimeOffsetSeconds: .15 });
  assert.equal(result.story, null);
  assert(result.report.issues.some(x => /exceeds the master duration/.test(x)));
});
