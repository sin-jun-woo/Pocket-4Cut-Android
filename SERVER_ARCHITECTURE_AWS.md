# Pocket 4Cut - AWS 서버 아키텍처 & 운영 계획

## 📌 문서 목적

- 이 문서는 **Pocket 4Cut**의 서버/백엔드를 **AWS 기반**으로 설계하고 운영하기 위한 기준 문서다.
- 클라이언트(Android)에서 필요한 기능을 기준으로, **필수 서비스 선택, 인프라 구조, 보안, 배포/운영 전략**을 정의한다.
- 구현 단계에서 세부적인 설정 값(리전, 인스턴스 스펙, 요금 정책 등)은 이 문서를 베이스로 별도 Terraform/CloudFormation/코드 리포지토리에서 관리한다.

---

## 🎯 서버가 해야 할 일 (역할 정리)

현재 Android 앱은 **완전 로컬**로 MVP를 진행하고, 이후 Phase 6~10에서 아래 기능들을 서버로 옮긴다.

- **계정/로그인 (Phase 6)**
  - 회원 가입, 로그인/로그아웃, 토큰 발급/갱신
  - 계정 정보(닉네임, 프로필 이미지, 설정값 등) 저장
  - 기본적인 보안/비밀번호 정책

- **소셜 레이어 (Phase 7)**
  - 결과물(네컷 이미지) 메타데이터 업로드 (실제 이미지는 S3)
  - 피드 조회 (글로벌/친구 피드)
  - 팔로우/언팔로우, 좋아요

- **AI 보정 (Phase 8, 선택적으로 서버 연동)**
  - 서버사이드 AI 보정 API (필요 시)
  - 비동기 처리/큐잉 (요청량 많을 경우)

- **프레임 마켓 & 결제 (Phase 9)**
  - 프레임 메타데이터/가격/소유 여부
  - 구매/영수증 검증 (In-App Purchase 연동 시 서버 검증)

- **QR/링크 공유 (Phase 9)**
  - 결과물 공유용 링크/QR이 가리키는 **랜딩 페이지** 또는 딥링크 리졸버

---

## 🏗️ 전체 아키텍처 개요 (AWS)

### 1) 선택 기준

- **초기 트래픽 낮음 / 빠른 개발**이 중요 → 완전한 마이크로서비스보단 **심플한 백엔드 + 매니지드 서비스** 선호.
- Android 개발 위주라 서버는 가능하면 **서버리스 or 관리형**으로 부담 줄이기.
- 추후 확장(웹/Android/어드민 등)을 고려해 **표준 HTTP API + JWT 기반 인증** 사용.

### 2) 제안 구조 (v1)

```text
 Android 앱
   │
   │ HTTPS (REST/JSON, JWT)
   ▼
 Amazon API Gateway
   │
   ▼
 AWS Lambda (Node.js/TypeScript or Python)
   │
   ├── Amazon Cognito (사용자 풀, OAuth, 토큰 검증)
   ├── Amazon DynamoDB (유저, 피드, 좋아요, 팔로우 등)
   ├── Amazon S3 (이미지/썸네일/QR 이미지)
   └── Amazon SQS or EventBridge (AI 보정 등 비동기 처리용, 필요 시)

 + CloudFront (S3 정적 리소스/랜딩 페이지 캐싱)
 + CloudWatch (로그/메트릭/알람)
 + AWS WAF (필요 시, API Gateway 앞단 방어)
```

> 초기 버전에서는 **API Gateway + Lambda + DynamoDB + S3 + Cognito** 조합을 기본으로 하고,  
> 트래픽/요금/운영 복잡도를 보면서 ECS, RDS 등으로 확장 여부를 판단한다.

---

## 🧱 서비스별 역할 정의

### 1. 인증/계정 - Amazon Cognito

- **User Pool**
  - 이메일 + 비밀번호 기반 회원가입/로그인
  - Apple 로그인(“Sign in with Apple”) 연동
  - 비밀번호 정책(최소 길이, 특수문자 등) 설정
  - 이메일 검증(선택)

- **토큰**
  - Cognito가 발급하는 **ID Token / Access Token / Refresh Token** 사용
  - Android는 JWT를 로컬에 보관(EncryptedSharedPreferences/DataStore 등)하고, API 호출 시 Authorization 헤더에 Bearer 토큰으로 전송

