# Pocket 4Cut Android - 프로젝트 설정 가이드

## 1단계: Android Studio 프로젝트 생성

1. Android Studio 실행
2. `File > New > New Project...` 선택
3. **Phone and Tablet > Empty Activity (Compose)** 또는 **Empty Views Activity** 선택 (Compose 권장)
4. 다음 정보 입력:
   - **Name**: `Pocket4Cut`
   - **Package name**: `com.pocket4cut` (또는 본인 도메인)
   - **Save location**: `Pocket-4Cut-Android` 폴더
   - **Language**: Kotlin
   - **Minimum SDK**: API 24 이상 (권장 API 26+)
   - **Build configuration language**: Kotlin DSL (권장) 또는 Groovy

5. `Finish` 클릭

---

## 2단계: 프로젝트 구조 생성

`app/src/main/java` (또는 `kotlin`) 아래에 패키지/폴더 구조를 만든다:

```
com.pocket4cut/
├── presentation/
│   ├── home/
│   ├── capture/
│   ├── selection/
│   ├── frame/
│   ├── edit/
│   ├── result/
│   └── gallery/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── storage/
│   └── local/
├── camera/
├── frame/
├── core/
│   ├── util/
│   ├── extensions/
│   └── constants/
└── di/
```

**방법**: 프로젝트 뷰에서 `com.pocket4cut` 우클릭 → New → Package 또는 Directory 생성 후 반복

---

## 3단계: AndroidManifest 권한 설정

카메라 및 저장소 접근을 위해 `AndroidManifest.xml`에 다음 권한을 추가한다.

```xml
<manifest ...>
    <!-- 카메라 -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="true" />

    <!-- 갤러리/저장 (버전별) -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" android:minSdkVersion="33" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />
    <!-- Android 10~12: requestLegacyExternalStorage 또는 MediaStore 사용 -->
    ...
</manifest>
```

- Android 13(API 33) 이상: `READ_MEDIA_IMAGES` 사용
- 갤러리 저장: MediaStore API 사용 시 스코프 저장소 정책에 맞게 처리

---

## 4단계: Gradle 의존성 (권장)

`app/build.gradle.kts` (또는 `.gradle`)에 예시:

- **Jetpack Compose**: UI
- **ViewModel, Lifecycle**: `androidx.lifecycle:lifecycle-viewmodel-compose`
- **Navigation**: `androidx.navigation:navigation-compose`
- **CameraX**: `androidx.camera:camera-*`
- **Room**: `androidx.room:room-*` (메타데이터 저장 시)
- **Hilt**: `com.google.dagger:hilt-android`, `androidx.hilt:hilt-navigation-compose`

필요 시 Coil/Glide(이미지 로딩), DataStore 등 추가.

---

## 5단계: 빌드 및 실행

1. `Build > Clean Project` 후 `Build > Rebuild Project`
2. 에뮬레이터 또는 실제 기기 선택 후 Run (▶)

**참고**: 카메라 기능은 실제 기기에서 테스트하는 것을 권장한다.

---

## 다음 단계

프로젝트가 생성되면 다음 순서로 개발한다:

1. Domain 모델 구현 (PhotoSession 등)
2. Camera 모듈 (CaptureEngine, CameraX) 구현
3. Data Layer (ImageStorage, SessionRepository) 구현
4. Presentation (각 화면 Composable + ViewModel) 구현
5. Frame 렌더링 및 공유(Intent) 연동

자세한 일정은 `PROJECT_PLAN.md`를 참고한다.
