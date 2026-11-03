# Kinesis Logger (v0.1)

A native Android data-collection instrument built for the **Kinesis** research project (Capstone-I, by P Samuel Vijay and Arikela Sriharsh).

The app records time-aligned, un-resampled raw sensor streams (100 Hz accelerometer, 100 Hz gyroscope, step detector, step counter, 1 Hz fused GNSS, and 1 Hz raw GPS provider) across controlled genuine movement, benign non-walking, and fraud bouts.

---

## Architecture & Technology Stack

- **Platform:** Native Android (Kotlin, Jetpack Compose, Material3)
- **Minimum SDK:** API 29 (Android 10)
- **Target SDK:** API 35 (Android 15)
- **Visual Design:** Appetite UI Fitness style (Dark-first, deep charcoal `#090D16`, cyan/blue/violet gradients)
- **Concurrency & IO:** Dedicated daemon worker thread per session with thread-safe bounded queues; non-blocking sensor event handlers; atomic periodic flushing (&le; 1 s) with crash recovery.
- **Background Execution:** Foreground Service with `foregroundServiceType="location|health"` and partial wake lock.

---

## 4-Commit Milestones

1. **Commit 1: Skeleton and Task Cards** *(Current)*
   - Single-module project setup (`app`), Material3 theme, Appetite UI design tokens.
   - Dynamic asset loader for `assets/tasks.json` supporting all protocol tasks (T01–T12).
   - Task Dashboard with subject picker (`S01`–`S12` + custom pseudonyms), device detection, and Prev/Next navigation.
   - Initial unit tests validating schema conformity and task counts.

2. **Commit 2: Sensor Recording and Writers**
   - High-throughput non-blocking CSV writers (`accel.csv.gz`, `gyro.csv.gz`, `step_detector.csv`, `step_counter.csv`, `location.csv`, `events.csv`).
   - Strict timestamp fidelity: `t_ns` preserves hardware `SensorEvent.timestamp` and `Location.getElapsedRealtimeNanos()`.
   - Boot offset calibration (`boot_offset_ms`) and dropped-sample counters.

3. **Commit 3: Service and Crash Safety**
   - Foreground service life-cycle surviving screen lock.
   - 10 s countdown vibration, 5 s initial stillness, bout execution, 5 s terminal stillness, completion vibration.
   - WAL flush guarantees and automatic interrupted-session detection on launch.
   - Live recording HUD with timer, sensor health, and 2-second hold-to-stop safety.

4. **Commit 4: Quality Summary and Export**
   - Pre-flight diagnostic screen (sensors, GPS fix accuracy, mock location check, storage &gt; 500 MB, battery &gt; 30%).
   - Post-session quality calculator (achieved rate, median gap, GPS fix %, OS vs manual step count).
   - Manifest generator (`manifest.sha256`), `session.json` metadata, and local-first zip export via system share sheet.

---

## How to Build & Run

### Prerequisites
- JDK 21 (Eclipse Temurin 21 recommended)
- Android SDK with Platform 35 and Build-Tools 34+

### Build from Terminal
```powershell
# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"

# Run Unit Tests
.\gradlew.bat testDebugUnitTest

# Assemble Debug APK
.\gradlew.bat assembleDebug
```
Output APK location:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Known Guidelines & Restrictions (v0)
- **No Internet Permission:** In v0, all data remains strictly local in external app storage (`Android/data/org.kinesis.logger/files/raw/`). Export is performed locally through the system share sheet (e.g. to Google Drive or local files) or ADB pull.
- **Battery Optimization:** Ensure battery optimization is disabled for Kinesis Logger on OEM skins (MIUI, ColorOS, OneUI) to prevent background killing during screen-off recordings.
