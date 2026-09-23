/* Static design contact sheets / previews / ZIPs. Does not run app tests. */
'use strict';
const fs=require('node:fs'),fsp=require('node:fs/promises'),path=require('node:path'),crypto=require('node:crypto');
const {pipeline}=require('node:stream/promises');
const sharp=require('sharp');sharp.concurrency(2);sharp.cache(false);
const {createCanvas,loadImage,GlobalFonts}=require('@napi-rs/canvas');
const JSZip=require('jszip');
const BASE=path.resolve(__dirname,'..'),ROOT=path.resolve(BASE,'../../..'),OUT=path.join(BASE,'deliverables');
const catalog=JSON.parse(fs.readFileSync(path.join(__dirname,'catalog.json'),'utf8'));
const fileExists=p=>fs.existsSync(p),slash=p=>p.split(path.sep).join('/');
const ARCHIVE_LIMIT_BYTES=85*1024*1024;
// STORE compression keeps payload sizes unchanged. Reserve ample ZIP header and
// filename space as well, so the actual archive remains below the 85 MiB cap.
const ARCHIVE_HEADER_RESERVE=64*1024;
GlobalFonts.registerFromPath('C:/Windows/Fonts/malgun.ttf','Pocket Labels');
GlobalFonts.registerFromPath(path.join(ROOT,'app/src/main/assets/fonts/BMHANNAPro.ttf'),'Pocket Display');
const text=(c,s,x,y,size=30,color='#33382F',align='left',font='Pocket Labels')=>{
  c.fillStyle=color;c.font=`${size}px "${font}"`;c.textAlign=align;c.textBaseline='alphabetic';c.fillText(s,x,y);
};
const recordFor=t=>{const p=path.join(BASE,'records',t.id+'-frames.json');return fileExists(p)?JSON.parse(fs.readFileSync(p,'utf8')):null;};
const ready=catalog.themes.filter(t=>recordFor(t));
const photoPaths=[1,2,3,4].map(n=>path.join(ROOT,`design/play-store/print-booth-v1/demo-photos/demo_0${n}.jpg`));
function fitFill(ctx,image,c) {
  const w=c[2]-c[0],h=c[3]-c[1],s=Math.max(w/image.width,h/image.height),sw=w/s,sh=h/s;
  ctx.drawImage(image,(image.width-sw)/2,(image.height-sh)/2,sw,sh,c[0],c[1],w,h);
}
async function example(t,photos) {
  const r=recordFor(t).files.find(f=>f.layoutId==='FOUR_GRID'&&!f.captionBand&&f.layoutVersion===2);
  const c=createCanvas(r.width,r.height),ctx=c.getContext('2d');ctx.fillStyle=t.paper;ctx.fillRect(0,0,r.width,r.height);
  r.photoSlots.forEach((s,i)=>fitFill(ctx,photos[i%photos.length],s));ctx.drawImage(await loadImage(path.join(OUT,r.path)),0,0);
  const p=`examples/${String(t.index).padStart(2,'0')}-${t.id}.jpg`;
  await fsp.mkdir(path.dirname(path.join(OUT,p)),{recursive:true});
  await sharp(await c.encode('png')).resize({width:1175,withoutEnlargement:true}).jpeg({quality:93,chromaSubsampling:'4:4:4'}).toFile(path.join(OUT,p));
  c.width=1;c.height=1;return p;
}
async function board(themes,title,file,cols=4,tileW=570,tileH=820) {
  const width=cols*tileW+120,rows=Math.ceil(themes.length/cols),height=rows*tileH+290;
  const c=createCanvas(width,height),ctx=c.getContext('2d');ctx.fillStyle='#F4EFE4';ctx.fillRect(0,0,width,height);
  text(ctx,title,60,97,52,'#34392E','left','Pocket Display');
  text(ctx,`POCKET4CUT  /  EVERYDAY EDITIONS  /  ${themes.length} DESIGNS`,62,146,25,'#697062');
  for(let i=0;i<themes.length;i++) {
    const t=themes[i],r=recordFor(t).files.find(f=>f.layoutId==='FOUR_GRID'&&!f.captionBand&&f.layoutVersion===2);
    const x=60+i%cols*tileW,y=208+Math.floor(i/cols)*tileH;
    text(ctx,`${String(t.index).padStart(2,'0')}  ${t.name}`,x+tileW/2,y+26,26,'#464C3F','center');
    const png=await sharp(path.join(OUT,r.path)).resize(tileW-54,tileH-84,{fit:'inside',withoutEnlargement:true}).png().toBuffer();
    const im=await loadImage(png),px=x+(tileW-im.width)/2,py=y+53+(tileH-84-im.height)/2;
    ctx.fillStyle='#DADBD2';ctx.fillRect(px,py,im.width,im.height);ctx.drawImage(im,px,py);
  }
  text(ctx,'사진 칸이 투명한 PNG · 날짜/장소 등은 디자인 예시 · 앱 적용 및 테스트는 다음 단계',62,height-40,23,'#73786B');
  await fsp.writeFile(path.join(OUT,file),await c.encode('jpeg',92));c.width=1;c.height=1;
}
async function archive(files,name) {
  const zip=new JSZip();
  for(const file of files) zip.file(file,fs.createReadStream(path.join(OUT,file)),{binary:true,date:new Date('2026-09-23T00:00:00Z')});
  const p=path.join(OUT,'downloads',name);await fsp.mkdir(path.dirname(p),{recursive:true});
  await pipeline(zip.generateNodeStream({streamFiles:true,compression:'STORE'}),fs.createWriteStream(p));
  const stat=await fsp.stat(p),h=crypto.createHash('sha256');for await(const chunk of fs.createReadStream(p))h.update(chunk);
  return {path:slash(path.relative(OUT,p)),bytes:stat.size,sha256:h.digest('hex'),entries:files.length};
}
function categoryBatches(category,themes,exampleMap) {
  const shared=['README-USAGE.txt',`overview-${category.id}.jpg`];
  const sizeOf=names=>names.reduce((total,name)=>total+fs.statSync(path.join(OUT,name)).size,0);
  const sharedBytes=sizeOf(shared)+ARCHIVE_HEADER_RESERVE;
  const batches=[];let current={themes:[],files:[...shared],estimatedBytes:sharedBytes};
  for(const theme of themes) {
    const record=recordFor(theme);
    if(record.files.length!==18) throw Error(`Theme ${theme.id} does not contain all 18 frame PNGs`);
    // A theme is an indivisible download unit: all layouts, its overview and its
    // photo-filled example always stay in the same independently usable ZIP.
    const names=[...record.files.map(file=>file.path),record.overview,exampleMap[theme.id]];
    const bytes=sizeOf(names);
    if(sharedBytes+bytes>ARCHIVE_LIMIT_BYTES) {
      throw Error(`Theme ${theme.id} alone exceeds the 85 MiB archive limit; keep it intact and revise packaging explicitly`);
    }
    if(current.themes.length&&current.estimatedBytes+bytes>ARCHIVE_LIMIT_BYTES) {
      batches.push(current);current={themes:[],files:[...shared],estimatedBytes:sharedBytes};
    }
    current.themes.push(theme);current.files.push(...names);current.estimatedBytes+=bytes;
  }
  if(current.themes.length)batches.push(current);
  return batches;
}
function downloadMarkdown(archives) {
  const complete=archives.find(a=>a.kind==='complete');
  const lines=[
    '# 포켓네컷 기념일 프레임 다운로드',
    '',
    '88개 테마 · 사진창이 투명한 PNG 1,584장 · 2/4/6컷 전체 배치',
    '',
    ...(complete?[`[전체 프레임 + 인스타 홍보 이미지 한 번에 다운로드](${complete.path}) — ${(complete.bytes/1024/1024).toFixed(1)} MiB`,'']:[]),
    'ZIP은 이 작업 폴더에 만들어진 로컬 다운로드 파일입니다. Git에는 중복 대용량 ZIP 대신 원본 PNG와 재생성 스크립트를 보존합니다. Git에서 새로 받은 경우 README의 패키지 생성 명령을 실행하세요.',
    '',
    '[전체 디자인 모아보기](overview-all-occasions.jpg) · [사용 안내](README-USAGE.txt) · [파일별 규격](manifest.json)',
    '',
    '원하는 카테고리만 내려받으실 수 있습니다. 큰 카테고리는 각 ZIP이 85 MiB 이하가 되도록 나누었습니다.',
    '**각 Part는 독립적으로 압축을 풀 수 있는 일반 ZIP입니다.** 하나의 분할압축 파일이 아니므로 합치기 도구가 필요하지 않습니다.',
    '기념일 한 테마의 PNG 18장, 테마 모아보기와 사진 예시는 같은 ZIP에 모두 들어 있습니다. 각 ZIP에는 사용 안내와 해당 카테고리 모아보기도 반복 포함됩니다.',
    '카테고리 전체가 필요하면 그 카테고리의 모든 Part를 받으세요. 한 테마만 필요하면 아래 포함 목록을 보고 해당 Part만 받으시면 됩니다.',
    '',
    'PNG 18장은 현재 8개 배치의 문구 공간 없음/있음 16장과 구형 6컷 배치 호환 2장입니다. 파일명과 원본 해상도를 유지했으며, ZIP을 열고 원하는 PNG를 꺼내 사용하시면 됩니다.',
    '동일 폴더에 여러 Part를 풀 때 안내와 카테고리 모아보기는 같은 공통 파일입니다. 테마별 파일은 Part 사이에 중복되지 않습니다.',
    '',
    '이 패키지는 프레임 디자인 이미지입니다. 앱 적용·프레임 선택 화면·날짜/D-Day/장소/날씨 자동 입력 기능은 이번 이미지 제작 단계에 포함되지 않습니다. 예시 사진은 기존 가상 성인 이미지이며 날짜·이름·장소 등은 디자인 예시입니다.',
    '',
  ];
  for(const category of catalog.categories) {
    const parts=archives.filter(archive=>archive.category===category.id);if(!parts.length)continue;
    const themeCount=parts.reduce((sum,part)=>sum+part.themeIds.length,0);
    lines.push(`## ${category.label} — ${themeCount}개 테마`,'',`[카테고리 모아보기](overview-${category.id}.jpg)`,'');
    for(const part of parts) {
      const label=part.parts>1?`Part ${part.part}/${part.parts}`:'전체';
      lines.push(`- [${category.label} ${label} 다운로드](${part.path}) — ${(part.bytes/1024/1024).toFixed(1)} MiB · ${part.themeIds.length}개 테마 · PNG ${part.themeIds.length*18}장`);
      lines.push(`  - 포함: ${part.themeNames.join(' / ')}`);
    }
    lines.push('');
  }
  lines.push('[ZIP 크기·SHA-256 목록](download-manifest.json)','');
  return lines.join('\n');
}
async function main() {
  await fsp.mkdir(OUT,{recursive:true});
  if(!ready.length) throw Error('No finished frame designs');
  const photos=await Promise.all(photoPaths.map(loadImage));
  const exampleMap={};
  for(const t of ready) exampleMap[t.id]=await example(t,photos);
  const boards=[];
  for(const category of catalog.categories) {
    const themes=ready.filter(t=>t.category===category.id);if(!themes.length)continue;
    const file=`overview-${category.id}.jpg`;await board(themes,`포켓네컷 · ${category.label}`,file);boards.push({category:category.id,path:file});
  }
  await board(ready,`오늘을 기념하는 ${ready.length}가지 방법`,'overview-all-occasions.jpg',8,430,630);
  const files=ready.flatMap(t=>recordFor(t).files);
  const manifest={schemaVersion:1,created:'2026-09-23',plannedThemes:88,completedThemes:ready.length,transparentFrames:files.length,examples:ready.length,method:'Static design composition. Photo slots copied from saved Android Paper Seasons manifest; Android not executed.',notImplemented:'App application, category/search UI and dynamic metadata behavior are outside this image-first stage.',exampleValues:'Date, time, N, place, temperature and names are illustrative. Example photographs are pre-existing fictional generated adults, not users.',categories:catalog.categories,themes:ready.map(t=>({id:t.id,index:t.index,name:t.name,category:t.category,group:t.group,overview:recordFor(t).overview,example:exampleMap[t.id]})),files,boards};
  await fsp.writeFile(path.join(OUT,'manifest.json'),JSON.stringify(manifest,null,2)+'\n');
  if(process.argv.includes('--overview-only')) {console.log(JSON.stringify({ready:ready.length,frames:files.length,mode:'overviews-only'}));return;}
  if(ready.length!==88||files.length!==1584) throw Error('Incomplete collection: packages are not marked final.');
  const archives=[];
  for(const category of catalog.categories) {
    const themes=ready.filter(t=>t.category===category.id);if(!themes.length)continue;
    const batches=categoryBatches(category,themes,exampleMap);
    for(let i=0;i<batches.length;i++) {
      const result=await archive(batches[i].files,`pocket4cut-${category.id}${batches.length>1?'-part-'+(i+1):''}.zip`);
      if(result.bytes>ARCHIVE_LIMIT_BYTES) throw Error(`Archive ${result.path} exceeds the 85 MiB limit`);
      archives.push({...result,category:category.id,categoryLabel:category.label,part:i+1,parts:batches.length,
        themeIds:batches[i].themes.map(t=>t.id),themeNames:batches[i].themes.map(t=>t.name)});
    }
  }
  const completeFiles=[...files.map(f=>f.path),...ready.map(t=>recordFor(t).overview),...ready.map(t=>exampleMap[t.id]),...boards.map(b=>b.path),'overview-all-occasions.jpg','manifest.json','README-USAGE.txt','instagram-promo.png','instagram-promo-master-2160x2700.png'];
  archives.unshift({...await archive(completeFiles,'pocket4cut-all-88-frames.zip'),kind:'complete',themeIds:ready.map(t=>t.id)});
  await fsp.writeFile(path.join(OUT,'download-manifest.json'),JSON.stringify({archives,categoryArchiveLimitBytes:ARCHIVE_LIMIT_BYTES,
    splitPolicy:'Complete theme units only: 18 frame PNGs, per-theme overview and example stay together. Every independent ZIP repeats its category board and README-USAGE.txt.',
    scope:'Full-resolution frame PNGs, per-theme previews, category board and usage guide.'},null,2)+'\n');
  await fsp.writeFile(path.join(OUT,'DOWNLOADS.md'),downloadMarkdown(archives));
  console.log(JSON.stringify({themes:ready.length,frames:files.length,examples:ready.length,archives}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
