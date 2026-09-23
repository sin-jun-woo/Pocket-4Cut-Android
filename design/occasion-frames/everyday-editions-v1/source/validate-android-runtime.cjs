/* Validate every generated Everyday Editions Android runtime asset and metadata contract. */
'use strict';

const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const sharp = require('sharp');
const {
  buildRuntimeAtlas,
  encodeRuntimeImages,
  motifCells,
  placements,
  profileFor,
  STATIC_MOTIF_SUBSTITUTIONS,
  RUNTIME_ONLY_MOTIF_SUBSTITUTIONS,
  verifyRuntimeBytes,
} = require('./export-android-runtime.cjs');

sharp.cache(false);
sharp.concurrency(2);

const SOURCE = __dirname;
const COLLECTION = path.resolve(SOURCE, '..');
const ROOT = path.resolve(COLLECTION, '../../..');
const OUTPUT = path.join(ROOT, 'app/src/main/assets/occasion/v1');
const SOURCE_CATALOG = JSON.parse(fs.readFileSync(path.join(SOURCE, 'catalog.json'), 'utf8'));
const CATALOG = JSON.parse(fs.readFileSync(path.join(OUTPUT, 'catalog.json'), 'utf8'));
const MANIFEST = JSON.parse(fs.readFileSync(path.join(OUTPUT, 'manifest.json'), 'utf8'));
const ART_WIDTH = 1536;
const ART_HEIGHT = 1024;
const CELL_SIZE = 512;
const THUMB_WIDTH = 240;
const THUMB_HEIGHT = 160;
const DESIGN_VERSION = 'everyday-editions-v1';
const sha256 = buffer => crypto.createHash('sha256').update(buffer).digest('hex');
const assetPath = (...parts) => ['occasion', 'v1', ...parts].join('/');

function fail(message) { throw new Error(message); }
function requireEqual(actual, expected, label) {
  if (actual !== expected) fail(`${label}: expected ${expected}, got ${actual}`);
}
function unique(values, label) {
  const set = new Set(values);
  requireEqual(set.size, values.length, `${label} uniqueness`);
  return set;
}
function deepClone(value) { return JSON.parse(JSON.stringify(value)); }

function expectedRuntimeCatalog(themes) {
  return {
    schemaVersion: 1,
    designVersion: DESIGN_VERSION,
    categories: SOURCE_CATALOG.categories.map((category, order) => ({
      id: category.id,
      displayName: category.label,
      order,
    })),
    renderContract: {
      coordinateSpace: 'normalized',
      atlasGrid: {columns: 3, rows: 2, cellWidth: CELL_SIZE, cellHeight: CELL_SIZE},
      variation: {formula: '(themeIndex % 3) * 0.035'},
      sizeRules: {
        hero: 'min(headerHeight * profile.hero, canvasWidth * 0.18)',
        side: 'min(canvasWidth / 390 * 25, (sideClearance - canvasWidth / 390 * 4) / 1.2)',
        gutter: 'min(canvasWidth / 390 * 17, gapHeight - canvasWidth / 390 * 2)',
        footer: 'min(footerHeight * 0.58, canvasWidth * 0.10)',
      },
    },
    themes,
  };
}

function expectedRuntimeManifest(catalogBuffer, catalog, files) {
  return {
    schemaVersion: 1,
    designVersion: DESIGN_VERSION,
    themeCount: catalog.themes.length,
    categoryCount: catalog.categories.length,
    occasionCount: catalog.themes.filter(theme => theme.group === 'occasion').length,
    specialCount: catalog.themes.filter(theme => theme.group === 'special').length,
    artCount: files.filter(file => file.kind === 'art').length,
    thumbnailCount: files.filter(file => file.kind === 'thumbnail').length,
    totalAssetBytes: files.reduce((sum, file) => sum + file.bytes, 0),
    catalogSha256: sha256(catalogBuffer),
    files,
  };
}

async function verifyFile(entry) {
  const relative = entry.path.replaceAll('/', path.sep).replace(`occasion${path.sep}v1${path.sep}`, '');
  const file = path.join(OUTPUT, relative);
  const buffer = await fsp.readFile(file);
  requireEqual(buffer.length, entry.bytes, `${entry.path} byte count`);
  requireEqual(sha256(buffer), entry.sha256, `${entry.path} SHA-256`);
  const metadata = await sharp(buffer).metadata();
  requireEqual(metadata.format, 'webp', `${entry.path} format`);
  requireEqual(metadata.width, entry.width, `${entry.path} width`);
  requireEqual(metadata.height, entry.height, `${entry.path} height`);
  requireEqual(Boolean(metadata.hasAlpha), true, `${entry.path} alpha channel`);
  const stats = await sharp(buffer).ensureAlpha().stats();
  if (stats.channels[3].min !== 0 || stats.channels[3].max < 250) {
    fail(`${entry.path}: expected transparent and effectively opaque pixels`);
  }
  return buffer;
}

