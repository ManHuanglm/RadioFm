package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.ui.components.StationListBody
import kotlinx.coroutines.launch

/**
 * 收藏页（对应 Flutter 版 favorites_page.dart）：
 * 页 0 收藏列表，左滑进入页 1 历史播放。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onOpenDrawer: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val favorites by container.favoritesStore.favorites.collectAsStateWithLifecycle()
    val history by container.historyStore.history.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (pagerState.currentPage == 0) "我的收藏" else "历史播放") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    if (pagerState.currentPage == 1 && history.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch { container.historyStore.clear() }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "清空历史")
                        }
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { page ->
            when (page) {
                0 -> StationListBody(
                    stations = favorites,
                    modifier = Modifier.fillMaxSize(),
                    emptyText = "还没有收藏，点击电台卡片右侧的红心收藏\n← 左滑查看历史播放",
                )
                else -> StationListBody(
                    stations = history,
                    modifier = Modifier.fillMaxSize(),
                    emptyText = "暂无播放记录，去听一首电台吧",
                )
            }
        }
    }
}
