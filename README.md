## 포켓 네 컷 (Pocket 4Cut) - Android

<div align="center">

**집에서 즐기는 모바일 인생네컷 부스 (Android 버전)**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📱 프로젝트 소개

**포켓 네 컷 (Pocket 4Cut)** Android 버전은  
오프라인 인생네컷 포토부스 경험을 스마트폰에서 그대로 즐길 수 있게 해 주는  
**모바일 포토부스 앱**입니다.

> 이 앱은 “사진 보정 앱”이 아니라  
> **찍는 순간 자체를 콘텐츠로 만드는 앱**입니다.

### 왜 만들었나요?

- 인생네컷을 찍으려면 매장까지 **직접 가야 합니다.**
- 줄을 서야 하고, 1회 촬영 비용도 적지 않습니다.
- 기본 카메라 앱은 “찍기만” 하고 **놀이성이 부족합니다.**
- 사진은 많이 쌓이지만, **공유 가능한 콘텐츠**로 잘 묶이지 않습니다.

### 어떻게 해결하나요?

- 8/10장 **자동 연속 촬영**으로 실제 부스 느낌을 재현합니다.
- 그중 원하는 컷만 골라 **4컷 / 6컷 콜라주**를 생성합니다.
- 감성 프레임 / 필터 자동 적용으로 **결과물 퀄리티를 보장합니다.**
- 결과 이미지를 **바로 저장하고 공유**할 수 있습니다.

핵심은 **“찍는 과정 자체가 콘텐츠”** 라는 점입니다.

---

## ✨ 제품 철학 & 절대 원칙

### 절대 원칙

- **촬영 경험이 재미있어야 합니다.**
- **결과물이 무조건 “예쁘게” 나와야 합니다.**
- **선택 UX가 스트레스가 없어야 합니다.**
- **촬영 완료 → 공유까지 10초 안에 끝나야 합니다.**

위 원칙이 지켜지지 않는다면, 기능이 아무리 많아도 실패한 앱으로 간주합니다.

### UX 원칙

- 앱을 열면 **바로 촬영을 시작할 수 있어야 합니다.**
- 촬영 흐름이 **팝업/세팅 등으로 끊기지 않아야 합니다.**
- 선택 과정은 **설명이 없어도 이해될 정도로 직관적**이어야 합니다.
- 결과 화면은 **“저장 / 공유” 두 가지 행동에 집중**되도록 단순해야 합니다.
- 최종 목표는 **“3번 터치 안에 결과물”** 입니다.

---

## 🎯 MVP 범위 (1차 출시)

### 📸 촬영

- 8장 / 10장 **자동 연속 촬영**
- 타이머 (3초 기본, 옵션 변경 가능)
- 전/후면 카메라 전환 지원 (기본: 전면)
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
- 사진 순서 변경 (드래그 & 드롭 또는 스왑)

### 💾 결과

- 고해상도 콜라주 이미지 생성
- 갤러리(미디어 스토어)에 저장
- Android 공유 시트(Intent Chooser)를 통한 공유

### 🗂️ 보관함

- 결과물 리스트 (썸네일)
- 날짜별 정렬
- 삭제

---

## 🚫 의도적으로 제외하는 기능 (1차)

초기 Android 버전에서는 **아래 기능을 의도적으로 제외합니다.**

- 로그인 / 회원 기능
- 친구 / 팔로우 / 피드
- 댓글 / 좋아요 / 타임라인
- AI 보정 / AI 리터칭
- 프레임 마켓 / 유료 프레임 스토어
- QR 공유 (링크 기반 공유)

이 기능들을 초반에 모두 넣으려고 하면,  
정작 **카메라 UX / 결과물 퀄리티**를 지키지 못할 가능성이 큽니다.

1차 버전에서는 **“찍는 재미”와 “결과물의 예쁨”** 두 가지에만 집중합니다.

---

## 🎨 주요 화면

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

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM / Clean Architecture
- **Min SDK**: 24 이상 (Android 7.0+)
- **Target SDK**: 최신 안정 버전

### 카메라 & 이미지 처리

- **카메라**: CameraX
  - 전/후면 카메라
  - 타이머 기반 자동 연속 촬영
- **이미지 처리**: Coil / Bitmap + Android Graphics
  - 필터 적용 (초기에는 간단한 색 보정 위주)
  - 프레임 합성
  - 콜라주 렌더링

### 저장 & 데이터

- **이미지 저장**: MediaStore / 앱 전용 디렉터리
- **메타데이터**: Room 또는 DataStore
  - 세션 정보, 보관함 리스트 등

### 공유

- **공유**: Android 공유 인텐트 (ACTION_SEND)

---

## 🧱 데이터 구조 (Android 기준)

Android 버전에서도 iOS와 유사한 개념의 세션 구조를 사용합니다.

