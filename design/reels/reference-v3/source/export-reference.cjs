#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process'),{createHash}=require('node:crypto');
const {createCanvas,loadImage,GlobalFonts}=require('@napi-rs/canvas');
const ROOT=path.resolve(__dirname,'../../../..'),BASE=path.resolve(__dirname,'..');
const OUT=path.join(BASE,'deliverables'),PROOF=path.join(BASE,'verification');
const FF=process.env.FFMPEG_PATH;assert(FF&&fs.existsSync(FF),'Set FFMPEG_PATH.');
const PROBE=process.env.FFPROBE_PATH||path.join(path.dirname(FF),process.platform==='win32'?'ffprobe.exe':'ffprobe');
const STORY=JSON.parse(fs.readFileSync(path.join(__dirname,'story-timed.json'),'utf8'));
const SCRIPT=JSON.parse(fs.readFileSync(path.join(__dirname,'narration-script.json'),'utf8'));
const D=STORY.duration,FPS=30,FRAMES=Math.round(D*FPS);
const VISUAL=path.join(ROOT,'build/reels-render/reference-v3-visual.mp4'),AUDIO=path.join(BASE,'audio/narration-master.wav');
const VIDEO=path.join(OUT,'Pocket4Cut-Reels-Reference-Voice.mp4');
function run(bin,args){const r=spawnSync(bin,args,{encoding:'utf8',windowsHide:true,maxBuffer:32e6});if(r.error)throw r.error;assert.equal(r.status,0,r.stderr);return r;}
const ff=args=>run(FF,['-hide_banner','-nostdin',...args]);
const sha=file=>createHash('sha256').update(fs.readFileSync(file)).digest('hex');
function stamp(t){const n=Math.round(t*1000);return `${String(Math.floor(n/3600000)).padStart(2,'0')}:${String(Math.floor(n/60000)%60).padStart(2,'0')}:${String(Math.floor(n/1000)%60).padStart(2,'0')},${String(n%1000).padStart(3,'0')}`;}
async function main(){
  fs.mkdirSync(OUT,{recursive:true});fs.mkdirSync(PROOF,{recursive:true});
  const voiceJob=JSON.parse(fs.readFileSync(path.join(PROOF,'local-voice-job.json'),'utf8'));
  const masterJob=JSON.parse(fs.readFileSync(path.join(PROOF,'narration-master.json'),'utf8'));
  const asrJob=JSON.parse(fs.readFileSync(path.join(PROOF,'asr-result.json'),'utf8'));
  assert.equal(voiceJob.status,'complete','Reference-conditioned voice generation has not completed.');
  assert.equal(voiceJob.scriptSha256,createHash('sha256').update(SCRIPT.text).digest('hex'),'Voice belongs to a different script.');
  assert.equal(voiceJob.outputSha256,sha(path.join(BASE,'audio/narration-original.wav')),'Generated voice source hash mismatch.');
  assert.equal(masterJob.sha256,sha(AUDIO),'Master audio hash mismatch.');
  assert.equal(masterJob.durationSeconds,D);assert.equal(asrJob.readyForRender,true);
  assert.equal(masterJob.sourceSha256,voiceJob.outputSha256);
  assert.equal(asrJob.inputVoiceSha256,voiceJob.outputSha256);
  assert.equal(asrJob.master.sha256,masterJob.sha256);
  const audioInfo=JSON.parse(run(PROBE,['-v','error','-show_format','-of','json',AUDIO]).stdout);
  assert(Math.abs(Number(audioInfo.format.duration)-D)<.002,'Actual audio duration would be truncated.');
  const renderJob=JSON.parse(fs.readFileSync(path.join(BASE,'visual-stills/render-manifest.json'),'utf8'));
  assert.equal(renderJob.storySha256,sha(path.join(__dirname,'story-timed.json')),'Visuals belong to an older story.');
  assert.equal(renderJob.visualSha256,sha(VISUAL),'Visual master hash mismatch.');
  ff(['-v','warning','-y','-i',VISUAL,'-i',AUDIO,'-map','0:v:0','-map','1:a:0','-c:v','copy','-c:a','aac','-b:a','192k','-ar','48000','-ac','2','-t',String(D),'-map_metadata','-1','-movflags','+faststart',VIDEO]);
  const p=JSON.parse(run(PROBE,['-v','error','-show_streams','-show_format','-of','json',VIDEO]).stdout);
  const v=p.streams.find(s=>s.codec_type==='video'),a=p.streams.find(s=>s.codec_type==='audio');
  assert.equal(p.streams.length,2);assert.equal(v.codec_name,'h264');assert.equal(v.width,1080);assert.equal(v.height,1920);
  assert.equal(v.pix_fmt,'yuv420p');assert.equal(v.avg_frame_rate,'30/1');assert.equal(Number(v.nb_frames),FRAMES);
  for(const k of ['color_space','color_transfer','color_primaries'])assert.equal(v[k],'bt709');
  assert.equal(a.codec_name,'aac');assert.equal(a.channels,2);assert.equal(a.sample_rate,'48000');
  assert(Math.abs(Number(p.format.duration)-D)<.04);assert(Math.abs(Number(v.duration)-Number(a.duration))<.04);
  const bytes=fs.readFileSync(VIDEO),boxes=[];
  for(let i=0;i+8<=bytes.length;){let size=bytes.readUInt32BE(i);const type=bytes.toString('ascii',i+4,i+8);if(size===1)size=Number(bytes.readBigUInt64BE(i+8));if(!size)size=bytes.length-i;assert(size>=8&&i+size<=bytes.length);boxes.push({type,position:i});i+=size;}
  assert(boxes.find(b=>b.type==='moov').position<boxes.find(b=>b.type==='mdat').position);
  assert.equal(ff(['-v','error','-xerror','-i',VIDEO,'-map','0:v:0','-map','0:a:0','-f','null','-']).stderr.trim(),'');
  const loud=ff(['-nostats','-i',VIDEO,'-vn','-af','ebur128=peak=true','-f','null','-']).stderr;
  const summary=loud.slice(loud.lastIndexOf('Summary:')),lufs=Number(summary.match(/I:\s+(-?[\d.]+) LUFS/)[1]),peak=Number(summary.match(/Peak:\s+(-?[\d.]+) dBFS/)[1]);
  assert(lufs>=-18&&lufs<=-14);assert(peak<=-1,'Insufficient AAC true-peak headroom');
  const black=ff(['-nostats','-i',VIDEO,'-an','-vf','blackdetect=d=0.12:pix_th=0.10:pic_th=0.98','-f','null','-']).stderr;
  assert(!/black_start:/.test(black),'Unexpected black interval');
  fs.writeFileSync(path.join(OUT,'CAPTIONS-KO.srt'),STORY.scenes.map((s,i)=>`${i+1}\n${stamp(s.start)} --> ${stamp(s.end)}\n${s.lines.join('\n')}\n`).join('\n'));
  fs.writeFileSync(path.join(OUT,'NARRATION-KO.txt'),SCRIPT.phrases.map(s=>s.text).join('\n')+'\n');
  ff(['-v','error','-y','-i',AUDIO,'-c:a','libmp3lame','-b:a','192k','-map_metadata','-1',path.join(OUT,'Pocket4Cut-Reference-Narration.mp3')]);
  assert(GlobalFonts.registerFromPath(process.env.REEL_FONT_REGULAR||path.join(process.env.WINDIR||'C:/Windows','Fonts/malgun.ttf'),'Proof Regular'));
  const cols=4,tw=270,th=535,sheet=createCanvas(cols*tw,Math.ceil(STORY.scenes.length/cols)*th),ctx=sheet.getContext('2d');
  ctx.fillStyle='#F3F0E8';ctx.fillRect(0,0,sheet.width,sheet.height);
  const proofs=[];
  for(let i=0;i<STORY.scenes.length;i++){
    const s=STORY.scenes[i],time=Math.min(s.end-1/FPS,s.start+Math.min(.7,(s.end-s.start)/2));
    const file=path.join(PROOF,`encoded-${String(i+1).padStart(2,'0')}-${s.id}.jpg`);
    ff(['-v','error','-y','-ss',String(time),'-i',VIDEO,'-frames:v','1','-vf','scale=in_color_matrix=bt709:out_color_matrix=bt601:out_range=pc','-q:v','2',file]);
    const im=await loadImage(file);assert.equal(im.width,1080);assert.equal(im.height,1920);
    ctx.drawImage(im,(i%cols)*tw+5,Math.floor(i/cols)*th+5,260,462.22);ctx.fillStyle='#181816';ctx.font='18px "Proof Regular"';
    ctx.fillText(`${time.toFixed(2)}s / ${s.id}`,(i%cols)*tw+8,Math.floor(i/cols)*th+510);proofs.push({file:path.relative(BASE,file).replaceAll('\\','/'),time});
  }
  fs.writeFileSync(path.join(PROOF,'encoded-contact-sheet.jpg'),sheet.toBuffer('image/jpeg',94));
  for(const [name,t] of [['first',0],['last',D-2/FPS]])ff(['-v','error','-y','-ss',String(t),'-i',VIDEO,'-frames:v','1','-vf','scale=in_color_matrix=bt709:out_color_matrix=bt601:out_range=pc','-q:v','2',path.join(PROOF,`encoded-${name}.jpg`)]);
  const cover=await loadImage(path.join(OUT,'Pocket4Cut-Reference-Cover.jpg'));assert.equal(cover.width,1080);assert.equal(cover.height,1920);
  const report={checkedAt:new Date().toISOString(),file:path.relative(BASE,VIDEO).replaceAll('\\','/'),bytes:bytes.length,sha256:sha(VIDEO),duration:D,
    video:{codec:v.codec_name,width:v.width,height:v.height,frames:FRAMES,fps:FPS,pixelFormat:v.pix_fmt,colorSpace:v.color_space,colorTransfer:v.color_transfer,colorPrimaries:v.color_primaries},
    audio:{codec:a.codec_name,sampleRate:Number(a.sample_rate),channels:a.channels,duration:Number(a.duration),integratedLUFS:lufs,truePeakDbTP:peak,musicAdded:false,soundEffectsAdded:false},
    checks:{fullDecode:'PASS',durationSync:'PASS',faststart:'PASS',blackIntervals:0,coverSize:'PASS'},proofs,
    scope:'Actual encoded-file checks and selected-frame extraction. Visual review is separate; not subjective listening, exact timbre equivalence or Instagram upload acceptance.'};
  fs.writeFileSync(path.join(PROOF,'export-validation.json'),JSON.stringify(report,null,2)+'\n');console.log(JSON.stringify({duration:D,frames:FRAMES,bytes:bytes.length,lufs,peak,decode:'PASS',sha256:report.sha256},null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
