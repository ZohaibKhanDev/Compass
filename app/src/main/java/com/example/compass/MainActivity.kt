package com.example.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    SensorListener(onDegreeChanged = { degree ->
        currentDegree = degree
    })

    LaunchedEffect(currentDegree) {
        smoothCurrentDegree = lowPassFilter(smoothCurrentDegree, currentDegree, 0.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222222))
    ) {
        CompassScreen(currentDegree = smoothCurrentDegree)
    }
}

@Composable
fun CompassScreen(currentDegree: Float) {
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
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2

            drawCircle(
                color = Color.LightGray,
                radius = radius,
                style = Stroke(width = 4.dp.toPx())
            )


            for (degree in 0..359 step 5) {
                val angleInRad = Math.toRadians(degree.toDouble() - currentDegree).toFloat()
                val lineLength = when {
                    degree % 90 == 0 -> 0.2f
                    degree % 30 == 0 -> 0.15f
                    else -> 0.1f
                }

                val startX = centerX + (radius * (1 - lineLength)) * cos(angleInRad)
                val startY = centerY + (radius * (1 - lineLength)) * sin(angleInRad)
                val endX = centerX + radius * cos(angleInRad)
                val endY = centerY + radius * sin(angleInRad)

                drawLine(
                    color = if (degree in (currentDegree.toInt() - 2)..(currentDegree.toInt() + 2)) Color.Red else Color.LightGray,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (degree % 30 == 0) 2.dp.toPx() else 1.dp.toPx()
                )

                if (degree % 30 == 0) {
                    val textRadius = radius * 0.7f
                    val textX = centerX + (textRadius * cos(angleInRad))
                    val textY = centerY + (textRadius * sin(angleInRad))
                    val degreeText = degree.toString()
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val textWidth = textPaint.measureText(degreeText)


                    drawContext.canvas.nativeCanvas.rotate(
                        degree.toFloat() - currentDegree.toFloat(),
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
                        -(degree.toFloat() - currentDegree.toFloat()),
                        textX,
                        textY
                    )
                }
            }

            val cardinalDirections = listOf("N", "E", "S", "W")
            for ((index, direction) in cardinalDirections.withIndex()) {
                val angleInRad = Math.toRadians((index * 90).toDouble() - currentDegree).toFloat()
                val textRadius = radius * 0.8f // Position further from the center
                val textX = centerX + (textRadius * cos(angleInRad))
                val textY = centerY + (textRadius * sin(angleInRad))

                drawContext.canvas.nativeCanvas.drawText(
                    direction,
                    textX,
                    textY,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 36f  // Larger font size
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                )
            }

            rotate(currentDegree) {
                val needlePath = Path().apply {
                    moveTo(centerX, centerY - radius * 0.6f)
                    lineTo(centerX - 10f, centerY)
                    lineTo(centerX + 10f, centerY)
                    close()
                }
                drawPath(
                    path = needlePath,
                    color = Color.Red
                )
            }

            drawContext.canvas.nativeCanvas.drawText(
                "${currentDegree.toInt()}°",
                centerX,
                centerY - radius * 0.3f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 50f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            )
        }
    }
}

@Composable
fun SensorListener(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
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
                val rotationMatrix = FloatArray(9)
                if (SensorManager.getRotationMatrix(
                        rotationMatrix,
                        null,
                        gravity,
                        geomagnetic
                    )
                ) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthDeg =
                        (Math.toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360
                    coroutineScope.launch(Dispatchers.Main) {
                        onDegreeChanged(azimuthDeg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }
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
        in 0..22 -> "N"
        in 23..67 -> "NE"
        in 68..112 -> "E"
        in 113..157 -> "SE"
        in 158..202 -> "S"
        in 203..247 -> "SW"
        in 248..292 -> "W"
        in 293..337 -> "NW"
        else -> "N"
    }
}