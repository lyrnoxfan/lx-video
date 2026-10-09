package com.lyrnox.lxvideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lyrnox.lxvideo.data.Api
import com.lyrnox.lxvideo.ui.MeScreen
import com.lyrnox.lxvideo.ui.VideoFeedScreen
import com.lyrnox.lxvideo.ui.VideoFeedVMFactory
import com.lyrnox.lxvideo.ui.VideoFeedViewModel
import com.lyrnox.lxvideo.ui.theme.Blue
import com.lyrnox.lxvideo.ui.theme.GradientBackground
import com.lyrnox.lxvideo.ui.theme.LxTheme
import com.lyrnox.lxvideo.ui.theme.Sub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LxTheme {
                var api by remember { mutableStateOf<Api?>(null) }
                var err by remember { mutableStateOf<String?>(null) }
                var attempt by remember { mutableIntStateOf(0) }

                // 内置服务器地址和密码，开机直连（用户要求免登录）
                LaunchedEffect(attempt) {
                    err = null
                    try {
                        val a = withContext(Dispatchers.IO) {
                            Api(BuildConfig.PD_SERVER).also { it.login(BuildConfig.PD_PASSWORD) }
                        }
                        api = a
                    } catch (e: Exception) {
                        err = "连接失败: ${e.message}"
                    }
                }

                // 401（服务端改密码）→ 回到错误页，可重试
                fun onUnauthorized() {
                    api = null
                    err = "会话失效，请重试"
                }

                val current = api
                when {
                    current != null -> {
                        val vm: VideoFeedViewModel =
                            viewModel(factory = remember(current) { VideoFeedVMFactory(current) })
                        LaunchedEffect(current) { vm.onUnauthorized = ::onUnauthorized }

                        // 底部导航：只有「首页 / 我」。注意：这是单用户本地播放器，
                        // 没有消息后端，所以不做"消息"tab（UI 不许谎报）。
                        var tab by remember { mutableIntStateOf(0) }
                        var jumpId by remember { mutableStateOf<String?>(null) }
                        BackHandler {
                            if (tab == 1) tab = 0 else finish()
                        }

                        Box(
                            Modifier.fillMaxSize()
                                .background(Color.Black),
                        ) {
                            // 首页视频流：切到"我"时暂停但保留在组合中（进度/页码不丢）
                            VideoFeedScreen(
                                vm = vm,
                                paused = tab == 1,
                                jumpId = jumpId,
                                onJumpDone = { jumpId = null },
                            )
                            // "我"页盖在视频流之上
                            if (tab == 1) {
                                MeScreen(
                                    videos = vm.videos,
                                    onPickVideo = { id ->
                                        jumpId = id
                                        tab = 0
                                    },
                                )
                            }
                            // 底部导航栏（常驻）
                            NavigationBar(
                                containerColor = Color.Black,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            ) {
                                val colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color.White.copy(alpha = 0.45f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.45f),
                                    indicatorColor = Color.Transparent,
                                )
                                NavigationBarItem(
                                    selected = tab == 0,
                                    onClick = { tab = 0 },
                                    icon = { Icon(Icons.Filled.Home, "首页") },
                                    label = { Text("首页") },
                                    colors = colors,
                                )
                                NavigationBarItem(
                                    selected = tab == 1,
                                    onClick = { tab = 1 },
                                    icon = { Icon(Icons.Filled.Person, "我") },
                                    label = { Text("我") },
                                    colors = colors,
                                )
                            }
                        }
                    }
                    err != null -> GradientBackground {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                Text(err ?: "", color = Color(0xFFDC2626))
                                Spacer(Modifier.height(8.dp))
                                Text("请检查网络或服务端状态", color = Sub)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { attempt++ }) { Text("重试") }
                            }
                        }
                    }
                    else -> GradientBackground {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Blue)
                        }
                    }
                }
            }
        }
    }
}
