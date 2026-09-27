/**
 * Editorial reel composites, NOT Android execution or device screenshots.
 * Uses the current bundled occasion catalog/atlas and a documented transcription
 * of CollageLayoutMath + OccasionFramePainter (2026-09-27, no numbered header).
 * Original design deliveries and app assets are read-only inputs.
 */
'use strict';
const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const {createCanvas, loadImage, GlobalFonts} = require('@napi-rs/canvas');
const ROOT = path.resolve(__dirname, '../../../..');
const OUT = path.resolve(__dirname, '../visual-assets');
const ASSETS = path.join(ROOT, 'app/src/main/assets');
const CAT_PATH = 'app/src/main/assets/occasion/v1/catalog.json';
const catalog = JSON.parse(fs.readFileSync(path.join(ROOT, CAT_PATH), 'utf8'));
const hash = b => crypto.createHash('sha256').update(b).digest('hex');
const fileHash = p => hash(fs.readFileSync(path.join(ROOT,p)));
const fonts = [
  ['app/src/main/assets/fonts/BMHANNAPro.ttf', 'Pocket Display'],
  ['app/src/main/assets/fonts/BMYEONSUNG_ttf.ttf', 'Pocket Hand'],
];
for(const [p,n] of fonts) GlobalFonts.registerFromPath(path.join(ROOT,p),n);
for(const [p,n] of [['georgia.ttf','Pocket Serif'],['georgiai.ttf','Pocket Serif Italic'],['consola.ttf','Pocket Mono'],['malgun.ttf','Pocket Sans']]) {
  const local = path.join(process.env.WINDIR || 'C:/Windows', 'Fonts', p);
  if(!fs.existsSync(local)) throw Error('Required installed font unavailable: '+p);
  GlobalFonts.registerFromPath(local,n);
}
const theme = id => {const t=catalog.themes.find(t=>t.id===id);if(!t)throw Error('Unknown theme '+id);return t;};

