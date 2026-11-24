package com.xiaobei206.hongguotvnext

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.xiaobei206.hongguotvnext.model.AppModel
import com.xiaobei206.hongguotvnext.model.Player
import com.xiaobei206.hongguotvnext.model.PlayerState
import com.xiaobei206.hongguotvnext.model.TimeControl
import com.xiaobei206.hongguotvnext.playback.PlaybackDiagnostics
import kotlinx.coroutines.delay
import java.util.Locale

private val PlayerHudGreen = Color(0xFF9DFFB0)
private val PlayerHudPanel = Color(0xE610141B)
private val PlayerHudText = Color(0xFFF5F7FA)
private val PlayerHudMuted = Color(0xFFB7C0CC)

/**
 * TV-first player controls:
 * - hidden HUD: left/right seek, up/down change episode, OK reveals the HUD
 * - HUD visible without button focus: left/right still seek; OK pauses and focuses Play/Pause
 * - button focus: OK activates buttons, arrows navigate controls
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable
internal fun TvPlayerScreen(model: AppModel, exo: ExoPlayer, onBack: () -> Unit) {
    val player = model.player
    val context = LocalContext.current
    val rootFocus = remember { FocusRequester() }
    val pauseFocus = remember { FocusRequester() }

    var menuVisible by remember { mutableStateOf(true) }
    var controlsFocused by remember { mutableStateOf(false) }
    var hideDelayMs by remember { mutableLongStateOf(650L) }
    var timerKey by remember { mutableLongStateOf(0L) }
    var backArmedUntil by remember { mutableLongStateOf(0L) }
    var hint by remember { mutableStateOf<String?>(null) }
    var hintKey by remember { mutableLongStateOf(0L) }
    var diagnosticsVisible by remember { mutableStateOf(false) }
    var diagnosticSnapshot by remember { mutableStateOf(PlaybackDiagnostics.snapshot()) }

    fun showMenu(timeoutMs: Long, focusControls: Boolean = false) {
        menuVisible = true
        hideDelayMs = timeoutMs
        controlsFocused = focusControls
        timerKey++
        if (focusControls) {
            runCatching { pauseFocus.requestFocus() }
        } else {
            runCatching { rootFocus.requestFocus() }
        }
    }

    fun showSeekHint(text: String) {
        hint = text
        hintKey++
    }

    fun handleBack() {
        val now = System.currentTimeMillis()
        if (menuVisible) {
            menuVisible = false
            controlsFocused = false
            backArmedUntil = now + 2_000L
            runCatching { rootFocus.requestFocus() }
        } else if (now <= backArmedUntil) {
            backArmedUntil = 0L
            onBack()
        } else {
            backArmedUntil = now + 2_000L
            Toast.makeText(context, "再按一次返回退出播放", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        handleBack()
    }

    LaunchedEffect(Unit) {
        runCatching { rootFocus.requestFocus() }
    }

    // A manual or automatic episode change briefly shows the episode/menu HUD.
    LaunchedEffect(player.number) {
        showMenu(500L)
    }

    LaunchedEffect(menuVisible, controlsFocused, timerKey) {
        if (!menuVisible) return@LaunchedEffect
        delay(hideDelayMs)
        menuVisible = false
        controlsFocused = false
        runCatching { rootFocus.requestFocus() }
    }

    LaunchedEffect(hintKey) {
        if (hint == null) return@LaunchedEffect
        delay(800L)
        hint = null
    }

    LaunchedEffect(diagnosticsVisible, player.number) {
        if (!diagnosticsVisible) return@LaunchedEffect
        while (diagnosticsVisible) {
            diagnosticSnapshot = PlaybackDiagnostics.snapshot()
            delay(250L)
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .onFocusChanged { if (it.isFocused && menuVisible) controlsFocused = false }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Back, Key.Escape -> {
                        handleBack()
                        true
                    }
                    Key.DirectionLeft -> {
                        if (menuVisible && controlsFocused) false
                        else {
                            player.skip(-10.0)
                            showSeekHint("−10s")
                            if (menuVisible) {
                                hideDelayMs = 3_000L
                                timerKey++
                            }
                            true
                        }
                    }
                    Key.DirectionRight -> {
                        if (menuVisible && controlsFocused) false
                        else {
                            player.skip(10.0)
                            showSeekHint("+10s")
                            if (menuVisible) {
                                hideDelayMs = 3_000L
                                timerKey++
                            }
                            true
                        }
                    }
                    Key.DirectionUp -> {
                        if (menuVisible && !controlsFocused) {
                            showMenu(5_000L, focusControls = true)
                            true
                        } else if (menuVisible && controlsFocused) {
                            true
                        } else {
                            player.step(-1)
                            true
                        }
                    }
                    Key.DirectionDown -> {
                        if (menuVisible && !controlsFocused) {
                            showMenu(5_000L, focusControls = true)
                            true
                        } else if (menuVisible && controlsFocused) {
                            true
                        } else {
                            player.step(1)
                            true
                        }
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        if (!menuVisible) {
                            showMenu(3_000L)
                            true
                        } else if (!controlsFocused) {
                            player.togglePause()
                            showMenu(5_000L, focusControls = true)
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    player = exo
                    setKeepContentOnPlayerReset(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
            },
            update = { view -> if (view.player !== exo) view.player = exo },
            modifier = Modifier.fillMaxSize()
        )

        // This is an informational overlay, intentionally independent of the
        // controls HUD: it stays visible until toggled off, like Stats for nerds.
        if (diagnosticsVisible) {
            StatsForNerds(snapshot = diagnosticSnapshot, player = player)
        }

        hint?.let {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xD9000000),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    color = Color.White,
                    fontSize = 25.sp
                )
            }
        }

        val failure = player.state as? PlayerState.Failed
        if (failure != null) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(28.dp).fillMaxWidth(),
                color = Color(0xF01A202B),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("播放失败", color = PlayerHudText, fontSize = 24.sp)
                    Text(failure.message, color = PlayerHudMuted, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { player.retry(); showMenu(5_000L, true) }) { Text("重新尝试") }
                        if (failure.diagnostic?.canRefreshDevice == true) {
                            TextButton(onClick = { player.retryWithFreshDevice(); showMenu(5_000L, true) }) {
                                Text("刷新匿名设备并重试", color = PlayerHudGreen)
                            }
                        }
                    }
                }
            }
        }

        // No controls are composed at all while hidden: there can be no
        // stale button silhouettes or clickable remnants after the timeout.
        if (menuVisible) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, bottom = 24.dp),
                color = PlayerHudPanel,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                player.page?.series?.title.orEmpty().ifBlank { "红果 TV 播放器" },
                                color = PlayerHudText,
                                fontSize = 19.sp
                            )
                            Text(
                                "第 ${player.number} 集  ·  ${player.video.position.toTimeLabel()} / ${player.video.duration.toTimeLabel()}  ·  ${if (player.video.status == TimeControl.PLAYING) "播放中" else if (player.video.status == TimeControl.WAITING) "缓冲中" else "已暂停"}",
                                color = PlayerHudMuted,
                                fontSize = 13.sp
                            )
                        }
                        TextButton(onClick = {
                            diagnosticsVisible = !diagnosticsVisible
                            if (diagnosticsVisible) diagnosticSnapshot = PlaybackDiagnostics.snapshot()
                            showMenu(5_000L, true)
                        }) {
                            Text(if (diagnosticsVisible) "关闭诊断" else "播放诊断", color = PlayerHudGreen)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            modifier = Modifier.onFocusChanged {
                                if (it.isFocused) {
                                    controlsFocused = true
                                    hideDelayMs = 5_000L
                                    timerKey++
                                }
                            }.focusRequester(pauseFocus),
                            onClick = {
                                player.togglePause()
                                showMenu(5_000L, true)
                            }
                        ) {
                            Text(if (player.video.wantsToPlay && player.video.status != TimeControl.PAUSED) "暂停" else "播放")
                        }
                        TextButton(
                            modifier = Modifier.onFocusChanged { if (it.isFocused) { controlsFocused = true; hideDelayMs = 5_000L; timerKey++ } },
                            onClick = { player.skip(-10.0); showSeekHint("−10s"); showMenu(5_000L, true) }
                        ) { Text("后退 10 秒", color = PlayerHudText) }
                        TextButton(
                            modifier = Modifier.onFocusChanged { if (it.isFocused) { controlsFocused = true; hideDelayMs = 5_000L; timerKey++ } },
                            onClick = { player.skip(10.0); showSeekHint("+10s"); showMenu(5_000L, true) }
                        ) { Text("快进 10 秒", color = PlayerHudText) }
                        TextButton(
                            modifier = Modifier.onFocusChanged { if (it.isFocused) { controlsFocused = true; hideDelayMs = 5_000L; timerKey++ } },
                            onClick = { player.step(-1); showMenu(500L) }
                        ) { Text("上一集", color = PlayerHudText) }
                        TextButton(
                            modifier = Modifier.onFocusChanged { if (it.isFocused) { controlsFocused = true; hideDelayMs = 5_000L; timerKey++ } },
                            onClick = { player.step(1); showMenu(500L) }
                        ) { Text("下一集", color = PlayerHudText) }
                        TextButton(
                            modifier = Modifier.onFocusChanged { if (it.isFocused) { controlsFocused = true; hideDelayMs = 5_000L; timerKey++ } },
                            onClick = {
                                val speeds = Player.speeds
                                val index = speeds.indexOf(player.speed).coerceAtLeast(-1)
                                player.chooseSpeed(speeds[(index + 1) % speeds.size])
                                showMenu(5_000L, true)
                            }
                        ) { Text(if (player.speed == 1f) "倍速" else "${player.speed}×", color = PlayerHudText) }
                        if (player.qualities.isNotEmpty()) {
                            TextButton(
                                modifier = Modifier.onFocusChanged { if (it.isFocused) { controlsFocused = true; hideDelayMs = 5_000L; timerKey++ } },
                                onClick = {
                                    val qualities = player.qualities
                                    val current = qualities.indexOf(player.quality).coerceAtLeast(-1)
                                    player.chooseQuality(qualities[(current + 1) % qualities.size])
                                    showMenu(5_000L, true)
                                }
                            ) { Text(if (player.quality > 0) "${player.quality}P" else "清晰度", color = PlayerHudText) }
                        }
                        TextButton(
                            modifier = Modifier.onFocusChanged { if (it.isFocused) { controlsFocused = true; hideDelayMs = 5_000L; timerKey++ } },
                            onClick = { onBack() }
                        ) { Text("退出播放", color = PlayerHudMuted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsForNerds(
    snapshot: PlaybackDiagnostics.Snapshot,
    player: Player
) {
    val networkMbps = snapshot.connectionSpeedBps / 1_000_000.0
    val bitrateMbps = snapshot.bitrate / 1_000_000.0
    Surface(
        modifier = Modifier.padding(24.dp).width(390.dp).align(Alignment.TopStart),
        color = Color(0xE8000000),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("STATS FOR NERDS", color = Color.White, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("VIDEO  ${snapshot.videoCodec.ifBlank { "—" }}", color = PlayerHudGreen, fontSize = 12.sp)
            Text("AUDIO  ${snapshot.audioCodec.ifBlank { "—" }}", color = PlayerHudGreen, fontSize = 12.sp)
            Text("DECODER  ${snapshot.decoderName.ifBlank { "—" }}  ·  ${when (snapshot.decoderHardware) { true -> "HARDWARE"; false -> "SOFTWARE"; null -> "—" }}", color = PlayerHudGreen, fontSize = 12.sp)
            Text("RESOLUTION  ${if (snapshot.width > 0 && snapshot.height > 0) "${snapshot.width}×${snapshot.height}" else "—"}", color = PlayerHudGreen, fontSize = 12.sp)
            Text("FRAME RATE  ${if (snapshot.frameRate > 0f) String.format(Locale.ROOT, "%.1f fps", snapshot.frameRate) else "—"}", color = PlayerHudGreen, fontSize = 12.sp)
            Text("BITRATE  ${if (snapshot.bitrate > 0) String.format(Locale.ROOT, "%.2f Mbps", bitrateMbps) else "—"}", color = PlayerHudGreen, fontSize = 12.sp)
            Text("NETWORK  ${if (snapshot.connectionSpeedBps > 0L) String.format(Locale.ROOT, "%.2f Mbps", networkMbps) else "—"}  ${if (snapshot.networkActive) "ACTIVE" else "IDLE"}", color = PlayerHudGreen, fontSize = 12.sp)
            Text("DROPPED FRAMES  ${snapshot.droppedFrames}", color = PlayerHudGreen, fontSize = 12.sp)
            Text("POSITION  ${player.video.position.toTimeLabel()} / ${player.video.duration.toTimeLabel()}", color = PlayerHudGreen, fontSize = 12.sp)
        }
    }
}

private fun Double.toTimeLabel(): String {
    if (!isFinite() || this < 0.0) return "—"
    val seconds = toLong()
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
