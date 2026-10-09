package com.lyrnox.lxvideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
                        VideoFeedScreen(vm = vm, onBack = { finish() })
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
