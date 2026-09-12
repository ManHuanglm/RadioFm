package com.huanglongmao.onlinefmradio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.huanglongmao.onlinefmradio.data.model.Station

/**
 * 电台 Logo（对应 Flutter 版 station_logo.dart）：
 * 优先 safeLogo，失败回退 favicon，再失败显示首字符占位。
 */
@Composable
fun StationLogo(station: Station, size: Dp = 48.dp, cornerRadius: Dp = 12.dp) {
    var useFavicon by remember(station.id) { mutableStateOf(false) }
    var failed by remember(station.id) { mutableStateOf(false) }

    val url = when {
        !failed && station.safeLogo.isNotEmpty() -> station.safeLogo
        !failed && useFavicon && station.faviconFallback.isNotEmpty() -> station.faviconFallback
        else -> null
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = station.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                onError = {
                    if (url == station.safeLogo && station.faviconFallback.isNotEmpty()) {
                        useFavicon = true
                    } else {
                        failed = true
                    }
                },
            )
        } else {
            // 图片获取失败：显示收音机图标占位
            Icon(
                imageVector = Icons.Filled.Radio,
                contentDescription = "电台",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}
