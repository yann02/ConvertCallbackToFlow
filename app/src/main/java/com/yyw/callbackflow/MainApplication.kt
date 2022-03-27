package com.yyw.callbackflow

import android.app.Application
import com.liulishuo.filedownloader.FileDownloader
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        /**
         * just for cache Application's Context, and ':filedownloader' progress will NOT be launched
         * by below code, so please do not worry about performance.
         * @see FileDownloader.init
         */
        FileDownloader.setupOnApplicationOnCreate(this)
            .connectionCreator(
                FileDownloadUrlConnection.Creator(
                    FileDownloadUrlConnection.Configuration()
                        .connectTimeout(15000) // set connection timeout.
                        .readTimeout(15000) // set read timeout.
                )
            )
            .commit()
    }
}