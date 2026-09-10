#!/usr/bin/env node
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const BASE = path.resolve(__dirname, '..');
const ROOT = path.resolve(BASE, '../../..');
const HOST = 'https://qwen-qwen3-tts.hf.space';
const ENDPOINT = HOST + '/gradio_api/call/generate_voice_clone';
const script = JSON.parse(fs.readFileSync(path.join(__dirname, 'narration-script.json'), 'utf8'));
const AUDIO = path.join(BASE, 'audio');
const PROOF = path.join(BASE, 'verification');
const jobPath = path.join(PROOF, 'voice-job.json');
const target = path.join(AUDIO, 'narration-original.wav');
const hash = b => createHash('sha256').update(b).digest('hex');
function save(job) { fs.writeFileSync(jobPath, JSON.stringify(job, null, 2)+'\n'); }
async function fetchOk(url, opts = {}, timeout = 30000) {
  const r = await fetch(url, { ...opts, signal: AbortSignal.timeout(timeout) });
  if (!r.ok) throw new Error(`Official provider HTTP ${r.status}: ${(await r.text()).slice(0, 900)}`);
  return r;
}
async function main() {
  fs.mkdirSync(AUDIO, { recursive: true }); fs.mkdirSync(PROOF, { recursive: true });
  if (fs.existsSync(target)) { console.log('Existing final narration preserved.'); return; }
  let job = fs.existsSync(jobPath) ? JSON.parse(fs.readFileSync(jobPath, 'utf8')) : null;
  if (job?.error) throw new Error('Previous provider error is retained; no automatic new request: '+job.error);
  if (!job) {
    assert.equal(process.env.REFERENCE_UPLOAD_AUTHORIZED, 'yes', 'Explicit user authorization for Qwen/Hugging Face voice upload is required.');
    const refAudio = process.env.REFERENCE_AUDIO || path.join(ROOT, 'build/reels-reference-v3/clone-reference.wav');
    const refTextFile = process.env.REFERENCE_TEXT_FILE || path.join(ROOT, 'build/reels-reference-v3/clone-reference.txt');
    assert(fs.existsSync(refAudio) && fs.existsSync(refTextFile), 'Verified reference clip and exact transcript are required.');
    const data = fs.readFileSync(refAudio), refText = fs.readFileSync(refTextFile, 'utf8').replace(/^\uFEFF/, '').trim();
    assert(refText.length > 10 && refText.length < 400, 'Use a bounded, verified speech reference.');
    const form = new FormData(); form.append('files', new Blob([data], { type: 'audio/wav' }), 'reference-voice.wav');
    const uploaded = await (await fetchOk(HOST+'/gradio_api/upload', { method: 'POST', body: form })).json();
    assert(Array.isArray(uploaded) && typeof uploaded[0] === 'string' && uploaded[0].startsWith('/tmp/gradio/'));
    const payload = { data: [
      { path: uploaded[0], orig_name: 'reference-voice.wav', mime_type: 'audio/wav', meta: { _type: 'gradio.FileData' } },
      refText, script.text, script.language, false, '1.7B'
    ] };
    const response = await (await fetchOk(ENDPOINT, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })).json();
    assert(/^[a-f0-9]{32}$/.test(response.event_id), 'Missing Gradio event id');
    job = { eventId: response.event_id, submittedAt: new Date().toISOString(), mode: 'reference-audio voice clone',
      model: 'Qwen/Qwen3-TTS-12Hz-1.7B-Base', language: 'Korean', useXvectorOnly: false,
      referenceSha256: hash(data), targetScriptSha256: hash(Buffer.from(script.text)),
      authorization: 'User expressly requested the reference AI timbre and approved extracted voice plus script transmission to Qwen/Hugging Face.',
      referenceMediaIncludedInDeliverables: false, paidApiCall: false };
    save(job); console.log('One reference-voice job submitted. Resume this same job after interruptions.');
  }
  const events = await fetchOk(ENDPOINT+'/'+job.eventId, {}, 55000);
  const reader = events.body.getReader(), decoder = new TextDecoder();
  let pending = '', output;
  while (!output) {
    const part = await reader.read(); if (part.done) break;
    pending += decoder.decode(part.value, { stream: true }).replace(/\r\n/g, '\n');
    for (let sep; (sep=pending.indexOf('\n\n')) >= 0;) {
      const event = pending.slice(0,sep); pending = pending.slice(sep+2);
      const kind=event.match(/^event:\s*(.+)$/m)?.[1], value=event.match(/^data:\s*(.+)$/m)?.[1];
      if (kind === 'error') { job.error=value || 'Provider error'; save(job); throw new Error(job.error); }
      if (kind === 'complete') output=JSON.parse(value);
    }
  }
  assert(output, 'Job not complete yet; resume the same event.'); await reader.cancel();
  if (!output[0]?.url) { job.error=String(output[1] || 'Missing generated audio'); save(job); throw new Error(job.error); }
  const url = new URL(output[0].url); assert.equal(url.hostname, 'qwen-qwen3-tts.hf.space');
  const bytes = Buffer.from(await (await fetchOk(url)).arrayBuffer());
  assert.equal(bytes.toString('ascii',0,4),'RIFF'); assert.equal(bytes.toString('ascii',8,12),'WAVE');
  fs.writeFileSync(target,bytes);
  Object.assign(job,{ completedAt:new Date().toISOString(), status:output[1], output:'audio/narration-original.wav', bytes:bytes.length, sha256:hash(bytes) });
  save(job); console.log(JSON.stringify({status:job.status,bytes:job.bytes,sha256:job.sha256},null,2));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
