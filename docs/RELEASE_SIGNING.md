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

```powershell
.\gradlew.bat assembleRelease
```

- APK: `app\build\outputs\apk\release\app-release.apk`

**Play Store 업로드 (권장):**

```powershell
.\gradlew.bat bundleRelease
```

- AAB: `app\build\outputs\bundle\release\app-release.aab`

## 3. Android Studio UI

`Build → Generate Signed Bundle / APK` → 기존 키 선택 시:

- Key store: `keystore/release.jks`
- `keystore.properties` / `SIGNING_CREDENTIALS.txt` 참고

## 4. keystore.properties 없을 때

`assembleRelease`는 **서명 없는** release APK가 나올 수 있음.  
반드시 1단계 스크립트를 먼저 실행하세요.
