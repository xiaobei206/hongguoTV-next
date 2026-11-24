package com.xiaobei206.hongguotvnext.covers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
import com.xiaobei206.hongguotvnext.BuildConfig
import com.xiaobei206.hongguotvnext.model.debugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Candidate URLs for ByteDance poster CDNs.
 *
 * The old TV client used these URL rewrites for a reason: a number of
 * byteimg/fqnovelpic URLs ending in tplv-obj.image negotiate an image format
 * that older TV decoders cannot read. Ask the CDN for its JPEG rendition
 * first, but always keep the original signed URL as a fallback.
 */
internal fun coverCandidateUrls(raw: String): List<String> {
    val source = raw.trim()
    if (source.isEmpty()) return emptyList()

    val first = if (source.startsWith("http://", true)) {
        "https://" + source.substringAfter("://")
    } else {
        source
    }
    val uri = runCatching { URI(first) }.getOrNull()
    val host = uri?.host.orEmpty().lowercase()
    val path = uri?.path.orEmpty()
    val candidates = LinkedHashSet<String>()

    // Novel covers can be served as a stable JPEG without the dynamic image
    // transformation suffix. Keep the original URL in the fallback list.
    if ((host.endsWith(".fqnovelpic.com") || host.endsWith(".byteimg.com")) &&
        (path.contains("/novel-pic/") || path.contains("/novel-static/"))
    ) {
        val segment = if (path.contains("/novel-pic/")) "/novel-pic/" else "/novel-static/"
        val id = path.substringAfter(segment).substringBefore("~").substringBefore(".")
        if (id.matches(Regex("[a-fA-F0-9]{32}"))) {
            candidates += "https://p3-novel.byteimg.com/img" + segment + id + "~tplv-noop.jpeg"
        }
    }

    // Force JPEG for the standard ByteDance dynamic image endpoint.
    if ((host.endsWith(".byteimg.com") || host.endsWith(".fqnovelpic.com")) &&
        path.contains("~tplv-obj.image")
    ) {
        val legacyPath = path.replace("~tplv-obj.image", "~tplv-noop.jpeg")
        if (uri != null) {
            runCatching {
                URI(uri.scheme, uri.authority, legacyPath, uri.query, null).toASCIIString()
            }.getOrNull()?.let(candidates::add)
        }
    }

    candidates += first
    if (source.startsWith("http://", true)) candidates += source
    return candidates.filter {
        it.startsWith("https://", true) || it.startsWith("http://", true)
    }.distinct()
}

/**
 * CoverStore downloads posters once, keeps them on disk as scaled JPEG,
 * and shares concurrent requests for the same CDN resource.
 */
class CoverStore(private val dir: File) {
    companion object {
        /** The longest side covers are kept at, in pixels. */
        const val MAX_PIXELS = 600
        private const val MAX_DOWNLOAD_BYTES = 16 * 1024 * 1024
    }

    private val memory = object : LruCache<String, Bitmap>(96 shl 20) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    // Compose asks on the main thread; network and decode work run on IO.
    private val loading = HashMap<String, Deferred<Bitmap?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val io = Dispatchers.IO.limitedParallelism(6)

    private val diagnosticsLock = Any()
    private var successfulLoads = 0
    private var failedLoads = 0
    @Volatile private var lastFailure = "尚未发生封面加载失败"

    init {
        dir.mkdirs()
    }

    /** A compact, query-free status line for the in-app diagnostic report. */
    fun diagnosticSummary(): String = synchronized(diagnosticsLock) {
        "成功 $successfulLoads 次，失败 $failedLoads 次；最近错误：$lastFailure"
    }

    private fun recordFailure(message: String) {
        synchronized(diagnosticsLock) {
            failedLoads++
            lastFailure = message.take(220)
        }
        debugLog { "cover: $message" }
    }

    private fun recordSuccess() {
        synchronized(diagnosticsLock) { successfulLoads++ }
    }

    /** Cancels in-flight cover downloads when playback takes priority; cache entries stay intact. */
    fun cancelPendingLoads() {
        val pending = loading.values.toList()
        loading.clear()
        pending.forEach { it.cancel() }
    }

    /** The cover at `raw` if memory has it: views show it at once, without a fade. */
    fun cached(raw: String): Bitmap? {
        val first = coverCandidateUrls(raw).firstOrNull() ?: return null
        return memory.get(key(first))
    }

    /** The cover at `raw`, from memory, the disk or the CDN. Call it on the main thread. */
    suspend fun image(raw: String): Bitmap? {
        val candidates = coverCandidateUrls(raw)
        val first = candidates.firstOrNull() ?: return null
        val k = key(first)
        memory.get(k)?.let { return it }

        val task = loading.getOrPut(k) { scope.async(io) { load(k, candidates) } }
        val bmp = task.await()
        loading.remove(k)
        if (bmp != null) {
            memory.put(k, bmp)
            recordSuccess()
        }
        return bmp
    }

