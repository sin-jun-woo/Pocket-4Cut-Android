/** Production derivatives only. Creative artwork and lettering are image_gen outputs. */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const sharp = require('sharp');

const base = __dirname;
const root = path.resolve(base, '../../..');
const res = path.join(root, 'app/src/main/res');
const listing = path.join(root, 'design/play-store/film-strip-v4');
const iconInput = path.join(base, 'source/icon-with-wordmark.png');
const featureInput = path.join(base, 'source/feature-with-wordmark.png');
const monoInput = fs.readFileSync(path.join(base, 'adaptive-monochrome-source.svg'));
const bg = { r: 27, g: 27, b: 25, alpha: 1 };
const densities = { mdpi: [48, 108], hdpi: [72, 162], xhdpi: [96, 216], xxhdpi: [144, 324], xxxhdpi: [192, 432] };
const outputs = [];
const pngOptions = { compressionLevel: 9, palette: false };

async function save(input, file, width, height, alpha = true) {
  let pipeline = sharp(input).resize(width, height, { fit: 'fill', kernel: 'lanczos3' }).toColourspace('srgb');
  pipeline = alpha ? pipeline.ensureAlpha() : pipeline.flatten({ background: bg }).removeAlpha();
  const bytes = await pipeline.withIccProfile('srgb').png(pngOptions).toBuffer();
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, bytes);
  outputs.push(file);
  return bytes;
}

function svg(body, width, height = width) {
  return Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height + '" viewBox="0 0 ' + width + ' ' + height + '">' + body + '</svg>');
}

async function mask(input, size, shape) {
  const bytes = await sharp(input).resize(size, size).ensureAlpha().png().toBuffer();
  return sharp(bytes).composite([{ input: svg(shape, size), blend: 'dest-in' }]).png(pngOptions).toBuffer();
}

async function metadata(file) {
  const bytes = fs.readFileSync(file);
  const meta = await sharp(bytes).metadata();
  const { data, info } = await sharp(bytes).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  let alphaMin = 255, alphaMax = 0;
  for (let i = 3; i < data.length; i += info.channels) {
    alphaMin = Math.min(alphaMin, data[i]);
    alphaMax = Math.max(alphaMax, data[i]);
  }
  return { file: path.relative(root, file).replaceAll('\\', '/'), width: meta.width, height: meta.height,
    bytes: bytes.length, space: meta.space, channels: meta.channels, bitDepth: bytes[24], pngColorType: bytes[25],
    icc: Boolean(meta.icc), alphaMin, alphaMax, sha256: createHash('sha256').update(bytes).digest('hex') };
}

