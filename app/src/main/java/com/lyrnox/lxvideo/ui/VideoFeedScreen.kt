package com.lyrnox.lxvideo.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.lyrnox.lxvideo.data.VideoItem
import com.lyrnox.lxvideo.data.VideoStore
import com.lyrnox.lxvideo.ui.theme.Blue
import com.lyrnox.lxvideo.ui.theme.Sub
import com.lyrnox.lxvideo.util.fmtSize
import com.lyrnox.lxvideo.util.fmtTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 倍速档位：1x → 1.5x → 2x → 0.5x → 1x 循环 */
private val SPEEDS = listOf(1f, 1.5f, 2f, 0.5f)

private fun speedLabel(s: Float) = when (s) {
    1f -> "1x"
    1.5f -> "1.5x"
    2f -> "2x"
    else -> "0.5x"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFeedScreen(vm: VideoFeedViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(ctx) { VideoStore.get(ctx) }
    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        if (vm.videos.isEmpty() && !vm.loading) vm.refresh()
    }

    // 单个 ExoPlayer 实例，页间复用；离开页面释放
    val player = remember(ctx) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 60_000, 1_000, 2_000)
            .build()
        ExoPlayer.Builder(ctx).setLoadControl(loadControl).build()
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    val videos = vm.videos
    val items = remember(videos) { videos.map { MediaItem.fromUri(it.url) } }
    val pagerState = rememberPagerState(pageCount = { videos.size })
    val page = pagerState.currentPage
    var lastPage by remember { mutableStateOf(0) }
    var speedIdx by remember { mutableStateOf(0) }

    // 切页：先保存切走页面的进度；换 mediaItem；断点续播；自动播放
    LaunchedEffect(page, items) {
        if (items.isEmpty() || page >= items.size) return@LaunchedEffect
        videos.getOrNull(lastPage)?.let { prev ->
            val p = player.currentPosition
            val d = player.duration
            store.saveWatch(prev.id, if (d > 0 && p >= d - 3000) 0L else p)
        }
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(items[page])
        // 断点续播：上次看到超过 5 秒且没播完，就接着播
        val vid = videos[page]
        val saved = store.position(vid.id).first()
        if (saved > 5000) {
            player.seekTo(saved)
            Toast.makeText(ctx, "已从 ${fmtFeedMs(saved)} 继续", Toast.LENGTH_SHORT).show()
        }
        items.getOrNull(page + 1)?.let { player.addMediaItem(it) }
        player.prepare()
        player.playbackParameters = PlaybackParameters(SPEEDS[speedIdx])
        player.playWhenReady = true
        vm.setIndex(page)
        lastPage = page
    }

    var showList by remember { mutableStateOf(false) }
    var showLib by remember { mutableStateOf(false) }

    /** 跳到指定视频 id 所在的页 */
    fun jumpTo(id: String) {
        val i = videos.indexOfFirst { it.id == id }
        if (i < 0) {
            Toast.makeText(ctx, "视频不在当前列表", Toast.LENGTH_SHORT).show()
            return
        }
        showLib = false
        vm.setIndex(i)
        scope.launch { pagerState.animateScrollToPage(i) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            vm.loading && videos.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Blue)
                    Spacer(Modifier.height(12.dp))
                    Text("正在加载视频…", color = Sub, fontSize = 13.sp)
                }
            }
            vm.error != null && videos.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(vm.error ?: "", color = Color(0xFFDC2626))
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { vm.refresh() }) { Text("重试", color = Blue) }
                }
            }
            videos.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无视频", color = Sub, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vm.refresh() }) { Text("刷新", color = Blue) }
                }
            }
            else -> {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { idx ->
                    FeedPage(
                        video = videos[idx],
                        player = if (idx == page) player else null,
                        isCurrent = idx == page,
                        store = store,
                        speedLabel = speedLabel(SPEEDS[speedIdx]),
                        onSpeedClick = {
                            speedIdx = (speedIdx + 1) % SPEEDS.size
                            player.playbackParameters = PlaybackParameters(SPEEDS[speedIdx])
                        },
                    )
                }

                // 顶部条：标题 + 随机/最新切换 + 收藏/历史 + 选视频
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .statusBarsPadding()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                            ),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "刷视频",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        vm.toggleOrder()
                        scope.launch { pagerState.scrollToPage(0) }
                    }) {
                        Icon(
                            Icons.Filled.Shuffle,
                            contentDescription = null,
                            tint = if (vm.order == FeedOrder.SHUFFLE) Blue else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (vm.order == FeedOrder.SHUFFLE) "随机" else "最新",
                            color = if (vm.order == FeedOrder.SHUFFLE) Blue else Color.White,
                            fontSize = 14.sp,
                        )
                    }
                    IconButton(onClick = { showLib = true }) {
                        Icon(Icons.Filled.History, "收藏/历史", tint = Color.White)
                    }
                    IconButton(onClick = { showList = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, "选视频", tint = Color.White)
                    }
                }
            }
        }

        // 选视频列表（保留原有）
        if (showList && videos.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { showList = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF101826),
            ) {
                LazyColumn(Modifier.fillMaxWidth()) {
                    itemsIndexed(videos) { i, v ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    showList = false
                                    vm.setIndex(i)
                                    scope.launch { pagerState.animateScrollToPage(i) }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                v.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(fmtSize(v.size), color = Sub, fontSize = 12.sp)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        // 收藏 / 历史二合一弹窗
        if (showLib && videos.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { showLib = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF101826),
            ) {
                var tab by remember { mutableStateOf(0) }
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                ) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("收藏") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("历史") })
                }
                val favIds by remember { store.favorites() }.collectAsState(initial = emptyList())
                val hist by remember { store.history() }.collectAsState(initial = emptyList())
                // 条目：(视频, 右侧小字)。收藏无小字，历史显示观看时间
                val entries: List<Pair<VideoItem, String?>> = remember(tab, favIds, hist, videos) {
                    if (tab == 0) {
                        favIds.mapNotNull { id -> videos.find { it.id == id }?.let { it to null } }
                    } else {
                        hist.mapNotNull { h -> videos.find { it.id == h.id }?.let { it to fmtTime(h.ts) } }
                    }
                }
                LazyColumn(Modifier.fillMaxWidth()) {
                    if (entries.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (tab == 0) "还没有收藏" else "还没有观看历史",
                                    color = Sub,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    } else {
                        items(entries.size) { i ->
                            val (v, extra) = entries[i]
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { jumpTo(v.id) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        v.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row {
                                        Text(fmtSize(v.size), color = Sub, fontSize = 12.sp)
                                        if (extra != null) {
                                            Text("  ·  $extra", color = Sub, fontSize = 12.sp)
                                        }
                                        if (tab == 1) {
                                            val h = hist.find { it.id == v.id }
                                            if (h != null && h.pos > 5000) {
                                                Text(
                                                    "  ·  看到 ${fmtFeedMs(h.pos)}",
                                                    color = Blue,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (tab == 0) {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/** 单页：当前页才挂载 player，切走后 detach */
@Composable
private fun FeedPage(
    video: VideoItem,
    player: ExoPlayer?,
    isCurrent: Boolean,
    store: VideoStore,
    speedLabel: String,
    onSpeedClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val liked by remember(video.id) { store.isLiked(video.id) }.collectAsState(initial = false)
    val fav by remember(video.id) { store.isFav(video.id) }.collectAsState(initial = false)

    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var buffered by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableLongStateOf(0L) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    var burst by remember { mutableStateOf(0) }

    DisposableEffect(player) {
        val p = player ?: return@DisposableEffect onDispose {}
        playing = p.isPlaying
        buffering = p.playbackState == Player.STATE_BUFFERING
        duration = p.duration.coerceAtLeast(0L)
        position = p.currentPosition
        buffered = p.bufferedPosition
        errMsg = p.playerError?.message
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    duration = p.duration.coerceAtLeast(0L)
                    buffering = false
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
            override fun onPlayerError(e: PlaybackException) {
                errMsg = e.message
            }
        }
        p.addListener(listener)
        onDispose { p.removeListener(listener) }
    }

    LaunchedEffect(playing, seeking) {
        val p = player ?: return@LaunchedEffect
        while (playing && !seeking) {
            position = p.currentPosition
            buffered = p.bufferedPosition
            delay(500)
        }
    }

    // 播放中每 5 秒存一次进度，用于断点续播
    LaunchedEffect(isCurrent, player) {
        if (!isCurrent || player == null) return@LaunchedEffect
        while (true) {
            delay(5000)
            if (!player.isPlaying) continue
            val pos = player.currentPosition
            val d = player.duration
            store.saveWatch(video.id, if (d > 0 && pos >= d - 3000) 0L else pos)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(isCurrent, errMsg) {
                detectTapGestures(
                    // 单击：播放/暂停
                    onTap = {
                        if (isCurrent && errMsg == null) {
                            player?.let { if (it.isPlaying) it.pause() else it.play() }
                        }
                    },
                    // 双击：点赞 + 红心动画
                    onDoubleTap = {
                        if (isCurrent && errMsg == null) {
                            burst++
                            scope.launch { store.setLiked(video.id, true) }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        if (isCurrent && buffering && errMsg == null) {
            CircularProgressIndicator(color = Color.White)
        }

        if (isCurrent && !playing && !buffering && errMsg == null) {
            Box(
                modifier = Modifier.size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, "播放", tint = Blue, modifier = Modifier.size(40.dp))
            }
        }

        // 双击红心放大动画
        if (burst > 0) {
            key(burst) {
                val scale = remember { Animatable(0.3f) }
                val alpha = remember { Animatable(1f) }
                LaunchedEffect(Unit) {
                    launch { scale.animateTo(1.1f, tween(200, easing = FastOutSlowInEasing)) }
                    alpha.animateTo(0f, tween(500, delayMillis = 120))
                }
                Icon(
                    Icons.Filled.Favorite,
                    null,
                    tint = Color(0xFFFE2C55),
                    modifier = Modifier.size(110.dp).graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    },
                )
            }
        }

        errMsg?.let { msg ->
            if (isCurrent) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text("播放失败", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        msg,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = {
                        errMsg = null
                        player?.let {
                            it.seekTo(0)
                            it.prepare()
                            it.play()
                        }
                    }) { Text("重试", color = Blue) }
                }
            }
        }

        // TikTok 式右侧竖排操作栏
        if (isCurrent && errMsg == null) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 190.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { scope.launch { store.setLiked(video.id, !liked) } }) {
                        Icon(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "点赞",
                            tint = if (liked) Color(0xFFFE2C55) else Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Text(
                        if (liked) "2" else "1",
                        color = Color.White,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                IconButton(onClick = { scope.launch { store.setFav(video.id, !fav) } }) {
                    Icon(
                        if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
                        "收藏",
                        tint = if (fav) Color(0xFFFFC107) else Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                IconButton(onClick = { downloadVideo(ctx, video) }) {
                    Icon(Icons.Filled.Download, "下载", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                IconButton(onClick = { shareVideo(ctx, video) }) {
                    Icon(Icons.Filled.Share, "分享", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onSpeedClick) {
                    Text(speedLabel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 底部：标题 + 大小 + 进度条
        if (isCurrent && errMsg == null) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        ),
                    )
                    .padding(horizontal = 12.dp)
                    .padding(top = 28.dp, bottom = 16.dp),
            ) {
                Text(
                    video.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    fmtSize(video.size),
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                val shownPos = if (seeking) seekPos else position
                val durF = duration.toFloat().coerceAtLeast(1f)
                Box(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LinearProgressIndicator(
                        progress = { (buffered.toFloat() / durF).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                        color = Color.White.copy(alpha = 0.35f),
                        trackColor = Color.Transparent,
                    )
                    Slider(
                        value = shownPos.toFloat().coerceIn(0f, durF),
                        onValueChange = { seeking = true; seekPos = it.toLong() },
                        onValueChangeFinished = {
                            player?.seekTo(seekPos)
                            position = seekPos
                            seeking = false
                        },
                        valueRange = 0f..durF,
                        enabled = duration > 0,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Blue,
                            inactiveTrackColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row {
                    Text(fmtFeedMs(shownPos), color = Color.White, fontSize = 12.sp)
                    Text(" / ", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(fmtFeedMs(duration), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    }
}

/** 用系统 DownloadManager 下载到 Download 文件夹，通知栏显示进度 */
private fun downloadVideo(ctx: Context, video: VideoItem) {
    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val req = DownloadManager.Request(Uri.parse(video.url))
        .setTitle(video.name)
        .setDescription("LX视频 · 正在下载")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, video.name)
        .setAllowedOverMetered(true)
    dm.enqueue(req)
    Toast.makeText(ctx, "已加入下载", Toast.LENGTH_SHORT).show()
}

/** 系统分享：视频名 + 签名直链 */
private fun shareVideo(ctx: Context, video: VideoItem) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${video.name}\n${video.url}")
    }
    ctx.startActivity(Intent.createChooser(send, "分享视频"))
}

private fun fmtFeedMs(ms: Long): String {
    if (ms < 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
