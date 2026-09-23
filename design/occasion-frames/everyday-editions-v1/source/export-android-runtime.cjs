/*
 * Export the 88 Everyday Editions illustration atlases as Android runtime assets.
 *
 * The source PNG atlases are already normalized to a 3 x 2 grid by
 * render-collection.cjs. This exporter only changes the container/encoding and
 * emits runtime metadata; it never copies the large pre-rendered frame set.
 *
 * Run from the repository root:
 *   node design/occasion-frames/everyday-editions-v1/source/export-android-runtime.cjs
 */
'use strict';

const crypto = require('node:crypto');
const childProcess = require('node:child_process');
const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const sharp = require('sharp');

sharp.cache(false);
sharp.concurrency(2);

const SOURCE = __dirname;
const COLLECTION = path.resolve(SOURCE, '..');
const ROOT = path.resolve(COLLECTION, '../../..');
const OUTPUT = path.join(ROOT, 'app/src/main/assets/occasion/v1');
const ART_OUTPUT = path.join(OUTPUT, 'art');
const THUMB_OUTPUT = path.join(OUTPUT, 'thumbs');
const INCOMPLETE_MARKER = path.join(OUTPUT, '.export-incomplete');
const CATALOG = JSON.parse(fs.readFileSync(path.join(SOURCE, 'catalog.json'), 'utf8'));
const DIRECTION = JSON.parse(fs.readFileSync(path.join(SOURCE, 'art-direction.json'), 'utf8'));

const ART_WIDTH = 1536;
const ART_HEIGHT = 1024;
const CELL_SIZE = 512;
const THUMB_WIDTH = 240;
const THUMB_HEIGHT = 160;
const metadataOnly = process.argv.includes('--metadata-only');

// render-collection.cjs substitutes this official reference at draw time. The Android atlas
// must contain the same motif or its dynamically composed result would differ from the approved
// static collection. Keep this list explicit so the validator can detect renderer drift.
const STATIC_MOTIF_SUBSTITUTIONS = Object.freeze([
  Object.freeze({themeId: 'liberation-day', motifIndex: 1, assetPath: 'references/mois-taegeukgi.png'}),
]);

// The generated new-year-sunrise compass contains cardinal letters. Preserve its master for
// provenance, but ship the visually approved, text-free brass compass already present in the
// enlistment atlas. The raw RGBA hash makes the reviewed donor pixels an explicit golden input.
const RUNTIME_ONLY_MOTIF_SUBSTITUTIONS = Object.freeze([
  Object.freeze({
    themeId: 'new-year-sunrise',
    motifIndex: 5,
    sourceThemeId: 'enlistment',
    sourceMotifIndex: 5,
    approvedRgbaSha256: 'dc96d8c99a9b213ecd75ba30f3238b6bca1842f0b713f1839315dc2b4ecbc0cb',
    contract: 'text-free brass compass',
  }),
]);

const sha256 = buffer => crypto.createHash('sha256').update(buffer).digest('hex');
const assetPath = (...parts) => ['occasion', 'v1', ...parts].join('/');

async function atomicWrite(file, buffer) {
  const pending = `${file}.pending-${process.pid}`;
  try {
    await fsp.writeFile(pending, buffer);
    await fsp.rename(pending, file);
  } catch (error) {
    await fsp.rm(pending, {force: true}).catch(() => {});
    throw error;
  }
}

function validatePublishedOutput() {
  const validator = path.join(SOURCE, 'validate-android-runtime.cjs');
  const result = childProcess.spawnSync(process.execPath, [validator], {
    cwd: ROOT,
    stdio: 'inherit',
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`Android runtime asset validation failed with exit code ${result.status}`);
  }
}

function profileFor(theme) {
  const entry = DIRECTION.themes[theme.id];
  if (!Array.isArray(entry) || !entry[0] || !DIRECTION.profiles[entry[0]]) {
    throw new Error(`${theme.id}: missing art-direction profile`);
  }
  return {id: entry[0], ...DIRECTION.profiles[entry[0]]};
}

