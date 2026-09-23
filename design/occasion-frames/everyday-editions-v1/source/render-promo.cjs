/* Exact exported frames, existing fictional demo photographs and generated paper.
 * Mechanical layout/typography only; no illustrations or people are redrawn.
 */
'use strict';
const fs=require('node:fs'),fsp=require('node:fs/promises'),path=require('node:path'),crypto=require('node:crypto');
const sharp=require('sharp');
const {createCanvas,loadImage,GlobalFonts}=require('@napi-rs/canvas');
const BASE=path.resolve(__dirname,'..'),ROOT=path.resolve(BASE,'../../..'),OUT=path.join(BASE,'deliverables');
GlobalFonts.registerFromPath(path.join(ROOT,'app/src/main/assets/fonts/BMHANNAPro.ttf'),'Display');
GlobalFonts.registerFromPath(path.join(ROOT,'app/src/main/assets/fonts/BMYEONSUNG_ttf.ttf'),'Hand');
GlobalFonts.registerFromPath('C:/Windows/Fonts/malgun.ttf','Sans');
GlobalFonts.registerFromPath('C:/Windows/Fonts/georgia.ttf','Serif');
GlobalFonts.registerFromPath('C:/Windows/Fonts/consola.ttf','Mono');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const LABEL=(ctx,s,x,y,size,font,color='#342E2A',align='left')=>{ctx.font=`${size}px "${font}"`;ctx.fillStyle=color;ctx.textAlign=align;ctx.textBaseline='alphabetic';ctx.fillText(s,x,y);};
async function sample(id,layout,photos) {
  const record=JSON.parse(fs.readFileSync(path.join(BASE,'records',id+'-frames.json'),'utf8'));
  const r=record.files.find(f=>f.layoutId===layout&&f.layoutVersion===2&&!f.captionBand);
  const c=createCanvas(r.width,r.height),ctx=c.getContext('2d');ctx.fillStyle='#F3EBD9';ctx.fillRect(0,0,r.width,r.height);
  r.photoSlots.forEach((p,i)=>{
    const im=photos[i%photos.length],w=p[2]-p[0],h=p[3]-p[1],scale=Math.max(w/im.width,h/im.height),sw=w/scale,sh=h/scale;
    ctx.drawImage(im,(im.width-sw)/2,(im.height-sh)/2,sw,sh,p[0],p[1],w,h);
  });
  ctx.drawImage(await loadImage(path.join(OUT,r.path)),0,0);
  const png=await sharp(await c.encode('png')).resize({width:900}).png().toBuffer();c.width=1;c.height=1;
  return {im:await loadImage(png),source:r.path,sourceSha256:r.sha256};
}
function card(ctx,im,cx,cy,width,angle) {
  const height=width*im.height/im.width;ctx.save();ctx.translate(cx,cy);ctx.rotate(angle*Math.PI/180);
  ctx.shadowColor='rgba(51,35,23,.21)';ctx.shadowBlur=20;ctx.shadowOffsetX=6;ctx.shadowOffsetY=14;
  ctx.fillStyle='#F7F0E5';ctx.fillRect(-width/2,-height/2,width,height);ctx.shadowColor='transparent';
  ctx.drawImage(im,-width/2,-height/2,width,height);ctx.restore();
}
async function main() {
  const photoFiles=[1,2,3,4].map(i=>path.join(ROOT,`design/play-store/print-booth-v1/demo-photos/demo_0${i}.jpg`));
  const photos=await Promise.all(photoFiles.map(loadImage));
  const samples=await Promise.all([sample('couple-100','TWO_VERTICAL',photos),sample('birthday','FOUR_GRID',photos),sample('concert','SIX_GRID_2X3',photos)]);
  const c=createCanvas(2160,2700),ctx=c.getContext('2d');ctx.scale(2,2);
  ctx.drawImage(await loadImage(path.join(__dirname,'promo-background.png')),0,0,1080,1350);
  const red='#A43832';
  LABEL(ctx,'Pocket 4Cut',73,72,31,'Serif');LABEL(ctx,'EVERYDAY EDITIONS',1007,69,17,'Mono','#665C51','right');
  ctx.strokeStyle='#A19581';ctx.lineWidth=.8;ctx.beginPath();ctx.moveTo(73,94);ctx.lineTo(1007,94);ctx.stroke();
  LABEL(ctx,'오늘의 분위기,',71,191,88,'Display',red);
  LABEL(ctx,'네컷에 남겨보세요.',71,286,88,'Display',red);
  LABEL(ctx,'생일의 설렘부터, 함께한 평범한 오늘까지.',76,338,27,'Sans','#62594D');
  card(ctx,samples[0].im,235,737,244,-9);
  card(ctx,samples[2].im,823,732,272,10);
  card(ctx,samples[1].im,535,727,386,3);
  LABEL(ctx,'기념일 프레임 88종',75,1160,45,'Display',red);
  LABEL(ctx,'2컷 · 4컷 · 6컷   /   8가지 레이아웃',77,1210,25,'Sans','#4F4A42');
  LABEL(ctx,'사진은 그대로, 오늘의 분위기는 새롭게.',77,1270,31,'Hand','#6E6253');
  const master=await c.encode('png');await fsp.writeFile(path.join(OUT,'instagram-promo-master-2160x2700.png'),master);
  const final=await sharp(master).resize(1080,1350).toColourspace('srgb').png({compressionLevel:9}).toBuffer();
  await fsp.writeFile(path.join(OUT,'instagram-promo.png'),final);
  await fsp.writeFile(path.join(BASE,'records','instagram-promo.json'),JSON.stringify({width:1080,height:1350,masterWidth:2160,masterHeight:2700,sha256:sha(final),sourceFrames:samples.map(({im,...s})=>s),method:'Actual exported frames and existing fictional adult demo photos, placed over image-generated paper; exact Korean typography rendered separately.',published:false},null,2)+'\n');
  console.log(JSON.stringify({file:'instagram-promo.png',width:1080,height:1350,bytes:final.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
