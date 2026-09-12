# 听电台 (RadioFm)

一款 Android 在线 FM 电台应用，使用 **Kotlin 原生**重写（原 Flutter 版 [online_fm_radio](https://github.com/Huanglongmao66/FlutterRadio) 的 Kotlin 移植）。

## 功能特性

- **在线收听**：基于 [radio-browser.info](https://www.radio-browser.info/) 开放 API，全球数万电台
- **后台播放**：Media3 ExoPlayer + MediaSessionService，媒体通知控制、音频焦点处理、断线指数退避重连
- **首页**：热门/最新电台列表，按国家偏好过滤
- **探索**：国家 / 语言 / 标签多维筛选浏览
- **搜索**：按名称实时搜索电台
- **收藏 / 历史**：本地持久化（DataStore），快速找回爱听的台
- **全量数据更新**：分批拉取电台库（断点续传），JSONL 文件缓存 10000+ 台，离线可用（内置资产兜底）
- **播放页**：渐变封面、音频可视化（多种样式/速度可调）、音量控制、睡眠定时
- **个性化**：8 套渐变主题、深浅色跟随系统
- **应用内更新**：静态版本清单（`update/update.json`）检查新版本，支持跳过版本
- **导入导出 / 随机电台 / 缓存管理**等辅助页面，录音与闹钟为占位页

## 技术栈

| 模块 | 选型 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 播放 | Media3 ExoPlayer / MediaSessionService |
| 网络 | Retrofit + OkHttp + Kotlinx Serialization |
| 存储 | DataStore Preferences + JSONL 文件缓存 |
| 架构 | 单 Activity + Compose Navigation，ViewModel/StateFlow，手动 DI（AppContainer） |

## 项目结构

```
app/src/main/java/com/huanglongmao/onlinefmradio/
├── App.kt / MainActivity.kt
├── core/
│   ├── constants/      # AppConstants 全局常量
│   ├── di/             # AppContainer 手动依赖注入
│   ├── network/        # RetrofitFactory（超时/重试/通用头）
│   ├── theme/          # 8 套渐变主题
│   └── util/           # 翻译工具、电池优化等
├── data/
│   ├── cache/          # StationFileCache(JSONL) / AssetFallback
│   ├── model/          # Station / ReleaseNote 等
│   ├── remote/         # RadioBrowserApi / UpdateApi
│   ├── repository/     # StationRepository
│   └── store/          # DataStore 封装
├── player/             # PlaybackService / PlayerController / 重连 / 睡眠定时
├── store/              # 收藏/历史/更新/导入导出管理器
└── ui/                 # 路由、抽屉、17 个页面 + 公共组件
update/
└── update.json         # 应用更新版本清单（见下文「发布新版本」）
```

## 构建

要求：Android Studio（或 JDK 17+ 与 Android SDK 34+）

```bash
# 命令行构建 Debug APK
./gradlew assembleDebug

# 产物
app/build/outputs/apk/debug/app-debug.apk
```

Android Studio 打开项目根目录，直接 Run 即可。

## 发布新版本（应用内更新流程）

应用检查更新采用**静态版本清单（方案 2）**：不依赖任何后端，只读一个 JSON 文件。

1. 修改 `app/src/main/java/.../core/constants/AppConstants.kt` 中的 `APP_VERSION`
2. 编辑 [update/update.json](update/update.json)，在 `releases` **顶部**追加新版本（列表按新到旧排列，首条正式版视为最新版）：

```json
{
  "version": "v1.1.0",
  "publishedAt": "2026-10-01",
  "body": "更新说明（支持换行）",
  "apkUrl": "https://github.com/ManHuanglm/RadioFm/releases/download/v1.1.0/app-release.apk",
  "htmlUrl": "https://github.com/ManHuanglm/RadioFm/releases/tag/v1.1.0",
  "prerelease": false
}
```

3. 构建 Release APK 并上传到 GitHub Releases（tag 与 `version` 一致）
4. 提交推送 `update.json` —— 客户端 24h 内自动感知（用户也可手动强制检查）

### 清单托管地址

默认读取 GitHub raw 直链（`AppConstants.UPDATE_MANIFEST_URL`）。如需国内加速，把 `update.json` 放到任一静态托管并改这一个常量即可：

- GitHub Pages：`https://<owner>.github.io/<repo>/update.json`
- 对象存储：阿里云 OSS / 腾讯云 COS / Cloudflare R2 直链

## 开源协议

仅供学习交流使用。
