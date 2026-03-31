package com.pocket4cut.core.util

/**
 * 앱 전역 상수
 */
object Constants {
    // 촬영 설정
    const val CAPTURE_COUNT_2_CUT = 4
    const val CAPTURE_COUNT_4_CUT = 8
    const val CAPTURE_COUNT_6_CUT = 10
    const val SELECT_COUNT_2_CUT = 2
    const val SELECT_COUNT_4_CUT = 4
    const val SELECT_COUNT_6_CUT = 6
    
    // 타이머 설정
    const val COUNTDOWN_SECONDS = 10
    const val CAPTURE_INTERVAL_SECONDS = 2
    
    // 결과 이미지 품질
    const val RESULT_IMAGE_QUALITY = 98
    const val RESULT_IMAGE_MAX_WIDTH = 2560

    /** 촬영 세션 단일 컷 저장 후 long edge 상한 (iOS downscaleForSession 대응) */
    const val CAPTURE_LONG_EDGE_MAX = 2048

    const val CAPTURE_JPEG_QUALITY = 92
}

