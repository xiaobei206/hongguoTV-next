package com.xiaobei206.hongguotvnext

import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

private val PlayerHudAccent = Color(0xFFF0B65A)
private val PlayerHudFocusBorder = Color(0xFFFFF3DD)
private val PlayerHudPanel = Color(0xE60D131A)
private val PlayerHudControl = Color(0xFF202B36)
private val PlayerHudControlBorder = Color(0xFF485563)
private val PlayerHudText = Color(0xFFF4F1E9)
private val PlayerHudMuted = Color(0xFFA7B0BA)

/**
 * TV-first player controls:
 * - hidden HUD: left/right seek, up/down change episode, OK reveals the HUD
 * - HUD visible without button focus: left/right still seek; OK pauses and focuses Play/Pause
 * - button focus: OK activates buttons, arrows navigate controls
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable
internal fun TvPlayerScreen(model: AppModel, exo: ExoPlayer, onBack: () -> Unit, calibration: DisplayCalibration) {
    val player = model.player
    val context = LocalContext.current
    val rootFocus = remember { FocusRequester() }
    val pauseFocus = remember { FocusRequester() }

    var menuVisible by remember { mutableStateOf(true) }
    var controlsFocused by remember { mutableStateOf(true) }
    var episodeKeyHeld by remember { mutableStateOf(false) }
    var hideDelayMs by remember { mutableLongStateOf(12_000L) }
    var timerKey by remember { mutableLongStateOf(0L) }
    var backArmedUntil by remember { mutableLongStateOf(0L) }
    var hint by remember { mutableStateOf<String?>(null) }
    var hintKey by remember { mutableLongStateOf(0L) }
    var diagnosticsVisible by remember { mutableStateOf(false) }
    var diagnosticSnapshot by remember { mutableStateOf(PlaybackDiagnostics.snapshot()) }
    var positionTick by remember { mutableLongStateOf(0L) }
    val positionTickForUi = positionTick

    fun showMenu(timeoutMs: Long, focusControls: Boolean = true) {
        menuVisible = true
        hideDelayMs = timeoutMs
        controlsFocused = focusControls
        timerKey++
    }

    fun renewControlsTimeout() {
        menuVisible = true
        controlsFocused = true
        hideDelayMs = 12_000L
        timerKey++
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

    // Focus the first menu action only after the menu has entered composition.
    LaunchedEffect(menuVisible, controlsFocused) {
        if (!menuVisible) return@LaunchedEffect
        withFrameMillis { }
        if (controlsFocused) {
            runCatching { pauseFocus.requestFocus() }
        } else {
            runCatching { rootFocus.requestFocus() }
        }
    }

    // A manual or automatic episode change opens the menu on a predictable control.
    LaunchedEffect(player.number) {
        showMenu(12_000L, focusControls = true)
    }

    LaunchedEffect(menuVisible, timerKey) {
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

    LaunchedEffect(diagnosticsVisible, menuVisible, player.number) {
        while (diagnosticsVisible || menuVisible) {
            if (diagnosticsVisible) diagnosticSnapshot = PlaybackDiagnostics.snapshot()
            positionTick++
            delay(250L)
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .focusRequester(rootFocus)
            .onFocusChanged { if (it.isFocused && menuVisible) controlsFocused = false }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.DirectionUp || event.key == Key.DirectionDown)) {
                    episodeKeyHeld = false
                    return@onPreviewKeyEvent true
                }
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
                        if (episodeKeyHeld) true
                        else {
                            episodeKeyHeld = true
                            if (menuVisible && !controlsFocused) {
                                showMenu(12_000L, focusControls = true)
                                true
                            } else if (menuVisible && controlsFocused) {
                                renewControlsTimeout()
                                true
                            } else if (player.hasPrevious) {
                                player.step(-1)
                                true
                            } else {
                                showSeekHint("已经是第一集")
                                true
                            }
                        }
                    }
                    Key.DirectionDown -> {
                        if (episodeKeyHeld) true
                        else {
                            episodeKeyHeld = true
                            if (menuVisible && !controlsFocused) {
                                showMenu(12_000L, focusControls = true)
                                true
                            } else if (menuVisible && controlsFocused) {
                                renewControlsTimeout()
                                true
                            } else if (player.hasNext) {
                                player.step(1)
                                true
                            } else {
                                showSeekHint("已经是最后一集")
                                true
                            }
                        }
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        if (!menuVisible || !controlsFocused) {
                            showMenu(12_000L, focusControls = true)
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
                (LayoutInflater.from(viewContext)
                    .inflate(R.layout.tv_player_view_texture, null, false) as PlayerView).apply {
                    useController = false
                    this.player = exo
                    setKeepContentOnPlayerReset(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
            },
            update = { view ->
                if (view.player !== exo) view.player = exo
            },
            // Transform the complete full-screen viewport. At 100% this preserves PlayerView's
            // normal aspect-fit behavior; individual edge expansion may intentionally stretch it.
            modifier = Modifier.fillMaxSize().graphicsLayer {
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                if (size.width <= 0f || size.height <= 0f) {
                    scaleX = 1f
                    scaleY = 1f
                    translationX = 0f
                    translationY = 0f
                } else {
                    val bounds = calibration.bounds(CalibrationTarget.VIDEO, size.width, size.height)
                    scaleX = (bounds.right - bounds.left) / size.width
                    scaleY = (bounds.bottom - bounds.top) / size.height
                    translationX = bounds.left
                    translationY = bounds.top
                }
            }
        )

        // Keep the video surface full-screen. Only the playback controls,
        // diagnostic panel, and status overlays use the interface calibration.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                if (size.width <= 0f || size.height <= 0f) {
                    scaleX = 1f
                    scaleY = 1f
                    translationX = 0f
                    translationY = 0f
                } else {
                    val bounds = calibration.bounds(CalibrationTarget.INTERFACE, size.width, size.height)
                    scaleX = (bounds.right - bounds.left) / size.width
                    scaleY = (bounds.bottom - bounds.top) / size.height
                    translationX = bounds.left
                    translationY = bounds.top
                }
            }
        ) {
        // This is an informational overlay, intentionally independent of the
        // controls HUD: it stays visible until toggled off, like Stats for nerds.
        if (diagnosticsVisible) {
            StatsForNerds(
                modifier = Modifier.align(Alignment.TopStart).padding(24.dp).width(390.dp),
                snapshot = diagnosticSnapshot,
                player = player,
                exo = exo
            )
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
                        Button(onClick = { player.retry(); renewControlsTimeout() }) { Text("重新尝试") }
                        if (failure.diagnostic?.canRefreshDevice == true) {
                            TextButton(onClick = { player.retryWithFreshDevice(); renewControlsTimeout() }) {
                                Text("刷新匿名设备并重试", color = PlayerHudAccent)
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
                                "第 ${player.number} 集  ·  ${positionTickForUi.let { player.video.position.toTimeLabel() }} / ${player.video.duration.toTimeLabel()}  ·  ${if (player.video.status == TimeControl.PLAYING) "播放中" else if (player.video.status == TimeControl.WAITING) "缓冲中" else "已暂停"}",
                                color = PlayerHudMuted,
                                fontSize = 13.sp
                            )
                        }
                        TextButton(onClick = {
                            diagnosticsVisible = !diagnosticsVisible
                            if (diagnosticsVisible) diagnosticSnapshot = PlaybackDiagnostics.snapshot()
                            renewControlsTimeout()
                        }) {
                            Text(if (diagnosticsVisible) "关闭诊断" else "播放诊断", color = PlayerHudAccent)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerControlButton(
                            label = if (player.video.wantsToPlay && player.video.status != TimeControl.PAUSED) "暂停" else "播放",
                            modifier = Modifier.focusRequester(pauseFocus),
                            primary = true,
                            onFocused = { renewControlsTimeout() },
                            onClick = { player.togglePause(); renewControlsTimeout() }
                        )
                        PlayerControlButton(
                            label = "后退 10 秒",
                            onFocused = { renewControlsTimeout() },
                            onClick = { player.skip(-10.0); showSeekHint("−10s"); renewControlsTimeout() }
                        )
                        PlayerControlButton(
                            label = "快进 10 秒",
                            onFocused = { renewControlsTimeout() },
                            onClick = { player.skip(10.0); showSeekHint("+10s"); renewControlsTimeout() }
                        )
                        PlayerControlButton(
                            label = "上一集",
                            onFocused = { renewControlsTimeout() },
                            onClick = { player.step(-1); renewControlsTimeout() }
                        )
                        PlayerControlButton(
                            label = "下一集",
                            onFocused = { renewControlsTimeout() },
                            onClick = { player.step(1); renewControlsTimeout() }
                        )
                        PlayerControlButton(
                            label = if (player.speed == 1f) "倍速" else "${player.speed}×",
                            onFocused = { renewControlsTimeout() },
                            onClick = {
                                val speeds = Player.speeds
                                val index = speeds.indexOf(player.speed).coerceAtLeast(-1)
                                player.chooseSpeed(speeds[(index + 1) % speeds.size])
                                renewControlsTimeout()
                            }
                        )
                        if (player.qualities.isNotEmpty()) {
                            PlayerControlButton(
                                label = if (player.quality > 0) "${player.quality}P" else "清晰度",
                                onFocused = { renewControlsTimeout() },
                                onClick = {
                                    val qualities = player.qualities
                                    val current = qualities.indexOf(player.quality).coerceAtLeast(-1)
                                    player.chooseQuality(qualities[(current + 1) % qualities.size])
                                    renewControlsTimeout()
                                }
                            )
                        }
                        PlayerControlButton(
                            label = "退出播放",
                            onFocused = { renewControlsTimeout() },
                            onClick = onBack
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun PlayerControlButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .widthIn(min = 124.dp)
            .height(58.dp)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .graphicsLayer {
                val emphasis = if (focused) 1.075f else 1f
                scaleX = emphasis
                scaleY = emphasis
            }
            .clickable(onClick = onClick)
            .focusable(),
        color = when {
            focused -> PlayerHudAccent
            primary -> Color(0xFF3C3024)
            else -> PlayerHudControl
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (focused) 4.dp else 1.dp,
            color = if (focused) PlayerHudFocusBorder else PlayerHudControlBorder
        ),
        shadowElevation = if (focused) 12.dp else 0.dp
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = when {
                    focused -> Color(0xFF171A1E)
                    primary -> Color(0xFFF2CE9B)
                    else -> PlayerHudText
                },
                fontSize = 17.sp,
                fontWeight = if (focused || primary) FontWeight.ExtraBold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class PlaybackStatsSample(
    val speedBps: Long = 0L,
    val networkBytes: Long = 0L,
    val bufferSeconds: Float = 0f,
)

private const val STATS_SAMPLE_INTERVAL_MS = 500L
private const val STATS_SAMPLE_COUNT = 40

/** Live Stats for nerds panel, matching the three graph rows used by 开心果. */
@Composable
private fun StatsForNerds(
    modifier: Modifier = Modifier,
    snapshot: PlaybackDiagnostics.Snapshot,
    player: Player,
    exo: ExoPlayer
) {
    var currentSnapshot by remember(player.number) { mutableStateOf(snapshot) }
    var bufferSeconds by remember(player.number) { mutableFloatStateOf(0f) }
    val graphHistory = remember(player.number) {
        mutableStateOf(List(STATS_SAMPLE_COUNT) { PlaybackStatsSample() })
    }
    val sweepPosition = remember(player.number) { mutableFloatStateOf(0f) }

    LaunchedEffect(player.number) {
        var sampleIndex = 0
        var smoothedGraphSpeedBps = 0.0
        while (true) {
            val nextSnapshot = PlaybackDiagnostics.snapshot()
            currentSnapshot = nextSnapshot
            val current = exo.currentPosition
            val buffered = exo.bufferedPosition
            bufferSeconds = if (buffered >= current && buffered >= 0L) {
                ((buffered - current) / 1000f).coerceAtLeast(0f)
            } else {
                0f
            }

            // Connection Speed is a rolling estimate; CDN requests arrive in bursts.
            // Smooth the trace and fade to baseline when the estimate expires.
            val reportedSpeed = nextSnapshot.connectionSpeedBps.toDouble()
            smoothedGraphSpeedBps = when {
                reportedSpeed > 0.0 -> if (smoothedGraphSpeedBps <= 0.0) {
                    reportedSpeed
                } else {
                    smoothedGraphSpeedBps * 0.62 + reportedSpeed * 0.38
                }
                else -> smoothedGraphSpeedBps * 0.78
            }
            if (smoothedGraphSpeedBps < 25_000.0) smoothedGraphSpeedBps = 0.0
            val sample = PlaybackStatsSample(
                speedBps = smoothedGraphSpeedBps.toLong(),
                networkBytes = nextSnapshot.networkSamples.takeLast(2).sum(),
                bufferSeconds = bufferSeconds,
            )
            graphHistory.value = graphHistory.value.toMutableList().also { it[sampleIndex] = sample }

            val frameStart = withFrameMillis { it }
            var elapsed = 0L
            while (elapsed < STATS_SAMPLE_INTERVAL_MS) {
                val frame = withFrameMillis { it }
                elapsed = (frame - frameStart).coerceAtLeast(0L)
                sweepPosition.floatValue = sampleIndex +
                    (elapsed.toFloat() / STATS_SAMPLE_INTERVAL_MS).coerceIn(0f, 1f)
            }
            sampleIndex = (sampleIndex + 1) % STATS_SAMPLE_COUNT
            if (sampleIndex == 0) sweepPosition.floatValue = 0f
        }
    }

    val width = currentSnapshot.width.takeIf { it > 0 } ?: player.sourceWidth
    val height = currentSnapshot.height.takeIf { it > 0 } ?: player.sourceHeight
    val currentRes = if (width > 0 && height > 0) "$width×$height" else if (player.quality > 0) "${player.quality}P" else "—"
    val optimal = player.qualities.maxOrNull()?.let { "${it}P" } ?: "—"
    val fpsNumber = if (currentSnapshot.frameRate > 0f) String.format(Locale.ROOT, "%.0f", currentSnapshot.frameRate) else ""
    val fps = if (fpsNumber.isNotEmpty()) "  ${fpsNumber}fps" else ""
    val videoCodec = currentSnapshot.videoCodec.ifBlank { player.selectedCodec.ifBlank { "—" } }
    val audioCodec = currentSnapshot.audioCodec
    val codecs = if (audioCodec.isNotBlank()) "$videoCodec · $audioCodec" else videoCodec
    val decoderKind = when (currentSnapshot.decoderHardware) {
        true -> "硬件解码"
        false -> "软件解码"
        null -> "等待解码器"
    }
    val decoder = currentSnapshot.decoderName.takeIf { it.isNotBlank() }?.let { "${decoderKind} · $it" } ?: decoderKind
    val speed = if (currentSnapshot.connectionSpeedBps > 0) {
        String.format(Locale.ROOT, "%.1f Mbps", currentSnapshot.connectionSpeedBps / 1_000_000.0)
    } else {
        "等待数据"
    }
    val netKb = currentSnapshot.networkBytesLast20Seconds / 1024.0
    val net = if (netKb >= 1024.0) {
        String.format(Locale.ROOT, "%.1f MB/20s", netKb / 1024.0)
    } else {
        String.format(Locale.ROOT, "%.0f KB/20s", netKb)
    }
    val buffer = String.format(Locale.ROOT, "%.1f s", bufferSeconds)

    Column(
        modifier.widthIn(max = 370.dp).fillMaxWidth(0.96f)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xE8101010))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("Stats for nerds", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        StatsTextRow("Current / Optimal Res", "$currentRes$fps / $optimal")
        StatsTextRow("Codecs", codecs)
        StatsTextRow("Decoder", decoder)
        StatsGraphRow("Connection Speed", speed, graphHistory.value, StatsGraphKind.SPEED, sweepPosition.floatValue)
        StatsGraphRow("Network Activity", net, graphHistory.value, StatsGraphKind.NETWORK, sweepPosition.floatValue)
        StatsGraphRow("Buffer Health", buffer, graphHistory.value, StatsGraphKind.BUFFER, sweepPosition.floatValue)
        StatsTextRow("Dropped Frames", currentSnapshot.droppedFrames.toString())
    }
}

