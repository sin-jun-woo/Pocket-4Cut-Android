#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process');
const {createHash}=require('node:crypto');
const BASE=path.resolve(__dirname,'..'),FF=process.env.FFMPEG_PATH;
assert(FF&&fs.existsSync(FF),'Set FFMPEG_PATH.');
const PROBE=process.env.FFPROBE_PATH||path.join(path.dirname(FF),process.platform==='win32'?'ffprobe.exe':'ffprobe');
const source=path.join(BASE,'audio/narration-original.wav'),target=path.join(BASE,'audio/narration-master.wav');
function run(bin,args){const r=spawnSync(bin,args,{encoding:'utf8',windowsHide:true,maxBuffer:8e6});if(r.error)throw r.error;assert.equal(r.status,0,r.stderr);return r;}
const info=JSON.parse(run(PROBE,['-v','error','-show_format','-show_streams','-of','json',source]).stdout);
const rawDuration=Number(info.format.duration),offset=0.12;
assert(rawDuration>10&&rawDuration<35,'Review unexpectedly short/long generated speech; never silently truncate it.');
const duration=Math.max(23,Math.ceil(rawDuration+offset+1));
const pre='highpass=f=60,acompressor=threshold=0.25:ratio=1.5:attack=6:release=80:makeup=1,aformat=sample_rates=48000:channel_layouts=stereo';
const ff=args=>run(FF,['-hide_banner','-nostdin',...args]);
const first=ff(['-i',source,'-af',pre+',loudnorm=I=-16:TP=-1.8:LRA=7:print_format=json','-f','null','-']).stderr;
const m=JSON.parse(first.match(/\{\s*"input_i"[\s\S]*?\}/)[0]);
const norm=`loudnorm=I=-16:TP=-1.8:LRA=7:measured_I=${m.input_i}:measured_TP=${m.input_tp}:measured_LRA=${m.input_lra}:measured_thresh=${m.input_thresh}:offset=${m.target_offset}:linear=true:print_format=json`;
const filter=`${pre},${norm},aresample=48000,afade=t=in:st=0:d=0.008,adelay=120:all=1,apad,atrim=duration=${duration}`;
const result=ff(['-y','-i',source,'-af',filter,'-ar','48000','-ac','2','-c:a','pcm_s24le',target]);
const report={source:'audio/narration-original.wav',output:'audio/narration-master.wav',rawDurationSeconds:rawDuration,durationSeconds:duration,
  sourceSha256:createHash('sha256').update(fs.readFileSync(source)).digest('hex'),
  voiceTimeOffsetSeconds:offset,speechTimeStretch:1,musicAdded:false,soundEffectsAdded:false,sampleRate:48000,channels:2,codec:'PCM24',
  filters:filter,measuredBeforeNormalization:m,normalizationResult:JSON.parse(result.stderr.match(/\{\s*"input_i"[\s\S]*?\}/)[0]),
  bytes:fs.statSync(target).size,sha256:createHash('sha256').update(fs.readFileSync(target)).digest('hex')};
fs.mkdirSync(path.join(BASE,'verification'),{recursive:true});
fs.writeFileSync(path.join(BASE,'verification/narration-master.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
