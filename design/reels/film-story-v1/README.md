# Pocket4Cut — Film Story / 30-second Reels

제작 기준: **2026-09-10 / Asia/Seoul**, 시작 커밋 `4527a05`, 앱 1.3 (4). 제작 시작 시 작업 트리는 깨끗했고 앱 소스·설정·아이콘을 변경하지 않았다. 이번 산출물은 `codex/reels-promo`의 별도 홍보 자산이다.

## 납품

- `deliverables/Pocket4Cut-Reels-30s.mp4`: 30초 완성 영상, 한국어 큰 자막 + 오리지널 음악 + 합성 효과음.
- `deliverables/Pocket4Cut-Reels-30s-NoMusic.mp4`: 같은 영상의 효과음 전용 버전.
- `deliverables/Pocket4Cut-Reels-Cover.jpg`: 1080×1920 JPEG 커버.
- `deliverables/INSTAGRAM-CAPTION.txt`, `CAPTIONS-KO.srt`, `UPLOAD-GUIDE.md`: 게시글 초안, 편집용 자막, 업로드 안내.
- `Pocket4Cut-Reels-Package.zip`: 위 납품 폴더 6개 파일을 모은 묶음.
- `verification/encoded-contact-sheet.jpg`: 최종 MP4에서 추출한 8장 장면 확인용. 업로드용 이미지가 아니다.
- `audio/`: 48 kHz stereo PCM16 음악/효과음/합산 WAV 원본 3개.
- `source/`: 재현 가능한 렌더·오디오·검증 소스 및 조사/제작 기록.

핵심 콘셉트는 **“네 컷 찍으러 어디 가? → 내 폰이 네 컷 부스.”**다. 필름 인화지, 앱의 종이색/잉크색/적갈색, 큰 타이포그래피를 사용한다. 새로운 가짜 휴대전화 UI나 타사 네 컷 브랜드 로고를 만들지 않았다. 정지 화면을 그대로 나열하는 파일이 아니라 사진의 이동·크기/회전 변화, 표정 컷 전환, 강조 링, 종이 전환, 자막 등장과 전용 사운드를 시간축에 편집한 MP4다. 인물의 실제 동작을 촬영한 실사 영상은 아니다.

## 구성

1. **0–3초**: “네 컷 찍으러 어디 가?” — 완성 필름과 표정 컷으로 결과 먼저 제시.
2. **3–6초**: “내 폰이 네 컷 부스.” — 실제 컷 수 선택 UI, 4컷을 위한 8장 자동 촬영 안내.
3. **6–10초**: “잘 나온 컷만, 쏙.” — 8장 중 원하는 4장 선택.
4. **10–13초**: “배치도 내 취향.” — 클래식/그리드/가로 선택 화면.
5. **13–16초**: “프레임 색도 나답게.” — 프레임 팔레트와 실제 색/필터 조합 출력 예시.
6. **16–20초**: “같은 사진, 다른 분위기.” — 원본/소프트/필름/흑백 UI와 결과.
7. **20–24초**: “오늘의 우리, 한 장으로.” — 저장/공유 UI.
8. **24–30초**: “다음 네 컷은 지금 여기서.” — 현행 Film Strip v4 아이콘, 앱 이름, Google Play 검색 안내.

선택 순서 전달, 스티커의 최종 출력, 캡션/날짜 위치 등 저장소의 알려진 미해결 차이는 홍보 기능에서 제외했다. 화면을 찍는 카메라 장면이나 저장 버튼의 실제 동작을 검증했다고 표현하지 않는다.

## 출처와 도구

UI는 `design/play-store/print-booth-v1/raw-screenshots`의 프로덕션 Compose 함수 호스트 렌더다. 필름 예시는 같은 패키지 `capture/fixtures`의 CollageRenderer JPEG, 가상 성인 사진은 `demo-photos`의 기존 image_gen 산출물이다. 현행 아이콘은 `design/play-store/film-strip-v4/app-icon-master-1024.png`를 재사용했다. 입력 경로·치수·SHA-256은 `verification/input-manifest.json`에 보관했다. 이번 작업에서 이미지 생성 도구로 새 사진이나 로고를 생성하지 않았다.

