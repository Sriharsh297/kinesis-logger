# Kinesis Data Collection Protocol (v0.1)

This document formalizes the data acquisition protocol, task taxonomy, and operational guidelines for the **Kinesis Logger** research instrument. It serves as the standard operating procedure for all field and bench data collections.

---

## 1. Experimental Objectives & Protocol Principles

The goal of Kinesis data collection is to record a high-fidelity, multimodal dataset capable of answering the core research question:
> **Does fusing on-chain wallet behavior with off-chain mobile sensor plausibility checks improve fraud detection over either signal alone in a Web3 reward ecosystem?**

### Core Protocol Rules to Prevent Shortcut Learning

1. **Card-Driven Ground Truth:** All labels originate strictly from pre-configured task cards (`assets/tasks.json`). Labels are never inferred from sensor outputs or OS step detectors.
2. **Balanced Participant & Phone Matrix:** Every participant records both genuine movement and fraud bouts. Every test device records both classes. A model must never be able to classify fraud simply by recognizing a participant's biometric style or a phone's sensor noise floor.
3. **Placement Invariance:** Genuine handheld movement and fraud in a pocket/bag must both exist so models cannot learn placement as a proxy for fraud.
4. **Mimic Attacks:** At least one-third of all fraud bouts must be conducted in **mimic mode**, where the operator consciously attempts to replicate human walking cadence (1.5 to 2.2 Hz) while shaking or swinging the device.
5. **No Shortcut Metadata in Feature Set:** Subject IDs, device models, session UUIDs, and placement names are strictly metadata and **must never be used as model inputs**.
6. **Integrity Rule:** If a session is interrupted (phone dropped, phone call received, operator paused prematurely), the session is marked discarded and re-recorded. Sessions are never retrospectively relabeled.

---

## 2. Session Lifecycle Timeline

Every recording bout enforces a strict 4-phase timeline to eliminate tap artifacts and capture resting gravity and sensor bias:

```
[Tap Start] ──> [10s Prep Countdown] ──> [5s Initial Stillness] ──> [Bout Execution] ──> [5s Terminal Stillness] ──> [Vibration] ──> [Hold 2s to Stop]
```

| Phase | Duration | Event Marker | Expected User State | Purpose |
|---|---|---|---|---|
| **Preparation** | 10 s | `countdown_end` | Positioning phone into designated pocket/bag/hand | Eliminates touchscreen handling and pocket-stowing artifacts |
| **Initial Stillness** | 5 s | `still_start` → `bout_start` | Standing completely stationary | Calibrates local gravity vector ($1g$), sensor bias, and gyro drift |
| **Active Bout** | Variable (3–10 min) | `bout_start` → `bout_end` | Performing the designated task | Active data collection window |
| **Terminal Stillness**| 5 s | `bout_end` → `still_end` | Standing completely stationary | Verifies post-exercise sensor baseline and zero-velocity |
| **Hold-to-Stop** | 2 s continuous hold | `tap_stop` | Deliberate termination | Prevents accidental pocket-tap cancellations |

---

## 3. Task Taxonomy

The protocol defines tasks categorized into three distinct families:

### Family 1: Genuine Movement (`GENUINE_MOVEMENT`)
All genuine tasks simulate legitimate human locomotion eligible for move-to-earn rewards (`reward_eligible = 1`).

| Task ID | Class | Description | Planned Duration | Placement | Environment | Speed / Cadence |
|---|---|---|---|---|---|---|
| **T01** | G1 | Walk Normal | 8 min | Front pocket | Outdoor open sky | Natural (1.6–1.9 Hz) |
| **T02** | G1 | Walk Fast | 5 min | Hand swinging | Outdoor open sky | Brisk / Fast (&gt; 2.0 Hz) |
| **T03** | G1 | Walk Normal | 5 min | Bag / Backpack | Outdoor open sky | Natural |
| **T04** | G1 | Counted Bouts ($3 \times 100$ steps) | ~3 min | Front pocket, back pocket, hand viewing | Outdoor open sky | Monitored step count |
| **T05** | G3 | Stairs Up &amp; Down | 3 min | Front pocket | Stairwell | Variable incline cadence |
| **T06** | G1 | Walk Slow | 5 min | Back pocket | Pavement / Track | Slow (&lt; 1.4 Hz) |
| **T07** | G2 | Running / Jogging | 4 min | Front pocket | Track / Outdoor | Jogging (&gt; 2.4 Hz) |
| **T09** | G4 | Treadmill Walk &amp; Run | 6 min | Front pocket | Indoor gym | Controlled belt speed |
| **T10** | G5 | Stop-and-Go Walking | 8 min | Front pocket | Urban campus / Market | Intermittent pauses &amp; turns |
| **T11** | G1 | Walk Normal (Hand Viewing) | 8 min | Hand viewing | Outdoor open sky | Screen actively monitored |

