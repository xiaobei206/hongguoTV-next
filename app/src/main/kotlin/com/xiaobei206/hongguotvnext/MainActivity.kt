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
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.xiaobei206.hongguotvnext.playback.PlaybackDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

private val TvBackground = Color(0xFF0A0F17)
private val TvPanel = Color(0xFF151E2B)
private val TvPanelRaised = Color(0xFF202C3D)
private val TvAccent = Color(0xFFB7E66E)
private val TvText = Color(0xFFF4F6FA)
private val TvMuted = Color(0xFFACB7C7)

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

private enum class TvScreen { BROWSE, DETAIL, PLAYER, DEVICE, DIAGNOSTICS, SETTINGS }

@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class MainActivity : ComponentActivity() {
    private val tvApp get() = application as TvApplication

    private var screen by mutableStateOf(TvScreen.BROWSE)
    private var tab by mutableStateOf(TvTab.RECOMMEND)
    private var detailId by mutableStateOf("")
    private var playerReturnScreen by mutableStateOf(TvScreen.BROWSE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
                        PlayerScreen(
                            model = model,
                            exo = tvApp.video.exo,
                            onBack = { goBack(model) }
                        )
                    } else StartupProblem(tvApp.startupError)
                    TvScreen.DEVICE -> DeviceScreen(
                        server = tvApp.handoffServer,
                        onBack = { screen = TvScreen.BROWSE }
                    )
                    TvScreen.DIAGNOSTICS -> DiagnosticsScreen(
                        model = model,
                        onBack = { screen = TvScreen.BROWSE }
                    )
                    TvScreen.SETTINGS -> SettingsScreen(
                        onBack = { screen = TvScreen.BROWSE }
                    )
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
            TvScreen.DETAIL, TvScreen.DEVICE, TvScreen.DIAGNOSTICS, TvScreen.SETTINGS ->
                screen = TvScreen.BROWSE
            TvScreen.BROWSE -> if (tab != TvTab.RECOMMEND) tab = TvTab.RECOMMEND
        }
    }

    override fun onStart() {
        super.onStart()
        tvApp.handoffServer?.let { server ->
            server.listener = ::handleHandoffRequest
            server.setAvailable(true)
        }
        tvApp.model?.player?.returnToApp()
    }

    override fun onStop() {
        tvApp.handoffServer?.let { server ->
            server.listener = null
            server.setAvailable(false)
        }
        tvApp.model?.let { model ->
            model.player.leaveApp()
            model.saveSession(model.session())
            model.flush()
        }
        super.onStop()
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
            val cards = feed?.cards ?: localCards
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
                    if (submittedQuery.isBlank()) {
                        Text(
                            if (model.hotLoading) "正在获取热门搜索…" else "输入关键词后开始搜索。搜索不阻塞其他栏目。",
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
                                date,
                                model.dayIsOn(date),
                                { model.chooseDay(date) }
                            )
                        }
                    }
                }

                if (cards.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            when {
                                selectedTab == TvTab.SEARCH && submittedQuery.isBlank() -> "等待搜索"
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
                        items(cards, key = { it.id }) { card ->
                            CatalogCardView(
                                card = card,
                                covers = covers,
                                onClick = {
                                    if (selectedTab == TvTab.FAVORITES || selectedTab == TvTab.HISTORY) {
                                        onOpenSeries(card.id)
                                    } else {
                                        onOpenSeries(card.id)
                                    }
                                },
                            )
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
            .clickable(onClick = onClick)
            .focusable(),
        color = when {
            selected -> TvAccent
            focused -> TvPanelRaised
            else -> TvPanel
        },
        shape = RoundedCornerShape(10.dp),
        border = if (focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                title,
                color = if (selected) TvBackground else TvText,
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
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
        modifier = Modifier.height(40.dp).onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick).focusable(),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) TvAccent else if (focused) TvPanelRaised else TvPanel,
        border = if (focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Box(Modifier.padding(horizontal = 15.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) TvBackground else TvText, fontSize = 15.sp)
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
        border = if (focused) BorderStroke(3.dp, TvAccent) else null
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
                                focusedEpisode == episode.number -> TvAccent
                                focused -> TvPanelRaised
                                else -> TvPanel
                            },
                            border = if (focused) BorderStroke(2.dp, Color.White) else null
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    episode.number.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (focusedEpisode == episode.number) TvBackground else TvText
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
private fun DeviceScreen(server: TvHandoffServer?, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember(server?.displayName) { mutableStateOf(server?.displayName.orEmpty()) }
    Column(
        Modifier.fillMaxSize().background(TvBackground).padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("设备接力", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("电视通过局域网服务发现与 TCP JSON 协议接收手机播放任务。", color = TvMuted, fontSize = 19.sp)
        Text("接力广播服务：${server?.let { "已初始化" } ?: "未初始化"}", fontSize = 18.sp)
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { server?.setAvailable(true) }, enabled = server != null) { Text("开启发现") }
            TextButton(onClick = { server?.setAvailable(false) }, enabled = server != null) { Text("关闭发现") }
            TextButton(onClick = onBack) { Text("返回") }
        }
        Text("真实手机发现与任务播放确认仍需在同一局域网的设备上测试。", color = TvMuted, fontSize = 15.sp)
    }
}

@Composable
private fun DiagnosticsScreen(model: AppModel?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transferState by remember { mutableStateOf("尚未发送诊断") }
    val report = remember(model, model?.player?.state) {
        buildString {
            appendLine("红果 TV Next v${BuildConfig.VERSION_NAME}")
            appendLine("ApplicationId: ${BuildConfig.APPLICATION_ID}")
            appendLine("SDK 启动状态: ${if (model == null) "失败或未就绪" else "已初始化"}")
            appendLine("播放状态: ${model?.player?.state}")
            appendLine("SDK 诊断: ${runCatching { com.xiaobei206.hongguotvnext.model.SdkDiagnostics.snapshot() }.getOrNull()}")
            appendLine("播放诊断: ${runCatching { PlaybackDiagnostics.snapshot() }.getOrNull()}")
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
private fun SettingsScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(TvBackground).padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("设置", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("版本：${BuildConfig.VERSION_NAME}", fontSize = 19.sp)
        Text("应用 ID：${BuildConfig.APPLICATION_ID}", fontSize = 17.sp, color = TvMuted)
        Text("当前为与旧红果 TV 并行安装的独立应用。", fontSize = 18.sp, color = TvMuted)
        Text("发布签名文件位于工程根目录；正式密钥不会内置在 APK 或公开仓库。", fontSize = 17.sp, color = TvMuted)
        TextButton(onClick = onBack) { Text("返回") }
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
