package com.xiaobei206.hongguotvnext.covers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.net.Uri
import androidx.annotation.RequiresApi
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * CoverStore downloads posters once, keeps them on disk as JPEG, scaled
 * down, and in memory as bitmaps while there is room. Android 9+ can decode
 * the CDN's HEIC responses; older systems request common formats and use
 * BitmapFactory as a compatibility fallback.
 */
class CoverStore(private val dir: File) {
    companion object {
        /** The longest side covers are kept at, in pixels: a grid's poster on a dense screen, or the series page's. */
        const val MAX_PIXELS = 600
    }

    private val memory = object : LruCache<String, Bitmap>(96 shl 20) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    // On the main thread, where the views ask.
    private val loading = HashMap<String, Deferred<Bitmap?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val io = Dispatchers.IO.limitedParallelism(6)

    init {
        dir.mkdirs()
    }

    /** Cancels in-flight cover downloads when playback takes priority; cache entries stay intact. */
    fun cancelPendingLoads() {
        val pending = loading.values.toList()
        loading.clear()
        pending.forEach { it.cancel() }
    }

    /** The cover at `raw` if memory has it: views show it at once, without a fade. */
    fun cached(raw: String): Bitmap? = if (raw.isEmpty()) null else memory.get(key(raw))

    /** The cover at `raw`, from memory, the disk or the CDN. Call it on the main thread. */
    suspend fun image(raw: String): Bitmap? {
        if (raw.isEmpty()) return null
        val k = key(raw)
        memory.get(k)?.let { return it }
        val task = loading.getOrPut(k) { scope.async(io) { load(k, raw) } }
        val bmp = task.await()
        loading.remove(k)
        if (bmp != null) memory.put(k, bmp)
        return bmp
    }

    /** The cover's JPEG, if the disk has it. */
    fun jpeg(raw: String): ByteArray? =
        if (raw.isEmpty()) null else file(key(raw)).takeIf { it.exists() }?.let { runCatching { it.readBytes() }.getOrNull() }

    /** Names a cover by its URL without the query, whose signature changes from one response to the next. */
    private fun key(raw: String): String {
        val u = runCatching { Uri.parse(raw) }.getOrNull()
        val name = if (u?.host != null) u.host + u.path.orEmpty() else raw
        return MessageDigest.getInstance("SHA-1").digest(name.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun file(k: String) = File(dir, "$k.jpg")

    private fun load(k: String, raw: String): Bitmap? {
        val path = file(k)
        if (path.exists()) {
            BitmapFactory.decodeFile(path.path)?.let { return it }
        }
        val bytes = download(raw) ?: return null
        val img = decode(bytes) ?: return null
        save(img, path)
        return img
    }

    private fun decode(bytes: ByteArray): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeModern(bytes) else decodeLegacy(bytes)

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(bytes: ByteArray): Bitmap? = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = MAX_PIXELS.toDouble() / max(w, h)
            if (scale < 1) {
                decoder.setTargetSize(
                    (w * scale).roundToInt().coerceAtLeast(1),
                    (h * scale).roundToInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (e: Exception) {
        null
    }

    /** API 23-27 has no platform HEIC decoder; decode negotiated JPEG/PNG/WebP with bounded memory. */
    private fun decodeLegacy(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sample = 1
        while (max(width / sample, height / sample) > MAX_PIXELS * 2) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
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
        val conn = runCatching { URL(raw).openConnection() as HttpURLConnection }.getOrNull() ?: return null
        return try {
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,*/*;q=0.8")
            if (conn.responseCode != 200) null else conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun save(img: Bitmap, path: File) {
        val tmp = File(path.path + ".tmp")
        try {
            FileOutputStream(tmp).use { img.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            tmp.renameTo(path)
        } catch (e: Exception) {
            tmp.delete()
        }
    }
}
