package com.loading.application.ui.loading

import androidx.lifecycle.ViewModel
import com.loading.application.domain.LoadingRepositoryImpl
import com.loading.application.domain.LoadingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoadingViewModel : ViewModel() {
    private val repository = LoadingRepositoryImpl()

    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    private val _status = MutableStateFlow(LoadingStatus.PRE_LOAD)
    val status = _status.asStateFlow()

    private val _errorState = MutableStateFlow(false)
    val errorState = _errorState.asStateFlow()

    private var timer = 0

    private var loadingScope: CoroutineScope? = CoroutineScope(Dispatchers.Default)
    private var timerScope: CoroutineScope? = CoroutineScope(Dispatchers.Default)

    var errorCallback: (() -> Unit)? = null

    var task1Status = false
    var task2Status = false
    var task3Status = false

    fun downloadConfig(callback: () -> Unit) {
        launchTimer(true)
        loadingScope?.launch {
            _status.value = LoadingStatus.DOWNLOAD_CONFIG
            repository.downloadConfig().collect {
                _progress.value += 1
                if (it) {
                    timerScope?.cancel()
                    _status.value = LoadingStatus.LOAD_CONFIG
                    callback.invoke()
                }
            }
        }
    }

    private fun launchTimer(isConfig: Boolean) {
        timerScope = CoroutineScope(Dispatchers.Default)
        timerScope?.launch {
            while (true) {
                delay(1000)
                timer += 1

                if ((isConfig && timer == 5) || (!isConfig && timer == 10)) {
                    timer = 0
                    errorCallback?.invoke()
                    loadingScope?.cancel()
                    timerScope?.cancel()
                    setErrorState(true)
                }
            }
        }
    }

    fun loadConfig() {
        loadingScope?.launch {
            _status.value = LoadingStatus.LOADING_CONFIG
            repository.loadConfig().collect {
                _progress.value += 1
                if (it) {
                    _status.value = LoadingStatus.LAUNCHING_TASKS
                    loadTasks()
                }
            }
        }
    }

    private fun loadTasks() {
        launchTimer(false)
        loadingScope?.launch {
            repository.loadTask().collect {
                _progress.value += 1
                if (it.second)
                    when (it.first) {
                        10 -> task1Status = true
                        20 -> task2Status = true
                        else -> {
                            timerScope?.cancel()
                            task3Status = true
                        }
                    }
            }
        }
    }

    fun finish() {
        _status.value = LoadingStatus.FINISH
    }

    fun setErrorState(state: Boolean) {
        _errorState.value = state
    }

    fun start() {
        timerScope = CoroutineScope(Dispatchers.Default)
        loadingScope = CoroutineScope(Dispatchers.Default)
        _progress.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        loadingScope?.cancel()
        loadingScope = null

        timerScope?.cancel()
        timerScope = null
    }
}