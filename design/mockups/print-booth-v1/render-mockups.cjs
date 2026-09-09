const fs = require('fs');
const path = require('path');
const { pathToFileURL } = require('url');
const { chromium } = require('playwright');
const sharp = require('sharp');

const outputDir = __dirname;
const htmlPath = path.join(outputDir, 'screens.html');
const chromePath = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const screens = [
  '01_launch', '02_home', '03_frame_count', '04_camera', '05_photo_select',
  '06_layout_select', '07_frame_mode', '08_color_frame', '09_season_frame',
  '10_custom_frame', '11_edit', '12_detail_edit', '13_result', '14_gallery',
  '15_settings', '16_privacy_policy', '17_contact_feedback',
];

async function main() {
  const browser = await chromium.launch({ executablePath: chromePath, headless: true });
  const page = await browser.newPage({
    viewport: { width: 360, height: 800 },
    deviceScaleFactor: 3,
  });
  await page.goto(pathToFileURL(htmlPath).href);
  await page.evaluate(() => document.fonts.ready);
  await page.evaluate(() => document.querySelectorAll('.screen').forEach((node) => { node.style.display = 'none'; }));

  const checks = [];
  for (const id of screens) {
    const screen = page.locator(`[data-screen="${id}"]`);
    await screen.evaluate((node) => { node.style.display = 'flex'; });
    const check = await screen.evaluate((node) => {
      const bounds = node.getBoundingClientRect();
      const buttons = [...node.querySelectorAll('.btn')].map((button) => {
        const rect = button.getBoundingClientRect();
        return { label: button.textContent.trim(), height: rect.height, visible: rect.top >= bounds.top && rect.bottom <= bounds.bottom && rect.left >= bounds.left && rect.right <= bounds.right };
      });
      return { width: bounds.width, height: bounds.height, overflow: node.scrollHeight > node.clientHeight + 1, buttons };
    });
    if (check.width !== 360 || check.height !== 800 || check.overflow || check.buttons.some((button) => !button.visible || button.height < 40)) {
      throw new Error(`Clipped or compressed screen ${id}: ${JSON.stringify(check)}`);
    }
    await screen.screenshot({ path: path.join(outputDir, `${id}.png`) });
    const metadata = await sharp(path.join(outputDir, `${id}.png`)).metadata();
    if (metadata.width !== 1080 || metadata.height !== 2400) throw new Error(`Unexpected PNG size: ${id}`);
    checks.push({ file: `${id}.png`, pngWidth: metadata.width, pngHeight: metadata.height, layout: check });
    await screen.evaluate((node) => { node.style.display = 'none'; });
  }

  const styleboard = await browser.newPage({ viewport: { width: 360, height: 800 }, deviceScaleFactor: 3 });
  await styleboard.setContent(`
    <style>
      *{box-sizing:border-box}body{margin:0;background:#f3f0e8;color:#1b1b19;font-family:Arial,sans-serif}.board{width:360px;height:800px;padding:28px 20px;display:flex;flex-direction:column}.ey{color:#c83d2d;font-weight:700;font-size:10px;letter-spacing:1.5px}.title{font-size:28px;line-height:34px;font-weight:700;margin-top:8px}.copy{font-size:13px;line-height:20px;color:#6e6a63;margin-top:8px}.swatches{display:flex;gap:8px;margin-top:30px}.sw{height:48px;flex:1;border:1px solid #d0cbc1}.line{height:1px;background:#d0cbc1;margin:24px 0}.spec{display:flex;justify-content:space-between;border-bottom:1px solid #d0cbc1;padding:12px 0;font-size:13px}.spec b{font-size:15px}.paper{margin-top:32px;display:flex;gap:8px;align-items:flex-start}.strip{width:78px;padding:5px;border:1px solid #d0cbc1;background:#fbfaf6;box-shadow:0 4px 8px rgba(27,27,25,.16);transform:rotate(-3deg)}.photo{height:42px;margin-bottom:4px;background:#d9ae8e}.photo:nth-child(2){background:#93ad9a}.photo:nth-child(3){background:#ddbd68}.photo:nth-child(4){background:#aba3bd}.notes{font-size:11px;line-height:18px;color:#6e6a63;flex:1}.notes strong{color:#1b1b19;display:block;font-size:14px;margin-bottom:5px}
    </style>
    <div class="board"><div class="ey">POCKET 4CUT / PRINT BOOTH V1</div><div class="title">인화지처럼 담백하게,<br>촬영 부스처럼 분명하게.</div><div class="copy">종이·잉크·등록 레드 세 가지를 기본으로 삼아 화면마다 같은 물성을 유지합니다.</div><div class="swatches"><div class="sw" style="background:#f3f0e8"></div><div class="sw" style="background:#fbfaf6"></div><div class="sw" style="background:#1b1b19"></div><div class="sw" style="background:#c83d2d"></div></div><div class="line"></div><div class="spec"><b>RADIUS</b><span>2 / 4 / 6 / 8 dp</span></div><div class="spec"><b>SHADOW</b><span>사진 프리뷰만 1개</span></div><div class="spec"><b>TYPE</b><span>UI 400–700 / 작은 영문 라벨</span></div><div class="spec"><b>RULE</b><span>둥근 것은 기능이 설명할 때만</span></div><div class="paper"><div class="strip"><div class="photo"></div><div class="photo"></div><div class="photo"></div><div class="photo"></div></div><div class="notes"><strong>PHOTO BOOTH OBJECT</strong>셀카 화면과 결과물은 각진 인화지로 강조하고, 버튼과 목록은 얇은 선으로 조용히 정리합니다.</div></div><div style="flex:1"></div><div class="ey" style="color:#6e6a63">DESIGN HANDOFF / 2026.09</div></div>
  `);
  await styleboard.screenshot({ path: path.join(outputDir, '00_styleboard.png') });
  await styleboard.close();
  await browser.close();

  try {
    const files = ['00_styleboard', ...screens].map((id) => path.join(outputDir, `${id}.png`));
    const meta = await sharp(files[0]).metadata();
    const thumbW = 270;
    const thumbH = 600;
    const cols = 4;
    const rows = Math.ceil(files.length / cols);
    const composites = [];
    for (let i = 0; i < files.length; i += 1) {
      composites.push({ input: await sharp(files[i]).resize(thumbW, thumbH).png().toBuffer(), left: (i % cols) * thumbW, top: Math.floor(i / cols) * thumbH });
    }
    await sharp({ create: { width: cols * thumbW, height: rows * thumbH, channels: 4, background: '#ded9cf' } }).composite(composites).png().toFile(path.join(outputDir, 'contact-sheet.png'));
    fs.writeFileSync(path.join(outputDir, 'render-validation.json'), JSON.stringify({ kind: 'design mockups; not Android device screenshots', screenCount: screens.length, pngSize: [1080, 2400], checks }, null, 2) + '\n');
    console.log(`Rendered ${screens.length} screen mockups and a styleboard at ${meta.width}x${meta.height}; contact sheet ready.`);
  } catch (error) {
    throw new Error(`Contact sheet or validation export failed: ${error.message}`);
  }
}

main().catch((error) => { console.error(error); process.exitCode = 1; });