async function verifyArtQuality(theme, expectedAtlas, runtimeBuffer) {
  const [{data: source, info}, {data: runtime}] = await Promise.all([
    sharp(expectedAtlas).ensureAlpha().raw().toBuffer({resolveWithObject: true}),
    sharp(runtimeBuffer).ensureAlpha().raw().toBuffer({resolveWithObject: true}),
  ]);
  requireEqual(runtime.length, source.length, `${theme.id} decoded byte count`);
  let squareError = 0;
  let comparedChannels = 0;
  for (let offset = 0; offset < source.length; offset += 4) {
    const sourceAlpha = source[offset + 3];
    const runtimeAlpha = runtime[offset + 3];
    requireEqual(runtimeAlpha, sourceAlpha, `${theme.id} alpha at pixel ${offset / 4}`);
    const sourceAlphaFraction = sourceAlpha / 255;
    const runtimeAlphaFraction = runtimeAlpha / 255;
    for (let channel = 0; channel < 3; channel++) {
      const sourceBlack = source[offset + channel] * sourceAlphaFraction;
      const runtimeBlack = runtime[offset + channel] * runtimeAlphaFraction;
      const blackDelta = sourceBlack - runtimeBlack;
      squareError += blackDelta * blackDelta;
      const sourceWhite = sourceBlack + 255 * (1 - sourceAlphaFraction);
      const runtimeWhite = runtimeBlack + 255 * (1 - runtimeAlphaFraction);
      const whiteDelta = sourceWhite - runtimeWhite;
      squareError += whiteDelta * whiteDelta;
      comparedChannels += 2;
    }
  }
  const mse = squareError / comparedChannels;
  const psnr = mse === 0 ? Number.POSITIVE_INFINITY : 10 * Math.log10((255 * 255) / mse);
  if (psnr < 38) fail(`${theme.id}: composite PSNR ${psnr.toFixed(2)} dB is below 38 dB`);
  return {psnr, pixels: info.width * info.height};
}

function verifyStaticRendererSubstitutionContract() {
  const renderer = fs.readFileSync(path.join(SOURCE, 'render-collection.cjs'), 'utf8');
  const regex = /if\(theme\.id==='([^']+)'\)\s*images\[(\d+)\]=await loadImage\(path\.join\(__dirname,'([^']+)'\)\)/g;
  const rendererRules = [...renderer.matchAll(regex)].map(match => ({
    themeId: match[1], motifIndex: Number(match[2]), assetPath: match[3],
  }));
  assert.deepStrictEqual(rendererRules, STATIC_MOTIF_SUBSTITUTIONS,
    'static renderer/runtime motif substitution contract');
}

function verifyRuntimeTextFreeSubstitutionContract() {
  requireEqual(RUNTIME_ONLY_MOTIF_SUBSTITUTIONS.length, 1, 'runtime-only substitution count');
  const rule = RUNTIME_ONLY_MOTIF_SUBSTITUTIONS[0];
  assert.deepStrictEqual(rule, {
    themeId: 'new-year-sunrise',
    motifIndex: 5,
    sourceThemeId: 'enlistment',
    sourceMotifIndex: 5,
    approvedRgbaSha256: 'dc96d8c99a9b213ecd75ba30f3238b6bca1842f0b713f1839315dc2b4ecbc0cb',
    contract: 'text-free brass compass',
  });
}

