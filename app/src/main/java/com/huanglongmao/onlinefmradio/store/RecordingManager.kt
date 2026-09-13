package com.huanglongmao.onlinefmradio.store

import android.content.Context
import com.huanglongmao.onlinefmradio.data.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 录音状态 */
sealed interface RecordingState {
    /** 空闲 */
    data object Idle : RecordingState

    /** 正在连接电台流（尚未收到音频数据） */
    data class Connecting(val station: Station) : RecordingState

    /** 录制中 */
    data class Recording(
        val station: Station,
        val fileName: String,
        val startedAt: Long,
        val bytes: Long,
    ) : RecordingState
}

/** 已完成的录音文件 */
data class RecordingFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
)

/**
 * 电台录音管理：将正在播放的网络音频流原样转存为本地文件。
 *
 * 实现方式为 HTTP 流直录（OkHttp 逐块读取写入文件），
 * 无损保留原始编码（mp3/aac/ogg…），无需录音权限。
 * 单例挂载于 [com.huanglongmao.onlinefmradio.core.di.AppContainer]。
 */
class RecordingManager(context: Context) {

    private val appContext = context.applicationContext

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // 直播流两次数据包之间的最大间隔，超时视为断流结束录制
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var job: Job? = null

    /** 当前正在执行的流请求，stop() 时立即中断 TCP 连接 */
    @Volatile
    private var activeCall: Call? = null

    /** 录音文件保存目录（应用外部私有目录，卸载时随应用清除） */
    val recordingsDir: File
        get() = File(appContext.getExternalFilesDir(null), "recordings").apply { mkdirs() }

    /**
     * 开始录制指定电台。
     * @return 是否成功启动（已在录制中时返回 false）
     */
    fun start(station: Station): Boolean {
        if (_state.value !is RecordingState.Idle) return false
        job = scope.launch { record(station) }
        return true
    }

    /** 停止录制（保留已写入的部分文件） */
    fun stop() {
        activeCall?.cancel()
        job?.cancel()
        job = null
    }

    /** 录音文件列表（按时间倒序） */
    suspend fun list(): List<RecordingFile> = withContext(Dispatchers.IO) {
        recordingsDir.listFiles()
            ?.filter { it.isFile && it.length() > 0 }
            ?.sortedByDescending { it.lastModified() }
            ?.map { RecordingFile(it.name, it.absolutePath, it.length(), it.lastModified()) }
            ?: emptyList()
    }

    /** 删除录音文件，返回是否删除成功 */
    suspend fun delete(name: String): Boolean = withContext(Dispatchers.IO) {
        // 录制中的文件不允许删除
        val recording = _state.value as? RecordingState.Recording
        if (recording?.fileName == name) return@withContext false
        File(recordingsDir, name).delete()
    }

    /** 流录制主循环：拉取电台流并逐块写入文件 */
    private suspend fun record(station: Station) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = station.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60).trim()
        _state.value = RecordingState.Connecting(station)
        try {
            val call = newCall(station.streamUrl)
            activeCall = call
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("空响应体")
                val ext = extensionOf(response.header("Content-Type"), station.streamUrl)
                val file = File(recordingsDir, "${safeName}_$stamp.$ext")
                val startedAt = System.currentTimeMillis()
                _state.value = RecordingState.Recording(station, file.name, startedAt, 0)
                var bytes = 0L
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            // 响应式检查取消：配合 call.cancel() 保证 stop() 及时生效
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            bytes += read
                            _state.value = RecordingState.Recording(
                                station, file.name, startedAt, bytes,
                            )
                        }
                    }
                }
                // 服务端正常结束流：录制完成
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            // 用户主动停止：保留已写入的文件
        } catch (_: IOException) {
            // 断流/连接失败：结束录制，保留部分文件
        } finally {
            activeCall = null
            _state.value = RecordingState.Idle
        }
    }

    private fun newCall(url: String) = client.newCall(
        Request.Builder()
            .url(url)
            .header("User-Agent", AppRecordingUserAgent)
            .build(),
    )

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val AppRecordingUserAgent = "RadioFm/1.0 (Recording)"

        /** 根据响应 Content-Type 与流地址推断文件扩展名 */
        fun extensionOf(contentType: String?, url: String): String {
            val ct = contentType?.lowercase().orEmpty()
            val lowerUrl = url.lowercase()
            return when {
                "aac" in ct || "aacp" in ct || ".aac" in lowerUrl -> "aac"
                "ogg" in ct || "opus" in ct || ".ogg" in lowerUrl -> "ogg"
                "flac" in ct || ".flac" in lowerUrl -> "flac"
                "mp4" in ct || "m4a" in ct -> "m4a"
                else -> "mp3"
            }
        }
    }
}