function motifCells(theme, record, motifContentSizes) {
  if (!record || record.id !== theme.id || record.motifs?.length !== 6) {
    throw new Error(`${theme.id}: invalid art record`);
  }
  return record.motifs.map((motif, index) => {
    if (motif.index !== index) throw new Error(`${theme.id}: motif index ${index} is inconsistent`);
    const x = (index % 3) * CELL_SIZE;
    const y = Math.floor(index / 3) * CELL_SIZE;
    return {
      index,
      cellRectPx: {x, y, width: CELL_SIZE, height: CELL_SIZE},
      cellRectNormalized: {
        x: x / ART_WIDTH,
        y: y / ART_HEIGHT,
        width: CELL_SIZE / ART_WIDTH,
        height: CELL_SIZE / ART_HEIGHT,
      },
      contentSizePx: motifContentSizes[index],
    };
  });
}

async function buildRuntimeAtlas(theme, record) {
  const sourcePath = path.join(SOURCE, 'atlases', `${theme.id}.png`);
  const sourceBuffer = await fsp.readFile(sourcePath);
  const substitutions = [...STATIC_MOTIF_SUBSTITUTIONS, ...RUNTIME_ONLY_MOTIF_SUBSTITUTIONS]
    .filter(rule => rule.themeId === theme.id);
  const defaultSizes = record.motifs.map(motif => ({width: motif.width, height: motif.height}));
  if (substitutions.length === 0) {
    return {buffer: sourceBuffer, motifContentSizes: defaultSizes};
  }

  const layers = [];
  const motifContentSizes = [];
  for (const motif of record.motifs) {
    const rule = substitutions.find(candidate => candidate.motifIndex === motif.index);
    const cellX = (motif.index % 3) * CELL_SIZE;
    const cellY = Math.floor(motif.index / 3) * CELL_SIZE;
    let input;
    if (rule?.assetPath) {
      const referencePath = path.join(SOURCE, ...rule.assetPath.split('/'));
      if (!fs.existsSync(referencePath)) throw new Error(`${theme.id}: missing substitution ${rule.assetPath}`);
      input = await sharp(referencePath)
        .resize(464, 464, {fit: 'inside', withoutEnlargement: true, kernel: 'lanczos3'})
        .toColourspace('srgb')
        .png()
        .toBuffer();
    } else if (rule?.sourceThemeId) {
      const donorRecordPath = path.join(COLLECTION, 'records', `${rule.sourceThemeId}-art.json`);
      const donorAtlasPath = path.join(SOURCE, 'atlases', `${rule.sourceThemeId}.png`);
      const donorRecord = JSON.parse(await fsp.readFile(donorRecordPath, 'utf8'));
      const donorMotif = donorRecord.motifs?.find(candidate => candidate.index === rule.sourceMotifIndex);
      if (!donorMotif) throw new Error(`${theme.id}: missing donor motif ${rule.sourceThemeId}/${rule.sourceMotifIndex}`);
      const donorCellX = (donorMotif.index % 3) * CELL_SIZE;
      const donorCellY = Math.floor(donorMotif.index / 3) * CELL_SIZE;
      input = await sharp(donorAtlasPath)
        .extract({
          left: donorCellX + Math.floor((CELL_SIZE - donorMotif.width) / 2),
          top: donorCellY + Math.floor((CELL_SIZE - donorMotif.height) / 2),
          width: donorMotif.width,
          height: donorMotif.height,
        })
        .png()
        .toBuffer();
      const rgba = await sharp(input).ensureAlpha().raw().toBuffer();
      if (sha256(rgba) !== rule.approvedRgbaSha256) {
        throw new Error(`${theme.id}: approved donor pixels changed for motif ${motif.index}`);
      }
    } else {
      input = await sharp(sourceBuffer)
        .extract({
          left: cellX + Math.floor((CELL_SIZE - motif.width) / 2),
          top: cellY + Math.floor((CELL_SIZE - motif.height) / 2),
          width: motif.width,
          height: motif.height,
        })
        .png()
        .toBuffer();
    }
    const metadata = await sharp(input).metadata();
    if (!metadata.width || !metadata.height || metadata.width > CELL_SIZE || metadata.height > CELL_SIZE) {
      throw new Error(`${theme.id}: invalid runtime motif ${motif.index} dimensions`);
    }
    motifContentSizes[motif.index] = {width: metadata.width, height: metadata.height};
    layers.push({
      input,
      left: cellX + Math.floor((CELL_SIZE - metadata.width) / 2),
      top: cellY + Math.floor((CELL_SIZE - metadata.height) / 2),
    });
  }
  const buffer = await sharp({
    create: {width: ART_WIDTH, height: ART_HEIGHT, channels: 4, background: {r: 0, g: 0, b: 0, alpha: 0}},
  }).composite(layers).png({compressionLevel: 9}).toBuffer();
  return {buffer, motifContentSizes};
}

