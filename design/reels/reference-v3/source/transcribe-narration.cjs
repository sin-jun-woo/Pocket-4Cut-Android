#!/usr/bin/env node
'use strict';
// Run local recognition without supplying the expected words to the recognizer.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process'),{createHash}=require('node:crypto');
const BASE=path.resolve(__dirname,'..'),ROOT=path.resolve(__dirname,'../../../..');
const BUILD=path.join(ROOT,'build/reels-reference-v3');
const FF=process.env.FFMPEG_PATH;
const CLI=process.env.WHISPER_CLI_PATH||path.join(ROOT,'build/reels-tools/whisper-cpp-b4938/bin/Release/whisper-cli.exe');
const MODEL=process.env.WHISPER_MODEL_PATH||path.join(ROOT,'build/reels-tools/whisper-cpp-b4938/ggml-small-q5_1.bin');
const source=path.join(BASE,'audio/narration-original.wav'),pcm=path.join(BUILD,'narration-asr-16k.wav'),prefix=path.join(BUILD,'narration-whisper-local');
const sha=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
for(const file of [FF,CLI,MODEL,source])assert(file&&fs.existsSync(file),'Missing local ASR prerequisite.');
function run(bin,args){const r=spawnSync(bin,args,{encoding:'utf8',windowsHide:true,maxBuffer:32e6});if(r.error)throw r.error;assert.equal(r.status,0,r.stderr);return r;}
fs.mkdirSync(BUILD,{recursive:true});
const sourceHash=sha(source);
run(FF,['-hide_banner','-nostdin','-v','error','-y','-i',source,'-ac','1','-ar','16000','-c:a','pcm_s16le',pcm]);
const args=['-m',MODEL,'-f',pcm,'-l','ko','-t','6','-ng','-nfa','-dtw','small','-ojf','-otxt','-osrt','-of',prefix];
const result=run(CLI,args);
fs.writeFileSync(path.join(BUILD,'narration-whisper-local.log'),result.stdout+'\n'+result.stderr);
assert.equal(sha(source),sourceHash,'Voice changed during recognition.');
const record={checkedAt:new Date().toISOString(),source:'audio/narration-original.wav',sourceSha256:sourceHash,
  convertedPcmSha256:sha(pcm),asrJsonSha256:sha(prefix+'.json'),whisperVersion:'b4938',model:'small-q5_1',modelSha256:sha(MODEL),
  local:true,language:'ko',dtw:'small',flashAttention:false,expectedTextPromptUsed:false};
fs.mkdirSync(path.join(BASE,'verification'),{recursive:true});
fs.writeFileSync(path.join(BASE,'verification/asr-input.json'),JSON.stringify(record,null,2)+'\n');
console.log(fs.readFileSync(prefix+'.txt','utf8'));
console.log(JSON.stringify(record,null,2));
