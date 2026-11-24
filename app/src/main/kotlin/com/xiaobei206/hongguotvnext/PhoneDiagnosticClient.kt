package com.xiaobei206.hongguotvnext

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class DiagnosticSendResult(
    val found: Int,
    val delivered: Int,
    val failures: List<String>,
)

private data class DiagnosticPhone(
    val serviceName: String,
    val host: String,
    val port: Int,
)

object PhoneDiagnosticClient {
    private const val TAG = "HongguoTVDiagnosticTx"
    private const val SERVICE_TYPE = "_kaixinguo-diagnostic._tcp."
    private const val PROTOCOL_VERSION = 1
    private const val MAX_REPLY_CHARS = 8_192

    suspend fun sendToAll(context: Context, report: String): DiagnosticSendResult =
        withContext(Dispatchers.IO) {
            require(report.isNotBlank()) { "诊断内容为空" }
            require(report.toByteArray(Charsets.UTF_8).size <= 192 * 1024) { "诊断信息超过局域网投送限制" }
            val phones = discover(context.applicationContext)
            if (phones.isEmpty()) return@withContext DiagnosticSendResult(0, 0, emptyList())
            val failures = mutableListOf<String>()
            var delivered = 0
            for (phone in phones) {
                try {
                    sendOne(phone, report)
                    delivered++
                } catch (e: Exception) {
                    failures += "${phone.serviceName}: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            DiagnosticSendResult(phones.size, delivered, failures)
        }

    private fun discover(context: Context): List<DiagnosticPhone> {
        val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val phones = ConcurrentHashMap<String, DiagnosticPhone>()
        val resolving = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "diagnostic receiver discovery started")
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.trimEnd('.').equals(SERVICE_TYPE.trimEnd('.'), true)) return
                if (!resolving.add(serviceInfo.serviceName)) return
                runCatching {
                    manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            resolving.remove(info.serviceName)
                            Log.w(TAG, "resolve failed: ${info.serviceName} code=$errorCode")
                        }
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            resolving.remove(info.serviceName)
                            val host = info.host?.hostAddress ?: return
                            if (info.port !in 1..65535) return
                            phones[info.serviceName] = DiagnosticPhone(info.serviceName, host, info.port)
                        }
                    })
                }.onFailure {
                    resolving.remove(serviceInfo.serviceName)
                    Log.w(TAG, "could not resolve ${serviceInfo.serviceName}", it)
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                phones.remove(serviceInfo.serviceName)
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "discovery start failed: $errorCode")
                runCatching { manager.stopServiceDiscovery(this) }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { manager.stopServiceDiscovery(this) }
            }
        }
        try {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            // Keep the browse window open long enough to resolve all advertised phones,
            // rather than sending only to the first result.
            Thread.sleep(2_300)
        } finally {
            runCatching { manager.stopServiceDiscovery(listener) }
        }
        // Resolve callbacks can be queued just after discovery stops.
        Thread.sleep(700)
        return phones.values.sortedBy { it.serviceName }
    }

    private fun sendOne(phone: DiagnosticPhone, report: String) {
        val reportId = UUID.randomUUID().toString()
        val json = JSONObject()
            .put("protocol", PROTOCOL_VERSION)
            .put("type", "diagnostic")
            .put("reportId", reportId)
            .put("sourceName", "hongguoTV")
            .put("sourceVersion", BuildConfig.VERSION_NAME)
            .put("createdAt", Instant.now().toString())
            .put("report", report)
        val socket = Socket()
        try {
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(phone.host, phone.port), 3_000)
            socket.soTimeout = 6_000
            socket.getOutputStream().write((json.toString() + "\n").toByteArray(Charsets.UTF_8))
            socket.getOutputStream().flush()
            val line = socket.getInputStream().bufferedReader(Charsets.UTF_8).readLine()
                ?: throw IllegalStateException("手机没有返回接收回执")
            if (line.length > MAX_REPLY_CHARS) throw IllegalStateException("手机回执过长")
            val reply = JSONObject(line)
            if (reply.optInt("protocol") != PROTOCOL_VERSION ||
                reply.optString("type") != "diagnostic_ack" ||
                reply.optString("reportId") != reportId
            ) throw IllegalStateException("手机回执编号不匹配")
            if (!reply.optBoolean("accepted", false)) {
                throw IllegalStateException(reply.optString("message", "手机拒绝接收"))
            }
        } finally {
            runCatching { socket.close() }
        }
    }
}
