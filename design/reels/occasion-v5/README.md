# Pocket 4Cut — 88가지 프레임 릴스

가장 마지막에 제작한 `seasonal-v4`의 기본 틀을 이어가는 23초 세로 영상입니다. 새로운 앱 UI나 다른 영상 스타일을 만드는 작업이 아니라, 기존 템플릿의 사진·대본·자막을 Everyday Editions 88종으로 바꾼 편입니다.

## 다운로드

- [업로드용 MP4](deliverables/Pocket4Cut-Reels-88Frames-Voice.mp4)
- [커버 JPG](deliverables/Pocket4Cut-88Frames-Cover.jpg)
- [전체 패키지 ZIP](Pocket4Cut-88Frames-Reels-Package.zip)
- [내레이션 MP3](deliverables/Pocket4Cut-88Frames-Narration.mp3), [자막 SRT](deliverables/CAPTIONS-KO.srt), [대본](deliverables/NARRATION-KO.txt)
- [게시글 초안](deliverables/INSTAGRAM-CAPTION.txt), [게시 전 안내](deliverables/UPLOAD-GUIDE.md)

## 유지한 것과 바꾼 것

- 유지: 1080×1920, 30fps, 종이색·먹색·붉은 포인트, 기존 글꼴, 기울어진 인화지, 약한 확대, 하드 컷, 짧은 큰 자막, 남성 존댓말 내레이션, 마지막 브랜드/검색 안내. 음악·효과음 없음.
- 변경: 사계절 소개를 생일·커플·졸업·여행의 서로 다른 분위기로 교체. 88종 전체 월, 10개 분류, 2·4·6컷, 저장·공유 소개.
- 음성: 기존 승인 참조와 같은 로컬 모델/설정으로 새 대본을 합성했다. 최종 원음 20.24초, 0.12초 시작 여유와 끝 안내를 더한 23초 마스터. 배속·발화 자르기 없음. 동일 참조의 재사용은 완전히 동일한 음색을 보증하지 않는다.

## 소재와 제품 사실

제작 기준 `caa4508`, 2026-09-27 앱1.7(8) 소스에는 88종/10분류가 통합되어 있다. 앱 코드를 바꾸지 않았다. 직접 기록11종은 위치·날씨·D-Day·횟수를 자동 조회하는 기능이 아니며 영상에서도 이를 주장하지 않는다. 이번 버전의 Google Play 게시·업데이트 수신 여부는 별도로 확인해야 한다.

현재 runtime 카탈로그·WebP 아틀라스와 기존 가상 성인 사진 4장으로 **편집용 합성**을 만들었다. 9월27일 제거된 상단 공통 Pocket/NN 표기를 반영했다. Node로 만든 합성이므로 Android의 실제 JPEG 출력 또는 기기 연속 녹화가 아니다. BM 글꼴은 기존 파일을 사용하고 시스템 serif/mono 계열은 설치 글꼴로 대체하여 Android와 픽셀 차이가 있을 수 있다. 세부 입력·규격·해시는 `visual-assets/input-manifest.json`에 있다.

전체 월에는 88개의 고유 프레임이 한 번씩 나오며 사진창은 중립색으로 표시된다. 짧은 본편에서 모든 개별 프레임을 크게 읽어볼 수 있다는 의미는 아니다. 2·4·6컷 구간은 전체 사진창이 보이도록 비율을 유지한다. 저장·공유 안내는 편집용 문구다. 실제 사용자 사진·원본 참조 영상·참조 음성은 공개/Git/ZIP에 넣지 않는다. 이전 영상과 기존 프레임 납품물도 보존했다.

## 재생성

로컬에 설치된 `@napi-rs/canvas`가 해석되도록 `NODE_PATH`를 지정하고, 검증된 FFmpeg 실행 파일을 `FFMPEG_PATH`에 지정한다. 로컬 폰트 Malgun Gothic·Georgia·Consolas와 앱에 포함된 BM 폰트를 사용하되 폰트 파일 자체는 배포하지 않는다.

1. `node source/build-visual-assets.cjs`
2. 기존 독립 Qwen 환경의 Python으로 `source/generate-local-voice.py` 실행. 이미 완성된 원음이 있으면 중단하여 보존한다.
3. `node source/master-narration.cjs`
4. `node source/transcribe-narration.cjs` → `node source/time-captions.cjs`
5. `node --test source/test-time-captions.cjs`
6. `node source/render-occasions.cjs` → `node source/export-occasions.cjs`
7. `source/package-occasions.ps1`

ASR에 예상 대본을 제공하지 않는다. 숫자 표기를 정규화하되 잘못 말한 단어를 고치지 않는다. 내용·타이밍 검사가 실패하면 렌더를 중단하고 실제 원인부터 확인한다. 원본 인식 자료·기기 경로가 들어가는 도구 로그는 ignored build 영역에만 둔다.

## 확인 기록

- 최종 MP4: 23초·690프레임·8,074,069bytes, H.264/yuv420p/BT.709, AAC48kHz stereo. 전체 디코딩·AV 길이·faststart PASS, 검은 화면 구간0, −16.1LUFS/−3.0dBTP.
- 최종 음성: 정답 프롬프트 없는 새 ASR에서 정규화127글자·edit distance0. 1차 조사1글자 불일치는 차단·보존하고 문장 수정 후 전체 재생성했다.
- 자막 정렬 합성 테스트12/12, 원본 단계28스틸·중요 텍스트50개·레이아웃13개 안전 영역 PASS. 실제 MP4의14장면 모아보기와88종 월도 직접 확인했다.
- 전체 월88개 고유 테마/10분류 및 원본·합성·음성·대본·renderer·인코딩 파일 해시 연결 PASS. ZIP7개 항목 전부 스트림 SHA-256 대조 PASS, 패키지8,978,121bytes.

최종 검사 수치는 `verification/export-validation.json`, `verification/asr-result.json`, `verification/package-validation.json`을 기준으로 한다. 타이밍의 합성 fixture 검사는 실제 발음 평가와 다르며 ASR 일치도 주관적 청취·원본 음색 동일성의 보증이 아니다. 앱 빌드·실기기 테스트·Instagram 로그인/업로드는 이번 영상 제작에서 실행하지 않는다.

실제 인코딩 파일에서 추출한 장면은 `verification/encoded-contact-sheet.jpg`와 각 `encoded-*.jpg`, 소스 단계의 스틸은 `visual-stills/`에 구분한다. 휴대전화 음성 청취와 Instagram 커버 자르기·압축 결과는 게시 직전에 확인한다.
