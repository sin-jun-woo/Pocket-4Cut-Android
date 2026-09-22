'use strict';

// Mechanical delivery-size conversion only. All artwork comes from image generation.
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const sharp = require('sharp');

const base = path.resolve(__dirname, '..');
const master = path.join(__dirname, 'instagram-layouts-promo-v2-master.png');
const output = path.join(base, 'deliverables', 'instagram-layouts-promo-v2.png');
const reference = path.join(base, 'deliverables', 'overview-all-seasons.png');
const digest = buffer => crypto.createHash('sha256').update(buffer).digest('hex');
const relative = file => path.relative(base, file).replaceAll('\\', '/');

async function describe(file) {
  const bytes = await fs.readFile(file);
  const metadata = await sharp(bytes).metadata();
  // Full raster decode, not a header-only format check.
  await sharp(bytes).raw().toBuffer();
  return { path: relative(file), bytes: bytes.length, sha256: digest(bytes),
    format: metadata.format, width: metadata.width, height: metadata.height,
    space: metadata.space, hasAlpha: metadata.hasAlpha };
}

async function main() {
  const input = await describe(master);
  assert(input.width >= 1080 && input.height >= 1350, 'Do not upscale the master');
  assert(Math.abs(input.width / input.height - 0.8) < 0.005, 'Unexpected source aspect ratio');
  await sharp(master).resize(1080, 1350, { fit: 'cover', position: 'centre' })
    .flatten({ background: '#F6F2E9' }).toColourspace('srgb')
    .png({ compressionLevel: 9 }).toFile(output);
  const delivered = await describe(output);
  assert.equal(delivered.width, 1080);
  assert.equal(delivered.height, 1350);
  assert.equal(delivered.format, 'png');
  assert.equal(delivered.space, 'srgb');
  assert.equal(delivered.hasAlpha, false);
  const record = {
    schemaVersion: 1, productReference: await describe(reference), master: input,
    delivery: delivered, conversion: '1080x1350 cover resize; opaque sRGB PNG; no content repaint',
    semanticReview: 'Separate human-readable review in docs/WORKLOG.md; dimensions and hashes do not prove text or layout accuracy',
    existingZipRebuilt: false,
  };
  await fs.writeFile(path.join(base, 'verification', 'instagram-layouts-v2-validation.json'),
    JSON.stringify(record, null, 2) + '\n');
  console.log(JSON.stringify(record, null, 2));
}

main().catch(error => { console.error(error); process.exitCode = 1; });
