# Kinesis Data Dictionary (v0.1)

This document specifies the exact directory structures, data formats, column definitions, engineering units, and coordinate systems used by the **Kinesis Logger** Android application.

---

## 1. Directory Structure

Each recording session is saved into an isolated container directory within the Android app-specific external storage:
`Android/data/org.kinesis.logger/files/raw/<session_id>/`

```text
raw/<session_id>/
├── accel.csv.gz          # 100 Hz 3-axis accelerometer stream (compressed)
├── gyro.csv.gz           # 100 Hz 3-axis gyroscope stream (compressed)
├── step_detector.csv     # Event-driven step detector triggers
├── step_counter.csv      # Cumulative hardware step counter values
├── location.csv          # 1 Hz fused & GPS provider location fixes
├── events.csv            # Protocol timeline markers and manual flags
├── session.json          # Complete session metadata & post-run QC audit
└── manifest.sha256       # Cryptographic checksums of all session files
```

### Session ID Format
`KIN-YYYYMMDD-Sxx-TaskID-UUID8`
- Example: `KIN-20261105-S01-T01-a1b2c3d4`
- Components: Project prefix (`KIN`), session start date (`YYYYMMDD`), pseudonymous subject code (`S01`–`S06`), protocol task code (`T01`–`T16`), and an 8-character unique alphanumeric hash.

---

## 2. Sensor Streams & Column Specifications

### A. Accelerometer (`accel.csv.gz`)
Captures raw linear acceleration including gravity as measured by the device's primary accelerometer (`Sensor.TYPE_ACCELEROMETER`).
- **Target Sampling Rate:** 100 Hz (10,000 µs interval).
- **Coordinate System:** Standard Android device coordinate frame ($+X$ points right, $+Y$ points up towards the top of the screen, $+Z$ points out of the front screen surface).
- **Compression:** GZIP format.

| Column | Type | Unit | Description |
|---|---|---|---|
| `t_ns` | Long (Int64) | Nanoseconds | Monotonic timestamp from `SensorEvent.timestamp` (elapsed realtime since device boot). |
| `x` | Float (Float32)| $\text{m/s}^2$ | Acceleration force along the $X$-axis. |
| `y` | Float (Float32)| $\text{m/s}^2$ | Acceleration force along the $Y$-axis. |
| `z` | Float (Float32)| $\text{m/s}^2$ | Acceleration force along the $Z$-axis. |
| `accuracy` | Integer | Enum | Hardware sensor accuracy status: `0` (Unreliable), `1` (Low), `2` (Medium), `3` (High). |

---

### B. Gyroscope (`gyro.csv.gz`)
Captures raw rate of rotation around the device's three physical axes (`Sensor.TYPE_GYROSCOPE`).
- **Target Sampling Rate:** 100 Hz (10,000 µs interval).
- **Compression:** GZIP format.

| Column | Type | Unit | Description |
|---|---|---|---|
| `t_ns` | Long (Int64) | Nanoseconds | Monotonic timestamp from `SensorEvent.timestamp` (elapsed realtime since device boot). |
| `x` | Float (Float32)| $\text{rad/s}$ | Angular speed of rotation around the $X$-axis. |
| `y` | Float (Float32)| $\text{rad/s}$ | Angular speed of rotation around the $Y$-axis. |
| `z` | Float (Float32)| $\text{rad/s}$ | Angular speed of rotation around the $Z$-axis. |
| `accuracy` | Integer | Enum | Hardware sensor accuracy status (`0`–`3`). |

---

### C. Step Detector (`step_detector.csv`)
Logs an instantaneous event whenever the underlying hardware or OS detects a single footstep (`Sensor.TYPE_STEP_DETECTOR`).
- **Sampling Pattern:** Event-driven (asynchronous).

| Column | Type | Unit | Description |
|---|---|---|---|
| `t_ns` | Long (Int64) | Nanoseconds | Monotonic timestamp from `SensorEvent.timestamp` marking the footstep instant. |

---

### D. Step Counter (`step_counter.csv`)
Logs the cumulative step count reported by the hardware step counter sensor since the last device boot (`Sensor.TYPE_STEP_COUNTER`).
- **Sampling Pattern:** On change.

| Column | Type | Unit | Description |
|---|---|---|---|
| `t_ns` | Long (Int64) | Nanoseconds | Monotonic timestamp from `SensorEvent.timestamp` when step counter updated. |
| `cumulative` | Long (Int64) | Count | Total cumulative steps recorded by the operating system since boot. |

---

### E. Location (`location.csv`)
Dual-stream location records captured from Google Play Services `FusedLocationProviderClient` (high accuracy mode) and Android `LocationManager.GPS_PROVIDER`.
- **Target Sampling Rate:** 1 Hz.
- **Datum:** World Geodetic System 1984 (WGS 84).

