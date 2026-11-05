# Kinesis Testing & Verification Guide

This document details the automated testing suite, hardware bench testing protocols (P0), field pilot verification (P1), and strict quality-control gates required before data freezing.

---

## 1. Automated Test Suite

The Android codebase includes unit tests verifying data pipeline integrity, CSV writer concurrency, schema compliance, and cryptographic hashing.

### Running Unit Tests
Execute the test runner via the Gradle wrapper from the terminal:

```powershell
# Set JDK 21 environment
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"

# Execute unit tests
.\gradlew.bat testDebugUnitTest
```

### Covered Test Modules
- **`SessionJsonBuilderTest`:** Verifies schema conformity, mandatory field presence, hardware capability mapping, and serializability.
- **`CsvWriterTest`:** Tests multi-threaded non-blocking queue ingestion, buffer flushing intervals ($\le 1\text{ s}$), gzip stream integrity, and dropped-sample counters under artificial backlog.
- **`QualityCalculatorTest`:** Validates median rate calculation, p99 gap detection, GPS fix ratio computations, and step counter delta comparisons against synthetic corrupted streams.
- **`TaskLoaderTest`:** Guarantees that `assets/tasks.json` loads all 16 protocol tasks without malformed fields.

---

## 2. P0 Bench Testing Protocol (Hardware Calibration)

Prior to any participant recording, each target smartphone model must undergo an automated 1-hour bench evaluation to characterize hardware noise floors:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        P0 BENCH TESTING GATES                          │
├───────────────────────┬────────────┬───────────────────────────────────┤
│ Test Name             │ Duration   │ Pass / Fail Criteria              │
├───────────────────────┼────────────┼───────────────────────────────────┤
│ Sensor Noise Floor    │ 10 min     │ Device stationary, screen on flat │
│                       │            │ surface. Mean gyro < 0.05 rad/s;  │
│                       │            │ Accel variance < 0.02 (m/s²)².    │
├───────────────────────┼────────────┼───────────────────────────────────┤
│ Screen-Off Background │ 10 min     │ Device stationary, display off.   │
│ Survivability         │            │ Retains >= 99.0% of expected      │
│                       │            │ 100 Hz IMU samples (60,000 pts).  │
├───────────────────────┼────────────┼───────────────────────────────────┤
│ Mid-Session Kill &    │ 3 min      │ Send SIGKILL to app process. Data │
│ Recovery              │            │ flushed to disk up to last 1-2 s  │
│                       │            │ must remain fully valid & gzipped.│
├───────────────────────┼────────────┼───────────────────────────────────┤
│ Monotonic Clock Audit │ 5 min      │ Zero negative time steps          │
│                       │            │ (t_ns[i] - t_ns[i-1] > 0 always). │
├───────────────────────┼────────────┼───────────────────────────────────┤
│ GPS State Audit       │ 10 min     │ 5 min indoor (no fix/coarse) +    │
│                       │            │ 5 min outdoor (>= 90% fixes <20m).│
└───────────────────────┴────────────┴───────────────────────────────────┘
```

---

## 3. P1 Field Pilot Protocol (Protocol Rehearsal)

Conducted by the primary research team (Samuel & Sriharsh) across all 6 test phones to validate task cards and human factors:
1. **Walk Normal (3 min):** Front pocket, outdoor open sky.
2. **Counted Bout (step audit):** Partner manual footfall count recorded and compared to OS delta.

   > [!WARNING]
   > **⚠️ Project-Owner Confirmation Required:** The number of counted step bouts differs between documentation sources:
   > - `docs/PROTOCOL.md` (Section 3, Task T04): specifies **3 × 100 steps** (front pocket, back pocket, and hand-viewing placements).
   > - `docs/TESTING.md` (this file, P1 Field Pilot, item 2): previously specified **2 × 100 steps**.
   > The `assets/tasks.json` defines T04 as a single card with one counted bout of 100 steps. The correct number of bouts per volunteer visit must be confirmed by the project owners before data collection. Do not change the protocol without explicit approval.
3. **Walk Hand-Swinging (3 min):** Brisk pace.
4. **Jogging (2 min):** High-cadence locomotion.
5. **Stationary Desk (2 min):** Baseline environmental noise.
6. **Physical Attack Rehearsal (F1, F3):** 1-minute seated hand-shake; 1-minute pendulum swing.
7. **Mock GPS Attack (F6):** 2-minute stationary session with Android mock location enabled. Verify `is_mock == 1` flags in `location.csv`.
8. **Paired Walk Test:** Two phones placed in opposite pockets of the same researcher during a single walk. Check timestamp alignment.

---

## 4. Dataset Quality Control Gates

Before freezing the experimental dataset (Milestone v1.0), every session directory is processed through an automated Quality Control (QC) validation script. Sessions failing any primary gate must be discarded or re-recorded:

```
                  ┌─────────────────────────────────────┐
                  │       INCOMING SESSION FOLDER       │
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
               ┌───────────────────────────────────────────┐
               │    Gate 1: Timestamp Monotonicity Check   │
               │    Δt = t_ns[i] - t_ns[i-1] > 0           │
               └─────────────────────┬─────────────────────┘
                                     │ Passed
                                     ▼
               ┌───────────────────────────────────────────┐
               │    Gate 2: Effective Sampling Rate Check  │
               │    Achieved Rate >= 95.0 Hz               │
               └─────────────────────┬─────────────────────┘
                                     │ Passed
                                     ▼
               ┌───────────────────────────────────────────┐
               │    Gate 3: Gap Frequency Audit            │
               │    < 0.5% of 10s windows have gap > 100ms │
               └─────────────────────┬─────────────────────┘
                                     │ Passed
                                     ▼
               ┌───────────────────────────────────────────┐
               │    Gate 4: Outdoor GPS Fix Ratio          │
               │    >= 90% of seconds have fix <= 20m      │
               └─────────────────────┬─────────────────────┘
                                     │ Passed
                                     ▼
               ┌───────────────────────────────────────────┐
               │    Gate 5: Cryptographic Hash Match       │
               │    SHA-256(files) == manifest.sha256      │
               └─────────────────────┬─────────────────────┘
                                     │ Passed
                                     ▼
                        [ ACCEPTED INTO DATASET ]
```

### Summary Quality Metrics Thresholds

| Metric | Target | Fail Threshold | Action on Failure |
|---|---|---|---|
| **Timestamp Continuity** | $100\%$ Monotonic | $\ge 1$ decreasing step | Discard session (clock corruption) |
| **IMU Achieved Rate** | $100.0\text{ Hz}$ | $&lt; 95.0\text{ Hz}$ | Review OEM throttling; re-record |
| **Large Gap Ratio** | $0.0\%$ | $&gt; 0.5\%$ of 10s windows | Check battery saver status |
| **Screen-Off Data Retention** | $\ge 99.5\%$ | $&lt; 98.0\%$ | Verify background wake lock |
| **GPS Fix Ratio (Outdoor)** | $\ge 95.0\%$ | $&lt; 90.0\%$ | Repeat bout with clear sky view |
| **Card / Label Match** | $100\%$ compliance | Inconsistent metadata | Discard; never retrospectively relabel |
| **Checksum Integrity** | $100\%$ match | Checksum mismatch | Re-export from source device |
