package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.ui.components.StationListBody

/**
 * 收藏页（对应 Flutter 版 favorites_page.dart）：收藏列表 + 直接播放。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onOpenDrawer: () -> Unit) {
    val container = LocalAppContainer.current
    val favorites by container.favoritesStore.favorites.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "菜单")
                    }
                },
            )
        },
    ) { padding ->
        StationListBody(
            stations = favorites,
            modifier = Modifier.fillMaxSize().padding(padding),
            emptyText = "还没有收藏，点击电台卡片右侧的红心收藏",
        )
    }
}