async function encodeRuntimeImages(runtimeAtlasBuffer) {
  const [art, thumb] = await Promise.all([
    sharp(runtimeAtlasBuffer)
      .toColourspace('srgb')
      .webp({quality: 95, alphaQuality: 100, smartSubsample: true, effort: 4})
      .toBuffer(),
    sharp(runtimeAtlasBuffer)
      .resize(THUMB_WIDTH, THUMB_HEIGHT, {fit: 'fill', kernel: 'lanczos3'})
      .toColourspace('srgb')
      .webp({quality: 90, alphaQuality: 100, smartSubsample: true, effort: 3})
      .toBuffer(),
  ]);
  return {art, thumb};
}

async function verifyEncodedImage(buffer, expectedWidth, expectedHeight, label) {
  const metadata = await sharp(buffer).metadata();
  if (metadata.format !== 'webp' || metadata.width !== expectedWidth ||
      metadata.height !== expectedHeight || !metadata.hasAlpha) {
    throw new Error(
      `${label}: invalid runtime image ` +
      `(format=${metadata.format}, size=${metadata.width}x${metadata.height}, alpha=${metadata.hasAlpha})`,
    );
  }
}

async function verifyRuntimeBytes(actual, expected, expectedWidth, expectedHeight, label) {
  await verifyEncodedImage(actual, expectedWidth, expectedHeight, label);
  if (sha256(actual) !== sha256(expected)) {
    throw new Error(`${label}: bytes do not match the reproducible source export`);
  }
}

function placements(theme, profile) {
  const headerSelection = theme.id === 'new-year' ? [0, 2] : [0, 1];
  const headerItems = profile.header === 'editorial'
    ? [{motifIndex: headerSelection[0], x: 0.865, y: 0.49, sizeRule: 'hero', rotationDegrees: 0}]
    : [
        {motifIndex: headerSelection[0], x: 0.115, y: 0.49, sizeRule: 'hero', rotationDegrees: -4},
        {motifIndex: headerSelection[1], x: 0.885, y: 0.49, sizeRule: 'hero', rotationDegrees: 4},
      ];
  return {
    header: {mode: profile.header, items: headerItems},
    sides: [
      {minSideMotifs: 2, motifIndex: 2, edge: 'left', yBase: 0.28, yVariationMultiplier: 1, sizeRule: 'side', rotationDegrees: -7},
      {minSideMotifs: 2, motifIndex: 3, edge: 'right', yBase: 0.69, yVariationMultiplier: -1, sizeRule: 'side', rotationDegrees: 6},
      {minSideMotifs: 4, motifIndex: 4, edge: 'left', yBase: 0.74, yVariationMultiplier: -1, sizeRule: 'side', rotationDegrees: 7},
      {minSideMotifs: 4, motifIndex: 5, edge: 'right', yBase: 0.36, yVariationMultiplier: 1, sizeRule: 'side', rotationDegrees: -7},
    ],
    gutter: {
      enabled: profile.gutterMotifs,
      motifStartIndex: 2,
      motifModulo: 6,
      xCycle: [0.33, 0.67],
      sizeRule: 'gutter',
    },
    footer: {
      enabled: profile.footerMotifs,
      items: [
        {motifIndex: 4, x: 0.105, y: 0.55, sizeRule: 'footer', rotationDegrees: -5},
        {motifIndex: 5, x: 0.895, y: 0.55, sizeRule: 'footer', rotationDegrees: 5},
      ],
    },
  };
}

