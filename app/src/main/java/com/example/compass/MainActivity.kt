package com.example.compass

import android.Manifest
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.core.content.ContextCompat
import java.lang.Math.toRadians
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isGpsEnabled by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } else {
            Toast.makeText(
                context,
                "Location permission is required to use the compass",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        if (ContextCompat.checkSelfPermission(
                context,
                locationPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasLocationPermission = true
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } else {
            launcher.launch(locationPermission)
        }
        onDispose { }
    }

    if (hasLocationPermission) {
        if (isGpsEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CompassScreen()
            }
        } else {
            Toast.makeText(context, "Please enable GPS", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    } else {
        Text(
            text = "Location permission is required to use the compass",
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun CompassScreen() {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    var azimuth by remember { mutableStateOf(0f) }
    val sensorEventListener = remember {
        object : SensorEventListener {
            var gravity = FloatArray(3)
            var geomagnetic = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
                    Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
                }

                val rotationMatrix = FloatArray(9)
                val inclinationMatrix = FloatArray(9)
                if (SensorManager.getRotationMatrix(
                        rotationMatrix,
                        inclinationMatrix,
                        gravity,
                        geomagnetic
                    )
                ) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(sensorManager) {
        sensorManager.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            sensorEventListener,
            magnetometer,
            SensorManager.SENSOR_DELAY_UI
        )
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    Compass(angle = azimuth)
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
            .background(Color.Black, shape = CircleShape)
    ) {
        CompassCanvas(primaryAngle)
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
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompassCanvas(primaryAngle: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2

        drawCircle(
            color = Color.DarkGray,
            radius = radius,
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )

        for (i in 0..359 step 5) {
            val angleInRad = toRadians(i.toDouble())
            val lineLength = if (i % 30 == 0) 18.dp.toPx() else 8.dp.toPx()
            val strokeWidth = if (i % 30 == 0) 3.dp.toPx() else 2.dp.toPx()

            val startRadius = radius - lineLength - 1.dp.toPx()
            val endRadius = radius - 1.dp.toPx()

            val startX = centerX + startRadius * cos(angleInRad)
            val startY = centerY + startRadius * sin(angleInRad)
            val endX = centerX + endRadius * cos(angleInRad)
            val endY = centerY + endRadius * sin(angleInRad)

            drawLine(
                color = if (isAngleBetween(
                        i.toFloat(),
                        primaryAngle,
                        angleTolerance = 2.5f
                    )
                ) Color.Red else Color.DarkGray,
                start = Offset(startX.toFloat(), startY.toFloat()),
                end = Offset(endX.toFloat(), endY.toFloat()),
                strokeWidth = strokeWidth
            )

            if (i % 30 == 0) {
                val textRadius = radius - 34.dp.toPx()
                val textX = centerX + textRadius * cos(angleInRad) - 10.dp.toPx()
                val textY = centerY + textRadius * sin(angleInRad) + 8.dp.toPx()

                drawContext.canvas.nativeCanvas.drawText(
                    "$i°",
                    textX.toFloat(),
                    textY.toFloat(),
                    android.graphics.Paint().apply {
                        textSize = 14.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        color = if (isAngleBetween(
                                i.toFloat(),
                                primaryAngle,
                                angleTolerance = 15f
                            )
                        ) Color.Red.toArgb() else Color.LightGray.toArgb()
                    }
                )
            }
        }
    }
}

fun isAngleBetween(angle1: Float, angle2: Float, angleTolerance: Float): Boolean {
    val diff = Math.abs(angle1 - angle2)
    return diff <= angleTolerance || diff >= 360 - angleTolerance
}
