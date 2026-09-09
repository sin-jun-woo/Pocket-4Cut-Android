const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');

async function main() {
  const input = process.argv[2];
  if (!input || !fs.existsSync(input)) throw new Error('Pass the generated four-photo contact sheet path.');
  const output = path.join(__dirname, '..', 'demo-photos');
  fs.mkdirSync(output, { recursive: true });
  fs.copyFileSync(input, path.join(output, 'generated-contact-sheet.png'));
  const { width, height } = await sharp(input).metadata();
  const side = Math.floor(Math.min(width, height) / 2);
  for (let index = 0; index < 4; index++) {
    await sharp(input)
      .extract({ left: (index % 2) * side, top: Math.floor(index / 2) * side, width: side, height: side })
      .toColourspace('srgb').jpeg({ quality: 95 })
      .toFile(path.join(output, `demo_${String(index + 1).padStart(2, '0')}.jpg`));
  }
  console.log(JSON.stringify({ inputSize: [width, height], photoSize: [side, side], output }));
}
main().catch(error => { console.error(error); process.exitCode = 1; });