function verifyManualContentContract(catalog) {
  const byId = new Map(catalog.themes.map(theme => [theme.id, theme]));
  const expected = {
    graduation: {title: 'GRADUATION DAY'},
    'year-end': {title: 'GOODBYE, OLD YEAR'},
    'monthly-memory': {title: 'THIS MONTH'},
    'annual-memory': {title: 'THIS YEAR'},
    'this-month': {title: 'THIS MONTH'},
    'this-year': {title: 'THIS YEAR'},
    'nth-fourcut': {title: 'NTH FOUR CUT'},
    dday: {displayName: 'D-Day 기록 프레임'},
  };
  for (const [id, values] of Object.entries(expected)) {
    const theme = byId.get(id) || fail(`${id}: missing content-contract theme`);
    for (const [key, value] of Object.entries(values)) requireEqual(theme[key], value, `${id} ${key}`);
  }
  for (const id of ['graduation', 'year-end', 'monthly-memory', 'annual-memory', 'this-month', 'this-year', 'nth-fourcut']) {
    const title = byId.get(id).title;
    if (/2026|SEPTEMBER|10TH/i.test(title)) fail(`${id}: example date/ordinal leaked into runtime title: ${title}`);
  }
  if (/자동/.test(byId.get('dday').displayName)) fail('dday: automatic behavior is not implemented');
}

function verifyPlacementSemantics(theme) {
  const motifIndices = new Set(theme.atlas.motifs.map(motif => motif.index));
  const verifyItem = (item, sizeRule, label) => {
    if (!motifIndices.has(item.motifIndex)) fail(`${theme.id} ${label}: unknown motif ${item.motifIndex}`);
    if (!Number.isFinite(item.x) || item.x < 0 || item.x > 1 ||
        !Number.isFinite(item.y) || item.y < 0 || item.y > 1) {
      fail(`${theme.id} ${label}: normalized placement is out of bounds`);
    }
    requireEqual(item.sizeRule, sizeRule, `${theme.id} ${label} sizeRule`);
    if (!Number.isFinite(item.rotationDegrees)) fail(`${theme.id} ${label}: invalid rotation`);
  };
  theme.placements.header.items.forEach((item, index) => verifyItem(item, 'hero', `header ${index}`));
  theme.placements.footer.items.forEach((item, index) => verifyItem(item, 'footer', `footer ${index}`));
  theme.placements.sides.forEach((item, index) => {
    if (!motifIndices.has(item.motifIndex)) fail(`${theme.id} side ${index}: unknown motif`);
    if (!['left', 'right'].includes(item.edge) || item.yBase < 0 || item.yBase > 1 ||
        !Number.isFinite(item.yVariationMultiplier) || !Number.isFinite(item.rotationDegrees)) {
      fail(`${theme.id} side ${index}: invalid side placement`);
    }
    requireEqual(item.sizeRule, 'side', `${theme.id} side ${index} sizeRule`);
  });
  const gutter = theme.placements.gutter;
  if (!motifIndices.has(gutter.motifStartIndex) || gutter.motifModulo !== motifIndices.size ||
      !Array.isArray(gutter.xCycle) || gutter.xCycle.length === 0 ||
      gutter.xCycle.some(value => !Number.isFinite(value) || value < 0 || value > 1)) {
    fail(`${theme.id}: invalid gutter placement`);
  }
  requireEqual(gutter.sizeRule, 'gutter', `${theme.id} gutter sizeRule`);
}

function verifySemanticGraph(catalog, manifest, expectedCatalog, expectedManifest) {
  assert.deepStrictEqual(catalog, expectedCatalog, 'runtime catalog differs from reproducible source contract');
  assert.deepStrictEqual(manifest, expectedManifest, 'runtime manifest differs from reproducible files');
  requireEqual(catalog.schemaVersion, 1, 'catalog schemaVersion');
  requireEqual(catalog.designVersion, DESIGN_VERSION, 'catalog designVersion');
  requireEqual(manifest.schemaVersion, 1, 'manifest schemaVersion');
  requireEqual(manifest.designVersion, DESIGN_VERSION, 'manifest designVersion');
  unique(catalog.categories.map(category => category.id), 'category ID');
  unique(catalog.themes.map(theme => theme.id), 'theme ID');
  unique(catalog.themes.map(theme => theme.index), 'theme index');
  unique(manifest.files.map(file => file.path), 'manifest path');
  requireEqual(manifest.totalAssetBytes, manifest.files.reduce((sum, file) => sum + file.bytes, 0),
    'manifest byte total');

  const themes = new Map(catalog.themes.map(theme => [theme.id, theme]));
  const files = new Map(manifest.files.map(file => [file.path, file]));
  const referenced = new Set();
  for (const theme of catalog.themes) {
    verifyPlacementSemantics(theme);
    requireEqual(theme.isManualRecord, theme.group === 'special', `${theme.id} manual-record flag`);
    requireEqual(theme.pattern, theme.profile.pattern, `${theme.id} resolved pattern`);
    for (const [kind, expectedPath, width, height] of [
      ['art', theme.atlas.assetPath, ART_WIDTH, ART_HEIGHT],
      ['thumbnail', theme.thumbnailAssetPath, THUMB_WIDTH, THUMB_HEIGHT],
    ]) {
      const entry = files.get(expectedPath) || fail(`${theme.id}: missing ${kind} manifest entry`);
      referenced.add(expectedPath);
      requireEqual(entry.kind, kind, `${expectedPath} kind`);
      requireEqual(entry.themeId, theme.id, `${expectedPath} themeId`);
      requireEqual(entry.width, width, `${expectedPath} width`);
      requireEqual(entry.height, height, `${expectedPath} height`);
      requireEqual(entry.hasAlpha, true, `${expectedPath} alpha`);
      if (!Number.isInteger(entry.bytes) || entry.bytes <= 0) fail(`${expectedPath}: invalid byte count`);
      if (!/^[0-9a-f]{64}$/.test(entry.sha256)) fail(`${expectedPath}: invalid SHA-256`);
    }
    requireEqual(theme.atlas.sha256, files.get(theme.atlas.assetPath).sha256, `${theme.id} atlas reverse SHA`);
    requireEqual(theme.atlas.motifs.length, 6, `${theme.id} motif count`);
  }
  requireEqual(referenced.size, manifest.files.length, 'catalog/manifest reverse reference count');
  for (const entry of manifest.files) {
    if (!themes.has(entry.themeId)) fail(`${entry.path}: unknown manifest themeId ${entry.themeId}`);
    if (!referenced.has(entry.path)) fail(`${entry.path}: unreferenced manifest file`);
  }
}

