# 포켓 네 컷 (Pocket 4Cut)

> **현재 구현 기준 안내 (2026-09-09)**: 개발을 시작할 때 [프로젝트 이해·인수 문서](PROJECT_UNDERSTANDING.md)를 먼저 읽어 주세요.
> [파일별 코드 지도](engineering/CODE_MAP.md), [확인된 문제](engineering/KNOWN_ISSUES.md), [실제 빌드·검증 기록](engineering/VERIFICATION.md)을 함께 정리했습니다.
> 아래 본문과 기존 설계 문서에는 초기 기획 당시의 상태·기술 설명이 남아 있습니다.

<div align="center">

**집에서 즐기는 모바일 인생네컷 부스**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📱 프로젝트 소개

**포켓 네 컷 (Pocket 4Cut)** 은 집에서도, 방에서도, 술자리에서도  
오프라인 인생네컷 부스 경험을 그대로 가져오는 **모바일 포토부스 앱**입니다.

> 이 앱은 “사진 보정 앱”이 아니라  
> **찍는 순간 자체를 콘텐츠로 만드는 앱**입니다.

### 왜 만들었나요?

- 인생네컷을 찍으려면 매장까지 **직접 가야 합니다.**
- 줄을 서야 하고, 1회 촬영 비용도 적지 않습니다.
- 기본 카메라 앱은 “찍기만” 하고 **놀이성이 부족합니다.**
- 사진은 많이 쌓이지만, **공유 가능한 콘텐츠**로 잘 묶이지 않습니다.

### 어떻게 해결하나요?

- 8/10장 **자동 연속 촬영**으로 실제 부스 느낌을 그대로 가져옵니다.
- 그중 원하는 컷만 골라 **4컷 / 6컷 콜라주**를 생성합니다.
- 감성 프레임 / 필터 자동 적용으로 **결과물 퀄리티를 보장합니다.**
- 결과 이미지를 **바로 저장하고 공유**할 수 있습니다.

핵심은 **“찍는 과정 자체가 콘텐츠”** 라는 점입니다.

---

## ✨ 제품 철학 & 절대 원칙

### 절대 원칙 (깨지면 앱의 의미가 없음)

- **촬영 경험이 재미있어야 한다**
- **결과물이 무조건 “예쁘게” 나와야 한다**
- **선택 UX가 스트레스 없어야 한다**
- **촬영 완료 → 공유까지 10초 안에 끝나야 한다**

이 네 가지가 지켜지지 않으면, 기능이 아무리 많아도 실패한 앱으로 간주합니다.

### UX 원칙

- 앱 열면 **바로 촬영 시작 가능**해야 함
- 촬영 흐름 중간에 **팝업/세팅으로 끊기면 안 됨**
- 선택 과정은 **“설명 안 봐도” 이해될 정도로 직관적**
- 결과 화면은 **“저장 / 공유” 두 개만 보이면 될 정도로 단순**
- 최종 목표: **“3번 터치 안에 결과물”**

---

## 🎬 오프라인 인생네컷 부스 경험 디테일 (요약)

### 촬영 타이밍

- 촬영 시작 전에 **준비 시간 10초 카운트다운 (10→9→…→1)** 를 제공합니다.
- 각 컷 사이 간격은 기본 **2초 전후**로 유지하여, 실제 부스처럼 빠르게 리듬감 있게 진행합니다.
- 8컷 기준 총 촬영 시간은 준비 포함 **25~30초**, 10컷 기준 **30~35초 이내**를 목표로 합니다.

### 감성 요소

- 카운트다운 숫자 애니메이션, 셔터 사운드, 플래시 느낌 시각효과 등을 통해  
  **“부스 안에 들어간 느낌”** 을 최대한 모바일에서 재현합니다.
- 촬영이 모두 끝났을 때는 짧은 완료 사운드와 함께, 결과로 넘어가는 애니메이션을 제공합니다.

자세한 상태 정의와 UX 흐름은 `ARCHITECTURE.md` 및 관련 문서를 기준으로 구현합니다.

---

## 🎯 MVP 범위 (1차 출시)

### 📸 촬영

- 8장 / 10장 **자동 연속 촬영**
- 타이머 (3초 기본, 옵션 변경 가능)
- 전면 카메라 기본
- 촬영 리듬 자동 진행 (카운트다운 + 진행 표시)

### 🖼️ 선택

- 촬영된 사진 전체 그리드 표시
- 4장 또는 6장 **정확히 선택**
- 선택 순서 표시 (1, 2, 3, 4…)
- 선택 개수 제한 (초과 시 피드백)

### 🎨 프레임

- 4컷 프레임 3종
- 6컷 프레임 2종
- 색상 옵션: 화이트 / 블랙 + 1~2 포인트 컬러

### ✏️ 편집

