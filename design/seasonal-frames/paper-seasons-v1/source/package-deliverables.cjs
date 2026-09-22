/*
 * Package actual Android-rendered seasonal exports. This script does not redraw
 * frame artwork or invent photo-slot geometry. It only fits existing PNGs into
 * labelled contact sheets and archives the original full-resolution files:
 * 72 transparent frame PNGs and 32 opaque fictional-photo JPEG examples.
 *
 * Requirements: sharp, @napi-rs/canvas and jszip (no new dependency is installed).
 * Usage: node design/seasonal-frames/paper-seasons-v1/source/package-deliverables.cjs
 *        Add --overview-only to inspect contact sheets before packaging.
 */
const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { pipeline } = require('node:stream/promises');
const sharp = require('sharp');
const { createCanvas, loadImage, GlobalFonts } = require('@napi-rs/canvas');
const JSZip = require('jszip');

const ROOT = path.resolve(__dirname, '../../../..');
const BASE = path.resolve(__dirname, '..');
const DELIVERABLES = path.join(BASE, 'deliverables');
const ARCHIVE_NAME = 'pocket4cut-paper-seasons-v1.zip';
const PAPER = '#f7f3eb';
const SLOT_PAPER = '#e5e1da';
const INK = '#343b34';
const SEASONS = [
  { id: 'spring', label: '봄', title: '봄의 정원', ink: '#a14d63' },
  { id: 'summer', label: '여름', title: '여름 바닷가', ink: '#2c6595' },
  { id: 'autumn', label: '가을', title: '가을의 기록', ink: '#89543c' },
  { id: 'winter', label: '겨울', title: '포근한 겨울', ink: '#365f4f' },
];
const LAYOUTS = [
  { id: 'TWO_VERTICAL', label: '2컷 · 세로' },
  { id: 'TWO_HORIZONTAL', label: '2컷 · 가로' },
  { id: 'FOUR_VERTICAL', label: '4컷 · 클래식' },
  { id: 'FOUR_GRID', label: '4컷 · 격자' },
  { id: 'FOUR_HORIZONTAL', label: '4컷 · 가로' },
  { id: 'SIX_GRID_2X3', label: '6컷 · 세로 격자' },
  { id: 'SIX_GRID_3X2', label: '6컷 · 가로 격자' },
  { id: 'SIX_COLLAGE', label: '6컷 · 콜라주' },
];

const slash = value => value.split(path.sep).join('/');
const relative = value => slash(path.relative(BASE, value));
const hashBuffer = buffer => crypto.createHash('sha256').update(buffer).digest('hex');
async function exists(file) { try { return (await fsp.stat(file)).isFile(); } catch(error) { if(error.code==='ENOENT') return false; throw error; } }
async function hashFile(file) {
  const hash=crypto.createHash('sha256');
  for await(const chunk of fs.createReadStream(file)) hash.update(chunk);
  return hash.digest('hex');
}
function localFile(name) {
  assert.equal(typeof name,'string','Export path must be a string');
  assert(!path.isAbsolute(name) && !name.includes('\\') && !name.split('/').includes('..'),'Only safe relative export paths are accepted');
  const resolved=path.resolve(DELIVERABLES,name);
  assert(resolved.startsWith(DELIVERABLES+path.sep),'Export must remain inside deliverables');
  return resolved;
}
function text(ctx, content, x, y, size=32, align='left', color=INK) {
  ctx.font=`${size}px "Pocket Labels"`;
  ctx.fillStyle=color; ctx.textAlign=align; ctx.textBaseline='alphabetic';
  ctx.fillText(content,x,y);
}
function page(width,height) {
  const canvas=createCanvas(width,height),ctx=canvas.getContext('2d');
  ctx.fillStyle=PAPER;ctx.fillRect(0,0,width,height);
  return {canvas,ctx};
}
async function placeOriginal(ctx, record, box) {
  const width=Math.max(1,Math.floor(box.width)),height=Math.max(1,Math.floor(box.height));
  const buffer=await sharp(localFile(record.path)).resize(width,height,{fit:'inside',withoutEnlargement:true,kernel:'lanczos3'}).png().toBuffer();
  const image=await loadImage(buffer);
  const x=Math.round(box.x+(box.width-image.width)/2),y=Math.round(box.y+(box.height-image.height)/2);
  // The neutral paper is behind the original alpha, not baked into exported frames.
  ctx.fillStyle=SLOT_PAPER;ctx.fillRect(x,y,image.width,image.height);
  ctx.drawImage(image,x,y);
}
function frameFor(records,season,layout) {
  const found=records.filter(record=>record.kind==='transparent-frame' && record.season===season && record.layoutId===layout && record.layoutVersion===2 && record.captionBand===false);
  assert.equal(found.length,1,`Exactly one active blank-caption template required: ${season}/${layout}`);
  return found[0];
}
async function savePage(canvas,name) {
  const file=localFile(name);
  await fsp.writeFile(file,await canvas.encode('png'));
  return name;
}

