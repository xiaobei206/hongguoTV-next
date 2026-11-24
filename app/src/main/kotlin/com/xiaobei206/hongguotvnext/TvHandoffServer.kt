package com.xiaobei206.hongguotvnext

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class TvHandoffRequest(
    val transferId: String,
    val targetDeviceId: String,
    val seriesId: String,
    val episode: Int,
    val positionMs: Long,
)

data class TvHandoffReply(
    val transferId: String,
    val accepted: Boolean,
    val message: String,
    val deviceId: String,
)

class TvHandoffServer(context: Context) {
    companion object {
        const val SERVICE_TYPE = "_hongguotv._tcp."
        const val PROTOCOL_VERSION = TvHandoffProtocol.PROTOCOL_VERSION
        private const val TAG = "HongguoTVHandoff"
        private const val PREFS = "tv_handoff_identity"
        private const val MAX_REQUEST_BYTES = TvHandoffProtocol.MAX_REQUEST_BYTES
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val executor = Executors.newCachedThreadPool { task ->
        Thread(task, "hongguotv-handoff").apply { isDaemon = true }
    }
    private val started = AtomicBoolean(false)

    val deviceId: String = prefs.getString("device_id", null)?.takeIf { it.isNotBlank() }
        ?: UUID.randomUUID().toString().also { prefs.edit().putString("device_id", it).apply() }

    val shortCode: String = prefs.getString("short_code", null)?.takeIf { it.length == 4 }
        ?: (1000 + kotlin.random.Random.nextInt(9000)).toString().also {
            prefs.edit().putString("short_code", it).apply()
        }

    @Volatile
    var displayName: String = prefs.getString("display_name", null)?.takeIf { it.isNotBlank() }
        ?: defaultDisplayName()
        private set

    @Volatile
    var enabled: Boolean = prefs.getBoolean("enabled", true)
        private set

    @Volatile
    var listener: ((TvHandoffRequest, (TvHandoffReply) -> Unit) -> Unit)? = null

    @Volatile private var foreground = false
    @Volatile private var available = false
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var serverPort = 0
    @Volatile private var registrationListener: NsdManager.RegistrationListener? = null
    @Volatile private var registrationInProgress = false
    @Volatile private var unregistering = false

    fun start() {
        if (!started.compareAndSet(false, true)) return
        executor.execute {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress(0))
                serverSocket = server
                serverPort = server.localPort
                if (available) registerService()
                while (!server.isClosed) {
                    val client = server.accept()
                    executor.execute { handleClient(client) }
                }
            } catch (e: Exception) {
                if (started.get()) Log.e(TAG, "TCP receiver stopped", e)
            }
        }
    }

    /** Persistent user preference. New installs default to enabled. */
    fun setEnabled(value: Boolean) {
        enabled = value
        prefs.edit().putBoolean("enabled", value).apply()
        updateAvailability()
    }

    /** NSD advertisement is only active while the Activity is in the foreground. */
    fun setForeground(value: Boolean) {
        foreground = value
        updateAvailability()
    }

    private fun updateAvailability() {
        val shouldAdvertise = foreground && enabled
        available = shouldAdvertise
        executor.execute {
            if (shouldAdvertise) registerService() else unregisterService()
        }
    }

    fun setDisplayName(value: String): Boolean {
        val clean = value.trim().replace(Regex("\\s+"), " ").take(40)
        if (clean.isBlank()) return false
        displayName = clean
        prefs.edit().putString("display_name", clean).apply()
        executor.execute {
            if (available) {
                if (registrationListener == null && !registrationInProgress && !unregistering) registerService()
                else unregisterService()
            }
        }
        return true
    }

    fun stop() {
        foreground = false
        available = false
        unregisterService()
        runCatching { serverSocket?.close() }
        serverSocket = null
        started.set(false)
        executor.shutdownNow()
    }

    private fun registerService() {
        if (!available || serverPort <= 0 || registrationListener != null || registrationInProgress || unregistering) return
        registrationInProgress = true
        val manufacturer = Build.MANUFACTURER.orEmpty().take(64)
        val model = Build.MODEL.orEmpty().take(64)
        val service = NsdServiceInfo().apply {
            serviceName = "hongguoTV-$shortCode"
            serviceType = SERVICE_TYPE
            port = serverPort
            setAttribute("deviceid", deviceId)
            setAttribute("label", displayName)
            setAttribute("manufacturer", manufacturer)
            setAttribute("model", model)
            setAttribute("code", shortCode)
            setAttribute("version", BuildConfig.VERSION_NAME)
            setAttribute("protocol", PROTOCOL_VERSION.toString())
        }
        val registration = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registrationListener = null
                registrationInProgress = false
                Log.w(TAG, "NSD registration failed: $errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                registrationInProgress = false
                Log.i(TAG, "Advertising ${serviceInfo.serviceName} as $displayName")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                registrationListener = null
                registrationInProgress = false
                unregistering = false
                if (available) registerService()
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD unregistration failed: $errorCode")
                registrationListener = null
                registrationInProgress = false
                unregistering = false
            }
        }
        registrationListener = registration
        try {
            nsd.registerService(service, NsdManager.PROTOCOL_DNS_SD, registration)
        } catch (e: Exception) {
            registrationListener = null
            Log.w(TAG, "Unable to register NSD service", e)
        }
    }

    private fun unregisterService() {
        val current = registrationListener ?: return
        registrationListener = null
        unregistering = true
        runCatching { nsd.unregisterService(current) }
            .onFailure {
                unregistering = false
                Log.w(TAG, "Unable to unregister NSD service", it)
            }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            try {
                client.soTimeout = 40_000
                val requestText = readRequest(client) ?: run {
                    writeReply(client, TvHandoffReply("", false, "请求为空", deviceId))
                    return
                }
                val request = parseRequest(requestText)
                if (request == null) {
                    writeReply(client, TvHandoffReply("", false, "播放任务格式无效或协议版本不兼容", deviceId))
                    return
                }
                if (request.targetDeviceId.isNotBlank() && request.targetDeviceId != deviceId) {
                    writeReply(client, TvHandoffReply(request.transferId, false, "任务目标不是这台电视", deviceId))
                    return
                }
                val receiver = listener
                if (receiver == null) {
                    writeReply(client, TvHandoffReply(request.transferId, false, "请先在电视上打开 hongguoTV 并保持运行", deviceId))
                    return
                }

                val latch = CountDownLatch(1)
                val completed = AtomicBoolean(false)
                val result = java.util.concurrent.atomic.AtomicReference<TvHandoffReply?>(null)
                val callback: (TvHandoffReply) -> Unit = { reply ->
                    if (completed.compareAndSet(false, true)) {
                        result.set(reply)
                        latch.countDown()
                    }
                }
                receiver.invoke(request, callback)
                val finished = latch.await(37, TimeUnit.SECONDS)
                val response = if (finished) {
                    result.get() ?: TvHandoffReply(request.transferId, false, "电视没有完成播放确认", deviceId)
                } else {
                    TvHandoffReply(request.transferId, false, "电视启动播放超时，请检查网络或重试", deviceId)
                }
                writeReply(client, response)
            } catch (e: Exception) {
                Log.w(TAG, "Handoff request failed", e)
                runCatching { writeReply(client, TvHandoffReply("", false, e.message ?: "电视接收请求失败", deviceId)) }
            }
        }
    }

    private fun readRequest(socket: Socket): String? {
        val output = ByteArrayOutputStream()
        val input = socket.getInputStream()
        while (output.size() <= MAX_REQUEST_BYTES) {
            val next = input.read()
            if (next < 0 || next == 10) break
            if (next != 13) output.write(next)
        }
        if (output.size() > MAX_REQUEST_BYTES) return null
        return output.toByteArray().toString(Charsets.UTF_8).takeIf { it.isNotBlank() }
    }

    private fun parseRequest(text: String): TvHandoffRequest? = TvHandoffProtocol.parseRequest(text)

    private fun writeReply(socket: Socket, reply: TvHandoffReply) {
        socket.getOutputStream().write(TvHandoffProtocol.encodeReply(reply).toByteArray(Charsets.UTF_8))
        socket.getOutputStream().flush()
    }

    private fun defaultDisplayName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        return listOf(manufacturer, model).filter { it.isNotBlank() }.distinct().joinToString(" ")
            .ifBlank { "Android TV" }
            .also { prefs.edit().putString("display_name", it).apply() }
    }
}


