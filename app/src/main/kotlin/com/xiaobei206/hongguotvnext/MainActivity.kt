package com.xiaobei206.hongguotvnext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF0A0F17)
private val Panel = Color(0xFF151D29)
private val Accent = Color(0xFFB4E66A)
private val Muted = Color(0xFFA5B0C0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                background = Background,
                surface = Panel,
                onPrimary = Background,
                onBackground = Color(0xFFF5F7FA),
                onSurface = Color(0xFFF5F7FA)
            )) {
                TvBootstrapScreen()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TvBootstrapScreen() {
    val destinations = listOf("推荐", "排行榜", "新剧上线", "搜索", "收藏", "历史", "设备接力", "设置")
    var selected by remember { mutableIntStateOf(0) }
    var backCount by remember { mutableIntStateOf(0) }
    BackHandler(enabled = selected != 0 || backCount > 0) {
        if (backCount > 0) backCount = 0 else selected = 0
    }

    Row(
        modifier = Modifier.fillMaxSize().background(Background).padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(
            modifier = Modifier.width(190.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("红果 TV", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Accent)
            Text("NEXT · v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(12.dp))
            destinations.forEachIndexed { index, label ->
                var focused by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .focusable()
                        .clickable { selected = index },
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        selected == index -> Accent
                        focused -> Color(0xFF334256)
                        else -> Panel
                    },
                    border = if (focused) BorderStroke(2.dp, Color.White) else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            fontSize = 17.sp,
                            fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected == index) Background else Color(0xFFE6EAF0)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxSize().padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(destinations[selected], fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = Panel
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(30.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("独立 TV 工程已启动", color = Accent, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "当前构建用于验证 Gradle、Compose、TV 启动入口与 CI。下一步会接入开心果的数据模型、Feed 分页、封面缓存和 Media3 播放核心。",
                        color = Color(0xFFE0E6EE), fontSize = 19.sp, lineHeight = 29.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Text("此页面不包含伪造的影视数据，也不代表播放或投屏已经完成验证。", color = Muted, fontSize = 15.sp)
                }
            }
            Text("遥控器方向键可在左侧导航间移动，确认键选择；返回键回到首页。", color = Muted, fontSize = 14.sp)
        }
    }
}
