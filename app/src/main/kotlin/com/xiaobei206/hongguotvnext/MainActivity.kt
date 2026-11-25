package com.xiaobei206.hongguotvnext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.xiaobei206.hongguotvnext.model.AppModel
import com.xiaobei206.hongguotvnext.model.Card
import com.xiaobei206.hongguotvnext.model.Fav
import com.xiaobei206.hongguotvnext.model.Feed
import com.xiaobei206.hongguotvnext.model.Player
import com.xiaobei206.hongguotvnext.model.PlayerState
import com.xiaobei206.hongguotvnext.model.Watch
import com.xiaobei206.hongguotvnext.model.dayLabel
import com.xiaobei206.hongguotvnext.model.todayKey
import com.xiaobei206.hongguotvnext.model.seasonLabel
import com.xiaobei206.hongguotvnext.playback.PlaybackDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

private val TvBackground = Color(0xFF0D131A)
private val TvPanel = Color(0xFF18212B)
private val TvPanelRaised = Color(0xFF24313D)
private val TvAccent = Color(0xFFC89452)
private val TvFocus = Color(0xFFF0B65A)
private val TvFocusBorder = Color(0xFFFFF3DD)
private val TvText = Color(0xFFF4F1E9)
private val TvMuted = Color(0xFFA7B0BA)

private enum class TvTab(val title: String) {
    RECOMMEND("推荐"),
    RANK("排行榜"),
    NEW("新剧上线"),
    SEARCH("搜索"),
    FAVORITES("收藏"),
    HISTORY("历史记录"),
    DEVICE("设备接力"),
    DIAGNOSTICS("诊断"),
    SETTINGS("设置"),
}

private enum class TvScreen { BROWSE, DETAIL, PLAYER, DEVICE, DIAGNOSTICS, SETTINGS, CALIBRATE_INTERFACE, CALIBRATE_VIDEO }

