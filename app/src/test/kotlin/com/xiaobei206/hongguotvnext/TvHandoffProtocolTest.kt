package com.xiaobei206.hongguotvnext

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvHandoffProtocolTest {
    @Test
    fun acceptsTheExistingPhoneToTvPlayMessage() {
        val request = JSONObject()
            .put("protocol", 1)
            .put("type", "play")
            .put("transferId", "transfer-123")
            .put("targetDeviceId", "tv-abc")
            .put("seriesId", "series-xyz")
            .put("episode", 7)
            .put("positionMs", 123_456L)
            .toString()

        val decoded = TvHandoffProtocol.parseRequest(request)
        assertEquals("transfer-123", decoded?.transferId)
        assertEquals("tv-abc", decoded?.targetDeviceId)
        assertEquals("series-xyz", decoded?.seriesId)
        assertEquals(7, decoded?.episode)
        assertEquals(123_456L, decoded?.positionMs)
    }

    @Test
    fun rejectsUnsupportedTypesVersionsAndOutOfRangeFields() {
        val valid = JSONObject()
            .put("protocol", 1)
            .put("type", "play")
            .put("transferId", "t")
            .put("seriesId", "s")
            .put("episode", 1)
            .put("positionMs", 0L)

        assertNull(TvHandoffProtocol.parseRequest(JSONObject(valid.toString()).put("type", "pause").toString()))
        assertNull(TvHandoffProtocol.parseRequest(JSONObject(valid.toString()).put("protocol", 2).toString()))
        assertNull(TvHandoffProtocol.parseRequest(JSONObject(valid.toString()).put("episode", 0).toString()))
        assertNull(TvHandoffProtocol.parseRequest(JSONObject(valid.toString()).put("positionMs", -1).toString()))
        assertNull(TvHandoffProtocol.parseRequest(JSONObject(valid.toString()).put("seriesId", "x".repeat(257)).toString()))
    }

    @Test
    fun rejectsOversizedMessagesAndKeepsReplyFieldNames() {
        assertNull(TvHandoffProtocol.parseRequest("x".repeat(TvHandoffProtocol.MAX_REQUEST_BYTES + 1)))

        val line = TvHandoffProtocol.encodeReply(
            TvHandoffReply("transfer-123", true, "电视已开始播放", "tv-abc")
        )
        assertTrue(line.endsWith("\n"))
        val json = JSONObject(line.trim())
        assertEquals(1, json.getInt("protocol"))
        assertEquals("transfer-123", json.getString("transferId"))
        assertTrue(json.getBoolean("accepted"))
        assertEquals("电视已开始播放", json.getString("message"))
        assertEquals("tv-abc", json.getString("deviceId"))
        assertFalse(json.has("secret"))
    }
}
