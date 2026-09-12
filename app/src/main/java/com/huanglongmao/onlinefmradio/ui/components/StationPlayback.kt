package com.huanglongmao.onlinefmradio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.di.AppContainer
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.data.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 电台列表页共享的播放/收藏交互封装。
 * 避免 15+ 个列表页重复接线播放状态与收藏切换。
 */
class StationPlayback internal constructor(
    val container: AppContainer,
) {
    val controller = container.playerController
    val favorites = container.favoritesStore

    fun play(station: Station) = controller.play(station)

    fun toggleFavorite(station: Station, scope: CoroutineScope) {
        scope.launch { favorites.toggle(station) }
    }
}

@Composable
fun rememberStationPlayback(): StationPlayback {
    val container = LocalAppContainer.current
    return remember(container) { StationPlayback(container) }
}

/** 当前正在播放的电台（列表页判断高亮用） */
@Composable
fun currentPlayingStationId(): String? {
    val container = LocalAppContainer.current
    val currentStation by container.playerController.currentStation.collectAsStateWithLifecycle()
    val isPlaying by container.playerController.isPlaying.collectAsStateWithLifecycle()
    return if (isPlaying) currentStation?.id else null
}

/** 收藏 ID 集合 */
@Composable
fun favoriteStationIds(): Set<String> {
    val container = LocalAppContainer.current
    val ids by container.favoritesStore.favoriteIds.collectAsStateWithLifecycle()
    return ids
}

/** 播放指定电台（封装常用交互） */
@Composable
fun playAction(): (Station) -> Unit {
    val playback = rememberStationPlayback()
    return { playback.play(it) }
}

/** 切换收藏（封装常用交互） */
@Composable
fun toggleFavoriteAction(): (Station) -> Unit {
    val playback = rememberStationPlayback()
    val scope = rememberCoroutineScope()
    return { playback.toggleFavorite(it, scope) }
}