@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class MainActivity : ComponentActivity() {
    private val tvApp get() = application as TvApplication

    private var screen by mutableStateOf(TvScreen.BROWSE)
    private var tab by mutableStateOf(TvTab.RECOMMEND)
    private var detailId by mutableStateOf("")
    private var playerReturnScreen by mutableStateOf(TvScreen.BROWSE)
    private var handoffEnabled by mutableStateOf(true)
    private var displayCalibration by mutableStateOf(DisplayCalibration())
    private val calibrationPreferences get() = getSharedPreferences("display_calibration", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        handoffEnabled = tvApp.handoffServer?.enabled ?: true
        displayCalibration = DisplayCalibration(
            interfaceScalePercent = calibrationPreferences.getInt("interface_scale_percent", 100),
            interfaceLeftExpandSteps = calibrationPreferences.getInt("interface_left_expand_steps", 0),
            interfaceRightExpandSteps = calibrationPreferences.getInt("interface_right_expand_steps", 0),
            interfaceTopExpandSteps = calibrationPreferences.getInt("interface_top_expand_steps", 0),
            interfaceBottomExpandSteps = calibrationPreferences.getInt("interface_bottom_expand_steps", 0),
            videoScalePercent = calibrationPreferences.getInt("video_scale_percent", 100),
            videoLeftExpandSteps = calibrationPreferences.getInt("video_left_expand_steps", 0),
            videoRightExpandSteps = calibrationPreferences.getInt("video_right_expand_steps", 0),
            videoTopExpandSteps = calibrationPreferences.getInt("video_top_expand_steps", 0),
            videoBottomExpandSteps = calibrationPreferences.getInt("video_bottom_expand_steps", 0),
        ).normalized()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = TvAccent,
                secondary = TvAccent,
                background = TvBackground,
                surface = TvPanel,
                onPrimary = TvBackground,
                onBackground = TvText,
                onSurface = TvText
            )) {
                val stateHolder = rememberSaveableStateHolder()
                val model = tvApp.model
                BackHandler(enabled = screen != TvScreen.BROWSE || tab != TvTab.RECOMMEND) {
                    goBack(model)
                }
                Box(Modifier.fillMaxSize().background(Color.Black).clipToBounds()) {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            // Settings and calibration test screens remain fixed and recoverable.
                            // On app screens, each edge expands independently; this is not a translation.
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                            val fixedScreen = screen == TvScreen.PLAYER ||
                                screen == TvScreen.SETTINGS ||
                                screen == TvScreen.CALIBRATE_INTERFACE ||
                                screen == TvScreen.CALIBRATE_VIDEO
                            if (fixedScreen || size.width <= 0f || size.height <= 0f) {
                                scaleX = 1f
                                scaleY = 1f
                                translationX = 0f
                                translationY = 0f
                            } else {
                                val bounds = displayCalibration.bounds(
                                    CalibrationTarget.INTERFACE, size.width, size.height
                                )
                                scaleX = (bounds.right - bounds.left) / size.width
                                scaleY = (bounds.bottom - bounds.top) / size.height
                                translationX = bounds.left
                                translationY = bounds.top
                            }
                        }
                    ) {
                when (screen) {
                    TvScreen.BROWSE -> stateHolder.SaveableStateProvider("tv-browse") {
                        BrowseScreen(
                            model = model,
                            covers = tvApp.covers,
                            selectedTab = tab,
                            onTab = ::selectTab,
                            onOpenSeries = { id ->
                                detailId = id
                                screen = TvScreen.DETAIL
                            },
                            onPlay = { id, episode, position ->
                                model?.cancelBackgroundFeeds()
                                tvApp.covers.cancelPendingLoads()
                                model?.player?.play(id, episode, position)
                                playerReturnScreen = TvScreen.BROWSE
                                screen = TvScreen.PLAYER
                            },
                            onDevice = { screen = TvScreen.DEVICE },
                            onDiagnostics = { screen = TvScreen.DIAGNOSTICS },
                            onSettings = { screen = TvScreen.SETTINGS },
                            startupError = tvApp.startupError
                        )
                    }
                    TvScreen.DETAIL -> if (model != null) {
                        DetailScreen(
                            model = model,
                            seriesId = detailId,
                            covers = tvApp.covers,
                            onBack = { screen = TvScreen.BROWSE },
                            onSelectSeason = { seasonId -> detailId = seasonId },
                            onPlay = { id, episode, position ->
                                model.cancelBackgroundFeeds()
                                tvApp.covers.cancelPendingLoads()
                                model.player.play(id, episode, position)
                                playerReturnScreen = TvScreen.DETAIL
                                screen = TvScreen.PLAYER
                            }
                        )
                    } else StartupProblem(tvApp.startupError)
                    TvScreen.PLAYER -> if (model != null) {
                        TvPlayerScreen(
                            model = model,
                            exo = tvApp.video.exo,
                            onBack = { goBack(model) },
                            calibration = displayCalibration
                        )
                    } else StartupProblem(tvApp.startupError)
                    TvScreen.DEVICE -> DeviceScreen(
                        server = tvApp.handoffServer,
                        enabled = handoffEnabled,
                        onEnabledChange = ::updateHandoffEnabled,
                        onBack = { screen = TvScreen.BROWSE }
                    )
                    TvScreen.DIAGNOSTICS -> DiagnosticsScreen(
                        model = model,
                        covers = tvApp.covers,
                        onBack = { screen = TvScreen.BROWSE }
                    )
                    TvScreen.SETTINGS -> SettingsScreen(
                        handoffEnabled = handoffEnabled,
                        onHandoffEnabledChange = ::updateHandoffEnabled,
                        calibration = displayCalibration,
                        onCalibrationChange = ::updateDisplayCalibration,
                        onOpenInterfaceCalibration = { screen = TvScreen.CALIBRATE_INTERFACE },
                        onOpenVideoCalibration = { screen = TvScreen.CALIBRATE_VIDEO },
                        onBack = { screen = TvScreen.BROWSE }
                    )
                    TvScreen.CALIBRATE_INTERFACE -> DisplayCalibrationScreen(
                        target = CalibrationTarget.INTERFACE,
                        calibration = displayCalibration,
                        onCalibrationChange = ::updateDisplayCalibration,
                        onBack = { screen = TvScreen.SETTINGS }
                    )
                    TvScreen.CALIBRATE_VIDEO -> DisplayCalibrationScreen(
                        target = CalibrationTarget.VIDEO,
                        calibration = displayCalibration,
                        onCalibrationChange = ::updateDisplayCalibration,
                        onBack = { screen = TvScreen.SETTINGS }
                    )
                }
                    }
                }
            }
        }
    }

    private fun selectTab(next: TvTab) {
        when (next) {
            TvTab.DEVICE -> screen = TvScreen.DEVICE
            TvTab.DIAGNOSTICS -> screen = TvScreen.DIAGNOSTICS
            TvTab.SETTINGS -> screen = TvScreen.SETTINGS
            else -> {
                tab = next
                screen = TvScreen.BROWSE
            }
        }
    }

    private fun goBack(model: AppModel?) {
        when (screen) {
            TvScreen.PLAYER -> {
                model?.player?.leave()
                screen = playerReturnScreen
            }
            TvScreen.CALIBRATE_INTERFACE, TvScreen.CALIBRATE_VIDEO ->
                screen = TvScreen.SETTINGS
            TvScreen.DETAIL, TvScreen.DEVICE, TvScreen.DIAGNOSTICS, TvScreen.SETTINGS ->
                screen = TvScreen.BROWSE
            TvScreen.BROWSE -> if (tab != TvTab.RECOMMEND) tab = TvTab.RECOMMEND
        }
    }

    override fun onStart() {
        super.onStart()
        tvApp.handoffServer?.let { server ->
            server.listener = if (server.enabled) ::handleHandoffRequest else null
            server.setForeground(true)
        }
        tvApp.model?.player?.returnToApp()
    }

    override fun onStop() {
        tvApp.handoffServer?.let { server ->
            server.listener = null
            server.setForeground(false)
        }
        tvApp.model?.let { model ->
            model.player.leaveApp()
            model.saveSession(model.session())
            model.flush()
        }
        super.onStop()
    }

    private fun updateHandoffEnabled(value: Boolean) {
        handoffEnabled = value
        val server = tvApp.handoffServer ?: return
        server.setEnabled(value)
        server.listener = if (value && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            ::handleHandoffRequest
        } else {
            null
        }
    }

    private fun updateDisplayCalibration(next: DisplayCalibration) {
        val safe = next.normalized()
        displayCalibration = safe
        calibrationPreferences.edit()
            .putInt("interface_scale_percent", safe.interfaceScalePercent)
            .putInt("interface_left_expand_steps", safe.interfaceLeftExpandSteps)
            .putInt("interface_right_expand_steps", safe.interfaceRightExpandSteps)
            .putInt("interface_top_expand_steps", safe.interfaceTopExpandSteps)
            .putInt("interface_bottom_expand_steps", safe.interfaceBottomExpandSteps)
            .putInt("video_scale_percent", safe.videoScalePercent)
            .putInt("video_left_expand_steps", safe.videoLeftExpandSteps)
            .putInt("video_right_expand_steps", safe.videoRightExpandSteps)
            .putInt("video_top_expand_steps", safe.videoTopExpandSteps)
            .putInt("video_bottom_expand_steps", safe.videoBottomExpandSteps)
            .apply()
    }

    private fun handleHandoffRequest(
        request: TvHandoffRequest,
        reply: (TvHandoffReply) -> Unit
    ) {
        runOnUiThread {
            val model = tvApp.model
            if (model == null) {
                reply(TvHandoffReply(
                    request.transferId, false, "电视应用模型尚未初始化",
                    tvApp.handoffServer?.deviceId.orEmpty()
                ))
                return@runOnUiThread
            }

            detailId = request.seriesId
            playerReturnScreen = TvScreen.BROWSE
            model.cancelBackgroundFeeds()
            tvApp.covers.cancelPendingLoads()
            model.player.play(request.seriesId, request.episode, request.positionMs / 1000.0)
            screen = TvScreen.PLAYER

            lifecycleScope.launch {
                val state = withTimeoutOrNull(34_000L) {
                    snapshotFlow { model.player.state }.first {
                        it == PlayerState.Playing || it is PlayerState.Failed
                    }
                }
                val current = model.player.seriesID == request.seriesId &&
                    model.player.number == request.episode
                val server = tvApp.handoffServer
                val result = when {
                    !current -> TvHandoffReply(
                        request.transferId, false, "电视播放任务已被其他操作替代", server?.deviceId.orEmpty()
                    )
                    state == PlayerState.Playing -> TvHandoffReply(
                        request.transferId, true, "电视已开始播放", server?.deviceId.orEmpty()
                    )
                    state is PlayerState.Failed -> {
                        model.player.leave()
                        TvHandoffReply(request.transferId, false, state.message, server?.deviceId.orEmpty())
                    }
                    else -> {
                        model.player.leave()
                        TvHandoffReply(
                            request.transferId, false, "电视加载视频超时",
                            server?.deviceId.orEmpty()
                        )
                    }
                }
                reply(result)
                Toast.makeText(
                    this@MainActivity,
                    if (result.accepted) "已接收手机接力播放" else "接力失败：${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    model: AppModel?,
    covers: com.xiaobei206.hongguotvnext.covers.CoverStore?,
    selectedTab: TvTab,
    onTab: (TvTab) -> Unit,
    onOpenSeries: (String) -> Unit,
    onPlay: (String, Int, Double) -> Unit,
    onDevice: () -> Unit,
    onDiagnostics: () -> Unit,
    onSettings: () -> Unit,
    startupError: String
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf("") }
    val tabs = TvTab.entries
    Row(
        Modifier.fillMaxSize().background(TvBackground).padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            Modifier.width(190.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("红果 TV", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = TvAccent)
            Text("NEXT · v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = TvMuted)
            Spacer(Modifier.height(10.dp))
            tabs.forEach { item ->
                NavigationItem(
                    title = item.title,
                    selected = selectedTab == item,
                    onClick = {
                        when (item) {
                            TvTab.DEVICE -> onDevice()
                            TvTab.DIAGNOSTICS -> onDiagnostics()
                            TvTab.SETTINGS -> onSettings()
                            else -> onTab(item)
                        }
                    }
                )
            }
        }

        if (model == null) {
            StartupProblem(startupError)
        } else {
            val feed = remember(
                selectedTab, submittedQuery, model.rankContent, model.rankBoard, model.newDate
            ) {
                when (selectedTab) {
                    TvTab.RECOMMEND -> model.home
                    TvTab.RANK -> model.rankFeed
                    TvTab.NEW -> model.newFeed
                    TvTab.SEARCH -> submittedQuery.trim().takeIf { it.isNotEmpty() }?.let(model::searchFeed)
                    else -> null
                }
            }
            val localCards = when (selectedTab) {
                TvTab.FAVORITES -> model.favorites.cards
                TvTab.HISTORY -> model.history.list.map(::watchCard)
                else -> emptyList()
            }
            val hotCards = if (selectedTab == TvTab.SEARCH && submittedQuery.isBlank()) {
                model.hot?.boards.orEmpty().flatMap { board ->
                    board.items.map { item ->
                        Card(
                            id = item.seriesId,
                            title = item.title,
                            cover = item.coverUrl,
                            sub = item.searchScore.ifEmpty { item.heat },
                            rank = item.position,
                            episodes = item.episodeCount
                        )
                    }
                }
            } else emptyList()
            val cards = if (selectedTab == TvTab.SEARCH && submittedQuery.isBlank()) hotCards else feed?.cards ?: localCards
            val gridState = rememberLazyGridState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(selectedTab) {
                if (selectedTab == TvTab.SEARCH) model.loadHot()
            }
            LaunchedEffect(feed) {
                if (feed == null) return@LaunchedEffect
                if (feed.wantsMore) feed.loadMore()
                snapshotFlow { feed.cards.size to feed.loading }
                    .distinctUntilChanged()
                    .collect { (size, loading) ->
                        // Show the arriving page immediately; top up to around two screens asynchronously.
                        if (size < 24 && feed.more && !loading) feed.loadMore()
                    }
            }
            LaunchedEffect(feed, gridState) {
                if (feed == null) return@LaunchedEffect
                snapshotFlow {
                    val info = gridState.layoutInfo
                    (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to feed.cards.size
                }.distinctUntilChanged().collect { (lastVisible, size) ->
                    if (size > 0 && lastVisible >= size - 7 && feed.more && !feed.loading) {
                        feed.loadMore()
                    }
                }
            }

            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(selectedTab.title, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when (selectedTab) {
                                TvTab.RECOMMEND -> "缓存优先展示 · 后台更新"
                                TvTab.RANK -> "榜单独立加载与续页"
                                TvTab.NEW -> "新剧按日期筛选"
                                TvTab.SEARCH -> "输入片名后按确认键搜索"
                                TvTab.FAVORITES -> "已收藏的剧集"
                                TvTab.HISTORY -> "最近观看记录"
                                else -> ""
                            },
                            fontSize = 13.sp,
                            color = TvMuted
                        )
                    }
                    Text(
                        when {
                            feed?.loading == true -> "正在加载…"
                            feed?.error != null -> "加载遇到问题"
                            selectedTab == TvTab.SEARCH && submittedQuery.isBlank() && model.hot != null ->
                                "${model.hot?.boards?.sumOf { it.items.size } ?: 0} 条热搜"
                            else -> if (cards.isNotEmpty()) "${cards.size} 部" else ""
                        },
                        color = TvMuted,
                        fontSize = 14.sp
                    )
                }

                if (selectedTab == TvTab.SEARCH) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("搜索影视名称") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            submittedQuery = searchText.trim()
                        })
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { submittedQuery = searchText.trim() }, enabled = searchText.isNotBlank()) {
                            Text("搜索")
                        }
                        TextButton(onClick = { searchText = ""; submittedQuery = "" }) {
                            Text("清除")
                        }
                    }
                    if (submittedQuery.isBlank() && hotCards.isEmpty()) {
                        Text(
                            when {
                                model.hotLoading -> "正在获取热门搜索…"
                                model.hotError != null -> "热门搜索加载失败：${model.hotError?.message ?: "未知错误"}"
                                model.hot != null -> "暂时没有热门搜索内容"
                                else -> "正在获取热门搜索…"
                            },
                            color = TvMuted,
                            fontSize = 17.sp
                        )
                    }
                }

                if (selectedTab == TvTab.RANK) {
                    FilterRow {
                        model.chartContents.forEach { content ->
                            FilterChip(
                                content.name,
                                model.currentRankContent == content.id,
                                { model.rankContent = content.id; model.rankBoard = "" }
                            )
                        }
                    }
                    FilterRow {
                        model.rankBoards.forEach { board ->
                            FilterChip(
                                board.name,
                                model.boardIsOn(board),
                                { model.rankBoard = board.id }
                            )
                        }
                    }
                }
                if (selectedTab == TvTab.NEW && model.newDates.isNotEmpty()) {
                    FilterRow {
                        model.newDates.forEach { date ->
                            FilterChip(
                                dayLabel(date, todayKey()),
                                model.dayIsOn(date),
                                { model.chooseDay(date) }
                            )
                        }
                    }
                }

                if (
                    selectedTab == TvTab.SEARCH &&
                    submittedQuery.isBlank() &&
                    model.hot?.boards?.any { it.items.isNotEmpty() } == true
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 30.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        model.hot!!.boards.filter { it.items.isNotEmpty() }.forEachIndexed { boardIndex, board ->
                            item(
                                key = "hot-board-$boardIndex",
                                span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                            ) {
                                Text(
                                    board.name,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TvText
                                )
                            }
                            items(
                                board.items,
                                key = { hot -> "hot-$boardIndex-${hot.seriesId}-${hot.position}" }
                            ) { hot ->
                                CatalogCardView(
                                    card = Card(
                                        id = hot.seriesId,
                                        title = hot.title,
                                        cover = hot.coverUrl,
                                        sub = hot.searchScore.ifEmpty { hot.heat },
                                        rank = hot.position,
                                        episodes = hot.episodeCount
                                    ),
                                    covers = covers,
                                    onClick = { onOpenSeries(hot.seriesId) }
                                )
                            }
                        }
                    }
                } else if (cards.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            when {
                                selectedTab == TvTab.SEARCH && submittedQuery.isBlank() && model.hotLoading -> "正在获取热门搜索…"
                                selectedTab == TvTab.SEARCH && submittedQuery.isBlank() && model.hotError != null -> "热门搜索加载失败：${model.hotError?.message ?: "未知错误"}"
                                selectedTab == TvTab.SEARCH && submittedQuery.isBlank() -> "暂无热门搜索"
                                feed?.loading == true -> "正在获取第一批内容…"
                                feed?.error != null -> "加载失败：${feed.error?.message ?: "未知错误"}"
                                selectedTab == TvTab.FAVORITES -> "还没有收藏剧集"
                                selectedTab == TvTab.HISTORY -> "还没有观看记录"
                                else -> "暂无内容"
                            },
                            color = TvMuted,
                            fontSize = 21.sp
                        )
                        if (feed?.error != null) {
                            Button(onClick = { feed.retry() }) { Text("重试") }
                        } else if (selectedTab == TvTab.SEARCH && submittedQuery.isBlank() && model.hotError != null) {
                            Button(onClick = { model.loadHot(force = true) }) { Text("重新加载热搜") }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 30.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        if (selectedTab == TvTab.SEARCH && submittedQuery.isBlank()) {
                            model.hot?.boards.orEmpty().filter { it.items.isNotEmpty() }.forEachIndexed { boardIndex, board ->
                                item(key = "hot-board-$boardIndex", span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        board.name,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                                        fontSize = 23.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TvText
                                    )
                                }
                                items(board.items, key = { item -> "hot-$boardIndex-${item.seriesId}-${item.position}" }) { item ->
                                    CatalogCardView(
                                        card = Card(
                                            id = item.seriesId,
                                            title = item.title,
                                            cover = item.coverUrl,
                                            sub = item.searchScore.ifEmpty { item.heat },
                                            rank = item.position,
                                            episodes = item.episodeCount
                                        ),
                                        covers = covers,
                                        onClick = { onOpenSeries(item.seriesId) }
                                    )
                                }
                            }
                        } else {
                            items(cards, key = { it.id }) { card ->
                                CatalogCardView(
                                    card = card,
                                    covers = covers,
                                    onClick = { onOpenSeries(card.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun watchCard(watch: Watch): Card = Card(
    id = watch.seriesID,
    title = watch.title.ifBlank { watch.seriesID },
    cover = watch.cover,
    sub = if (watch.finished) "已看完" else "第 ${watch.episode} 集 · ${formatPosition(watch.position)}",
    episodes = watch.total
)

private fun formatPosition(seconds: Double): String {
    if (!seconds.isFinite() || seconds < 0) return "进度未知"
    val total = seconds.toLong()
    return "${total / 60} 分 ${total % 60} 秒"
}

@Composable
private fun NavigationItem(title: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().height(46.dp)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                val emphasis = if (focused) 1.025f else 1f
                scaleX = emphasis
                scaleY = emphasis
            }
            .clickable(onClick = onClick)
            .focusable(),
        color = when {
            focused -> TvFocus
            selected -> TvAccent
            else -> TvPanel
        },
        shape = RoundedCornerShape(10.dp),
        border = if (focused) BorderStroke(4.dp, Color.White) else null
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                title,
                color = if (selected || focused) Color.Black else TvText,
                fontSize = 17.sp,
                fontWeight = if (selected || focused) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.height(40.dp)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                val emphasis = if (focused) 1.04f else 1f
                scaleX = emphasis
                scaleY = emphasis
            }
            .clickable(onClick = onClick).focusable(),
        shape = RoundedCornerShape(20.dp),
        color = if (focused) TvFocus else if (selected) TvAccent else TvPanel,
        border = if (focused) BorderStroke(4.dp, Color.White) else null
    ) {
        Box(Modifier.padding(horizontal = 15.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected || focused) Color.Black else TvText,
                fontSize = 15.sp,
                fontWeight = if (focused) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CatalogCardView(
    card: Card,
    covers: com.xiaobei206.hongguotvnext.covers.CoverStore?,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val requester = remember(card.id) { FocusRequester() }
    val bitmap by produceState(
        initialValue = if (covers != null) covers.cached(card.cover) else null,
        key1 = covers,
        key2 = card.cover
    ) {
        if (covers != null && card.cover.isNotBlank()) {
            value = runCatching { covers.image(card.cover) }.getOrNull()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
            .focusRequester(requester)
            .clickable(onClick = onClick)
            .focusable(),
        color = if (focused) TvPanelRaised else TvPanel,
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(3.dp, TvFocusBorder) else null
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    .background(Color(0xFF273447), RoundedCornerShape(8.dp))
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = card.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("红果 TV", color = TvMuted, fontSize = 14.sp)
                    }
                }
                if (card.rank > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                        color = TvAccent,
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            "#${card.rank}",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = TvBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                card.title.ifBlank { "未命名剧集" },
                maxLines = 2,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TvText
            )
            if (card.sub.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(card.sub, maxLines = 1, fontSize = 13.sp, color = TvMuted)
            }
        }
    }
    DisposableEffect(card.id) {
        onDispose { /* Poster work is URL-deduplicated and belongs to the cache. */ }
    }
}

@Composable
private fun DetailScreen(
    model: AppModel,
    seriesId: String,
    covers: com.xiaobei206.hongguotvnext.covers.CoverStore,
    onBack: () -> Unit,
    onSelectSeason: (String) -> Unit,
    onPlay: (String, Int, Double) -> Unit
) {
    val page = model.seriesPage(seriesId)
    val series = page.series
    val title = series?.title ?: model.history.get(seriesId)?.title?.ifBlank { null } ?: "影视详情"
    val cover = series?.coverUrl.orEmpty().ifBlank { model.history.get(seriesId)?.cover.orEmpty() }
    val bitmap by produceState(initialValue = covers.cached(cover), key1 = cover) {
        if (cover.isNotBlank()) value = runCatching { covers.image(cover) }.getOrNull()
    }
    var focusedEpisode by rememberSaveable(seriesId) { mutableIntStateOf(-1) }

    Row(
        Modifier.fillMaxSize().background(TvBackground).padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(Modifier.width(320.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(), contentDescription = title,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().weight(1f).background(TvPanel, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("封面加载中", color = TvMuted) }
            }
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("共 ${series?.episodeCount ?: page.episodes.size} 集", color = TvMuted, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val watch = model.history.get(seriesId)
                    val episode = watch?.episode ?: 1
                    val position = if (watch != null && !watch.finished) maxOf(0.0, watch.position - 3.0) else 0.0
                    onPlay(seriesId, episode, position)
                }, enabled = page.episodes.isNotEmpty()) { Text("继续播放") }
                TextButton(onClick = {
                    model.favorites.toggle(Fav(seriesId, title, cover, series?.episodeCount ?: page.episodes.size))
                }) {
                    Text(if (model.favorites.has(seriesId)) "取消收藏" else "收藏")
                }
            }
            TextButton(onClick = onBack) { Text("返回列表") }
        }

        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val seasons = page.seasons.orEmpty()
            if (seasons.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("同系列", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${seasons.size} 季", color = TvMuted, fontSize = 14.sp)
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(seasons, key = { it.seriesId }) { season ->
                        val selected = season.seriesId == seriesId
                        var focused by remember(season.seriesId) { mutableStateOf(false) }
                        Surface(
                            modifier = Modifier.width(178.dp).height(70.dp)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable(enabled = season.episodeCount > 0) {
                                    if (!selected) onSelectSeason(season.seriesId)
                                }
                                .focusable(enabled = season.episodeCount > 0),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                selected -> TvAccent
                                focused -> TvPanelRaised
                                else -> TvPanel
                            },
                            border = when {
                                focused -> BorderStroke(3.dp, TvFocusBorder)
                                selected -> BorderStroke(1.dp, TvAccent)
                                else -> null
                            }
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    seasonLabel(season),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) TvBackground else TvText
                                )
                                Text(
                                    season.title,
                                    fontSize = 11.sp,
                                    color = if (selected) Color(0xFF352515) else TvMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (season.episodeCount > 0) "${season.episodeCount} 集" else "暂未更新",
                                    fontSize = 11.sp,
                                    color = if (selected) Color(0xFF352515) else TvMuted
                                )
                            }
                        }
                    }
                }
            } else if (series?.isMultiSeason == true && page.seasons == null) {
                Text("正在获取同系列季信息…", fontSize = 14.sp, color = TvMuted)
            }
            Text("选集", fontSize = 31.sp, fontWeight = FontWeight.Bold)
            when {
                page.loading && page.episodes.isEmpty() -> Text("正在加载详情和选集…", color = TvMuted)
                page.error != null && page.episodes.isEmpty() -> Text("加载失败：${page.error?.message ?: "未知错误"}", color = TvMuted)
                page.episodes.isEmpty() -> Text("暂无可用选集", color = TvMuted)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 88.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(page.episodes, key = { it.id }) { episode ->
                        var focused by remember { mutableStateOf(false) }
                        Surface(
                            modifier = Modifier.height(58.dp).onFocusChanged { focused = it.isFocused }
                                .clickable {
                                    focusedEpisode = episode.number
                                    if (episode.playable) onPlay(seriesId, episode.number, 0.0)
                                }
                                .focusable(),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                focused -> TvFocus
                                focusedEpisode == episode.number -> TvAccent
                                else -> TvPanel
                            },
                            border = if (focused) BorderStroke(3.dp, TvFocusBorder) else null
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    episode.number.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (focused || focusedEpisode == episode.number) TvBackground else TvText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable
private fun PlayerScreen(model: AppModel, exo: androidx.media3.exoplayer.ExoPlayer, onBack: () -> Unit) {
    val player = model.player
    val failure = player.state as? PlayerState.Failed
    var speedIndex by remember { mutableIntStateOf(Player.speeds.indexOf(player.speed).coerceAtLeast(0)) }
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    player.page?.series?.title.orEmpty().ifBlank { "红果 TV 播放器" },
                    fontSize = 23.sp, color = TvText, fontWeight = FontWeight.Bold
                )
                Text("第 ${player.number} 集 · ${playerStateLabel(player.state)}", color = TvMuted, fontSize = 14.sp)
            }
            TextButton(onClick = onBack) { Text("返回", color = TvText) }
        }
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    this.player = exo
                    setKeepContentOnPlayerReset(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { view -> if (view.player !== exo) view.player = exo },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        failure?.let { failed ->
            Column(
                Modifier.fillMaxWidth().background(TvPanel, RoundedCornerShape(10.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("播放失败：${failed.message}", color = TvText, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { player.retry() }) { Text("重新尝试") }
                    if (failed.diagnostic?.canRefreshDevice == true) {
                        TextButton(onClick = { player.retryWithFreshDevice() }) {
                            Text("更换匿名设备重试", color = TvAccent)
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { player.togglePause() }) {
                Text(if (player.video.wantsToPlay && player.video.status != com.xiaobei206.hongguotvnext.model.TimeControl.PAUSED) "暂停" else "播放")
            }
            TextButton(onClick = { player.skip(-10.0) }) { Text("后退 10 秒", color = TvText) }
            TextButton(onClick = { player.skip(10.0) }) { Text("前进 10 秒", color = TvText) }
            TextButton(onClick = { player.step(-1) }) { Text("上一集", color = TvText) }
            TextButton(onClick = { player.step(1) }) { Text("下一集", color = TvText) }
            TextButton(onClick = {
                speedIndex = (speedIndex + 1) % Player.speeds.size
                player.chooseSpeed(Player.speeds[speedIndex])
            }) { Text("${player.speed}×", color = TvAccent) }
            TextButton(onClick = {
                val qualities = player.qualities
                if (qualities.isNotEmpty()) {
                    val current = qualities.indexOf(player.quality).coerceAtLeast(-1)
                    player.chooseQuality(qualities[(current + 1) % qualities.size])
                }
            }) {
                Text(if (player.quality > 0) "${player.quality}P" else "清晰度", color = TvAccent)
            }
        }
        Text("位置 ${formatPosition(player.video.position)} / ${formatPosition(player.video.duration)}", color = TvMuted, fontSize = 13.sp)
    }
}

private fun playerStateLabel(state: PlayerState): String = when (state) {
    PlayerState.Idle -> "待机"
    PlayerState.Resolving -> "正在解析视频"
    PlayerState.ResettingDevice -> "正在恢复设备"
    PlayerState.Buffering -> "缓冲中"
    PlayerState.Playing -> "播放中"
    is PlayerState.Failed -> "播放失败：${state.message}"
}

@Composable
private fun HandoffToggleRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    controlEnabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (focused) TvFocus else TvPanel,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (focused) 4.dp else 1.dp, if (focused) TvFocusBorder else TvPanelRaised)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "设备接力（手机投屏）",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (focused) Color.Black else TvText
                )
                Text(
                    if (enabled) "已开启：应用在前台时，手机可以发现电视并发送播放任务。"
                    else "已关闭：停止广播接力服务，也不接收接力任务。",
                    color = if (focused) Color(0xFF352515) else TvMuted,
                    fontSize = 14.sp
                )
            }
            Switch(
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
                checked = enabled,
                onCheckedChange = onEnabledChange,
                enabled = controlEnabled
            )
        }
    }
}

