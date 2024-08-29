package com.example.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompassApp()
        }
    }
}

@Composable
fun CompassApp() {
    val context = LocalContext.current
    var currentDegree by remember { mutableStateOf(0f) }
    var smoothCurrentDegree by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val degreeAnimation = remember { Animatable(0f) }

    SensorListener(onDegreeChanged = { degree ->
        currentDegree = degree
        coroutineScope.launch {
            degreeAnimation.animateTo(
                targetValue = degree,
                animationSpec = tween(durationMillis = 500)
            )
        }
    })

    LaunchedEffect(currentDegree) {
        smoothCurrentDegree = lowPassFilter(smoothCurrentDegree, degreeAnimation.value, 0.1f)
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        CompassScreen(currentDegree = smoothCurrentDegree)
    }
}

@Composable
fun CompassScreen(currentDegree: Float) {
    val correctedDegree = currentDegree
    val directionText = getDirectionFromDegree(correctedDegree.toInt())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .rotate(-correctedDegree)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2

            drawCircle(
                color = Color(0xFF111111),
                radius = radius,
                style = Stroke(width = 8.dp.toPx())
            )

            for (degree in 0..359 step 5) {
                val angleInRad = Math.toRadians(degree.toDouble()).toFloat()
                val lineLength = when {
                    degree % 90 == 0 -> 0.25f
                    degree % 30 == 0 -> 0.20f
                    else -> 0.15f
                }

                val startX = centerX + (radius * (1 - lineLength)) * cos(angleInRad)
                val startY = centerY + (radius * (1 - lineLength)) * sin(angleInRad)
                val endX = centerX + radius * cos(angleInRad)
                val endY = centerY + radius * sin(angleInRad)


                val isNorth =
                    (degree == 0 && correctedDegree in 337.5..360.0) || (degree == 0 && correctedDegree in 0.0..22.5)
                val lineColor = if (isNorth) {
                    Color.Red
                } else if (degree % 90 == 0) {
                    Color.Red
                } else if (degree % 30 == 0) {
                    Color.White
                } else {
                    Color.White.copy(alpha = 0.7f)
                }

                drawLine(
                    color = lineColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (degree % 30 == 0) 3.dp.toPx() else 2.dp.toPx()
                )

                if (degree % 30 == 0) {
                    drawDegreeText(degree.toString(), angleInRad, centerX, centerY, radius)
                }
            }


            if (correctedDegree in 337.5..360.0 || correctedDegree in 0.0..22.5) {
                drawArrow(centerX, centerY - radius + 40.dp.toPx())
            }

            drawContext.canvas.nativeCanvas.drawText(
                "${correctedDegree.toInt()}°",
                centerX,
                centerY,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    textSize = 80f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(
                        "sans-serif-light",
                        android.graphics.Typeface.BOLD
                    )
                }
            )

            drawContext.canvas.nativeCanvas.drawText(
                directionText,
                centerX,
                centerY + 50f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    textSize = 45f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(
                        "sans-serif-light",
                        android.graphics.Typeface.NORMAL
                    )
                }
            )
        }
    }
}


private fun DrawScope.drawArrow(centerX: Float, centerY: Float) {
    val arrowSize = 30.dp.toPx()
    val arrowPath = android.graphics.Path().apply {
        moveTo(0f, 0f)
        lineTo(-arrowSize / 2, arrowSize)
        lineTo(arrowSize / 2, arrowSize)
        close()
    }

    drawContext.canvas.nativeCanvas.save()
    drawContext.canvas.nativeCanvas.translate(centerX, centerY)
    drawContext.canvas.nativeCanvas.drawPath(arrowPath, android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    })
    drawContext.canvas.nativeCanvas.restore()
}

private fun DrawScope.drawDegreeText(
    degreeText: String,
    angleInRad: Float,
    centerX: Float,
    centerY: Float,
    radius: Float
) {
    val textRadius = radius * 0.7f
    val textX = centerX + (textRadius * cos(angleInRad))
    val textY = centerY + (textRadius * sin(angleInRad))
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 35f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val textWidth = textPaint.measureText(degreeText)

    drawContext.canvas.nativeCanvas.rotate(
        Math.toDegrees(angleInRad.toDouble()).toFloat(),
        textX,
        textY
    )
    drawContext.canvas.nativeCanvas.drawText(
        degreeText,
        textX - textWidth / 2,
        textY + 10f,
        textPaint
    )
    drawContext.canvas.nativeCanvas.rotate(
        -Math.toDegrees(angleInRad.toDouble()).toFloat(),
        textX,
        textY
    )
}


@Composable
fun SensorListener(
    onDegreeChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val sensorEventListener = remember {
        object : SensorEventListener {
            private val gravity = FloatArray(3)
            private val geomagnetic = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                    }
                }
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    val azimuthDeg =
                        (Math.toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360
                    onDegreeChanged(azimuthDeg)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(sensorManager) {
        sensorManager.registerListener(
            sensorEventListener,
            magneticSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener(
            sensorEventListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
}

fun lowPassFilter(previousValue: Float, currentValue: Float, alpha: Float): Float {
    return previousValue + alpha * (currentValue - previousValue)
}

fun getDirectionFromDegree(degree: Int): String {
    return when (degree) {
        in 0..22 -> "North"
        in 23..67 -> "NE"
        in 68..112 -> "East"
        in 113..157 -> "SE"
        in 158..202 -> "South"
        in 203..247 -> "SW"
        in 248..292 -> "West"
        in 293..337 -> "NW"
        else -> "North"
    }
}