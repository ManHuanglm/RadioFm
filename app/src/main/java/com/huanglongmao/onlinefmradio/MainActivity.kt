package com.huanglongmao.onlinefmradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.util.BatteryOptimizationUtils
import com.huanglongmao.onlinefmradio.ui.AppRoot
import kotlinx.coroutines.launch

/**
 * 唯一 Activity（对应 Flutter 版 main.dart / main_app.dart）。
 * 首次启动请求通知权限，引导电池优化白名单。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as App).container
        lifecycleScope.launch {
            val shown = runCatching { container.settings.getBool(AppConstants.KEY_PERMISSION_SHOWN) }
                .getOrNull() ?: false
            if (!shown) {
                BatteryOptimizationUtils.requestNotificationPermission(this@MainActivity)
                runCatching {
                    container.settings.putBool(AppConstants.KEY_PERMISSION_SHOWN, true)
                }
            }
        }

        setContent {
            AppRoot()
        }
    }
}
