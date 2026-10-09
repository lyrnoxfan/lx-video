package com.lyrnox.lxvideo.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.lyrnox.lxvideo.data.VideoItem
import com.lyrnox.lxvideo.ui.theme.Blue
import com.lyrnox.lxvideo.ui.theme.Sub
import com.lyrnox.lxvideo.util.fmtSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFeedScreen(vm: VideoFeedViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
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

    // 切页：换 mediaItem 并自动播放；同时把下一条加入播放列表预加载
    LaunchedEffect(page, items) {
        if (items.isEmpty() || page >= items.size) return@LaunchedEffect
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(items[page])
        items.getOrNull(page + 1)?.let { player.addMediaItem(it) }
        player.prepare()
        player.playWhenReady = true
        vm.setIndex(page)
    }

    var showList by remember { mutableStateOf(false) }

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
                    )
                }

                // 顶部条：标题 + 随机/最新切换 + 选视频
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
                    IconButton(onClick = { showList = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, "选视频", tint = Color.White)
                    }
                }
            }
        }

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
    }
}

/** 单页：当前页才挂载 player，切走后 detach */
@Composable
private fun FeedPage(video: VideoItem, player: ExoPlayer?, isCurrent: Boolean) {
    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var buffered by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableLongStateOf(0L) }
    var errMsg by remember { mutableStateOf<String?>(null) }

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

    Box(
        modifier = Modifier.fillMaxSize()
            .clickable(enabled = isCurrent && errMsg == null) {
                player?.let { if (it.isPlaying) it.pause() else it.play() }
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

        // 底部：文件名 + 简单进度条（风格贴近现有播放器）
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

private fun fmtFeedMs(ms: Long): String {
    if (ms < 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