function verifyNegativeTamperCases(expectedCatalog, expectedManifest) {
  const cases = [
    ['catalog theme index', (catalog) => { catalog.themes[0].index = 999; }],
    ['catalog theme group', (catalog) => { catalog.themes[0].group = 'special'; }],
    ['catalog manual flag', (catalog) => { catalog.themes.at(-1).isManualRecord = false; }],
    ['catalog paper color', (catalog) => { catalog.themes[0].paperColor = '#000000'; }],
    ['catalog profile', (catalog) => { catalog.themes[0].profile.hero = 0.01; }],
    ['catalog placement bounds', (catalog) => { catalog.themes[0].placements.header.items[0].x = 999; }],
    ['catalog atlas sha', (catalog) => { catalog.themes[0].atlas.sha256 = '0'.repeat(64); }],
    ['manifest schema', (_catalog, manifest) => { manifest.schemaVersion = 999; }],
    ['manifest design version', (_catalog, manifest) => { manifest.designVersion = 'bogus'; }],
    ['manifest byte total', (_catalog, manifest) => { manifest.totalAssetBytes = 1; }],
    ['manifest kind', (_catalog, manifest) => { manifest.files[0].kind = 'bogus'; }],
    ['manifest themeId', (_catalog, manifest) => { manifest.files[0].themeId = 'bogus'; }],
    ['manifest file bytes', (_catalog, manifest) => { manifest.files[0].bytes += 1; }],
    ['manifest file sha', (_catalog, manifest) => { manifest.files[0].sha256 = '0'.repeat(64); }],
  ];
  for (const [label, mutate] of cases) {
    const catalog = deepClone(expectedCatalog);
    const manifest = deepClone(expectedManifest);
    mutate(catalog, manifest);
    let rejected = false;
    try {
      verifySemanticGraph(catalog, manifest, expectedCatalog, expectedManifest);
    } catch (_expected) {
      rejected = true;
    }
    if (!rejected) fail(`negative tamper case was accepted: ${label}`);
  }
  return cases.length;
}

