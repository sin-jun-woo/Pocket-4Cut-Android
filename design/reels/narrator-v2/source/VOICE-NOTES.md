# 원본 한국어 남성 AI 음성

## 생성 출처

[Qwen3-TTS 공식 저장소](https://github.com/QwenLM/Qwen3-TTS), [VoiceDesign 모델 카드](https://huggingface.co/Qwen/Qwen3-TTS-12Hz-1.7B-VoiceDesign), [Qwen 공식 공개 데모](https://huggingface.co/spaces/Qwen/Qwen3-TTS)를 사용했다. 모델은 `Qwen/Qwen3-TTS-12Hz-1.7B-VoiceDesign`이며, 공개 데모의 `generate_voice_design` 경로로 한국어 대본과 텍스트 음색 지시만 전달했다.

원본 요청 전문은 `voice-request.json`에 보존했다. 젊은 성인 한국 남성, 자연스러운 서울 말투, 차분한 중저음, 친구에게 이야기하듯 약간 빠른 속도, 힘을 뺀 유머와 가벼운 어미 상승을 요청했다. 특정 실존 인물·성우·크리에이터를 모방하지 말라고 명시했고 참조 음성을 제공하지 않았다.

공식 모델/데모는 Apache-2.0로 표기되어 있다. 이는 생성 음성의 독점권이나 모든 국가의 법적 권리/광고 심사 통과를 보증한다는 뜻이 아니다. 생성 사실을 공개하며, 타인의 상업용 음원을 추출하거나 소비자용 음성 서비스의 보호 수단을 우회하지 않았다. [공식 ZeroGPU 안내](https://huggingface.co/docs/hub/en/spaces-zerogpu)의 익명 이용량 제한 안에서 시험 1회·전체 생성 1회를 수행했다. 별도 유료 API, 계정 키, 플러그인 설치, 음성 복제는 사용하지 않았다.

## 음성 원본 및 후처리

- 최종 원본: `audio/narration-original.wav`, 25.953750초 / 24 kHz / mono / PCM16 / 1,245,824 bytes.
- SHA-256: `c0cf285df090d47d978c5779c32cac8df3f41a968f5da73c9ba5bb6b39dd2c19`.
- 편집 원본: `audio/narration-master-30s.wav`, 30초 / 48 kHz / stereo / PCM24.
- 음성에 70 Hz high-pass, 약한 압축, 2-pass loudness normalization을 적용했다. 실제 처리 파라미터와 계측은 `master-narration.cjs`, `verification/narration-master.json`에 있다.
- 말속도는 1.0배다. 0.15초 시작 여유, 마지막 무음을 추가했으며 음악·효과음을 혼합하지 않았다. 최종 단어 이후 검색 화면을 읽을 시간을 남겼다.
- MP4에는 AAC 192 kbps 설정, 별도 음성에는 MP3 192 kbps 설정을 사용했다. 최종 AAC의 측정값은 `verification/export-validation.json`이 기준이다.

## 내용 검증과 자막

[Qwen3-ASR 공식 공개 데모](https://huggingface.co/spaces/Qwen/Qwen3-ASR)에서 전체 원본 음성에 대해 ASR와 forced alignment를 한 번 수행했다. 정규화한 한국어 대본의 119개 음절이 모두 일치했다. 결과와 발화 구간은 `verification/asr-result.json`에 보존했다. 음성은 “포켓 네 컷”으로 읽고 화면 브랜드는 `Pocket4Cut`으로 표기한다.

실제 발화 구간에 시작 여유 150 ms를 더하고 자막을 약 60 ms 먼저 보여준다. “배치랑 필터도 바꿨어요”는 두 시각 구간으로 나누되 자막을 유지한다. 흑백 감상 구간은 실제 흑백 스타일 결과와 대응한다. 마지막 CTA는 무음 화면 문구이므로 SRT의 모든 줄이 음성 대사는 아니다.

## 검증 한계

생성·전사·음량·파일 디코딩을 실제로 확인했지만, 현재 작업 환경에서는 사람이 듣듯 음성의 억양·음색·친근함을 주관적으로 청취 평가하지 못했다. ASR 일치가 자연스러움, 성별 인상, 설득력 또는 Instagram 심사를 증명하지 않는다. 게시 전 제공한 MP3/MP4를 휴대전화에서 직접 듣고 확인해야 한다.
