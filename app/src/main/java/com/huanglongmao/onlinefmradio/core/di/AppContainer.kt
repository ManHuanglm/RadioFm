package com.huanglongmao.onlinefmradio.core.di

import android.content.Context
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.data.cache.AssetFallback
import com.huanglongmao.onlinefmradio.data.cache.StationFileCache
import com.huanglongmao.onlinefmradio.data.remote.RadioBrowserApi
import com.huanglongmao.onlinefmradio.data.repository.StationRepository
import com.huanglongmao.onlinefmradio.player.PlayerController
import com.huanglongmao.onlinefmradio.player.SleepTimerManager
import com.huanglongmao.onlinefmradio.store.AppUpdateManager
import com.huanglongmao.onlinefmradio.store.CountryPreferenceStore
import com.huanglongmao.onlinefmradio.store.FavoritesStore
import com.huanglongmao.onlinefmradio.store.HistoryStore
import com.huanglongmao.onlinefmradio.store.ImportExportManager
import com.huanglongmao.onlinefmradio.store.LocalStationStore
import com.huanglongmao.onlinefmradio.store.RecordingManager
import com.huanglongmao.onlinefmradio.store.SettingsDataStore
import com.huanglongmao.onlinefmradio.store.StationUpdateManager
import com.huanglongmao.onlinefmradio.store.ThemeStore
import com.huanglongmao.onlinefmradio.store.VisualizerStore

/**
 * 手写 DI 容器（对应 Flutter 版 provider 的多Provider注入）。
 * 全部单例挂载于进程级 AppContainer，UI 层通过 App.container 访问。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // ===== 基础设施 =====

    val settings: SettingsDataStore by lazy { SettingsDataStore.from(appContext) }

    val radioBrowserApi: RadioBrowserApi by lazy {
        com.huanglongmao.onlinefmradio.core.network.RetrofitFactory.create(AppConstants.RADIO_BROWSER_API_BASE)
    }

    val updateApi: com.huanglongmao.onlinefmradio.data.remote.UpdateApi by lazy {
        com.huanglongmao.onlinefmradio.core.network.RetrofitFactory.create("https://raw.githubusercontent.com/")
    }

    val stationCache: StationFileCache by lazy { StationFileCache(appContext) }

    val assetFallback: AssetFallback by lazy { AssetFallback(appContext) }

    val stationRepository: StationRepository by lazy {
        StationRepository(radioBrowserApi, stationCache, assetFallback)
    }

    // ===== 存储服务 =====

    val favoritesStore: FavoritesStore by lazy { FavoritesStore(settings) }

    val historyStore: HistoryStore by lazy { HistoryStore(settings) }

    val themeStore: ThemeStore by lazy { ThemeStore(settings) }

    val visualizerStore: VisualizerStore by lazy { VisualizerStore(settings) }

    val countryPreferenceStore: CountryPreferenceStore by lazy { CountryPreferenceStore(settings) }

    val localStationStore: LocalStationStore by lazy { LocalStationStore(settings) }

    // ===== 播放 =====

    val playerController: PlayerController by lazy {
        PlayerController(appContext, historyStore, settings)
    }

    val sleepTimerManager: SleepTimerManager by lazy {
        SleepTimerManager(playerController, settings)
    }

    // ===== 数据管理 =====

    val stationUpdateManager: StationUpdateManager by lazy {
        StationUpdateManager(stationRepository, settings)
    }

    val importExportManager: ImportExportManager by lazy {
        ImportExportManager(radioBrowserApi)
    }

    val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManager(updateApi, settings)
    }

    // ===== 录音 =====

    val recordingManager: RecordingManager by lazy { RecordingManager(appContext) }
}
