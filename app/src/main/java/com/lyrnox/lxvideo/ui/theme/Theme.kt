package com.lyrnox.lxvideo.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Blue = Color(0xFF2563EB)
val BlueLight = Color(0xFF60A5FA)
val Ink = Color(0xFF1E3A5F)
val Sub = Color(0xFF64748B)
val GlassWhite = Color(1f, 1f, 1f, 0.62f)
val GlassBorder = Color(1f, 1f, 1f, 0.75f)
val StrokeBlue = Color(0xFF3B82F6).copy(alpha = 0.18f)

private val Scheme = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    secondary = BlueLight,
    background = Color(0xFFF0F7FF),
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8F1FD),
    onSurfaceVariant = Sub,
    error = Color(0xFFDC2626),
)

@Composable
fun LxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

/** 蓝白渐变底（对齐网页版液态玻璃风） */
@Composable
fun GradientBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF0F7FF), Color(0xFFE8F1FD), Color(0xFFEEF4FF)),
                ),
            ),
        content = content,
    )
}

/** 液态玻璃卡片 */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
        content = content,
    )
}
