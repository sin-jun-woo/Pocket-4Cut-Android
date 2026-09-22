# Pocket4Cut · 사계절 프레임 릴스

마지막 완성본인 [Reference v3](../reference-v3/README.md)의 종이색·인화사진·자막·전환·남성 내레이션 중심 구성을 유지하고, 내용을 새 봄·여름·가을·겨울 프레임 소개로 교체했습니다. **23초 / 1080×1920 / 30 fps**, 음악·효과음은 없습니다.

2026-09-22 (Asia/Seoul), 시작 기준 `6d0c151`, `codex/seasonal-frames`, 시작 작업 트리 clean. 앱 코드·런타임 자산·기존 릴스는 수정하지 않았습니다.

## 다운로드

- [업로드용 영상 MP4](deliverables/Pocket4Cut-Reels-Seasons-Voice.mp4)
- [전체 전달 패키지 ZIP](Pocket4Cut-Seasons-Reels-Package.zip)
- [커버 JPG](deliverables/Pocket4Cut-Seasons-Cover.jpg)
- [내레이션 MP3](deliverables/Pocket4Cut-Seasons-Narration.mp3)
- [화면 자막 SRT](deliverables/CAPTIONS-KO.srt), [대본](deliverables/NARRATION-KO.txt), [게시글](deliverables/INSTAGRAM-CAPTION.txt), [업로드 안내](deliverables/UPLOAD-GUIDE.md)

ZIP에는 위 영상·커버·음성·자막·대본·게시글·안내의 7개 파일만 포함합니다. 원본 참조 영상·음성·대사, 실행 도구, 모델, 폰트 파일은 포함하지 않습니다.

## 내용과 규격

같은 사진의 계절별 변화 → 봄·여름·가을·겨울 → 2·4·6컷과 배치 선택 → 저장·공유 → 브랜드 검색 안내로 이어지는 14장면입니다. 배치 소개는 대표 3종을 보여 주며, 실제 지원하는 8배치를 자막으로 안내합니다. 네 계절과 여덟 배치의 조합은 32종입니다.

- 본편: 23.000초, 690프레임, H.264 High / yuv420p / BT.709 / faststart, AAC 48 kHz stereo, 9,984,514 bytes. 최종 음량 −16.0 LUFS / −2.4 dBTP.
- 커버: 1080×1920 JPEG, “사계절을 / 네 컷에 담아요”.
- 내레이션: 23초 / 48 kHz / stereo MP3, 192 kbps 설정. 원본은 20.960초 / 24 kHz / mono / 32-bit float WAV, 마스터는 23초 / 48 kHz / stereo / PCM24 WAV입니다. 배속·발화 잘라내기는 하지 않았습니다.
- 이미지: 실제 Android CollageRenderer의 새 사계절 JPEG, 기존 가상 성인 사진·앱 아이콘·결과 화면의 저장/공유 영역을 재사용했습니다. 새 사진·일러스트 생성이나 앱 화면 변경은 없습니다. 편집된 홍보 영상이며 실제 기기의 연속 녹화가 아닙니다.

기존 영상에서 제거했던 설명 오버레이는 다시 넣지 않았습니다. 친근한 존댓말과 기존 참조 음색·로컬 모델·설정을 재사용했으며, 음색이 완전히 동일하다는 보증은 아닙니다. 자세한 제작 판단과 출처는 [CREATIVE-BRIEF](source/CREATIVE-BRIEF.md), [VOICE-NOTES](source/VOICE-NOTES.md), [입력 이미지 목록·해시](visual-stills/input-manifest.json)를 참조하세요.

## 이번에 실행한 검증

