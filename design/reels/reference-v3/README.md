# Pocket4Cut · 내 폰이 네 컷 사진관으로

2026-09-10 KST 제작. 기존 `film-story-v1`, `narrator-v2`를 보존하는 별도 23초 광고다. 참조 음성을 기준으로 새 내레이션을 생성하고, 포켓네컷을 소개하는 자체 흐름과 친근한 존댓말을 사용한다. 음악·효과음은 추가하지 않는다.

## 사용할 파일

- `deliverables/Pocket4Cut-Reels-Reference-Voice.mp4`: 세로 본편, 1080×1920 / 30 fps / H.264 BT.709 + AAC stereo 48 kHz.
- `deliverables/Pocket4Cut-Reference-Narration.mp3`: 동일한 음성 트랙, MP3 192 kbps 설정.
- `deliverables/Pocket4Cut-Reference-Cover.jpg`: 1080×1920 커버.
- `deliverables/CAPTIONS-KO.srt`: 화면 자막 타이밍. 숫자 표기와 축약 자막을 포함해 음성의 축어 전사와 다르다.
- `deliverables/NARRATION-KO.txt`, `INSTAGRAM-CAPTION.txt`, `UPLOAD-GUIDE.md`: 대본, 게시글 초안, 업로드 확인 사항.
- `Pocket4Cut-Reference-Reels-Package.zip`: 위 일곱 파일의 납품 패키지.

## 무엇이 달라졌나

“내 폰이 네 컷 사진관이 된다면요?”로 시작해 완성 사진을 먼저 보여준다. 앱 이름 → 촬영 → 선택 → 배치/필터 → 저장/공유 → 사용할 상황 → 검색 순서로 소개한다. 원본 의류 광고의 이야기나 반말 대사는 옮기지 않는다. 참조 계정명·원본 영상·원본 대본을 본편/커버/ZIP에 넣지 않는다.

큰 필름 사진과 종이색 배경, 짧은 자막, 필요한 부분만 확대한 앱 화면을 사용한다. 화면은 실제 Compose 코드 기반 호스트 렌더와 CollageRenderer 출력의 편집 예시이며 실기기 연속 녹화가 아니다. 기존 Film Strip v4 아이콘과 AI 생성 가상 성인 예시 사진을 재사용했다. 새 앱 아이콘이나 사진을 생성하지 않았고 앱 소스·리소스·의존성도 변경하지 않았다.

사용자의 후속 요청에 따라 본편과 커버의 상·하단 설명 오버레이를 제거했다. 음성·대본·소개 자막은 그대로다. 자세한 편집 의도는 `source/CREATIVE-BRIEF.md`에 있다.

## 음성과 검증

실제 결과: 20.160초 원본 음성을 23초 마스터로 정리했다. 자동 전사는 숫자·공백·구두점 정규화 후 대본과 일치했다. 14장면/690프레임 최종 MP4의 전체 디코딩·규격·동기·faststart·음량 검사를 통과했고, 검은 구간은 없었다. 최종 AAC는 −16.0 LUFS / −3.4 dBTP다. 설명 오버레이 제거본의 28개 시점 스틸과 핵심 텍스트 경계 41개를 검사하고 최종 추출 프레임·커버를 시각 검토했다. ZIP의 7개 항목 모두 원본 해시와 일치했다.

사용자가 참조 AI 음색 사용과 Qwen/Hugging Face로의 추출 음성·새 대본 전송을 명시적으로 허용했다. 공식 공개 TTS/ASR 서비스는 각 한 번의 요청에서 상세 원인 없는 오류를 반환했다. 성공했다고 처리하거나 임의의 다른 음색으로 대체하지 않고 로컬 생성·전사 경로로 전환한다. 원본 영상·참조 음성·원본 전체 전사는 ignored `build/` 작업 영역에만 두며 Git과 납품 패키지에 포함하지 않는다.

- `audio/narration-original.wav`: 참조 음성 조건으로 생성한 새 대본의 원본 음성.
- `audio/narration-master.wav`: 시작 여유·후반 검색 여유 및 음량을 정리한 48 kHz stereo PCM24 마스터.
- `verification/local-voice-job.json`: 실제 로컬 모델·설정·생성 결과 기록.
- `verification/asr-result.json`: 로컬 자동 전사의 실제 원문, 대본 비교와 타이밍 근거. 오인식은 정답 전사로 바꾸지 않는다.
- `verification/narration-master.json`: 길이·음량 처리·해시.
- `source/story-timed.json`: 실제 음성을 기준으로 한 연속 장면 구간.
- `visual-stills/`: 각 장면의 두 시점 이미지, 재료 경로·치수·해시, 핵심 자막 경계 검사.
- `verification/export-validation.json`: 실제 MP4 규격·디코딩·길이 동기·음량·검은 구간·해시 검사.
- `verification/encoded-contact-sheet.jpg`: 최종 MP4에서 추출한 장면 모아보기. 렌더 전 시안과 구별한다.
- `verification/package-validation.json`: ZIP 내부 항목과 원본의 SHA-256 비교.

자동 전사·수치 검증은 음색 동일성이나 자연스러움을 실제로 들은 평가가 아니다. 참조 조건을 사용한 생성이 원본과 완전히 같은 음색을 보증하지 않는다. 휴대전화 청취, Instagram 자르기/UI 겹침, 실제 스토어 배포 상태는 게시 전에 확인해야 한다. 계정 게시나 광고 집행은 수행하지 않는다.

## 재출력

Node.js 24, `@napi-rs/canvas`, FFmpeg 9.0.1, 한글 글꼴을 사용한다. `NODE_PATH`는 Canvas 패키지 폴더, `FFMPEG_PATH`는 FFmpeg 실행 파일로 지정한다. `FFPROBE_PATH`를 생략하면 같은 폴더의 ffprobe를 사용한다. 글꼴 경로는 `REEL_FONT_REGULAR`, `REEL_FONT_BOLD`로 바꿀 수 있다. 모델·실행 파일·폰트는 앱/납품 ZIP에 포함하지 않는다.

```powershell
$reelSteps = @('transcribe-narration', 'master-narration', 'time-captions', 'render-reference', 'export-reference')
foreach ($reelStep in $reelSteps) {
    node "design/reels/reference-v3/source/$reelStep.cjs"
    if ($LASTEXITCODE -ne 0) { throw "Stopped at $reelStep" }
}
powershell -NoProfile -ExecutionPolicy Bypass -File design/reels/reference-v3/source/package-reference.ps1
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
```

전사 단계에는 로컬 whisper.cpp CLI와 small-q5_1 모델이 필요하며 `WHISPER_CLI_PATH`, `WHISPER_MODEL_PATH`로 경로를 지정할 수 있다. 기존 전사 자료가 있고 입력 WAV/전사 JSON/마스터의 해시가 맞으면 전사 단계를 생략할 수 있다. 음성을 재생성하려면 비공개 참조 클립과 정확한 참조 대본을 준비하고 `generate-local-voice.py`의 인자를 사용한다. 원본 음성이 이미 있으면 그것을 보존한다. 실패한 공식 서비스 요청은 `voice-job.json`에 남기며 자동 재요청하지 않는다.

도구의 공식 문서는 [FFmpeg](https://ffmpeg.org/), [Qwen3-TTS](https://github.com/QwenLM/Qwen3-TTS), [whisper.cpp](https://github.com/ggml-org/whisper.cpp)에 있다. 모델 라이선스와 참조 음성의 상업적 이용 권한은 별개다. 사용자 제공 음성의 AI 생성·권리 설명을 독립적으로 검증했다는 주장은 하지 않는다.
