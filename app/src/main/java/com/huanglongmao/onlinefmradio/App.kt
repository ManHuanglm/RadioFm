package com.huanglongmao.onlinefmradio

import android.app.Application
import com.huanglongmao.onlinefmradio.core.di.AppContainer

/**
 * 应用入口（对应 Flutter 版 main.dart 的多Provider装配职责）。
 */
class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
