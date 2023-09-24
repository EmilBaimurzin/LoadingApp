package com.loading.application.domain

import kotlinx.coroutines.flow.Flow

interface LoadingRepository {
    suspend fun downloadConfig(): Flow<Boolean>
    suspend fun loadConfig(): Flow<Boolean>
    suspend fun loadTask(): Flow<Pair<Int, Boolean>>
}