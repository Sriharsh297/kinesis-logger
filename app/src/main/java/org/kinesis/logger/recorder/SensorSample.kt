package org.kinesis.logger.recorder

sealed interface SensorSample

data class AccelSample(
    val tNs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val accuracy: Int
) : SensorSample

data class GyroSample(
    val tNs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val accuracy: Int
) : SensorSample

data class StepDetectorSample(
    val tNs: Long
) : SensorSample

data class StepCounterSample(
    val tNs: Long,
    val cumulative: Long
) : SensorSample

data class LocationSample(
    val tNs: Long,
    val utcMs: Long,
    val provider: String,
    val lat: Double,
    val lon: Double,
    val alt: Double,
    val accuracy: Float,
    val speed: Float,
    val speedAcc: Float,
    val bearing: Float,
    val bearingAcc: Float,
    val isMock: Int
) : SensorSample

data class EventSample(
    val tNs: Long,
    val type: String,
    val note: String
) : SensorSample
