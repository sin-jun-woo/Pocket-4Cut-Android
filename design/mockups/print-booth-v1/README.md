# Pocket 4Cut — Print Booth v1

화면의 둥근 카드·핑크 글로우·캔디 그라디언트를 걷어내고, 동네 셀프사진관의 인화지와 부스 조작 패널을 기준으로 정리한 화면 시안입니다.

## 파일

- `00_styleboard.png`: 컬러·반경·그림자·타이포 기준
- `01_launch.png` ~ `17_contact_feedback.png`: 화면별 1080×2400 PNG (360×800 CSS px @3x)
- `contact-sheet.png`: 전체 화면 빠른 확인용
- `screens.html`: 수정 가능한 화면 원본
- `render-mockups.cjs`: PNG 재생성 스크립트
- `render-validation.json`: PNG 해상도, 화면 넘침, 버튼 높이·노출 검사 결과

모든 PNG는 **디자인 기준 시안**이며 Android 기기 캡처가 아닙니다. 사진은 레이아웃 확인용 자리 표시자이고, 일부 긴 목록은 화면 안에서 스크롤하도록 표현했습니다. 개인정보·문의 문구는 프로젝트의 기존 화면 내용을 사용했습니다.

## 화면 목록

01 시작 · 02 홈 · 03 컷 수 · 04 촬영 · 05 사진 선택 · 06 레이아웃 · 07 프레임 방식 · 08 색 프레임 · 09 계절 프레임 · 10 커스텀 프레임 · 11 콜라주 편집 · 12 상세 편집 · 13 결과 · 14 보관함 · 15 설정 · 16 개인정보 처리방침 · 17 문의/피드백

## 디자인 기준

- Paper `#F3F0E8`, Surface `#FBFAF6`, Ink `#1B1B19`, Muted `#6E6A63`, Hairline `#D0CBC1`
- 이 시안의 포인트는 봄 테마의 인화/REC 레드 `#C83D2D`; 실제 앱은 여름 청록, 가을 브라운, 겨울 블루 강조색도 지원
- 사진과 프리뷰는 0~2px, 카드와 버튼은 2~8px
- 일반 화면의 그림자는 인화지 프리뷰 한 개로 제한; 임시 토스트·다이얼로그는 약한 중립 그림자 사용
- 원형은 셔터, 슬라이더 thumb, 색상 스와치처럼 기능이 설명하는 경우에만 사용

## 재생성

Node.js에서 `playwright`, `sharp`를 사용할 수 있어야 합니다. 기본 브라우저는 Windows에 설치된 Chrome이며, 다른 경로는 `CHROME_PATH` 환경변수로 지정합니다.

PowerShell 예시:

```powershell
$env:NODE_PATH = '<playwright와 sharp가 있는 node_modules 경로>'
node .\render-mockups.cjs
```

실행하면 이 폴더의 PNG와 검사 결과를 다시 생성합니다.