| Column | Type | Unit | Description |
|---|---|---|---|
| `t_ns` | Long (Int64) | Nanoseconds | Monotonic timestamp from `Location.getElapsedRealtimeNanos()`. |
| `utc_ms` | Long (Int64) | Milliseconds | Wall-clock UTC time of the fix from `Location.getTime()`. |
| `provider` | String | String | Source provider name: `fused` or `gps`. |
| `lat` | Double (Float64)| Degrees ($^\circ$) | Latitude in decimal degrees (WGS 84). |
| `lon` | Double (Float64)| Degrees ($^\circ$) | Longitude in decimal degrees (WGS 84). |
| `alt` | Double (Float64)| Meters ($\text{m}$) | Altitude above the WGS 84 reference ellipsoid (or `0.0` if unavailable). |
| `accuracy` | Float (Float32)| Meters ($\text{m}$) | Estimated horizontal accuracy radius (1-sigma, 68% confidence). |
| `speed` | Float (Float32)| $\text{m/s}$ | Instantaneous velocity over ground. |
| `speed_acc` | Float (Float32)| $\text{m/s}$ | Estimated speed accuracy (or `0.0` if unavailable). |
| `bearing` | Float (Float32)| Degrees ($^\circ$) | Horizontal direction of travel ($0^\circ$–$360^\circ$). |
| `bearing_acc` | Float (Float32)| Degrees ($^\circ$) | Bearing accuracy (or `0.0` if unavailable). |
| `is_mock` | Integer | Boolean | `1` if detected as mock/spoofed (`Location.isMock()` / developer options); `0` otherwise. |

---

### F. Protocol Events (`events.csv`)
Chronological record of session state transitions, user markers, and system lifecycle interruptions.

| Column | Type | Description |
|---|---|---|
| `t_ns` | Long (Int64) | Monotonic elapsed realtime nanoseconds (`SystemClock.elapsedRealtimeNanos()`). |
| `type` | String | Standardized event token (see table below). |
| `note` | String | Optional context string (e.g., landmark, turn, obstacle, interruption reason). |

#### Standard Event Tokens
- `tap_start`: Operator pressed Start button on Pre-Flight screen.
- `countdown_end`: 10-second preparation countdown finished.
- `still_start`: Initial 5-second stillness calibration phase began.
- `bout_start`: Active exercise or attack bout commenced.
- `bout_end`: Active bout reached target duration; terminal stillness began.
- `still_end`: Terminal 5-second stillness phase ended; completion haptics fired.
- `tap_stop`: Operator completed the 2-second hold-to-stop gesture.
- `user_marker`: Operator tapped "MARK EVENT" during active recording (e.g. crossing road, stairs turn).
- `interruption`: External interruption event (screen off, incoming phone call, app backgrounded).

---

## 3. Metadata Specification (`session.json`)

Written at session initialization and populated with quality metrics upon completion:

```json
{
  "session_id": "KIN-20261105-S01-T01-4f9e1a2b",
  "subject_id": "S01",
  "task_id": "T01",
  "protocol_version": "v0.1",
  "app_version": "0.1.0",
  "build_hash": "2ca6f99",
  "time": {
    "start_utc_ms": 1762319400000,
    "tz_offset_min": 330,
    "start_elapsed_ns": 45892110293411,
    "boot_offset_ms": 1762273507889
  },
  "device": {
    "manufacturer": "Google",
    "model": "Pixel 9",
    "android_version": "15",
    "sdk_int": 35,
    "has_step_counter": true,
    "has_step_detector": true,
    "accel_info": "ICM42688 Accelerometer (InvenSense)",
    "gyro_info": "ICM42688 Gyroscope (InvenSense)"
  },
  "labels": {
    "label_family": "GENUINE_MOVEMENT",
    "label_type": "G1",
    "label_binary_fraud": 0,
    "reward_eligible": 1,
    "speed_label": "normal",
    "bout_planned_s": 480
  },
  "context": {
    "placement": "front_pocket",
    "orientation": "Screen facing body, top pointing up",
    "surface": "pavement",
    "environment": "outdoor_open"
  },
  "ground_truth": {
    "bout_type": "free",
    "manual_step_count": null,
    "count_method": null
  },
  "quality": {
    "duration_s": 480.2,
    "n_samples_accel": 47980,
    "n_samples_gyro": 47965,
    "achieved_rate_accel_hz": 99.92,
    "achieved_rate_gyro_hz": 99.88,
    "p99_gap_accel_ms": 10.4,
    "gaps_over_100ms": 0,
    "gps_fix_pct": 98.4,
    "gps_accuracy_median_m": 8.5,
    "os_step_counter_delta": 792,
    "os_step_detector_count": 790,
    "mock_flag_pct": 0.0,
    "battery_start_pct": 85,
    "battery_end_pct": 82
  },
  "admin": {
    "session_ok": true,
    "discard_reason": null,
    "notes": "Clear weather, open park trail."
  }
}
```

---

## 4. Checksum Manifest (`manifest.sha256`)

Prior to export or cloud synchronization, the application hashes all session data files using standard SHA-256:

```text
a1b2c3d4e5f6...  accel.csv.gz
b2c3d4e5f6a1...  gyro.csv.gz
c3d4e5f6a1b2...  step_detector.csv
d4e5f6a1b2c3...  step_counter.csv
e5f6a1b2c3d4...  location.csv
f6a1b2c3d4e5...  events.csv
1a2b3c4d5e6f...  session.json
```

This manifest guarantees end-to-end data integrity from the physical Android storage through API Gateway/Lambda ingestion to the private S3 research bucket.
