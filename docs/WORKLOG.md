# Pocket 4Cut 작업 로그

작업 단위로 최신 기록을 위에 추가한다. 날짜는 별도 표기가 없으면 Asia/Seoul(KST) 기준이다. 실제 수행 결과와 향후 계획을 구분하고, 실패·캐시 재사용·미실행을 성공으로 합치지 않는다.

`docs/`는 GitHub Pages 배포 대상이므로 공개 가능한 요약만 기록한다. 비밀값, 사용자 사진, 기기 serial, 개인 로컬 경로, 원시 실행 로그를 넣지 않는다. 세부 실행 산출물은 로컬 build/캐시 영역에 두고 필요한 명령·결과만 남긴다.

## 2026-09-10 — 참조 음색과 자체 존댓말 대본의 릴스 v3 (Asia/Seoul)

### 요청과 변경 범위

- 요청: 사용자 제공 영상의 AI 음색을 참조하고, 원본의 이야기·반말을 따르지 않는 포켓네컷 자체 앱 소개 광고를 제작. 음악을 넣지 않고 참조 계정/출처 크레딧을 광고에 삽입하지 않는다. 추출 음성과 새 대본의 Qwen/Hugging Face 전송은 사용자에게 별도 명시 승인을 받았다.
- 기준: `codex/reels-promo`, HEAD `f79b2fd389fdb56a0a39b21ca5223f5f0bc0979b`, 시작 시 미커밋/스테이징 변경 없음. 신규 `design/reels/reference-v3/`, 이 로그와 자산 가이드만 작업 범위다. 기존 v1/v2, 앱 소스·Manifest·Gradle·아이콘·원본 이미지와 사용자 제공 영상은 보존한다.
- 대본: “내 폰이 네 컷 사진관이 된다면요?” → 앱 이름 → 4컷/8장 촬영 → 4장 선택 → 배치/필터/흑백 → 저장/공유 → 사용할 상황 → 브랜드 검색. 친근한 존댓말 12문장으로 구성하고 실제 후기를 가장하지 않는다.
- 화면: 기존 Compose 호스트 렌더, CollageRenderer 결과, Film Strip v4 아이콘, 가상 성인 생성 사진을 재사용한다. 새 사진·아이콘을 생성하거나 앱 기능을 변경하지 않는다. 광고·AI 사용 예시 표시는 출처 크레딧과 별개로 유지한다.

### 음성 경로와 보존 원칙

- 공식 공개 ASR/TTS는 승인된 각 1회 요청에서 상세 원인 없는 `error:null`을 반환했다. 원인을 할당량·계정 문제로 단정하거나 성공으로 처리하지 않았다. 동일 요청 반복이나 다른 기본 음색으로 대체하지 않고 로컬 ASR·참조 조건 음성 생성으로 전환했다.
- 공식 whisper.cpp b4938와 다국어 small-q5_1로 참조 음성을 로컬 전사했다. 6초 참조의 철자 일부는 영상 자막과 대조했다. 사용자 원본 영상, 참조 클립, 원본 전체 전사는 ignored `build/`에만 보관하며 Git·납품 ZIP에 포함하지 않는다. 실제로 듣고 참조 음색을 검수했다는 주장은 하지 않는다.
- 공식 Qwen3-TTS 1.7B Base와 토크나이저를 pinned revision으로 내려받아 두 대형 가중치의 공개 SHA-256 일치를 확인했다. 모델과 Python 패키지는 ignored `build/`의 독립 환경에 설치했다. 앱/공유 런타임/시스템 PATH를 변경하지 않으며 계정 자격증명·유료 API를 사용하지 않는다.

### 검증 기록

- 타이밍 도구의 합성 fixture 9개 테스트와 PowerShell 패키지 스크립트 구문 검사를 통과했다. 이는 실제 새 음성의 대본 일치 또는 음색 평가가 아니다.
- JavaScript 7개 `node --check`, Python 생성 스크립트 문법 검사 통과. 공개 자료에서 개인 원본 파일명·참조 계정 URL·자격증명 패턴을 검색해 발견하지 못했다.
- 실제 새 음성을 로컬 CPU에서 1회 생성했다. 20.160초/24 kHz/mono/float32 WAV, 생성 209.125초, 전체 220.281초. 모델에 실제 6초 참조와 참조 대본을 함께 전달했으며 배속·발화 삭제·음악·효과음 추가는 없다.
- 로컬 Whisper ASR + DTW는 정답 프롬프트 없이 성공했다. 숫자·공백·구두점 정규화 후 136문자가 새 대본과 일치했다(숫자를 한글로 풀면 137음절). 원문과 토큰 시각은 검증 자료에 보존했다. 마지막 ASR 끝 시각이 원본보다 20 ms 늦은 한계도 기록했다.
- `master-narration.cjs`, `time-captions.cjs`, `render-reference.cjs`, `export-reference.cjs` 실제 실행. 새 음성에 0.12초 시작 여유와 후반 검색 여유를 더해 23초/48 kHz/stereo PCM24로 정리하고 14개 연속 편집 구간을 생성했다. 초안 스틸은 ignored 영역으로 이동해 보존하고 최종 구간 스틸과 혼합하지 않았다.
- 인코딩 결과는 1080×1920, 30 fps, 690프레임, 23초, H.264 High/yuv420p/BT.709, AAC stereo 48 kHz. 전체 디코딩·길이 동기·faststart 검사 통과, 검은 구간 0, 최종 AAC −16.0 LUFS / −3.4 dBTP. 음성 원본·ASR·마스터·story·visual의 해시를 연결해 이전 출력의 혼입/잘림을 차단했다.
- 28개 장면 시점과 핵심 텍스트 경계 70개 검사. 최종 MP4에서 추출한 14장면 모아보기/시작/끝과 커버를 실제 시각 검토했다. 흑백 결과가 저장/공유까지 이어지는지 확인했고, CTA 상단 광고 표기와 필름 인쇄 문자의 겹침은 헤더 배경을 정리한 뒤 재출력했다. 자동 보고서의 프레임 추출과 실제 시각 검수도 구분했다.
- 앱 변경이 없어 Android 빌드·단위 테스트·Lint·기기 E2E는 재실행하지 않았다. 기존 앱 문제의 해결이나 Play/Instagram 실제 배포를 주장하지 않는다.

### 산출물과 남은 확인

