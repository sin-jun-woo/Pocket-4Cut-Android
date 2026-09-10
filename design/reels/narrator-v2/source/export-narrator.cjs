#!/usr/bin/env node
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const { createHash } = require('node:crypto');
const { createCanvas, loadImage, GlobalFonts } = require('@napi-rs/canvas');
const ROOT = path.resolve(__dirname, '../../../..'), BASE = path.resolve(__dirname, '..');
const OUT = path.join(BASE,'deliverables'), PROOF = path.join(BASE,'verification');
const FFMPEG = process.env.FFMPEG_PATH;
assert(FFMPEG && fs.existsSync(FFMPEG),'Set FFMPEG_PATH.');
const FFPROBE = process.env.FFPROBE_PATH || path.join(path.dirname(FFMPEG), process.platform==='win32'?'ffprobe.exe':'ffprobe');
const VISUAL = path.join(ROOT,'build/reels-render/narrator-v2-visual.mp4');
const AUDIO = path.join(BASE,'audio/narration-master-30s.wav');
const TARGET = path.join(OUT,'Pocket4Cut-Reels-Male-Narration-30s.mp4');
const STORY = JSON.parse(fs.readFileSync(path.join(__dirname,'story-timed.json'),'utf8'));
function run(program,args){
  const r=spawnSync(program,args,{encoding:'utf8',windowsHide:true,maxBuffer:32e6});
  if(r.error)throw r.error;
  assert.equal(r.status,0,r.stderr); return r;
}
function ff(args){return run(FFMPEG,['-hide_banner','-nostdin',...args]);}
function sha(file){return createHash('sha256').update(fs.readFileSync(file)).digest('hex');}
function clock(t){
  const ms=Math.round(t*1000);return `${String(Math.floor(ms/3600000)).padStart(2,'0')}:${String(Math.floor(ms/60000)%60).padStart(2,'0')}:${String(Math.floor(ms/1000)%60).padStart(2,'0')},${String(ms%1000).padStart(3,'0')}`;
}
async function main(){
  fs.mkdirSync(OUT,{recursive:true});fs.mkdirSync(PROOF,{recursive:true});
  ff(['-v','warning','-y','-i',VISUAL,'-i',AUDIO,'-map','0:v:0','-map','1:a:0','-c:v','copy','-c:a','aac','-b:a','192k',
    '-ar','48000','-ac','2','-t','30','-map_metadata','-1','-movflags','+faststart',TARGET]);
  const probe=JSON.parse(run(FFPROBE,['-v','error','-show_streams','-show_format','-of','json',TARGET]).stdout);
  const v=probe.streams.find(s=>s.codec_type==='video'),a=probe.streams.find(s=>s.codec_type==='audio');
  assert.equal(probe.streams.length,2);assert.equal(v.codec_name,'h264');assert.equal(v.width,1080);assert.equal(v.height,1920);
  assert.equal(v.pix_fmt,'yuv420p');assert.equal(v.avg_frame_rate,'30/1');assert.equal(Number(v.nb_frames),900);
  assert.equal(v.color_space,'bt709');assert.equal(v.color_transfer,'bt709');assert.equal(v.color_primaries,'bt709');
  assert.equal(a.codec_name,'aac');assert.equal(a.channels,2);assert.equal(a.sample_rate,'48000');
  assert(Math.abs(Number(probe.format.duration)-30)<0.04);assert(Math.abs(Number(v.duration)-Number(a.duration))<0.04);
  const bytes=fs.readFileSync(TARGET),boxes=[];
  for(let p=0;p+8<=bytes.length;){let size=bytes.readUInt32BE(p);const type=bytes.toString('ascii',p+4,p+8);if(size===1)size=Number(bytes.readBigUInt64BE(p+8));if(!size)size=bytes.length-p;assert(size>=8&&p+size<=bytes.length);boxes.push({type,position:p});p+=size;}
  assert(boxes.find(b=>b.type==='moov').position<boxes.find(b=>b.type==='mdat').position);
  const decode=ff(['-v','error','-xerror','-i',TARGET,'-map','0:v:0','-map','0:a:0','-f','null','-']);assert.equal(decode.stderr.trim(),'');
  const loud=ff(['-nostats','-i',TARGET,'-vn','-af','ebur128=peak=true','-f','null','-']).stderr;
  const summary=loud.slice(loud.lastIndexOf('Summary:'));
  const lufs=Number(summary.match(/I:\s+(-?[\d.]+) LUFS/)[1]),peak=Number(summary.match(/Peak:\s+(-?[\d.]+) dBFS/)[1]);
  assert(lufs>=-18&&lufs<=-14,'Narration integrated loudness');assert(peak<=-1.0,'AAC true peak headroom');
  const black=ff(['-nostats','-i',TARGET,'-an','-vf','blackdetect=d=0.12:pix_th=0.10:pic_th=0.98','-f','null','-']).stderr;
  assert(!/black_start:/.test(black),'Unexpected black interval');
  const transcript=STORY.scenes.map((s,i)=>`${i+1}\n${clock(s.start)} --> ${clock(s.end)}\n${s.lines.join('\n')}\n`).join('\n');
  fs.writeFileSync(path.join(OUT,'CAPTIONS-KO.srt'),transcript);
  ff(['-v','error','-y','-i',AUDIO,'-c:a','libmp3lame','-b:a','192k','-map_metadata','-1',path.join(OUT,'Pocket4Cut-Male-Narration.mp3')]);
  const font=process.env.REEL_FONT_REGULAR||path.join(process.env.WINDIR||'C:/Windows','Fonts/malgun.ttf');assert(GlobalFonts.registerFromPath(font,'Reel Proof'));
  const cols=5,rows=Math.ceil(STORY.scenes.length/cols),tw=250,th=510;
  const sheet=createCanvas(cols*tw,rows*th),ctx=sheet.getContext('2d');ctx.fillStyle='#eeeae2';ctx.fillRect(0,0,sheet.width,sheet.height);
  const frameProof=[];
  for(let i=0;i<STORY.scenes.length;i++){
    const s=STORY.scenes[i],t=Math.min(s.end-1/30,s.start+Math.min(0.8,(s.end-s.start)/2));
    const file=path.join(PROOF,`encoded-${String(i+1).padStart(2,'0')}-${s.id}.jpg`);
    ff(['-v','error','-y','-ss',String(t),'-i',TARGET,'-frames:v','1','-vf','scale=in_color_matrix=bt709:out_color_matrix=bt601:out_range=pc','-q:v','2',file]);
    const im=await loadImage(file);assert.equal(im.width,1080);assert.equal(im.height,1920);
    ctx.drawImage(im,(i%cols)*tw+7,Math.floor(i/cols)*th+7,236,419.56);ctx.font='19px "Reel Proof"';ctx.fillStyle='#181a18';
    ctx.fillText(`${t.toFixed(2)}s / ${s.id}`,(i%cols)*tw+8,Math.floor(i/cols)*th+465);
    frameProof.push({file:path.relative(BASE,file).replaceAll('\\','/'),time:t});
  }
  fs.writeFileSync(path.join(PROOF,'encoded-contact-sheet.jpg'),sheet.toBuffer('image/jpeg',94));
  for(const [name,t] of [['first',0],['last',29.96]]){
    const file=path.join(PROOF,`encoded-${name}.jpg`);ff(['-v','error','-y','-ss',String(t),'-i',TARGET,'-frames:v','1',
      '-vf','scale=in_color_matrix=bt709:out_color_matrix=bt601:out_range=pc','-q:v','2',file]);assert(fs.existsSync(file)&&fs.statSync(file).size>1000);
  }
  const cover=await loadImage(path.join(OUT,'Pocket4Cut-Narrator-Cover.jpg'));assert.equal(cover.width,1080);assert.equal(cover.height,1920);
  const report={checkedAt:new Date().toISOString(),file:path.relative(BASE,TARGET).replaceAll('\\','/'),bytes:bytes.length,sha256:sha(TARGET),
    video:{codec:v.codec_name,width:v.width,height:v.height,frames:Number(v.nb_frames),fps:v.avg_frame_rate,pixelFormat:v.pix_fmt,colorSpace:v.color_space,colorTransfer:v.color_transfer,colorPrimaries:v.color_primaries},
    audio:{codec:a.codec_name,sampleRate:Number(a.sample_rate),channels:a.channels,duration:Number(a.duration),integratedLUFS:lufs,truePeakDbTP:peak,music:false,soundEffects:false},
    duration:Number(probe.format.duration),checks:{fullDecode:'PASS',durationSync:'PASS',faststart:'PASS',blackIntervals:0,coverSize:'PASS'},
    frames:frameProof,scope:'File-level verification and selected encoded image inspection, not subjective listening or Instagram upload acceptance.'};
  fs.writeFileSync(path.join(PROOF,'export-validation.json'),JSON.stringify(report,null,2)+'\n');
  console.log(JSON.stringify({duration:report.duration,frames:900,bytes:bytes.length,lufs,peak,decode:'PASS',sha256:report.sha256},null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
