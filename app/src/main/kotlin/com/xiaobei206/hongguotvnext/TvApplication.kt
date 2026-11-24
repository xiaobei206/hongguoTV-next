package com.xiaobei206.hongguotvnext

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.media3.datasource.DefaultHttpDataSource
import com.xiaobei206.hongguotvnext.covers.CoverStore
import com.xiaobei206.hongguotvnext.model.AppModel
import com.xiaobei206.hongguotvnext.model.SdkCatalog
import com.xiaobei206.hongguotvnext.model.SdkDiagnostics
import com.xiaobei206.hongguotvnext.model.debugLogger
import com.xiaobei206.hongguotvnext.playback.ExoVideo
import com.xiaobei206.hongguotvnext.playback.StreamRegistry
import dev.openhongguo.ClientOptions
import dev.openhongguo.OpenhongguoClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.io.File

@androidx.media3.common.util.UnstableApi
class TvApplication : Application() {
    var model: AppModel? = null
        private set
    lateinit var video: ExoVideo
        private set
    lateinit var covers: CoverStore
        private set
    private lateinit var appScope: CoroutineScope
    var startupError: String = ""
        private set
    var handoffServer: TvHandoffServer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        val debug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        SdkDiagnostics.initialize(this)
        if (debug) debugLogger = { Log.d("HongguoTVNext", it) }

        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("hongguoTV-next/${BuildConfig.VERSION_NAME} AndroidTV")
        val streams = StreamRegistry(http)
        video = ExoVideo(this, streams, http)
        covers = CoverStore(File(cacheDir, "covers"))
        appScope = MainScope()

        try {
            val client = OpenhongguoClient.create(
                this,
                ClientOptions(
                    retryAttempts = 3,
                    deviceResets = 3,
                    logger = { line ->
                        SdkDiagnostics.record(line)
                        if (debug) Log.d("HongguoTVNext-SDK", line)
                    },
                ),
            )
            model = AppModel(SdkCatalog(client), filesDir, cacheDir, appScope, video, streams).also {
                it.artwork = covers::jpeg
            }
            handoffServer = TvHandoffServer(this).also { it.start() }
        } catch (e: Exception) {
            startupError = e.message ?: e.javaClass.simpleName
            Log.e("HongguoTVNext", "SDK initialization failed", e)
        }
    }

    override fun onTerminate() {
        handoffServer?.stop()
        model?.flush()
        if (::video.isInitialized) video.exo.release()
        super.onTerminate()
    }
}
