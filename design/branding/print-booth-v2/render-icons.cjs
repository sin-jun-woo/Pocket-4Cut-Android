/** PNG derivatives of the reviewed v2 SVG. Requires sharp; refuses mismatched app vectors. */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const sharp = require('sharp');
const { createHash } = require('node:crypto');
const root = path.resolve(__dirname, '../../..');
const res = path.join(root, 'app/src/main/res');
const svg = fs.readFileSync(path.join(__dirname, 'app-icon-source.svg'), 'utf8');
const monoSvg = fs.readFileSync(path.join(__dirname, 'adaptive-monochrome-source.svg'), 'utf8');
const artwork = value => value.split('<!-- ARTWORK START -->')[1].split('<!-- ARTWORK END -->')[0];
const mark = artwork(svg);
const monoMark = artwork(monoSvg);
const frame = (body, viewBox = '0 0 108 108', size = 108) => `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="${viewBox}">${body}</svg>`;
const foreground = frame(mark);
const monochrome = frame(monoMark);
const normalize = value => value.replaceAll('\r\n', '\n').trim();
assert.equal(normalize(fs.readFileSync(path.join(root, 'app/src/main/assets/branding/pocket_4cut_app_icon.svg'), 'utf8')), normalize(svg));
for (const [body, name, mono] of [[mark, 'ic_launcher_print_foreground.xml', false], [monoMark, 'ic_launcher_monochrome.xml', true]]) {
  const xml = fs.readFileSync(path.join(res, 'drawable', name), 'utf8');
  const svgPaths = [...body.matchAll(/<path fill="([^"]+)" d="([^"]+)"/g)].map(m => [m[1], m[2]]);
  const androidPaths = [...xml.matchAll(/<path android:fillColor="([^"]+)"[^>]*android:pathData="([^"]+)"/g)].map(m => [m[1], m[2]]);
  assert.deepEqual(androidPaths, svgPaths);
  for (const attribute of ['android:scaleX="0.88"', 'android:scaleY="0.88"', 'android:pivotX="54"', 'android:pivotY="54"', 'android:rotation="9"', 'android:pivotX="68.5"', 'android:pivotY="56.5"']) assert(xml.includes(attribute));
  if (mono) assert.equal([...xml.matchAll(/android:fillType="evenOdd"/g)].length, 2);
}
for (const name of ['ic_launcher.xml', 'ic_launcher_round.xml']) {
  const xml = fs.readFileSync(path.join(res, 'mipmap-anydpi', name), 'utf8');
  for (const id of ['ic_launcher_print_foreground', 'ic_launcher_monochrome', 'ic_launcher_background']) assert(xml.includes(`@drawable/${id}`));
}
assert(fs.readFileSync(path.join(res, 'drawable/ic_launcher_background.xml'), 'utf8').includes('<solid android:color="#C83D2D" />'));
const densities = { mdpi: [48, 108], hdpi: [72, 162], xhdpi: [96, 216], xxhdpi: [144, 324], xxxhdpi: [192, 432] };
const outputs = [];
async function render(input, size, file, mask) {
  let result = await sharp(Buffer.from(input), { density: 384 }).resize(size, size).ensureAlpha().toColourspace('srgb').withIccProfile('srgb').png({ compressionLevel: 9 }).toBuffer();
  if (mask) result = await sharp(result).composite([{ input: Buffer.from(frame(mask, '0 0 100 100', size)), blend: 'dest-in' }]).ensureAlpha().withIccProfile('srgb').png({ compressionLevel: 9 }).toBuffer();
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, result);
  outputs.push(file);
  return result;
}
async function metadata(file) {
  const bytes = fs.readFileSync(file);
  const meta = await sharp(bytes).metadata();
  const { data, info } = await sharp(bytes).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  let alphaMin = 255, alphaMax = 0;
  for (let i = 3; i < data.length; i += info.channels) { alphaMin = Math.min(alphaMin, data[i]); alphaMax = Math.max(alphaMax, data[i]); }
  return { file: path.relative(root, file).replaceAll('\\', '/'), width: meta.width, height: meta.height, bytes: bytes.length, space: meta.space, channels: meta.channels, pngColorType: bytes[25], bitDepth: bytes[24], icc: Boolean(meta.icc), alphaMin, alphaMax, sha256: createHash('sha256').update(bytes).digest('hex') };
}
async function main() {
  await render(svg, 512, path.join(__dirname, 'play-store-icon-512.png'));
  await render(svg, 1024, path.join(__dirname, 'app-icon-master-1024.png'));
  await render(foreground, 432, path.join(__dirname, 'adaptive-foreground-432.png'));
  await render(monochrome, 432, path.join(__dirname, 'adaptive-monochrome-432.png'));
  for (const [density, [legacy, layer]] of Object.entries(densities)) {
    await render(svg, legacy, path.join(res, `mipmap-${density}/ic_launcher.png`));
    await render(svg, legacy, path.join(res, `mipmap-${density}/ic_launcher_round.png`), '<circle cx="50" cy="50" r="50" fill="white"/>');
    await render(foreground, layer, path.join(res, `drawable-${density}/ic_launcher_foreground.png`));
  }
  const shapes = ['<rect width="100" height="100" fill="white"/>', '<circle cx="50" cy="50" r="50" fill="white"/>', '<rect width="100" height="100" rx="24" fill="white"/>', '<path d="M50 0C88 0 100 12 100 50S88 100 50 100 0 88 0 50 12 0 50 0" fill="white"/>'];
  const composites = [];
  const icon = await sharp(Buffer.from(svg)).resize(192).png().toBuffer();
  for (let i = 0; i < shapes.length; i++) {
    const masked = await sharp(icon).composite([{ input: Buffer.from(frame(shapes[i], '0 0 100 100', 192)), blend: 'dest-in' }]).png().toBuffer();
    composites.push({ input: masked, left: 52 + i * 250, top: 118 });
    const small = await sharp(masked).resize(48).png().toBuffer();
    composites.push({ input: small, left: 124 + i * 250, top: 375 });
    composites.push({ input: await sharp(small).resize(96, 96, { kernel: 'nearest' }).png().toBuffer(), left: 100 + i * 250, top: 464 });
  }
  for (const [i, [background, tint]] of [['#DDD7CB', '#383329'], ['#383329', '#DDD7CB']].entries()) {
    const themed = frame(`<path fill="${background}" d="M0,0H108V108H0Z"/>${monoMark.replaceAll('#000000', tint)}`, '18 18 72 72', 192);
    composites.push({ input: await sharp(Buffer.from(themed)).png().toBuffer(), left: 52 + i * 250, top: 670 });
  }
  const safe = frame(`<rect width="108" height="108" fill="#F3F0E8"/><rect x="18" y="18" width="72" height="72" fill="none" stroke="#928C83" stroke-width=".5"/><circle cx="54" cy="54" r="33" fill="none" stroke="#C83D2D" stroke-width=".5"/>${mark}`, '0 0 108 108', 192);
  composites.push({ input: await sharp(Buffer.from(safe)).png().toBuffer(), left: 552, top: 670 });
  const labels = ['FULL SQUARE', 'CIRCLE', 'ROUNDED', 'SQUIRCLE'];
  const board = `<svg width="1080" height="940" xmlns="http://www.w3.org/2000/svg"><rect width="1080" height="940" fill="#F3F0E8"/><g font-family="Arial,sans-serif" fill="#1B1B19"><text x="52" y="53" font-size="26" font-weight="700">POCKET 4CUT / CAMERA + PRINT</text><text x="52" y="81" font-size="14">Lens, shutter, flash and four portrait frames. V2 vector artwork.</text>${labels.map((label,i)=>`<text x="${52+i*250}" y="341" font-size="14" font-weight="700">${label}</text>`).join('')}<text x="52" y="405" font-size="12">48px</text><text x="52" y="585" font-size="12">48px enlarged 2x with nearest-neighbour pixels</text><text x="52" y="641" font-size="18" font-weight="700">THEMED ICONS / ADAPTIVE SAFE ZONE</text><text x="52" y="890" font-size="13">LIGHT THEME</text><text x="302" y="890" font-size="13">DARK THEME</text><text x="552" y="890" font-size="13">108dp layer / 66dp safe circle</text><text x="802" y="715" font-size="14">Simplified monochrome:</text><text x="802" y="742" font-size="14">open lens + four windows.</text><text x="802" y="795" font-size="14">Flat, square background.</text><text x="802" y="822" font-size="14">Internal paper depth only.</text></g></svg>`;
  await sharp(Buffer.from(board)).composite(composites).ensureAlpha().withIccProfile('srgb').png().toFile(path.join(__dirname, 'icon-preview-grid.png'));
  outputs.push(path.join(__dirname, 'icon-preview-grid.png'));
  const assets = await Promise.all(outputs.map(metadata));
  for (const item of assets) { assert.equal(item.channels, 4); assert.equal(item.bitDepth, 8); assert.equal(item.pngColorType, 6); assert.equal(item.space, 'srgb'); assert(item.icc); }
  assert.equal(assets[0].width, 512); assert.equal(assets[0].height, 512); assert.equal(assets[0].alphaMin, 255); assert(assets[0].bytes < 1024 * 1024);
  for (const [density, [legacy, layer]] of Object.entries(densities)) {
    for (const [file, size, minAlpha] of [[`mipmap-${density}/ic_launcher.png`, legacy, 255], [`mipmap-${density}/ic_launcher_round.png`, legacy, 0], [`drawable-${density}/ic_launcher_foreground.png`, layer, 0]]) {
      const item = assets.find(a => a.file.endsWith(file));
      assert.equal(item.width, size); assert.equal(item.height, size); assert.equal(item.alphaMin, minAlpha); assert.equal(item.alphaMax, 255);
    }
  }
  let maximumRadiusDp = 0;
  for (const layer of [foreground, monochrome]) {
    const { data, info } = await sharp(Buffer.from(layer)).resize(864).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
    for (let i = 3; i < data.length; i += info.channels) if (data[i] > 0) {
      const pixel = (i - 3) / info.channels;
      maximumRadiusDp = Math.max(maximumRadiusDp, Math.hypot((pixel % info.width + .5) / 8 - 54, (Math.floor(pixel / info.width) + .5) / 8 - 54));
    }
  }
  assert(maximumRadiusDp < 33, `Artwork exceeds safe circle: ${maximumRadiusDp}`);
  const { data, info } = await sharp(Buffer.from(monochrome)).resize(864).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  const holeAlpha = (x,y) => data[(Math.floor((54+(y-54)*.88)*8)*info.width+Math.floor((54+(x-54)*.88)*8))*info.channels+3];
  assert.equal(holeAlpha(44,51), 0);
  for (const y of [43.5,52,60.5,69]) {
    const a = 9 * Math.PI / 180;
    assert.equal(holeAlpha(68.5-(y-56.5)*Math.sin(a), 56.5+(y-56.5)*Math.cos(a)), 0);
  }
  const report = { date:'2026-09-10', version:'print-booth-v2', source:'design/branding/print-booth-v2/app-icon-source.svg', renderer:`sharp ${sharp.versions.sharp} / librsvg ${sharp.versions.rsvg}`, vectorGeometryMatches:true, maximumRadiusDp:Number(maximumRadiusDp.toFixed(4)), safeCircleRadiusDp:33, monochromeTransparentWindows:4, monochromeLensTransparent:true, limitations:['Rendered mask previews, not a physical-device launcher test.', 'Android resource build is checked separately.'], assets };
  fs.writeFileSync(path.join(__dirname, 'icon-validation.json'), JSON.stringify(report,null,2)+'\n');
  console.log(JSON.stringify({outputs:assets.length,storeIcon:assets[0],maximumRadiusDp:report.maximumRadiusDp,monochromeHoles:5},null,2));
}
main().catch(error=>{console.error(error);process.exitCode=1;});
