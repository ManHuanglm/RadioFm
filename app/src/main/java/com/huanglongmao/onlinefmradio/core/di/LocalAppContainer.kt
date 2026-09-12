package com.huanglongmao.onlinefmradio.core.di

import androidx.compose.runtime.staticCompositionLocalOf

/** Compose 树内的 DI 容器访问点 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
