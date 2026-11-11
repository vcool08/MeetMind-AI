package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.ElectricCyanAccent

@Composable
fun WaveformVisualizer(
  amplitudes: List<Float>,
  isRecording: Boolean,
  modifier: Modifier = Modifier,
  height: Dp = 96.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
  ) {
    val barCount = amplitudes.size.coerceAtLeast(1)
    val totalWidth = size.width
    val canvasHeight = size.height
    val centerY = canvasHeight / 2f
    val spacing = 4.dp.toPx()
    val availableWidth = totalWidth - (barCount - 1) * spacing
    val barWidth = (availableWidth / barCount).coerceIn(3.dp.toPx(), 12.dp.toPx())

    val gradient = Brush.verticalGradient(
      colors = listOf(
        ElectricCyanAccent,
        ElectricBluePrimary,
        ElectricCyanAccent
      )
    )

    amplitudes.forEachIndexed { index, rawAmp ->
      val amp = if (isRecording) (rawAmp * pulseScale).coerceIn(0.1f, 1.0f) else rawAmp * 0.4f
      val barHeight = (canvasHeight * amp).coerceAtLeast(6.dp.toPx())
      val x = index * (barWidth + spacing)
      val y = centerY - (barHeight / 2f)

      drawRoundRect(
        brush = gradient,
        topLeft = Offset(x, y),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
      )
    }
  }
}
