#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const BASE=path.resolve(__dirname,'..');
const asr=JSON.parse(fs.readFileSync(path.join(BASE,'verification/asr-result.json'),'utf8'));
assert(asr.comparison.normalizedTranscriptEqualsExpected,'Narration differs from script');
const offset=.15,lead=.06;
const descriptions=[
  ['hook',['저, 네 컷','만들었어요.']],
  ['reveal',['근데 사진관은','안 갔어요.']],
  ['brand',['이거,','포켓 네 컷이에요.']],
  ['capture',['네 컷 고르고','촬영 누르면,']],
  ['capture',['8장이 자동으로','찍혀요.']],
  ['selection',['잘 나온 4장만','고르고요.']],
  ['layout',['배치랑 필터도','바꿨어요.']],
  ['filter',['어, 흑백','좀 괜찮은데요?']],
  ['save',['완성하면','저장하고,']],
  ['save',['바로 공유해요.']],
  ['memory',['친구 만났는데']],
  ['memory',['사진 안 남긴 날,','있잖아요.']],
  ['cta',['다음엔 폰으로','네 컷 남겨봐요.']],
];
assert.equal(asr.captionPhrases.length,descriptions.length);
const scenes=descriptions.map(([id,lines],i)=>({
  start:i?Number((asr.captionPhrases[i].start_time+offset-lead).toFixed(3)):0,
  end:i<descriptions.length-1?Number((asr.captionPhrases[i+1].start_time+offset-lead).toFixed(3)):26,
  id,lines,voiceStart:asr.captionPhrases[i].start_time+offset,voiceEnd:asr.captionPhrases[i].end_time+offset,
}));
// The combined spoken sentence remains on screen across one editorial layout→filter cut.
const idx=scenes.findIndex(s=>s.id==='layout'),combined=scenes[idx];
scenes.splice(idx,1,{...combined,end:13.65},{...combined,start:13.65,id:'filter',variant:'original'});
scenes.push({start:26,end:30,id:'cta',lines:['Pocket4Cut','폰으로 남기는 네 컷'],spoken:false});
const story={duration:30,source:'Qwen ASR + forced alignment, checked against all 119 Korean syllables',voiceOffsetSeconds:offset,captionLeadSeconds:lead,
  note:'Estimated phrase alignment, not sample-accurate. Editorial hold caption at 26–30s is not spoken. Numbers are typeset as 8/4.',scenes};
for(let i=0;i<scenes.length;i++){
  assert.equal(scenes[i].start,i?scenes[i-1].end:0);assert(scenes[i].end>scenes[i].start);assert(scenes[i].lines.length<=2);
}
fs.writeFileSync(path.join(__dirname,'story-timed.json'),JSON.stringify(story,null,2)+'\n');
console.log(`Aligned ${scenes.length} visual beats to 13 spoken phrases; no narration speed change.`);
