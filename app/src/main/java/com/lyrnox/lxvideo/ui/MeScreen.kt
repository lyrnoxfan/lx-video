package com.lyrnox.lxvideo.ui

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrnox.lxvideo.R
import com.lyrnox.lxvideo.data.VideoItem
import com.lyrnox.lxvideo.data.VideoStore
import com.lyrnox.lxvideo.ui.theme.Blue
import com.lyrnox.lxvideo.ui.theme.MeHeaderBrush
import com.lyrnox.lxvideo.ui.theme.Pink
import com.lyrnox.lxvideo.ui.theme.PinkBlueBrush
import com.lyrnox.lxvideo.ui.theme.glassDark
import com.lyrnox.lxvideo.util.fmtSize
import com.lyrnox.lxvideo.util.fmtTime

/**
 * 「我」页面：粉蓝渐变头图 + 毛玻璃统计卡 + 圆角卡片列表。
 * 注意：这是单用户本地播放器，没有消息后端，所以不做"消息"页。
 */
@Composable
fun MeScreen(
    videos: List<VideoItem>,
    onPickVideo: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val store = remember(ctx) { VideoStore.get(ctx) }
    val likedIds by remember { store.likedIds() }.collectAsState(initial = emptyList())
    val favIds by remember { store.favorites() }.collectAsState(initial = emptyList())
    val hist by remember { store.history() }.collectAsState(initial = emptyList())
    var showAbout by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF06090F)),
    ) {
        // 头图：粉蓝渐变 + 柔光色块
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(210.dp)
                    .background(MeHeaderBrush),
            ) {
                // 装饰柔光（Android 12+ 硬件模糊，低版本直接跳过）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Box(
                        Modifier.size(150.dp)
                            .offset(x = 220.dp, y = (-50).dp)
                            .blur(60.dp)
                            .background(Color.White.copy(alpha = 0.45f), CircleShape),
                    )
                    Box(
                        Modifier.size(110.dp)
                            .offset(x = 40.dp, y = 120.dp)
                            .blur(50.dp)
                            .background(Pink.copy(alpha = 0.5f), CircleShape),
                    )
                }
                // 头像 + 昵称
                Row(
                    modifier = Modifier.align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp)
                        .offset(y = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = "头像",
                        modifier = Modifier.size(84.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.padding(bottom = 26.dp),
                    ) {
                        Text(
                            "LX视频",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.size(2.dp))
                        Text(
                            "本地短视频播放器",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(44.dp))
        }

        // 统计卡：毛玻璃悬浮
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .glassDark(20.dp, alpha = 0.5f)
                    .padding(vertical = 16.dp),
            ) {
                Stat("获赞", likedIds.size, Modifier.weight(1f))
                Stat("收藏", favIds.size, Modifier.weight(1f))
                Stat("观看", hist.size, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        // 我的收藏
        item { SectionTitle("我的收藏") }
        val favs = favIds.mapNotNull { id -> videos.find { it.id == id } }
        if (favs.isEmpty()) {
            item {
                EmptyHint(
                    text = "还没有收藏",
                    subtitle = "刷视频时点右边的星星",
                    icon = Icons.Filled.Star,
                )
            }
        } else {
            items(favs.size) { i ->
                val v = favs[i]
                VideoEntryRow(
                    title = v.name,
                    subtitle = fmtSize(v.size),
                    onClick = { onPickVideo(v.id) },
                )
            }
        }

        // 观看历史
        item { SectionTitle("观看历史") }
        val hs = hist.mapNotNull { h -> videos.find { it.id == h.id }?.let { it to h } }
        if (hs.isEmpty()) {
            item {
                EmptyHint(
                    text = "还没有观看历史",
                    subtitle = "看过的视频会记在这里",
                    icon = Icons.Filled.History,
                )
            }
        } else {
            items(hs.size) { i ->
                val (v, h) = hs[i]
                val sub = buildString {
                    append(fmtTime(h.ts))
                    if (h.pos > 5000) append("  ·  看到 ${fmtMs(h.pos)}")
                }
                VideoEntryRow(
                    title = v.name,
                    subtitle = sub,
                    onClick = { onPickVideo(v.id) },
                )
            }
        }

        // 关于
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { showAbout = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Info,
                    null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "关于",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        // 底部悬浮导航预留
        item { Spacer(Modifier.height(110.dp)) }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

/** 统计：大字重数字 + 灰字标签，数字用粉蓝渐变提亮 */
@Composable
private fun Stat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            style = TextStyle(
                brush = PinkBlueBrush,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
        )
        Spacer(Modifier.size(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}

private fun fmtMs(ms: Long): String {
    if (ms < 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
