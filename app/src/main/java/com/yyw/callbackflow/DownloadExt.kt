package com.yyw.callbackflow

import android.util.Log
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloadQueueSet
import com.liulishuo.filedownloader.FileDownloader
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

fun BaseDownloadTask.downloadFlow() = callbackFlow {
    val listener = object : FileDownloadListener() {
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
            close()
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
    setListener(listener)
    start()
    awaitClose {
        Log.d("wyy", "awaitClose")
    }
}

fun List<String>.multipleDownloadFlow() = callbackFlow {
    val listener = object : FileDownloadListener() {
        override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            Log.d("wyy", "pending")
        }

        override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            Log.d("wyy", "progress tag:${task?.tag}")
            val sendRes = trySend(Result.Progress(soFarBytes, totalBytes)).isSuccess
            Log.d("wyy", "progress sendRes:$sendRes")
        }

        override fun completed(task: BaseDownloadTask?) {
            Log.d("wyy", "completed tag:${task?.tag}")
            val sendRes = trySend(Result.Success).isSuccess
            Log.d("wyy", "completed sendRes:$sendRes")
            task?.let {
                if (it.tag == size) {
                    close()
                }
            }
            if (null == task) {
                close()
            }
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
    val queueSet = FileDownloadQueueSet(listener)
    val tasks = ArrayList<BaseDownloadTask>()
    for ((index, value) in this@multipleDownloadFlow.toTypedArray().withIndex()) {
        tasks.add(
            FileDownloader.getImpl().create(value)
                .setPath(File.separator + "sdcard" + File.separator + "abc_$index.${getFileSuffix(value)}")
                .setForceReDownload(true)
                .setTag(index + 1)
        )
    }
    queueSet.downloadSequentially(tasks)
    queueSet.start()
    awaitClose {
        Log.d("wyy", "awaitClose")
    }
}

fun getFileSuffix(value: String): String = value.split(".").last()
