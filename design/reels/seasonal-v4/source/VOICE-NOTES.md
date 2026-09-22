# 사계절 프레임 내레이션 · Reference v3와 같은 로컬 생성 경로

사용자는 마지막 제작 릴스의 기본 틀을 유지하고 소개 내용만 교체하도록 요청했다. 이전에 제공·사용을 승인한 6초 AI 음색 참조와 같은 Qwen 모델, CPU float32/SDPA, Korean, 6스레드, seed 91026, 참조 대본 포함 조건을 사용한다. 새로운 타인 음성이나 다른 기본 화자로 바꾸지 않는다. 참조 조건의 재사용이 음색의 완전한 동일성을 보증하지는 않는다.

## 현재 실행 환경 확인

- 기존 독립 Python 3.12.14 환경에서 qwen-tts 0.1.1, torch/torchaudio 2.8.0+cpu, transformers 4.57.3, accelerate 1.12.0, soundfile 0.14.0을 확인했다. `pip check`는 의존성 오류가 없었다.
- 모델은 공식 [Qwen3-TTS 12Hz-1.7B-Base](https://huggingface.co/Qwen/Qwen3-TTS-12Hz-1.7B-Base), revision `fd4b254389122332181a7c3db7f27e918eec64e3`다. 모델과 speech tokenizer의 현재 파일 해시를 기존 공개 검증값과 대조했다. 새 설치/다운로드는 하지 않았다.
- 참조 WAV의 현재 해시도 Reference v3와 일치한다. ffprobe 결과 6초 / 24 kHz / mono PCM16이다. 참조 내용·음성·사용자 영상은 ignored build 영역에서만 읽고 Git/ZIP에 넣지 않는다.
- 모델 라이선스 표기는 Apache-2.0이며 참조 음성이나 생성물의 상업적 권리 보증과는 다르다. 사용자 제공 음성이 AI 생성이라는 설명을 독립적으로 검증했다고 주장하지 않는다.

## 실행·검증 경계

`generate-local-voice.py`는 네트워크를 사용하지 않는 offline 로컬 생성이다. 새 `audio/narration-original.wav`를 생성하며 기존 완료 파일이 있으면 덮어쓰지 않는다. 발화를 자르거나 배속하지 않고, `master-narration.cjs`로 0.12초 시작 여유·끝 안내 여유·음량을 조정한다. 음악·효과음은 없다.

`transcribe-narration.cjs`는 로컬 whisper.cpp b4938/small-q5_1에 정답 대본 프롬프트 없이 새 음성을 입력한다. `time-captions.cjs`는 실제 ASR 원문을 보존하며 공백·문장부호 및 컷/장/가지 앞의 2/4/6/8 숫자 표기만 정규화한다. 실제 인식 내용이 다르면 후속 렌더를 중단한다. 2·4·6컷 화면은 각 숫자의 DTW 시각을 기준으로 전환한다.

실제 생성 시간·길이·peak·음성 해시는 `verification/local-voice-job.json`, 마스터 필터와 해시는 `verification/narration-master.json`, 전사 비교와 추정 타이밍은 `verification/asr-result.json`이 기준이다. 이 안내의 존재를 생성 또는 검증 완료로 대신하지 않는다.

자동 전사 일치는 발음·억양·주관적 자연스러움이나 원본과 동일한 음색의 청취 평가가 아니다. 실제 휴대전화 청취와 Instagram 업로드 결과는 게시 전에 별도로 확인해야 한다.