음악은 Node.js 기반 FM/노이즈 신시사이저의 절차적 제작물이다. 외부 노래·샘플·MIDI·보이스오버를 사용하지 않았다. `source/AUDIO-NOTES.md`에 제작 방법과 권리/검증 한계를 기록했다. 특정 아티스트의 곡이나 실제 유행 음원이라고 주장하지 않는다.

전용 생성형 영상/음악 도구는 현재 연결되어 있지 않아 기존 프로젝트 자산을 네이티브 Canvas와 FFmpeg로 편집했다. 플러그인을 설치하거나 모델을 전환했다고 주장하지 않는다. 한글은 실행 환경의 맑은 고딕을 래스터 출력했고 폰트 파일은 배포하지 않는다.

FFmpeg는 [공식 다운로드 페이지](https://ffmpeg.org/download.html)가 안내하는 [Gyan Windows builds](https://www.gyan.dev/ffmpeg/builds/)의 9.0.1 essentials 배포를 작업용 `build/`에만 압축 해제했다. 제공자의 SHA-256과 다운로드 파일을 비교했다. ZIP SHA-256: `FEC81AE03971D9DD4BE3EBE02E263BD2EC1D789483F931BDBA5F5715E65DA2E9`. FFmpeg/x264 실행 파일 또는 라이브러리를 납품 ZIP, 앱, Git에 포함하지 않는다. 전역 설치/PATH 변경도 하지 않았다.

## 재생성

Node.js 24와 `@napi-rs/canvas`가 있는 환경에서 저장소 루트에서 실행한다. 플랫폼에 맞는 FFmpeg/ffprobe를 준비하고 `FFMPEG_PATH`를 실제 실행 파일로 설정한다. `FFPROBE_PATH`는 생략하면 FFmpeg와 같은 폴더를 사용한다. 한글 글꼴이 기본 위치에 없으면 `REEL_FONT_REGULAR`와 `REEL_FONT_BOLD`를 지정한다. 앱에 새 의존성을 추가하지 않는다.

```powershell
node --check design/reels/film-story-v1/source/make-audio.cjs
node --check design/reels/film-story-v1/source/render-reel.cjs
node --check design/reels/film-story-v1/source/export-reel.cjs
node design/reels/film-story-v1/source/make-audio.cjs
node design/reels/film-story-v1/source/render-reel.cjs
node design/reels/film-story-v1/source/export-reel.cjs
./design/reels/film-story-v1/source/package-reel.ps1
```

`render-reel.cjs --stills`는 시안/커버만 만든다. 전체 명령은 기존의 이 작업 출력 파일을 다시 생성하므로 수동 편집본이 있다면 먼저 별도로 보존한다. 동영상 인코딩용 중간 파일은 Git 무시 대상인 `build/reels-render`에 있다. `canvas.data()`의 native backing이 바뀌는 문제를 피하도록 매 프레임을 `Buffer.from()`으로 소유 복사하고 pipe backpressure를 처리한다. 장면 모아보기도 live Canvas 대신 인코딩된 스냅샷을 읽는다.

## 검증 범위

`verification/export-validation.json`이 두 MP4의 실제 규격·SHA-256·크기·오디오 측정값 및 전체 디코딩 검증 결과다. `render-manifest.json`은 8장면/900프레임 구성이고, `text-safe-area-checks.json`은 핵심 글자의 사용자 정의 안전 영역 검사다. 이 영역을 Instagram의 모든 기기에 동일하게 적용되는 공식 pixel 규격으로 표현하지 않는다.

원본 시안과 최종 인코딩에서 뽑은 장면, 시작/마지막 프레임, 커버의 시각 검토는 파일 수준 검수다. 실제 휴대전화 청취/재생, Instagram 업로드 자르기와 심사, 앱의 실기기 E2E 검증은 수행하지 않았다. 앱 변경이 없으므로 이번 영상 제작을 이유로 Gradle 빌드·단위 테스트·Lint를 재실행하지 않았다. 기존 Lint/제품 문제를 해결했다고 주장하지 않는다.
