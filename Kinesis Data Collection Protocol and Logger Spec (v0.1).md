# Kinesis Data Collection: Protocol and Logger Spec (v0.1)

For P Samuel Vijay and A Sriharsh. Kinesis Capstone-I. Draft 22 Oct 2026. Written by Claude as project manager; your coding agent writes the code from section 11.

## 0. Decisions at a glance

I tightened several of your ideas. Push back on any of them.

| Your idea | My call | Why |
| --- | --- | --- |
| Android step events as step labels | Log them as a weak reference only. Labels come from the protocol and are audited on counted bouts. | The OS step stream is the phone's own black-box algorithm, so a model trained on it relearns that algorithm. In fraud sessions the OS counts fake steps as real, so it is the thing under attack. |
| Dropdowns for activity, placement, environment | Task cards that pre-fill every field. | Wrong labels are the cheapest data bug to prevent. |
| Save to S3 or GitHub | Local first, then a private S3 bucket via pre-signed URLs. Never GitHub for raw data. | GPS traces are personal data, GitHub has file-size limits, and AWS keys must never ship inside an APK. |
| Walking and running | Three families: genuine movement, benign non-walking, fraud. | A fraud detector is only as good as its attack data; the mini project failed on easy attacks. |
| One phone per session | Optional paired sessions: two phones, one walk. | Free cross-device data. |
| Tap start, tap stop | Countdown, still periods, event markers. | Removes tap artifacts and gives a gravity and bias reference. |
| Not raised | Native Kotlin for the collector. | Exact control of sensor timestamps and the background service. The product app can stay Flutter or React Native. |

## 1. What the data must support

Research question: does fusing on-chain wallet behaviour with phone-sensor plausibility detect reward fraud better than either alone? This phase builds the sensor half.

- Baselines: OS step counter alone, GPS rules (speed vs cadence, mock flag, GNSS checks), sensor-only ML. On-chain and fusion come later.
- Headline metric: false blocks per device-hour on held-out people, plus detection rate per attack type, including attack types never seen in training.
- Splits: leave-one-subject-out, leave-one-device-out, leave-one-attack-type-out. So we need many people, several phones and several attacks, with no label tied to one person, phone or placement.
- An audit of the OS step counter on manually counted bouts.

## 2. Label schema

| Family | Code | Type |
| --- | --- | --- |
| GENUINE\_MOVEMENT | G1 | Walking (slow, normal, fast) |
| GENUINE\_MOVEMENT | G2 | Running or jogging |
| GENUINE\_MOVEMENT | G3 | Stairs up and down |
| GENUINE\_MOVEMENT | G4 | Treadmill walk or run |
| GENUINE\_MOVEMENT | G5 | Stop-and-go walking (crossings, shopping) |
| BENIGN\_NON\_WALK | B1 | Sitting or standing; phone in pocket or typing on a desk |
| BENIGN\_NON\_WALK | B2 | Vehicle: bus, car, auto-rickshaw, two-wheeler |
| BENIGN\_NON\_WALK | B3 | Cycling |
| BENIGN\_NON\_WALK | B4 | Household movement (chores, carrying things) |
| FRAUD | F1 | Hand shake, slow |
| FRAUD | F2 | Hand shake, fast |
| FRAUD | F3 | Pendulum or swing (phone on a string, seated arm swing) |
| FRAUD | F4 | Motorised auto-walker or phone swinger |
| FRAUD | F5 | Mechanical rotating mount (fan, crank, turntable) |
| FRAUD | F6 | Mock GPS while the phone is stationary |
| FRAUD | F7 | Mock GPS plus a physical fake (F1, F3 or F4) |
| FRAUD | F8 | Sensor replay or emulator injection (phase 2) |

**Label columns (per bout):** label\_family, label\_type, label\_binary\_fraud (1 only for FRAUD), reward\_eligible (1 only for GENUINE\_MOVEMENT), attack\_mode (naive, or mimic = the operator copies a walking cadence of about 1.5 to 2.2 steps per second), speed\_label (slow, normal, fast, self-selected).

**Rules**

1. The label comes from the task card, never from the sensors.
2. The 10 s countdown and the two 5 s still periods are logged as events and labelled STILL, not as the activity.
3. Per-sample and per-window labels are derived offline from bout boundaries.
4. If the task was not performed as written (phone fell, phone call, stopped early), discard the session; never relabel it.