### Family 2: Benign Non-Walking (`BENIGN_NON_WALK`)
Everyday non-locomotive behaviors that produce sensor motion or GPS drift but must **not** trigger rewards (`reward_eligible = 0`, `label_binary_fraud = 0`).

| Task ID | Class | Description | Planned Duration | Placement | Environment | Notes |
|---|---|---|---|---|---|---|
| **T08** | B1 | Sitting / Typing | 3 min | Desk / Flat surface | Indoor office | Baseline environmental vibrations |
| **B2** | B2 | Motorized Transit | 10–30 min | Lap / Pocket / Seat | Bus, car, auto-rickshaw | High GPS speed, low human cadence |
| **B3** | B3 | Cycling | 10–15 min | Handlebar / Pocket | Road / Path | Smooth rotational acceleration |
| **B4** | B4 | Household Chores | 5–10 min | Pocket / Hand | Indoor residence | Erratic micro-movements |

### Family 3: Fraud Attacks (`FRAUD`)
Active attempts to simulate physical movement or spoof geographic location (`reward_eligible = 0`, `label_binary_fraud = 1`).

| Attack Code | Attack Description | Mode | Device Placement | Implementation Details |
|---|---|---|---|---|
| **F1** | Hand Shake (Slow) | Naive &amp; Mimic | Handheld | Oscillating phone at walking frequency (1.6–2.0 Hz) seated |
| **F2** | Hand Shake (Fast) | Naive | Handheld | Rapid arm shaking (&gt; 3.0 Hz) to inflate step count |
| **F3** | Pendulum / Swing | Semi-automated | String / Arm swing | Low-frequency periodic harmonic swinging |
| **F4** | Motorized Auto-Walker | Automated | Cradle / Rig | Mechanical swinging device *(Status: Dropped per Handover)* |
| **F5** | Rotating Mount / Fan | Automated | Turntable / Fan | Continuous rotational centripetal acceleration *(Status: Conditional)* |
| **F6** | Mock GPS (Stationary) | Spoofed | Stationary desk | Android Developer Mock Location Provider enabled |
| **F7** | Physical Fake + Mock GPS | Hybrid | Handheld / Shaken | Physical shake (F1/F3) combined with mock walking GPS route |
| **F8** | Sensor Replay / Injection | Digital | Android Emulator | Injected virtual IMU streams *(Status: Phase 2)* |

---

## 4. Counted Bouts Step-Audit Methodology

To evaluate the reliability of mobile operating system step counts:
- In approximately 1 out of every 5 genuine walking sessions (specifically task **T04**), participants perform an audited **100-step counted bout**.
- An external partner counts the physical footfalls manually (or films the feet).
- Upon session completion, the exact integer count is entered into the **Manual Step Count** field in the Post-Session screen.
- **Scientific Rationale:** The Android OS `TYPE_STEP_COUNTER` is a black-box hardware vendor algorithm that regularly under-counts or over-counts during abnormal placements and can be easily tricked by fraud attacks (F1/F4). Ground truth step counts establish empirical sensor error distributions across phone manufacturers.

---

## 5. Protocol Discrepancies & Confirmation Items

The following technical differences exist between preliminary specifications and the approved project handover, and are explicitly documented here:

> [!IMPORTANT]
> ### Protocol Confirmation Log
> 
> 1. **Target Hours (34 h vs 55 h):**
>    - *Handover Spec:* Proposes an achievable ~34 collection hours (20 h genuine, 6 h benign, 8 h fraud) optimized for a focused team of 2 researchers plus 4 recruited volunteers across 6 phones.
>    - *Current App Target:* Configured with an aspirational 55.0-hour milestone (32 h genuine, 9 h benign, 14 h fraud).
>    - *Resolution:* The 34-hour collection meets the statistical validity floor for Leave-One-Subject-Out (LOSO) cross-validation; 55 hours remains the extended milestone.
> 
> 2. **Subject Cohort (6 Subjects vs Custom IDs):**
>    - *Handover Spec:* Restricts the active study cohort to **6 participants (S01–S06)** to maintain high protocol discipline and complete paired sessions.
>    - *App Implementation:* The dropdown menu is restricted to `S01`–`S06` by default, with a `+ New ID` prompt available if additional participants receive ethics clearance.
> 
> 3. **Attack Availability Status:**
>    - **F4 (Motorized Auto-Walker):** Dropped from the primary matrix due to hardware unavailability; physical shaking (F1, F2) and pendulum swings (F3) serve as valid mechanical proxies.
>    - **F5 (Rotating Mount):** Conditional upon access to safe bench mounts.
>    - **F8 (Sensor Injection):** Scheduled for Capstone-II testing in emulated environments.
> 
> 4. **Hardware Sampling Rate (100 Hz Target):**
>    - 100 Hz (10,000 µs interval) is the *requested* rate via `SENSOR_DELAY_FASTEST` / `SENSOR_DELAY_GAME`. Actual delivered rates depend on underlying MEMS hardware, Android HAL throttling, and battery levels. Sessions achieving $\ge 95\text{ Hz}$ are considered compliant.
