"""Generate the approved reference-conditioned narration locally with Qwen3-TTS.

Install the official packages and model in an isolated build directory first.
The reference clip is intentionally local-only and is not shipped in this package.
No networking, account credentials, music, time stretching or speech cropping is used.
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
import os
from pathlib import Path
import threading
import time
import traceback
from datetime import datetime, timezone


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def main() -> None:
    package_root = Path(__file__).resolve().parents[1]
    repo_root = package_root.parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model-dir", type=Path, default=repo_root / "build/reels-tools/qwen-tts-local/model-1.7b-base")
    parser.add_argument("--reference-audio", type=Path, default=repo_root / "build/reels-reference-v3/clone-reference.wav")
    parser.add_argument("--reference-text", type=Path, default=repo_root / "build/reels-reference-v3/clone-reference.txt")
    parser.add_argument("--threads", type=int, default=6)
    args = parser.parse_args()

    output = package_root / "audio/narration-original.wav"
    job_file = package_root / "verification/local-voice-job.json"
    if output.exists():
        raise FileExistsError("Existing completed audio preserved: " + str(output))
    for source in (args.model_dir / "model.safetensors", args.reference_audio, args.reference_text):
        if not source.is_file():
            raise FileNotFoundError(source)

    isolated_cache = repo_root / "build/reels-tools/qwen-tts-local/cache"
    os.environ["HF_HOME"] = str(isolated_cache / "huggingface")
    os.environ["TORCH_HOME"] = str(isolated_cache / "torch")
    os.environ["NUMBA_CACHE_DIR"] = str(isolated_cache / "numba")
    os.environ["HF_HUB_OFFLINE"] = "1"
    os.environ["TRANSFORMERS_OFFLINE"] = "1"
    os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
    os.environ["TOKENIZERS_PARALLELISM"] = "false"
    os.environ["OMP_NUM_THREADS"] = str(args.threads)
    os.environ["MKL_NUM_THREADS"] = str(args.threads)

    script = json.loads((package_root / "source/narration-script.json").read_text(encoding="utf-8"))
    reference_text = args.reference_text.read_text(encoding="utf-8").strip()
    started = time.monotonic()
    job = {
        "status": "starting", "startedAt": utc_now(),
        "provider": "Local Qwen3-TTS inference; no external inference request",
        "model": "Qwen/Qwen3-TTS-12Hz-1.7B-Base",
        "modelRevision": "fd4b254389122332181a7c3db7f27e918eec64e3",
        "publishedModelSha256": "38fc7fc51c5e776e840414b6fd443962e9411b9654888fd7913e4da643cb857c",
        "publishedSpeechTokenizerSha256": "836b7b357f5ea43e889936a3709af68dfe3751881acefe4ecf0dbd30ba571258",
        "publishedQwenPackageSha256": "11a290d8dabc7ef91a90c54478c8ab19b3edb1d85c0882313721892bdc4af15d",
        "modelLicense": "Apache-2.0", "device": "cpu", "dtype": "float32",
        "attentionImplementation": "sdpa", "threads": args.threads,
        "seed": 91026, "language": "Korean", "xVectorOnlyMode": False,
        "referenceAudioSha256": hashlib.sha256(args.reference_audio.read_bytes()).hexdigest(),
        "referenceAudioDurationSeconds": 6.0,
        "referenceAuthorization": "User explicitly requested this reference AI timbre and approved voice and script transfer to official Qwen/Hugging Face; local inference used for this completed attempt.",
        "referenceTranscriptProvenance": "Local ASR with visible subtitle spelling correction; no subjective listening claim",
        "referenceAudioPackaged": False,
        "scriptSha256": hashlib.sha256(script["text"].encode("utf-8")).hexdigest(),
        "script": script["text"], "musicAdded": False, "effectsAdded": False,
        "speedMultiplier": 1.0, "speechCropped": False,
        "generationMaxNewTokens": 2048, "nonStreamingMode": True,
        "talkerForwardCalls": 0,
        "limitations": ["Reference-conditioned synthesis does not guarantee acoustically identical voice.", "Subjective voice similarity and listening are not verified by this script."],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    job_file.parent.mkdir(parents=True, exist_ok=True)
    lock = threading.Lock()

    def record(status: str | None = None) -> None:
        with lock:
            if status:
                job["status"] = status
            job["updatedAt"] = utc_now()
            job["elapsedSeconds"] = round(time.monotonic() - started, 3)
            job_file.write_text(json.dumps(job, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            print(json.dumps({key: job[key] for key in ("status", "elapsedSeconds", "talkerForwardCalls")}), flush=True)

    stopped = threading.Event()

    def heartbeat() -> None:
        while not stopped.wait(30):
            record()

    record()
    threading.Thread(target=heartbeat, daemon=True).start()
    try:
        import numpy as np
        import soundfile as sf
        import torch
        from qwen_tts import Qwen3TTSModel

        torch.set_num_threads(args.threads)
        torch.set_num_interop_threads(1)
        torch.manual_seed(job["seed"])
        np.random.seed(job["seed"])
        job["versions"] = {name: importlib.metadata.version(name) for name in ("qwen-tts", "torch", "torchaudio", "transformers", "accelerate", "soundfile")}
        record("loading_model")
        model = Qwen3TTSModel.from_pretrained(
            str(args.model_dir), device_map="cpu", dtype=torch.float32,
            attn_implementation="sdpa", local_files_only=True,
        )
        record("encoding_reference")
        reference_audio, reference_sr = sf.read(args.reference_audio, dtype="float32")
        prompt = model.create_voice_clone_prompt(
            ref_audio=(reference_audio, reference_sr), ref_text=reference_text,
            x_vector_only_mode=False,
        )

        def count_step(module, inputs, result):
            job["talkerForwardCalls"] += 1

        hook = model.model.talker.register_forward_hook(count_step)
        record("generating_speech")
        generation_started = time.monotonic()
        with torch.inference_mode():
            waves, sample_rate = model.generate_voice_clone(
                text=script["text"], language="Korean", voice_clone_prompt=prompt,
                non_streaming_mode=True, max_new_tokens=2048,
            )
        hook.remove()
        job["generationSeconds"] = round(time.monotonic() - generation_started, 3)
        wave = np.asarray(waves[0], dtype=np.float32)
        if wave.ndim != 1 or len(wave) == 0 or not np.isfinite(wave).all():
            raise ValueError("Invalid generated waveform")
        # Store a float WAV so the source is not silently clipped by PCM conversion.
        sf.write(output, wave, sample_rate, subtype="FLOAT")
        job.update({
            "sampleRate": int(sample_rate), "channels": 1, "sampleCount": int(len(wave)),
            "durationSeconds": len(wave) / sample_rate, "peakAbsoluteAmplitude": float(np.max(np.abs(wave))),
            "output": "audio/narration-original.wav", "outputBytes": output.stat().st_size,
            "outputSha256": hashlib.sha256(output.read_bytes()).hexdigest(), "completedAt": utc_now(),
        })
        record("complete")
    except Exception as error:
        job["error"] = {"type": type(error).__name__, "message": str(error)}
        record("error")
        traceback.print_exc()
        raise
    finally:
        stopped.set()


if __name__ == "__main__":
    main()