**OS step events.** Logged: step-detector event times and step-counter cumulative values. Role: a weak reference and a consistency feature (OS steps vs physics-plausible steps vs GPS distance). Not the label. Audit: in about 1 in 5 genuine walking sessions, record a counted bout: walk exactly 100 steps while a partner counts (or film it) and enter the count afterwards. This gives the OS error per phone and placement. Offline, step times are also estimated from the accelerometer by peak detection and validated on the counted bouts.

## 3. Protocol

**Session timeline:** tap Start, 10 s countdown (put the phone in position), 5 s stand still, bout, 5 s stand still, vibration, hold 2 s to Stop. Everything between the taps is logged. In fraud bouts the still periods are done with the rig stopped.

**Bout lengths:** walking or running 5 to 10 min (minimum 3); counted bout 100 steps (about 1 min); stairs 3 min; stop-and-go 8 to 10 min; vehicle up to 30 min; still or typing 5 min; fraud 3 to 5 min.

**Factors to vary** (each covered, none tied to one label): subject (at least 10, target 12 to 15); phone model (at least 3, at least 2 brands, one low-end); placement (front pocket, back pocket, hand swinging, hand viewing, bag or backpack); orientation (recorded); speed; surface (pavement, track or trail, indoor corridor, treadmill, stairs); GPS environment (open sky, urban canyon, indoor); day and time (every volunteer on 2 different days).

**Rules that prevent shortcut learning**

- Every subject records both genuine and fraud data. Every phone model does too.
- Genuine hand-held walking and fraud in a pocket or bag must both exist, otherwise the model learns placement.
- At least one third of fraud sessions are in mimic mode.
- Same app build and sampling settings for every session.
- Subject, phone, session and placement IDs are metadata, never model features.
- Paired sessions (two phones, one walk) share a walk\_group\_id and always fall in the same split.

**Volunteer visits (about 2 h in total, two days)**

| Task | Class | Details |
| --- | --- | --- |
| T01 | G1 | Walk normal, front pocket, 8 min |
| T02 | G1 | Walk fast, hand swinging, 5 min |
| T03 | G1 | Walk normal, bag, 5 min |
| T04 | G1 | Counted bouts x3: front pocket, back pocket, hand viewing (100 steps each) |
| T05 | G3 | Stairs, front pocket, 3 min |
| T06 | G1 | Walk slow, back pocket, 5 min |
| T07 | G2 | Jog, front pocket, 4 min |
| T08 | B1 | Sit and type, phone on desk, 3 min |
| T09 | G4 | Treadmill walk then run, 6 min (if available) |
| T10 | G5 | Stop-and-go walk on campus roads or a market, 8 min |
| T11 | G1 | Walk normal, hand viewing, 8 min |
| T12 | F1, F2, F3, F6, F7 | Fraud pack, about 20 min (3 min each, plus F4 if the device exists, plus one mimic repeat) |

T01 to T08 are one visit, T09 to T12 the other. Team-only sessions: long passive logs (commute, vehicles, cycling), auto-walker and rig sessions, mock-GPS combinations, F8 later.

## 4. Metadata fields

Written to session.json at start and completed at the end.

| Group | Fields |
| --- | --- |
| Identity | session\_id (KIN-YYYYMMDD-Sxx-nn plus a UUID), subject\_id (pseudonym), task\_id, protocol\_version, app\_version, build\_hash, walk\_group\_id, paired\_session\_id |
| Device (auto) | manufacturer, model, android\_version, sdk\_int, install\_id (random, not IMEI), per-sensor name, vendor, range, resolution, min\_delay\_us, requested and achieved rate, has\_step\_counter, has\_step\_detector, gnss\_raw\_supported |
| Time (auto) | start\_utc\_ms, tz\_offset\_min, start\_elapsed\_ns, boot\_offset\_ms (wall clock minus elapsed realtime at start) |
| Labels (from card) | label\_family, label\_type, label\_binary\_fraud, reward\_eligible, speed\_label, bout\_planned\_s |
| Context | placement, orientation, carried\_with, surface, environment (outdoor open, urban canyon, indoor, treadmill), route\_id, footwear (optional) |
| Fraud only | attack\_mode, rig\_id, mock\_app\_name, mock\_route\_id, operator note |
| Ground truth | bout\_type (free or counted), manual\_step\_count, count\_method (partner, video, tap counter) |
| Quality (auto at end) | duration\_s, n\_samples per stream, achieved rate median, p99 gap, gap\_count\_over\_100ms, gps\_fix\_pct, gps\_acc\_median\_m, os\_step\_counter\_delta, os\_step\_event\_count, mock\_flag\_pct, screen\_off\_s, battery\_start and end, app\_restarts, files\_sha256 |
| Admin | session\_ok, discard\_reason, notes, consent\_version, upload\_status |

