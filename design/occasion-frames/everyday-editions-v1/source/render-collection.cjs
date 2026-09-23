/*
 * Static design export only. Reuses the previously exported Android slot geometry;
 * this does not execute Android, change the app or claim live renderer parity.
 * Image-generated art is only alpha-trimmed, resized and positioned, never redrawn.
 * Run: node source/render-collection.cjs [--theme birthday] [--prepare-only]
 */
'use strict';
const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const sharp = require('sharp');
sharp.concurrency(2);
sharp.cache(false);
const {createCanvas, loadImage, GlobalFonts} = require('@napi-rs/canvas');
const BASE = path.resolve(__dirname, '..');
const ROOT = path.resolve(BASE, '../../..');
const CATALOG = JSON.parse(fs.readFileSync(path.join(__dirname, 'catalog.json'), 'utf8'));
const DIRECTION_FILE = path.join(__dirname, 'art-direction.json');
const DIRECTION = JSON.parse(fs.readFileSync(DIRECTION_FILE, 'utf8'));
const style = t => DIRECTION.profiles[DIRECTION.themes[t.id][0]];
const GEOMETRY_FILE = path.join(ROOT, 'design/seasonal-frames/paper-seasons-v1/deliverables/manifest.json');
const GEOMETRY = JSON.parse(fs.readFileSync(GEOMETRY_FILE, 'utf8')).files
  .filter(f => f.kind === 'transparent-frame' && f.season === 'spring');
const OUT = path.join(BASE, 'deliverables');
const hash = b => crypto.createHash('sha256').update(b).digest('hex');
const posix = p => p.split(path.sep).join('/');
const fileExists = p => fs.existsSync(p);
const arg = name => { const i=process.argv.indexOf(name); return i<0?null:process.argv[i+1]; };
const only = arg('--theme');
const THEMES = CATALOG.themes.filter(t => (!only || t.id === only) &&
  (!arg('--from') || t.index >= Number(arg('--from'))) && (!arg('--to') || t.index <= Number(arg('--to'))));
const FONTS = [
  ['app/src/main/assets/fonts/BMHANNAPro.ttf', 'Pocket Display'],
  ['app/src/main/assets/fonts/BMYEONSUNG_ttf.ttf', 'Pocket Hand'],
];
for (const [p,n] of FONTS) GlobalFonts.registerFromPath(path.join(ROOT,p),n);
for (const [p,n] of [['C:/Windows/Fonts/georgia.ttf','Pocket Serif'],['C:/Windows/Fonts/georgiai.ttf','Pocket Serif Italic'],['C:/Windows/Fonts/malgun.ttf','Pocket Sans'],['C:/Windows/Fonts/consola.ttf','Pocket Mono']]) {
  if(fileExists(p)) GlobalFonts.registerFromPath(p,n);
}

