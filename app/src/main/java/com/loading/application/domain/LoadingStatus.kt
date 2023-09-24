package com.loading.application.domain

enum class LoadingStatus {
    PRE_LOAD,
    DOWNLOAD_CONFIG,
    LOAD_CONFIG,
    LOADING_CONFIG,
    LAUNCHING_TASKS,
    FINISH
}