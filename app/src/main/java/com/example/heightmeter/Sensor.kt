package com.example.heightmeter

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


@Composable
fun <T> rememberSensorValueAsState(
    type: Int,
    transformSensorEvent: (event: SensorEvent?) -> T,
): State<T> {
    val context = LocalContext.current
    val sensorEventCallbackFlow = remember {
        sensorEventCallbackFlow(
            context = context,
            type = type
        )
    }
    val sensorEvent by sensorEventCallbackFlow.collectAsStateWithLifecycle(
        initialValue = ComposableSensorEvent(),
        minActiveState = Lifecycle.State.RESUMED,
    )
    return remember { derivedStateOf { transformSensorEvent(sensorEvent.event) } }
}

internal fun sensorEventCallbackFlow(
    context: Context,
    type: Int,
): Flow<ComposableSensorEvent> = callbackFlow {
    val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
        ?: throw RuntimeException("SensorManager is null")

    val sensor = sensorManager.getDefaultSensor(type)
        ?: throw RuntimeException("Sensor of type $type is not available, use one of the isSensorAvailable functions")

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val composableEvent = ComposableSensorEvent(event = event)
            trySend(composableEvent)
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // TODO: Handle sensor accuracy changes?
        }
    }

    val successful = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    if (!successful) throw RuntimeException("Failed to register listener for sensor ${sensor.name}")
    awaitClose { sensorManager.unregisterListener(listener) }
}

internal data class ComposableSensorEvent(
    val event: SensorEvent? = null,
    val timestamp: Long = event?.timestamp ?: SystemClock.elapsedRealtimeNanos(),
)