package com.xiaobei206.hongguotvnext

/** Which full-screen calibration surface is being adjusted. */
enum class CalibrationTarget(val title: String) {
    INTERFACE("界面边界校准"),
    VIDEO("视频画面校准"),
}

/** A transformed rectangle in pixels, measured against the original full-screen viewport. */
data class CalibrationRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * Kodi-style overscan calibration.
 *
 * First choose a uniform scale, which creates a centered rectangle. Then expand
 * each of the four edges independently. An edge action never translates the
 * whole rectangle: it only moves that one edge outward toward the display edge.
 */
data class DisplayCalibration(
    val interfaceScalePercent: Int = 100,
    val interfaceLeftExpandSteps: Int = 0,
    val interfaceRightExpandSteps: Int = 0,
    val interfaceTopExpandSteps: Int = 0,
    val interfaceBottomExpandSteps: Int = 0,
    val videoScalePercent: Int = 100,
    val videoLeftExpandSteps: Int = 0,
    val videoRightExpandSteps: Int = 0,
    val videoTopExpandSteps: Int = 0,
    val videoBottomExpandSteps: Int = 0,
) {
    fun normalized(): DisplayCalibration {
        val uiScale = interfaceScalePercent.coerceIn(MIN_SCALE_PERCENT, MAX_SCALE_PERCENT)
        val videoScale = videoScalePercent.coerceIn(MIN_SCALE_PERCENT, MAX_SCALE_PERCENT)
        return copy(
            interfaceScalePercent = uiScale,
            interfaceLeftExpandSteps = interfaceLeftExpandSteps.coerceIn(0, MAX_EXPAND_STEPS(uiScale)),
            interfaceRightExpandSteps = interfaceRightExpandSteps.coerceIn(0, MAX_EXPAND_STEPS(uiScale)),
            interfaceTopExpandSteps = interfaceTopExpandSteps.coerceIn(0, MAX_EXPAND_STEPS(uiScale)),
            interfaceBottomExpandSteps = interfaceBottomExpandSteps.coerceIn(0, MAX_EXPAND_STEPS(uiScale)),
            videoScalePercent = videoScale,
            videoLeftExpandSteps = videoLeftExpandSteps.coerceIn(0, MAX_EXPAND_STEPS(videoScale)),
            videoRightExpandSteps = videoRightExpandSteps.coerceIn(0, MAX_EXPAND_STEPS(videoScale)),
            videoTopExpandSteps = videoTopExpandSteps.coerceIn(0, MAX_EXPAND_STEPS(videoScale)),
            videoBottomExpandSteps = videoBottomExpandSteps.coerceIn(0, MAX_EXPAND_STEPS(videoScale)),
        )
    }

    /** Returns the exact edge positions previewed on the full-screen calibration target. */
    fun bounds(target: CalibrationTarget, widthPx: Float, heightPx: Float): CalibrationRect {
        val c = normalized()
        val scale: Int
        val leftSteps: Int
        val rightSteps: Int
        val topSteps: Int
        val bottomSteps: Int
        if (target == CalibrationTarget.INTERFACE) {
            scale = c.interfaceScalePercent
            leftSteps = c.interfaceLeftExpandSteps
            rightSteps = c.interfaceRightExpandSteps
            topSteps = c.interfaceTopExpandSteps
            bottomSteps = c.interfaceBottomExpandSteps
        } else {
            scale = c.videoScalePercent
            leftSteps = c.videoLeftExpandSteps
            rightSteps = c.videoRightExpandSteps
            topSteps = c.videoTopExpandSteps
            bottomSteps = c.videoBottomExpandSteps
        }

        val centeredInset = (MAX_SCALE_PERCENT - scale) / 200f
        val step = EDGE_STEP_SCREEN_PERCENT
        return CalibrationRect(
            left = widthPx * (centeredInset - leftSteps * step),
            top = heightPx * (centeredInset - topSteps * step),
            right = widthPx * (1f - centeredInset + rightSteps * step),
            bottom = heightPx * (1f - centeredInset + bottomSteps * step),
        )
    }

    companion object {
        const val MIN_SCALE_PERCENT = 70
        const val MAX_SCALE_PERCENT = 100
        const val SCALE_STEP_PERCENT = 5
        /** Each edge-button press extends just that edge by 0.5% of the screen dimension. */
        const val EDGE_STEP_SCREEN_PERCENT = 0.005f
        const val EDGE_STEP_LABEL_PERCENT = 0.5f

        /** The centered inset on each side is half the unused percentage. */
        fun MAX_EXPAND_STEPS(scalePercent: Int): Int =
            (MAX_SCALE_PERCENT - scalePercent.coerceIn(MIN_SCALE_PERCENT, MAX_SCALE_PERCENT))
    }
}
