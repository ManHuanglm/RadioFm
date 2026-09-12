package com.huanglongmao.onlinefmradio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.data.model.Station

/**
 * 电台列表通用渲染（分页加载 / 空态 / 加载态）。
 */
@Composable
fun StationListBody(
    stations: List<Station>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    emptyText: String = "暂无电台",
    onReachEnd: () -> Unit = {},
    trailing: @Composable ((Station) -> Unit)? = null,
) {
    if (stations.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(text = emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(stations, key = { it.id }) { station ->
            val playing = currentPlayingStationId() == station.id
            val favorite = favoriteStationIds().contains(station.id)
            val play = playAction()
            val toggleFavorite = toggleFavoriteAction()
            StationCard(
                station = station,
                isPlaying = playing,
                isFavorite = favorite,
                onClickPlay = { play(station) },
                onClickFavorite = { toggleFavorite(station) },
                trailing = trailing?.let { transform -> { transform(station) } },
            )
        }
        item {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                if (isLoadingMore) {
                    CircularProgressIndicator(Modifier.padding(8.dp), strokeWidth = 2.dp)
                } else if (!hasMore) {
                    Text(
                        text = "— 到底了 —",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    // 触底加载更多
    val shouldLoad = listState.canScrollForward.not() && hasMore && !isLoadingMore
    if (shouldLoad) {
        androidx.compose.runtime.LaunchedEffect(stations.size) { onReachEnd() }
    }
}