**Participants sheet** (offline, restricted, never uploaded with data): subject\_id, name and contact, consent date, age band, height band, usual phone model.

**events.csv** (t\_ns, type, note): tap\_start, countdown\_end, still\_start, bout\_start, bout\_end, still\_end, tap\_stop, user\_marker (turn, crossing, stop, stairs), interruption, screen\_on, screen\_off, service\_restart, permission\_change.

## 5. Data format, time base and storage

| Stream | Rate | Columns | Phase |
| --- | --- | --- | --- |
| accel | 100 Hz | t\_ns, x, y, z, accuracy | v0 |
| gyro | 100 Hz | t\_ns, x, y, z, accuracy | v0 |
| step\_detector | per event | t\_ns | v0 |
| step\_counter | on change | t\_ns, cumulative | v0 |
| location | 1 Hz fused plus 1 Hz GPS provider | t\_ns (elapsed), utc\_ms, provider, lat, lon, alt, accuracy, speed, speed\_acc, bearing, bearing\_acc, is\_mock | v0 |
| events | per event | see section 4 | v0 |
| mag | 50 Hz | t\_ns, x, y, z, accuracy | v0.5 |
| pressure, light, proximity | 10 Hz, on change, on change | t\_ns, value | v0.5 |
| gnss\_status | per epoch | t\_ns, n\_visible, n\_used, mean C/N0 | v0.5 |
| gnss\_raw (if supported) | per epoch | t\_ns, per-satellite C/N0 and AGC | v0.5 optional |

**Time base rules**