- 납품: 23초 본편 MP4, 동일 음성 MP3, 1080×1920 JPEG 커버, 화면 자막 SRT, 새 대본, 게시글, 업로드 안내와 ZIP. 원본 새 음성, 제작 스크립트, 출처/라이선스·한계 문서, 해시/검증 자료는 별도 제작 폴더에 보존한다. 참조 원본은 포함하지 않는다.
- `package-reference.ps1` 실행 완료: ZIP 6,834,141 bytes, 7개 항목 모두 원본 SHA-256 일치. 최종 MP4 SHA-256은 `21d42e7eb026b001d77572cf345329ce369d5fa6b2ec615a89cb7595622bf2f4`다.
- 직접 청취와 독립 음색 비교는 수행하지 못했다. 참조 조건 적용과 자동 전사 일치는 원본 음색의 완전한 동일성·자연스러움 보증이 아니다. 휴대전화 청취와 실제 Instagram UI/커버 자르기, 스토어 검색/설치 가능 여부는 게시 전 확인해야 한다.
- 완료 커밋은 `feat: add reference-voice Pocket4Cut reel`로 식별한다. SHA를 문서 자체에 넣어 amend하지 않으며, 일반 push 이후 원격 SHA/작업 트리 상태를 최종 응답에서 보고한다.

## 2026-09-10 — 음악 없는 남성 나레이션 릴스 추가 (Asia/Seoul)

### 요청과 변경 범위

- 요청: 기존 영상과 별개로 “저 ~했어요” 계열의 남성 나레이션을 넣고, 음악을 빼고, 최근 릴스 광고 표현을 조사해 한 편 더 제작.
- 기준: 시작/작업 브랜치 `codex/reels-promo`, HEAD `c33b83a68a192a8aab8dde4f90b78c2e3357146e`, 기존 미커밋 변경 없음. `design/reels/narrator-v2/` 및 이 로그/자산 가이드만 변경했다. 기존 `film-story-v1`과 앱 소스·Manifest·Gradle·아이콘은 보존했다.
- 조사: 곰랩 2026-03-03의 국내 남성 AI 일상 나레이션 설명, Later 2026-09-04 및 New Engen 본문 2026-09-01의 상황 설정/반전/1인칭 표현, Meta Blueprint 제작 기본기를 구분해 참고했다. 한국 전체의 실시간 유행 순위나 전환 성과를 확인했다는 주장은 하지 않는다.
- 제작: “저, 네 컷 만들었어요. 근데 사진관은 안 갔어요.”를 도입으로, 실제 음성에 맞춘 15개 편집 구간에 결과·사용법·흑백 감상·저장/공유·친구와의 추억·검색 안내를 배치했다. 기존 앱 코드 기반 렌더와 가상 성인 생성 사진을 재사용했다. 실제 사용자 후기나 실기기 녹화가 아니며 영상/커버/게시글에 광고·생성 예시 고지를 넣었다.
- 음성: 전용 음성 제작 수단을 조사한 뒤 Qwen 공식 공개 VoiceDesign 데모로 참조 음성 없는 원본 한국어 남성 음색을 지정해 생성했다. 짧은 시험 1회, 본편 1회만 요청했으며 별도 유료 API/자격증명/플러그인 설치/음성 복제는 사용하지 않았다. 음악과 효과음은 없다.
- 산출물: 30초 세로 MP4, 30초 음성 MP3, 1080×1920 JPEG 커버, SRT, 대본, 게시글, 업로드 안내 및 ZIP. 원본 WAV, 음색 지시, 재출력 코드, 공식 생성/전사 출처와 검증 자료도 별도 폴더에 보존했다.

### 실제 실행한 검증과 보완

- 공식 Qwen3-ASR/ForcedAligner로 생성 원본을 전사했다. 공백/문장부호를 제외한 대본의 119개 한글 음절이 모두 일치했고 음성 구간 13개를 추출했다. 원본 25.953750초를 배속 없이 처리하고 시작 0.15초 여유/끝 무음을 더했다. 발화 시각에 맞춰 화면 자막을 구성했다.
- `node --check` JavaScript 5개 통과, PowerShell Parser 1개 오류 0. `master-narration.cjs`, `time-captions.cjs`, `render-narrator.cjs`, `export-narrator.cjs` 실행 완료. 공개 음성 생성 스크립트도 본편 생성에 실제 사용했다.
- 독립 검토에서 흑백 다음 저장 화면이 컬러로 되돌아가는 연속성 문제와 커버의 생성 예시 고지 누락을 발견해 수정하고 재출력했다. “흑백” 대사에는 흑백 결과를, 이어지는 저장/공유에도 같은 결과를 유지한다. 기존 필터 UI의 원본 선택 상태가 흑백 결과와 함께 보이지 않도록 편집했다.
- 최종 영상 1080×1920, 30 fps, 900프레임, 정확히 30초, H.264 High/yuv420p/BT.709, AAC stereo 48 kHz 확인. 전체 영상/음성 디코딩 오류 0, 길이 동기/faststart 통과, 예상치 못한 검은 구간 0. 최종 AAC −15.8 LUFS / −2.7 dBTP로 측정했다.
- 30개 시점의 장면 스틸과 핵심 텍스트 경계 123개 검사. 최종 MP4에서 15장면/첫 프레임/끝 프레임을 추출했고, 최종 장면 모아보기·흑백 저장 결과·커버를 시각 검토했다. 원본 자산 경로/치수/SHA-256을 기록했다.
- `package-narrator.ps1`: 납품 파일 7개, ZIP 7,298,978 bytes. 모든 ZIP 항목의 SHA-256이 원본과 일치했다. MP4 SHA-256은 `360731d52fa0f7adc6b7c968c7a82a8ee461d3d5f4410fcc7a96f8e2fbd8bcfd`다.
- 앱 변경이 없어 Gradle 빌드/앱 단위 테스트/Lint는 재실행하지 않았다. 기존 앱 문제나 이전 Lint 결과를 해결했다고 주장하지 않는다.

### 남은 확인

- 파일/자동 전사 검증과 주관적 음성 청취는 다르다. 이번 환경에서는 음색·억양·자연스러움을 실제로 듣고 평가하지 못했다. 제공한 MP4/MP3를 휴대전화에서 재생해 확인해야 한다.
- Instagram의 실제 UI 겹침/커버 자르기, 스토어 검색·설치 가능 여부와 최신 배포 UI 일치는 게시 전에 확인해야 한다. 계정 게시·광고 집행·심사 확인은 수행하지 않았다. 생성 음성의 모델 라이선스를 생성물 독점권이나 자동 광고 권리 보증으로 확대하지 않는다.

## 2026-09-10 — 30초 Instagram 릴스 홍보 영상 (Asia/Seoul)

