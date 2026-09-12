package com.huanglongmao.onlinefmradio.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 后台保活权限工具（对应 Flutter 版 battery_optimization_utils.dart）：
 * - Android 13+ 通知权限
 * - 电池优化白名单（缓解国产 ROM 后台断网杀进程）
 */
object BatteryOptimizationUtils {

    /** 是否已授予通知权限（Android 13+） */
    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    /** 请求通知权限（Android 13+，低版本直接跳过） */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_CODE,
            )
        }
    }

    /** 是否在电池优化白名单中 */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** 弹出加入电池优化白名单的系统对话框 */
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (isIgnoringBatteryOptimizations(activity)) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        runCatching { activity.startActivity(intent) }
    }

    /** 首次播放时的保活权限引导：通知权限 + 电池优化白名单 */
    fun checkAndRequestAllPermissions(activity: Activity) {
        if (!hasNotificationPermission(activity)) {
            requestNotificationPermission(activity)
        }
        if (!isIgnoringBatteryOptimizations(activity)) {
            requestIgnoreBatteryOptimizations(activity)
        }
    }

    /** 打开应用的系统设置详情页 */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        runCatching { context.startActivity(intent) }
    }

    private const val REQUEST_NOTIFICATION_CODE = 1001
}