function occupancy(data,w,x0,y0,x1,y1) {
  let n=0;
  for(let y=y0;y<y1;y++) for(let x=x0;x<x1;x++) if(data[(y*w+x)*4+3]>16) n++;
  return n;
}
function chooseCut(data,w,h,axis,target,range,lo=0,hi=axis==='x'?h:w) {
  let best=null;
  for(let p=Math.round(target-range);p<=Math.round(target+range);p++) {
    const n=axis==='x'?occupancy(data,w,p-1,lo,p+2,hi):occupancy(data,w,lo,p-1,hi,p+2);
    const score=n*1000+Math.abs(p-target);
    if(!best||score<best.score) best={p,n,score};
  }
  return best;
}
function trimBounds(data,w,roi) {
  let left=roi[2],top=roi[3],right=-1,bottom=-1;
  for(let y=roi[1];y<roi[3];y++) for(let x=roi[0];x<roi[2];x++) if(data[(y*w+x)*4+3]>8) {
    left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
  }
  if(right<left) throw Error('Empty illustration cell');
  left=Math.max(roi[0],left-3);top=Math.max(roi[1],top-3);
  right=Math.min(roi[2]-1,right+3);bottom=Math.min(roi[3]-1,bottom+3);
  return {left,top,width:right-left+1,height:bottom-top+1};
}
async function prepare(theme) {
  const masterFile=path.join(__dirname,'masters',theme.id+'.png');
  if(!fileExists(masterFile)) return null;
  const master=await fsp.readFile(masterFile),sha=hash(master);
  const meta=await sharp(master).metadata();
  if(!meta.hasAlpha) throw Error(theme.id+': generated master has no alpha');
  const {data,info}=await sharp(master).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const w=info.width,h=info.height;
  const cut1=chooseCut(data,w,h,'x',w/3,w*.07),cut2=chooseCut(data,w,h,'x',w*2/3,w*.07);
  const xs=[0,cut1.p,cut2.p,w],ys=[];
  for(let col=0;col<3;col++) ys.push(chooseCut(data,w,h,'y',h*.5,h*.17,xs[col],xs[col+1]));
  const overridePath=path.join(__dirname,'roi-overrides.json');
  const override=fileExists(overridePath)?JSON.parse(fs.readFileSync(overridePath,'utf8'))[theme.id]:null;
  const rois=(Array.isArray(override)?override:override?.rois)||Array.from({length:6},(_,i)=>{
    const col=i%3,row=Math.floor(i/3);return [xs[col],row?ys[col].p:0,xs[col+1],row?h:ys[col].p];
  });
  if(!override&&(cut1.n>4||cut2.n>4||ys.some(y=>y.n>4))) {
    throw Error(theme.id+': illustration crosses a crop corridor; manual ROI or regeneration needed');
  }
  const motifs=[],layers=[],cell=512;
  for(let i=0;i<6;i++) {
    const bounds=trimBounds(data,w,rois[i]);
    let crop=await sharp(master).extract(bounds).png().toBuffer();
    const polygon=override?.polygons?.[i];
    if(polygon) {
      const points=polygon.map(([x,y])=>`${x-bounds.left},${y-bounds.top}`).join(' ');
      const mask=Buffer.from(`<svg width="${bounds.width}" height="${bounds.height}" xmlns="http://www.w3.org/2000/svg"><polygon points="${points}" fill="white"/></svg>`);
      crop=await sharp(crop).composite([{input:mask,blend:'dest-in'}]).png().toBuffer();
    }
    const png=await sharp(crop).resize(464,464,{fit:'inside',withoutEnlargement:true,kernel:'lanczos3'}).toColourspace('srgb').png().toBuffer();
    const m=await sharp(png).metadata();
    const x=i%3*cell+Math.floor((cell-m.width)/2),y=Math.floor(i/3)*cell+Math.floor((cell-m.height)/2);
    layers.push({input:png,left:x,top:y});
    motifs.push({index:i,name:theme.motifs[i],roi:rois[i],polygon:polygon||null,bounds,width:m.width,height:m.height,png});
  }
  const atlas=await sharp({create:{width:1536,height:1024,channels:4,background:{r:0,g:0,b:0,alpha:0}}}).composite(layers).png({compressionLevel:9}).toBuffer();
  await fsp.mkdir(path.join(__dirname,'atlases'),{recursive:true});
  await fsp.mkdir(path.join(BASE,'review'),{recursive:true});
  await fsp.writeFile(path.join(__dirname,'atlases',theme.id+'.png'),atlas);
  await sharp(atlas).flatten({background:theme.paper}).resize(900,600).jpeg({quality:90}).toFile(path.join(BASE,'review',theme.id+'-art.jpg'));
  const record={id:theme.id,source:'source/masters/'+theme.id+'.png',masterSha256:sha,sourceWidth:w,sourceHeight:h,atlasWidth:1536,atlasHeight:1024,atlasSha256:hash(atlas),method:'Original alpha preserved; mechanical crop, transparent padding and downscale only.',motifs:motifs.map(({png,...m})=>m)};
  await fsp.mkdir(path.join(BASE,'records'),{recursive:true});
  await fsp.writeFile(path.join(BASE,'records',theme.id+'-art.json'),JSON.stringify(record,null,2)+'\n');
  const images=await Promise.all(motifs.map(m=>loadImage(m.png)));
  // The national flag is a precise official symbol, not an AI interpretation.
  if(theme.id==='liberation-day') images[1]=await loadImage(path.join(__dirname,'references/mois-taegeukgi.png'));
  return {record,motifs:images};
}