@Composable
private fun DeviceScreen(
    server: TvHandoffServer?,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(server?.displayName) { mutableStateOf(server?.displayName.orEmpty()) }
    Column(
        Modifier.fillMaxSize().background(TvBackground).padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("设备接力", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("电视通过局域网服务发现与 TCP JSON 协议接收手机播放任务。", color = TvMuted, fontSize = 19.sp)
        HandoffToggleRow(enabled, onEnabledChange, controlEnabled = server != null)
        Text("设备编号：${server?.shortCode ?: "—"}", fontSize = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("电视显示名称") }, singleLine = true
            )
            Button(onClick = {
                val ok = server?.setDisplayName(name) == true
                Toast.makeText(
                    context,
                    if (ok) "名称已保存" else "无法保存名称",
                    Toast.LENGTH_SHORT
                ).show()
            }, enabled = server != null) { Text("保存名称") }
        }
        TextButton(onClick = onBack) { Text("返回") }
        Text("启用状态会保存；默认开启。为避免后台占用，只有应用在前台时才广播发现。", color = TvMuted, fontSize = 15.sp)
    }
}

@Composable
private fun DiagnosticsScreen(
    model: AppModel?,
    covers: com.xiaobei206.hongguotvnext.covers.CoverStore?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transferState by remember { mutableStateOf("尚未发送诊断") }
    val report = remember(model, model?.player?.state, covers) {
        buildString {
            appendLine("红果 TV Next v${BuildConfig.VERSION_NAME}")
            appendLine("ApplicationId: ${BuildConfig.APPLICATION_ID}")
            appendLine("SDK 启动状态: ${if (model == null) "失败或未就绪" else "已初始化"}")
            appendLine("播放状态: ${model?.player?.state}")
            appendLine("SDK 诊断: ${runCatching { com.xiaobei206.hongguotvnext.model.SdkDiagnostics.snapshot() }.getOrNull()}")
            appendLine("播放诊断: ${runCatching { PlaybackDiagnostics.snapshot() }.getOrNull()}")
            appendLine("封面诊断: ${covers?.diagnosticSummary() ?: "封面服务不可用"}")
        }
    }
    Column(
        Modifier.fillMaxSize().background(TvBackground).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("设备与播放诊断", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("诊断信息可能包含运行环境与播放错误，不包含签名密码。分享前请自行检查内容。", color = TvMuted)
        Surface(Modifier.weight(1f).fillMaxWidth(), color = TvPanel, shape = RoundedCornerShape(12.dp)) {
            LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
                item { Text(report, color = TvText, fontSize = 14.sp) }
            }
        }
        Text(transferState, color = TvMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("hongguoTV-next-diagnostics", report))
                Toast.makeText(context, "诊断信息已复制", Toast.LENGTH_SHORT).show()
            }) { Text("复制诊断") }
            Button(onClick = {
                scope.launch {
                    transferState = "正在搜索开心果手机并发送…"
                    val result = runCatching { PhoneDiagnosticClient.sendToAll(context, report) }
                    transferState = result.fold(
                        { "发现 ${it.found} 台手机，成功发送 ${it.delivered} 台；失败：${it.failures.joinToString().ifBlank { "无" }}" },
                        { "发送失败：${it.message ?: it.javaClass.simpleName}" }
                    )
                }
            }) { Text("发送到开心果手机") }
            TextButton(onClick = onBack) { Text("返回") }
        }
    }
}