### 요청과 변경 범위

- 요청: 현재 릴스 트렌드를 조사하고 Pocket4Cut의 사용법과 장점을 소개하는 약 30초 홍보 영상을 제작.
- 기준: 시작 `main`, HEAD `4527a05`, 미커밋/스테이징 변경 없음. `codex/reels-promo`에서 별도 홍보 자산을 작성했다. 앱 1.3 (4)의 소스·Manifest·Gradle·런처 리소스는 변경하지 않았다.
- 조사: 2026-09-04 Later 트렌드, 2026-09-01 New Engen, Meta Blueprint의 세로 영상/초반 관심 유도/소리/안전 영역 안내를 확인했다. 한국 전체의 실시간 인기 순위나 광고 전환 보장으로 해석하지 않는다. 참고 링크와 편집 판단은 제작물의 `source/CREATIVE-BRIEF.md`에 기록했다.
- 제작: “네 컷 찍으러 어디 가? → 내 폰이 네 컷 부스.” 콘셉트로 결과 → 4컷/8장 촬영 안내 → 4장 선택 → 레이아웃/색/필터 → 저장/공유 → 검색 CTA의 8장면을 구성했다. 실제 Compose 호스트 렌더·CollageRenderer 결과·현행 Film Strip v4 아이콘과 기존 가상 성인 생성 사진을 재사용했다. 실기기 녹화 또는 고객 후기라고 표현하지 않는다.
- 산출물: `design/reels/film-story-v1/`에 음악 포함 MP4, 효과음 전용 MP4, 1080×1920 JPEG 커버, 게시글, SRT, 업로드 안내, 원본 WAV 3개, 제작/검증 코드와 기록. 앱 기능이 아닌 별도 마케팅 자산이다. 이 섹션과 `IMAGE_ASSET_GUIDE.md`에 새 자산을 문서화했다.
- 현재 사용 가능한 전용 영상 생성 도구가 없어 native Canvas/FFmpeg로 시간축 모션 편집했다. 외부 곡·음성·샘플 없이 120 BPM의 음악과 셔터/종이 효과음을 절차적으로 제작했다. 플러그인 설치·전역 PATH 수정·모델 전환·계정 게시·광고 집행은 하지 않았다.

### 실행한 검증과 발견/보완

- 오디오/렌더/내보내기 JavaScript 구문 검사 및 실행. WAV 3개는 48 kHz stereo PCM16, 30.000초, 믹스 sample peak −3.100 dBFS, clipping 0. MP4 AAC 변환 후 완성본 −15.3 LUFS / −2.1 dBTP, 효과음 전용본 −42.0 LUFS / −20.6 dBTP를 측정했다.
- `render-reel.cjs`: 1080×1920, 30 fps, 900프레임 출력. 핵심 한글 자막의 사용자 정의 여백 검사. 최초 시안의 글자/사진 겹침과 색 선택 crop을 수정한 뒤 8장면/커버를 재검토했다.
- 독립 read-only 검토에서 native `canvas.data()` 버퍼 공유를 재현했다. 실제 인코더 write가 큐에 남았을 때의 위험을 없애도록 `Buffer.from()` 소유 복사 및 backpressure 대기를 적용했다. 장면 모아보기는 live Canvas 참조 대신 immutable 이미지 스냅샷을 쓴다.
- 최초 MP4의 BT.709 transfer/primaries 태그 누락을 자동 검사에서 발견했다. 검사 조건을 낮추지 않고 scale 뒤 `setparams`를 적용해 재출력했다. 최종 RGB 디코딩 배경 `[200, 59, 44]`는 원본 `[200, 61, 45]` 대비 허용 오차 안이다. JPEG 검증 스냅샷은 BT.709→BT.601 변환으로 일반 JPEG 뷰어의 색 해석에 맞췄다.
- `export-reel.cjs`: 두 MP4 각각 H.264 High/yuv420p/BT.709, 1080×1920, 900프레임, 30 fps, 영상/오디오/컨테이너 30초, AAC stereo 48 kHz 확인. faststart 통과, 전체 영상·오디오 디코딩 오류 0, 예상치 못한 검은 구간 0. 최종 인코딩 8장면/첫 프레임/마지막 프레임도 추출하여 검토했다.
- `package-reel.ps1`: 정확한 납품 파일 6개를 8,357,126 bytes ZIP에 포함했다. 각 항목을 압축 해제 없이 읽어 원본 SHA-256과 비교해 6개 모두 일치했다. PowerShell Parser 구문 오류 0, JavaScript 3개 구문 검사 통과, 변경 문서의 `git diff --check` 통과.
- 앱을 변경하지 않아 Gradle 빌드/앱 단위 테스트/Lint는 이번에 재실행하지 않았다. 이전 `PermissionImpliesUnsupportedChromeOsHardware` Lint 실패와 기존 제품 문제는 해결 범위가 아니다.

### 남은 확인

- 실제 휴대전화 스피커 청취·연속 재생, Instagram의 우측/하단 UI와 커버 썸네일 자르기, 실제 스토어 노출과 배포 화면 일치를 게시 전에 확인해야 한다. 수치 검증을 청취·실기기 촬영·업로드 심사 통과로 과장하지 않는다.
- 생성 예시 고지를 유지한다. 유행 음악으로 교체하면 실제 브랜드 홍보/광고 사용 범위의 권리를 별도로 확인해야 한다. 음악의 독자 제작은 자동 권리 판정/법적 독점성의 보증이 아니다.
- 앱에서 미해결로 기록된 재정렬/스티커 출력/캡션 위치 등은 홍보 기능에서 제외했다. 프레임 색과 필터 예시는 조합된 스타일링이며 단일 변수 A/B 테스트가 아니다.

## 2026-09-10 — 미커밋 전체 작업 통합 및 main 반영 (Asia/Seoul)

### 요청과 반영 범위