async function encodeTheme(theme, writeImages = !metadataOnly, verifyExisting = false) {
  const sourcePath = path.join(SOURCE, 'atlases', `${theme.id}.png`);
  const recordPath = path.join(COLLECTION, 'records', `${theme.id}-art.json`);
  if (!fs.existsSync(sourcePath)) throw new Error(`${theme.id}: source atlas is missing`);
  if (!fs.existsSync(recordPath)) throw new Error(`${theme.id}: art record is missing`);

  const record = JSON.parse(await fsp.readFile(recordPath, 'utf8'));
  const runtimeAtlas = await buildRuntimeAtlas(theme, record);
  const metadata = await sharp(runtimeAtlas.buffer).metadata();
  if (metadata.width !== ART_WIDTH || metadata.height !== ART_HEIGHT || !metadata.hasAlpha) {
    throw new Error(`${theme.id}: source atlas must be ${ART_WIDTH}x${ART_HEIGHT} RGBA`);
  }

  const artFile = path.join(ART_OUTPUT, `${theme.id}.webp`);
  const thumbFile = path.join(THUMB_OUTPUT, `${theme.id}.webp`);
  const expected = (writeImages || verifyExisting)
    ? await encodeRuntimeImages(runtimeAtlas.buffer)
    : null;
  const art = writeImages ? expected.art : await fsp.readFile(artFile);
  const thumb = writeImages ? expected.thumb : await fsp.readFile(thumbFile);
  if (verifyExisting) {
    await verifyRuntimeBytes(art, expected.art, ART_WIDTH, ART_HEIGHT, `${theme.id} art`);
    await verifyRuntimeBytes(thumb, expected.thumb, THUMB_WIDTH, THUMB_HEIGHT, `${theme.id} thumbnail`);
  } else {
    await verifyEncodedImage(art, ART_WIDTH, ART_HEIGHT, `${theme.id} art`);
    await verifyEncodedImage(thumb, THUMB_WIDTH, THUMB_HEIGHT, `${theme.id} thumbnail`);
  }
  if (writeImages) {
    await atomicWrite(artFile, art);
    await atomicWrite(thumbFile, thumb);
  }

  const profile = profileFor(theme);
  return {
    theme: {
      index: theme.index,
      id: theme.id,
      displayName: theme.name,
      categoryId: theme.category,
      group: theme.group,
      isManualRecord: theme.group === 'special',
      title: theme.title,
      paperColor: theme.paper,
      inkColor: theme.ink,
      // render-collection.cjs resolves the same art-direction profile pattern. The source
      // catalog also contains historical per-theme pattern metadata, but it is not rendered.
      pattern: profile.pattern,
      profile,
      thumbnailAssetPath: assetPath('thumbs', `${theme.id}.webp`),
      atlas: {
        assetPath: assetPath('art', `${theme.id}.webp`),
        width: ART_WIDTH,
        height: ART_HEIGHT,
        sha256: sha256(art),
        motifs: motifCells(theme, record, runtimeAtlas.motifContentSizes),
      },
      placements: placements(theme, profile),
    },
    files: [
      {kind: 'art', themeId: theme.id, path: assetPath('art', `${theme.id}.webp`), width: ART_WIDTH, height: ART_HEIGHT, hasAlpha: true, bytes: art.length, sha256: sha256(art)},
      {kind: 'thumbnail', themeId: theme.id, path: assetPath('thumbs', `${theme.id}.webp`), width: THUMB_WIDTH, height: THUMB_HEIGHT, hasAlpha: true, bytes: thumb.length, sha256: sha256(thumb)},
    ],
  };
}