@Composable
private fun StatsTextRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.83f), fontSize = 9.sp, modifier = Modifier.width(112.dp), maxLines = 1)
        Text(value, color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

private enum class StatsGraphKind { SPEED, NETWORK, BUFFER }

/** These thresholds apply to the aggregated 500 ms display slot. */
private fun statsNetworkActivityColor(bytes: Long): Color = when {
    bytes <= 0L -> Color(0xFF262626)
    bytes < 20L * 1024L -> Color(0xFFF44336)
    bytes < 40L * 1024L -> Color(0xFFFF7043)
    bytes < 80L * 1024L -> Color(0xFFFFEE58)
    bytes < 160L * 1024L -> Color(0xFFD4E157)
    else -> Color(0xFF9CCC65)
}

private fun statsBufferHealthColor(seconds: Float): Color = when {
    seconds <= 0f -> Color(0xFF262626)
    seconds < 15f -> Color(0xFFFFEE58)
    seconds < 30f -> Color(0xFFD4E157)
    seconds < 60f -> Color(0xFF26A69A)
    seconds < 90f -> Color(0xFF2196F3)
    else -> Color.White
}

/**
 * The three diagnostics use distinct visual encodings: a continuous throughput
 * trace, spaced network activity peaks, and a filled buffer-health curve.
 * History has fixed columns; the black sweep cursor only overlays the live edge.
 */
@Composable
private fun StatsGraphRow(
    label: String,
    value: String,
    samples: List<PlaybackStatsSample>,
    kind: StatsGraphKind,
    sweepPosition: Float,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.83f), fontSize = 9.sp, modifier = Modifier.width(112.dp), maxLines = 1)
        Canvas(
            Modifier.weight(1f).height(14.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
        ) {
            drawRect(Color(0xFF181818))
            drawLine(
                Color(0xFF343434),
                Offset(0f, size.height - 0.5.dp.toPx()),
                Offset(size.width, size.height - 0.5.dp.toPx()),
                strokeWidth = 0.7.dp.toPx(),
            )

            val count = samples.size.coerceAtLeast(1)
            val slot = size.width / count
            val gap = 0.8.dp.toPx()

            when (kind) {
                StatsGraphKind.SPEED -> {
                    // A single blue hue; brightness (not hue or bars) carries the
                    // speed history. Keep a dim continuous baseline while idle.
                    val maxSpeedBps = 80_000_000.0
                    val floorSpeedBps = 100_000.0
                    val maxLog = kotlin.math.ln(1.0 + maxSpeedBps / floorSpeedBps)
                    val ratios = samples.map { sample ->
                        if (sample.speedBps <= 0L) 0f else {
                            (kotlin.math.ln(1.0 + sample.speedBps.toDouble() / floorSpeedBps) / maxLog)
                                .toFloat().coerceIn(0f, 1f)
                        }
                    }
                    val baselineY = size.height - 0.65.dp.toPx()
                    val curveHeight = (size.height - 1.4.dp.toPx()).coerceAtLeast(1f)
                    val points = ratios.mapIndexed { index, ratio ->
                        Offset((index.toFloat() + 0.5f) * slot, baselineY - ratio * curveHeight)
                    }
                    val speedBlue = Color(0xFF42A5F5)
                    drawLine(
                        speedBlue.copy(alpha = 0.18f),
                        Offset(0f, baselineY),
                        Offset(size.width, baselineY),
                        strokeWidth = 0.8.dp.toPx(),
                    )

                    // Catmull-Rom-style cubic segments round off abrupt sample changes.
                    // The entire time series is drawn, including zero/idle samples.
                    for (index in 0 until points.lastIndex) {
                        val p0 = points[(index - 1).coerceAtLeast(0)]
                        val p1 = points[index]
                        val p2 = points[index + 1]
                        val p3 = points[(index + 2).coerceAtMost(points.lastIndex)]
                        val dx = p2.x - p1.x
                        val control1Y = (p1.y + (p2.y - p0.y) / 6f).coerceIn(0f, size.height)
                        val control2Y = (p2.y - (p3.y - p1.y) / 6f).coerceIn(0f, size.height)
                        val curve = androidx.compose.ui.graphics.Path().apply {
                            moveTo(p1.x, p1.y)
                            cubicTo(
                                p1.x + dx / 3f, control1Y,
                                p2.x - dx / 3f, control2Y,
                                p2.x, p2.y,
                            )
                        }
                        val intensity = ((ratios[index] + ratios[index + 1]) / 2f).coerceIn(0f, 1f)
                        drawPath(
                            curve,
                            speedBlue.copy(alpha = 0.08f + intensity * 0.12f),
                            style = Stroke(
                                width = (2.6f + intensity * 1.0f).dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                        drawPath(
                            curve,
                            speedBlue.copy(alpha = 0.32f + intensity * 0.68f),
                            style = Stroke(
                                width = (1.15f + intensity * 0.45f).dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }

                StatsGraphKind.NETWORK -> {
                    // A fixed 2 MiB / 500 ms reference scale prevents typical
                    // CDN bursts from saturating every column. Square-root scaling
                    // makes smaller real transfers visible without capping them.
                    val maxBytesPerSlot = 2L * 1024L * 1024L
                    val barWidth = (slot * 0.62f).coerceAtLeast(1.dp.toPx()).coerceAtMost(slot)
                    samples.forEachIndexed { index, sample ->
                        val bytes = sample.networkBytes
                        if (bytes > 0L) {
                            val normalized = (bytes.toDouble() / maxBytesPerSlot).coerceIn(0.0, 1.0)
                            val ratio = kotlin.math.sqrt(normalized).toFloat()
                            val barHeight = (size.height * ratio).coerceAtLeast(0.65.dp.toPx()).coerceAtMost(size.height)
                            val x = index * slot + (slot - barWidth) / 2f
                            drawRect(
                                statsNetworkActivityColor(bytes),
                                Offset(x, size.height - barHeight),
                                Size((barWidth - gap * 0.15f).coerceAtLeast(0.8.dp.toPx()), barHeight),
                            )
                        }
                    }
                }

                StatsGraphKind.BUFFER -> {
                    val maxSeconds = 90f
                    for (index in 1 until samples.size) {
                        val previous = samples[index - 1].bufferSeconds.coerceIn(0f, maxSeconds)
                        val current = samples[index].bufferSeconds.coerceIn(0f, maxSeconds)
                        if (previous <= 0f && current <= 0f) continue

                        val x1 = (index - 1 + 0.5f) * slot
                        val x2 = (index + 0.5f) * slot
                        val y1 = size.height - previous / maxSeconds * size.height
                        val y2 = size.height - current / maxSeconds * size.height
                        val color = statsBufferHealthColor(maxOf(previous, current))
                        val area = androidx.compose.ui.graphics.Path().apply {
                            moveTo(x1, size.height)
                            lineTo(x1, y1)
                            lineTo(x2, y2)
                            lineTo(x2, size.height)
                            close()
                        }
                        drawPath(area, color.copy(alpha = 0.16f))
                        drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = 1.7.dp.toPx())
                    }
                }
            }

            val cursor = (sweepPosition % count.toFloat()).coerceIn(0f, count.toFloat())
            val cursorWidth = 1.3.dp.toPx()
            val cursorX = (cursor / count) * size.width
            val safeCursorX = cursorX.coerceIn(0f, (size.width - cursorWidth).coerceAtLeast(0f))
            drawRect(Color(0xFF080808), Offset(safeCursorX, 0f), Size(cursorWidth, size.height))
            drawLine(
                Color.White.copy(alpha = 0.32f),
                Offset((safeCursorX + cursorWidth).coerceAtMost(size.width), 0f),
                Offset((safeCursorX + cursorWidth).coerceAtMost(size.width), size.height),
                strokeWidth = 0.6.dp.toPx(),
            )
        }
        Text(value, color = Color.White, fontSize = 9.sp, modifier = Modifier.width(62.dp).padding(start = 4.dp), textAlign = TextAlign.End, maxLines = 1)
    }
}


private fun Double.toTimeLabel(): String {
    if (!isFinite() || this < 0.0) return "—"
    val seconds = toLong()
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
