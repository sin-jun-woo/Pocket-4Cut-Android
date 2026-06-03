package com.pocket4cut.legal

/**
 * Google Play · 앱 내 「개인정보 처리방침」 공통 본문.
 * 웹 호스팅용: [docs/privacy-policy.html](../../../docs/privacy-policy.html)
 */
object PrivacyPolicyContent {
    const val EFFECTIVE_DATE = "2026년 6월 3일"
    const val CONTACT_EMAIL = "sus3456@naver.com"

    val fullTextKo: String = """
Pocket 4Cut (포켓 네 컷) 개인정보 처리방침

시행일: $EFFECTIVE_DATE
최종 개정일: $EFFECTIVE_DATE

1. 총칙
본 앱(Pocket 4Cut)은 「개인정보 보호법」 등 관련 법령을 준수합니다.
운영자: Pocket 4Cut 개발자
문의: $CONTACT_EMAIL
패키지명: com.pocket4cut

2. 요약
· 서버로 개인정보를 전송·저장하지 않습니다.
· 회원가입, 로그인, 광고·분석 SDK를 사용하지 않습니다.
· 카메라·사진은 기기 안에서만 촬영·편집·저장합니다.

3. 처리하는 정보 (기기 내)
· 촬영 사진 및 콜라주 결과물: 촬영·편집·저장·공유 기능 제공
· 보관함 메타데이터: 세션 ID, 경로, 생성 시각 등
· 앱 설정: 카메라 방향, 카운트다운, 자동 저장 옵션 등
· 문의: 이용자가 메일 앱으로 직접 보낸 내용만 수신

4. 접근 권한
· 카메라: 연속 촬영 (필수)
· 사진·동영상: 이용자가 갤러리 저장을 선택할 때 등 (해당 기능 사용 시)
본 앱은 사진첩의 기존 사진을 임의로 업로드하지 않습니다.

5. 보관 및 파기
· 앱 삭제 또는 앱 내 데이터 삭제로 기기 내 데이터를 제거할 수 있습니다.
· 갤러리에 저장한 파일은 이용자가 직접 삭제합니다.

6. 제3자 제공
· 운영자는 정보를 판매·제공하지 않습니다.
· 공유는 이용자가 선택한 앱으로 직접 전송됩니다.

7. 위탁
· 개인정보 처리 위탁을 하지 않습니다.

8. 이용자 권리
· 권한 철회, 데이터 삭제, 문의($CONTACT_EMAIL)가 가능합니다.

9. 아동
· 만 14세 미만을 대상으로 하지 않으며, 고의로 수집하지 않습니다.

10. 안전 조치
· 외부 서버 미사용, 추적 SDK 미사용

11. 기기 백업
· 이용자가 Android 자동 백업을 켠 경우 앱 데이터가 계정 백업에 포함될 수 있습니다.

12. 변경
· 변경 시 시행일을 갱신하여 앱·웹에 공지합니다.

13. 문의
Pocket 4Cut 개발자
$CONTACT_EMAIL

※ 전문(웹): https://sin-jun-woo.github.io/Pocket-4Cut-Android/privacy-policy.html
""".trimIndent()
}