function text(ctx,words,cx,y,size,maxWidth,font,ink,align='center') {
  ctx.textAlign=align;ctx.textBaseline='alphabetic';ctx.fillStyle=ink;
  ctx.font=`${size}px "${font}"`;
  while(ctx.measureText(words).width>maxWidth&&size>8) {size-=.5;ctx.font=`${size}px "${font}"`;}
  ctx.fillText(words,cx,y);return size;
}
function pattern(ctx,t,w,h,u) {
  const direction=style(t);t={...t,pattern:direction.pattern};
  ctx.fillStyle=t.paper;ctx.fillRect(0,0,w,h);
  ctx.save();ctx.strokeStyle=t.ink;ctx.fillStyle=t.ink;ctx.lineWidth=.32*u;ctx.globalAlpha=direction.patternAlpha;
  if(t.pattern==='fine-rules'||t.pattern==='micro-check') for(let y=10*u;y<h;y+=10*u) {ctx.beginPath();ctx.moveTo(0,y);ctx.lineTo(w,y);ctx.stroke();}
  if(t.pattern==='pinstripe'||t.pattern==='micro-check') for(let x=10*u;x<w;x+=10*u) {ctx.beginPath();ctx.moveTo(x,0);ctx.lineTo(x,h);ctx.stroke();}
  if(t.pattern==='dotted-paper') for(let y=9*u;y<h;y+=12*u) for(let x=9*u;x<w;x+=12*u) {ctx.beginPath();ctx.arc(x,y,.55*u,0,Math.PI*2);ctx.fill();}
  ctx.restore();
  ctx.strokeStyle=t.ink;ctx.lineWidth=.6*u;ctx.strokeRect(1.8*u,1.8*u,w-3.6*u,h-3.6*u);
  if(t.pattern==='double-edge') {ctx.globalAlpha=.48;ctx.strokeRect(4*u,4*u,w-8*u,h-8*u);ctx.globalAlpha=1;}
}
function motif(ctx,img,cx,cy,side,angle=0) {
  const scale=Math.min(side/img.width,side/img.height),w=img.width*scale,h=img.height*scale;
  ctx.save();ctx.translate(cx,cy);ctx.rotate(angle*Math.PI/180);ctx.drawImage(img,-w/2,-h/2,w,h);ctx.restore();
}
function header(ctx,t,g,art) {
  const w=g.width,h=g.header[3],u=w/390;
  const d=style(t),compact=h/u<52,hero=Math.min(h*d.hero,w*.18),editorial=d.header==='editorial';
  const titleFont=/[가-힣]/.test(t.title)?'Pocket Hand':d.font;
  const selection=t.id==='new-year'?[0,2]:t.id==='white-day'?[0,1]:[0,1];
  if(editorial) {
    motif(ctx,art[selection[0]],w*.865,h*.49,hero,0);
    text(ctx,t.title,w*.065,h*.51,Math.min(d.size*u,h*.32),w*.70,titleFont,t.ink,'left');
    text(ctx,'POCKET4CUT  /  '+String(t.index).padStart(2,'0'),w*.068,h*.76,Math.min(6.6*u,h*.12),w*.65,'Pocket Mono',t.ink,'left');
  } else {
    motif(ctx,art[selection[0]],w*.115,h*.49,hero,-4);
    motif(ctx,art[selection[1]],w*.885,h*.49,hero,4);
    text(ctx,t.title,w*.5,h*.51,Math.min(d.size*u,h*.33),w*.54,titleFont,t.ink);
    const subtitle=t.id==='hangeul-day'?'ㄱ  ㄴ  ㄷ   /   POCKET4CUT':'POCKET4CUT  /  '+String(t.index).padStart(2,'0');
    text(ctx,subtitle,w*.5,h*.75,Math.min(6.8*u,h*.13),w*.46,'Pocket Sans',t.ink);
  }
  if(!compact&&!editorial) {
    ctx.save();ctx.strokeStyle=t.ink;ctx.globalAlpha=.35;ctx.lineWidth=.4*u;
    ctx.beginPath();ctx.moveTo(w*.285,h*.85);ctx.lineTo(w*.715,h*.85);ctx.stroke();ctx.restore();
  }
}
function metadataLine(t) {
  const pairs={
    'couple-100':'D+100  ·  2026.09.23',anniversary:'OUR DAY  ·  2026.09.23',
    'friend-anniversary':'10 YEARS TOGETHER', 'sixty-seventy':'60 / 70  ·  오래도록, 함께',
    'today-fourcut':'2026.09.23  WED  14:30','this-month':'SEPTEMBER 2026','monthly-memory':'SEPTEMBER 2026',
    'this-year':'2026','annual-memory':'2026', 'here-a-cut':'SEOUL, KOREA','today-weather':'SUNNY  /  24°C  /  2026.09.23',
    'right-now':'2026.09.23  14:30', dday:'D+100  ·  OUR STORY', 'nth-fourcut':'OUR 10TH 4CUT',
    'one-line':'오늘을 오래 기억할게요', 'custom-anniversary':'우리의 이름  ·  2026.09.23',
  };
  return pairs[t.id]||'2026.09.23  ·  A DAY TO REMEMBER';
}
function draw(ctx,t,g,art) {
  const w=g.width,h=g.height,u=w/390,cells=g.photoSlots,d=style(t);
  pattern(ctx,t,w,h,u);header(ctx,t,g,art);
  const minX=Math.min(...cells.map(c=>c[0])),maxX=Math.max(...cells.map(c=>c[2]));
  const minY=Math.min(...cells.map(c=>c[1])),maxY=Math.max(...cells.map(c=>c[3]));
  const leftSize=Math.min(25*u,(minX-4*u)/1.2),rightSize=Math.min(25*u,(w-maxX-4*u)/1.2);
  const variation=(t.index%3)*.035;
  if(d.sideMotifs>=2) {
    motif(ctx,art[2],minX/2,minY+(maxY-minY)*(.28+variation),leftSize,-7);
    motif(ctx,art[3],(maxX+w)/2,minY+(maxY-minY)*(.69-variation),rightSize,6);
  }
  if(d.sideMotifs>=4) {
    motif(ctx,art[4],minX/2,minY+(maxY-minY)*(.74-variation),leftSize,7);
    motif(ctx,art[5],(maxX+w)/2,minY+(maxY-minY)*(.36+variation),rightSize,-7);
  }
  const sorted=[...cells].sort((a,b)=>a[1]-b[1]);let bottom=sorted[0][3],gapCount=0;
  for(const cell of sorted.slice(1)) {
    if(d.gutterMotifs&&cell[1]>bottom+4*u) {
      const side=Math.min(17*u,(cell[1]-bottom-2*u));
      motif(ctx,art[(gapCount+2)%6],w*(gapCount%2?.67:.33),(bottom+cell[1])/2,side,0);gapCount++;
    }
    bottom=Math.max(bottom,cell[3]);
  }
  const caption=g.captionArea;
  const footerTop=caption?Math.max(maxY,caption[1]):maxY;
  const footerH=h-footerTop;
  if(caption) {
    text(ctx,metadataLine(t),w*.5,footerTop+footerH*.54,Math.min(11.5*u,footerH*.21),w*.74,'Pocket Hand',t.ink);
    text(ctx,'pocket4cut',w*.5,footerTop+footerH*.81,Math.min(8*u,footerH*.14),w*.6,'Pocket Serif Italic',t.ink);
    const s=Math.min(footerH*.58,w*.1);
    if(d.footerMotifs) {motif(ctx,art[4],w*.105,footerTop+footerH*.55,s,-5);motif(ctx,art[5],w*.895,footerTop+footerH*.55,s,5);}
  } else if(footerH>8*u) {
    text(ctx,'pocket4cut',w*.5,footerTop+footerH*.63,Math.min(8*u,footerH*.35),w*.55,'Pocket Serif Italic',t.ink);
  }
  // Exact empty slots come from the earlier Android export manifest. Decorations
  // never become part of photo windows; true alpha is retained in exported PNGs.
  for(const c of cells) {
    ctx.clearRect(c[0],c[1],c[2]-c[0],c[3]-c[1]);
    ctx.strokeStyle=t.ink;ctx.lineWidth=.45*u;ctx.strokeRect(c[0]-.35*u,c[1]-.35*u,c[2]-c[0]+.7*u,c[3]-c[1]+.7*u);
  }
}
function name(g) {return g.layoutId.toLowerCase()+(g.layoutVersion===1?'_legacy':'')+(g.captionBand?'_caption':'');}
async function themeSheet(t,records) {
  const active=records.filter(r=>r.layoutVersion===2&&!r.captionBand);
  const canvas=createCanvas(2400,2100),ctx=canvas.getContext('2d');
  ctx.fillStyle='#F5F0E6';ctx.fillRect(0,0,2400,2100);
  text(ctx,`${String(t.index).padStart(2,'0')}  ${t.name}`,72,112,62,2200,'Pocket Sans','#343731','left');
  text(ctx,'EVERYDAY EDITIONS  /  POCKET4CUT',74,167,25,2200,'Pocket Mono','#6E7468','left');
  const labels=['2컷 세로','2컷 가로','4컷 클래식','4컷 격자','4컷 가로','6컷 세로 격자','6컷 가로 격자','6컷 콜라주'];
  for(let i=0;i<active.length;i++) {
    const r=active[i],x=70+i%4*575,y=235+Math.floor(i/4)*880;
    text(ctx,labels[i],x+270,y+32,29,500,'Pocket Sans','#60685E');
    const png=await sharp(path.join(OUT,r.path)).resize(508,795,{fit:'inside',withoutEnlargement:true}).png().toBuffer(),im=await loadImage(png);
    const px=x+(540-im.width)/2,py=y+63+(795-im.height)/2;
    ctx.fillStyle='#DADAD2';ctx.fillRect(px,py,im.width,im.height);ctx.drawImage(im,px,py);
  }
  text(ctx,'사진 칸이 투명한 PNG  ·  문구 공간 / 이전 6컷 호환 파일도 포함',72,2050,25,2250,'Pocket Sans','#697166','left');
  const p='overviews/'+t.id+'.jpg';await fsp.mkdir(path.join(OUT,'overviews'),{recursive:true});
  await fsp.writeFile(path.join(OUT,p),await canvas.encode('jpeg',92));return p;
}
async function renderTheme(t) {
  const existing=path.join(BASE,'records',t.id+'-frames.json');
  const master=path.join(__dirname,'masters',t.id+'.png');
  if(!fileExists(master)) return {id:t.id,status:'awaiting-art'};
  const sourceHash=hash(await fsp.readFile(master)),rendererHash=hash(Buffer.concat([await fsp.readFile(__filename),await fsp.readFile(DIRECTION_FILE)]));
  if(!process.argv.includes('--force')&&fileExists(existing)) {
    const old=JSON.parse(await fsp.readFile(existing,'utf8'));
    if(old.masterSha256===sourceHash&&old.rendererSha256===rendererHash) return {id:t.id,status:'already-exported'};
  }
  const art=await prepare(t);
  if(process.argv.includes('--prepare-only')) return {id:t.id,status:'art-prepared'};
  const records=[];
  for(const g of GEOMETRY) {
    const c=createCanvas(g.width,g.height),ctx=c.getContext('2d');draw(ctx,t,g,art.motifs);
    const p=`frames/${t.category}/${String(t.index).padStart(2,'0')}-${t.id}/${name(g)}.png`;
    await fsp.mkdir(path.dirname(path.join(OUT,p)),{recursive:true});
    const buffer=await c.encode('png');await fsp.writeFile(path.join(OUT,p),buffer);
    c.width=1;c.height=1;
    records.push({path:p,themeId:t.id,category:t.category,layoutId:g.layoutId,layoutVersion:g.layoutVersion,captionBand:g.captionBand,width:g.width,height:g.height,photoSlots:g.photoSlots,header:g.header,captionArea:g.captionArea,bytes:buffer.length,sha256:hash(buffer)});
  }
  const overview=await themeSheet(t,records);
  const record={id:t.id,index:t.index,name:t.name,category:t.category,group:t.group,masterSha256:sourceHash,rendererSha256:rendererHash,geometrySha256:hash(await fsp.readFile(GEOMETRY_FILE)),method:'Static composition from generated art and saved Android slot coordinates; not Android execution.',count:records.length,overview,files:records};
  await fsp.writeFile(existing,JSON.stringify(record,null,2)+'\n');
  return {id:t.id,status:'exported',frames:records.length,overview};
}
async function main() {
  await fsp.mkdir(OUT,{recursive:true});
  for(const t of THEMES) {
    try {console.log(JSON.stringify(await renderTheme(t)));}
    catch(e) {console.error(JSON.stringify({id:t.id,status:'needs-art-review',message:e.message}));if(only)process.exitCode=1;}
  }
}
main().catch(e=>{console.error(e);process.exitCode=1;});
