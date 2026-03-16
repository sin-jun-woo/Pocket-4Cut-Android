# Pocket 4Cut - 안드로이드 도메인 & Repository / UseCase 명세

> 이 앱은 **완전 로컬(Android)** 앱을 1차 목표로 하며, 네트워크 API는 사용하지 않는다.  
> 여기서는 **Kotlin + MVVM + UseCase** 기준으로 도메인 레이어와 Repository 인터페이스를 정의한다.

---

## 📚 개요

### 레이어 구조 (Android)

```text
View (Jetpack Compose / Activity / Fragment)
        ↓
ViewModel (AndroidX ViewModel / State 관리)
        ↓
Use Case (비즈니스 로직)
        ↓
Repository / Service (추상화)
        ↓
Storage / CameraX / ImageProcessor (구현체)
```

---

## 🗂️ Repository / Storage 인터페이스

### 1. SessionRepository

세션 메타데이터 및 목록 관리 (Room / DataStore 등 구현)

```kotlin
interface SessionRepository {
    /** 세션 저장 (결과 이미지 경로 포함) */
    suspend fun save(session: PhotoSession)

    /** 전체 목록 (날짜 역순) */
    suspend fun getAll(): List<PhotoSession>

    /** ID로 조회 */
    suspend fun getById(id: String): PhotoSession?

    /** 세션 삭제 (메타데이터 + 연관 파일) */
    suspend fun delete(id: String)
}
```

> 구현체 예시: `RoomSessionRepository`, 내부적으로 Room DAO 또는 DataStore 사용.

---

### 2. ImageStorage

이미지 파일 저장/로드/삭제 (앱 전용 디렉터리 + MediaStore 저장)

```kotlin
interface ImageStorage {
    /** 촬영본 1장 저장 (세션별 폴더) */
    suspend fun saveCapture(
        imageBytes: ByteArray,
        sessionId: String,
        index: Int
    ): String

    /** 세션별 촬영본 경로 목록 */
    suspend fun getCapturePaths(sessionId: String): List<String>

    /** 최종 콜라주 이미지 저장 (파일 경로 반환) */
    suspend fun saveResult(
        bitmap: Bitmap,
        sessionId: String
    ): String

    /** 세션 폴더 및 결과 파일 삭제 */
    suspend fun deleteSessionFiles(sessionId: String)

    /** 갤러리(사진 앱)에 저장 */
    suspend fun saveToGallery(filePath: String)
}
```

구현 시 내부적으로는:

- `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)` 하위에 세션별 폴더 생성  
- MediaStore API를 사용해 최종 결과물을 시스템 갤러리에 등록

---

## 🎯 Use Cases

### Capture

#### StartCaptureUseCase (또는 CaptureEngine 래핑)

- **입력**: 촬영 장수(8/10), 타이머 간격(초)
- **출력**: `List<Bitmap>` 또는 로컬 파일 경로 리스트
- **역할**: `CaptureEngine` 호출, 권한 확인, 타이머 기반 연속 촬영 트리거

```kotlin
class StartCaptureUseCase(
    private val captureEngine: CaptureEngine
) {
    suspend operator fun invoke(
        captureCount: Int,
        intervalSeconds: Int
    ): List<Bitmap> {
        return captureEngine.captureSequence(
            captureCount = captureCount,
            intervalSeconds = intervalSeconds
        )
    }
}
```

> 구현 시 `CaptureSessionState`(Idle, Ready, Countdown, Capturing, Failed 등)를 함께 관리해서  
> ViewModel 상태(StateFlow/MutableState)를 통해 UI를 제어한다.

---

### Selection

#### ValidateSelectionUseCase

- **입력**: 선택된 인덱스 배열, 필요 개수(4 or 6)
- **출력**: 성공/실패 (예외 또는 Result)
- **역할**: 정확히 4장 또는 6장 선택했는지 검증

```kotlin
class ValidateSelectionUseCase {
    operator fun invoke(
        selectedIndexes: List<Int>,
        requiredCount: Int
    ) {
        if (selectedIndexes.size != requiredCount) {
            throw SelectionException.InvalidCount(requiredCount)
        }
    }
}
```

#### BuildSessionUseCase (선택)

- **입력**: 촬영 이미지 경로들, `selectedIndexes`, `frameId`, `captureCount`, `selectedCount`
- **출력**: `PhotoSession` (아직 `finalImagePath` 없음)
- **역할**: 선택 결과로 `PhotoSession` 생성

```kotlin
class BuildSessionUseCase {
    operator fun invoke(
        capturePaths: List<String>,
        selectedIndexes: List<Int>,
        frameId: String,
        captureCount: Int,
        selectedCount: Int
    ): PhotoSession {
        val id = UUID.randomUUID().toString()
        return PhotoSession(
            id = id,
            captureCount = captureCount,
            selectedCount = selectedCount,
            imagePaths = capturePaths,
            selectedIndexes = selectedIndexes,
            frameId = frameId,
            finalImagePath = null,
            createdAt = System.currentTimeMillis()
        )
    }
}
```

---

### Frame & Edit

#### RenderCollageUseCase

- **입력**: 선택된 이미지들(경로 또는 `Bitmap`), `frameId`, 필터 옵션, 텍스트/날짜 옵션
- **출력**: `Bitmap` (고해상도 콜라주)
- **역할**: `CollageRenderer` 호출, 필터/프레임 합성

```kotlin
class RenderCollageUseCase(
    private val collageRenderer: CollageRenderer
) {
    suspend operator fun invoke(
        imagePaths: List<String>,
        selectedIndexes: List<Int>,
        frameId: String,
        options: EditOptions
    ): Bitmap {
        return collageRenderer.render(
            imagePaths = imagePaths,
            selectedIndexes = selectedIndexes,
            frameId = frameId,
            options = options
        )
    }
}
```