@Composable
private fun SettingsScreen(
    handoffEnabled: Boolean,
    onHandoffEnabledChange: (Boolean) -> Unit,
    calibration: DisplayCalibration,
    onCalibrationChange: (DisplayCalibration) -> Unit,
    onOpenInterfaceCalibration: () -> Unit,
    onOpenVideoCalibration: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(TvBackground).padding(34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("设置", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("选择要调整的项目；画面校准会进入全屏高对比测试图。", color = TvMuted, fontSize = 17.sp)
            }
            SettingsActionRow(
                title = "返回主页",
                description = "离开设置",
                onClick = onBack,
                modifier = Modifier.width(220.dp)
            )
        }

        HandoffToggleRow(
            enabled = handoffEnabled,
            onEnabledChange = onHandoffEnabledChange
        )

        SettingsActionRow(
            title = "界面尺寸与边界",
            description = "先等比例缩放，再分别扩展上、下、左、右边界",
            onClick = onOpenInterfaceCalibration
        )
        SettingsActionRow(
            title = "视频画面尺寸与边界",
            description = "只调整视频画面；播放控制和诊断浮层不跟着变形",
            onClick = onOpenVideoCalibration
        )

        SettingsActionRow(
            title = "恢复所有画面设置默认",
            description = "界面和视频都恢复到 100%，四条边扩展值清零",
            onClick = {
                onCalibrationChange(DisplayCalibration())
            }
        )

        Spacer(Modifier.weight(1f))
        Text(
            "界面：${calibration.interfaceScalePercent}%  ·  视频：${calibration.videoScalePercent}%  ·  设置自动保存在本机",
            fontSize = 15.sp,
            color = TvMuted
        )
        Text("版本：${BuildConfig.VERSION_NAME}  ·  应用 ID：${BuildConfig.APPLICATION_ID}", fontSize = 13.sp, color = TvMuted)
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    focusedColor: Color = TvFocus,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.height(78.dp)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                val emphasis = if (focused) 1.025f else 1f
                scaleX = emphasis
                scaleY = emphasis
            }
            .clickable(onClick = onClick)
            .focusable(),
        color = if (focused) focusedColor else TvPanel,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (focused) 5.dp else 2.dp, if (focused) TvFocusBorder else TvPanelRaised),
        shadowElevation = if (focused) 8.dp else 0.dp
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    color = if (focused) Color.Black else TvText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    description,
                    color = if (focused) Color(0xFF352515) else TvMuted,
                    fontSize = 15.sp
                )
            }
            Text(
                "›",
                color = if (focused) Color.Black else TvAccent,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun StartupProblem(message: String) {
    Column(
        Modifier.fillMaxSize().background(TvBackground).padding(36.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("红果 TV Next", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TvAccent)
        Spacer(Modifier.height(14.dp))
        Text("核心服务尚未就绪", fontSize = 25.sp, color = TvText)
        Spacer(Modifier.height(10.dp))
        Text(message.ifBlank { "应用正在初始化，或者 SDK 初始化失败。请查看 Logcat 中 HongguoTVNext 标签。" }, color = TvMuted, fontSize = 18.sp)
    }
}
