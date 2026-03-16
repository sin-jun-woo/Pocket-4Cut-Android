package com.pocket4cut.domain.model

/**
 * 촬영 세션 도메인 모델
 */
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