```kotlin
data class PhotoSession(
    val id: String,
    val captureCount: Int,          // 8 or 10
    val selectedCount: Int,         // 4 or 6
    val imageUris: List<String>,    // 로컬 URI 문자열
    val selectedIndexes: List<Int>, // 선택한 인덱스 순서
    val frameId: String,            // 적용된 프레임 ID
    val finalImageUri: String?,     // 최종 콜라주 이미지 URI
    val createdAt: Long             // epoch millis
)
```

---

## 🔑 기술 핵심 포인트 (Android)

1. **연속 촬영 엔진**
   - CameraX 기반 연속 촬영 루프
   - 타이머 + 셔터 템포 제어
   - 생명주기(Lifecycle)와 함께 안전하게 동작하도록 설계

2. **선택 제한 로직**
   - 4장 / 6장 **정확히 선택**해야 다음 단계로 이동
   - 선택 순서(`selectedIndexes`)를 보장
   - 초과 선택, 선택 취소 UX를 명확하게 처리

3. **프레임 렌더링**
   - Bitmap 기반 콜라주 렌더링
   - 고정 비율 / 해상도 유지 (인쇄 가능성까지 고려)
   - 메모리 사용량과 렌더링 속도 최적화

4. **저장 최적화**
   - 이미지 압축 품질/용량 밸런스 조절
   - IO 작업은 코루틴 + Dispatchers.IO 에서 처리
   - MediaStore를 통한 안전한 저장 및 갤러리 노출

---

## 📆 개발 단계 (6주 플랜 - Android)

### 1주차 — 구조 설계

- 전체 화면 흐름 정의
- `PhotoSession` 등 도메인 모델 설계
- Android 프로젝트 생성 및 모듈 구조 정의

### 2주차 — 홈 / 설정 / 권한

- 홈 화면 Compose UI
- 촬영 옵션(연속 장수, 컷 수, 타이머) 설정 화면
- 카메라 권한 요청 및 에러 처리 플로우

### 3주차 — 연속 촬영 기능

- CameraX 기본 세팅
- 타이머 + 자동 연속 촬영 구현
- 촬영 진행 상태 UI 반영

### 4주차 — 선택 화면 & 로직

- 썸네일 그리드 Compose UI
- 선택 개수 제한 / 검증
- 선택 순서 표시

### 5주차 — 프레임 & 결과 생성

- 프레임 레이아웃 정의
- Bitmap 기반 콜라주 렌더링
- 고해상도 결과 이미지 생성 & 저장

### 6주차 — 편집 / 저장 / 공유 / QA

- 필터 / 텍스트 / 날짜 옵션
- 공유 인텐트 연동
- 보관함 리스트 구현
- 실제 기기 테스트 및 성능/UX 튜닝

---

## 📁 프로젝트 구조 (예시)

```text
Pocket4Cut-Android/
├── app/
│   ├── src/main/java/com/pocket4cut/
│   │   ├── Pocket4CutApp.kt
│   │   ├── ui/
│   │   │   ├── home/
│   │   │   ├── capture/
│   │   │   ├── selection/
│   │   │   ├── frame/
│   │   │   ├── edit/
│   │   │   ├── result/
│   │   │   └── gallery/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── usecase/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   ├── storage/
│   │   │   └── mapper/
│   │   ├── camera/
│   │   ├── frame/
│   │   └── common/
│   └── src/androidTest / test ...
└── build.gradle 등
```

---

## 🚀 빠른 시작 (개발용)

### 필요 환경

- Android Studio 최신 버전 (Giraffe 이상 권장)
- JDK 17 권장
- Android SDK / 에뮬레이터 혹은 실제 기기

### 프로젝트 실행

```bash
# 1. 저장소 클론
git clone https://github.com/yourusername/pocket-4cut-android.git

# 2. Android Studio에서 프로젝트 열기
# File > Open > pocket-4cut-android 선택

# 3. Gradle Sync 완료 후
# Run ▶ 버튼 또는 Shift + F10
```

---

## 📊 프로젝트 상태

**현재 상태**: 🔴 기획 완료 / Android 구조 설계 예정

### 정리된 것

- 문제 정의 / 해결 방식
- 절대 원칙 / UX 원칙
- Android용 MVP 범위
- 데이터 구조 초안 (`PhotoSession`)
- 화면 플로우 / 프로젝트 구조 초안

### 다음 단계

1. Android 프로젝트 생성
2. CameraX 기반 촬영 구조 설계
3. 촬영 세션 로직 구현
4. 프레임/콜라주 렌더링 구조 정의
5. UI 와이어프레임 및 Compose 컴포넌트 설계

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
> “찍는 순간 자체를 콘텐츠로 만드는 앱”입니다.**