- **권장 패턴**
  - **서버(Lambda)** 는 Cognito가 발급한 토큰 검증만 수행 → 자체 세션 관리 최소화
  - 중요 정보(비밀번호, 인증 플로우)는 Cognito 위임

### 2. API 계층 - API Gateway + Lambda

- **Amazon API Gateway**
  - REST API 엔드포인트 정의 (`/auth`, `/feed`, `/frames`, `/market`, `/share` 등)
  - JWT 인증 미들웨어 (Cognito Authorizer) 설정
  - Rate Limit / Throttling, CORS 설정

- **AWS Lambda**
  - 각 도메인별 Handler (예: `auth-login`, `feed-list`, `market-purchase` 등)
  - Node.js(또는 TypeScript)로 작성, 비즈니스 로직은 최대한 도메인 모듈로 분리
  - 공통 유틸: 응답 포맷, 에러 핸들링, 로깅, Cognito 토큰 파싱 등

### 3. 데이터 저장 - DynamoDB (v1)

> 초기에는 스키마리스 + 빠른 개발이 중요한 만큼 DynamoDB 사용.  
> 관계가 복잡해지거나 쿼리가 복잡해지면 RDS(PostgreSQL)로 일부 이전 고려.  
> **핵심은 테이블 구조보다 Access Pattern(조회 패턴)을 먼저 정의하는 것.**
> 그리고 그 다음 단계에서 **PK/SK/GSI 설계**를 Access Pattern에 맞춰 고정한다.

#### 3-1) 주요 Access Pattern (v1 기준)

- **글로벌 피드**
  - 최신 공개 세션을 시간 순으로 내려받기
  - 예: `GET /feed/global?cursor=...`
- **내 게시물**
  - 특정 사용자의 세션 목록(최신순)
  - 예: `GET /users/{userId}/sessions`
- **세션 상세**
  - sessionId로 단건 조회
  - 예: `GET /sessions/{sessionId}`
- **좋아요 상태**
  - 특정 세션에 대한 내 좋아요 여부
  - 특정 세션의 좋아요 수
- **팔로우 관계**
  - 내가 팔로우하는 유저 목록
  - 나를 팔로우하는 유저 수(필요 시)
- **프레임 마켓**
  - 모든 프레임(필터/정렬)
  - 내가 소유한 프레임 목록
- **공유 링크**
  - shareId → sessionId 매핑, 만료 여부 확인

> v1에서는 **글로벌 피드 + 내 게시물 + 좋아요 + 팔로우만** 커버하면 충분하고,  
> 친구 피드/추천 피드는 이후 버전에서 별도 설계한다.

#### 3-2) 테이블 예시

1) `users`
   - `userId` (PK, Cognito User Sub 또는 자체 UUID)
   - `nickname`
   - `profileImageUrl`
   - `createdAt`

2) `sessions` (네컷 결과 세션)
   - `sessionId` (PK)
   - `userId` (GSI)
   - `imageUrl` (S3 경로, 최종 콜라주)
   - `thumbnailUrl`
   - `createdAt`
   - `likeCount`
   - `commentCount` (Phase 7 이후)
   - `visibility` (public/private/friends)

3) `follows`
   - `userId` (PK)            # 내가 팔로우하는 목록 조회용
   - `followeeId` (SK)        # 한 줄이 한 명을 표현
   - `createdAt`

4) `likes`
   - `sessionId` (PK)         # 세션별 좋아요 목록
   - `userId` (SK)            # 세션 + 유저 조합으로 중복 좋아요 방지
   - `createdAt`

5) `frames`
   - `frameId` (PK)
   - `type` (4cut/6cut)
   - `isPremium` (무료/유료)
   - `price`
   - `metadata` (색상, 태그 등)

6) `purchases`
   - `userId` (PK)
   - `purchaseId` (SK)
   - `frameId`
   - `platform` (android 등)
   - `receipt` (영수증 데이터/해시)
   - `createdAt`

7) `shares`
   - `shareId` (PK)
   - `sessionId`
   - `ownerId`
   - `visibility` (public/friends/private)
   - `expiresAt`
   - `qrImageUrl` (선택)
   - `createdAt`
   - `status` (active/expired/deleted)

> 각 테이블에 대해, 실제 구현 시에는  
> - 어떤 Access Pattern을 지원하는지  
> - PK/SK/GSI가 각각 어떤 조회를 위한 것인지  
> 를 추가로 명시해서 설계 리스크를 줄인다.

### 4. 파일 저장 - S3 + CloudFront

#### 4-1) S3 버킷 구조

