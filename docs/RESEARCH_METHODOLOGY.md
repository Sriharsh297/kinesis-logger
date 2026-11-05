# Kinesis Research Methodology & Evaluation Framework

This document outlines the theoretical framing, baseline definitions, experimental evaluation protocol, and scientific rigor measures for the Kinesis research project.

---

## 1. Research Question & Hypothesis

### Primary Research Question
> **Does fusing on-chain wallet behavior analysis with off-chain mobile sensor plausibility checks improve fraud-detection performance over either signal alone in a Web3 reward ecosystem?**

### The Core Problem
Move-to-Earn (M2E) and Play-to-Earn (P2E) decentralized systems incentivize physical exertion through crypto-token issuance. However, existing commercial platforms suffer from severe economic collapse driven by two vulnerabilities:
1. **Physical Sybil & Sensor Fraud:** Automated swingers, manual shaking, and GPS spoofing allow attackers to harvest rewards without expending physical effort.
2. **Siloed Defense Architectures:** Existing fraud detectors inspect either on-chain transaction graphs (missing the physical reality of the user) or phone sensor streams (blind to multi-wallet sybil collusion and token liquidation patterns).

Kinesis tests the hypothesis that **cross-domain multimodal fusion** significantly reduces the operational false-alarm rate while reliably detecting both known and zero-day mechanical attack patterns.

---

## 2. Experimental Baselines

To rigorously measure empirical gains, Kinesis compares its proposed fusion engine against four distinct baselines:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        EVALUATION BASELINE HIERARCHY                   │
├───────────────────────┬────────────────────────────────────────────────┤
│ Baseline              │ Data Sources & Decision Logic                  │
├───────────────────────┼────────────────────────────────────────────────┤
│ B0: OS Step Counter   │ Raw Android TYPE_STEP_COUNTER deltas only.     │
│ B1: GPS Heuristic     │ Speed < 25 km/h, is_mock == 0, cadence bounds. │
│ B2: Sensor-Only ML    │ 100 Hz IMU features (Lee et al. 2025 approach).│
│ B3: On-Chain-Only     │ Wallet mint intervals, transfer fan-out graphs.│
│ B4: Kinesis Fusion    │ Joint multimodal feature vector + attestation. │
└───────────────────────┴────────────────────────────────────────────────┘
```

### B0: Operating System Step Counter Alone
- Evaluates the status quo of common health applications: accepting hardware pedometer increments directly.
- **Hypothesis:** Shows near 0% detection against hand-shakes (F1/F2) and mechanical swingers (F4), because hardware step engines cannot distinguish foot strikes from harmonic pendulum swings.

### B1: GPS Heuristic Rules
- Applies deterministic checks on speed ($\le 25\text{ km/h}$ for walking/running), GPS accuracy ($\le 20\text{ m}$), and Android mock-provider status flags (`Location.isMock()`).
- **Hypothesis:** Catches naive stationary spoofing (F6) but completely fails against physical shaking without GPS (F1–F3) or advanced kernel-level root spoofers that bypass standard flags.

### B2: Sensor-Only Machine Learning Baseline (Lee et al. 2025)
- Implements the state-of-the-art methodology established by **Lee et al. (2025)** (*"Real Steps or Not: Detecting Auto-Walkers in Move-to-Earn via Smartphone IMU Sensors"*, *Sensors*, 25(4):1002).
- Extracts windowed statistical, time-domain (mean, variance, jerk, peak cadence), and frequency-domain (FFT power spectrum, spectral entropy) features from 100 Hz accelerometer and gyroscope streams.
- Classifies bouts using supervised classifiers (LightGBM / 1D-CNN / BiLSTM).
- **Limitation:** Blind to whether a user is operating 10 accounts simultaneously or dumping rewards through automated liquidity pools.

### B3: On-Chain-Only Behavioral Baseline
- Extracts transaction topology features from the Polygon Amoy testnet: inter-claim time variance, gas price bidding patterns, token velocity, and transfer graph entropy.
- **Limitation:** Cannot verify if legitimate physical movement produced the claim.

### B4: Kinesis Multimodal Fusion Engine (Ours)
- Integrates physical movement plausibility with wallet behavioral consistency.
- Combines late-fusion classification with sustained-evidence window filtering.

---

## 3. Evaluation Splits (Strict Leakage Prevention)

To guarantee scientific validity and prevent inflated performance metrics, Kinesis enforces strict non-overlapping validation splits:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     CROSS-VALIDATION SPLIT PROTOCOL                     │
├──────────────────────────┬──────────────────────────────────────────────┤
│ Split Strategy           │ Leakage Prevention Guarantee                 │
├──────────────────────────┼──────────────────────────────────────────────┤
│ Leave-One-Subject-Out    │ Models evaluated on people never seen during │
│ (LOSO)                   │ training. Prevents biometric memorization.   │
├──────────────────────────┼──────────────────────────────────────────────┤
│ Leave-One-Device-Out     │ Evaluated on phone models never in training. │
│ (LODO)                   │ Prevents learning hardware noise floors.     │
├──────────────────────────┼──────────────────────────────────────────────┤
│ Leave-One-Attack-Out     │ Evaluated on attack types (e.g., F3) omitted │
│ (LOAO)                   │ from training. Tests zero-day resilience.    │
└──────────────────────────┴──────────────────────────────────────────────┘
```