async function createOverviews(records) {
  const font=path.join(ROOT,'app/src/main/assets/fonts/BMHANNAPro.ttf');
  assert(GlobalFonts.registerFromPath(font,'Pocket Labels'),'Cannot register bundled Korean label font');
  const created=[];
  {
    const {canvas,ctx}=page(3840,2920);
    text(ctx,'포켓네컷 · 사계절 프레임',72,114,76);
    text(ctx,'봄부터 겨울까지, 모든 2·4·6컷 레이아웃',76,184,38);
    text(ctx,'실제 Android 렌더러 출력 · 사진 칸은 투명 PNG입니다',76,240,29,'left','#697268');
    const startX=222,startY=318,tileWidth=426,tileHeight=592,gapX=20,gapY=48;
    for(let row=0;row<SEASONS.length;row++) {
      const season=SEASONS[row],rowY=startY+row*(tileHeight+gapY);
      text(ctx,season.label,72,rowY+58,48,'left',season.ink);
      for(let column=0;column<LAYOUTS.length;column++) {
        const layout=LAYOUTS[column],x=startX+column*(tileWidth+gapX);
        text(ctx,layout.label,x+tileWidth/2,rowY+28,29,'center');
        await placeOriginal(ctx,frameFor(records,season.id,layout.id),{x:x+12,y:rowY+56,width:tileWidth-24,height:tileHeight-66});
      }
    }
    text(ctx,'기본 프레임 32종 · 문구 영역 포함·이전 콜라주 호환 버전은 다운로드 폴더에 함께 들어 있습니다.',76,2876,28,'left','#697268');
    created.push(await savePage(canvas,'overview-all-seasons.png'));
  }
  for(const season of SEASONS) {
    const {canvas,ctx}=page(2400,2000);
    text(ctx,`포켓네컷 · ${season.title}`,64,110,74,'left',season.ink);
    text(ctx,'8가지 레이아웃의 실제 프레임 · 사진 칸이 투명한 원본 PNG',68,176,34);
    const tileWidth=550,tileHeight=810,startX=64,startY=260,gapX=24,gapY=36;
    for(let index=0;index<LAYOUTS.length;index++) {
      const layout=LAYOUTS[index],x=startX+(index%4)*(tileWidth+gapX),y=startY+Math.floor(index/4)*(tileHeight+gapY);
      text(ctx,layout.label,x+tileWidth/2,y+36,38,'center');
      await placeOriginal(ctx,frameFor(records,season.id,layout.id),{x:x+20,y:y+74,width:tileWidth-40,height:tileHeight-94});
    }
    text(ctx,'원본 파일은 frames 폴더에서 계절과 레이아웃별로 확인해 주세요.',68,1954,32,'left','#697268');
    created.push(await savePage(canvas,`overview-${season.id}.png`));
  }
  {
    const {canvas,ctx}=page(1080,1350);
    text(ctx,'같은 네 컷,',64,128,68);
    text(ctx,'네 가지 계절.',64,214,68);
    text(ctx,'포켓네컷 · 클래식 4컷 프레임 비교',68,279,31,'left','#697268');
    for(let index=0;index<SEASONS.length;index++) {
      const season=SEASONS[index],x=60+index*246;
      text(ctx,season.label,x+111,404,36,'center',season.ink);
      await placeOriginal(ctx,frameFor(records,season.id,'FOUR_VERTICAL'),{x,y:448,width:222,height:770});
    }
    text(ctx,'사진 칸이 투명한 PNG · 실제 앱 프레임',540,1292,28,'center','#697268');
    created.push(await savePage(canvas,'overview-classic-four.png'));
  }
  return created;
}