/**
 * Protocol codec split from Android sockets/NSD so the existing phone-to-TV
 * payload can be regression-tested without a device or network service.
 */
object TvHandoffProtocol {
    const val PROTOCOL_VERSION = 1
    const val MAX_REQUEST_BYTES = 8_192

    fun parseRequest(text: String): TvHandoffRequest? {
        if (text.toByteArray(Charsets.UTF_8).size > MAX_REQUEST_BYTES) return null
        return runCatching {
            val json = JSONObject(text)
            if (json.optInt("protocol") != PROTOCOL_VERSION || json.optString("type") != "play") {
                return null
            }
            val transferId = json.optString("transferId")
            val target = json.optString("targetDeviceId")
            val seriesId = json.optString("seriesId")
            val episode = json.optInt("episode")
            val position = json.optLong("positionMs", 0L)
            if (transferId.length !in 1..64 || seriesId.isBlank() || seriesId.length > 256) return null
            if (episode !in 1..100_000 || position !in 0L..86_400_000L) return null
            TvHandoffRequest(transferId, target, seriesId, episode, position)
        }.getOrNull()
    }

    fun encodeReply(reply: TvHandoffReply): String = JSONObject()
        .put("protocol", PROTOCOL_VERSION)
        .put("transferId", reply.transferId)
        .put("accepted", reply.accepted)
        .put("message", reply.message)
        .put("deviceId", reply.deviceId)
        .toString() + "\n"
}