- Node 문법 검사와 자막/타이밍 회귀 테스트 **10/10 통과**.
- 최종 음성을 정답 프롬프트 없이 로컬 Whisper로 새로 전사했습니다. 공백·문장부호·명시한 개수 표기만 정규화한 **136자 일치, 편집 거리 0**이며 실제 토큰 타이밍으로 14장면을 배치했습니다.
- 첫 음성의 두 표현은 전사 결과가 달라 렌더를 차단했습니다. 진단 후 두 문장을 간결하게 고쳐 같은 참조·설정으로 새로 생성했습니다. 실패한 전사를 정답으로 바꾸지 않았으며, 첫 시도 자료는 ignored 작업 폴더에 보존했습니다. [시도별 기록](verification/narration-attempts.json).
- 장면별 두 시점의 스틸 **28장**, 주요 텍스트 경계 **41건**, 전체 프레임 배치 경계 **12건**을 검사했습니다. 계절 장식, 2·4·6 사진창 수, 저장→공유의 겨울 프레임/사진 순서, 커버·검색 안내를 시각 검수했습니다. 배경 인화지의 의도적인 화면 밖 배치는 주요 프레임 누락과 구분했습니다.
- 최종 MP4 전체 영상·음성 디코딩, 규격, A/V 길이 일치, faststart, 음량, 예상치 못한 검은 구간 검사를 통과했습니다. 최종 인코딩 파일에서 추출한 장면 모아보기·계절 확대 장면도 확인했습니다. [최종 영상 검증](verification/export-validation.json), [인코딩 장면 모아보기](verification/encoded-contact-sheet.jpg).
- ZIP을 다시 열어 **7개 항목 전부 원본 SHA-256과 일치**함을 확인했습니다. ZIP 크기는 10,939,270 bytes입니다. 실제 패키지 해시·실행 결과는 [패키지 검증](verification/package-validation.json)이 기준입니다.

본편 SHA-256: `4583fc67fbf61dd9fe2f65c393b14e8f67864daf8f225ef94eb659d6cfa96dfb`.

앱 코드 변경이 없어 Android 빌드·단위·계측 테스트는 이번에 재실행하지 않았습니다. 자동 전사/음량 검사는 자연스러움·발음·음색 동일성의 주관적인 청취 검사를 대신하지 않습니다. 게시 전에 휴대전화 청취, 커버 자르기, Instagram 화면 겹침·압축, 실제 배포 앱의 새 프레임 및 스토어 검색 상태를 확인해야 합니다. Instagram 게시·광고 집행·Play 배포는 수행하지 않았습니다.

## 재현

저장소 루트에서 실행합니다. 기존 Node 환경에 `@napi-rs/canvas`, Python 환경에 Qwen3-TTS, 로컬 FFmpeg/ffprobe와 whisper.cpp 모델이 필요합니다. 소스의 기본값은 기존 ignored `build/reels-tools/` 경로를 사용합니다. 새 패키지/모델 설치를 이 제작 과정에서 수행하지 않았습니다. 참조 파일은 저장소에 포함하지 않으므로 새 checkout에서 음성을 재생성하려면 사용 권한이 있는 참조와 모델을 별도로 준비해야 합니다. 보존된 최종 WAV로는 다음 후처리부터 실행할 수 있습니다.

```powershell
node --test design/reels/seasonal-v4/source/test-time-captions.cjs
node design/reels/seasonal-v4/source/master-narration.cjs
node design/reels/seasonal-v4/source/transcribe-narration.cjs
node design/reels/seasonal-v4/source/time-captions.cjs
node design/reels/seasonal-v4/source/render-seasons.cjs --stills
node design/reels/seasonal-v4/source/render-seasons.cjs
node design/reels/seasonal-v4/source/export-seasons.cjs
powershell -NoProfile -ExecutionPolicy Bypass -File design/reels/seasonal-v4/source/package-seasons.ps1
```

`NODE_PATH`는 준비한 Node 라이브러리 폴더에, `FFMPEG_PATH`/`FFPROBE_PATH`는 사용할 실행 파일에 맞춥니다. 후처리/렌더는 이 폴더의 제작물을 갱신하므로 수정 전 보관본을 확보하세요. 음성 생성 스크립트는 기존 완료 WAV를 임의로 덮어쓰지 않습니다. 실제 ASR 불일치 또는 대본·음성·렌더 해시 불일치 시 후속 생성을 중단하는 검사를 유지합니다.
