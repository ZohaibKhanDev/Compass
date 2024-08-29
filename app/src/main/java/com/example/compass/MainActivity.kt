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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }
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
                text = "12:04",
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(300.dp)
            .graphicsLayer { rotationZ = -angle }
            .background(Color.DarkGray, shape = CircleShape)
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2


            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )


            for (i in 0..359 step 10) {
                val angleInRad = Math.toRadians(i.toDouble())
                val lineLength = if (i % 30 == 0) 16.dp.toPx() else 8.dp.toPx()
                val startX = centerX + (radius - lineLength) * kotlin.math.cos(angleInRad)
                val startY = centerY + (radius - lineLength) * kotlin.math.sin(angleInRad)
                val endX = centerX + radius * kotlin.math.cos(angleInRad)
                val endY = centerY + radius * kotlin.math.sin(angleInRad)

                drawLine(
                    color = Color.White,
                    start = Offset(startX.toFloat(), startY.toFloat()),
                    end = Offset(endX.toFloat(), endY.toFloat()),
                    strokeWidth = 2.dp.toPx()
                )


                if (i % 30 == 0) {
                    val textRadius = radius - 30.dp.toPx()
                    val textX = centerX + textRadius * kotlin.math.cos(angleInRad) - 10.dp.toPx()
                    val textY = centerY + textRadius * kotlin.math.sin(angleInRad) + 8.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        "$i°",
                        textX.toFloat(),
                        textY.toFloat(),
                        android.graphics.Paint().apply {
                            textSize = 16.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            color = Color.White.toArgb()
                        }
                    )
                }
            }
        }


        Canvas(
            modifier = Modifier
                .size(12.dp, 90.dp)
        ) {
            val path = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(0f, size.height * 0.2f)
                lineTo(size.width / 2, size.height * 0.8f)
                lineTo(size.width, size.height * 0.2f)
                close()
            }
            drawPath(
                path = path,
                color = Color.Red,
            )
        }


        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "North",
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${angle.toInt()}°",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
