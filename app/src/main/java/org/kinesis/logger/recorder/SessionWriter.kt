package org.kinesis.logger.recorder

import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class SessionWriter(
    val sessionDir: File,
    private val queueCapacity: Int = 10_000
) {
    private val accelQueue = ArrayBlockingQueue<AccelSample>(queueCapacity)
    private val gyroQueue = ArrayBlockingQueue<GyroSample>(queueCapacity)
    private val stepDetectorQueue = ArrayBlockingQueue<StepDetectorSample>(1_000)
    private val stepCounterQueue = ArrayBlockingQueue<StepCounterSample>(1_000)
    private val locationQueue = ArrayBlockingQueue<LocationSample>(1_000)
    private val eventQueue = ArrayBlockingQueue<EventSample>(1_000)

    val accelCount = AtomicLong(0)
    val gyroCount = AtomicLong(0)
    val stepDetectorCount = AtomicLong(0)
    val stepCounterCount = AtomicLong(0)
    val locationCount = AtomicLong(0)
    val eventCount = AtomicLong(0)

    val droppedAccel = AtomicLong(0)
    val droppedGyro = AtomicLong(0)
    val droppedStepDetector = AtomicLong(0)
    val droppedStepCounter = AtomicLong(0)
    val droppedLocation = AtomicLong(0)
    val droppedEvents = AtomicLong(0)

    private val isRunning = AtomicBoolean(false)
    private var writerThread: Thread? = null

    private lateinit var accelWriter: BufferedWriter
    private lateinit var gyroWriter: BufferedWriter
    private lateinit var stepDetectorWriter: BufferedWriter
    private lateinit var stepCounterWriter: BufferedWriter
    private lateinit var locationWriter: BufferedWriter
    private lateinit var eventWriter: BufferedWriter

    val accelFile = File(sessionDir, "accel.csv")
    val gyroFile = File(sessionDir, "gyro.csv")
    val stepDetectorFile = File(sessionDir, "step_detector.csv")
    val stepCounterFile = File(sessionDir, "step_counter.csv")
    val locationFile = File(sessionDir, "location.csv")
    val eventsFile = File(sessionDir, "events.csv")

    init {
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        initFiles()
    }

    private fun initFiles() {
        val isNewSession = !accelFile.exists()

        accelWriter = BufferedWriter(FileWriter(accelFile, true), 32 * 1024)
        gyroWriter = BufferedWriter(FileWriter(gyroFile, true), 32 * 1024)
        stepDetectorWriter = BufferedWriter(FileWriter(stepDetectorFile, true), 8 * 1024)
        stepCounterWriter = BufferedWriter(FileWriter(stepCounterFile, true), 8 * 1024)
        locationWriter = BufferedWriter(FileWriter(locationFile, true), 8 * 1024)
        eventWriter = BufferedWriter(FileWriter(eventsFile, true), 8 * 1024)

        if (isNewSession) {
            accelWriter.write("t_ns,x,y,z,accuracy\n")
            gyroWriter.write("t_ns,x,y,z,accuracy\n")
            stepDetectorWriter.write("t_ns\n")
            stepCounterWriter.write("t_ns,cumulative\n")
            locationWriter.write("t_ns,utc_ms,provider,lat,lon,alt,accuracy,speed,speed_acc,bearing,bearing_acc,is_mock\n")
            eventWriter.write("t_ns,type,note\n")
            flushAll()
        }
    }

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            writerThread = Thread({ processQueues() }, "KinesisWriterThread").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
        }
    }

    fun offerAccel(sample: AccelSample): Boolean {
        return if (accelQueue.offer(sample)) {
            true
        } else {
            droppedAccel.incrementAndGet()
            false
        }
    }

    fun offerGyro(sample: GyroSample): Boolean {
        return if (gyroQueue.offer(sample)) {
            true
        } else {
            droppedGyro.incrementAndGet()
            false
        }
    }

    fun offerStepDetector(sample: StepDetectorSample): Boolean {
        return if (stepDetectorQueue.offer(sample)) {
            true
        } else {
            droppedStepDetector.incrementAndGet()
            false
        }
    }

    fun offerStepCounter(sample: StepCounterSample): Boolean {
        return if (stepCounterQueue.offer(sample)) {
            true
        } else {
            droppedStepCounter.incrementAndGet()
            false
        }
    }

    fun offerLocation(sample: LocationSample): Boolean {
        return if (locationQueue.offer(sample)) {
            true
        } else {
            droppedLocation.incrementAndGet()
            false
        }
    }

    fun offerEvent(sample: EventSample): Boolean {
        return if (eventQueue.offer(sample)) {
            true
        } else {
            droppedEvents.incrementAndGet()
            false
        }
    }

    private fun processQueues() {
        var lastFlushTime = System.currentTimeMillis()

        while (isRunning.get() || hasPendingItems()) {
            var itemsDrained = false

            // Drain accel
            var a = accelQueue.poll()
            while (a != null) {
                itemsDrained = true
                accelWriter.write("${a.tNs},${a.x},${a.y},${a.z},${a.accuracy}\n")
                accelCount.incrementAndGet()
                a = accelQueue.poll()
            }

            // Drain gyro
            var g = gyroQueue.poll()
            while (g != null) {
                itemsDrained = true
                gyroWriter.write("${g.tNs},${g.x},${g.y},${g.z},${g.accuracy}\n")
                gyroCount.incrementAndGet()
                g = gyroQueue.poll()
            }

            // Drain step detector
            var sd = stepDetectorQueue.poll()
            while (sd != null) {
                itemsDrained = true
                stepDetectorWriter.write("${sd.tNs}\n")
                stepDetectorCount.incrementAndGet()
                sd = stepDetectorQueue.poll()
            }

            // Drain step counter
            var sc = stepCounterQueue.poll()
            while (sc != null) {
                itemsDrained = true
                stepCounterWriter.write("${sc.tNs},${sc.cumulative}\n")
                stepCounterCount.incrementAndGet()
                sc = stepCounterQueue.poll()
            }

            // Drain location
            var loc = locationQueue.poll()
            while (loc != null) {
                itemsDrained = true
                locationWriter.write("${loc.tNs},${loc.utcMs},${loc.provider},${loc.lat},${loc.lon},${loc.alt},${loc.accuracy},${loc.speed},${loc.speedAcc},${loc.bearing},${loc.bearingAcc},${loc.isMock}\n")
                locationCount.incrementAndGet()
                loc = locationQueue.poll()
            }

            // Drain events
            var ev = eventQueue.poll()
            while (ev != null) {
                itemsDrained = true
                val cleanNote = ev.note.replace(",", ";").replace("\n", " ")
                eventWriter.write("${ev.tNs},${ev.type},$cleanNote\n")
                eventCount.incrementAndGet()
                ev = eventQueue.poll()
            }

            val now = System.currentTimeMillis()
            if (now - lastFlushTime >= 1000L || !isRunning.get()) {
                flushAll()
                lastFlushTime = now
            }

            if (!itemsDrained && isRunning.get()) {
                try {
                    Thread.sleep(10)
                } catch (ignored: InterruptedException) {
                }
            }
        }

        flushAll()
    }

    private fun hasPendingItems(): Boolean {
        return !accelQueue.isEmpty() || !gyroQueue.isEmpty() ||
                !stepDetectorQueue.isEmpty() || !stepCounterQueue.isEmpty() ||
                !locationQueue.isEmpty() || !eventQueue.isEmpty()
    }

    @Synchronized
    fun flushAll() {
        try {
            accelWriter.flush()
            gyroWriter.flush()
            stepDetectorWriter.flush()
            stepCounterWriter.flush()
            locationWriter.flush()
            eventWriter.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopAndClose() {
        isRunning.set(false)
        try {
            writerThread?.join(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        try {
            accelWriter.close()
            gyroWriter.close()
            stepDetectorWriter.close()
            stepCounterWriter.close()
            locationWriter.close()
            eventWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Compresses accel.csv and gyro.csv to .gz and verifies integrity per spec section 5.
     */
    fun compressAndVerify(): Boolean {
        val accelCompressed = gzipFile(accelFile, File(sessionDir, "accel.csv.gz"))
        val gyroCompressed = gzipFile(gyroFile, File(sessionDir, "gyro.csv.gz"))

        if (accelCompressed && gyroCompressed) {
            val accelOk = verifyGzip(File(sessionDir, "accel.csv.gz"))
            val gyroOk = verifyGzip(File(sessionDir, "gyro.csv.gz"))
            if (accelOk && gyroOk) {
                accelFile.delete()
                gyroFile.delete()
                return true
            }
        }
        return false
    }

    companion object {
        fun gzipFile(source: File, target: File): Boolean {
            if (!source.exists()) return false
            return try {
                FileInputStream(source).use { fis ->
                    GZIPOutputStream(FileOutputStream(target)).use { gzos ->
                        fis.copyTo(gzos)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun verifyGzip(file: File): Boolean {
            if (!file.exists()) return false
            return try {
                GZIPInputStream(FileInputStream(file)).use { gzis ->
                    val buffer = ByteArray(8192)
                    while (gzis.read(buffer) > 0) {
                        // read through to verify CRC
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
