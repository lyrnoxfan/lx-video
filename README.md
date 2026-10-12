# LX视频

本地短视频播放器 —— 刷视频，就这么简单。

一个纯原生 Android 短视频 App，TikTok 式上下滑动刷视频。没有推荐算法绑架，没有臃肿功能，打开即刷。

## 功能

- **沉浸式刷视频**：上下滑动切换，全屏播放，双指缩放 1x~3x
- **长按加速**：视频左右两侧长按 2 倍速
- **随机 / 最新**：两种播放顺序，抽屉里一键切换
- **搜索**：按视频名本地搜索
- **收藏**：右滑星星收藏，本地保存
- **断点续播**：看到一半退出，下次接着看
- **下载**：视频下载到本地，随时离线看
- **分享**：系统分享，发送给朋友

所有数据（收藏、观看历史、下载）只保存在本机，不上传任何服务器。

## 技术栈

- Kotlin + Jetpack Compose（纯原生，无 WebView）
- ExoPlayer（播放器，TextureView 渲染）
- OkHttp（网络）
- DataStore（本地存储）
- Material3 + 液态玻璃风 UI

## 视频源

App 通过 HTTP API 获取视频列表（`/api/videos` 返回 JSON），视频文件走 HTTP Range 分片流式加载，支持拖动秒切。

默认配置指向示例服务器，实际使用时改成你自己的：

1. 复制 `local.properties.example` 为 `local.properties`（如果没有就新建）
2. 填入你的服务器地址：

```properties
PD_SERVER=http://你的服务器:8080
PD_PASSWORD=
```

`PD_PASSWORD` 为空时跳过登录（适用于无鉴权服务器）。

API 格式参考：

```json
[
  { "id": "abc123", "name": "视频名.mp4", "size": 12345678, "url": "/media/视频名.mp4" }
]
```

## 构建

```bash
./gradlew assembleDebug
```

产物在 `app/build/outputs/apk/debug/`。

GitHub Actions 推送到 main 分支自动构建（见 `.github/workflows/`）。

## 截图

_TODO: 放几张截图_

## License

MIT
