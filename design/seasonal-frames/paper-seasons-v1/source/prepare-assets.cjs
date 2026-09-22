/*
 * Mechanical export only: retain the generated masters and their original alpha,
 * isolate the six existing motifs, and centre them in fixed 512px atlas cells.
 * No object is redrawn, recoloured, or placed on an opaque background.
 * Run from any directory with Node.js and sharp available.
 */
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const sharp = require('sharp');

const ROOT = path.resolve(__dirname, '../../../..');
const OUTPUT = path.join(ROOT, 'app/src/main/assets/seasonal');
const QA = path.join(ROOT, 'build/seasonal-preview');
const CELL = 512;
const PADDING = 14;
const ALPHA_BOUND_THRESHOLD = 8;
const GUARD = 3;
// ROI edges follow the transparent corridors in each generated master, not an
// assumed equal source grid. Summer excludes the faint generated cell guides.
const SEASONS = [
  { id: 'spring', names: ['cherry-blossom', 'tulip-bouquet', 'rose-ribbon', 'butterfly', 'strawberry', 'pink-flower'],
    rois: [[0,0,535,560],[535,0,1030,560],[1030,0,1536,560], [0,560,535,1024],[535,560,1030,1024],[1030,560,1536,1024]] },
  { id: 'summer', names: ['parasol', 'lifebuoy', 'seashells', 'lemon', 'seagull', 'sun'],
    rois: [[0,0,516,509],[524,0,1013,509],[1021,0,1536,509], [0,515,516,1024],[524,515,1013,1024],[1021,515,1536,1024]] },
  { id: 'autumn', names: ['gingko-maple-leaves', 'coffee-cup', 'acorns', 'knit-scarf', 'books', 'pear'],
    rois: [[0,0,520,505],[520,0,1040,505],[1040,0,1536,505], [0,505,520,1024],[520,505,1040,1024],[1040,505,1536,1024]] },
  { id: 'winter', names: ['mittens', 'snowman', 'snowy-pine', 'cocoa-cup', 'snowflake', 'tartan-ribbon'],
    rois: [[0,0,530,520],[530,0,1030,520],[1030,0,1536,520], [0,520,530,1024],[530,520,1010,1024],[1010,520,1536,1024]] },
];

const sha256 = buffer => crypto.createHash('sha256').update(buffer).digest('hex');

function bounds(data, width, [x0,y0,x1,y1]) {
  let left=x1, top=y1, right=-1, bottom=-1;
  for(let y=y0;y<y1;y++) for(let x=x0;x<x1;x++) {
    if(data[(y*width+x)*4+3] > ALPHA_BOUND_THRESHOLD) {
      left=Math.min(left,x); top=Math.min(top,y);
      right=Math.max(right,x); bottom=Math.max(bottom,y);
    }
  }
  if(right<left) throw new Error('Empty source motif');
  left=Math.max(x0,left-GUARD); top=Math.max(y0,top-GUARD);
  right=Math.min(x1-1,right+GUARD); bottom=Math.min(y1-1,bottom+GUARD);
  return { left, top, width:right-left+1, height:bottom-top+1 };
}

async function alphaStats(buffer) {
  const {data,info}=await sharp(buffer).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  let zero=0, nearOpaque=0, maxAlpha=0, boundaryMaxAlpha=0;
  const cells=Array.from({length:6},()=>({minX:CELL,minY:CELL,maxX:-1,maxY:-1,nearOpaquePixels:0}));
  for(let y=0;y<info.height;y++) for(let x=0;x<info.width;x++) {
    const alpha=data[(y*info.width+x)*4+3];
    if(alpha===0) zero++;
    if(alpha>=250) nearOpaque++;
    maxAlpha=Math.max(maxAlpha,alpha);
    const cell=cells[Math.floor(y/CELL)*3+Math.floor(x/CELL)];
    const dx=x%CELL,dy=y%CELL;
    if(alpha>0) {
      cell.minX=Math.min(cell.minX,dx); cell.maxX=Math.max(cell.maxX,dx);
      cell.minY=Math.min(cell.minY,dy); cell.maxY=Math.max(cell.maxY,dy);
    }
    if(alpha>=250) cell.nearOpaquePixels++;
    if(dx===0||dy===0||dx===CELL-1||dy===CELL-1) boundaryMaxAlpha=Math.max(boundaryMaxAlpha,alpha);
  }
  if(boundaryMaxAlpha!==0) throw new Error('Atlas cell has nontransparent outer edge');
  for(const cell of cells) {
    if(cell.minX<PADDING||cell.minY<PADDING||cell.maxX>=CELL-PADDING||cell.maxY>=CELL-PADDING) {
      throw new Error('Motif exceeds cell safe padding');
    }
    if(cell.nearOpaquePixels<1000) throw new Error('Motif has no sufficiently opaque interior');
  }
  return {width:info.width,height:info.height,channels:info.channels,zeroAlphaPixels:zero,
    nearOpaquePixels:nearOpaque,maxAlpha,boundaryMaxAlpha,cells};
}

