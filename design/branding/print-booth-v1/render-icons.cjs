/** Reproducible PNG exports from the SVG mark; requires sharp. */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const sharp = require('sharp');

const root = path.resolve(__dirname, '../../..');
const sourcePath = path.join(root, 'app/src/main/assets/branding/pocket_4cut_app_icon.svg');
const res = path.join(root, 'app/src/main/res');
const svg = fs.readFileSync(sourcePath, 'utf8');
const mark = svg.match(/<g id="photo-strip"[\s\S]*?<\/g>/)[0];
const paths = [...mark.matchAll(/<path[^>]* d="([^"]+)"/g)].map((entry) => entry[1]);
const foregroundXml = fs.readFileSync(path.join(res, 'drawable/ic_launcher_print_foreground.xml'), 'utf8');
const monoXml = fs.readFileSync(path.join(res, 'drawable/ic_launcher_monochrome.xml'), 'utf8');
assert.equal(paths.length, 2);
assert.deepEqual([...foregroundXml.matchAll(/android:pathData="([^"]+)"/g)].map((entry) => entry[1]), paths);
assert(monoXml.includes(`android:pathData="${paths.join(' ')}"`));
assert(monoXml.includes('android:fillType="evenOdd"'));
for (const xml of [foregroundXml, monoXml]) {
  assert(xml.includes('android:rotation="-15"'));
  assert(xml.includes('android:pivotX="54"'));
  assert(xml.includes('android:pivotY="54"'));
}
for (const name of ['ic_launcher.xml', 'ic_launcher_round.xml']) {
  const adaptiveXml = fs.readFileSync(path.join(res, 'mipmap-anydpi', name), 'utf8');
  assert(adaptiveXml.includes('@drawable/ic_launcher_print_foreground'));
  assert(adaptiveXml.includes('@drawable/ic_launcher_monochrome'));
  assert(adaptiveXml.includes('@drawable/ic_launcher_background'));
}
assert(fs.readFileSync(path.join(res, 'drawable/ic_launcher_background.xml'), 'utf8').includes('<solid android:color="#C83D2D" />'));

const frame = (body, viewBox = '0 0 108 108', size = 108) => `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="${viewBox}">${body}</svg>`;
const foreground = frame(mark);
const monoMark = `<g transform="rotate(-15 54 54)"><path fill="#000000" fill-rule="evenodd" d="${paths.join(' ')}"/></g>`;
const monochrome = frame(monoMark);
const densitySizes = { mdpi: [48, 108], hdpi: [72, 162], xhdpi: [96, 216], xxhdpi: [144, 324], xxxhdpi: [192, 432] };
const outputs = [];

async function render(input, size, output, mask) {
  let buffer = await sharp(Buffer.from(input), { density: 384 }).resize(size, size).ensureAlpha().toColourspace('srgb').withIccProfile('srgb').png({ compressionLevel: 9, palette: false }).toBuffer();
  if (mask) buffer = await sharp(buffer).composite([{ input: Buffer.from(frame(mask, '0 0 100 100', size)), blend: 'dest-in' }]).ensureAlpha().withIccProfile('srgb').png({ compressionLevel: 9, palette: false }).toBuffer();
  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, buffer);
  outputs.push(output);
  return buffer;
}

async function metadata(file) {
  const bytes = fs.readFileSync(file);
  const meta = await sharp(bytes).metadata();
  const { data, info } = await sharp(bytes).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  let min = 255, max = 0;
  for (let i = 3; i < data.length; i += info.channels) { min = Math.min(min, data[i]); max = Math.max(max, data[i]); }
  return { file: path.relative(root, file).replaceAll('\\', '/'), width: meta.width, height: meta.height, bytes: bytes.length, space: meta.space, bitDepth: bytes[24], pngColorType: bytes[25], channels: meta.channels, icc: Boolean(meta.icc), alphaMin: min, alphaMax: max };
}