```text
s3://pocket4cut-prod/
  ├── originals/           # (필요 시) 업로드 원본
  ├── results/             # 최종 콜라주 이미지
  ├── thumbnails/          # 피드용 썸네일
  ├── qr/                  # QR 코드 이미지
  └── frames/              # 프레임 리소스(이미지/JSON 등)
```

#### 4-2) 저장 정책 (비용/운영 기준)

- **원본 업로드 정책**
  - 기본: **최종 결과물(콜라주)만 서버(S3)에 저장**하고, 촬영본(8/10장 원본)은 기기/로컬 우선.
  - 원본을 서버에 올리는 기능은 **추가 유료 옵션** 또는 별도 기능으로 처리.
- **썸네일**
  - 피드용 이미지는 별도 썸네일 생성(`thumbnails/`), 원본/결과 이미지보다 작은 사이즈로 저장.
- **정리 정책**
  - 오래된 공유 리소스(예: 1년 이상 미접속)는 배치로 정리 또는 저렴한 스토리지 클래스로 이동.
  - 삭제된 세션에 연결된 결과/썸네일/QR도 주기적으로 정리.

#### 4-3) 접근 방식

- 업로드: Android → 서버(Lambda)에서 **Presigned URL** 발급 → Android에서 S3 직접 업로드
- 조회: CloudFront 도메인으로 이미지 요청 (캐싱)

---

## 📡 주요 API 설계 (개요)

> 세부 스펙은 `API_SPECIFICATION.md`에서 클라이언트/서버 통합으로 정리.  
> 여기서는 **AWS 리소스 관점에서 필요한 엔드포인트 그룹**만 나열한다.

### 1. Auth

- `POST /auth/login` (필요 시 커스텀 플로우, 기본은 Cognito Hosted UI/SDK 권장)
- `POST /auth/logout`
- `GET /auth/me` (현재 유저 정보)

> 가능하면 Cognito SDK + Hosted UI로 처리하고, 백엔드는 `GET /auth/me` 정도만 필수.

### 2. User/Profile

- `GET /users/me`
- `PATCH /users/me` (닉네임, 프로필 이미지 URL 등)
- `GET /users/{userId}`

### 3. Feed

- v1 범위에서는 **글로벌 피드 + 내 게시물**까지만 제공하고,  
  친구 피드/추천 피드는 이후 버전(트래픽/요구 확인 후)에서 다룬다.

- `GET /feed/global?cursor=...`  → 공개 세션 전체 피드
- `GET /users/{userId}/sessions?cursor=...` → 특정 유저 게시물
- `POST /sessions` (콜라주 완료 후 메타데이터 업로드: sessionId, imageUrl, thumbnailUrl 등)
- `GET /sessions/{sessionId}`

### 4. Social (Follow / Like)

- `POST /users/{userId}/follow`
- `DELETE /users/{userId}/follow`
- `POST /sessions/{sessionId}/like`
- `DELETE /sessions/{sessionId}/like`

### 5. Frame Market

- `GET /frames` (무료/유료, 태그 필터 포함)
- `GET /frames/{frameId}`
- `GET /users/me/frames` (소유 프레임 목록)
- `POST /purchases`
  - 클라이언트에서 **인앱결제 완료 후 영수증/트랜잭션 정보**를 서버로 전달
  - 서버는 영수증을 **검증**하고, 검증 성공 시 `purchases`/소유 프레임 정보를 갱신
  - 즉, 서버는 **결제를 직접 처리하는 주체가 아니라, “검증/권한 서버”** 역할을 한다.

### 6. Share / QR

- `POST /share` → 공유용 링크/토큰 생성, QR 코드 이미지 생성
  - 입력: sessionId, 공개 범위(공개/친구/비공개), 만료 정책(기본값 사용)
  - 처리:
    - `shareId` 생성, `shares` 테이블에 저장 (sessionId, ownerId, visibility, expiresAt 등)
    - 필요 시 QR 코드 이미지를 생성해 S3 `qr/` 경로에 저장
- `GET /share/{shareId}` → 웹/앱에서 결과물 랜딩
  - 동작 정책:
    - 만료(`expiresAt`)가 지났으면 404 또는 만료 안내
    - visibility가 `private`이면 소유자만, `friends`면 친구 관계만 조회 가능
    - 세션/이미지가 삭제된 경우에도 적절한 안내 페이지(삭제됨) 노출

