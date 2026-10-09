package com.lyrnox.lxvideo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrnox.lxvideo.R
import com.lyrnox.lxvideo.data.VideoItem
import com.lyrnox.lxvideo.data.VideoStore
import com.lyrnox.lxvideo.util.fmtSize
import com.lyrnox.lxvideo.util.fmtTime

/**
 * 「我」页面：头像 + 昵称 + 本地统计（获赞/收藏/观看），
 * 下面是我的收藏 / 观看历史 / 关于。点条目跳回首页播放。
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

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
    ) {
        // 头部：头像 + 昵称
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.avatar),
                contentDescription = "头像",
                modifier = Modifier.size(72.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text("LX视频", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("本地短视频播放器", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        }

        // 统计：获赞 / 收藏 / 观看（全部读本地）
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat("获赞", likedIds.size)
            Stat("收藏", favIds.size)
            Stat("观看", hist.size)
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.padding(vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            // 我的收藏
            item { SectionTitle("我的收藏") }
            val favs = favIds.mapNotNull { id -> videos.find { it.id == id } }
            if (favs.isEmpty()) {
                item { EmptyHint("还没有收藏，去刷视频时点右边的星星吧") }
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
                item { EmptyHint("还没有观看历史") }
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
                        .clickable { showAbout = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Info,
                        null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("关于", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Filled.ChevronRight,
                        null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            // 底部导航栏预留
            item { Spacer(Modifier.height(92.dp)) }
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

/** 统计数字：数字白粗 + 下方灰字标签 */
@Composable
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

private fun fmtMs(ms: Long): String {
    if (ms < 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