async function inspectInputs(manifest) {
  assert(Array.isArray(manifest.files),'manifest.files must be an array');
  assert.equal(manifest.templateCount,72,'Expected 72 transparent templates');
  assert.equal(manifest.exampleCount,32,'Expected 32 fictional-photo examples');
  assert.equal(manifest.files.length,104,'Expected 104 original Android output files');
  assert.equal(new Set(manifest.files.map(record=>record.path)).size,104,'Duplicate original export paths');
  assert.equal(manifest.files.filter(record=>record.kind==='transparent-frame').length,72);
  assert.equal(manifest.files.filter(record=>record.kind==='fictional-photo-composite').length,32);
  const missing=[];
  for(const record of manifest.files) if(!await exists(localFile(record.path))) missing.push(record.path);
  if(missing.length) {
    console.log(`WAITING: ${missing.length} Android output file(s) have not been copied yet. No contact sheet or ZIP was changed.`);
    console.log(missing.slice(0,8).join('\n'));
    return false;
  }
  for(const record of manifest.files) {
    const metadata=await sharp(localFile(record.path)).metadata();
    if(record.kind==='transparent-frame') {
      assert.equal(metadata.format,'png',record.path);
      assert(metadata.hasAlpha,`Missing alpha: ${record.path}`);
      assert(record.path.endsWith('.png'),`Frame must retain PNG format: ${record.path}`);
    } else if(record.kind==='fictional-photo-composite') {
      assert.equal(metadata.format,'jpeg',record.path);
      assert(!metadata.hasAlpha,`JPEG example must be opaque: ${record.path}`);
      assert(/\.jpe?g$/i.test(record.path),`Example must use JPEG format: ${record.path}`);
    }
    assert.equal(metadata.width,record.width,record.path);
    assert.equal(metadata.height,record.height,record.path);
    assert(metadata.width<=8192 && metadata.height<=8192 && metadata.width*metadata.height<=16_000_000,`Output exceeds renderer bounds: ${record.path}`);
  }
  return true;
}

async function createAndVerifyZip(names,manifest) {
  assert.equal(new Set(names).size,names.length,'Duplicate archive entry');
  const zip=new JSZip(),entries=[];
  for(const name of names) {
    const file=localFile(name),stat=await fsp.stat(file);
    entries.push({path:name,bytes:stat.size,sha256:await hashFile(file)});
    // PNG and JPEG are already compressed; STORE avoids needless recompression.
    zip.file(name,fs.createReadStream(file),{binary:true,compression:'STORE'});
  }
  const archivePath=localFile(ARCHIVE_NAME);
  const pendingPath=localFile(`.${ARCHIVE_NAME}.pending`);
  await pipeline(zip.generateNodeStream({type:'nodebuffer',streamFiles:true,compression:'STORE'}),fs.createWriteStream(pendingPath));
  // Read the completed ZIP back; verify every decompressed entry independently.
  const archive=await fsp.readFile(pendingPath),reopened=await JSZip.loadAsync(archive);
  const actualNames=Object.values(reopened.files).filter(entry=>!entry.dir).map(entry=>entry.name).sort();
  assert.deepEqual(actualNames,[...names].sort(),'ZIP entry inventory mismatch');
  for(const entry of entries) {
    const restored=await reopened.file(entry.path).async('nodebuffer');
    assert.equal(restored.length,entry.bytes,`ZIP byte count mismatch: ${entry.path}`);
    assert.equal(hashBuffer(restored),entry.sha256,`ZIP hash mismatch: ${entry.path}`);
  }
  const validation={schemaVersion:1,archive:ARCHIVE_NAME,archiveBytes:archive.length,archiveSha256:hashBuffer(archive),
    verifiedBy:'Reopened ZIP; every extracted entry length and SHA-256 matched its original file',
    originalRenderer:manifest.renderer,originalFrames:72,originalExamples:32,entryCount:entries.length,
    includesInstagramImage:names.includes('instagram-promo.png'),entries};
  // Publish only after a complete read-back; a failed run keeps an earlier ZIP intact.
  await fsp.rename(pendingPath,archivePath);
  await fsp.writeFile(localFile('package-validation.json'),JSON.stringify(validation,null,2)+'\n');
  console.log(`VERIFIED: ${relative(archivePath)} (${archive.length} bytes; ${entries.length} files; every SHA-256 matched).`);
}

async function main() {
  const manifestPath=path.join(DELIVERABLES,'manifest.json');
  if(!await exists(manifestPath)) {
    console.log('WAITING: deliverables/manifest.json is not present. Copy the actual Android exports before running. Nothing was changed.');
    return;
  }
  const manifest=JSON.parse(await fsp.readFile(manifestPath,'utf8'));
  if(!await inspectInputs(manifest)) return;
  const overviews=await createOverviews(manifest.files);
  console.log(`Created ${overviews.length} labelled contact sheets from the 32 current Android templates.`);
  if(process.argv.includes('--overview-only')) return;
  if(!await exists(localFile('README-USAGE.txt'))) {
    console.log('WAITING: contact sheets are ready, but README-USAGE.txt is missing. ZIP creation was skipped; an existing ZIP was not changed.');
    return;
  }
  const names=[...manifest.files.map(record=>record.path),'manifest.json','README-USAGE.txt',...overviews];
  if(await exists(localFile('instagram-promo.png'))) names.push('instagram-promo.png');
  else console.log('NOTE: instagram-promo.png is not present; rerun after it is saved to include it in the ZIP.');
  await createAndVerifyZip(names,manifest);
}
main().catch(error=>{console.error(error);process.exitCode=1;});
