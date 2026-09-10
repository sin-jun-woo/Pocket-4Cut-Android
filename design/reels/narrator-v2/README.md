# Pocket4Cut · 저, 네 컷 만들었어요

2026-09-10 KST 제작. 기존 `film-story-v1`을 보존한 두 번째 릴스 광고다. 잔잔한 남성 AI 나레이션을 중심으로, 사진관에 가지 않았다는 반전과 실제 앱 사용 흐름을 30초에 담았다. 음악과 효과음은 넣지 않았다.

## 바로 사용할 파일

- `deliverables/Pocket4Cut-Reels-Male-Narration-30s.mp4`: 완성 영상. 1080×1920, 9:16, 30 fps, 30초, H.264/BT.709 + AAC stereo 48 kHz.
- `deliverables/Pocket4Cut-Male-Narration.mp3`: 동일한 30초 음성 트랙. 음악 없음. 끝부분은 검색 화면용 무음이다.
- `deliverables/Pocket4Cut-Narrator-Cover.jpg`: 1080×1920 커버.
- `deliverables/CAPTIONS-KO.srt`: 영상에 보이는 자막의 타이밍. 일부 숫자 표기 및 마지막 무음 CTA를 포함하므로 음성의 축어 전사와는 다르다.
- `deliverables/NARRATION-KO.txt`: 실제 음성 생성 대본.
- `deliverables/INSTAGRAM-CAPTION.txt`, `deliverables/UPLOAD-GUIDE.md`: 게시글 초안과 업로드 확인 사항.
- `Pocket4Cut-Narrator-Reels-Package.zip`: 위 7개 파일을 모은 납품본.

## 구성과 근거

“저, 네 컷 만들었어요. 근데 사진관은 안 갔어요.”로 시작한다. 결과를 먼저 보여주고, 4컷 선택 → 8장 자동 촬영 안내 → 잘 나온 4장 선택 → 배치/필터 → 저장/공유 → 친구와 다음에 찍어보자는 제안으로 연결한다. 음성 문장에 맞춘 15개 편집 구간에 큰 사진, 확대된 앱 화면, 두 줄 이내 자막을 배치했다.

남성 내레이션의 일상 보고체·무심한 유머, 초반 반전, 구체적인 사용 장면을 참고했다. 특정 성우/크리에이터의 음성을 복제하지 않았다. 최신 조사 자료와 국내 말투 참고의 날짜·한계는 `source/CREATIVE-BRIEF.md`에 구분했다. 특정 세대 전체의 취향이나 광고 성과를 보장하는 작업이 아니다.

실제 앱 코드 기반 Compose 호스트 렌더와 CollageRenderer 결과, 기존 Film Strip v4 아이콘 및 AI 생성 가상 성인 예시 사진을 재사용했다. 실기기 연속 녹화나 실제 고객 후기가 아니다. 상단 `광고 · 사용 예시`, 하단 `AI 내레이션 · AI 생성 예시 사진` 고지를 유지했다. 앱 소스·런처 아이콘·의존성은 변경하지 않았다.

## 제작 및 검증 자료

- `source/voice-request.json`: 한국어 대본, 원본 음색 지시, 모델과 공개 API 출처.
- `audio/narration-original.wav`: Qwen 공식 공개 데모가 생성한 원본. 25.953750초, 24 kHz mono PCM16.
- `audio/narration-master-30s.wav`: 48 kHz stereo PCM24. 말속도 변경 없이 0.15초 시작 여유와 마지막 무음을 추가했다.
- `audio/voice-test.wav`: 제작 전 짧은 음색 시험. 최종 영상/ZIP에는 포함하지 않는다.
- `verification/asr-result.json`: Qwen 공식 ASR 및 forced alignment. 문장부호/공백을 제외한 119개 한글 음절이 대본과 일치했다. 음성의 자연스러움이나 음색에 대한 청취 평가를 대신하지 않는다.
- `source/story-timed.json`: 실제 발화 시각에 맞춘 자막/화면 구간. 가독성을 위해 자막은 발화보다 약 60 ms 앞서 표시하고 다음 구간까지 유지한다.
- `visual-stills/`: 15구간의 두 시점 스틸, 재료 목록/해시, 자막 여백 검사, 렌더 기록.
- `verification/export-validation.json`: 최종 MP4의 코덱, 길이, 동기, 전체 디코딩, 검은 구간, 음량, 해시 검사.
- `verification/encoded-contact-sheet.jpg`: 최종 MP4에서 추출한 15구간 모아보기. 생성 시안과 구별한다.
- `verification/package-validation.json`: ZIP 항목별 원본 SHA-256 일치 검사.

## 재출력

Node.js 24, `@napi-rs/canvas`, FFmpeg 9.0.1과 한글 글꼴을 사용했다. 프로젝트의 Gradle/npm 의존성을 추가하지 않았다. FFmpeg 및 글꼴 바이너리는 납품하지 않는다. 공식 FFmpeg 배포 안내는 <https://ffmpeg.org/download.html>, 사용한 Windows 빌드는 <https://www.gyan.dev/ffmpeg/builds/>다.

PowerShell에서 `NODE_PATH`는 Canvas 패키지를 찾을 수 있는 모듈 폴더, `FFMPEG_PATH`는 실행 파일의 위치로 지정한다. `FFPROBE_PATH`는 생략하면 FFmpeg 옆 실행 파일을 사용한다. 글꼴 경로가 다르면 `REEL_FONT_REGULAR` 및 `REEL_FONT_BOLD`를 지정한다. 개인 환경 경로는 저장소 설정에 기록하지 않는다.

```powershell
node design/reels/narrator-v2/source/master-narration.cjs
node design/reels/narrator-v2/source/time-captions.cjs
node design/reels/narrator-v2/source/render-narrator.cjs
node design/reels/narrator-v2/source/export-narrator.cjs
powershell -NoProfile -ExecutionPolicy Bypass -File design/reels/narrator-v2/source/package-narrator.ps1
```

원본 음성이 이미 포함되어 있어 외부 음성 API를 다시 호출할 필요가 없다. `generate-voice.cjs`는 원본이 있으면 덮어쓰지 않는다. 공식 공개 데모는 이용 제한/가용성이 바뀔 수 있으므로 무제한 재생성을 전제하지 않는다. 원본 시험/생성/전사 요청에 계정 자격증명이나 사용자 사진을 전달하지 않았다.

휴대전화 스피커로 실제 청취, Instagram 화면의 자막/커버 자르기, 실제 Play 스토어 등록 상태와 최신 배포 UI 일치는 게시 전에 확인해야 한다. 이번 작업에서는 계정 게시나 광고 집행을 하지 않았다.
