package com.xiaobei206.hongguotvnext

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

private val CalibrationUiOutside = Color(0xFFFFD600)
private val CalibrationUiInside = Color(0xFF0066FF)
private val CalibrationVideoOutside = Color(0xFFF50057)
private val CalibrationVideoInside = Color(0xFF00D8A0)
private val CalibrationFocus = Color(0xFFF0B65A)
private val CalibrationMarker = Color(0xFFFF2D00)
private val CalibrationFocusBorder = Color(0xFFFFF3DD)
private val CalibrationInk = Color(0xFF101820)

@Composable
internal fun DisplayCalibrationScreen(
    target: CalibrationTarget,
    calibration: DisplayCalibration,
    onCalibrationChange: (DisplayCalibration) -> Unit,
    onBack: () -> Unit,
) {
    val current = calibration.normalized()
    val outside = if (target == CalibrationTarget.INTERFACE) CalibrationUiOutside else CalibrationVideoOutside
    val inside = if (target == CalibrationTarget.INTERFACE) CalibrationUiInside else CalibrationVideoInside
    val scale = if (target == CalibrationTarget.INTERFACE) current.interfaceScalePercent else current.videoScalePercent
    val left = if (target == CalibrationTarget.INTERFACE) current.interfaceLeftExpandSteps else current.videoLeftExpandSteps
    val right = if (target == CalibrationTarget.INTERFACE) current.interfaceRightExpandSteps else current.videoRightExpandSteps
    val top = if (target == CalibrationTarget.INTERFACE) current.interfaceTopExpandSteps else current.videoTopExpandSteps
    val bottom = if (target == CalibrationTarget.INTERFACE) current.interfaceBottomExpandSteps else current.videoBottomExpandSteps
    val maxEdgeSteps = DisplayCalibration.MAX_EXPAND_STEPS(scale)

    fun changeScale(value: Int) {
        val bounded = value.coerceIn(DisplayCalibration.MIN_SCALE_PERCENT, DisplayCalibration.MAX_SCALE_PERCENT)
        val next = if (target == CalibrationTarget.INTERFACE) {
            current.copy(interfaceScalePercent = bounded)
        } else {
            current.copy(videoScalePercent = bounded)
        }
        onCalibrationChange(next.normalized())
    }

    fun changeEdge(edge: String) {
        val next = when (target) {
            CalibrationTarget.INTERFACE -> current.copy(
                interfaceLeftExpandSteps = if (edge == "left") (left + 1).coerceAtMost(maxEdgeSteps) else left,
                interfaceRightExpandSteps = if (edge == "right") (right + 1).coerceAtMost(maxEdgeSteps) else right,
                interfaceTopExpandSteps = if (edge == "top") (top + 1).coerceAtMost(maxEdgeSteps) else top,
                interfaceBottomExpandSteps = if (edge == "bottom") (bottom + 1).coerceAtMost(maxEdgeSteps) else bottom,
            )
            CalibrationTarget.VIDEO -> current.copy(
                videoLeftExpandSteps = if (edge == "left") (left + 1).coerceAtMost(maxEdgeSteps) else left,
                videoRightExpandSteps = if (edge == "right") (right + 1).coerceAtMost(maxEdgeSteps) else right,
                videoTopExpandSteps = if (edge == "top") (top + 1).coerceAtMost(maxEdgeSteps) else top,
                videoBottomExpandSteps = if (edge == "bottom") (bottom + 1).coerceAtMost(maxEdgeSteps) else bottom,
            )
        }
        onCalibrationChange(next.normalized())
    }

    fun resetCurrentTarget() {
        val next = if (target == CalibrationTarget.INTERFACE) {
            current.copy(
                interfaceScalePercent = 100,
                interfaceLeftExpandSteps = 0,
                interfaceRightExpandSteps = 0,
                interfaceTopExpandSteps = 0,
                interfaceBottomExpandSteps = 0,
            )
        } else {
            current.copy(
                videoScalePercent = 100,
                videoLeftExpandSteps = 0,
                videoRightExpandSteps = 0,
                videoTopExpandSteps = 0,
                videoBottomExpandSteps = 0,
            )
        }
        onCalibrationChange(next.normalized())
    }

    Box(Modifier.fillMaxSize().background(outside).clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(outside)
            val rect = current.bounds(target, size.width, size.height)
            val frameWidth = (rect.right - rect.left).coerceAtLeast(1f)
            val frameHeight = (rect.bottom - rect.top).coerceAtLeast(1f)
            val origin = Offset(rect.left, rect.top)
            val frameSize = Size(frameWidth, frameHeight)

            // Full-bleed high-contrast colours make the four boundaries easy to distinguish.
            drawRect(inside, topLeft = origin, size = frameSize)

            // Grid, centre cross and circle make any non-uniform stretching obvious.
            for (i in 1..9) {
                val x = rect.left + frameWidth * i / 10f
                val y = rect.top + frameHeight * i / 10f
                drawLine(Color(0xAAFFFFFF), Offset(x, rect.top), Offset(x, rect.bottom), strokeWidth = 1.5.dp.toPx())
                drawLine(Color(0xAAFFFFFF), Offset(rect.left, y), Offset(rect.right, y), strokeWidth = 1.5.dp.toPx())
            }
            drawCircle(
                color = Color.White,
                radius = minOf(frameWidth, frameHeight) * 0.12f,
                center = Offset((rect.left + rect.right) / 2f, (rect.top + rect.bottom) / 2f),
                style = Stroke(width = 4.dp.toPx()),
            )
            drawLine(Color.White, Offset((rect.left + rect.right) / 2f, rect.top),
                Offset((rect.left + rect.right) / 2f, rect.bottom), strokeWidth = 3.dp.toPx())
            drawLine(Color.White, Offset(rect.left, (rect.top + rect.bottom) / 2f),
                Offset(rect.right, (rect.top + rect.bottom) / 2f), strokeWidth = 3.dp.toPx())

            // Double frame and red corner markers remain visible against either colour.
            drawRect(Color.Black, topLeft = origin, size = frameSize, style = Stroke(width = 10.dp.toPx()))
            drawRect(Color.White, topLeft = origin, size = frameSize, style = Stroke(width = 4.dp.toPx()))
            val corner = minOf(frameWidth, frameHeight) * 0.035f
            listOf(
                Offset(rect.left, rect.top) to Offset(rect.left + corner, rect.top),
                Offset(rect.left, rect.top) to Offset(rect.left, rect.top + corner),
                Offset(rect.right, rect.top) to Offset(rect.right - corner, rect.top),
                Offset(rect.right, rect.top) to Offset(rect.right, rect.top + corner),
                Offset(rect.left, rect.bottom) to Offset(rect.left + corner, rect.bottom),
                Offset(rect.left, rect.bottom) to Offset(rect.left, rect.bottom - corner),
                Offset(rect.right, rect.bottom) to Offset(rect.right - corner, rect.bottom),
                Offset(rect.right, rect.bottom) to Offset(rect.right, rect.bottom - corner),
            ).forEach { (start, end) ->
                drawLine(CalibrationMarker, start, end, strokeWidth = 8.dp.toPx())
            }
        }

        // The panel sits in the centre so it does not cover the four screen-edge markers.
        Surface(
            modifier = Modifier.align(Alignment.Center).widthIn(max = 900.dp).fillMaxWidth().padding(12.dp),
            color = Color(0xF4FFFFFF),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(4.dp, CalibrationInk),
            shadowElevation = 12.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(target.title, color = CalibrationInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "先等比例缩小到合适尺寸，再分别向外扩展四条边。每次只动指定的一条边。",
                    color = CalibrationInk, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CalibrationActionButton(
                        label = "− 等比例缩小", enabled = scale > DisplayCalibration.MIN_SCALE_PERCENT,
                        onClick = { changeScale(scale - DisplayCalibration.SCALE_STEP_PERCENT) },
                        modifier = Modifier.width(205.dp),
                    )
                    Text("$scale%", modifier = Modifier.width(90.dp), color = CalibrationFocus,
                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    CalibrationActionButton(
                        label = "+ 等比例放大", enabled = scale < DisplayCalibration.MAX_SCALE_PERCENT,
                        onClick = { changeScale(scale + DisplayCalibration.SCALE_STEP_PERCENT) },
                        modifier = Modifier.width(205.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CalibrationActionButton(
                        label = "↑ 只扩展上边  +${formatEdgePercent(top)}",
                        enabled = top < maxEdgeSteps, onClick = { changeEdge("top") },
                        modifier = Modifier.width(330.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CalibrationActionButton(
                            label = "← 只扩展左边  +${formatEdgePercent(left)}",
                            enabled = left < maxEdgeSteps, onClick = { changeEdge("left") },
                            modifier = Modifier.width(260.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        CalibrationActionButton(
                            label = "→ 只扩展右边  +${formatEdgePercent(right)}",
                            enabled = right < maxEdgeSteps, onClick = { changeEdge("right") },
                            modifier = Modifier.width(260.dp),
                        )
                    }
                    CalibrationActionButton(
                        label = "↓ 只扩展下边  +${formatEdgePercent(bottom)}",
                        enabled = bottom < maxEdgeSteps, onClick = { changeEdge("bottom") },
                        modifier = Modifier.width(330.dp),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CalibrationActionButton(
                        label = "恢复本项默认", onClick = ::resetCurrentTarget,
                        modifier = Modifier.width(210.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    CalibrationActionButton(
                        label = "返回设置", onClick = onBack,
                        modifier = Modifier.width(210.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationActionButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.height(50.dp)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                val emphasis = if (focused) 1.035f else 1f
                scaleX = emphasis
                scaleY = emphasis
            }
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled = enabled),
        color = when {
            !enabled -> Color(0xFFD5D8DC)
            focused -> CalibrationFocus
            else -> Color.White
        },
        shape = RoundedCornerShape(9.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (focused) 5.dp else 2.dp,
            color = if (focused) CalibrationFocusBorder else CalibrationInk,
        ),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = when {
                    !enabled -> Color(0xFF66717D)
                    focused -> CalibrationInk
                    else -> CalibrationInk
                },
                fontSize = if (focused) 17.sp else 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

private fun formatEdgePercent(steps: Int): String =
    if (steps % 2 == 0) "${steps / 2}.0%" else "${steps / 2}.5%"
