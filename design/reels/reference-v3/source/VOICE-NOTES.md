# 참조 조건 내레이션 · 실제 제작 기록

## 생성

사용자가 참조 AI 음색 사용과 추출 음성·새 대본의 공식 Qwen/Hugging Face 전송을 명시적으로 허용했다. 공개 ASR/TTS 서비스는 각각 1회 요청에서 상세 설명 없는 오류를 반환했다. 원인을 추정하지 않고 기록을 보존한 뒤 로컬 방식으로 전환했다. 원본 녹화 영상은 음성 서비스에 전송하지 않았다.

공식 [Qwen3-TTS](https://github.com/QwenLM/Qwen3-TTS)의 [12Hz-1.7B-Base](https://huggingface.co/Qwen/Qwen3-TTS-12Hz-1.7B-Base) 모델을 사용했다. revision `fd4b254389122332181a7c3db7f27e918eec64e3`, 모델/토크나이저 가중치와 qwen-tts 배포 패키지의 공개 SHA-256을 확인했다. 모델 표기 라이선스는 Apache-2.0이다.

로컬 Python 3.12 독립 환경에 qwen-tts 0.1.1, PyTorch/torchaudio 2.8.0+cpu, Transformers 4.57.3, Accelerate 1.12.0을 설치했다. 앱 및 공유 Python 의존성은 변경하지 않았다. `pip check` 결과 의존성 오류가 없었다. CPU float32/SDPA, 6스레드, seed 91026, Korean, `x_vector_only_mode=False`로 6초 참조와 참조 대본을 함께 사용했다. 참조 대본 철자 일부는 로컬 자동 전사와 화면 자막을 대조했다.

새 대본 전체를 한 번에 생성했다. 실제 생성 209.125초, 전체 220.281초, talker 253 steps. 출력은 20.160초/24 kHz/mono/32-bit float WAV다. 최대 절대 진폭 0.72057이며 PCM 범위 초과는 없었다. 새 음성의 SHA-256은 `e443490cc35c5e545a5c264c667c7e3d73f17cb1fa2a65be11f0208ab7b0d7bc`다. 참조 원본·대본·영상은 ignored 작업 영역에 두며 공개 파일에 포함하지 않는다.

## 전사와 마스터

공식 [whisper.cpp b4938](https://github.com/ggml-org/whisper.cpp/tree/b4938)와 다국어 small-q5_1 모델을 로컬에서 사용했다. 새 음성을 16 kHz mono PCM16으로 변환한 뒤, 정답 대본 프롬프트 없이 인식했다. `-nfa -dtw small`로 실제 DTW가 켜진 로그를 확인했다. 입력/변환 WAV/출력 JSON의 해시는 `verification/asr-input.json`에 연결돼 있다.

인식 원문은 `verification/asr-result.json`에 보존한다. 공백·문장부호와 컷/장 앞의 숫자 표기를 정규화한 136문자가 대본과 일치한다. 숫자를 한글로 환산하면 대본의 한글 음절 137개에 해당한다. 마지막 ASR 구간 끝은 20.18초로 원본 WAV보다 20 ms 늦다. 문장 끝은 추정 시각이며 샘플 단위 정렬을 의미하지 않는다.

음성은 배속/발화 삭제 없이 0.12초 시작 여유와 후반 검색 화면용 무음을 추가해 23초로 정리했다. high-pass 60 Hz, 약한 압축, 두 단계 loudness normalization을 적용하고 48 kHz stereo PCM24로 저장했다. 음악·효과음·참조 광고의 실제 음성 조각을 최종 음성에 삽입하지 않았다. 실제 필터/수치/해시는 `verification/narration-master.json`에 있다.

## 한계

참조 오디오를 조건으로 사용한 사실과 원본과 완전히 같은 목소리라는 평가는 다르다. 이번 환경에서는 직접 청취, 독립 speaker embedding 비교, 자연스러움 평가를 수행하지 않았다. 자동 전사 일치는 이 평가를 대신하지 않는다. 실제 휴대전화에서 말투·브랜드 발음·원하신 음색과의 유사성을 들어 보고 게시해야 한다.

사용자 제공 음성이 AI로 생성됐다는 설명을 독립적으로 검증하거나 상업적 권리를 보증한 작업은 아니다. 모델 라이선스는 참조 음성의 권리와 별개다. 참조 계정명·원본 장면·원본 대사를 광고나 ZIP에 넣지 않는다.