- Every row uses one clock: SensorEvent.timestamp and Location elapsedRealtimeNanos share the elapsed-realtime base on modern Android. Verify this on every pilot phone.
- Never stamp rows with wall-clock time. Store one boot\_offset\_ms per session to convert later.
- Log what the hardware delivered. No resampling on the phone; resample offline. (WISDM's non-uniform, misaligned streams are the cautionary tale.)
- Keep the accuracy field; never drop rows.

**Layout**

```
raw/<session_id>/
  session.json  events.csv  manifest.sha256
  accel.csv.gz  gyro.csv.gz
  step_detector.csv  step_counter.csv  location.csv
  (v0.5) mag.csv.gz  env.csv  gnss_status.csv
processed/      Parquet, produced offline, never hand-edited
participants/   offline, restricted
```

**Size estimate (assumption, confirm in the pilot):** about 50 bytes per CSV row, so accel plus gyro at 100 Hz is about 36 MB per hour and the magnetometer adds about 9 MB per hour. The whole dataset (about 55 h) is roughly 2.5 GB raw and about 0.5 GB gzipped.

**Storage plan**

- Pilot: one zip per session via the share sheet to a private Drive folder, or ADB pull.
- v0.5: upload queue to a private S3 bucket. The app asks a small endpoint (Lambda or Flask) for a pre-signed URL. Wi-Fi only, resumable, SHA-256 checked; delete the local copy only after a verified upload. Never embed AWS keys in the app.
- GitHub holds code, task-card JSON and the schema only. No raw data, no GPS.
- Check your account's current AWS free-tier terms in the Billing console. The data is small, but I have not verified current limits.

## 6. App functionality

**v0 (build first)**

- Consent and permissions: ACTIVITY\_RECOGNITION, fine location, notifications; guided exemption from battery optimisation.
- Subject picker with pseudonymous IDs (S01, S02, ...).
- Task cards loaded from a JSON asset (editable without rebuilding), with Prev and Next and today's progress.
- Pre-flight: sensors present, requested rates, GPS fix and accuracy, battery above 30%, free storage, step sensors, battery-optimisation status, mock-location status (expected ON only for F6 and F7, otherwise a warning).
- Countdown with vibration, still periods, recording in a foreground service (screen-off safe, wake lock), long-press to stop, marker button.
- Recording: accel, gyro, step detector, step counter, location (fused and GPS) with is\_mock, events.
- Writer thread with queues, flush every second, no UI-thread I/O, dropped-event counters, crash recovery (an interrupted session stays readable and is flagged).
- Post-session summary (achieved rates, gaps, GPS fix %, OS steps), manual step count for counted bouts, session OK, notes, save or discard.
- Sessions list with status; export a zip via the share sheet.

**v0.5:** magnetometer, pressure, light, proximity, GNSS status and raw; S3 upload queue with retry and hash check; paired-session mode; coverage tracker (hours collected vs target per class, placement and phone); 60 s sensor self-test.

**v1:** remote task plans, per-subject progress, in-app warnings for high gap rates.

**Non-goals:** no on-device ML, no wallet or blockchain code, no accounts.

**Robustness:** many phones sold in India (Xiaomi, Oppo, Vivo, Realme, Samsung) kill background apps aggressively. Include one in the pilot; dontkillmyapp.com has per-OEM steps.

## 7. Wireframes

A. Task card (home)

```
+------------------------------+
| Kinesis Logger          [gear]|
| Subject: [ S07 v ]  [+ new]  |
| Phone: Redmi Note 11 (auto)  |
+------------------------------+
| TASK T03        (3 of 14)    |
| Class: GENUINE - Walk        |
| Speed: Normal                |
| Placement: Front pocket      |
|   screen to body, top up     |
| Environment: Outdoor, open   |
| Duration: 8 min              |
| Counted bout: No             |
| [ Prev ]          [ Next ]   |
| [   PRE-FLIGHT CHECK     ]   |
+------------------------------+
```

B. Pre-flight

```
+------------------------------+
| Pre-flight                   |
| [OK] Accelerometer 100 Hz    |
| [OK] Gyroscope 100 Hz        |
| [OK] Step sensors            |
| [..] GPS fix: acc 12 m       |
| [OK] Battery 78%, storage OK |
| [!!] Mock location: ON       |
|      (expected for F6, F7)   |
| [OK] Battery optimisation off|
| [       START (10 s)      ]  |
+------------------------------+
```

C. Recording

```
+------------------------------+
| RECORDING     08:12 / 08:00  |
| T03 Walk - Front pocket      |
| acc OK  gyro OK  GPS 9 m     |
| OS steps: 842 (reference)    |
|                              |
| [      MARK EVENT       ]    |
|  (turn, stop, crossing...)   |
|                              |
| [  hold 2 s to STOP     ]    |
+------------------------------+
```

D. Post-session

```
+------------------------------+
| KIN-20261105-S07-03          |
| Duration 8:02   Gaps: 0      |
| acc 100.2 Hz   gyro 99.9 Hz  |
| GPS fixes 97%   OS steps 842 |
| Manual step count: [ ___ ]   |
|   (counted bouts only)       |
| Session OK?  (o) Yes ( ) No  |
| If No, why: [ phone fell v ] |
| Notes: [                  ]  |
| [ SAVE ]          [ DISCARD ]|
+------------------------------+
```

E. Sessions and upload: a list of sessions (date, task, duration, status: local, uploaded, failed), a Retry button, Export zip, and (v0.5) a Coverage screen showing a grid of hours collected vs target.

## 8. Pilot

**P0 bench (no people, about 1 h per phone):** phone flat on a table for 10 min (noise floor); 10 min with the screen off; kill the app mid-session and recover; GPS indoors and outdoors, 5 min each; clock-alignment check with marker taps.

**P1 field (you two, 2 or 3 phones including one aggressive-OEM phone, about 45 min per phone):** walk normal in front pocket 3 min plus two counted 100-step bouts; walk hand swinging 3 min; jog 2 min; sit 2 min; F1 shake 1 min; F3 pendulum 1 min; F6 mock GPS stationary 2 min; one paired session.

**Gates (all must pass before full collection)**

| Check | Gate |
| --- | --- |
| Timestamps | Zero decreasing timestamps; sensor and location rows on one clock (any constant offset over 100 ms is a defect) |
| Sampling | Achieved rate recorded per sensor; fewer than 0.5% of 10 s windows contain a gap over 100 ms |
| Screen off | 10 min with the screen off keeps at least 99% of expected samples |
| Crash safety | App killed mid-session: data readable up to the last 2 s |
| GPS | Outdoors, at least 90% of seconds have a fix with accuracy 20 m or better |
| Labels | Zero wrong-card errors across pilot sessions |
| Storage | MB per hour within 25% of the section 5 estimate |
| Export | Every pilot session exported and hash-verified |
| Step audit | OS-vs-manual error per phone recorded (information, not a gate) |

**Outputs:** final sampling rate (50 or 100 Hz), window-length candidates, a defect list, and a go or no-go.

## 9. Full collection plan

| Family | Phase 1 target | Extended (Phase 1b) |
| --- | ---: | ---: |
| GENUINE\_MOVEMENT | 20 h | 32 h |
| BENIGN\_NON\_WALK | 6 h | 9 h |
| FRAUD | 8 h | 14 h |
| **Total** | **~34 h** | **~55 h** |

These targets are cumulative recording time across individual task-card bouts, subjects, and phones; they are not continuous sessions. Each task-card bout is the atomic unit (for example, T01 is 8 minutes and T04 is 1 minute). Repeating a card for another subject or phone adds another bout toward the dataset total. As a rough planning estimate, 20 hours of genuine movement is about 150 bouts of 3–8 minutes each, distributed across the cohort and devices.

10 s windows with a 5 s stride give 720 windows per hour: about 40,000 windows in total, about 20,000 independent.

**Who records what**

- 12 volunteers (floor 10) at about 2 h each over two days: 24 h (100 min genuine and benign, 20 min fraud each).
- You two: the remaining 31 h, about 15 h each over 3 weeks (roughly 45 min a day): long passive logs, rigs, mock GPS, vehicles.
- Phones: the volunteers' own plus your reference phones, at least 3 models. Paired sessions on about 20% of volunteer sessions.

**Why this size**

- Subjects: leave-one-subject-out needs enough people to measure the spread between people. 10 is the floor, 12 to 15 the target. Public datasets in the literature have tens of subjects (for example 27 and 30).
- False blocks: with about 40 h of genuine and benign data from held-out people, zero false blocks bounds the true rate below about 0.075 per hour at 95% confidence (rule of three, 3 / 40 h). Hours are correlated within people, so read it as about 0.1 per hour. This is the number that matters for the product; accuracy did not reveal it in the mini project.
- Attacks: 1.5 to 2.5 h per type, from at least 3 operators and 2 phones, so leave-one-attack-type-out has something to test.
- If recruiting falls short (say 6 volunteers): keep the protocol, report wider confidence intervals, and do not pad evaluation with public datasets. Public sets are for pre-training on genuine walking only.

**Schedule (relative weeks, adjust to your calendar)**

| Week | Work | Gate |
| --- | --- | --- |
| 1 | Approve this spec; coding agent builds v0; consent form | v0 installs and records |
| 2 | Pilot P0 and P1; fix defects | Section 8 gates pass |
| 3 to 5 | Volunteer visits and team sessions; weekly coverage review | At least 80% of every coverage cell |
| 6 | QC pass, fill gaps, freeze dataset v1.0 with a hash manifest | No open critical QC flags |
| 7+ | Harness and baselines (section 10) |  |

**After every session** run the QC script (prompt 2). Flagged sessions are re-recorded within the week.

## 10. Harness preview and lessons from the mini project

- Windows: 10 s with a 5 s stride to start; test 5 s and 20 s after the pilot.
- Baselines: B0 OS step counter alone; B1 GPS rules; B2 sensor-only ML; B3 fusion with on-chain features later.
- Decision rule: sustained evidence over several windows, with the threshold chosen on validation people by the cost of a false block.

| Mini project problem | Control here |
| --- | --- |
| Accuracy hid a 1-in-12 false alarm rate | Headline metric is false blocks per device-hour at a threshold chosen on validation people |
| SMOTE and a balanced test set | No resampling before splitting; class weights; report at natural prevalence (assume 1 to 5% fraud) |
| Test set from the same short collection | Leave-one-subject, leave-one-device and leave-one-attack-type splits |
| Training and live features computed differently | One shared feature function used offline and in serving, with a parity unit test |
| Synthetic attacks were easy to tell apart | Real physical attacks, mimic mode, unseen-attack evaluation |
| Model could read the simulator, not the attack | IDs are never features; leakage tests: shuffled labels, and predicting subject or phone from the features |
| Three strikes locked out good devices | Sustained evidence, a safe recovery path, cost-based threshold |

## 11. Coding-agent prompts

**Prompt 1: v0 logger**

```
You are building an Android data-collection app, a research instrument called Kinesis Logger. Kotlin, Jetpack Compose, single module. The attached spec is the source of truth: sections 2 to 7 define labels, metadata, file formats and screens. Do not add features that are not in the spec; ask when something is ambiguous.

Goal: record time-aligned raw sensor and GPS data for labelled sessions with no data loss, then export it.

1. Permissions: ACTIVITY_RECOGNITION (runtime, API 29+), fine and coarse location, notifications (API 33+). No internet permission in v0.
2. Task cards: load from assets/tasks.json (fields per spec sections 2 and 4). Subject picker with pseudonymous IDs. Home screen shows the current card with Prev and Next.
3. Pre-flight screen with the checks in spec section 6. Block Start if the accelerometer or gyroscope is missing or free storage is under 500 MB.
4. Recording runs in a foreground service with a partial wake lock and survives screen off. Declare the right foregroundServiceType and check the current Android 14 and 15 requirements. Sequence: 10 s countdown with vibration, 5 s still, bout until the planned duration or a 2 s long-press to stop, 5 s still, vibration. Write every transition to events.csv.
5. Sensors: accelerometer and gyroscope at a 10,000 us sampling period (100 Hz); TYPE_STEP_DETECTOR and TYPE_STEP_COUNTER; location from FusedLocationProviderClient (1 s, high accuracy) plus LocationManager GPS_PROVIDER (1 s). Log is_mock with Location.isMock() on API 31+ and isFromMockProvider below that. Use sensor batching only if timestamps stay exact.
6. Timestamps: write SensorEvent.timestamp and Location.getElapsedRealtimeNanos() unchanged as t_ns. Never write wall-clock time per row. At session start write start_utc_ms, start_elapsed_ns and boot_offset_ms. Do not resample on the phone.
7. Writing: sensor callbacks only push into bounded queues. A separate writer thread appends plain CSV and flushes at least every second. No file I/O on the main thread. Count and log dropped events. Gzip the files on save and verify.
8. Crash safety: if the app or service dies mid-session, the files written so far stay readable and the session is marked interrupted on the next launch.
9. Post-session screen: compute and show the quality fields from spec section 4 (achieved rate, gaps over 100 ms, GPS fix percent, OS step delta). Manual step count only for counted bouts. Session OK yes or no with a reason, notes, save or discard.
10. Storage: app-specific external storage under raw/<session_id>/ with the file names in spec section 5. Sessions list screen. Export a session as a zip through the share sheet. Write session.json with every field in spec section 4 (null where unknown) and manifest.sha256 on save.
11. Non-goals: no ML, no network upload, no blockchain, no accounts.

Quality: unit tests for the CSV writers, the session.json builder and the quality calculator; an instrumented test that records 10 minutes with the screen off and verifies the files. Provide a README with build steps and known issues. Deliver in four commits: (1) skeleton and task cards, (2) sensor recording and writers, (3) service and crash safety, (4) quality summary and export.
```

**Prompt 2: QC script**

```
Write a Python 3 command-line tool using pandas that takes a raw/<session_id> folder (or a folder of sessions) and checks it against the Kinesis spec. For each session report: timestamp monotonicity per stream; achieved sampling rate, p99 gap and gaps over 100 ms; GPS fix percentage and median accuracy; whether location and sensor timestamps share one clock; OS step-counter delta vs step-detector event count vs manual step count; missing or null required metadata fields; and plots saved as PNG (accelerometer magnitude, its spectrum, GPS speed vs step cadence). Write a per-session JSON report and one summary CSV for the whole folder. Exit non-zero if any pilot gate in spec section 8 fails. Include unit tests with a tiny synthetic session.
```

## 12. Ethics and privacy

- Written informed consent before any recording; volunteers can stop and ask for their data to be deleted.
- Pseudonymous subject IDs; the name mapping stays offline.
- Raw GPS stays private. If the dataset is ever shared, trim or fuzz start and end points near homes.
- Check whether your college needs guide or ethics approval for human-subject data; I do not know your institution's rule.
- Mock-GPS and rig tests are done on your own devices only.

## 13. Assumptions and open items

- Android only. At least 10 volunteers reachable, at least 3 phone models, treadmill access, and an auto-walker or phone swinger you can buy (otherwise F4 slips).
- I did not re-open the Android Sensor class reference this turn. The coding agent should confirm constant names and foreground-service rules on your target API level.
- Hours, gates and thresholds in sections 8 and 9 are my proposals, not literature values.
