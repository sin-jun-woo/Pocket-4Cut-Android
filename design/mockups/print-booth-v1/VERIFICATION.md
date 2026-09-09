# 확인 기록 — 2026-09-09

## Android 코드

- `:app:compileDebugKotlin`, `:app:assembleDebug` 성공
- `:app:testDebugUnitTest` 성공: 저장소에 있는 기본 단위 테스트 1개. 화면 동작을 검증하는 UI 테스트는 아님
- 출력: `app/build/outputs/apk/debug/app-debug.apk`
- 현재 프로젝트 버전 `1.2 (3)` 유지
- 사용자 작업 중이던 레이아웃 화면의 스크롤·배치 로직 유지; 색·그림자·모서리·체크 표시만 보완
- `settings.gradle.kts`의 빌드 진단용 임시 변경은 원복

## 이미지

- 실제 앱 경로에 대응하는 17개 화면을 각각 PNG로 제공
- 화면 PNG와 스타일 보드: 1080×2400; 전체 보기: 1080×3000
- 렌더러에서 화면 크기, 루트 영역 넘침, 버튼 높이와 화면 내 노출, PNG 크기 검사
- 전체 보기 및 잘림이 있었던 사진 선택·레이아웃·편집 화면을 눈으로 재확인
- 사진 선택·개인정보 전문은 화면 안의 스크롤 영역으로 표현
- 법률 본문과 문의 주소는 기존 프로젝트 내용을 재사용했으며 법률 검토를 수행한 것은 아님
- 검사 수치는 `render-validation.json` 참고

## 남은 검증 한계

- PNG는 디자인 기준 시안이다. 실제 Android 화면을 촬영한 스크린샷은 아니다.
- ADB 조회 결과 연결된 기기 없음. 실기기에서 촬영·저장·공유·삭제 및 다양한 화면 크기의 UI 동작은 확인하지 못함.
- `:app:lintDebug`는 기존 `AndroidManifest.xml:6`의 `PermissionImpliesUnsupportedChromeOsHardware` 오류 1건으로 실패했다. 카메라 권한에 대응하는 하드웨어 기능 선언 문제이며, 같은 선언이 변경 전 HEAD에도 존재함을 확인했다. 이 디자인 작업에서는 기기 지원 정책을 변경하지 않았다.
- 정적 검사에는 경고 95개와 힌트 2개도 남아 있다. 전체 결과는 `app/build/reports/lint-results-debug.html` 참고.
