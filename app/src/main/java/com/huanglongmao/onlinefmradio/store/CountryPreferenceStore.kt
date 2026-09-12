package com.huanglongmao.onlinefmradio.store

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 国家偏好服务（对应 Flutter 版 country_preference_service.dart）。
 * 推荐页按此偏好筛选电台；空值表示不筛选。
 */
class CountryPreferenceStore(private val settings: SettingsDataStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry

    /** 启动时加载 */
    suspend fun load() {
        _selectedCountry.value = settings.getString(AppConstants.KEY_SELECTED_COUNTRY)
    }

    /** 设置国家偏好（传 null/空清除） */
    suspend fun setCountry(country: String?) {
        _selectedCountry.value = country?.takeIf { it.isNotEmpty() }
        runCatching {
            settings.putString(AppConstants.KEY_SELECTED_COUNTRY, country?.takeIf { it.isNotEmpty() })
        }
    }

    init {
        scope.launch { load() }
    }
}
