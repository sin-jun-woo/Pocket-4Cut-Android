#!/usr/bin/env node
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const BASE = path.resolve(__dirname, '..');
const AUDIO = path.join(BASE, 'audio');
const PROOF = path.join(BASE, 'verification');
const request = JSON.parse(fs.readFileSync(path.join(__dirname, 'voice-request.json'), 'utf8'));
const jobPath = path.join(PROOF, 'voice-job.json');
const target = path.join(AUDIO, 'narration-original.wav');

async function main() {
  fs.mkdirSync(AUDIO, { recursive: true }); fs.mkdirSync(PROOF, { recursive: true });
  assert.equal(new URL(request.endpoint).hostname, 'qwen-qwen3-tts.hf.space');
  if (fs.existsSync(target)) { console.log('Existing narration preserved.'); return; }
  let job;
  if (fs.existsSync(jobPath)) {
    job = JSON.parse(fs.readFileSync(jobPath, 'utf8'));
    if (job.error) throw new Error('Provider previously returned an error; no quota bypass or automatic resubmission: ' + job.error);
  } else {
    const res = await fetch(request.endpoint, { method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ data: [request.text, request.language, request.voiceDescription] }), signal: AbortSignal.timeout(30000) });
    if (!res.ok) throw new Error(`Provider HTTP ${res.status}: ${(await res.text()).slice(0,1000)}`);
    const result = await res.json();
    assert(/^[a-f0-9]{32}$/.test(result.event_id), 'Missing public Gradio event id');
    job = { eventId: result.event_id, submittedAt: new Date().toISOString(), provider: request.provider, model: request.model };
    fs.writeFileSync(jobPath, JSON.stringify(job, null, 2)+'\n');
    console.log('Voice request submitted; a resumed run collects the same job rather than generating a duplicate.');
  }
  const events = await fetch(`${request.endpoint}/${job.eventId}`, { signal: AbortSignal.timeout(55000) });
  if (!events.ok) throw new Error(`Result HTTP ${events.status}`);
  const reader = events.body.getReader(), decoder = new TextDecoder();
  let pending = '', output;
  while (!output) {
    const part = await reader.read();
    if (part.done) break;
    pending += decoder.decode(part.value, { stream: true }).replace(/\r\n/g, '\n');
    for (let sep; (sep = pending.indexOf('\n\n')) >= 0;) {
      const event = pending.slice(0, sep); pending = pending.slice(sep+2);
      const kind = event.match(/^event:\s*(.+)$/m)?.[1];
      const data = event.match(/^data:\s*(.+)$/m)?.[1];
      if (kind === 'error') {
        job.error = data || 'Unknown provider error';
        fs.writeFileSync(jobPath, JSON.stringify(job, null, 2)+'\n');
        throw new Error(job.error);
      }
      if (kind === 'complete') output = JSON.parse(data);
    }
  }
  assert(output, 'No completed result yet. Rerun to collect the same job.');
  await reader.cancel();
  if (!output[0]?.url) {
    job.error = String(output[1] || 'Audio missing'); fs.writeFileSync(jobPath, JSON.stringify(job, null, 2)+'\n');
    throw new Error(job.error);
  }
  const url = new URL(output[0].url);
  assert.equal(url.hostname, 'qwen-qwen3-tts.hf.space', 'Unexpected provider download host');
  const download = await fetch(url, { signal: AbortSignal.timeout(30000) });
  assert(download.ok, `Audio download ${download.status}`);
  const audio = Buffer.from(await download.arrayBuffer());
  assert.equal(audio.toString('ascii', 0, 4), 'RIFF'); assert.equal(audio.toString('ascii', 8, 12), 'WAVE');
  fs.writeFileSync(target, audio);
  Object.assign(job, { completedAt: new Date().toISOString(), url: url.href, status: output[1], file: 'audio/narration-original.wav',
    bytes: audio.length, sha256: createHash('sha256').update(audio).digest('hex'), referenceVoice: false, paidApiCall: false });
  fs.writeFileSync(jobPath, JSON.stringify(job, null, 2)+'\n');
  console.log(`Narration generated: ${audio.length} bytes / ${job.sha256}`);
}
main().catch(e => { console.error(e.message); process.exitCode = 1; });