- 요청: GitHub에 반영하지 않은 작업 전부의 내용을 정리하여 커밋하고 `main`으로 push. 사용자가 기존 미커밋 작업 전체의 포함을 명시적으로 승인했다.
- 기준: 시작 브랜치 `codex/setup-project-guidance`, HEAD `317b711`; fetch 후 `origin/main`은 `95140a0`이며 기존 개발 지침·Java 안내 커밋 2개가 앞서 있었다. 사용자 이력을 재작성하지 않는 일반 커밋과 fast-forward 통합을 사용한다.
- 앱 버전은 현재 실제 설정인 **1.3 (versionCode 4)**를 반영한다. 초기 조사와 이전 출시 초안의 1.2 표기는 역사적 기록으로 구분했다.
- UI: Compose 화면 16개·디자인 시스템 10개를 종이색/잉크색, 계절 강조색, 간격·글꼴·작은 모서리로 정리했다. 홈·컷 수·프레임 선택·배치·보관함 구성을 갱신하고 결과 저장/공유를 가로 배치했다. 시작 지연을 1.15초로 줄이고 결과 액션의 지연·confetti를 제거한 기존 로컬 작업을 포함한다.
- 브랜드/스토어: Film Strip v4 런처 PNG·adaptive/monochrome XML·브랜드 PNG/SVG, Play 아이콘 **512×512 RGBA**, 피처 그래픽 **1024×500 RGB**, 기존 휴대전화 소개 이미지 **1080×1920 8장**을 포함한다. `design/`의 최종 원본, 프롬프트, 마스크 검토, 제작 스크립트, 가상 성인 사진 fixture, 미채택 초안·이전 아이콘 백업·ZIP도 제작 이력으로 함께 보존한다. 최종본과 미채택 자료는 각 폴더 README로 구분한다. 이번 Git 통합에서 새 이미지를 생성하지 않았다.
- 문서: 프로젝트 인수 분석과 파일 지도·알려진 문제·검증 기록, UI/자산 작업 로그, 이미지 가이드, 기존 출시 준비 초안 및 실제 아키텍처의 현재 반영 상태를 포함한다.
- 검증 도구: ADB smoke 스크립트와 명시적으로 실행할 때만 사용하는 Paparazzi init script/fixture를 포함한다. 공개 전에 ADB 스크립트의 개인 기기 serial 기본값과 로컬 경로를 제거하고, 세션 JSON·사진 파일 목록 출력은 개수/읽기 가능 여부로 바꿨다. 검증 문서의 개인 설치 경로는 환경 변수 기반 재실행 예시로 일반화했다.
- `.gitignore`에 Kotlin 컴파일러의 로컬 세션 캐시 `/.kotlin/`을 추가했다. 기존 규칙으로 제외된 서명키·서명 암호 파일·SDK 로컬 설정·빌드 캐시는 포함하지 않는다. 자격증명 패턴 검사에서 발견한 Gradle 항목은 속성 조회 코드였고 실제 비밀값은 아니었다.

