# Pocket 4Cut - 데이터 저장 구조 (Android)

## 📊 개요

- **이미지 저장**: 앱 전용 디렉터리 (`getExternalFilesDir` / `context.filesDir`) + 필요 시 MediaStore
- **메타데이터**: Room DB 또는 DataStore (세션 목록, 설정)
- **네트워크**: 사용하지 않음 (완전 로컬)

---

## 🗂️ 도메인 모델: PhotoSession

```kotlin
data class PhotoSession(
    val id: String,                    // UUID 문자열
    val captureCount: Int,              // 8 or 10
    val selectedCount: Int,            // 4 or 6
    val imagePaths: List<String>,      // 촬영본 로컬 경로
    val selectedIndexes: List<Int>,    // 선택한 인덱스 순서 (4 or 6개)
    val frameId: String,              // 적용된 프레임 ID
    val finalImagePath: String?,       // 최종 콜라주 이미지 경로
    val createdAt: Long               // epoch millis
)
```

- **저장 시점**: 촬영 완료 후 임시 세션으로 메모리 유지 → 결과 이미지 저장 후 메타데이터만 영구 저장 (Room/DataStore)
- **보관함**: `finalImagePath`가 있는 세션만 리스트에 표시

---

## 📁 파일 저장 구조 (Android)

### 1. 촬영본 (세션별)

```
{context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/captures/
    └── {sessionId}/
        ├── 0.jpg
        ├── 1.jpg
        └── ...
```

- 세션 ID별 폴더에 연속 촬영 이미지 저장
- 결과 저장 후 필요 시 정리(삭제) 가능
- `getExternalFilesDir` 사용 시 앱 삭제 시 함께 삭제됨

### 2. 최종 콜라주 이미지

```
.../Pictures/results/
    └── {sessionId}_result.jpg
```

또는

```
.../Pictures/results/
    └── {yyyy-MM-dd_HHmmss}.jpg
```

- 고해상도 JPEG (Bitmap.CompressFormat.JPEG, quality 90 등)
- 갤러리 저장 시 MediaStore API로 해당 파일을 공용 갤러리에 등록

### 규칙

- 파일명: UUID 또는 `날짜_시간` 형식으로 중복 방지
- 결과물 저장 시 압축 품질/해상도 정책 문서화 (예: max 1920px, quality 0.9)
- 앱 삭제 시 앱 전용 디렉터리 전체 삭제됨

---

## 💾 메타데이터 저장 (Room 예시)

### 저장 단위

- **세션 목록**: Room `@Entity`로 `PhotoSession` (또는 DTO) 테이블
- **DAO**: `getAll()`, `getById()`, `insert()`, `delete()`

### Room 엔티티 예시

```kotlin
@Entity(tableName = "photo_sessions")
data class PhotoSessionEntity(
    @PrimaryKey val id: String,
    val captureCount: Int,
    val selectedCount: Int,
    val imagePaths: String,           // JSON 배열 문자열 또는 별도 테이블
    val selectedIndexes: String,
    val frameId: String,
    val finalImagePath: String?,
    val createdAt: Long
)
```

### DataStore 사용 시 (설정/간단 리스트)

- 세션 목록을 JSON으로 직렬화해 DataStore에 저장하는 방식도 가능 (소규모일 때)
- 목록 조회 시 `createdAt` 기준 정렬

---

## 🔧 ImageStorage 역할 (Android)

| 메서드 | 설명 |
|--------|------|
| saveCapture(imageBytes: ByteArray, sessionId: String, index: Int): String | 촬영본 1장 저장, 경로 반환 |
| loadCaptures(sessionId: String): List<Bitmap> | 세션별 촬영본 로드 (또는 경로만 반환) |
| saveResult(bitmap: Bitmap, sessionId: String): String | 최종 콜라주 저장, 경로 반환 |
| deleteSessionFiles(sessionId: String) | 세션 폴더 및 결과 파일 삭제 |
| saveToGallery(path: String) | MediaStore로 갤러리 저장 |

---

## 🔧 SessionRepository 역할 (Android)

| 메서드 | 설명 |
|--------|------|
| save(session: PhotoSession) | 세션 메타데이터 저장 (Room insert/update) |
| getAll(): List<PhotoSession> | 전체 목록 (날짜 역순) |
| getById(id: String): PhotoSession? | 단건 조회 |
| delete(id: String) | 메타데이터 및 연관 파일 삭제 (ImageStorage 연동) |

---

## 📈 성능/용량 고려

- **촬영본**: 촬영 후 바로 디스크에 저장하여 메모리 부담 감소
- **결과물**: 1회 생성 후 경로만 보관, 중복 생성 지양
- **목록**: 필요 시 페이징 또는 최근 N개만 로드 (Room PagingSource)

---

## 🧪 테스트 데이터

- 개발 시 `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)` 하위 `captures`, `results` 경로 확인
- 에뮬레이터/실기기: `adb shell run-as <package> ls ...` 또는 로그로 경로 출력하여 검증

---

## 🎨 프레임 정의 및 저장 (요약, Android)

프레임 레이아웃은 코드/리소스 내에서 다음 모델로 관리한다.

```kotlin
data class FrameDefinition(
    val id: String,                  // 예: "4cut_classic_white"
    val name: String,               // 사용자 노출용 이름
    val type: FrameType,            // FOUR_CUT or SIX_CUT
    val thumbnailName: String,      // drawable 리소스 이름
    val backgroundColorHex: String,
    val accentColorHex: String?,
    val slots: List<FrameSlot>      // 각 사진 슬롯 위치/크기
)

enum class FrameType { FOUR_CUT, SIX_CUT }

data class FrameSlot(
    val index: Int,       // 0~3 또는 0~5
    val x: Float,         // 0.0~1.0 (정규화 좌표)
    val y: Float,
    val width: Float,
    val height: Float
)
```

- 프레임 ID 규칙: `{컷수}cut_{스타일명}_{색상}` (예: `4cut_classic_white`)
- 좌표는 0.0~1.0 정규화 값으로 정의하고, 실제 렌더링 해상도에 맞게 스케일

---

**문서 버전**: 2.0.0 (Android 기준)  
**문서 위치**: `/Pocket-4Cut-Android/DATA_STORAGE.md`
