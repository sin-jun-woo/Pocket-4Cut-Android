# Pocket 4Cut - 에러 처리 가이드 (Android)

## 🎯 에러 처리 원칙

1. **사용자 친화적**: 기술 용어 없이 쉬운 문구로 안내
2. **명확한 안내**: 무엇이 문제인지, 어떻게 할 수 있는지 제시
3. **촬영 흐름 보호**: 촬영 중 다이얼로그로 흐름을 자주 끊지 않기
4. **복구 가능 시 재시도**: 저장/공유 실패 시 "다시 시도" 제공

### 에러 레벨

| 레벨 | 설명 | 처리 방식 |
|------|------|-----------|
| **Critical** | 앱/카메라 동작 불가 | 안내 화면 + 설정 이동 유도 |
| **Major** | 촬영/저장/공유 실패 | AlertDialog + 재시도 또는 설정 이동 |
| **Minor** | 부가 기능 실패 | Snackbar/Toast 또는 조용한 복구 |
| **Silent** | 사용자 영향 없음 | 로그만 기록 |

---

## ❌ 커스텀 에러 정의 (Kotlin 예시)

### 1. 카메라/권한

```kotlin
sealed class CameraError : Exception() {
    object PermissionDenied : CameraError()
    object NotAvailable : CameraError()
    data class CaptureFailed(val reason: String?) : CameraError()
}

fun CameraError.userMessage(): String = when (this) {
    CameraError.PermissionDenied -> "카메라 권한이 필요해요. 설정에서 허용해주세요."
    CameraError.NotAvailable -> "카메라를 사용할 수 없어요."
    is CameraError.CaptureFailed -> "촬영에 실패했어요. 다시 시도해주세요."
}
```

### 2. 선택 검증

```kotlin
sealed class SelectionError : Exception() {
    data class InvalidCount(val required: Int) : SelectionError()
}

fun SelectionError.userMessage(): String = when (this) {
    is SelectionError.InvalidCount -> "${this.required}장을 선택해주세요."
}
```

### 3. 저장/파일

```kotlin
sealed class StorageError : Exception() {
    object SaveFailed : StorageError()
    object NoSpace : StorageError()
    object GalleryDenied : StorageError()
}

fun StorageError.userMessage(): String = when (this) {
    StorageError.SaveFailed -> "저장에 실패했어요. 다시 시도해주세요."
    StorageError.NoSpace -> "저장 공간이 부족해요."
    StorageError.GalleryDenied -> "갤러리 저장 권한이 필요해요. 설정에서 허용해주세요."
}
```

### 4. 렌더링/프레임

```kotlin
object RenderError : Exception("이미지 생성 실패")

fun RenderError.userMessage(): String = "이미지 생성에 실패했어요. 다시 시도해주세요."
```

---

## 🎨 UI 에러 표시 (Android)

### 권한 거부 시

- **카메라**: 촬영 화면 대신 "카메라 권한이 필요해요" 문구 + "설정으로 이동" 버튼 (Compose 또는 Activity)
- **갤러리**: 저장 시도 시 AlertDialog → "설정으로 이동" / "취소"
- 설정 이동: `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` + 패키지 URI

### 촬영 실패 시

- 카운트다운 중 중단되면 "다시 촬영" 버튼 노출
- Critical이면 한 번에 하나의 메시지만 표시

### 저장/공유 실패 시

- AlertDialog: 사용자 메시지 + "다시 시도" / "확인"
- 재시도 시 같은 액션 다시 수행

---

## 📝 에러 메시지 목록 (한국어)

| 상황 | 사용자 메시지 |
|------|---------------|
| 카메라 권한 없음 | 카메라 권한이 필요해요. 설정에서 허용해주세요. |
| 카메라 사용 불가 | 카메라를 사용할 수 없어요. |
| 촬영 실패 | 촬영에 실패했어요. 다시 시도해주세요. |
| 4/6장 미선택 | 4장(또는 6장)을 선택해주세요. |
| 이미지 저장 실패 | 저장에 실패했어요. 다시 시도해주세요. |
| 저장 공간 부족 | 저장 공간이 부족해요. |
| 갤러리 권한 없음 | 갤러리 저장 권한이 필요해요. 설정에서 허용해주세요. |
| 이미지 생성 실패 | 이미지 생성에 실패했어요. 다시 시도해주세요. |

---

## 🔄 ViewModel에서 처리 패턴 (Android)

```kotlin
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showPermissionRationale = MutableStateFlow(false)
    val showPermissionRationale: StateFlow<Boolean> = _showPermissionRationale.asStateFlow()

    fun handleError(error: Throwable) {
        val message = when (error) {
            is CameraError.PermissionDenied -> {
                _showPermissionRationale.value = true
                error.userMessage()
            }
            is CameraError -> error.userMessage()
            else -> "오류가 발생했어요. 다시 시도해주세요."
        }
        _errorMessage.value = message
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", application.packageName, null)
        }
        application.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
```

---

## 🧪 테스트 시나리오 (주요 예외 상황)

- **카메라 권한 거부 후 촬영 시도**
  - 첫 실행에서 권한 거부 → `촬영 시작` 시 권한 안내 + 설정 이동 버튼 노출
- **사진 저장 권한 거부 후 갤러리 저장 시도**
  - 결과 화면에서 `갤러리에 저장` 탭 → 권한 안내 + 설정 이동 버튼
- **연속 촬영 중 앱 백그라운드 이동**
  - capturing 상태에서 홈 버튼 → 복귀 시 세션을 Failed 처리하고 홈 또는 재시작 안내
- **연속 촬영 중 오류 발생**
  - CaptureEngine 내부 오류 시 `CameraError.CaptureFailed` 로 처리, 다시 촬영 유도
- **이미지 렌더링 실패**
  - `RenderError` 발생 시 편집 화면으로 돌아가 재시도 안내
- **저장 공간 부족**
  - `StorageError.NoSpace` 발생 시, 저장 공간 확보 후 다시 시도하라는 안내
- **결과물이 너무 많을 때 정리 정책**
  - (예: 최근 N개만 유지, 나머지는 사용자 동의 후 일괄 삭제) 정책에 따라 정리 시나리오 검증

---

**문서 버전**: 2.0.0 (Android 기준)  
**문서 위치**: `/Pocket-4Cut-Android/ERROR_HANDLING.md`