> [!WARNING]
> **Prohibited Practices:**
> - **No Random Splits:** Randomly shuffling 10-second windows across train and test partitions is strictly forbidden. Because consecutive windows within a walk are strongly autocorrelated, random splitting produces illusory $\approx 99\%$ accuracy.
> - **No Synthetic Resampling on Test Data:** Techniques like SMOTE must never be applied to validation or test splits. Evaluation must occur at the natural real-world prevalence of fraud.

---

## 4. Headline Research Metrics

In physical security and financial fraud systems, standard accuracy is misleading: in a dataset where 97% of bouts are genuine, a trivial dummy classifier predicting "Genuine" achieves 97% accuracy while missing 100% of attacks.

Kinesis establishes **False Blocks per Device-Hour** as its primary headline metric:

$$\text{False Block Rate (FBR)} = \frac{\text{Total Genuine Windows Incorrectly Blocked}}{\text{Total Device-Hours of Held-Out Genuine Locomotion}}$$

### Operational Evaluation Criteria
1. **Target False Block Rate:** $\le 0.1\text{ false blocks per device-hour}$ on held-out participants.
2. **Detection Latency:** The minimum sustained window duration required before a fraud flag is raised (target: $\le 30\text{ seconds}$, or 3 consecutive 10-second windows).
3. **Unseen Attack Detection Rate:** Recall on LOAO attack classes.
4. **Step Audit Error:** Mean Absolute Error ($\text{MAE}$) between Android OS step counter increments and audited 100-step counted bouts across all tested device models.

---

## 5. Lessons Learned from Prior Work (IoT-Behavioral-IDS)

The methodology directly resolves methodological defects identified during earlier academic trials:

| Previous Limitation | Corrective Control in Kinesis |
|---|---|
| 94.2% accuracy obscured a 1-in-12 false alarm rate | Replaced accuracy with false blocks per device-hour as headline metric. |
| Test set was time-sliced from identical collection sessions | Implemented strict Leave-One-Subject-Out and Leave-One-Device-Out splits. |
| Synthetic attacks trivially separated from real data | Captured real physical hand-shakes, pendulum swings, and cadence mimicry. |
| Model learned simulator timing artifacts rather than attack physics | Subject IDs, device models, and timestamps are strictly excluded from features. |
| Three-strike lockout permanently banned legitimate users | Implemented sliding-window sustained evidence and graceful recovery protocols. |