async function main() {
  await fs.mkdir(OUTPUT,{recursive:true});
  await fs.mkdir(QA,{recursive:true});
  const validation={schemaVersion:1,method:'mechanical crop, alpha trim and downscale; generated art unchanged',
    atlas:{width:CELL*3,height:CELL*2,cellSize:CELL,columns:3,rows:2,padding:PADDING,colorSpace:'sRGB',format:'RGBA PNG'},
    alpha:{preserved:true,boundsThreshold:ALPHA_BOUND_THRESHOLD,boundsGuardPixels:GUARD,
      note:'The generated masters peak at alpha 254, not 255. Interior alpha >=250 is validated; no opaque fill or alpha remapping is applied.'},seasons:[]};
  const previewLayers=[];
  for(let seasonIndex=0;seasonIndex<SEASONS.length;seasonIndex++) {
    const season=SEASONS[seasonIndex];
    const sourcePath=path.join(__dirname,`${season.id}-atlas.png`);
    const master=await fs.readFile(sourcePath);
    const {data,info}=await sharp(master).ensureAlpha().raw().toBuffer({resolveWithObject:true});
    if(info.width!==1536||info.height!==1024||info.channels!==4) throw new Error(`Unexpected source dimensions: ${season.id}`);
    const layers=[],motifs=[];
    for(let index=0;index<season.rois.length;index++) {
      const rect=bounds(data,info.width,season.rois[index]);
      const scale=Math.min(1,(CELL-PADDING*2)/rect.width,(CELL-PADDING*2)/rect.height);
      const width=Math.max(1,Math.floor(rect.width*scale));
      const height=Math.max(1,Math.floor(rect.height*scale));
      const tile=await sharp(master).extract(rect).resize(width,height,{kernel:'lanczos3'}).toColourspace('srgb').png().toBuffer();
      const left=(index%3)*CELL+Math.floor((CELL-width)/2);
      const top=Math.floor(index/3)*CELL+Math.floor((CELL-height)/2);
      layers.push({input:tile,left,top});
      motifs.push({index,name:season.names[index],regionOfInterest:season.rois[index],sourceRect:rect,
        destinationRect:{left,top,width,height},scale});
    }
    const atlas=await sharp({create:{width:CELL*3,height:CELL*2,channels:4,background:{r:0,g:0,b:0,alpha:0}}})
      .composite(layers).toColourspace('srgb').png({compressionLevel:9,adaptiveFiltering:true}).toBuffer();
    const result=await alphaStats(atlas);
    const outputPath=path.join(OUTPUT,`${season.id}.png`);
    await fs.writeFile(outputPath,atlas);
    validation.seasons.push({id:season.id,source:`source/${season.id}-atlas.png`,sourceBytes:master.length,
      sourceSha256:sha256(master),output:path.relative(ROOT,outputPath).split(path.sep).join('/'),outputBytes:atlas.length,
      outputSha256:sha256(atlas),...result,motifs});
    for(let paper=0;paper<2;paper++) {
      const background=paper===0?'#f7f3e9':'#242925';
      const panel=await sharp(atlas).flatten({background}).resize(768,512).png().toBuffer();
      previewLayers.push({input:panel,left:paper*768,top:seasonIndex*562+50});
    }
    // Deliberately font-free so QA export also works in headless environments.
    // Rows: spring, summer, autumn, winter; columns: light paper, dark paper.
    const heading=await sharp({create:{width:1536,height:50,channels:3,background:'#e6e1d6'}}).png().toBuffer();
    previewLayers.push({input:heading,left:0,top:seasonIndex*562});
    console.log(`${season.id}: ${atlas.length} bytes, ${result.width}x${result.height}, alpha edges ${result.boundaryMaxAlpha}`);
  }
  await sharp({create:{width:1536,height:562*4,channels:3,background:'#e6e1d6'}}).composite(previewLayers)
    .png().toFile(path.join(QA,'atlas-qa.png'));
  await fs.writeFile(path.resolve(__dirname,'../asset-validation.json'),JSON.stringify(validation,null,2)+'\n');
  console.log('QA: build/seasonal-preview/atlas-qa.png');
}
main().catch(error=>{console.error(error);process.exitCode=1;});