// Current FrameLayouts + FrameCatalog clean-white defaults and CollageLayoutMath.
// Dates/captions are intentionally off; no auto weather/location/day-count claims.
function geometry(id,width) {
  const s=width/390;
  if(id==='FOUR_VERTICAL') {
    const height=width*4920/1650, pad=20*s,gap=10*s,header=45*s;
    const cellH=(height-header-2*pad-3*gap)/4;
    return {layoutId:id,width,height,header:pad+header,cells:Array.from({length:4},(_,i)=>[pad,pad+header+i*(cellH+gap),width-pad,pad+header+i*(cellH+gap)+cellH])};
  }
  if(id==='SIX_COLLAGE') {
    const x=20*s,cw=110*s,ch=440/3*s,g=10*s,top=40*s;
    const second=top+ch+g,third=second+ch+g;
    return {layoutId:id,width,height:545*s,header:top,cells:[
      [x,top,x+cw*2+g,second+ch], [x+2*(cw+g),top,x+3*cw+2*g,top+ch],
      [x+2*(cw+g),second,x+3*cw+2*g,second+ch],
      ...[0,1,2].map(i=>[x+i*(cw+g),third,x+i*(cw+g)+cw,third+ch]),
    ]};
  }
  const layouts={TWO_VERTICAL:[1,2],TWO_HORIZONTAL:[2,1],FOUR_GRID:[2,2],FOUR_HORIZONTAL:[4,1],SIX_GRID_2X3:[2,3],SIX_GRID_3X2:[3,2]};
  const [cols,rows]=layouts[id]||[]; if(!cols)throw Error('Unsupported layout '+id);
  const pad=(id==='TWO_VERTICAL'?35:20)*s,gap=(id==='TWO_VERTICAL'?25:10)*s,header=45*s;
  const cw=(width-2*pad-gap*(cols-1))/cols,ch=cw/(3/4);
  const height=header+rows*ch+gap*(rows-1)+2*pad;
  return {layoutId:id,width,height,header:pad+header,cells:Array.from({length:cols*rows},(_,i)=>{const x=pad+(i%cols)*(cw+gap),y=pad+header+Math.floor(i/cols)*(ch+gap);return[x,y,x+cw,y+ch];})};
}
function text(ctx,word,x,y,size,maxWidth,font,color,align='center') {
  ctx.textAlign=align;ctx.textBaseline='alphabetic';ctx.fillStyle=color;
  while(size>8){ctx.font=`${size}px "${font}"`;if(ctx.measureText(word).width<=maxWidth)break;size-=.5;}
  ctx.font=`${Math.max(1,size)}px "${font}"`;ctx.fillText(word,x,y);
}
function motif(ctx,t,atlas,index,cx,cy,side,rotation=0) {
  if(side<=0)return;
  const m=t.atlas.motifs.find(m=>m.index===index),cell=m.cellRectPx,content=m.contentSizePx;
  const sx=cell.x+Math.floor((cell.width-content.width)/2),sy=cell.y+Math.floor((cell.height-content.height)/2);
  const scale=Math.min(side/content.width,side/content.height),w=content.width*scale,h=content.height*scale;
  ctx.save();ctx.translate(cx,cy);ctx.rotate(rotation*Math.PI/180);
  ctx.drawImage(atlas,sx,sy,content.width,content.height,-w/2,-h/2,w,h);ctx.restore();
}
function backdrop(ctx,t,g) {
  const w=g.width,h=g.height,u=w/390;
  ctx.fillStyle=t.paperColor;ctx.fillRect(0,0,w,h);
  ctx.save();ctx.strokeStyle=t.inkColor;ctx.fillStyle=t.inkColor;ctx.lineWidth=.32*u;ctx.globalAlpha=t.profile.patternAlpha;
  if(['fine-rules','micro-check'].includes(t.pattern))for(let y=10*u;y<h;y+=10*u){ctx.beginPath();ctx.moveTo(0,y);ctx.lineTo(w,y);ctx.stroke();}
  if(['pinstripe','micro-check'].includes(t.pattern))for(let x=10*u;x<w;x+=10*u){ctx.beginPath();ctx.moveTo(x,0);ctx.lineTo(x,h);ctx.stroke();}
  if(t.pattern==='dotted-paper')for(let y=9*u;y<h;y+=12*u)for(let x=9*u;x<w;x+=12*u){ctx.beginPath();ctx.arc(x,y,.55*u,0,Math.PI*2);ctx.fill();}
  ctx.restore();ctx.strokeStyle=t.inkColor;ctx.lineWidth=.6*u;ctx.strokeRect(1.8*u,1.8*u,w-3.6*u,h-3.6*u);
  if(t.pattern==='double-edge'){ctx.save();ctx.globalAlpha=.48;ctx.strokeRect(4*u,4*u,w-8*u,h-8*u);ctx.restore();}
}
function cover(ctx,img,c) {
  const [x,y,r,b]=c,w=r-x,h=b-y,scale=Math.max(w/img.width,h/img.height),sw=w/scale,sh=h/scale;
  ctx.save();ctx.beginPath();ctx.rect(x,y,w,h);ctx.clip();
  ctx.drawImage(img,(img.width-sw)/2,(img.height-sh)/2,sw,sh,x,y,w,h);ctx.restore();
}
function artwork(ctx,t,g,atlas) {
  const w=g.width,h=g.height,u=w/390,hh=g.header,p=t.profile,items=t.placements;
  const hero=Math.min(hh*p.hero,w*.18);
  for(const item of items.header.items)motif(ctx,t,atlas,item.motifIndex,w*item.x,hh*item.y,hero,item.rotationDegrees);
  const editorial=items.header.mode==='editorial',font=/[가-힣]/.test(t.title)?'Pocket Hand':p.font;
  text(ctx,t.title,w*(editorial?.065:.5),hh*.51,Math.min(p.size*u,hh*(editorial?.32:.33)),w*(editorial?.70:.54),font,t.inkColor,editorial?'left':'center');
  if(!editorial&&t.id==='hangeul-day')text(ctx,'ㄱ  ㄴ  ㄷ / Pocket 4Cut',w*.5,hh*.75,Math.min(6.8*u,hh*.13),w*.46,'Pocket Sans',t.inkColor);
  // Current painter deliberately omits the common numbered brand subtitle.
  if(!editorial&&hh/u>=52){ctx.save();ctx.strokeStyle=t.inkColor;ctx.globalAlpha=.35;ctx.lineWidth=.4*u;ctx.beginPath();ctx.moveTo(w*.285,hh*.85);ctx.lineTo(w*.715,hh*.85);ctx.stroke();ctx.restore();}
  const cells=g.cells,minX=Math.min(...cells.map(c=>c[0])),maxX=Math.max(...cells.map(c=>c[2])),minY=Math.min(...cells.map(c=>c[1])),maxY=Math.max(...cells.map(c=>c[3]));
  const left=Math.min(25*u,Math.max(0,(minX-4*u)/1.2)),right=Math.min(25*u,Math.max(0,(w-maxX-4*u)/1.2)),variation=t.index%3*.035;
  for(const item of items.sides.filter(v=>p.sideMotifs>=v.minSideMotifs))motif(ctx,t,atlas,item.motifIndex,item.edge==='left'?minX/2:(maxX+w)/2,minY+(maxY-minY)*(item.yBase+item.yVariationMultiplier*variation),item.edge==='left'?left:right,item.rotationDegrees);
  if(items.gutter.enabled&&p.gutterMotifs){
    const sorted=[...cells].sort((a,b)=>a[1]-b[1]);let bottom=sorted[0][3],count=0;
    for(const c of sorted.slice(1)){if(c[1]>bottom+4*u){const rule=items.gutter;motif(ctx,t,atlas,(rule.motifStartIndex+count)%rule.motifModulo,w*rule.xCycle[count%rule.xCycle.length],(bottom+c[1])/2,Math.min(17*u,c[1]-bottom-2*u));count++;}bottom=Math.max(bottom,c[3]);}
  }
  const fh=h-maxY;
  if(fh>8*u){
    if(items.footer.enabled&&p.footerMotifs)for(const item of items.footer.items){const cx=w*item.x,cy=maxY+fh*item.y,clearance=Math.max(0,Math.min(cx,w-cx,cy-maxY,h-cy));motif(ctx,t,atlas,item.motifIndex,cx,cy,Math.min(fh*.58,w*.1,clearance*2),item.rotationDegrees);}
    text(ctx,'Pocket 4Cut',w*.5,maxY+fh*.63,Math.min(8*u,fh*.35),w*.55,'Pocket Serif Italic',t.inkColor);
  }
}
async function render(t,layout,width,photos=null) {
  const g=geometry(layout,width),canvas=createCanvas(Math.round(g.width),Math.round(g.height)),ctx=canvas.getContext('2d');
  const atlas=await loadImage(path.join(ASSETS,t.atlas.assetPath));
  backdrop(ctx,t,g);
  for(let i=0;i<g.cells.length;i++){
    const c=g.cells[i];
    if(photos)cover(ctx,photos[i%photos.length],c);
    else{ctx.fillStyle='#DADBD4';ctx.fillRect(c[0],c[1],c[2]-c[0],c[3]-c[1]);}
    ctx.strokeStyle=t.inkColor;ctx.lineWidth=.45*(g.width/390);const e=.35*(g.width/390);ctx.strokeRect(c[0]-e,c[1]-e,c[2]-c[0]+2*e,c[3]-c[1]+2*e);
  }
  artwork(ctx,t,g,atlas);
  return{canvas,geometry:g};
}
async function save(canvas,file,quality=95){const buffer=await canvas.encode(path.extname(file)==='.png'?'png':'jpeg',quality);await fsp.writeFile(path.join(OUT,file),buffer);return{file,width:canvas.width,height:canvas.height,bytes:buffer.length,sha256:hash(buffer)};}
async function main(){
  await fsp.mkdir(path.join(OUT,'all88'),{recursive:true});
  const photoPaths=Array.from({length:4},(_,i)=>`design/play-store/print-booth-v1/demo-photos/demo_0${i+1}.jpg`),photos=[];
  for(const p of photoPaths)photos.push(await loadImage(path.join(ROOT,p)));
  const requests=[['birthday','birthday','FOUR_VERTICAL',825],['couple','couple-100','FOUR_VERTICAL',825],['graduation','graduation','FOUR_VERTICAL',825],['travel','travel-memory','FOUR_VERTICAL',825],['two','birthday','TWO_HORIZONTAL',1170],['four','couple-100','FOUR_GRID',1170],['six','travel-memory','SIX_COLLAGE',1170]];
  const files=[];
  for(const [key,id,layout,width]of requests){const {canvas,geometry:g}=await render(theme(id),layout,width,photos);files.push({...await save(canvas,key+'.jpg'),key,themeId:id,layoutId:layout,photoCount:g.cells.length,photoSlots:g.cells,headerHeight:g.header,paper:theme(id).paperColor,photoSources:photoPaths});canvas.width=1;canvas.height=1;console.log('ready '+key);}
  for(const t of catalog.themes){const {canvas}=await render(t,'FOUR_GRID',390);files.push({...await save(canvas,'all88/'+t.id+'.png'),key:'gallery-'+t.id,themeId:t.id,categoryId:t.categoryId,layoutId:'FOUR_GRID',photoCount:0,photoSlots:'neutral placeholders, not photographs',manualRecord:t.isManualRecord});canvas.width=1;canvas.height=1;}
  // Every one of the 88 themes appears once, no duplicated or omitted tile.
  if(catalog.themes.length!==88||new Set(catalog.themes.map(t=>t.id)).size!==88)throw Error('The gallery requires 88 unique theme IDs.');
  const wall=createCanvas(1920,3720),wc=wall.getContext('2d');wc.fillStyle='#F6F3EC';wc.fillRect(0,0,wall.width,wall.height);
  for(let i=0;i<catalog.themes.length;i++){const im=await loadImage(path.join(OUT,'all88',catalog.themes[i].id+'.png')),w=220,h=im.height*w/im.width;wc.drawImage(im,20+i%8*240,20+Math.floor(i/8)*336,w,h);}
  files.push({...await save(wall,'all-88.jpg'),key:'all88',themeIds:catalog.themes.map(t=>t.id),grid:{columns:8,rows:11},photoCount:0});
  wall.width=1;wall.height=1;
  // A second, denser wall fits the existing reel's lower-stage viewport better.
  // The last seven themes are centered; every tile keeps the same scale.
  const overview=createCanvas(2160,3380),oc=overview.getContext('2d');oc.fillStyle='#F6F3EC';oc.fillRect(0,0,overview.width,overview.height);
  for(let i=0;i<catalog.themes.length;i++){
    const im=await loadImage(path.join(OUT,'all88',catalog.themes[i].id+'.png')),row=Math.floor(i/9),rowCount=Math.min(9,catalog.themes.length-row*9),offset=(9-rowCount)*120,w=220,h=im.height*w/im.width;
    oc.drawImage(im,20+offset+i%9*240,20+row*336,w,h);
  }
  files.push({...await save(overview,'overview-88.jpg'),key:'overview88',themeIds:catalog.themes.map(t=>t.id),grid:{columns:9,rows:10,lastRowCount:7,lastRowCentered:true},photoCount:0});
  overview.width=1;overview.height=1;
  const sheet=createCanvas(1800,2000),sc=sheet.getContext('2d');sc.fillStyle='#F6F3EC';sc.fillRect(0,0,1800,2000);
  for(let i=0;i<requests.length;i++){const im=await loadImage(path.join(OUT,requests[i][0]+'.jpg')),box={x:30+i%4*445,y:30+Math.floor(i/4)*990,w:415,h:930},s=Math.min(box.w/im.width,box.h/im.height);sc.drawImage(im,box.x+(box.w-im.width*s)/2,box.y,im.width*s,im.height*s);text(sc,requests[i][0],box.x+box.w/2,box.y+955,22,400,'Pocket Mono','#3E443E');}
  files.push({...await save(sheet,'representative-sheet.jpg'),key:'review-sheet'});
  const sourcePaths=[CAT_PATH,'app/src/main/java/com/pocket4cut/frame/CollageLayoutMath.kt','app/src/main/java/com/pocket4cut/frame/FrameLayouts.kt','app/src/main/java/com/pocket4cut/frame/FrameTheme.kt','app/src/main/java/com/pocket4cut/frame/rendering/OccasionFramePainter.kt',...photoPaths,...fonts.map(f=>f[0]),...catalog.themes.map(t=>'app/src/main/assets/'+t.atlas.assetPath)];
  await fsp.writeFile(path.join(OUT,'input-manifest.json'),JSON.stringify({created:'2026-09-27',method:'Editorial Canvas compositions using the current Android runtime catalog and original bundled WebP atlases. Layout math and painter rules are transcribed from the listed Kotlin sources. NOT generated by Android or captured from a device.',geometry:'No caption or date band; current base-theme defaults, current asymmetric six-cut layout version 2. Four vertical scales the 1650x4920 production geometry.',typographyLimit:'Bundled BM fonts are reused exactly; Android generic serif/italic/monospace/sans-serif are represented by installed Georgia/Georgia Italic/Consolas/Malgun. Font metrics and rasterization can differ from Android. No exact pixel parity claim.',header:'Common Pocket 4Cut / NN header subtitle omitted, matching current 2026-09-27 policy. Theme titles and existing footer brand retained. Hangul Day specific letter line retained.',manualRecords:'No weather, location, D-day, date or count data is automatically inserted. Empty slots in the 88-theme wall are neutral placeholders.',photoProvenance:'Only four pre-existing generated fictional adult sample images; repeated for six slots. No user or device camera photos.',inputs:sourcePaths.map(file=>({file,sha256:fileHash(file)})),rendererSha256:hash(fs.readFileSync(__filename)),files},null,2)+'\n');
  console.log(JSON.stringify({representatives:requests.length,gallery:catalog.themes.length,files:files.length}));
}
main().catch(error=>{console.error(error);process.exitCode=1;});
