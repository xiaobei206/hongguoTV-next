package com.xiaobei206.hongguotvnext

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayCalibrationTest {
    @Test
    fun normalizedClampsInterfaceZoomAndOffsets() {
        val result = DisplayCalibration(
            interfaceScalePercent = 999,
            interfaceOffsetXDp = -999,
            interfaceOffsetYDp = 999,
        ).normalized()

        assertEquals(DisplayCalibration.INTERFACE_SCALE_MAX, result.interfaceScalePercent)
        assertEquals(-DisplayCalibration.INTERFACE_OFFSET_LIMIT_DP, result.interfaceOffsetXDp)
        assertEquals(DisplayCalibration.INTERFACE_OFFSET_LIMIT_DP, result.interfaceOffsetYDp)
    }

    @Test
    fun normalizedClampsVideoZoomAndOffsets() {
        val result = DisplayCalibration(
            videoScalePercent = 1,
            videoOffsetXDp = 999,
            videoOffsetYDp = -999,
        ).normalized()

        assertEquals(DisplayCalibration.VIDEO_SCALE_MIN, result.videoScalePercent)
        assertEquals(DisplayCalibration.VIDEO_OFFSET_LIMIT_DP, result.videoOffsetXDp)
        assertEquals(-DisplayCalibration.VIDEO_OFFSET_LIMIT_DP, result.videoOffsetYDp)
    }

    @Test
    fun defaultsAreNeutral() {
        val result = DisplayCalibration().normalized()
        assertEquals(100, result.interfaceScalePercent)
        assertEquals(0, result.interfaceOffsetXDp)
        assertEquals(0, result.interfaceOffsetYDp)
        assertEquals(100, result.videoScalePercent)
        assertEquals(0, result.videoOffsetXDp)
        assertEquals(0, result.videoOffsetYDp)
    }
}