async function main() {
  if (CATALOG.themes?.length !== 88) throw new Error(`Expected 88 themes, found ${CATALOG.themes?.length}`);
  await fsp.mkdir(ART_OUTPUT, {recursive: true});
  await fsp.mkdir(THUMB_OUTPUT, {recursive: true});
  // The export updates 178 files individually. Keep a durable marker for the entire operation
  // so an interruption cannot be mistaken for a publishable asset set. Gradle rejects assets
  // while this marker exists, and only the full reproducibility validator may clear it.
  await atomicWrite(
    INCOMPLETE_MARKER,
    Buffer.from(`pid=${process.pid}\nstartedAt=${new Date().toISOString()}\n`),
  );

  const repairIndex = process.argv.indexOf('--repair');
  let writeImages = !metadataOnly;
  let verifyExisting = metadataOnly;
  if (repairIndex >= 0) {
    const id = process.argv[repairIndex + 1];
    const theme = CATALOG.themes.find(candidate => candidate.id === id);
    if (!theme) throw new Error(`Unknown repair theme: ${id || '<missing>'}`);
    if (metadataOnly) throw new Error('--repair and --metadata-only cannot be combined');
    await encodeTheme(theme, true);
    console.log(`Repaired runtime images for ${id}`);
    // Re-read every runtime image so catalog/manifest hashes and byte counts are atomically
    // regenerated after the targeted repair. A repaired image must never leave stale metadata.
    writeImages = false;
    verifyExisting = true;
  }

  const exported = [];
  const batchSize = 4;
  for (let index = 0; index < CATALOG.themes.length; index += batchSize) {
    const batch = CATALOG.themes.slice(index, index + batchSize);
    exported.push(...await Promise.all(batch.map(theme =>
      encodeTheme(theme, writeImages, verifyExisting),
    )));
  }

  const runtimeCatalog = {
    schemaVersion: 1,
    designVersion: 'everyday-editions-v1',
    categories: CATALOG.categories.map((category, order) => ({
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
    themes: exported.map(item => item.theme),
  };
  const catalogBuffer = Buffer.from(`${JSON.stringify(runtimeCatalog, null, 2)}\n`);
  await atomicWrite(path.join(OUTPUT, 'catalog.json'), catalogBuffer);

  const files = exported.flatMap(item => item.files);
  const manifest = {
    schemaVersion: 1,
    designVersion: runtimeCatalog.designVersion,
    themeCount: runtimeCatalog.themes.length,
    categoryCount: runtimeCatalog.categories.length,
    occasionCount: runtimeCatalog.themes.filter(theme => theme.group === 'occasion').length,
    specialCount: runtimeCatalog.themes.filter(theme => theme.group === 'special').length,
    artCount: files.filter(file => file.kind === 'art').length,
    thumbnailCount: files.filter(file => file.kind === 'thumbnail').length,
    totalAssetBytes: files.reduce((sum, file) => sum + file.bytes, 0),
    catalogSha256: sha256(catalogBuffer),
    files,
  };
  await atomicWrite(
    path.join(OUTPUT, 'manifest.json'),
    Buffer.from(`${JSON.stringify(manifest, null, 2)}\n`),
  );
  console.log(`Exported ${manifest.themeCount} themes (${manifest.occasionCount}+${manifest.specialCount})`);
  console.log(`Runtime images: ${manifest.artCount} art + ${manifest.thumbnailCount} thumbnails`);
  console.log(`Runtime image bytes: ${manifest.totalAssetBytes}`);
  validatePublishedOutput();
  await fsp.rm(INCOMPLETE_MARKER, {force: true});
  console.log('Runtime asset set validated and marked complete');
}

if (require.main === module) {
  main().catch(error => {
    console.error(error.stack || error.message || error);
    process.exitCode = 1;
  });
}

module.exports = {
  buildRuntimeAtlas,
  encodeRuntimeImages,
  motifCells,
  placements,
  profileFor,
  STATIC_MOTIF_SUBSTITUTIONS,
  RUNTIME_ONLY_MOTIF_SUBSTITUTIONS,
  verifyRuntimeBytes,
};
