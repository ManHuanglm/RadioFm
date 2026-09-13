package com.huanglongmao.onlinefmradio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.player.PlayerController
import com.huanglongmao.onlinefmradio.store.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

/**
 * 迷你播放条——展开态（对应 Flutter 版 mini_player_bar.dart）。
 * 位于底部导航上方，点击进入播放页；左滑收缩；
 * 开启"自动收缩"后 10 秒无操作收缩为封面圆。
 *
 * 展开/收缩状态由外部持有（[expanded]/[onCollapse]），
 * 收缩态请配合 [CollapsedMiniCover] 悬浮层使用。
 */
@Composable
fun MiniPlayerBar(
    controller: PlayerController = LocalAppContainer.current.playerController,
    expanded: Boolean,
    onCollapse: () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val station by controller.currentStation.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by controller.isBuffering.collectAsStateWithLifecycle()

    // 自动收缩开关（DataStore 直读，设置页改动即时生效）
    val autoCollapse by remember(context) {
        context.dataStore.data.map {
            it[booleanPreferencesKey(AppConstants.KEY_MINI_AUTO_COLLAPSE)] ?: true
        }
    }.collectAsStateWithLifecycle(initialValue = true)

    // 任意交互 +1，用于重置自动收缩计时
    var interactionKey by remember { mutableIntStateOf(0) }

    // 展开态 10 秒无操作自动收缩为封面圆
    LaunchedEffect(expanded, autoCollapse, interactionKey, station?.id) {
        if (expanded && autoCollapse && station != null) {
            delay(10_000)
            onCollapse()
        }
    }

    if (station == null || !expanded) {
        Spacer(Modifier.height(0.dp))
        return
    }

    ExpandedBar(
        controller = controller,
        station = station,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        onCollapse = onCollapse,
        onInteract = { interactionKey++ },
        onClick = { interactionKey++; onClick() },
    )
}

/**
 * 迷你播放条——收缩态：封面圆悬浮于内容区左下角（配合内容 Box 的 align 使用）。
 * 点击展开；右滑进入播放页。
 */
@Composable
fun CollapsedMiniCover(
    controller: PlayerController = LocalAppContainer.current.playerController,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val station by controller.currentStation.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by controller.isBuffering.collectAsStateWithLifecycle()
    val s = station ?: return

    var dragX by remember { mutableFloatStateOf(0f) }
    Surface(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer { translationX = dragX }
            .pointerInput(Unit) {
                val openThreshold = 72.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragX >= openThreshold) onOpenPlayer()
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                ) { change, amount ->
                    dragX = (dragX + amount).coerceIn(0f, 160f)
                    change.consume()
                }
            }
            .clip(CircleShape)
            .clickable(onClick = onExpand),
        shape = CircleShape,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(Modifier.fillMaxSize()) {
            StationLogo(station = s, size = 48.dp, cornerRadius = 24.dp)
            // 播放状态角标
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

/** 展开态：完整播放条，左滑收缩，点击进播放页 */
@Composable
private fun ExpandedBar(
    controller: PlayerController,
    station: com.huanglongmao.onlinefmradio.data.model.Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onCollapse: () -> Unit,
    onInteract: () -> Unit,
    onClick: () -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .graphicsLayer { translationX = dragX }
            .pointerInput(Unit) {
                val collapseThreshold = 72.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragX <= -collapseThreshold) onCollapse()
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                ) { change, amount ->
                    dragX = (dragX + amount).coerceIn(-160f, 0f)
                    change.consume()
                }
            }
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            station?.let { StationLogo(station = it, size = 40.dp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = station?.name.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = station?.country.orEmpty().ifEmpty { " " },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isBuffering) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = {
                onInteract()
                controller.togglePlayPause()
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                )
            }
        }
    }
}