- 필터 3종 (예: Soft / Film / B&W)
- 텍스트 입력 (간단 문구)
- 날짜 자동 삽입 (On/Off)
- 사진 순서 변경 (드래그 & 드롭 or 간단 스왑)

### 💾 결과

- 고해상도 콜라주 이미지 생성
- 갤러리 저장
- **Android 공유 시트(Intent.ACTION_SEND)** 로 바로 공유

### 🗂️ 보관함

- 결과물 리스트 (썸네일)
- 날짜별 정렬
- 삭제

---

## 🚫 MVP에서 제외하지만 이후 추가 예정인 기능

MVP(1차 출시)에서는 **아래 기능을 구현하지 않지만**,  
기본 촬영/편집/공유 경험이 안정화된 이후 **정식 출시 전까지 순차적으로 추가하는 것**을 목표로 합니다.

- 로그인 / 회원 기능
- 친구 / 팔로우 / 피드
- 댓글 / 좋아요
- AI 보정 / AI 리터칭
- 프레임 마켓 / 유료 프레임 스토어 (프리미엄 프레임)
- QR 공유 (링크 기반 공유)

이 기능들은 **확장성과 성장(공유/바이럴, 수익화)에 중요한 축**이지만,  
초기에는 이를 모두 넣기보다는 **카메라 UX / 결과물 퀄리티**를 우선 완성한 뒤,  
**MVP 안정화 이후 별도 마일스톤으로 추가**하는 방향으로 진행합니다.

---

## 🎨 화면 구성

- **1. 홈**
  - “촬영 시작” 메인 버튼
  - 최근 결과물 미리보기

- **2. 촬영 설정**
  - 연속 촬영 장수: 8장 / 10장
  - 최종 레이아웃: 4컷 / 6컷
  - 타이머 설정

- **3. 촬영**
  - 자동 연속 촬영
  - 진행 표시 (예: 3/10)
  - 카운트다운 애니메이션

- **4. 선택**
  - 촬영된 전체 사진 표시 (그리드)
  - 4장 또는 6장 선택
  - 선택 순서 뱃지

- **5. 프레임**
  - 프레임 리스트
  - 실시간 미리보기

- **6. 편집**
  - 필터 적용
  - 텍스트 입력
  - 날짜 표기 On/Off
  - 사진 순서 변경

- **7. 결과**
  - 최종 이미지 표시
  - 저장 / 공유 버튼

- **8. 보관함**
  - 결과 리스트
  - 날짜 기준 정렬
  - 삭제

---

## 🏗️ 기술 스택 (Android)

### App

- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose
- **Architecture**: MVVM + 레이어 분리 (필요 시 모듈 분리)
- **Min SDK**: API 24+ (권장 API 26+)
- **Target SDK**: API 34+

### 카메라 & 이미지 처리

- **카메라**: CameraX
  - 전면 카메라
  - 타이머 기반 자동 연속 촬영
- **이미지 처리**: Android Graphics / Bitmap
  - 필터 적용, 프레임 합성, 콜라주 렌더링

### 저장 & 데이터

- **이미지 저장**: 앱 전용 디렉터리 (`getExternalFilesDir` 등) + MediaStore(갤러리)
- **메타데이터**: Room 또는 DataStore
  - 세션 정보, 보관함 리스트 등
- **서버/백엔드 (향후)**: AWS 기반 REST API (예: 인증, 피드, 마켓 등) — 구체 구성은 서버 설계 문서 참고

### 공유

- **공유**: `Intent.ACTION_SEND` (공유 시트)

---

## 🧱 데이터 구조 (Android 기준)

```kotlin
data class PhotoSession(
    val id: String,                // UUID 문자열
    val captureCount: Int,         // 8 or 10
    val selectedCount: Int,        // 4 or 6
    val imagePaths: List<String>,  // 로컬 경로
    val selectedIndexes: List<Int>,
    val frameId: String,
    val finalImagePath: String?,
    val createdAt: Long           // epoch millis
)
```

---

## 🔑 기술 핵심 포인트

1. **연속 촬영 엔진**
   - 타이머 기반 자동 촬영
   - 상태 관리 (idle → countdown → capturing → done)
   - 끊김 없이 일정한 리듬 유지

2. **선택 제한 로직**
   - 4장 / 6장 **정확히 선택**해야 다음 단계로 이동
   - 선택 순서 보장 (`selectedIndexes`)
   - 초과 선택 / 취소 UX 매끄럽게

3. **프레임 렌더링**
   - 이미지 합성 (프레임 + 사진)
   - 고정 비율 / 해상도 유지 (인쇄 고려)
   - 렌더링 성능 최적화

4. **저장 최적화**
   - 이미지 압축 품질/용량 밸런스
   - 백그라운드에서 렌더링/저장
   - 보관함 스토리지 관리 전략

---

