package com.lyrnox.lxvideo.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import com.lyrnox.lxvideo.ui.theme.FavGold
import com.lyrnox.lxvideo.ui.theme.LikeRed
import com.lyrnox.lxvideo.ui.theme.Pink
import com.lyrnox.lxvideo.ui.theme.PinkBlueBrush
import com.lyrnox.lxvideo.ui.theme.Sub
import com.lyrnox.lxvideo.ui.theme.TimeStyle
import com.lyrnox.lxvideo.ui.theme.TimeTotalStyle
import com.lyrnox.lxvideo.ui.theme.glassDark
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

/** 底部悬浮导航预留，避免遮挡进度条 */
private val NavBarReserve = 100.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFeedScreen(
    vm: VideoFeedViewModel,
    paused: Boolean,
    jumpId: String?,
    onJumpDone: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(ctx) { VideoStore.get(ctx) }

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

    // 切到"我"页时暂停播放（保留进度和页码）
    LaunchedEffect(paused) {
        if (paused) player.pause()
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

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showList by remember { mutableStateOf(false) }
    var showLib by remember { mutableStateOf(false) }
    var libTab by remember { mutableStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }
    // 控制层（顶栏/右侧栏/底部条）显隐：点按显示，播放中 3 秒无操作自动淡出
    var controlsVisible by remember { mutableStateOf(true) }

    /** 跳到指定视频 id 所在的页 */
    fun jumpTo(id: String) {
        val i = videos.indexOfFirst { it.id == id }
        if (i < 0) {
            Toast.makeText(ctx, "视频不在当前列表", Toast.LENGTH_SHORT).show()
            return
        }
        showList = false
        showLib = false
        searching = false
        vm.setIndex(i)
        scope.launch { pagerState.animateScrollToPage(i) }
    }

    // 外部跳播请求（从"我"页点条目过来）
    LaunchedEffect(jumpId) {
        if (jumpId != null) {
            jumpTo(jumpId)
            onJumpDone()
        }
    }

    /** 打开抽屉里的列表弹窗：先关抽屉再弹 */
    fun openSheet(tab: Int) {
        scope.launch {
            drawerState.close()
            libTab = tab
            showLib = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0B1220).copy(alpha = 0.82f),
                drawerContentColor = Color.White,
                modifier = Modifier.statusBarsPadding(),
            ) {
                // 抽屉头部：粉蓝渐变标题
                Text(
                    "LX视频",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        brush = PinkBlueBrush,
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(Modifier.height(8.dp))
                DrawerRow(
                    icon = Icons.Filled.Shuffle,
                    label = "播放顺序",
                    trailing = if (vm.order == FeedOrder.SHUFFLE) "随机" else "最新",
                    onClick = {
                        vm.toggleOrder()
                        scope.launch {
                            drawerState.close()
                            pagerState.scrollToPage(0)
                        }
                    },
                )
                DrawerRow(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "选择视频",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            showList = true
                        }
                    },
                )
                DrawerRow(
                    icon = Icons.Filled.Star,
                    label = "我的收藏",
                    onClick = { openSheet(0) },
                )
                DrawerRow(
                    icon = Icons.Filled.History,
                    label = "观看历史",
                    onClick = { openSheet(1) },
                )
                DrawerRow(
                    icon = Icons.Filled.Info,
                    label = "关于",
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAbout = true
                    },
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when {
                vm.loading && videos.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingPlaceholder("正在加载视频…")
                }
                vm.error != null && videos.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorPlaceholder(
                        message = vm.error ?: "加载失败",
                        icon = Icons.Filled.VideocamOff,
                        onRetry = { vm.refresh() },
                    )
                }
                videos.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyHint(
                            text = "这里空空如也",
                            subtitle = "服务端还没有视频，去传一点吧",
                            icon = Icons.Filled.VideocamOff,
                        )
                        GlassTextButton("刷新", onClick = { vm.refresh() })
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
                            controlsVisible = controlsVisible,
                            onControlsChange = { controlsVisible = it },
                            speedLabel = speedLabel(SPEEDS[speedIdx]),
                            onSpeedClick = {
                                speedIdx = (speedIdx + 1) % SPEEDS.size
                                player.playbackParameters = PlaybackParameters(SPEEDS[speedIdx])
                            },
                        )
                    }

                    // 顶栏：悬浮毛玻璃胶囊（左菜单 / 右搜索）
                    AnimatedVisibility(
                        visible = controlsVisible && !searching,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .glassDark(999.dp, alpha = 0.38f)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, "菜单", tint = Color.White)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "LX视频",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Filled.Search, "搜索", tint = Color.White)
                            }
                        }
                    }
                    if (searching) {
                        // 搜索模式：返回 + 输入框（毛玻璃胶囊）
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .glassDark(999.dp, alpha = 0.55f)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { searching = false; query = "" }) {
                                Icon(Icons.Filled.Close, "关闭搜索", tint = Color.White)
                            }
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("搜索视频名", color = Sub, fontSize = 15.sp) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Blue,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // 搜索结果：按视频名本地过滤
                        val results = remember(query, videos) {
                            if (query.isBlank()) emptyList()
                            else videos.filter { it.name.contains(query, ignoreCase = true) }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = 104.dp)
                                .padding(horizontal = 12.dp)
                                .glassDark(20.dp, alpha = 0.72f),
                        ) {
                            if (results.isEmpty()) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("没有匹配的视频", color = Sub, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                items(results.size) { i ->
                                    val v = results[i]
                                    VideoEntryRow(
                                        title = v.name,
                                        subtitle = fmtSize(v.size),
                                        onClick = { jumpTo(v.id) },
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }

            // 选视频列表
            if (showList && videos.isNotEmpty()) {
                VideoListSheet(
                    title = "选择视频",
                    onDismiss = { showList = false },
                ) {
                    itemsIndexed(videos) { i, v ->
                        VideoEntryRow(
                            title = v.name,
                            subtitle = fmtSize(v.size),
                            onClick = {
                                showList = false
                                vm.setIndex(i)
                                scope.launch { pagerState.animateScrollToPage(i) }
                            },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }

            // 收藏 / 历史二合一弹窗
            if (showLib && videos.isNotEmpty()) {
                VideoListSheet(
                    title = null,
                    onDismiss = { showLib = false },
                ) {
                    item {
                        TabRow(
                            selectedTabIndex = libTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ) {
                            Tab(selected = libTab == 0, onClick = { libTab = 0 }, text = { Text("收藏") })
                            Tab(selected = libTab == 1, onClick = { libTab = 1 }, text = { Text("历史") })
                        }
                    }
                    item {
                        LibEntries(
                            tab = libTab,
                            videos = videos,
                            store = store,
                            onPick = { jumpTo(it) },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }

            if (showAbout) {
                AboutDialog(onDismiss = { showAbout = false })
            }
        }
    }
}

/** 抽屉里的一行 */
@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (trailing != null) {
                    Text(trailing, color = Blue, fontSize = 13.sp)
                }
            }
        },
        icon = { Icon(icon, null, tint = Color.White.copy(alpha = 0.85f)) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
        ),
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

/** 深色毛玻璃底部弹窗容器 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoListSheet(
    title: String?,
    onDismiss: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0D1420).copy(alpha = 0.94f),
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f)),
            )
        },
    ) {
        if (title != null) {
            Text(
                title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        LazyColumn(Modifier.fillMaxWidth(), content = content)
    }
}

/** 收藏/历史条目列表（供抽屉弹窗和"我"页复用逻辑） */
@Composable
private fun LibEntries(
    tab: Int,
    videos: List<VideoItem>,
    store: VideoStore,
    onPick: (String) -> Unit,
) {
    val favIds by remember { store.favorites() }.collectAsState(initial = emptyList())
    val hist by remember { store.history() }.collectAsState(initial = emptyList())
    if (tab == 0) {
        val favs = remember(favIds, videos) {
            favIds.mapNotNull { id -> videos.find { it.id == id } }
        }
        if (favs.isEmpty()) {
            EmptyHint("还没有收藏")
        } else {
            favs.forEach { v ->
                VideoEntryRow(
                    title = v.name,
                    subtitle = fmtSize(v.size),
                    onClick = { onPick(v.id) },
                    trailing = {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    } else {
        val hs = remember(hist, videos) {
            hist.mapNotNull { h -> videos.find { it.id == h.id }?.let { it to h } }
        }
        if (hs.isEmpty()) {
            EmptyHint("还没有观看历史")
        } else {
            hs.forEach { (v, h) ->
                val sub = buildString {
                    append(fmtSize(v.size))
                    append("  ·  ${fmtTime(h.ts)}")
                    if (h.pos > 5000) append("  ·  看到 ${fmtFeedMs(h.pos)}")
                }
                VideoEntryRow(
                    title = v.name,
                    subtitle = sub,
                    onClick = { onPick(v.id) },
                )
            }
        }
    }
}

/** 关于弹窗：只说真实情况，不虚构功能 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            GlassTextButton("知道了", onClick = onDismiss)
        },
        title = {
            Text(
                "关于",
                style = TextStyle(brush = PinkBlueBrush, fontWeight = FontWeight.Bold, fontSize = 18.sp),
            )
        },
        text = {
            Text(
                "LX视频 v1.3.0\n本地短视频播放器，视频来自你的 Polydrive。\n点赞 / 收藏 / 观看历史只保存在本机，不上传。",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
        },
        containerColor = Color(0xFF0D1420).copy(alpha = 0.96f),
        shape = RoundedCornerShape(24.dp),
    )
}

/** 右侧操作栏按钮：毛玻璃圆钮 + 下方小字计数，激活态带光晕 */
@Composable
private fun ActionItem(
    icon: ImageVector,
    desc: String,
    tint: Color = Color.White,
    glow: Color? = null,
    count: String? = null,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(50.dp)
                .then(if (glow != null) Modifier.shadow(12.dp, CircleShape, ambientColor = glow.copy(alpha = 0.5f)) else Modifier)
                .glassDark(999.dp, alpha = 0.42f)
                .pressScale(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, desc, tint = tint, modifier = Modifier.size(26.dp))
        }
        if (count != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                count,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
}

/** 单页：当前页才挂载 player，切走后 detach */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedPage(
    video: VideoItem,
    player: ExoPlayer?,
    isCurrent: Boolean,
    store: VideoStore,
    controlsVisible: Boolean,
    onControlsChange: (Boolean) -> Unit,
    speedLabel: String,
    onSpeedClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val liked by remember(video.id) { store.isLiked(video.id) }.collectAsState(initial = false)
    val fav by remember(video.id) { store.isFav(video.id) }.collectAsState(initial = false)
    val favTotal by remember { store.favorites() }.collectAsState(initial = emptyList())

    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var buffered by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableLongStateOf(0L) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    var burst by remember { mutableStateOf(0) }

    // 播放中 3 秒无操作，控制层自动淡出；点按任意处唤回
    LaunchedEffect(controlsVisible, playing, isCurrent) {
        if (controlsVisible && playing && isCurrent && errMsg == null) {
            delay(3000)
            onControlsChange(false)
        }
    }

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
                    // 单击：先唤出控制层，再播放/暂停
                    onTap = {
                        if (isCurrent && errMsg == null) {
                            onControlsChange(true)
                            player?.let { if (it.isPlaying) it.pause() else it.play() }
                        }
                    },
                    // 双击：点赞 + 弹性红心动画
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
            RefinedSpinner()
        }

        if (isCurrent && !playing && !buffering && errMsg == null) {
            Box(
                modifier = Modifier.size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .shadow(16.dp, CircleShape, ambientColor = Color.White.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, "播放", tint = Blue, modifier = Modifier.size(42.dp))
            }
        }

        // 双击红心：弹性缩放 + 淡出
        if (burst > 0) {
            key(burst) {
                val scale = remember { Animatable(0.2f) }
                val alpha = remember { Animatable(1f) }
                LaunchedEffect(Unit) {
                    launch {
                        scale.animateTo(
                            1.15f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                    alpha.animateTo(0f, tween(550, delayMillis = 150))
                }
                Icon(
                    Icons.Filled.Favorite,
                    null,
                    tint = LikeRed,
                    modifier = Modifier.size(120.dp)
                        .shadow(24.dp, CircleShape, ambientColor = LikeRed.copy(alpha = 0.55f))
                        .graphicsLayer {
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
                    modifier = Modifier.padding(40.dp),
                ) {
                    ErrorPlaceholder(
                        message = msg,
                        icon = Icons.Filled.VideocamOff,
                        onRetry = {
                            errMsg = null
                            player?.let {
                                it.seekTo(0)
                                it.prepare()
                                it.play()
                            }
                        },
                    )
                }
            }
        }

        // 右侧竖排操作栏：毛玻璃圆钮，点按控制层时淡入淡出
        AnimatedVisibility(
            visible = isCurrent && errMsg == null && controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Column(
                modifier = Modifier.padding(end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ActionItem(
                    icon = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    desc = "点赞",
                    tint = if (liked) LikeRed else Color.White,
                    glow = if (liked) LikeRed else null,
                    count = if (liked) "2" else "1",
                    onClick = { scope.launch { store.setLiked(video.id, !liked) } },
                )
                Spacer(Modifier.height(12.dp))
                ActionItem(
                    icon = if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
                    desc = "收藏",
                    tint = if (fav) FavGold else Color.White,
                    glow = if (fav) FavGold else null,
                    count = favTotal.size.toString(),
                    onClick = { scope.launch { store.setFav(video.id, !fav) } },
                )
                Spacer(Modifier.height(12.dp))
                ActionItem(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    desc = "分享",
                    onClick = { shareVideo(ctx, video) },
                )
                Spacer(Modifier.height(12.dp))
                ActionItem(
                    icon = Icons.Filled.Download,
                    desc = "下载",
                    onClick = { downloadVideo(ctx, video) },
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.size(50.dp)
                        .glassDark(999.dp, alpha = 0.42f)
                        .pressScale(onClick = onSpeedClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(speedLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 底部：等宽时间 + 发光细进度条（可拖动），随控制层淡入淡出
        AnimatedVisibility(
            visible = isCurrent && errMsg == null && controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        ),
                    )
                    .padding(horizontal = 18.dp)
                    .padding(top = 26.dp, bottom = NavBarReserve),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val shownPos = if (seeking) seekPos else position
                val durF = duration.toFloat().coerceAtLeast(1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(fmtFeedMs(shownPos), style = TimeStyle)
                    Text(" / ", style = TimeTotalStyle)
                    Text(fmtFeedMs(duration), style = TimeTotalStyle)
                }
                Spacer(Modifier.height(4.dp))
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
                    thumb = {
                        Box(
                            Modifier.size(15.dp)
                                .shadow(10.dp, CircleShape, ambientColor = Pink.copy(alpha = 0.8f), spotColor = Pink)
                                .background(Color.White, CircleShape),
                        )
                    },
                    track = { sliderState ->
                        Box(
                            Modifier.fillMaxWidth().height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        ) {
                            // 底轨
                            Box(
                                Modifier.fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.22f)),
                            )
                            // 已缓冲
                            val bufFrac = (buffered.toFloat() / durF).coerceIn(0f, 1f)
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(bufFrac)
                                    .background(Color.White.copy(alpha = 0.4f)),
                            )
                            // 已播放：粉蓝渐变
                            val playFrac = (sliderState.value / durF).coerceIn(0f, 1f)
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(playFrac)
                                    .background(PinkBlueBrush),
                            )
                        }
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
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
