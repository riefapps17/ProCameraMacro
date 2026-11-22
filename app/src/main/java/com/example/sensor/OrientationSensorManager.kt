package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2

class OrientationSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _rollAngle = MutableStateFlow(0f)
    val rollAngle: StateFlow<Float> = _rollAngle

    private val _pitchAngle = MutableStateFlow(0f)
    val pitchAngle: StateFlow<Float> = _pitchAngle

    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // Calculate Roll angle in degrees
        val roll = Math.toDegrees(atan2(ax.toDouble(), ay.toDouble())).toFloat()
        // Calculate Pitch angle
        val pitch = Math.toDegrees(atan2(-az.toDouble(), Math.sqrt((ax * ax + ay * ay).toDouble()))).toFloat()

        _rollAngle.value = roll
        _pitchAngle.value = pitch
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