## 📆 개발 단계 (6주 플랜)

### 1주차 — 구조 설계

- 전체 화면 흐름 정의
- `PhotoSession` 등 데이터 모델 설계
- Android Studio 프로젝트 생성, 기본 모듈 설정

### 2주차 — 홈 / 설정 / 권한

- 홈 화면 UI (Compose)
- 촬영 옵션(연속 장수, 컷 수, 타이머) 설정
- 카메라/저장 권한 요청 및 에러 처리

### 3주차 — 연속 촬영 기능

- CameraX 카메라 세팅
- 타이머 + 자동 연속 촬영
- 촬영 진행 상태 표시

### 4주차 — 선택 화면 & 로직

- 썸네일 그리드 UI
- 선택 개수 제한 / 검증
- 선택 순서 표시

### 5주차 — 프레임 & 결과 생성

- 프레임 레이아웃 정의
- Bitmap 기반 콜라주 렌더링
- 고해상도 결과 이미지 생성 & 저장

### 6주차 — 편집 / 저장 / 공유 / QA

- 필터 / 텍스트 / 날짜 옵션
- 공유 Intent 연동
- 보관함 리스트
- 실제 기기 테스트 & 튜닝

---

## 📁 프로젝트 구조 (초안, Android)

```text
app/src/main/java/com/pocket4cut/
├── presentation/          # 화면 & UI (Compose)
│   ├── home/
│   ├── capture/
│   ├── selection/
│   ├── frame/
│   ├── edit/
│   ├── result/
│   └── gallery/
├── domain/                 # 도메인 모델 & 로직
│   ├── model/
│   │   └── PhotoSession.kt
│   ├── repository/
│   └── usecase/
├── data/                   # 저장소 구현
│   ├── storage/
│   │   ├── ImageStorageImpl.kt
│   │   └── SessionRepositoryImpl.kt
│   └── local/
├── camera/
│   └── CaptureEngine.kt    # CameraX 연속 촬영
├── frame/
│   ├── FrameDefinitions.kt
│   └── CollageRenderer.kt
└── core/                   # 유틸, DesignSystem
```

---

## 🚀 빠른 시작

### 필요 환경

- Android Studio Ladybug (2024.2.1) 이상 권장
- JDK 17
- Android API 24+ 에뮬레이터 또는 실제 기기

### 프로젝트 실행

```bash
# 1. 저장소 클론
git clone https://github.com/yourusername/Pocket-4Cut-Android.git
cd Pocket-4Cut-Android

# 2. Android Studio에서 열기
# File > Open > Pocket-4Cut-Android 폴더 선택

# 3. Gradle sync 후 Run
# ▶ 버튼 또는 Shift+F10 (Windows/Linux) / ⌃R (Mac)
```

---

## 📖 문서 (개발 시작 전 계획/설정)

| 문서 | 설명 |
|------|------|
| [QUICK_START.md](./QUICK_START.md) | 빠른 시작 요약 |
| [PROJECT_PLAN.md](./PROJECT_PLAN.md) | 6주 개발 계획, Phase별 상세 작업 |
| [PROJECT_SETUP_GUIDE.md](./PROJECT_SETUP_GUIDE.md) | Xcode 프로젝트 생성, 폴더 구조, 권한 |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 레이어 구조, 모듈 역할, 데이터 흐름 |
| [DATA_STORAGE.md](./DATA_STORAGE.md) | 이미지/메타데이터 저장 구조 |
| [ERROR_HANDLING.md](./ERROR_HANDLING.md) | 에러 종류, 사용자 메시지, UI 처리 |
| [API_SPECIFICATION.md](./API_SPECIFICATION.md) | Repository / UseCase 인터페이스 |

---

## 📊 프로젝트 상태

**현재 상태**: 🔴 기획 완료 / 개발 준비 중

### 정리된 것

- 문제 정의 / 해결 방식
- 절대 원칙 / UX 원칙
- MVP 범위
- 데이터 구조 초안 (`PhotoSession`)
- 화면 플로우 / 프로젝트 구조 초안

### 다음 단계

1. Android 프로젝트 생성 (PROJECT_SETUP_GUIDE.md 참고)
2. 카메라 구조 설계 (`CaptureEngine`, CameraX)
3. 촬영 세션 로직 구현
4. 프레임 구조 / 콜라주 렌더링 정의
5. UI(Compose) 화면 구성

---

## 📄 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.  
자세한 내용은 [LICENSE](LICENSE) 파일을 참고해 주세요.

---

## 👤 개발자

- **이메일**: sus3456@naver.com
- **GitHub**: [@sin-jun-woo](https://github.com/sin-jun-woo)

---

## 🔥 한 줄 요약

> **포켓 네 컷은 “사진 앱”이 아니라  
> “찍는 순간 자체를 콘텐츠로 만드는 앱”이다.**