async function main() {
  fs.copyFileSync(sourcePath, path.join(__dirname, 'app-icon-source.svg'));
  await render(svg, 512, path.join(__dirname, 'play-store-icon-512.png'));
  await render(svg, 1024, path.join(__dirname, 'app-icon-master-1024.png'));
  await render(foreground, 432, path.join(__dirname, 'adaptive-foreground-432.png'));
  await render(monochrome, 432, path.join(__dirname, 'adaptive-monochrome-432.png'));
  for (const [density, [legacySize, layerSize]] of Object.entries(densitySizes)) {
    await render(svg, legacySize, path.join(res, `mipmap-${density}/ic_launcher.png`));
    await render(svg, legacySize, path.join(res, `mipmap-${density}/ic_launcher_round.png`), '<circle cx="50" cy="50" r="50" fill="white"/>');
    await render(foreground, layerSize, path.join(res, `drawable-${density}/ic_launcher_foreground.png`));
  }

  // Mask previews use the visible 72dp crop of a full 108dp adaptive icon.
  const previewIcon = await sharp(Buffer.from(svg), { density: 384 }).resize(192).png().toBuffer();
  const maskShapes = [
    '<rect width="100" height="100" fill="white"/>',
    '<circle cx="50" cy="50" r="50" fill="white"/>',
    '<rect width="100" height="100" rx="24" fill="white"/>',
    '<path d="M50 0C88 0 100 12 100 50S88 100 50 100 0 88 0 50 12 0 50 0" fill="white"/>',
  ];
  const labels = ['FULL SQUARE', 'CIRCLE', 'ROUNDED', 'SQUIRCLE'];
  const composites = [];
  for (let index = 0; index < 4; index++) {
    const icon = await sharp(previewIcon).composite([{ input: Buffer.from(frame(maskShapes[index], '0 0 100 100', 192)), blend: 'dest-in' }]).png().toBuffer();
    composites.push({ input: icon, left: 52 + index * 250, top: 118 });
    const actual48 = await sharp(icon).resize(48).png().toBuffer();
    composites.push({ input: actual48, left: 124 + index * 250, top: 375 });
    const zoom48 = await sharp(actual48).resize(96, 96, { kernel: 'nearest' }).png().toBuffer();
    composites.push({ input: zoom48, left: 100 + index * 250, top: 464 });
  }
  const themedLight = frame(`<path fill="#DDD7CB" d="M18,18H90V90H18Z"/>${monoMark.replace('#000000', '#383329')}`, '18 18 72 72', 192);
  const themedDark = frame(`<path fill="#383329" d="M18,18H90V90H18Z"/>${monoMark.replace('#000000', '#DDD7CB')}`, '18 18 72 72', 192);
  for (const [index, themed] of [themedLight, themedDark].entries()) {
    composites.push({ input: await sharp(Buffer.from(themed)).png().toBuffer(), left: 52 + index * 250, top: 670 });
  }
  const outline = frame(`<rect width="108" height="108" fill="#F3F0E8"/><rect x="18" y="18" width="72" height="72" fill="none" stroke="#928C83" stroke-width=".5"/><circle cx="54" cy="54" r="33" fill="none" stroke="#C83D2D" stroke-width=".5"/>${mark}`, '0 0 108 108', 192);
  composites.push({ input: await sharp(Buffer.from(outline)).png().toBuffer(), left: 552, top: 670 });
  const text = `<svg width="1080" height="940" xmlns="http://www.w3.org/2000/svg"><rect width="1080" height="940" fill="#F3F0E8"/><g font-family="Arial,sans-serif" fill="#1B1B19"><text x="52" y="53" font-size="26" font-weight="700">POCKET 4CUT / ICON CHECK</text><text x="52" y="81" font-size="14" fill="#6E6A63">Print red + ivory + ink. Four photo windows. No baked shadows.</text>${labels.map((label, i) => `<text x="${52+i*250}" y="341" font-size="14" font-weight="700">${label}</text>`).join('')}<text x="52" y="405" font-size="12">48px</text><text x="52" y="585" font-size="12">48px enlarged 2x with nearest-neighbour pixels</text><text x="52" y="641" font-size="18" font-weight="700">THEMED ICONS / ADAPTIVE SAFE ZONE</text><text x="52" y="890" font-size="13">LIGHT THEME</text><text x="302" y="890" font-size="13">DARK THEME</text><text x="552" y="890" font-size="13">108dp layer / 66dp safe circle</text><text x="802" y="720" font-size="14">Four transparent</text><text x="802" y="744" font-size="14">windows survive tint.</text><text x="802" y="790" font-size="14">Rotation: -15 degrees</text><text x="802" y="814" font-size="14">Farthest corner: 32.45dp</text></g></svg>`;
  await sharp(Buffer.from(text)).composite(composites).ensureAlpha().withIccProfile('srgb').png().toFile(path.join(__dirname, 'icon-preview-grid.png'));
  outputs.push(path.join(__dirname, 'icon-preview-grid.png'));
  const result = await Promise.all(outputs.map(metadata));
  const storeIcon = result[0];
  assert.equal(storeIcon.width, 512); assert.equal(storeIcon.height, 512);
  assert.equal(storeIcon.pngColorType, 6); assert.equal(storeIcon.bitDepth, 8);
  assert.equal(storeIcon.alphaMin, 255); assert(storeIcon.bytes <= 1024 * 1024); assert(storeIcon.icc);
  const foregroundMeta = result.find((entry) => entry.file.endsWith('adaptive-foreground-432.png'));
  assert.equal(foregroundMeta.alphaMin, 0); assert.equal(foregroundMeta.alphaMax, 255);
  for (const [density, [legacySize, layerSize]] of Object.entries(densitySizes)) {
    for (const [relative, expected, minimumAlpha] of [
      [`mipmap-${density}/ic_launcher.png`, legacySize, 255],
      [`mipmap-${density}/ic_launcher_round.png`, legacySize, 0],
      [`drawable-${density}/ic_launcher_foreground.png`, layerSize, 0],
    ]) {
      const asset = result.find((entry) => entry.file.endsWith(relative));
      assert.equal(asset.width, expected); assert.equal(asset.height, expected);
      assert.equal(asset.pngColorType, 6); assert.equal(asset.bitDepth, 8);
      assert.equal(asset.alphaMin, minimumAlpha); assert.equal(asset.alphaMax, 255);
      assert.equal(asset.space, 'srgb'); assert(asset.icc);
    }
  }
  const corner = await sharp(path.join(__dirname, 'play-store-icon-512.png')).extract({ left: 0, top: 0, width: 1, height: 1 }).raw().toBuffer();
  assert.deepEqual([...corner], [200, 61, 45, 255]);
  const maxRadius = Math.hypot(18, 27);
  assert(maxRadius < 33);
  const { data: monoPixels, info } = await sharp(Buffer.from(monochrome)).resize(432).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  for (const y of [35.5, 47.5, 59.5, 71.5]) {
    const angle = -15 * Math.PI / 180;
    const rotatedX = 54 - (y - 54) * Math.sin(angle);
    const rotatedY = 54 + (y - 54) * Math.cos(angle);
    const alpha = monoPixels[(Math.floor(rotatedY * 4) * info.width + Math.floor(rotatedX * 4)) * info.channels + 3];
    assert.equal(alpha, 0);
  }
  const report = { date: '2026-09-10', source: path.relative(root, sourcePath).replaceAll('\\', '/'), renderer: `sharp ${sharp.versions.sharp} / librsvg ${sharp.versions.rsvg}`, vectorGeometryMatches: true, adaptiveSafeCircleRadiusDp: 33, maximumMarkRadiusDp: Number(maxRadius.toFixed(4)), monochromeWindowCentersTransparent: 4, limitations: ['Mask grid is a rendered design check, not a real-device launcher screenshot.', 'Android resource compilation is verified separately by the integrated project build.'], assets: result };
  fs.writeFileSync(path.join(__dirname, 'icon-validation.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify({ outputs: result.length, storeIcon, vectorGeometryMatches: true, safeZone: true, monochromeWindows: 4 }, null, 2));
}

main().catch((error) => { console.error(error); process.exitCode = 1; });