---

### Save & Share

#### SaveResultUseCase

- **입력**: `PhotoSession`(finalImagePath 채움 대상), 생성된 `Bitmap`
- **역할**: `ImageStorage`에 결과 저장, `SessionRepository`에 세션 저장
- **출력**: 저장된 경로 또는 `Unit`

```kotlin
class SaveResultUseCase(
    private val imageStorage: ImageStorage,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        session: PhotoSession,
        resultImage: Bitmap
    ): String {
        val path = imageStorage.saveResult(resultImage, session.id)
        val updated = session.copy(finalImagePath = path)
        sessionRepository.save(updated)
        return path
    }
}
```

#### ShareResultUseCase (또는 View 레벨에서 직접)

- **입력**: 결과 이미지 경로
- **역할**: Android 공유 시트(`Intent.ACTION_SEND`) 호출
- **출력**: 없음 (UI/Activity 책임)

```kotlin
class ShareResultUseCase(
    private val context: Context
) {
    fun invoke(resultPath: String) {
        val uri = File(resultPath).toUri()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "네 컷 공유하기")
        )
    }
}
```

---

### Gallery

#### GetGallerySessionsUseCase

- **입력**: 없음 (또는 페이징 파라미터)
- **출력**: `List<PhotoSession>`
- **역할**: `SessionRepository.getAll()` 호출, 정렬

```kotlin
class GetGallerySessionsUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): List<PhotoSession> {
        return sessionRepository.getAll()
            .sortedByDescending { it.createdAt }
    }
}
```

#### DeleteSessionUseCase

- **입력**: `sessionId: String`
- **역할**: `SessionRepository.delete`, `ImageStorage.deleteSessionFiles`
- **출력**: `Unit` 또는 예외

```kotlin
class DeleteSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val imageStorage: ImageStorage
) {
    suspend operator fun invoke(sessionId: String) {
        imageStorage.deleteSessionFiles(sessionId)
        sessionRepository.delete(sessionId)
    }
}
```

---

## 🎭 도메인 모델 (요약, Android)

```kotlin
data class PhotoSession(
    val id: String,
    val captureCount: Int,          // 8 or 10
    val selectedCount: Int,         // 4 or 6
    val imagePaths: List<String>,   // 촬영본 로컬 경로
    val selectedIndexes: List<Int>, // 선택한 인덱스 순서
    val frameId: String,            // 적용된 프레임 ID
    val finalImagePath: String?,    // 최종 콜라주 이미지 경로
    val createdAt: Long             // epoch millis
)
```

추가적으로 프레임 정의, 편집 옵션, 에러 타입은 별도 파일(예: `FrameDefinition`, `EditOptions`, `SelectionException`)에서 관리한다.

---

## 📊 사용 예시 (ViewModel 기준)

### 촬영 완료 후 세션 생성

```kotlin
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val startCaptureUseCase: StartCaptureUseCase,
    private val imageStorage: ImageStorage,
    private val buildSessionUseCase: BuildSessionUseCase
) : ViewModel() {

    fun startCapture(frameType: FrameType) {
        viewModelScope.launch {
            val captureCount = when (frameType) {
                FrameType.FOUR_CUT -> 8
                FrameType.SIX_CUT -> 10
            }
            val images = startCaptureUseCase(
                captureCount = captureCount,
                intervalSeconds = 3
            )
            val sessionId = UUID.randomUUID().toString()
            val paths = images.mapIndexed { index, bitmap ->
                imageStorage.saveCapture(
                    imageBytes = bitmap.toPngBytes(),
                    sessionId = sessionId,
                    index = index
                )
            }
            // 다음 화면(Selection)으로 paths, sessionId 전달
        }
    }
}
```

### 결과 저장

```kotlin
@HiltViewModel
class ResultViewModel @Inject constructor(
    private val renderCollageUseCase: RenderCollageUseCase,
    private val saveResultUseCase: SaveResultUseCase
) : ViewModel() {

    fun saveResult(session: PhotoSession, options: EditOptions) {
        viewModelScope.launch {
            val collage = renderCollageUseCase(
                imagePaths = session.imagePaths,
                selectedIndexes = session.selectedIndexes,
                frameId = session.frameId,
                options = options
            )
            saveResultUseCase(session, collage)
            // UI에 저장 완료 이벤트 전달
        }
    }
}
```

### 보관함 목록

```kotlin
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getGallerySessionsUseCase: GetGallerySessionsUseCase
) : ViewModel() {

    var uiState by mutableStateOf(GalleryUiState())
        private set

    fun loadSessions() {
        viewModelScope.launch {
            val sessions = getGallerySessionsUseCase()
            uiState = uiState.copy(sessions = sessions)
        }
    }
}
```

---

## 🔒 권한 (Android)

| 권한 | 용도 |
|------|------|
| `android.permission.CAMERA` | 연속 촬영 |
| `android.permission.READ_MEDIA_IMAGES` (또는 `READ_EXTERNAL_STORAGE`) | 촬영본/갤러리 읽기 (선택) |
| `android.permission.WRITE_EXTERNAL_STORAGE` (Android 10 이하) | 결과물 갤러리 저장 |

- Android 13(Tiramisu) 이상에서는 `READ_MEDIA_IMAGES` 권장.
- 실제 요청은 `ActivityResultContracts.RequestPermission(s)` 또는 `Accompanist Permissions` 등을 통해 수행.

---

**문서 버전**: 2.0.0 (Android 기준으로 재작성)  
**문서 위치**: `/Pocket-4Cut-Android/API_SPECIFICATION.md`