### 실행한 검증

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --continue --offline --console=plain
```

- 저장된 사용자 JBR 21.0.10 및 기존 Gradle 캐시로 실행했다. 전체 52개 태스크 중 **17개 실행 / 35개 UP-TO-DATE**, 소요 2분 12초. 결합 명령의 종료 코드는 Lint 실패로 **1**이다.
- `assembleDebug`: **성공**. 리소스 처리와 APK 패키징을 실행했으며 출력 metadata의 버전 **1.3 (4)**가 현재 설정과 일치한다. Kotlin 앱 컴파일 등은 캐시를 재사용했다.
- `testDebugUnitTest`: **실제 실행 성공**, 1개 / 실패 0 / 오류 0 / 건너뜀 0. XML 시각은 `2026-09-09T16:46:08.589Z`. 기본 산술 테스트이므로 제품 기능 커버리지를 의미하지 않는다.
- `lintDebug`: **실패**, Error 1 / Warning 91 / Hint 2. 기존 `AndroidManifest.xml:6`의 `PermissionImpliesUnsupportedChromeOsHardware`가 원인이다. CAMERA 권한에 대응하는 hardware feature 선언 문제이며, 이번 통합에서는 Manifest 변경이나 오류 억제를 하지 않았다.
- JavaScript 제작 도구 6개는 `node --check` 구문 검사 통과. ADB smoke 스크립트는 PowerShell Parser 구문 오류 0개. 실제 ADB·촬영·앱 설치·기기 데이터 변경이나 새 Paparazzi 렌더는 실행하지 않았다.
- `design/`은 203개 파일 / 54,869,311 bytes이며 100 MB 초과 파일이 없다. 변경된 파일 목록과 staged diff를 검토하고 Git에서 추적할 대상만 정확한 경로 목록으로 stage한다.
- staged 공백 검사에서 출시 초안의 Markdown 줄바꿈은 동일한 의미의 명시적 줄바꿈으로 정리했다. 보관된 이전 브랜드 SVG의 마지막 공백/빈 줄은 기존 원본 보존을 위해 유지했다. 전체 `git diff --cached --check`는 이 백업 파일 한 개 때문에 종료 코드 2이며, 해당 파일을 제외한 나머지 변경은 공백 검사를 통과했다. 디자인 ZIP 3개의 44개 항목은 대응 원본과 SHA-256이 일치한다.

### 남은 확인과 배포 경계

- 기존 선택 순서 전달·캡션 위치·초안 저장·복원 문제는 [알려진 문제](../engineering/KNOWN_ISSUES.md)에 남아 있다. 이번 UI/자산 통합을 이 결함들의 해결로 간주하지 않는다.
- 실제 기기의 카메라/저장/공유, 다양한 화면·접근성·런처 마스크는 추가 확인 대상이다. 좌표 기반 ADB smoke는 ADB 실패·시간초과·결과 미생성을 모두 실패로 판정하지 않으므로 성공 표시만으로 제품 E2E 통과를 주장할 수 없다. Paparazzi export도 전체 화면 수 검증을 보강할 여지가 있다.
- `main`의 `docs/**` 변경은 저장소의 GitHub Pages 워크플로 대상이다. Git push 성공, Pages 배포 성공, Play Store 게시/심사는 별개이며 스토어 업로드는 이 작업에 포함하지 않는다. 최종 커밋 SHA·원격 반영 결과는 완료 보고와 Git 이력에서 확인한다.

## 2026-09-10 — Windows JAVA_HOME 오류 해결 (Asia/Seoul)

- 요청: 터미널의 `JAVA_HOME is not set` 및 Java PATH 미발견 오류 해결.
- 기준: `codex/setup-project-guidance`, HEAD `ac42903` 및 기존 미커밋 UI·버전·이미지·문서 변경이 있는 작업 트리. 기존 변경은 보존하고 이번 안내와 로그 추가분만 커밋 대상으로 구분했다.
- 확인: Android Studio 번들 JBR 21.0.10의 `java`와 `javac`는 직접 실행 가능했지만, 기존 터미널 환경에서 Java를 찾지 못했다. IDE의 로컬 Gradle JDK 지정과 wrapper의 `JAVA_HOME`/`PATH` 조회가 별개임을 확인했다.
- 조치: 이 PC의 사용자 환경 변수 `JAVA_HOME`을 확인된 JBR로 영구 등록하고, 기존 사용자 `Path` 항목을 보존하며 JDK `bin`을 추가했다. Windows에 환경 변경 알림을 보냈다. 앱 소스·Gradle 설정·이미지 에셋은 수정하지 않았다.
- 변경 파일: `QUICK_START.md`에 설정·터미널 재시작·현재 PowerShell 갱신 방법을 추가하고, `docs/WORKLOG.md`에 이번 기록을 추가했다. OS 환경 변수 자체는 Git 커밋에 포함되지 않는다.
- 검증: 저장된 사용자 환경 변수를 별도 PowerShell 실행에서 다시 읽고, `PATH`의 JDK 항목이 한 개이며 `java`가 해당 JDK로 해석되는 것을 확인했다. `java -version`과 `javac -version`은 21.0.10, `./gradlew.bat --version --console=plain`은 Gradle 8.13 / Launcher JVM 21.0.10으로 성공했다.
- `./gradlew.bat :app:assembleDebug --offline --console=plain`: **성공**, 36개 태스크 중 8개 실행 / 28개 UP-TO-DATE. 기존 사용자 Gradle 캐시를 이번 검증 프로세스에서 지정해 사용했다. 완전한 재컴파일이나 새 기능 테스트로 간주하지 않는다.
- 범위/잔여 확인: Java 실행 환경 수정이므로 단위 테스트·Lint·실기기 검사는 재실행하지 않았다. 이전 CAMERA Lint 오류를 수정한 작업이 아니다. 이미 실행 중인 터미널/IDE는 재시작하거나 문서의 PowerShell 갱신 명령을 적용해야 한다.

## 2026-09-10 — Film Strip v4 아이콘 적용·피처 그래픽 제작 (Asia/Seoul)

- 기준: `codex/setup-project-guidance`, HEAD `ac42903`과 기존 미커밋 UI/버전 변경을 포함한 작업 트리. 사용자는 앞선 웹 조사 방향의 이미지 제작·새 아이콘 적용·Play 아이콘/그래픽 파일을 요청했고, 작업 중 인화지 하단에 소문자 `pocket4cut` 추가를 요청했다.
- 내장 image_gen으로 가상의 성인 두 사람이 담긴 네 컷 인화지, 좁은 필름 가장자리와 빨간 표식을 제작했다. 최초 투명 요청은 RGB 체크무늬로 출력되어 검사에서 제외하고 도구로 차콜 배경으로 수정했다. 아이콘과 피처 그래픽 모두 후속 도구 편집으로 하단 `pocket4cut`을 넣었다. 프롬프트와 미채택 원본은 `design/branding/film-strip-v4/source/`에 구분 보관했다. 사용자 사진·타사 이미지 파일을 가져오지 않았다.
- 실제 적용: adaptive 일반/원형 XML의 foreground를 새 density PNG로 전환, 배경차콜, 호환 전경alias, 단색벡터, density PNG15개, 브랜드PNG/SVG wrapper 갱신. 사진 표현을 위한 래스터 전환이며 앱 UI/Manifest/Gradle/촬영·콜라주 코드는 수정하지 않았다. 이전 v1 파일21개는 별도 backup으로 보존했다.
- 스토어 등록 출력: `design/play-store/film-strip-v4/`의 512px RGBA8 아이콘344,668B와 1024×500 RGB8 피처그래픽793,055B. 모두sRGB ICC 및8bit, 아이콘픽셀알파255/피처알파없음. 보관용master, 한국어대체텍스트, 업로드안내, 전체프롬프트도 제공한다. 등록 가이드에는 내장 AI 생성/편집 사실과 현재 Play의 자산별 신고 안내를 포함했다.
- `node design/branding/film-strip-v4/export-assets.cjs`: 출력23개 생성/검사 성공. 처음 시스템Node에서sharp탐색실패는 제공된 런타임 패키지경로 설정으로 해결했다. 마스크검토판 렌더링의 fontconfig 캐시 쓰기 경고는 있었으나 출력 생성과 글자 표시를 직접 확인했다. 등록문구는 생성 이미지에 포함되어 별도 시스템폰트 렌더가 아니다.
- 시각검사: 각 인화지4프레임, 하단표기철자, 체크무늬제거, 사진/문구잘림없음 확인. 원형·사각·둥근사각·squircle 및48/72/96/144px 축소 검토. 작은크기에서는얼굴·글씨·천공세부식별이제한된다. 독립 검토에서도 중요 수정 문제는 발견하지 못했다.
- 컬러전경은 차콜backing을 포함한 불투명RGBA다. 대비>20 픽셀의 최대반경31.2824dp<33dp를 보조검사했고, 낮은대비의 실루엣을포함하는 완전한알파경계검사로 주장하지 않는다. 단색벡터는 SVG/XML path일치, 사진창4개·천공6개투명, 외곽반경30.017dp 확인.
- `./gradlew.bat :app:assembleDebug --offline --console=plain`: 성공, 36태스크 중10실행/26UP-TO-DATE. 리소스컴파일과packageDebug를 실행해 새APK 생성. APK ZIP에서 브랜드PNG/SVG와15개런처PNG,5개아이콘XML이 포함된 것을 확인했다. 배포release서명이나기기설치는하지 않았다.
- 최종 패키지 `design/play-store/Pocket4Cut-PlayStore-Film-v4.zip`: 4,616,601 bytes, 파일 7개. ZIP 내부 7개 항목이 원본과 SHA-256 일치한다. APK에 포함된 런처 PNG 15개도 현재 리소스와 해시가 일치한다. 기존 휴대전화 소개 이미지 8장과 이전 ZIP의 해시가 변경되지 않은 것을 확인했다.
- 단위 테스트·Lint·실기기 런처·촬영/저장/공유 E2E는 이번 이미지 교체에서 재실행하지 않았다. 앞선 CAMERA Lint 오류를 해결한 작업이 아니다. 스토어 등록 규격 검증과 심사 승인은 별개다. 새 아이콘/피처 ZIP만 추가했으며 commit/push/스토어 업로드는 하지 않았다.

## 2026-09-10 — 아이콘 방향 재검토와 필름 레퍼런스 조사 (Asia/Seoul)

- 기준: `codex/setup-project-guidance`, HEAD `ac42903` 및 기존 미커밋 변경 포함. 사용자 피드백은 단순 인화지 아이콘 → 카메라 일러스트도 부적합 → 숫자 4 중심도 부적합 → 실제 네 컷/필름 사진을 웹에서 참고하라는 순서로 추가되었다.
- `design/branding/print-booth-v2/`에 카메라와 인화지 벡터 초안·PNG·검증 코드를 만들었다. 512px PNG 33,316B, RGBA8/sRGB ICC, 66dp 안전원 검사 및 Android 리소스 빌드는 통과했지만 **사용자가 방향을 거절해 최종 채택하지 않았다.**
- `design/branding/print-booth-v3/`의 숫자 4 심볼 SVG/PNG도 **미채택 비교용 초안**이다. v3 밀도별 PNG/배포 ZIP 생성은 중단했다. 두 폴더의 README에 현행 아이콘이 아님을 표시했다.
- 거절된 초안이 SVG/XML/PNG에 혼재하지 않도록 앱 아이콘을 마지막 납품본 v1으로 복구했다. `v2/previous-icon/` 기준 PNG 15개 SHA-256 일치, SVG/XML 6개 줄바꿈 정규화 후 내용 일치를 확인했다. 최초 바이트 비교의 배경 XML 불일치는 줄바꿈 차이였으며 전체 파일이 바이트 단위로 동일하다고 보고하지 않는다.
- 복구 후 `./gradlew.bat :app:assembleDebug --offline --console=plain` 성공: 36개 태스크 중 10개 실행/26개 UP-TO-DATE. 새 단위 테스트·Lint·실기기 검증은 실행하지 않았다. 앞선 Lint 오류를 해결한 작업도 아니다.
- 스토어 아이콘 사본, 기존 소개 PNG 8장, 기존 다운로드 ZIP은 변경하지 않았다. 초안에 실제 사용자 사진을 쓰거나 기기 데이터를 변경하지 않았다.
- 웹 조사: [Photo Pronto 실제 네 컷 인화지](https://www.photo-pronto.com/gallery), [Photoautomat 아날로그 사진 스트립](https://photoautomat.de/vermietung), [Kodak 필름 참고서](https://www.kodak.com/content/products-brochures/Film/kodak-essential-reference-guide-for-filmmakers.pdf), [ILFORD 흑백 필름 가이드](https://www.ilfordphoto.com/wp/wp-content/uploads/2017/04/Processing-your-first-black-and-white-film.pdf). 인생네컷 공식 클래식 프레임 페이지는 검색 색인에서 확인했으나 직접 열기는 403이므로 세부 이미지 확인 자료로 사용하지 않았다.
- 도출한 방향은 제작 제안이며 아직 구현하지 않았다: 세로로 이어진 네 사진과 인화지 여백을 중심에 두고, 실제 필름의 반복 천공·프레임 경계를 절제해서 참조한다. 포토부스 인화지와 네거티브 필름은 서로 다른 매체임을 구분하며 타사 로고/프레임을 복제하지 않는다.
- commit/push/스토어 업로드는 하지 않았다.

## 2026-09-10 — Print Booth 앱 아이콘 및 Play 휴대전화 등록 이미지 (Asia/Seoul)

### 범위와 기준

- 요청: 기존 UI 디자인에 맞춘 새 앱 아이콘 적용, 스토어 아이콘 파일, 앱 소개와 사용법을 설명하는 휴대전화 스크린샷 10장 이내 제작.
- 기준: 브랜치 `codex/setup-project-guidance`, HEAD `ac42903`과 이전부터 존재한 미커밋 UI·버전 변경을 포함한 로컬 작업 트리.
- Google Play의 기기 유형별 최대 8장 규격을 확인해 등록 이미지 8장으로 구성했다. 앱 아이콘은 별도 파일이다. 등록정보 업로드·게시·심사·release 서명 작업은 하지 않았다.
- 기존 README, 앱 버전, UI 재설계, `engineering/`, 이전 HTML 시안과 이미지, 출시 메모는 유지했다. 이번 아이콘/스토어 이미지 작업에서는 프로덕션 화면 코드와 기존 Gradle 설정을 추가 변경하지 않았다.

### 변경 파일과 산출물

- `app/src/main/assets/branding/pocket_4cut_app_icon.svg`: 종이색 네 컷 인화지, 잉크색 네 칸, 빨간 배경을 사용한 단순 벡터 아이콘.
- `app/src/main/res/`: 런처 전경/배경/monochrome XML, 일반/원형 adaptive 연결, 밀도별 PNG 15개. Manifest의 기존 아이콘 참조를 유지하며 새 자산으로 교체했다.
- `design/branding/print-booth-v1/`: SVG 원본, 512px 스토어 아이콘, 1024px 마스터, 마스크 확인 이미지와 검증 JSON, 재생성 스크립트. 교체 전 기존 아이콘 파일 19개는 `previous-icon/`에 보관했다.
- `design/play-store/print-booth-v1/`: `phone-screenshots/`에 번호순 개별 PNG 8장, 앱 아이콘, 전체 미리보기, 한국어 대체 텍스트, 업로드 안내와 검증 메모. 생성 사진·실제 Compose 원본·제작 코드는 하위 폴더로 분리했다.
- `scripts/store-screenshots.init.gradle`와 `design/play-store/print-booth-v1/capture/`: 명시적인 init script 실행에서만 붙는 Paparazzi 화면 렌더 도구. 앱의 기존 Screen 함수에 예시 상태를 공급하며 일반/release 앱에는 포함하지 않는다.
- `docs/IMAGE_ASSET_GUIDE.md`: 최초 조사 당시 아이콘 정보와 이번 갱신을 구분해 현행 아이콘 자산·안전 영역·검증 정보를 추가했다.

### 디자인과 이미지 검증

- 아이콘은 512×512 RGBA8, sRGB ICC, 14,882 bytes, 알파 255의 정사각형 PNG다. 미리 둥근 외곽/그림자를 넣지 않았다. 원본 SVG와 스토어 사본의 일치, 전경/단색 레이어, 밀도별 크기, 네 칸의 단색 투명 구멍을 확인했다.
- 화면 구성: 완성 예시 → 컷 수 → 사진 선택 → 레이아웃 → 프레임 색 → 필터 → 사진별 상세 편집 → 보관함.
- 등록 PNG는 8장 모두 1080×1920, 알파 없는 24-bit RGB PNG다. 추가 문구를 실제 앱 UI 밖에 배치하고 크기·색 채널·경계·SHA-256을 `asset-validation.json`에 기록했다.
- 실제 앱 Compose 함수를 1080×2400으로 렌더한 원본을 사용했다. 이전 HTML 시안을 Android 실행 캡처로 바꾸어 표기하지 않았다. 이미지 생성 도구는 가상의 성인 예시 사진에만 사용했고 UI나 문구는 생성 모델로 그리지 않았다.
- 초기 캡처의 빈 사진·로딩 상태를 시각 검사에서 발견해 테스트 전용 파일 경로/I/O와 예시 상태를 수정한 뒤 재캡처했다. 필터·레이아웃·사진별 선택지는 실제 스크롤 상태로 노출했다. 스크롤 위쪽의 긴 미리보기가 부분적으로 보이는 상태와 이미지 제작 과정의 잘림을 구분한다.
- 사진 선택 번호 1–4, 레이아웃 3종, 필터 4종, 사진별 썸네일과 보정 조작부, 보관함의 실제 Renderer 결과 4개를 확인했다. 생성 사진과 테스트 fixture는 앱에 번들하지 않았다.

### 실행한 검사와 결과

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain
```

- `assembleDebug`: 성공. `packageDebug`를 실제 실행해 새 아이콘이 포함된 APK를 생성했다. 리소스 컴파일 등 일부 입력은 앞선 렌더 빌드의 결과를 재사용했다.
- 기본 `testDebugUnitTest`: 실제 실행 성공, tests=1/failures=0/errors=0/skipped=0. 기본 산술 테스트이며 앱 사용 흐름 테스트는 아니다. 실행 시각 KST 00:33:15.947.
- `lintDebug`: 실패, 오류 1개/경고 91개/힌트 2개. 기존 CAMERA의 `PermissionImpliesUnsupportedChromeOsHardware`이며 이 작업에서 Manifest를 수정하거나 suppress하지 않았다.
- 결합 명령 exit code 1, 52개 태스크 중 15개 실행/37개 UP-TO-DATE. Lint 실패와 APK·단위 테스트 결과를 구분한다.

```powershell
./gradlew.bat -I scripts/store-screenshots.init.gradle :app:testDebugUnitTest --tests com.pocket4cut.storecapture.StoreScreenshotsTest --offline --console=plain
node design/play-store/print-booth-v1/source/render-listing.cjs
```

- Compose 화면 렌더 테스트 9개 성공, 실패/오류/건너뜀 0개. Home 1장을 추가로 렌더했지만 스토어 등록 구성은 요청한 8종이다.
- PNG 제작 스크립트 성공, 등록 이미지 8개 출력 및 크기·알파·색 채널·문구/이미지 경계 검사 통과. 최종 화면은 별도 시각 검사를 수행했다.
- 최종 원본 생성 완료 KST 00:45:13. 결과 화면은 실제 120px 스크롤로 인화지 하단·홈·저장·공유가 모두 보이도록 확인했다.
- `design/play-store/Pocket4Cut-PlayStore-Assets.zip` 생성: 4,290,390 bytes, 파일 13개. 휴대전화 PNG 8개와 아이콘 2개·대체 텍스트·업로드 안내·전체 미리보기로 구성했다. ZIP을 다시 열어 내부 파일 13개가 원본과 SHA-256 일치함을 확인했고 제작용 원본/fixture/코드는 포함하지 않았다.
- 정확한 렌더 실행 시각·화면별 테스트는 `capture/capture-manifest.json`, 이미지 파일 해시는 `asset-validation.json`, 상세 한계는 `VERIFICATION.md`에서 확인한다.

### 미검증과 남은 사항

- 연결된 기기/AVD가 없어 실기기 런처, 카메라 촬영, 사진첩 저장·공유, Android E2E는 미실행이다. 호스트 렌더 성공을 이 기능들의 성공으로 해석하지 않는다.
- 사진 재정렬 최종 출력, 커스텀 스티커 내보내기 등 기존 결함은 이번 소개 소재로 삼지 않았으며 수정하지 않았다.
- 스토어 이미지 규격 준수와 Play 심사 승인은 별개다. 실제 배포할 앱이 변경되면 등록 이미지를 해당 버전으로 다시 생성해야 한다.
- 사용자 사진 삭제, 앱 데이터 초기화, stage/commit/push는 수행하지 않았다.

## 2026-09-10 — 프로젝트 개발 지침과 기준 문서 구성

### 요청과 작업 범위

기존 README·기획 문서·실제 코드 구조·빌드 설정·테스트·Git 상태를 조사하고 프로젝트 맞춤 Codex 지침을 적용했다. 기능 코드는 변경하지 않고 다음 네 파일을 새로 작성하는 작업이다.

- [AGENTS.md](../AGENTS.md): 한국어 소통, 조사 순서, 변경 범위, 기존 작업/사진 보존, 이미지 처리 검증, Git·작업 로그 운영 지침.
- [ARCHITECTURE.md](ARCHITECTURE.md): 실제 단일 모듈·화면·상태·이미지·저장 구조와 미연결 코드, 향후 제안.
- [IMAGE_ASSET_GUIDE.md](IMAGE_ASSET_GUIDE.md): 현재 프레임·스티커·아이콘·패턴 구현, 파일명·해상도·투명 배경·내보내기 기준과 신규 자산 제안.
- `WORKLOG.md`: 이번 조사·검증 기준과 이후 기록 방식.

루트 `AGENTS.md`와 위 docs 파일은 작업 시작 시 존재하지 않았다. 사용자 지정 경로를 사용했으며 전역 Codex 설정이나 별도 규칙 디렉터리를 만들지 않았다. AGENTS.md의 프로젝트 지침 역할은 [OpenAI 공식 안내](https://learn.chatgpt.com/docs/agent-configuration/agents-md)로 확인했다.

### 조사 기준과 기존 작업 보호

- 시작 HEAD: `95140a0`, 시작 브랜치: `main`.
- 작업 브랜치: `codex/setup-project-guidance`를 생성했다.
- origin은 `sin-jun-woo/Pocket-4Cut-Android` GitHub 저장소다. 이번 작업은 main 병합이나 강제 push를 포함하지 않는다.
- 시작 시 staged 변경은 없었다. 기존 tracked 변경은 README 1개와 app 관련 27개였으며, 이전 분석 문서·디자인·출시노트·smoke script 등 미추적 파일도 있었다.
- 기존 추적/미추적 일반 파일 190개의 SHA-256 기준을 작업 전 로컬 캐시에 보관했고, 문서 작성 후 **190/190개 불변**을 확인했다. 비밀 파일과 ignored 빌드 산출물은 이 비교 대상이 아니다.
- 기존 앱 변경, README 변경, `PROJECT_UNDERSTANDING.md`, `engineering/`, `design/`, 기존 출시노트와 smoke script는 이번 stage/commit 대상에서 제외한다.
- **아래 빌드 결과와 구조 문서는 미커밋 앱 변경을 포함한 로컬 작업 트리 기준이다.** 문서만 커밋하므로 원격 브랜치의 앱 소스까지 같은 상태가 되는 것은 아니다.

### 확인된 프로젝트 구성

- Gradle 모듈은 `:app` 하나, applicationId/namespace는 `com.pocket4cut`.
- compileSdk/targetSdk 36, minSdk 26, Gradle 8.13, AGP 8.13.2, Kotlin 2.0.21, Java/Kotlin 코드 타깃 11.
- 이번 실행 환경은 Windows, Gradle 실행 JVM은 JBR 21.0.10이다. 개인 JDK/SDK 경로를 문서나 빌드 설정에 추가하지 않았다.
- 로컬 앱 버전은 `1.2 (versionCode 3)`이나 시작 HEAD의 앱 버전은 `1.0 (versionCode 1)`이다. 버전 변경은 기존 미커밋 작업이며 이번에 반영하지 않는다.
- main Kotlin 85파일, 15,224줄. CameraX·Compose·Canvas, JSON 세션 저장소, SharedPreferences를 사용하는 로컬 앱이다.
- 기존 루트 README/ARCHITECTURE/API/DATA_STORAGE/ERROR_HANDLING/PROJECT_PLAN에는 과거 상태나 미래 설계가 포함되어 있다. Room/Hilt/완성된 UseCase/AWS를 현재 구현으로 옮겨 적지 않았다.
- GitHub Actions workflow는 main의 docs 변경 또는 수동 실행으로 docs 전체를 Pages에 게시한다. 현재 Android build/test/lint CI는 없다.

### 이번에 실행한 검증

저장소 루트에서 실행했다. 의존성이 있는 기존 Gradle 캐시와 로컬 JDK를 사용했고, 소스·의존성·Lint 설정은 바꾸지 않았다.

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain
```

- `:app:assembleDebug`: 통과, **UP-TO-DATE**. 기존 APK 산출물과 입력 상태를 재사용했다.
- `:app:testDebugUnitTest`: 첫 결합 명령에서는 **UP-TO-DATE**였으므로 아래 명령으로 테스트 태스크만 실제 재실행했다.
- `:app:lintDebug`: 실패. 기존 분석 결과를 재사용한 검사에서 **오류 1개, 경고 95개, 힌트 2개**를 보고했다.
- 결합 명령 exit code는 1이며, 52개 actionable task 중 1개 실행·51개 UP-TO-DATE였다. 빌드/단위 테스트 통과와 Lint 실패를 구분한다.

```powershell
./gradlew.bat :app:testDebugUnitTest --rerun --offline --console=plain
```

- 실제 테스트 태스크 재실행: **성공**, exit code 0. 의존 태스크는 재사용했다.
- 결과: tests=1, failures=0, errors=0, skipped=0.
- 테스트는 `ExampleUnitTest.addition_isCorrect`의 기본 산술 확인이며 촬영·편집·저장 회귀 검증이 아니다.
- 결과 XML 실행 시각: `2026-09-09T15:04:12.439Z` = KST `2026-09-10 00:04:12.439`.
- 산출물 경로: `app/build/outputs/apk/debug/app-debug.apk`, `app/build/test-results/testDebugUnitTest/TEST-com.pocket4cut.ExampleUnitTest.xml`, `app/build/reports/lint-results-debug.html` 및 `.xml`. 이 파일은 이번 커밋에 포함하지 않는다.

Lint 차단 사유는 `app/src/main/AndroidManifest.xml:6`의 `PermissionImpliesUnsupportedChromeOsHardware`다. CAMERA permission에 대응하는 하드웨어 uses-feature 선언이 없다. 같은 선언은 시작 HEAD에도 있으며 이번 문서 작업에서 수정하거나 suppress하지 않았다.

경고 95개에는 UseKtx 49, GradleDependency 11, ModifierParameter 7, UnusedResources 7 등이 포함된다. 업데이트 경고를 실제 기능 실패 또는 일괄 업데이트 필요성으로 단정하지 않는다.

### 검토와 미검증 범위

- 새 문서의 소스 링크, 현재 구현/제안 구분, 미커밋 작업 기준, 공개될 정보 범위를 검토했다. 링크·코드 블록·UTF-8 문자·공백 검사 오류는 0개였다.
- Git diff와 문서 네 파일만 포함된 staged 목록을 검토했고, staged 내용이 검토한 파일과 일치함을 확인했다. `git diff --cached --check`도 통과했다. 기존 파일 190개의 해시 보존을 확인했다.
- Android instrumentation 테스트는 새로 실행하지 않았다. 기본 packageName 테스트는 있으나 이번에는 기기 앱 설치·촬영·데이터 초기화를 수행하지 않았다.
- 실기기 전체 흐름, 프로세스 복원, 저메모리, API26–28 사진첩 저장, 백업 복원, 접근성, release 서명과 Play 공개 상태는 이번 검증 범위 밖이다.
- 문서에서 확인한 사진 순서·문구 영역·색/스티커 출력·삭제 수명 문제는 후속 기능 작업 대상으로 남긴다.

### 커밋과 원격 확인 방식

이번 커밋의 파일 범위는 위 네 문서이며 메시지는 `docs: Pocket 4Cut 개발 지침 및 작업 로그 추가`다. 커밋 SHA는 자기 자신을 파일 본문에 넣어 반복 amend하지 않고 Git 이력과 최종 완료 보고에서 확인한다.

```powershell
git log -1 --format='%H %s' -- docs/WORKLOG.md
git status --short --branch
git rev-parse --abbrev-ref '@{upstream}'
git ls-remote --heads origin codex/setup-project-guidance
```

문서 검토 후 `git push --set-upstream origin codex/setup-project-guidance`를 수행하고 원격 SHA·upstream 설정 결과를 최종 보고에 남긴다. 이 항목의 존재만으로 원격 반영 성공을 추정하지 않는다.

## 이후 작업 기록 양식

```markdown
## YYYY-MM-DD — 작업 제목 (Asia/Seoul)
- 요청/범위:
- 기준 브랜치·커밋 / 미커밋 변경 포함 여부:
- 변경 파일과 목적:
- 현재 구현에서 확인한 사실:
- 실행 명령과 결과(통과/실패/캐시 재사용):
- 미실행 항목과 이유:
- 기존 변경·사용자 데이터 보존 확인:
- 남은 문제 / 향후 제안:
- commit/push를 수행한 경우 식별 방법과 결과:
```
