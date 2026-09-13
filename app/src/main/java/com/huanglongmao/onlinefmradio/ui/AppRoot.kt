package com.huanglongmao.onlinefmradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.huanglongmao.onlinefmradio.App
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.theme.OnlineFmRadioTheme
import com.huanglongmao.onlinefmradio.store.ThemeMode
import com.huanglongmao.onlinefmradio.ui.components.CollapsedMiniCover
import com.huanglongmao.onlinefmradio.ui.components.MiniPlayerBar
import com.huanglongmao.onlinefmradio.ui.screens.AlarmScreen
import com.huanglongmao.onlinefmradio.ui.screens.CachedStationsScreen
import com.huanglongmao.onlinefmradio.ui.screens.ChangelogScreen
import com.huanglongmao.onlinefmradio.ui.screens.CountryListScreen
import com.huanglongmao.onlinefmradio.ui.screens.CountryStationsScreen
import com.huanglongmao.onlinefmradio.ui.screens.ExploreScreen
import com.huanglongmao.onlinefmradio.ui.screens.FavoritesScreen
import com.huanglongmao.onlinefmradio.ui.screens.HelpScreen
import com.huanglongmao.onlinefmradio.ui.screens.HistoryScreen
import com.huanglongmao.onlinefmradio.ui.screens.HomeScreen
import com.huanglongmao.onlinefmradio.ui.screens.LanguageListScreen
import com.huanglongmao.onlinefmradio.ui.screens.LanguageStationsScreen
import com.huanglongmao.onlinefmradio.ui.screens.LocalStationsScreen
import com.huanglongmao.onlinefmradio.ui.screens.PlayerScreen
import com.huanglongmao.onlinefmradio.ui.screens.DeveloperScreen
import com.huanglongmao.onlinefmradio.ui.screens.LogsScreen
import com.huanglongmao.onlinefmradio.ui.screens.ProfileScreen
import com.huanglongmao.onlinefmradio.ui.screens.RandomStationScreen
import com.huanglongmao.onlinefmradio.ui.screens.RecordingScreen
import com.huanglongmao.onlinefmradio.ui.screens.SearchScreen
import com.huanglongmao.onlinefmradio.ui.screens.SettingsScreen
import com.huanglongmao.onlinefmradio.ui.screens.StationUpdateScreen
import com.huanglongmao.onlinefmradio.ui.screens.TagListScreen
import com.huanglongmao.onlinefmradio.ui.screens.TagStationsScreen
import kotlinx.coroutines.launch

/** 应用根组件：主题 + DI 注入 + 主脚手架 */
@Composable
fun AppRoot() {
    val container = (LocalContext.current.applicationContext as App).container
    val themeMode by container.themeStore.themeMode.collectAsStateWithLifecycle()
    val wallpaperIndex by container.themeStore.wallpaperIndex.collectAsStateWithLifecycle()
    val fontScale by container.themeStore.fontScale.collectAsStateWithLifecycle()

    // 应用内字体大小（标准/大/特大）：叠加系统字体缩放
    val density = LocalDensity.current
    val scaledDensity = Density(
        density = density.density,
        fontScale = density.fontScale * fontScale,
    )

    CompositionLocalProvider(LocalAppContainer provides container) {
        OnlineFmRadioTheme(
            themeMode = themeMode,
            wallpaperIndex = wallpaperIndex,
        ) {
            CompositionLocalProvider(
                LocalDensity provides scaledDensity,
            ) {
                MainScaffold()
            }
        }
    }
}

/** 主脚手架：抽屉 + 底部 4 Tab + 迷你播放条 + 导航图 */
@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomTabs
    // 迷你播放条展开/收缩状态（收缩后封面圆悬浮于内容区左下角）
    var miniExpanded by rememberSaveable { mutableStateOf(true) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(currentRoute = currentRoute, onNavigate = { route ->
                navController.navigate(route) { launchSingleTop = true }
                scope.launch { drawerState.close() }
            })
        },
    ) {
        Scaffold(
            // 页面内 TopAppBar 已处理状态栏 insets，这里归零避免双重叠加
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    Column {
                        MiniPlayerBar(
                            onClick = { navController.navigate(Routes.PLAYER) { launchSingleTop = true } },
                            expanded = miniExpanded,
                            onCollapse = { miniExpanded = false },
                        )
                        BottomNavBar(navController, currentRoute)
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxWidth(),
            ) {
                AppNavHost(
                    navController = navController,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                )
                // 收缩态封面圆：悬浮于内容区左下角（绘制在导航内容之上）
                if (showBottomBar && !miniExpanded) {
                    CollapsedMiniCover(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 14.dp, bottom = 10.dp),
                        onExpand = { miniExpanded = true },
                        onOpenPlayer = { navController.navigate(Routes.PLAYER) { launchSingleTop = true } },
                    )
                }
            }
        }
    }
}