> 공유 링크는 **TTL(만료 시간)**, **공개 범위(visibility)**, **삭제 시 동작**을 반드시 정의하고,  
> S3/CloudFront 비용 및 abuse(남용) 가능성을 고려해 주기적인 정리 배치를 둔다.

---

## 🔐 보안 & 권한

- **인증**
  - 모든 민감 API는 **Cognito Authorizer** 적용 (JWT 검증).
  - Public 피드/공유 링크는 일부 엔드포인트만 익명 허용.

- **권한**
  - `userId`는 토큰에서 파싱, 본인 리소스만 수정 가능하게 서비스 레벨 검증.
  - 관리자/운영 도구가 필요해지면 별도 Admin Role + 별도 콘솔/툴 추가.

- **네트워크**
  - v1에서는 **Lambda를 VPC 밖에서** 운영하고, 관리형 서비스(API Gateway, DynamoDB, S3, Cognito)에 직접 접근한다.
  - RDS, ElastiCache, 내부 전용 자원 등 **VPC 의존 리소스가 도입되는 시점**에만 Lambda의 VPC 연결을 검토한다.

- **비밀 관리**
  - AWS Systems Manager Parameter Store 또는 Secrets Manager 사용 (3rd party 키, 서드파티 API 등).

---

## 🚀 배포 & 환경 전략

### 1. 환경 구성

- 최소 2개 환경:
  - **dev**: 개발/테스트용 (낮은 스펙, 비용 최소화)
  - **prod**: 실제 서비스용

- 각 환경별로 **별도 스택/리소스**:
  - 예: `pocket4cut-dev-users`, `pocket4cut-prod-users` 처럼 테이블/버킷 분리.

### 2. IaC (Infrastructure as Code)

- **우선순위**
  - 초기: AWS Console로 빠르게 PoC 만들고,  
  - 안정화되면 Terraform 또는 AWS CDK로 재정리.

- 권장:
  - TypeScript AWS CDK → Lambda 코드와 같은 언어/리포에서 관리 가능.

### 3. 배포 파이프라인

- GitHub Actions or AWS CodePipeline:
  - main 브랜치 푸시 → dev 환경 배포 (자동)
  - 태그/릴리즈 → prod 환경 배포 (승인 후)

- Lambda 배포:
  - Zip 업로드 or Docker 이미지 (ECR) 사용.

---

## 📊 모니터링 & 로깅

- **CloudWatch Logs**
  - Lambda 로그, 에러 스택, 성능 로그
  - 로그 포맷 통일 (requestId, userId, path 등)

- **CloudWatch Metrics**
  - Lambda 오류율, 지연 시간
  - API Gateway 4xx/5xx 비율

- **알람**
  - SNS + 이메일/슬랙 → 5xx 비율 급증, Lambda Error 증가, DynamoDB Throttle 등 알림.

---

## 🧪 테스트 전략 (서버 측)

- **단위 테스트**
  - Lambda 핸들러 수준에서 비즈니스 로직 테스트 (입력 → 출력).

- **통합 테스트**
  - dev 환경에 배포 후 Postman/자동화 스크립트로 주요 플로우 검증:
    - 회원가입/로그인 → 토큰 얻기
    - 결과물 업로드 → 피드 노출
    - 팔로우/좋아요 → 카운트/피드 반영

- **로드 테스트 (간단)**
  - k6, Artillery 등으로 READ 중심 시나리오 부하테스트 (피드 조회, 세션 조회).

---

## 🔮 이후 확장 아이디어

- **웹 뷰어**
  - 공유 링크를 눌렀을 때, S3/CloudFront 기반 웹 페이지에서 결과물을 보여주고 앱 설치 유도.

- **어드민 콘솔**
  - 프레임 마켓 관리, 유저 신고 처리, 통계 대시보드 등.
  - Amplify Console + React Admin 간단 구축 or 별도 Next.js + Cognito + Admin Role.

- **로그/분석**
  - CloudWatch Logs → OpenSearch/Datadog 등으로 보내서 쿼리/대시보드 강화.

---

## 📁 문서 메타

- **문서 버전**: 0.2.0 (Android 클라이언트 기준 반영)  
- **리전 제안**: ap-northeast-2 (서울) 또는 ap-northeast-1 (도쿄)  
- **관련 문서**:
  - `PROJECT_PLAN.md` (Phase 6~10 계획)
  - `README.md` (기술 스택 개요, Android)
  - `API_SPECIFICATION.md` (클라이언트/서버 API 상세 스펙 예정)

