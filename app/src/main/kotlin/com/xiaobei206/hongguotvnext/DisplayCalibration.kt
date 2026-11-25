package com.xiaobei206.hongguotvnext

/**
 * Persistent display calibration for older TVs with overscan or unusual picture sizing.
 *
 * Interface calibration transforms Compose UI. Video calibration transforms only the
 * TextureView that renders the video, leaving controls and diagnostics in place.
 */
data class DisplayCalibration(
    val interfaceScalePercent: Int = 100,
    val interfaceOffsetXDp: Int = 0,
    val interfaceOffsetYDp: Int = 0,
    val videoScalePercent: Int = 100,
    val videoOffsetXDp: Int = 0,
    val videoOffsetYDp: Int = 0,
) {
    fun normalized(): DisplayCalibration = copy(
        interfaceScalePercent = interfaceScalePercent.coerceIn(INTERFACE_SCALE_MIN, INTERFACE_SCALE_MAX),
        interfaceOffsetXDp = interfaceOffsetXDp.coerceIn(-INTERFACE_OFFSET_LIMIT_DP, INTERFACE_OFFSET_LIMIT_DP),
        interfaceOffsetYDp = interfaceOffsetYDp.coerceIn(-INTERFACE_OFFSET_LIMIT_DP, INTERFACE_OFFSET_LIMIT_DP),
        videoScalePercent = videoScalePercent.coerceIn(VIDEO_SCALE_MIN, VIDEO_SCALE_MAX),
        videoOffsetXDp = videoOffsetXDp.coerceIn(-VIDEO_OFFSET_LIMIT_DP, VIDEO_OFFSET_LIMIT_DP),
        videoOffsetYDp = videoOffsetYDp.coerceIn(-VIDEO_OFFSET_LIMIT_DP, VIDEO_OFFSET_LIMIT_DP),
    )

    companion object {
        const val INTERFACE_SCALE_MIN = 70
        const val INTERFACE_SCALE_MAX = 110
        const val INTERFACE_OFFSET_LIMIT_DP = 120
        const val VIDEO_SCALE_MIN = 70
        const val VIDEO_SCALE_MAX = 160
        const val VIDEO_OFFSET_LIMIT_DP = 320
        const val SCALE_STEP_PERCENT = 5
        const val OFFSET_STEP_DP = 8
    }
}
