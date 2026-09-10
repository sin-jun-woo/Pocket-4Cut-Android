# 릴리즈 서명 & 빌드

## 1. 키스토어 생성 (최초 1회)

**Android Studio** → 하단 **Terminal** (내장 JDK 사용):

```powershell
cd D:\Pocket-4Cut-Android
.\scripts\generate-release-keystore.bat
```

또는 (실행 정책 오류 시):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generate-release-keystore.ps1
```

생성물 (Git에 올리지 말 것):

- `keystore/release.jks`
- `keystore.properties`
- `SIGNING_CREDENTIALS.txt` → USB/비밀번호 관리자에 **백업**

## 2. 릴리즈 빌드

저장소 루트에서 JDK 17 이상과 Gradle wrapper를 사용한다. 현재 설정은 AGP 9.0.1 / Gradle 9.1.0 / Build Tools 36.1.0이다. release는 R8 코드 최적화·난독화와 최적화된 리소스 축소를 수행한다.

```powershell
.\gradlew.bat :app:assembleRelease --console=plain
```

- APK: `app\build\outputs\apk\release\app-release.apk`

**Play Store 업로드 (권장):**

```powershell
.\gradlew.bat :app:bundleRelease --console=plain
```

- AAB: `app\build\outputs\bundle\release\app-release.aab`
- 난독화 매핑: `app\build\outputs\mapping\release\mapping.txt`

배포하는 APK/AAB와 같은 빌드에서 생성된 `mapping.txt`를 함께 보관한다. 격리된 buildDir로 검증했다면 해당 buildDir 아래의 산출물과 mapping을 사용한다. AAB 안에는 Play Console이 최적화 상태를 판정하는 `BUNDLE-METADATA/com.android.tools/r8.json`과 난독화 매핑이 포함되어야 한다. 로컬 빌드 결과만으로 Console 표시 변경을 확정하지 말고, 새 AAB 업로드와 처리 완료 후 앱 최적화 항목을 다시 확인한다.

## 3. Android Studio UI

`Build → Generate Signed Bundle / APK` → 기존 키 선택 시:

- Key store: `keystore/release.jks`
- `keystore.properties` / `SIGNING_CREDENTIALS.txt` 참고

## 4. keystore.properties 없을 때

`assembleRelease`는 **서명 없는** release APK가 나올 수 있음.  
반드시 1단계 스크립트를 먼저 실행하세요.
