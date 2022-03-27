package com.yyw.callbackflow

import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {
    private var toggle = true
    val res = flow {
        while (toggle) {
            emit((0..10).random())
            delay(5000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(300), 0)

    fun startCoroutine() {
        toggle = true
    }

    fun cancelCoroutine() {
        toggle = false
    }

    private val _increaseValue = MutableStateFlow(0)

    val increaseValue: StateFlow<Int> = _increaseValue

    fun increase() {
        _increaseValue.value = _increaseValue.value + 1
    }

    private val _mSizeOfSoFarByte = MutableStateFlow(0)
    val mSizeOfSoFarByte: StateFlow<Int> = _mSizeOfSoFarByte

    private val _mTotalSizeOfByte = MutableStateFlow(0)
    val mTotalSizeOfByte: StateFlow<Int> = _mTotalSizeOfByte

    private val _mCompleted = MutableStateFlow(false)

    private val _mDownloadTips: StateFlow<String> = _mCompleted.map {
        if (it) {
            "下载完成"
        } else {
            ""
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(300), "")

    val mDownloadTips: StateFlow<String> = _mDownloadTips

    fun downloadFile(v: View) {
//        FileDownloader.setup(v.context)
        val path = File.separator + "sdcard" + File.separator + "abc.mp4"
        FileDownloader.getImpl().create("https://www.dfs.smart0.cn/group2/M00/30/94/rBIavGGfK8uEf6G-AAAAAHnGJYQ393.mp4")
            .setPath(path).setForceReDownload(true).setListener(object : FileDownloadListener() {
                override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                    Log.d("wyy", "pending")
                }

                override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                    Log.d("wyy", "progress")
                    _mSizeOfSoFarByte.value = soFarBytes
                    _mTotalSizeOfByte.value = totalBytes
                }

                override fun completed(task: BaseDownloadTask?) {
                    Log.d("wyy", "completed")
                    _mCompleted.value = true
                }

                override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                    Log.d("wyy", "paused")
                }

                override fun error(task: BaseDownloadTask?, e: Throwable?) {
                    Log.d("wyy", "error")
                    e?.printStackTrace()
                }

                override fun warn(task: BaseDownloadTask?) {
                    Log.d("wyy", "warn")
                }
            }).start()
    }

    private val _mDownloadResult = callbackFlow {
        val path = File.separator + "sdcard" + File.separator + "abc.mp4"
        val cb = object : FileDownloadListener() {
            override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                Log.d("wyy", "pending")
            }

            override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                Log.d("wyy", "progress")
                val sendRes = trySend(Result.Progress(soFarBytes, totalBytes)).isSuccess
                Log.d("wyy", "progress sendRes:$sendRes")
            }

            override fun completed(task: BaseDownloadTask?) {
                Log.d("wyy", "completed")
                val sendRes = trySend(Result.Success).isSuccess
                Log.d("wyy", "completed sendRes:$sendRes")
            }

            override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                Log.d("wyy", "paused")
            }

            override fun error(task: BaseDownloadTask?, e: Throwable?) {
                Log.d("wyy", "error")
                val sendRes = trySend(Result.Error(Exception(e?.message))).isSuccess
                Log.d("wyy", "error sendRes:$sendRes")
                close(e)
            }

            override fun warn(task: BaseDownloadTask?) {
                Log.d("wyy", "warn")
            }
        }
        FileDownloader.getImpl().create("https://www.dfs.smart0.cn/group2/M00/30/94/rBIavGGfK8uEf6G-AAAAAHnGJYQ393.mp4")
            .setPath(path).setForceReDownload(true).setListener(cb).start()
    }

    fun downloadFileWithFlow() {
        viewModelScope.launch {
            val path = File.separator + "sdcard" + File.separator + "abc.mp4"
            FileDownloader.getImpl().create("https://www.dfs.smart0.cn/group2/M00/30/94/rBIavGGfK8uEf6G-AAAAAHnGJYQ393.mp4")
                .setPath(path).setForceReDownload(true).downloadFlow().conflate().catch { e ->
                    Log.d("wyy", e.message ?: "没有错误信息")
                }.collect {
                    when (it) {
                        is Result.Success -> _mCompleted.value = true
                        is Result.Error -> Log.d("wyy", "文件下载失败 e:${it.exception.message}")
                        is Result.Progress -> {
                            _mSizeOfSoFarByte.value = it.soFarBytes
                            _mTotalSizeOfByte.value = it.totalBytes
                        }
                    }
                }
        }
    }

    fun downloadMultipleFileWithFlow() {
        viewModelScope.launch {
            listOf(
                "https://www.dfs.smart0.cn/group2/M00/30/94/rBIavGGfK8uEf6G-AAAAAHnGJYQ393.mp4",
                "http://cdn.llsapp.com/android/LLS-v4.0-595-20160908-143200.apk"
            ).multipleDownloadFlow().conflate().catch { e ->
                Log.d("wyy", e.message ?: "没有错误信息")
            }.collect {
                when (it) {
                    is Result.Success -> {
                        _mCompleted.value = true
                        Log.d("wyy", "下载成功啦")
                    }
                    is Result.Error -> Log.d("wyy", "文件下载失败 e:${it.exception.message}")
                    is Result.Progress -> {
                        if (_mCompleted.value) {
                            _mCompleted.value = false
                        }
                        _mSizeOfSoFarByte.value = it.soFarBytes
                        _mTotalSizeOfByte.value = it.totalBytes
                    }
                }
            }
        }
    }
}

sealed class Result {
    object Success : Result()
    data class Error(val exception: Exception) : Result()
    data class Progress(var soFarBytes: Int, var totalBytes: Int) : Result()
}