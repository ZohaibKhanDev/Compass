package com.example.compass

import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompassScreen()
        }
    }
}

@Composable
fun CompassScreen() {
    var angle by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner) {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val alpha = 0.9f
                    angle = alpha * angle + (1 - alpha) * event.values[0]
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                orientationSensor?.let {
                    sensorManager.registerListener(
                        sensorEventListener,
                        it,
                        SensorManager.SENSOR_DELAY_GAME
                    )
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                sensorManager.unregisterListener(sensorEventListener)
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // Set the background color of the entire screen to black
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "00:00",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 20.dp)
            )

            Compass(angle)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NL",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Text(
                    text = "EL",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun Compass(angle: Float) {
    val primaryAngle = (angle + 360).mod(360.0).toFloat()
    var directionText by remember { mutableStateOf("North") }

    when (primaryAngle) {
        in 22.5..67.5 -> directionText = "Northeast"
        in 67.5..112.5 -> directionText = "East"
        in 112.5..157.5 -> directionText = "Southeast"
        in 157.5..202.5 -> directionText = "South"
        in 202.5..247.5 -> directionText = "Southwest"
        in 247.5..292.5 -> directionText = "West"
        in 292.5..337.5 -> directionText = "Northwest"
        else -> directionText = "North"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(300.dp)
            .graphicsLayer { rotationZ = -angle }
            .background(Color.Black, shape = CircleShape) // Set the background of the circle to black
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2

            // Draw the outer circle
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )

            // Draw degree markings and direction labels
            for (i in 0..359 step 5) {
                val angleInRad = toRadians(i.toDouble())
                val lineLength =
                    if (i % 30 == 0) 18.dp.toPx() else 8.dp.toPx()
                val strokeWidth = if (i % 30 == 0) 3.dp.toPx() else 2.dp.toPx()

                // 1 dp padding between lines and circle
                val startRadius = radius - lineLength - 1.dp.toPx()
                val endRadius = radius - 1.dp.toPx()

                val startX = centerX + startRadius * cos(angleInRad)
                val startY = centerY + startRadius * sin(angleInRad)
                val endX = centerX + endRadius * cos(angleInRad)
                val endY = centerY + endRadius * sin(angleInRad)

                drawLine(
                    color = if (isAngleBetween(i.toFloat(), primaryAngle, angleTolerance = 2.5f)) Color.Red else Color.White,
                    start = Offset(startX.toFloat(), startY.toFloat()),
                    end = Offset(endX.toFloat(), endY.toFloat()),
                    strokeWidth = strokeWidth
                )

                // Add degree text for major markings
                if (i % 30 == 0) {
                    val textRadius = radius - 34.dp.toPx() // Adjusted for padding
                    val textX = centerX + textRadius * cos(angleInRad) - 10.dp.toPx()
                    val textY = centerY + textRadius * sin(angleInRad) + 8.dp.toPx()

                    drawContext.canvas.nativeCanvas.drawText(
                        "$i°",
                        textX.toFloat(),
                        textY.toFloat(),
                        android.graphics.Paint().apply {
                            textSize = 14.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            color = if (isAngleBetween(i.toFloat(), primaryAngle, angleTolerance = 15f)) Color.Red.toArgb() else Color.White.toArgb()
                        }
                    )
                }
            }
        }

        // Center Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${primaryAngle.toInt()}°",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = directionText,
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun isAngleBetween(target: Float, angle: Float, angleTolerance: Float): Boolean {
    val minAngle = (angle - angleTolerance + 360) % 360
    val maxAngle = (angle + angleTolerance) % 360
    return (target >= minAngle && target <= maxAngle) ||
            (maxAngle < minAngle && (target >= minAngle || target <= maxAngle))
}