async function buildExpectedTheme(source) {
  const record = JSON.parse(await fsp.readFile(
    path.join(COLLECTION, 'records', `${source.id}-art.json`),
    'utf8',
  ));
  const runtimeAtlas = await buildRuntimeAtlas(source, record);
  const expectedEncoded = await encodeRuntimeImages(runtimeAtlas.buffer);
  const artPath = path.join(OUTPUT, 'art', `${source.id}.webp`);
  const thumbPath = path.join(OUTPUT, 'thumbs', `${source.id}.webp`);
  const [art, thumb] = await Promise.all([fsp.readFile(artPath), fsp.readFile(thumbPath)]);
  await Promise.all([
    verifyRuntimeBytes(art, expectedEncoded.art, ART_WIDTH, ART_HEIGHT, `${source.id} art`),
    verifyRuntimeBytes(thumb, expectedEncoded.thumb, THUMB_WIDTH, THUMB_HEIGHT, `${source.id} thumbnail`),
  ]);
  const profile = profileFor(source);
  return {
    theme: {
      index: source.index,
      id: source.id,
      displayName: source.name,
      categoryId: source.category,
      group: source.group,
      isManualRecord: source.group === 'special',
      title: source.title,
      paperColor: source.paper,
      inkColor: source.ink,
      pattern: profile.pattern,
      profile,
      thumbnailAssetPath: assetPath('thumbs', `${source.id}.webp`),
      atlas: {
        assetPath: assetPath('art', `${source.id}.webp`),
        width: ART_WIDTH,
        height: ART_HEIGHT,
        sha256: sha256(art),
        motifs: motifCells(source, record, runtimeAtlas.motifContentSizes),
      },
      placements: placements(source, profile),
    },
    files: [
      {kind: 'art', themeId: source.id, path: assetPath('art', `${source.id}.webp`), width: ART_WIDTH,
        height: ART_HEIGHT, hasAlpha: true, bytes: art.length, sha256: sha256(art)},
      {kind: 'thumbnail', themeId: source.id, path: assetPath('thumbs', `${source.id}.webp`),
        width: THUMB_WIDTH, height: THUMB_HEIGHT, hasAlpha: true, bytes: thumb.length, sha256: sha256(thumb)},
    ],
    runtimeAtlas,
    art,
  };
}

async function main() {
  verifyStaticRendererSubstitutionContract();
  verifyRuntimeTextFreeSubstitutionContract();
  requireEqual(SOURCE_CATALOG.themes?.length, 88, 'source theme count');

  const exported = [];
  let minimumCompositePsnr = Number.POSITIVE_INFINITY;
  const batchSize = 4;
  for (let index = 0; index < SOURCE_CATALOG.themes.length; index += batchSize) {
    const batch = await Promise.all(SOURCE_CATALOG.themes.slice(index, index + batchSize).map(buildExpectedTheme));
    exported.push(...batch);
    for (const item of batch) {
      const quality = await verifyArtQuality(item.theme, item.runtimeAtlas.buffer, item.art);
      minimumCompositePsnr = Math.min(minimumCompositePsnr, quality.psnr);
    }
  }

  const expectedCatalog = expectedRuntimeCatalog(exported.map(item => item.theme));
  const expectedCatalogBuffer = Buffer.from(`${JSON.stringify(expectedCatalog, null, 2)}\n`);
  const expectedManifest = expectedRuntimeManifest(
    expectedCatalogBuffer,
    expectedCatalog,
    exported.flatMap(item => item.files),
  );
  verifySemanticGraph(CATALOG, MANIFEST, expectedCatalog, expectedManifest);
  verifyManualContentContract(CATALOG);
  const negativeTamperCases = verifyNegativeTamperCases(expectedCatalog, expectedManifest);

  for (const entry of MANIFEST.files) await verifyFile(entry);
  const catalogBuffer = await fsp.readFile(path.join(OUTPUT, 'catalog.json'));
  assert.deepStrictEqual(catalogBuffer, expectedCatalogBuffer, 'catalog JSON bytes differ from reproducible output');
  requireEqual(sha256(catalogBuffer), MANIFEST.catalogSha256, 'catalog SHA-256');
  const diskArt = (await fsp.readdir(path.join(OUTPUT, 'art'))).filter(name => name.endsWith('.webp'));
  const diskThumbs = (await fsp.readdir(path.join(OUTPUT, 'thumbs'))).filter(name => name.endsWith('.webp'));
  requireEqual(diskArt.length, 88, 'art file count on disk');
  requireEqual(diskThumbs.length, 88, 'thumbnail file count on disk');

  console.log('Everyday Editions Android runtime validation passed');
  console.log('themes=88 categories=10 groups=77+11 art=88 thumbnails=88');
  console.log(`verifiedBytes=${MANIFEST.totalAssetBytes}`);
  console.log(`minimumCompositePsnr=${minimumCompositePsnr.toFixed(2)}dB alphaDifference=0`);
  console.log(`negativeTamperCases=${negativeTamperCases}`);
  console.log('staticRendererSubstitutions=liberation-day:1:references/mois-taegeukgi.png');
  console.log('runtimeTextFreeSubstitutions=new-year-sunrise:5<-enlistment:5');
}

main().catch(error => {
  console.error(error.stack || error.message || error);
  process.exitCode = 1;
});