    /** The cached JPEG, if the disk has it. Uses the same normalized key as the image loader. */
    fun jpeg(raw: String): ByteArray? {
        val first = coverCandidateUrls(raw).firstOrNull() ?: return null
        val cached = file(key(first))
        return if (cached.isFile) runCatching { cached.readBytes() }.getOrNull() else null
    }

    /** Names a cover by host + path, ignoring volatile signed query parameters. */
    private fun key(raw: String): String {
        val u = runCatching { Uri.parse(raw.trim()) }.getOrNull()
        val name = if (u?.host != null) u.host + u.path.orEmpty() else raw.substringBefore("?")
        return MessageDigest.getInstance("SHA-1").digest(name.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun file(k: String) = File(dir, "$k.jpg")

    private fun load(k: String, candidates: List<String>): Bitmap? {
        val path = file(k)
        if (path.exists()) {
            BitmapFactory.decodeFile(path.path)?.let { return it }
            // A partial/old cache file must not block a fresh download.
            path.delete()
            recordFailure("已删除无法解码的本地封面缓存")
        }

        for (candidate in candidates) {
            val bytes = download(candidate) ?: continue
            val img = decode(bytes)
            if (img == null) {
                recordFailure("图片格式无法解码：${safeHost(candidate)}，${bytes.size} bytes")
                continue
            }
            save(img, path)
            return img
        }
        return null
    }

    private fun decode(bytes: ByteArray): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeModern(bytes) else decodeLegacy(bytes)
    } catch (e: OutOfMemoryError) {
        memory.evictAll()
        recordFailure("图片解码内存不足")
        null
    } catch (e: Exception) {
        recordFailure("图片解码异常：${e.javaClass.simpleName}")
        null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(bytes: ByteArray): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            if (w <= 0 || h <= 0) throw IOException("图片尺寸无效")
            val scale = MAX_PIXELS.toDouble() / max(w, h)
            if (scale < 1) {
                decoder.setTargetSize(
                    (w * scale).roundToInt().coerceAtLeast(1),
                    (h * scale).roundToInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

    /** API 23-27 decodes common formats with bounded memory; HEIC/AVIF URLs use the JPEG CDN fallback above. */
    private fun decodeLegacy(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sample = 1
        while (max(width / sample, height / sample) > MAX_PIXELS * 2) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            },
        ) ?: return null

        val scale = minOf(1.0, MAX_PIXELS.toDouble() / max(decoded.width, decoded.height))
        if (scale >= 1.0) return decoded

        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).roundToInt().coerceAtLeast(1),
            (decoded.height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun download(raw: String): ByteArray? {
        val connection = runCatching { URL(raw).openConnection() as HttpURLConnection }.getOrNull()
            ?: run {
                recordFailure("封面 URL 无法打开：${safeHost(raw)}")
                return null
            }
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,image/*;q=0.8,*/*;q=0.5")
            connection.setRequestProperty("User-Agent", "hongguoTV-next/${BuildConfig.VERSION_NAME} AndroidTV")

            val status = connection.responseCode
            if (status !in 200..299) {
                recordFailure("封面 HTTP $status：${safeHost(raw)}")
                null
            } else {
                val length = connection.contentLength.toLong()
                if (length > MAX_DOWNLOAD_BYTES) {
                    recordFailure("封面文件过大：${length} bytes")
                    null
                } else {
                    connection.inputStream.use { input ->
                        val bytes = input.readBytes()
                        if (bytes.size > MAX_DOWNLOAD_BYTES) {
                            recordFailure("封面响应超过下载上限：${bytes.size} bytes")
                            null
                        } else {
                            bytes
                        }
                    }
                }
            }
        } catch (e: Exception) {
            recordFailure("封面网络异常：${e.javaClass.simpleName}，${safeHost(raw)}")
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun save(img: Bitmap, path: File) {
        val tmp = File(path.parentFile, path.name + ".tmp")
        try {
            val compressed = FileOutputStream(tmp).use { out ->
                val ok = img.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
                out.fd.sync()
                ok
            }
            if (!compressed) throw IOException("封面 JPEG 压缩失败")
            if (!tmp.renameTo(path)) {
                // Some Android filesystems do not replace an existing target.
                path.delete()
                if (!tmp.renameTo(path)) throw IOException("封面缓存文件替换失败")
            }
        } catch (e: Exception) {
            tmp.delete()
            recordFailure("封面缓存保存失败：${e.javaClass.simpleName}")
        }
    }

    private fun safeHost(raw: String): String =
        runCatching { URI(raw).host }.getOrNull()?.take(100) ?: "unknown"
}