async function main() {
  for (const name of ['ic_launcher.xml', 'ic_launcher_round.xml']) {
    const xml = fs.readFileSync(path.join(res, 'mipmap-anydpi', name), 'utf8');
    for (const id of ['ic_launcher_foreground', 'ic_launcher_monochrome', 'ic_launcher_background']) assert(xml.includes('@drawable/' + id));
    assert(!xml.includes('@drawable/ic_launcher_print_foreground'));
  }
  assert(fs.readFileSync(path.join(res, 'drawable/ic_launcher_background.xml'), 'utf8').includes('#1B1B19'));
  const monoXml = fs.readFileSync(path.join(res, 'drawable/ic_launcher_monochrome.xml'), 'utf8');
  const svgPaths = [...monoInput.toString().matchAll(/ d="([^"]+)"/g)].map(m => m[1]);
  const xmlPaths = [...monoXml.matchAll(/android:pathData="([^"]+)"/g)].map(m => m[1]);
  assert.deepEqual(svgPaths, xmlPaths);
  assert.equal((monoXml.match(/android:fillType="evenOdd"/g) || []).length, 2);

  // The approved master is opaque, including the charcoal photographic backing.
  // Do not claim it is a transparent cutout or synthesize alpha from facial tones.
  // 1296 pixels = 108dp at 12x; uniform padding keeps the actual print inside 66dp.
  const paddedSize = 1296, placedSize = 1104, offset = (paddedSize - placedSize) / 2;
  const scaled = await sharp(iconInput).resize(placedSize, placedSize).png().toBuffer();
  const layer = await sharp({ create: { width: paddedSize, height: paddedSize, channels: 4, background: bg } })
    .composite([{ input: scaled, left: offset, top: offset }]).png(pngOptions).toBuffer();

  // Safe-area diagnostic detects visible artwork against the almost-flat charcoal backing.
  // This is a color-contrast diagnostic, NOT an alpha silhouette or real-device test.
  const { data, info } = await sharp(layer).removeAlpha().raw().toBuffer({ resolveWithObject: true });
  let maxContrastRadiusDp = 0, contrastPixels = 0;
  for (let y = 0; y < info.height; y++) for (let x = 0; x < info.width; x++) {
    const i = (y * info.width + x) * info.channels;
    if (Math.max(Math.abs(data[i] - 27), Math.abs(data[i + 1] - 27), Math.abs(data[i + 2] - 25)) > 20) {
      contrastPixels++;
      maxContrastRadiusDp = Math.max(maxContrastRadiusDp, Math.hypot(x + 0.5 - 648, y + 0.5 - 648) / 12);
    }
  }
  assert(contrastPixels > 100000, 'Artwork detection unexpectedly empty');
  assert(maxContrastRadiusDp < 33, 'Visible mark outside the 66dp safe circle: ' + maxContrastRadiusDp);

  await save(layer, path.join(base, 'adaptive-foreground-master-1296.png'), 1296, 1296);
  await save(monoInput, path.join(base, 'adaptive-monochrome-432.png'), 432, 432);
  // The centered 72dp viewport matches normal launcher mask framing.
  const visible = await sharp(layer).extract({ left: 216, top: 216, width: 864, height: 864 }).png().toBuffer();
  const master = await save(visible, path.join(listing, 'app-icon-master-1024.png'), 1024, 1024);
  const icon = await save(visible, path.join(listing, 'app-icon-512.png'), 512, 512);
  await save(master, path.join(root, 'app/src/main/assets/branding/pocket_4cut_app_icon.png'), 1024, 1024);
  await save(featureInput, path.join(listing, 'feature-graphic-1024x500.png'), 1024, 500, false);
  const featureMeta = await sharp(featureInput).metadata();
  await save(featureInput, path.join(listing, 'feature-graphic-master.png'), featureMeta.width, featureMeta.height, false);

  for (const [density, [legacy, foreground]] of Object.entries(densities)) {
    await save(visible, path.join(res, 'mipmap-' + density + '/ic_launcher.png'), legacy, legacy);
    const round = await mask(visible, legacy, '<circle cx="' + legacy / 2 + '" cy="' + legacy / 2 + '" r="' + legacy / 2 + '" fill="white"/>');
    await save(round, path.join(res, 'mipmap-' + density + '/ic_launcher_round.png'), legacy, legacy);
    await save(layer, path.join(res, 'drawable-' + density + '/ic_launcher_foreground.png'), foreground, foreground);
  }

  // Review sheet is an offline export preview, never labeled as a device capture.
  const previewLayers = [];
  const shapes = [
    '<rect width="192" height="192" fill="white"/>',
    '<circle cx="96" cy="96" r="96" fill="white"/>',
    '<rect width="192" height="192" rx="45" fill="white"/>',
    '<path d="M96 0C169 0 192 23 192 96S169 192 96 192 0 169 0 96 23 0 96 0" fill="white"/>'
  ];
  for (let i = 0; i < 4; i++) previewLayers.push({ input: await mask(icon, 192, shapes[i]), left: 44 + 252 * i, top: 92 });
  const monoVisible = await sharp(monoInput).resize(1296).extract({ left: 216, top: 216, width: 864, height: 864 }).png().toBuffer();
  previewLayers.push({ input: await sharp(monoVisible).resize(120).png().toBuffer(), left: 694, top: 353 });
  for (const [i, size] of [48, 72, 96, 144].entries()) {
    previewLayers.push({ input: await sharp(icon).resize(size).png().toBuffer(), left: 44 + i * 152, top: 358 });
  }
  const textLayer = svg('<style>text{font-family:Arial,sans-serif;fill:#1b1b19}</style><text x="44" y="45" font-size="23" font-weight="700">Pocket4Cut / Film Strip</text><text x="44" y="322" font-size="16">Square</text><text x="296" y="322" font-size="16">Circle</text><text x="548" y="322" font-size="16">Rounded square</text><text x="800" y="322" font-size="16">Squircle</text><text x="44" y="534" font-size="16">48 / 72 / 96 / 144 px</text><text x="688" y="510" font-size="16">Themed silhouette</text><text x="44" y="582" font-size="14">Offline mask and size previews; not a device screenshot.</text>', 1056, 616);
  const preview = await sharp({ create: { width: 1056, height: 616, channels: 4, background: '#F3F0E8' } })
    .composite([...previewLayers, { input: textLayer, left: 0, top: 0 }]).png().toBuffer();
  await save(preview, path.join(base, 'icon-preview-grid.png'), 1056, 616, false);

  const validation = await Promise.all(outputs.map(metadata));
  for (const row of validation) {
    assert.equal(row.bitDepth, 8);
    assert.equal(row.space, 'srgb');
    assert(row.icc);
  }
  const store = validation.find(r => r.file.endsWith('/app-icon-512.png'));
  assert.equal(store.width, 512); assert.equal(store.height, 512);
  assert.equal(store.pngColorType, 6); assert.equal(store.alphaMin, 255);
  assert(store.bytes <= 1024 * 1024);
  const feature = validation.find(r => r.file.endsWith('/feature-graphic-1024x500.png'));
  assert.equal(feature.width, 1024); assert.equal(feature.height, 500);
  assert.equal(feature.channels, 3); assert.equal(feature.pngColorType, 2);
  const report = { generatedAt: new Date().toISOString(), sharpVersion: sharp.versions.sharp,
    masterMode: 'Opaque photographic foreground on charcoal; no fabricated transparency',
    safeArea: { diameterDp: 66, maxContrastRadiusDp, contrastThreshold: 20, contrastPixels, method: 'RGB distance from charcoal, manual visual review also required' },
    wordmark: 'pocket4cut', outputs: validation, deviceTested: false };
  fs.writeFileSync(path.join(base, 'asset-validation.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify({ outputCount: validation.length, maxContrastRadiusDp, store, feature }, null, 2));
}

main().catch(error => { console.error(error); process.exitCode = 1; });
