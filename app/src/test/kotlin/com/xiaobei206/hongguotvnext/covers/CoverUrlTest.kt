package com.xiaobei206.hongguotvnext.covers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverUrlTest {
    @Test
    fun byteimgDynamicImagesPreferJpegAndKeepOriginalFallback() {
        val raw = "https://p3-sign.byteimg.com/tos-cn-i-123/abcdef~tplv-obj.image?x=1&sig=abc"
        val urls = coverCandidateUrls(raw)

        assertEquals(
            "https://p3-sign.byteimg.com/tos-cn-i-123/abcdef~tplv-noop.jpeg?x=1&sig=abc",
            urls.first(),
        )
        assertTrue(urls.contains(raw))
    }

    @Test
    fun novelCoverUsesStableJpegRenditionAndKeepsSignedOriginal() {
        val raw = "https://p3-novel.byteimg.com/img/novel-pic/0123456789abcdef0123456789abcdef~tplv-obj.image?token=abc"
        val urls = coverCandidateUrls(raw)

        assertEquals(
            "https://p3-novel.byteimg.com/img/novel-pic/0123456789abcdef0123456789abcdef~tplv-noop.jpeg",
            urls.first(),
        )
        assertTrue(urls.contains(raw))
    }

    @Test
    fun httpSourcesTryHttpsBeforeOriginalHttp() {
        val raw = "http://example.com/poster.jpg?sig=1"
        val urls = coverCandidateUrls(raw)

        assertEquals("https://example.com/poster.jpg?sig=1", urls.first())
        assertTrue(urls.contains(raw))
    }

    @Test
    fun blankAndUnsupportedSchemesAreIgnored() {
        assertTrue(coverCandidateUrls("  ").isEmpty())
        assertTrue(coverCandidateUrls("file:///poster.jpg").isEmpty())
    }
}
