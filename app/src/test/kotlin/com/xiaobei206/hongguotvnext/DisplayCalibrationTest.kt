package com.xiaobei206.hongguotvnext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayCalibrationTest {
    @Test
    fun defaultsUseFullViewportWithNoEdgeExpansion() {
        val c = DisplayCalibration().normalized()
        assertEquals(100, c.interfaceScalePercent)
        assertEquals(100, c.videoScalePercent)
        assertEquals(0, c.interfaceLeftExpandSteps)
        assertEquals(0, c.interfaceRightExpandSteps)
        assertEquals(0, c.interfaceTopExpandSteps)
        assertEquals(0, c.interfaceBottomExpandSteps)
    }

    @Test
    fun eachInterfaceEdgeMovesOutwardWithoutMovingOppositeEdge() {
        val base = DisplayCalibration(interfaceScalePercent = 95)
        val before = base.bounds(CalibrationTarget.INTERFACE, 1920f, 1080f)
        val after = base.copy(interfaceTopExpandSteps = 1)
            .bounds(CalibrationTarget.INTERFACE, 1920f, 1080f)

        assertTrue(after.top < before.top)
        assertEquals(before.bottom, after.bottom, 0.001f)
        assertEquals(before.left, after.left, 0.001f)
        assertEquals(before.right, after.right, 0.001f)
        assertEquals(5.4f, before.top - after.top, 0.001f)
    }

    @Test
    fun oppositeEdgesCanBeExpandedIndependentlyToViewportBoundary() {
        val c = DisplayCalibration(
            interfaceScalePercent = 95,
            interfaceLeftExpandSteps = 5,
            interfaceBottomExpandSteps = 5,
        )
        val bounds = c.bounds(CalibrationTarget.INTERFACE, 1920f, 1080f)
        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(1080f, bounds.bottom, 0.001f)
        assertEquals(0.975f * 1920f, bounds.right, 0.001f)
        assertEquals(0.025f * 1080f, bounds.top, 0.001f)
    }

    @Test
    fun normalizationClampsScaleAndEdgesToAvailableMargin() {
        val normalized = DisplayCalibration(
            interfaceScalePercent = 999,
            interfaceLeftExpandSteps = 20,
            interfaceTopExpandSteps = 20,
            videoScalePercent = 1,
            videoRightExpandSteps = 999,
        ).normalized()

        assertEquals(100, normalized.interfaceScalePercent)
        assertEquals(0, normalized.interfaceLeftExpandSteps)
        assertEquals(0, normalized.interfaceTopExpandSteps)
        assertEquals(70, normalized.videoScalePercent)
        assertEquals(30, normalized.videoRightExpandSteps)
    }

    @Test
    fun videoAndInterfaceBoundsAreIndependent() {
        val c = DisplayCalibration(
            interfaceScalePercent = 95,
            interfaceLeftExpandSteps = 2,
            videoScalePercent = 90,
            videoRightExpandSteps = 4,
        )
        val ui = c.bounds(CalibrationTarget.INTERFACE, 1000f, 600f)
        val video = c.bounds(CalibrationTarget.VIDEO, 1000f, 600f)
        assertEquals(15f, ui.left, 0.001f)
        assertEquals(970f, video.right, 0.001f)
    }
}
