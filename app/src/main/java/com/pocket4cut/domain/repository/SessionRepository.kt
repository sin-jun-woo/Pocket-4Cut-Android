package com.pocket4cut.domain.repository

import com.pocket4cut.domain.model.PhotoSession

/**
 * 세션 메타데이터 저장소 인터페이스
 */
interface SessionRepository {
    suspend fun save(session: PhotoSession)
    suspend fun getAll(): List<PhotoSession>
    suspend fun getById(id: String): PhotoSession?
    suspend fun delete(id: String)
}

