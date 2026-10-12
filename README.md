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

## 打造你自己的视频源

App 本身不带视频，视频来自你自己的服务器。原理很简单：

```
你的服务器          App
  /media/  ←——  视频文件直链（MP4）
  /api/videos  ←——  视频列表 JSON
```

### 第 1 步：准备一台服务器

任意 VPS/云主机，有公网 IP 就行。以下用 Debian + nginx 举例。

### 第 2 步：放视频文件

把 MP4 传到服务器，比如 `/var/www/videos/`：

```bash
mkdir -p /var/www/videos
# 把你的 mp4 传进来（scp / rsync / 网盘都行）
```

### 第 3 步：生成视频列表 JSON

在服务器上跑这个 Python 脚本，扫描目录生成 `/api/videos` 要的 JSON：

```python
#!/usr/bin/env python3
"""扫描视频目录，生成 videos.json"""
import json, os

VIDEO_DIR = "/var/www/videos"
BASE_URL = "http://你的服务器IP:8080"  # 改成你的

videos = []
for i, fn in enumerate(sorted(os.listdir(VIDEO_DIR))):
    if not fn.lower().endswith((".mp4", ".mkv", ".webm", ".mov")):
        continue
    fp = os.path.join(VIDEO_DIR, fn)
    videos.append({
        "id": f"v{i}",
        "name": fn,
        "size": os.path.getsize(fp),
        "url": f"{BASE_URL}/media/{fn}",
    })

with open("/var/www/videos.json", "w") as f:
    json.dump(videos, f, ensure_ascii=False)
print(f"共 {len(videos)} 个视频")
```

### 第 4 步：配 nginx

```nginx
server {
    listen 8080;
    # 视频文件：支持 Range 分片（拖动秒切就靠它）
    location /media/ {
        alias /var/www/videos/;
    }
    # 视频列表 API
    location = /api/videos {
        alias /var/www/videos.json;
        # 或者用 alias + try_files，根据你的 nginx 版本调整
    }
}
```

确保 `http://你的服务器IP:8080/api/videos` 能返回 JSON，
`http://你的服务器IP:8080/media/xxx.mp4` 能下载视频。

### 第 5 步：App 填服务器地址

在项目根目录建 `local.properties`（已 gitignore，不会提交）：

```properties
PD_SERVER=http://你的服务器IP:8080
PD_PASSWORD=
```

- `PD_SERVER`：你的服务器地址（注意 `http://` 开头，App 已允许明文流量）
- `PD_PASSWORD`：如果你的 API 需要登录才填，否则留空跳过

### 加新视频

以后加视频只需两步：

1. 把新 MP4 传到服务器的视频目录
2. 重新跑一遍上面的 Python 脚本更新 `videos.json`

App 下次启动自动拉取新列表，不用重新打包。

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
