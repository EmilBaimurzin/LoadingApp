package com.loading.application.domain

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class LoadingRepositoryImpl : LoadingRepository {
    private val downloadDelay: Long = 100
    private val loadDelay: Long = 20
    private val randomTaskDelay = (100..10000).random().toLong() / 30
    override suspend fun downloadConfig(): Flow<Boolean> {
        Log.d("config","downloading config")
        return flow {
            repeat(40) {
                delay(downloadDelay)
                emit(it == 39)
                if (it == 39) {
                    Log.d("config","config successfully downloaded")
                }
            }
        }
    }

    override suspend fun loadConfig(): Flow<Boolean> {
        Log.d("config","loading config")
        return flow {
            repeat(30) {
                delay(loadDelay)
                emit(it == 29)
                Log.d("config","config successfully loaded")
            }
        }
    }

    override suspend fun loadTask(): Flow<Pair<Int, Boolean>> {
        Log.d("tasks","starting tasks")
        return flow {
            repeat(30) {
                emit((it + 1) to ((it + 1) == 10 || (it + 1) == 20 || (it + 1) == 30))
                if (it == 29) {
                    Log.d("tasks","tasks successfully finished")
                }
            }
        }.onEach {
            delay(randomTaskDelay)
        }
    }
}