/** 全部路由导航图 */
@Composable
private fun AppNavHost(navController: NavHostController, onOpenDrawer: () -> Unit) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(onOpenDrawer = onOpenDrawer, onNavigate = navController::navigate)
        }
        composable(Routes.EXPLORE) {
            ExploreScreen(onOpenDrawer = onOpenDrawer, onNavigate = navController::navigate)
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(onOpenDrawer = onOpenDrawer)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(onOpenDrawer = onOpenDrawer, onNavigate = navController::navigate)
        }
        composable(Routes.PLAYER) { PlayerScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() }, onNavigate = navController::navigate)
        }
        composable(Routes.STATION_UPDATE) { StationUpdateScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.CACHED_STATIONS) { CachedStationsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.LOCAL_STATIONS) { LocalStationsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.RANDOM_STATION) { RandomStationScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.COUNTRY_LIST) {
            CountryListScreen(onBack = { navController.popBackStack() }) { name, code ->
                navController.navigate(Routes.countryStations(name, code))
            }
        }
        composable(Routes.COUNTRY_STATIONS) { entry ->
            CountryStationsScreen(
                onBack = { navController.popBackStack() },
                name = entry.arguments?.getString("name").orEmpty(),
                code = entry.arguments?.getString("code").orEmpty(),
            )
        }
        composable(Routes.LANGUAGE_LIST) {
            LanguageListScreen(onBack = { navController.popBackStack() }) { name ->
                navController.navigate(Routes.languageStations(name))
            }
        }
        composable(Routes.LANGUAGE_STATIONS) { entry ->
            LanguageStationsScreen(
                onBack = { navController.popBackStack() },
                name = entry.arguments?.getString("name").orEmpty(),
            )
        }
        composable(Routes.TAG_LIST) {
            TagListScreen(onBack = { navController.popBackStack() }) { tag ->
                navController.navigate(Routes.tagStations(tag))
            }
        }
        composable(Routes.TAG_STATIONS) { entry ->
            TagStationsScreen(
                onBack = { navController.popBackStack() },
                tag = entry.arguments?.getString("tag").orEmpty(),
            )
        }
        composable(Routes.RECORDING) { RecordingScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ALARM) { AlarmScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.CHANGELOG) { ChangelogScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.HELP) { HelpScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.DEVELOPER) {
            DeveloperScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogs = { navController.navigate(Routes.LOGS) { launchSingleTop = true } },
            )
        }
        composable(Routes.LOGS) { LogsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.HISTORY) { HistoryScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SEARCH) { SearchScreen(onBack = { navController.popBackStack() }) }
    }
}

/** 底部 4 Tab（主页 / 探索 / 收藏 / 我的） */
@Composable
private fun BottomNavBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        val items = listOf(
            Triple("主页", Icons.Filled.Home, Routes.HOME),
            Triple("探索", Icons.Filled.Explore, Routes.EXPLORE),
            Triple("收藏", Icons.Filled.Favorite, Routes.FAVORITES),
            Triple("我的", Icons.Filled.Person, Routes.PROFILE),
        )
        for ((label, icon, route) in items) {
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

/** 侧边抽屉菜单（对应 Flutter 版 app_drawer.dart） */
@Composable
private fun AppDrawer(currentRoute: String?, onNavigate: (String) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            // 头部
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(20.dp),
            ) {
                Text(
                    text = AppConstants.APP_NAME,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "v${AppConstants.APP_VERSION}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            val items = listOf(
                Triple("设置", Icons.Filled.Settings, Routes.SETTINGS),
                Triple("搜索电台", Icons.Filled.QueueMusic, Routes.SEARCH),
                Triple("电台数据更新", Icons.Filled.CloudDownload, Routes.STATION_UPDATE),
                Triple("缓存电台", Icons.Filled.Save, Routes.CACHED_STATIONS),
                Triple("本地电台", Icons.Filled.Description, Routes.LOCAL_STATIONS),
                Triple("随机电台", Icons.Filled.Shuffle, Routes.RANDOM_STATION),
                Triple("国家列表", Icons.Filled.Explore, Routes.COUNTRY_LIST),
                Triple("语言列表", Icons.Filled.QueueMusic, Routes.LANGUAGE_LIST),
                Triple("标签列表", Icons.Filled.Explore, Routes.TAG_LIST),
                Triple("录音", Icons.Filled.Mic, Routes.RECORDING),
                Triple("闹钟", Icons.Filled.Alarm, Routes.ALARM),
                Triple("更新日志", Icons.Filled.Description, Routes.CHANGELOG),
                Triple("帮助", Icons.Filled.Help, Routes.HELP),
            )
            items.forEachIndexed { index, (label, icon, route) ->
                if (index == 3 || index == 9 || index == 12) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}
