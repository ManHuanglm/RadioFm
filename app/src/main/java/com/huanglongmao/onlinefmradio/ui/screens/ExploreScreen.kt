package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.theme.GradientThemes
import com.huanglongmao.onlinefmradio.ui.Routes

private data class DiscoveryCard(val tag: String, val title: String, val subtitle: String, val icon: ImageVector)

private val discoveryCards = listOf(
    DiscoveryCard("decades", "年代金曲", "穿越时光的旋律", Icons.Filled.History),
    DiscoveryCard("talk", "脱口秀", "观点与故事", Icons.Filled.Forum),
    DiscoveryCard("sports", "体育赛事", "赛场实况解说", Icons.Filled.SportsBasketball),
    DiscoveryCard("news", "新闻资讯", "全球时事滚动", Icons.Filled.Newspaper),
)

/** 分类中文名 */
private val categoryNameMap = mapOf(
    "pop" to "流行", "rock" to "摇滚", "jazz" to "爵士", "classical" to "古典",
    "electronic" to "电子", "hip hop" to "嘻哈", "country" to "乡村", "reggae" to "雷鬼",
    "blues" to "蓝调", "r&b" to "节奏布鲁斯", "latin" to "拉丁", "world" to "世界音乐",
)

/**
 * 探索页（对应 Flutter 版 explore_page.dart）：
 * 4 张发现渐变卡片 + 音乐类型 12 格网格 → 标签电台页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onOpenDrawer: () -> Unit, onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("探索") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "菜单")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text("发现", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            // 2×2 发现卡片
            discoveryCards.chunked(2).forEachIndexed { rowIdx, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { colIdx, card ->
                        val theme = GradientThemes.resolve(rowIdx * 2 + colIdx + 1)
                        DiscoveryCardView(
                            card = card,
                            colors = theme.gradient,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Routes.tagStations(card.tag)) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))
            Text("音乐类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            // 12 格分类网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false,
            ) {
                items(AppConstants.supportedCategories) { category ->
                    val theme = GradientThemes.resolve(
                        AppConstants.supportedCategories.indexOf(category) % GradientThemes.presets.size,
                    )
                    Box(
                        modifier = Modifier
                            .height(60.dp)
                            .background(
                                Brush.linearGradient(theme.gradient),
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { onNavigate(Routes.tagStations(category)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = categoryNameMap[category.lowercase()] ?: category,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiscoveryCardView(
    card: DiscoveryCard,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(110.dp)
            .background(Brush.linearGradient(colors), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column {
            Icon(card.icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(
                text = card.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = card.subtitle,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
