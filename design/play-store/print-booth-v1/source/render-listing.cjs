const fs = require('node:fs');
const path = require('node:path');
const { createHash } = require('node:crypto');
const sharp = require('sharp');
const { chromium } = require('playwright');

const base = path.resolve(__dirname, '..');
const story = require('./story.json');
const output = path.join(base, 'phone-screenshots');
const rawDir = path.join(base, 'raw-screenshots');
const chrome = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const htmlEscape = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

async function main() {
  if (story.length !== 8 || new Set(story.map(card => card.id)).size !== 8) throw new Error('Expected eight unique listing cards');
  if (story.some(card => [...card.alt].length > 140)) throw new Error('Alternative text exceeds 140 characters');
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ executablePath: chrome, headless: true });
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
  const validation = [];
  try {
    for (let index = 0; index < story.length; index++) {
      const card = story[index];
      const raw = path.join(rawDir, `${card.screen}.png`);
      if (!fs.existsSync(raw)) throw new Error(`Missing actual Compose render: ${raw}`);
      const metadata = await sharp(raw).metadata();
      const bytes = fs.readFileSync(raw);
      const dark = card.theme === 'red';
      const title = card.title.map(htmlEscape).join('<br>');
      await page.setContent(`<!doctype html><html lang="ko"><meta charset="utf-8"><style>
        *{box-sizing:border-box}html,body{margin:0;width:1080px;height:1920px;overflow:hidden}
        body{background:${dark?'#C83D2D':'#F3F0E8'};color:${dark?'#FBFAF6':'#1B1B19'};font-family:"Malgun Gothic",Arial,sans-serif}
        .copy{position:absolute;left:76px;right:76px;top:58px;height:252px}
        .brand{font-size:21px;line-height:28px;letter-spacing:3px;font-weight:700;margin:0 0 22px}
        .brand span{float:right;font-size:19px;font-weight:400;letter-spacing:2px}
        h1{margin:0;font-size:64px;line-height:78px;letter-spacing:-3.5px;font-weight:700}
        p{margin:14px 0 0;font-size:27px;line-height:38px;letter-spacing:-.7px;color:${dark?'#F9E9E3':'#6E6A63'}}
        .stage{position:absolute;left:70px;right:70px;top:358px;height:1482px;display:flex;align-items:flex-start;justify-content:center}
        .app{display:block;max-width:100%;max-height:100%;width:auto;height:auto;border:1px solid ${dark?'#A23428':'#D0CBC1'};box-shadow:0 8px 20px #1B1B191C}
        .footer{position:absolute;bottom:30px;left:76px;right:76px;font-size:17px;line-height:24px;letter-spacing:2px;display:flex;justify-content:space-between;opacity:.8}
      </style><div class="copy"><div class="brand">POCKET / 4CUT <span>${String(index+1).padStart(2,'0')} — 08</span></div><h1>${title}</h1><p>${htmlEscape(card.description)}</p></div><div class="stage"><img class="app" alt="${htmlEscape(card.alt)}" src="data:image/png;base64,${bytes.toString('base64')}"></div><div class="footer"><span>SELF PHOTO BOOTH</span><span>POCKET 4CUT</span></div></html>`);
      await page.evaluate(async () => { await document.fonts.ready; await Promise.all([...document.images].map(image=>image.decode())); });
      const checks = await page.evaluate(() => {
        const copy=document.querySelector('.copy');
        const image=document.querySelector('.app').getBoundingClientRect();
        const paragraph=document.querySelector('p').getBoundingClientRect();
        return { textBottom:paragraph.bottom, imageBounds:{x:image.x,y:image.y,width:image.width,height:image.height,bottom:image.bottom}, copyFits:copy.scrollWidth<=copy.clientWidth, rootFits:document.documentElement.scrollHeight===1920 };
      });
      if (!checks.copyFits || !checks.rootFits || checks.textBottom>340 || checks.imageBounds.bottom>1841) throw new Error(`Overflow on ${card.id}: ${JSON.stringify(checks)}`);
      const screenshot=await page.screenshot();
      const file=path.join(output,`${card.id}.png`);
      await sharp(screenshot).flatten({background:dark?'#C83D2D':'#F3F0E8'}).removeAlpha().toColourspace('srgb').png({compressionLevel:9}).toFile(file);
      const finalMetadata=await sharp(file).metadata();
      if (finalMetadata.format!=='png'||finalMetadata.width!==1080||finalMetadata.height!==1920||finalMetadata.hasAlpha||finalMetadata.channels!==3||finalMetadata.space!=='srgb') throw new Error(`Invalid Play PNG: ${file}`);
      validation.push({file:path.basename(file),width:1080,height:1920,channels:finalMetadata.channels,space:finalMetadata.space,bytes:fs.statSync(file).size,sha256:createHash('sha256').update(fs.readFileSync(file)).digest('hex'),source:{file:path.basename(raw),width:metadata.width,height:metadata.height,sha256:createHash('sha256').update(bytes).digest('hex')},...checks});
    }
  } finally { await browser.close(); }
  const thumbnails=[];
  for(let index=0;index<story.length;index++) thumbnails.push({input:await sharp(path.join(output,`${story[index].id}.png`)).resize(270,480).png().toBuffer(),left:(index%4)*270,top:Math.floor(index/4)*480});
  await sharp({create:{width:1080,height:960,channels:3,background:'#F3F0E8'}}).composite(thumbnails).png().toFile(path.join(base,'overview.png'));
  fs.writeFileSync(path.join(base,'asset-validation.json'),JSON.stringify({phoneScreenshotCount:story.length,renderMethod:'Actual app Compose screenshots, with typography added outside app UI',validation},null,2)+'\n');
  fs.writeFileSync(path.join(base,'alt-text-ko.txt'),story.map(s=>`${s.id}.png\n${s.alt}`).join('\n\n')+'\n');
  console.log(`Exported ${story.length} RGB 1080x1920 phone screenshots.`);
}
main().catch(error=>{console.error(error);process.exitCode=1;});
