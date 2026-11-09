package org.kinesis.logger.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class SensorRecorder(
    private val context: Context,
    private val writer: SessionWriter
) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val isRecording = AtomicBoolean(false)

    var startUtcMs: Long = 0L
        private set
    var startElapsedNs: Long = 0L
        private set
    var bootOffsetMs: Long = 0L
        private set

    val lastOsStepCumulative = AtomicLong(-1L)
    val initialOsStepCount = AtomicLong(-1L)
    val osStepEventCount = AtomicLong(0L)
    val lastSensorEventMs = AtomicLong(0L)

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                recordLocation(location, "fused")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (!isRecording.compareAndSet(false, true)) return

        startUtcMs = System.currentTimeMillis()
        startElapsedNs = SystemClock.elapsedRealtimeNanos()
        bootOffsetMs = startUtcMs - (startElapsedNs / 1_000_000L)

        writer.start()
        writer.offerEvent(
            EventSample(
                tNs = startElapsedNs,
                type = "tap_start",
                note = "boot_offset_ms=$bootOffsetMs"
            )
        )

        // 100 Hz sampling period = 10,000 microseconds
        val samplingPeriodUs = 10_000

        accelSensor?.let {
            sensorManager.registerListener(this, it, samplingPeriodUs)
        }
        gyroSensor?.let {
            sensorManager.registerListener(this, it, samplingPeriodUs)
        }
        stepDetectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Fused Location Provider at 1 s
        val fusedRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        try {
            fusedClient.requestLocationUpdates(fusedRequest, fusedCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // LocationManager GPS_PROVIDER at 1 s
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    this,
                    Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun logEvent(type: String, note: String = "") {
        val tNs = SystemClock.elapsedRealtimeNanos()
        writer.offerEvent(EventSample(tNs = tNs, type = type, note = note))
    }

    fun stopRecording() {
        if (!isRecording.compareAndSet(true, false)) return

        val stopElapsedNs = SystemClock.elapsedRealtimeNanos()
        writer.offerEvent(EventSample(tNs = stopElapsedNs, type = "tap_stop", note = ""))

        sensorManager.unregisterListener(this)

        try {
            fusedClient.removeLocationUpdates(fusedCallback)
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        writer.stopAndClose()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isRecording.get()) return

        lastSensorEventMs.set(System.currentTimeMillis())
        val tNs = event.timestamp

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                writer.offerAccel(
                    AccelSample(
                        tNs = tNs,
                        x = event.values[0],
                        y = event.values[1],
                        z = event.values[2],
                        accuracy = event.accuracy
                    )
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                writer.offerGyro(
                    GyroSample(
                        tNs = tNs,
                        x = event.values[0],
                        y = event.values[1],
                        z = event.values[2],
                        accuracy = event.accuracy
                    )
                )
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                osStepEventCount.incrementAndGet()
                writer.offerStepDetector(StepDetectorSample(tNs = tNs))
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val count = event.values[0].toLong()
                if (initialOsStepCount.get() == -1L) {
                    initialOsStepCount.set(count)
                }
                lastOsStepCumulative.set(count)
                writer.offerStepCounter(StepCounterSample(tNs = tNs, cumulative = count))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handled within samples
    }

    private fun recordLocation(location: Location, providerOverride: String? = null) {
        if (!isRecording.get()) return

        val tNs = location.elapsedRealtimeNanos
        val utcMs = location.time
        val provider = providerOverride ?: location.provider ?: "unknown"

        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (location.isMock) 1 else 0
        } else {
            @Suppress("DEPRECATION")
            if (location.isFromMockProvider) 1 else 0
        }

        val speedAcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
            location.speedAccuracyMetersPerSecond
        } else {
            -1f
        }

        val bearingAcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy()) {
            location.bearingAccuracyDegrees
        } else {
            -1f
        }

        lastSensorEventMs.set(System.currentTimeMillis())
        try {
            org.kinesis.logger.service.RecordingService.updateHudGps(location.accuracy, provider)
        } catch (_: Exception) {}

        writer.offerLocation(
            LocationSample(
                tNs = tNs,
                utcMs = utcMs,
                provider = provider,
                lat = location.latitude,
                lon = location.longitude,
                alt = location.altitude,
                accuracy = location.accuracy,
                speed = location.speed,
                speedAcc = speedAcc,
                bearing = location.bearing,
                bearingAcc = bearingAcc,
                isMock = isMock
            )
        )
    }

    override fun onLocationChanged(location: Location) {
        recordLocation(location, "gps_provider")
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
        logEvent("permission_change", "provider_enabled=$provider")
    }

    override fun onProviderDisabled(provider: String) {
        logEvent("permission_change", "provider_disabled=$provider")
    }
}
