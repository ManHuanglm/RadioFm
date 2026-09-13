package com.huanglongmao.onlinefmradio.core.theme

import androidx.compose.ui.graphics.Color

/**
 * 播放页等全屏渐变背景场景的固定语义色。
 * 这类页面不使用主题 colorScheme 作为背景，因此无法直接引用
 * MaterialTheme 语义色，统一在此集中管理。
 */

/** 收藏激活色（爱心） */
val FavoriteRed = Color(0xFFF87171)

/** 播放页错误提示文字色（浅红，保证深色渐变背景上的可读性） */
val PlayerErrorText = Color(0xFFFFCDD2)
