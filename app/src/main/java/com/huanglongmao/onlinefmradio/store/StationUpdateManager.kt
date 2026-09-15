package com.huanglongmao.onlinefmradio.store

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.util.AppLogger
import com.huanglongmao.onlinefmradio.data.model.RadioStats
import com.huanglongmao.onlinefmradio.data.repository.StationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 电台数据更新管理器（对应 Flutter 版 station_update_service.dart）。
 *
 * - 全量更新：断点续传 + 暂停/继续/重新获取状态机
 * - 进度持久化（offset/fetched/total），中断后可继续
 * - 刷新前对比缓存与远程数量，一致则跳过
 */
class StationUpdateManager(
    private val repository: StationRepository,
    private val settings: SettingsDataStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _fetchedCount = MutableStateFlow(0)
    val fetchedCount: StateFlow<Int> = _fetchedCount

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    private val _updateComplete = MutableStateFlow(false)
    val updateComplete: StateFlow<Boolean> = _updateComplete

    private val _hasResumeData = MutableStateFlow(false)
    val hasResumeData: StateFlow<Boolean> = _hasResumeData

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _cachedCount = MutableStateFlow(0)
    val cachedCount: StateFlow<Int> = _cachedCount

    private val _remoteStats = MutableStateFlow<RadioStats?>(null)
    val remoteStats: StateFlow<RadioStats?> = _remoteStats

    private val _isCleaningBroken = MutableStateFlow(false)
    val isCleaningBroken: StateFlow<Boolean> = _isCleaningBroken

    private val _brokenRemovedCount = MutableStateFlow(0)
    val brokenRemovedCount: StateFlow<Int> = _brokenRemovedCount

    @Volatile private var cancelled = false

    /** 进度（0.0 ~ 1.0） */
    val progress: Float
        get() = if (_totalCount.value > 0) {
            (_fetchedCount.value.toFloat() / _totalCount.value).coerceIn(0f, 1f)
        } else 0f

    /** 是否需要更新（缓存数量与远程数量不同） */
    val needsUpdate: Boolean
        get() {
            val stats = _remoteStats.value ?: return true
            return _cachedCount.value != stats.stations
        }

    init {
        scope.launch { checkResumeData() }
    }

    private suspend fun checkResumeData() {
        val offset = settings.getInt(AppConstants.KEY_UPDATE_RESUME_OFFSET) ?: 0
        if (offset > 0) {
            _hasResumeData.value = true
            _fetchedCount.value = settings.getInt(AppConstants.KEY_UPDATE_RESUME_FETCHED) ?: 0
            _totalCount.value = settings.getInt(AppConstants.KEY_UPDATE_RESUME_TOTAL) ?: 0
        }
    }

    /** 刷新缓存与远程对比数据 */
    suspend fun refreshStats() {
        runCatching {
            _cachedCount.value = repository.getCachedStationCount()
            val stats = repository.loadStats()
            _remoteStats.value = stats
        }
    }

    /**
     * 刷新：对比缓存与远程数量，相同则跳过；有断点则直接续传。
     */
    suspend fun refresh() {
        if (_isUpdating.value) return
        refreshStats()
        if (_hasResumeData.value) {
            updateAllStations()
            return
        }
        val remoteTotal = _remoteStats.value?.stations ?: 0
        if (_cachedCount.value > 0 && _cachedCount.value == remoteTotal) {
            _updateComplete.value = true
            _errorMessage.value = null
            return
        }
        updateAllStations()
    }

    /**
     * 在管理器自身的应用级作用域启动刷新任务：
     * 页面退出 / 应用切后台不会取消，更新持续进行，
     * 进度通过状态流（isUpdating/fetchedCount 等）随时可观察。
     */
    fun startRefresh() {
        scope.launch { refresh() }
    }

    /** 在应用级作用域启动"清空重新获取"任务 */
    fun startRestart() {
        scope.launch { restart() }
    }

    /** 全量更新（带断点续传与暂停/停止支持） */
    suspend fun updateAllStations() {
        if (_isUpdating.value) return

        _isUpdating.value = true
        _isPaused.value = false
        cancelled = false
        _errorMessage.value = null
        _updateComplete.value = false

        val savedOffset = settings.getInt(AppConstants.KEY_UPDATE_RESUME_OFFSET) ?: 0
        val effectiveOffset: Int
        if (savedOffset > 0) {
            effectiveOffset = savedOffset
            _fetchedCount.value = settings.getInt(AppConstants.KEY_UPDATE_RESUME_FETCHED) ?: 0
            _totalCount.value = settings.getInt(AppConstants.KEY_UPDATE_RESUME_TOTAL)
                ?: AppConstants.DEFAULT_MAX_STATIONS
        } else {
            // 无断点数据：从本地缓存末端开始增量拉取
            effectiveOffset = repository.getCachedStationCount()
            _fetchedCount.value = effectiveOffset
            _totalCount.value = AppConstants.DEFAULT_MAX_STATIONS
            if (effectiveOffset > 0) {
                saveResume(effectiveOffset, effectiveOffset, _totalCount.value)
            }
        }

        try {
            AppLogger.i(TAG, "全量更新开始：offset=$effectiveOffset, 已获取=${_fetchedCount.value}")
            val count = repository.fetchAllAndCache(
                resumeOffset = effectiveOffset,
                resumeFetched = _fetchedCount.value,
                onProgress = { fetched, total ->
                    _fetchedCount.value = fetched
                    _totalCount.value = total
                },
                onBatchSaved = { offset, fetched, total ->
                    saveResume(offset, fetched, total)
                },
                isPaused = { _isPaused.value },
                shouldStop = { cancelled },
            )
            if (cancelled) {
                AppLogger.i(TAG, "全量更新已停止，保留断点 offset=$effectiveOffset")
                _hasResumeData.value = true
            } else {
                AppLogger.i(TAG, "全量更新完成：共 $count 条")
                _fetchedCount.value = count
                _hasResumeData.value = false
                clearResumeData()
                // 更新完成后自动清理故障电台，再统计最终缓存数量
                val removed = cleanBrokenStationsInternal()
                _brokenRemovedCount.value = removed
                if (removed > 0) {
                    AppLogger.i(TAG, "更新后自动清理故障电台：$removed 条")
                }
                _updateComplete.value = true
                _cachedCount.value = repository.getCachedStationCount()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "全量更新失败：${e.message}")
            _errorMessage.value = e.message ?: "更新失败"
            _hasResumeData.value = true
        } finally {
            _isUpdating.value = false
            _isPaused.value = false
        }
    }

    private suspend fun saveResume(offset: Int, fetched: Int, total: Int) {
        runCatching {
            settings.putInt(AppConstants.KEY_UPDATE_RESUME_OFFSET, offset)
            settings.putInt(AppConstants.KEY_UPDATE_RESUME_FETCHED, fetched)
            settings.putInt(AppConstants.KEY_UPDATE_RESUME_TOTAL, total)
        }
    }

    /** 暂停更新 */
    fun pause() {
        if (!_isUpdating.value || _isPaused.value) return
        _isPaused.value = true
    }

    /** 继续更新 */
    fun resumeUpdate() {
        if (!_isPaused.value) return
        _isPaused.value = false
    }

    /** 重新获取：取消当前任务，清空缓存与断点，从头开始 */
    suspend fun restart() {
        if (_isUpdating.value) {
            cancelled = true
            _isPaused.value = false
            while (_isUpdating.value) delay(100)
        }
        repository.clearCache()
        clearResumeData()
        updateAllStations()
    }

    /** 清除断点续传数据 */
    suspend fun clearResumeData() {
        runCatching {
            settings.putInt(AppConstants.KEY_UPDATE_RESUME_OFFSET, null)
            settings.putInt(AppConstants.KEY_UPDATE_RESUME_FETCHED, null)
            settings.putInt(AppConstants.KEY_UPDATE_RESUME_TOTAL, null)
        }
        _hasResumeData.value = false
        _fetchedCount.value = 0
        _totalCount.value = 0
    }

    /** 重置完成标记与错误，准备下一次更新 */
    fun resetState() {
        _updateComplete.value = false
        _errorMessage.value = null
    }

    /** 同步远程数据：只下载本地缺少的差异数据，返回新增数量 */
    suspend fun syncData(): Int {
        if (_isUpdating.value) return 0
        _isUpdating.value = true
        _isPaused.value = false
        cancelled = false
        _errorMessage.value = null
        _updateComplete.value = false
        _fetchedCount.value = 0
        _totalCount.value = AppConstants.DEFAULT_MAX_STATIONS
        return try {
            val newCount = repository.syncRemoteStations(
                onProgress = { compared, total ->
                    _fetchedCount.value = compared
                    _totalCount.value = total
                },
                shouldStop = { cancelled },
            )
            _fetchedCount.value = newCount
            // 同步完成后同样自动清理故障电台
            val removed = cleanBrokenStationsInternal()
            _brokenRemovedCount.value = removed
            _updateComplete.value = true
            _cachedCount.value = repository.getCachedStationCount()
            newCount
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "同步失败"
            0
        } finally {
            _isUpdating.value = false
        }
    }

    /** 停止当前任务（不清理断点，保留续传能力） */
    fun stop() {
        cancelled = true
        _isPaused.value = false
    }

    /**
     * 清理本地缓存中的故障电台：拉取 radio-browser.info 的故障电台列表，
     * 从缓存中移除对应 ID，返回移除数量。
     */
    fun startCleanBroken() {
        scope.launch {
            if (_isCleaningBroken.value) return@launch
            _isCleaningBroken.value = true
            _errorMessage.value = null
            try {
                val removed = cleanBrokenStationsInternal()
                _brokenRemovedCount.value = removed
                _cachedCount.value = repository.getCachedStationCount()
                AppLogger.i(TAG, "故障电台清理完成：移除 $removed 条")
            } finally {
                _isCleaningBroken.value = false
            }
        }
    }

    /**
     * 清理故障电台的内部实现（更新流程与手动清理共用）。
     * 失败不抛异常（返回 0），避免影响更新主流程。
     */
    private suspend fun cleanBrokenStationsInternal(): Int = runCatching {
        repository.removeBrokenStations()
    }.getOrElse { e ->
        AppLogger.e(TAG, "故障电台清理失败：${e.message}")
        0
    }

    private companion object {
        const val TAG = "StationUpdate"
    }
}
