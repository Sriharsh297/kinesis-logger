# Kinesis — Complete Project Handover

## For: A Sriharsh (continuing in a new AI session)

## Prepared by: Claude (project manager role), from the full conversation with P Samuel Vijay

## Date: November 2026

---

## Current Project Status Snapshot

| Item | Status |
| --- | --- |
| Project name | Kinesis |
| Stage | Capstone-I documentation complete; active implementation starting |
| Next concrete action | Build the Android data-collection app (Kotlin, v0 logger) |
| Formal deliverables done | Synopsis, abstract, literature survey, PPT decks (3), architecture diagram |
| Implementation done | Nothing yet |
| Blocking open item | Android v0 logger; coding-agent prompt is ready (see section 10) |
| Data collection | 2 people + up to 4 more, 6 phone models, hand-shake attacks, no mechanical rigs |

---

## 10 Immediate Actionable Tasks

1. Approve the data-collection protocol spec doc titled "Kinesis Data Collection: Protocol and Logger Spec v0.1". Lock label schema, 100 Hz default, task-card list.
2. Run the v0 logger coding-agent prompt (section 10 of that spec) to build the Kotlin app.
3. Run the QC script coding-agent prompt (also in that spec) in a parallel session.
4. Run the P0 bench test on every phone before any human data is collected.
5. Run the P1 field pilot (you two only, all 6 phones) and verify all pilot gates pass.
6. Update the literature survey in the synopsis to add Lee 2025 ("Real Steps or Not", Sensors 25(4):1002) as the primary sensor-only baseline — this paper is directly about move-to-earn auto-walker detection.
7. Fill in the faculty guide name placeholder (P Phani Prasad, Asst. Professor, Dept. of CSE - Allied) across all submitted documents.
8. Confirm whether MVSR requires ethics or guide approval before human-subject data collection.
9. Confirm treadmill access (affects whether task T09 is kept or dropped).
10. Decide on attack label F4 (auto-walker): confirm it is dropped since mechanical rigs are out.

---

## 1. Project Identity and Overview

Kinesis is a research testbed and demonstrable Web3 application combining play-to-earn gaming with move-to-earn (walking rewards). It is simultaneously a B.E. final-year Capstone-I/II major project at MVSR Engineering College, Dept. of CSE (IoT & Cyber Security including Blockchain Technology); a formal research project testing one specific unanswered question; and a working prototype of a fraud-resistant self-sustaining Web3 reward ecosystem.

**Research question:** Does fusing on-chain wallet-behaviour analysis with off-chain phone-sensor plausibility checks improve fraud-detection accuracy over either signal alone, in a Web3 reward ecosystem?

**The real-world problem:** Move-to-earn and play-to-earn platforms (Sweatcoin, StepN, Axie Infinity) have proven people will change behaviour for crypto rewards — but every major platform has collapsed on two fronts: (1) reward fraud via bots and GPS spoofing tools, and (2) token value collapse when reward issuance outpaces real demand (StepN lost over 90% of its token value). Existing fraud detection looks at on-chain patterns only, or at sensor signals only — never both together. That gap is Kinesis's contribution.

**Team:** P Samuel Vijay (2451-23-749-051) and A Sriharsh (2451-23-749-060). Faculty guide: P Phani Prasad, Asst. Professor, Dept. of CSE – Allied.

**Why the research framing is mandatory:** The faculty guide explicitly required research positioning — explicit research question, three baselines (on-chain-only, sensor-only, fused model), and evaluation metrics (precision, recall, F1, false-positive rate, detection latency). All documentation must reflect this framing.

---

## 2. Project Evolution (Chronological)

**Phase 1 — Project selection:** Over 100 senior project titles were reviewed to avoid overlap. Several directions explored and dropped: parametric crypto insurance (too close to commercial work), digital inheritance (low coolness factor), soulbound event tickets (real companies already doing this), ZK bug bounty platform (requires ZK-SNARK expertise not in the team's current stack), DePIN fraud detection with IoT sensor nodes (became the ancestor of the current project).

**Phase 2 — Crystallisation:** Play-to-earn + move-to-earn emerged as strongest because: real documented failures (StepN) provide a defensible problem statement; the team's existing Hyperledger + IoT + AI stack maps directly onto it; it has a memorable live demo; the mini project established behavioural fingerprinting credibility.

**Phase 3 — Feature expansion:** Added Chainlink VRF, ERC-4337, DAO governance, dual-token economics, Amazon vouchers, The Graph, Chainlink Automation.

**Phase 4 — Research framing:** After guide review, project repositioned as research. Added formal research question, separated development vs research objectives, reframed literature survey around four specific papers, added methodology and evaluation plan.

**Phase 5 — Implementation planning:** Android data-collection app decided as first concrete step — before smart contracts, model, or web dApp. Data quality comes first; this directly addresses the mini project failure.

**Key scope changes:**

| Original idea | Current position |
| --- | --- |
| A "Sweatcoin with crypto" product | Research testbed that happens to be a working app |
| AWS Managed Blockchain | Dropped — both teammates have HLF experience from mini project |
| AWS SageMaker (full use) | Narrowed: train locally, deploy endpoint only near final demo, delete immediately |
| Mechanical phone swingers | Dropped — hand shaking and mock GPS are sufficient |
| 10–15 volunteers | Revised to max 6 people including both teammates, 6 phone models |

## 3. Functional Requirements

### 3A. Android Data-Collection App (v0 — currently building)

| Feature | Status | Notes |
| --- | --- | --- |
| Foreground sensor recording service | Planned | Screen-off safe, wake lock, crash recovery |
| Accelerometer + gyroscope at 100 Hz | Planned | TYPE\_ACCELEROMETER, TYPE\_GYROSCOPE |
| Step counter + step detector | Planned | TYPE\_STEP\_COUNTER cumulative, TYPE\_STEP\_DETECTOR per-event |
| GPS via FusedLocationProviderClient | Planned | 1 Hz, fused + GPS provider, is\_mock flag |
| Task-card driven session labelling | Planned | JSON asset, pre-fills all label fields |
| Pre-flight checklist | Planned | Sensor presence, GPS fix, battery, storage, mock-location warning |
| Countdown + still periods + markers | Planned | 10 s countdown, 5 s stills, vibration, long-press stop |
| Post-session quality summary | Planned | Achieved rates, gaps, OS step delta, manual count for counted bouts |
| Local file export (zip via share sheet) | Planned | SHA-256 manifest |
| S3 upload queue with pre-signed URLs | Planned v0.5 | Wi-Fi only, resumable, hash-verified |
| Magnetometer, pressure, light, proximity | Planned v0.5 |  |
| GNSS status and raw measurements | Planned v0.5 |  |
| Coverage tracker screen | Planned v0.5 | Hours per class vs target |

### 3B. Blockchain Layer (planned, Capstone-II)

| Contract | Status |
| --- | --- |
| KinesisUtilityToken (ERC-20, mintable) | Planned |
| KinesisGovToken (ERC-20, scarce) | Planned |
| Staking.sol (lock + yield) | Planned |
| GamePool.sol (stake-to-play + Chainlink VRF) | Planned |
| Treasury.sol (auto fee collection) | Planned |
| DAOGovernor.sol (token-holder voting) | Planned |
| ERC-4337 EntryPoint + Paymaster | Planned |

### 3C. AI Fraud Detection (planned, Capstone-II)

| Component | Status |
| --- | --- |
| Sensor feature extraction (window-based) | Planned |
| On-chain wallet behaviour features | Planned |
| Fusion model (the research contribution) | Planned |
| Baseline B0: OS step counter only | Planned |
| Baseline B1: GPS rules only | Planned |
| Baseline B2: sensor-only ML (Lee 2025 approach) | Planned |
| Baseline B3: on-chain only | Planned |
| Baseline B4: fusion (ours) | Planned |
| SageMaker endpoint (demo/eval only) | Planned, brief deployment |

### 3D. Web dApp (planned, Capstone-II)

| Feature | Status |
| --- | --- |
| Wallet login (Sign-In With Ethereum) | Planned |
| Skill-based stake-to-play games | Planned |
| Staking UI | Planned |
| Live dashboard (balances, fraud flags) | Planned |
| DAO voting UI | Planned |

---

## 4. Non-Functional Requirements

| Requirement | Target / Decision |
| --- | --- |
| Headline metric | False blocks per device-hour at the threshold chosen on validation data — NOT accuracy |
| Evaluation splits | Leave-one-subject-out AND leave-one-device-out AND leave-one-attack-type-out; random splits forbidden |
| Sensor sampling | 100 Hz for accel and gyro; never resampled on-phone; no wall-clock per row |
| Attack prevalence | Evaluate at 1–5% fraud, not 50/50 from SMOTE |
| Blockchain network | Polygon Amoy testnet throughout; no real financial value |
| AWS cost | Everything within free tier; SageMaker endpoint spun up for eval demo only, then deleted immediately |
| Smart contract security | Checks-effects-interactions, ReentrancyGuard, OpenZeppelin primitives |
| Background service | Must survive screen-off for full session duration; tested on at least one aggressive-OEM phone |
| Data privacy | GPS traces are personal data; never on GitHub; subject IDs pseudonymous; name mapping offline only |
| File integrity | SHA-256 manifest per session; upload is hash-verified before local deletion |

---

## 5. Technical Architecture

```
Layer 1 — User Layer
  Web dApp (MetaMask/WalletConnect)      Mobile App (Accelerometer, Gyroscope, GPS)
       |      Stake-to-Play Games                  |
       |      Staking Pool                         |
       v                                           v
Layer 2 — Verification Layer
  Flask Backend  ──►  AI Fraud Detection Engine
                           On-Chain Wallet Behaviour Analysis
                        +  Off-Chain Sensor Plausibility Check
                                    Fusion Model  ◄── (the research contribution)
                                         |
                       ┌─────────────────┴──────────────────┐
                       v                                     v
Layer 3 — Blockchain Layer
  Smart Contracts (Solidity, Polygon Amoy)   Hyperledger Fabric Audit Ledger
  Chainlink VRF                              Immutable Logging (permissioned)
  Dual-Token (Gov + Utility)
  ERC-4337 Paymaster
  Treasury Contract
                       └─────────────────┬──────────────────┘
                                         v
Layer 4 — Output Layer
  Live Dashboard ──► DAO Governance
                └──► Amazon Voucher Redemption

Feedback loop: Fraud Detection Engine ──► Mobile App ("Reward Throttled if Flagged")
```

**Data flow for a single reward claim:**

1. User walks; mobile app streams sensor data to Flask backend.
2. Flask calls AI verification layer: windowed features, physical plausibility score + on-chain wallet behaviour score, fused into a trust score.
3. Trust score passes → Flask calls smart contract to mint/transfer utility tokens. Transaction logged on Hyperledger Fabric simultaneously.
4. Trust score fails → claim flagged, logged on Fabric, reward withheld. Mobile app shows "Reward Throttled".
5. Live dashboard reads Hyperledger logs and testnet events via The Graph (later phase).

**Why Solidity (justification for the guide):** EVM-native; directly compatible with Chainlink VRF and ERC-4337 (both Solidity/EVM standards, no bridging). OpenZeppelin audited libraries reduce custom attack surface. Our contribution is the architectural fusion, not the language. Contracts port to any EVM chain with zero rewrite.

**Why Hyperledger Fabric alongside Polygon Amoy:** Public chain is ideal for token transfers but every byte costs gas and all data is publicly visible — unsuitable for rich audit metadata. HLF is permissioned, channel-based, tamper-evident, with endorsement policy so no single party (including developers) can alter a committed record. Both teammates have prior HLF experience.

**Why Chainlink VRF:** Naive on-chain randomness (block hash, timestamp) is biasable by miners/validators. VRF generates a random value and cryptographic proof off-chain; the coordinator verifies the proof on-chain before the contract accepts the result. Used for trivia tie-breaks and prediction-game settlement.

**Why ERC-4337:** Standard EOA wallets require gas tokens before the user can do anything — enforced at protocol level, not fixable at app layer. ERC-4337 is a pure contract-level standard; UserOperations go through Bundlers to the EntryPoint contract; a Paymaster (funded by the treasury) sponsors gas. New user signs in with familiar login, smart-contract wallet created silently, play/earn immediately.

---

## 6. Technology Stack

| Technology | Purpose | Justified | Notes |
| --- | --- | --- | --- |
| Solidity | Smart contracts | Yes | Hardhat for dev/test/deploy |
| Polygon Amoy testnet | Public chain | Yes | Successor to deprecated Mumbai |
| Hyperledger Fabric | Permissioned audit ledger | Yes — prior experience | Self-hosted locally |
| Chainlink VRF | Provably-fair randomness | Yes | Testnet LINK from faucet |
| ERC-4337 | Gasless account abstraction | Yes | OpenZeppelin AA toolkit |
| OpenZeppelin Contracts | ERC-20, staking, access-control | Yes | Avoids custom attack surface |
| Python / Flask | Backend orchestration | Yes | Existing skill |
| React / Next.js | Web dApp frontend | Yes | With ethers.js |
| React Native or Flutter | Mobile product app | Yes | Data-collection app is native Kotlin |
| Kotlin (native Android) | Data-collection app | Yes — exact timestamp control | Not Flutter/RN, which hide timestamps |
| Scikit-learn / PyTorch | Fraud detection model | Yes | Train locally; SageMaker for eval only |
| Docker | Containerized deployment | Yes — existing skill | Backend, HLF peers, AI inference |
| The Graph Protocol | Subgraph indexing testnet events | Planned | Free for testnet |
| Chainlink Automation | Autonomous recurring contract tasks | Planned | Treasury sweeps; free testnet LINK |
| AWS S3 | Raw sensor data storage | Planned (free tier) | Pre-signed URLs; no AWS keys in APK |
| AWS SageMaker | ML endpoint for demo/eval only | Planned, brief | Delete immediately after eval |
| AWS CloudWatch | Monitoring (optional) | Planned | Ongoing free tier |
| Amazon Kinesis | Real-time sensor telemetry streaming | Planned | "Our project Kinesis uses AWS Kinesis" |

---

## 7. Data Models

### Label schema

| Family | Code | Description |
| --- | --- | --- |
| GENUINE\_MOVEMENT | G1 | Walking (slow/normal/fast) |
| GENUINE\_MOVEMENT | G2 | Running or jogging |
| GENUINE\_MOVEMENT | G3 | Stairs |
| GENUINE\_MOVEMENT | G4 | Treadmill (if available) |
| GENUINE\_MOVEMENT | G5 | Stop-and-go walking |
| BENIGN\_NON\_WALK | B1 | Sitting/standing/typing |
| BENIGN\_NON\_WALK | B2 | Vehicle (bus, car, auto, bike) |
| BENIGN\_NON\_WALK | B3 | Cycling |
| BENIGN\_NON\_WALK | B4 | Household movement |
| FRAUD | F1 | Hand shake, slow |
| FRAUD | F2 | Hand shake, fast |
| FRAUD | F3 | Pendulum/seated arm swing |
| FRAUD | F5 | Mechanical rotating mount (if feasible) |
| FRAUD | F6 | Mock GPS while stationary |
| FRAUD | F7 | Mock GPS + physical fake (F1 or F3) |
| FRAUD | F8 | Sensor replay/emulator injection (phase 2) |

**Dropped:** F4 (auto-walker/phone swinger) — mechanical rigs are out.

### Sensor streams (CSV, gzipped)

| Stream | Rate | Columns |
| --- | --- | --- |
| accel | 100 Hz | t\_ns, x, y, z, accuracy |
| gyro | 100 Hz | t\_ns, x, y, z, accuracy |
| step\_detector | per event | t\_ns |
| step\_counter | on change | t\_ns, cumulative |
| location | 1 Hz | t\_ns, utc\_ms, provider, lat, lon, alt, accuracy, speed, speed\_acc, bearing, bearing\_acc, is\_mock |
| events | per event | t\_ns, type, note |

**Critical rule:** t\_ns is always SensorEvent.timestamp or Location.getElapsedRealtimeNanos() — never wall-clock time. One boot\_offset\_ms per session for alignment. No resampling on-phone.

---

## 8. Literature Survey

| Paper | Role in Kinesis | Key limitation (the gap) |
| --- | --- | --- |
| Yu et al. 2022, arXiv:2211.01000 | Documents P2E economic failure modes | Diagnostic only; no solution proposed |
| **Lee 2025, Sensors 25(4):1002** | **Sensor-only M2E baseline (B2) — ADD THIS** | Sensors only; no on-chain wallet behaviour |
| Chaitanya Anil et al. 2026, Springer ICCCT | On-chain fraud baseline (B3) | On-chain only; no off-chain signals |
| Campos et al. 2020, SecureComm | GPS spoofing detection (supplementary) | Vehicle/non-M2E setting; no on-chain context |
| Honar Pajooh et al. 2021, Sensors | Validates HLF for IoT audit | Device-layer only; no application-layer fraud |
| Meneguzzo et al. 2025, arXiv:2504.11341 | DAO governance design evidence | Governance only; no fraud detection |
| Kim et al. 2023, arXiv:2306.13672 | Tokenomics context | Theory only, zero empirical data — do not cite as evidence |

**Gap statement:** No reviewed work fuses on-chain behavioural signals with off-chain physical-sensor plausibility checks within one sustainable, auditable reward system.

**IMPORTANT:** Never write "no research on M2E fraud" anywhere. Lee 2025 is directly on point. It becomes Baseline B2, not a threat to the gap — because it uses sensors only, with no on-chain context.

---

## 9. Documents Already Produced

| Document | Format | File | Notes |
| --- | --- | --- | --- |
| Formal abstract (120 words) | Word | Proof\_of\_Motion\_Abstract.docx | Use for forms |
| Short bullet abstract | PDF | Kinesis\_Short\_Abstract.pdf |  |
| Complete technical documentation | Word | Proof\_of\_Motion\_Full\_Documentation.docx | 14 sections |
| Capstone-I Synopsis | Word | Kinesis\_Capstone\_Synopsis.docx | 5 pages, 6-section template; guide name placeholder needs filling |
| Literature survey table | PDF | Kinesis\_Literature\_Survey.pdf | 7 papers; Lee 2025 needs adding |
| Acceptance Review PPT | PPTX | Kinesis\_Acceptance\_Review.pptx | 8 slides |
| Literature Review PPT | PPTX | Kinesis\_Literature\_Review.pptx | 6 slides |
| Guide Review PPT (blockchain deep-dive) | PPTX | Kinesis\_Guide\_Review.pptx | 9 slides; explains VRF, ERC-4337, HLF rationale in depth |
| Architecture diagram | PNG | Kinesis\_Architecture\_Diagram.png |  |
| Data-collection protocol spec | Claude Doc | "Kinesis Data Collection: Protocol and Logger Spec v0.1" | Full plan including coding-agent prompts |

---

## 10. Coding-Agent Prompts (Ready to Paste)

Both prompts are in full in section 11 of the data-collection spec doc. Summary:

**Prompt 1 — Android v0 Logger (Kotlin):** Kotlin + Jetpack Compose, single module, no ML, no network in v0, foreground service with wake lock and crash safety, task cards from JSON asset, sensor callbacks only push to queues (writer thread does all I/O), timestamps are t\_ns from elapsed-realtime clock never wall-clock, four commits, unit tests and a 10-min screen-off instrumented test.

**Prompt 2 — QC Script (Python):** pandas CLI, monotonicity, achieved rate, gap analysis, GPS fix %, clock alignment check, OS step audit, PNG plots, per-session JSON report, summary CSV, non-zero exit on pilot gate failure, unit tests with a synthetic session.

---

## 11. Data Collection Plan (Revised for 2 People + Up to 4 More + 6 Phones)

**Revised constraints:** 2 people (Samuel and Sriharsh) + maximum 4 more = 6 people total. 6 phone models available. No mechanical rigs. Hand shaking is the primary physical fraud attack.

**Revised dataset targets:**

| Family | Approx hours | How |
| --- | --- | --- |
| GENUINE\_MOVEMENT | 20 h | 6 people × multiple tasks × multiple phones/placements |
| BENIGN\_NON\_WALK | 6 h | Passive logging during commutes, sitting, vehicles |
| FRAUD | 8 h | Samuel and Sriharsh collecting F1–F7 across all 6 phones |
| Total | \~34 h |  |

**What 2-person-only collection changes:** Leave-one-subject-out is weak with only 2 people. Try to recruit 4 more. Even 4 friends doing the volunteer task-card visit (about 2 h, well-defined tasks) gives 6 subjects and makes the split meaningful. If truly impossible: report explicitly in the paper; evaluate with leave-one-session-out; acknowledge smaller subject pool; argue 6 phone models gives device diversity.

**Attack data with hand shaking:**

- F1 (slow shake) and F2 (fast shake): primary physical attacks.
- F3 (pendulum): phone on a string or held in a swinging arm seated — no rig needed.
- F6: mock GPS while stationary — just enable a mock-location app.
- F7: F6 + F1 or F3 simultaneously.
- Collect at least 3 h of fraud across F1–F7, from both people on all 6 phones.
- Include mimic-mode attempts: consciously try to replicate real-walking cadence while shaking.

---

## 12. Lessons from the Mini Project (Must Apply)

The mini project (IoT-Behavioral-IDS, github.com/psamuelvijay/IoT-Behavioral-IDS) had specific problems that must not be repeated:

| Problem | Control applied in Kinesis |
| --- | --- |
| 94.2% accuracy hid \~1-in-12 false-alarm rate | Headline metric is false blocks per device-hour |
| SMOTE on balanced test set | No resampling before splitting; class weights; evaluate at realistic prevalence (1–5%) |
| Test set was time-slice of same collection | Leave-one-subject, leave-one-device, leave-one-attack-type splits |
| Training and live inference computed features differently | One shared feature function; parity unit test |
| Synthetic attacks trivially separable from real data | Real physical attacks; mimic-mode; unseen-attack evaluation |
| Model learned simulator artifact, not attack signal | Subject/device IDs are never features; leakage test |
| Three-strikes policy locked out good devices | Sustained evidence over windows; cost-based threshold; safe recovery path |
| README claimed 100% accuracy; report said 94.2% | Consistent, honest reporting throughout |

---

## 13. Security and Privacy

**Smart contracts:** Checks-effects-interactions on all state-changing functions. ReentrancyGuard on staking withdraw and game-pool settlement. Role-based minting: only the Flask backend relayer address can call mintReward. Treasury fee set at construction, not adjustable by a single key.

**Data collection:** Raw GPS traces are personal data — never on GitHub. S3 bucket is private; pre-signed URLs with short expiry; no AWS credentials in the APK. Pseudonymous subject IDs; name-to-ID mapping stored offline only. Confirm whether MVSR requires formal ethics approval before recording human subjects.

**Blockchain:** Everything on testnet; no real financial value. ERC-4337 Paymaster funded from transparent treasury. Hyperledger Fabric MSP and channel policy prevent single-party record alteration.

---

## 14. Rejected Ideas and Hard Constraints

| Rejected | Why |
| --- | --- |
| AWS Managed Blockchain | Unnecessary cost; both teammates already deploy HLF locally |
| Mechanical phone swingers / auto-walkers | Out of scope; hand shaking and mock GPS are sufficient |
| GitHub for raw sensor data | File size limits; GPS is personal data |
| Random train/test splits | Repeats mini project evaluation mistake |
| SMOTE before splitting | Leads to inflated test scores |
| OS step events as training labels | The OS step counter is what we're trying to verify/subvert |
| Accuracy as the headline metric | Hid a 1-in-12 false-alarm rate in the mini project |
| AWS keys in the APK | Security violation; use pre-signed URL endpoint |
| Wall-clock timestamps per sensor row | Prevents alignment; use elapsed-realtime nanoseconds |
| Resampling on the phone | Hides hardware timing characteristics needed for spoof detection |

---

## 15. Open Questions

| Question | Blocks what |
| --- | --- |
| Faculty guide name (P Phani Prasad confirmed) | Fill placeholder in all submitted docs |
| Ethics/consent approval at MVSR | Human-subject data collection |
| Treadmill access | Task T09 (G4 label) |
| Can 4 more volunteers be recruited | Leave-one-subject-out validity |
| Lee 2025 added to synopsis and literature slide | Accuracy of submitted documents |
| Exact AWS S3 free-tier limits at time of use | Upload architecture |
| Android 14/15 foreground service type requirements | Coding agent to verify |
| Aggressive-OEM battery kill on pilot phones | Background service reliability |

---

## 16. Glossary

| Term | Meaning |
| --- | --- |
| Kinesis | The project name and research testbed |
| Fusion model | AI model combining on-chain + off-chain signals; the core research contribution |
| DePIN | Decentralised Physical Infrastructure Network; the industry category |
| Move-to-earn | Crypto rewards for verified real-world physical activity |
| DAO | Decentralised Autonomous Organisation; token-holder governance |
| VRF | Verifiable Random Function; Chainlink's implementation for provably-fair randomness |
| ERC-4337 | Ethereum standard for account abstraction; enables gasless transactions |
| EOA | Externally Owned Account; standard Ethereum wallet requiring gas tokens |
| Paymaster | An ERC-4337 contract that sponsors gas fees on behalf of users |
| Utility token | The inflationary token earned by walking/playing; spent inside the app |
| Governance token | The scarce token used for DAO voting and staking |
| Token sink | Mechanism that removes utility tokens from circulation |
| Mimic attack | Fraud attempt where the attacker consciously reproduces a human walking cadence |
| is\_mock | Android API flag indicating whether a GPS location was supplied by a mock provider |
| boot\_offset\_ms | Conversion constant between elapsed-realtime nanoseconds and wall-clock time |
| Task card | Screen in the data-collection app that pre-fills all label fields for one recording bout |
| Pilot gate | Binary pass/fail check that must pass before full data collection begins |
| t\_ns | Elapsed-realtime nanosecond timestamp; the single time base for all sensor and GPS rows |

---

## 20. Instructions for the Next AI (Sriharsh's Session)

You are continuing an existing, active final-year capstone research project called Kinesis. This document is your primary context. Read all sections before proposing any action.

**What is built:** nothing in code yet. The Android data-collection app (v0 logger) is the first implementation task.

**What is decided and must not be reopened without explicit approval:**

- The research question and three-baseline evaluation structure.
- Polygon Amoy testnet + Hyperledger Fabric dual-ledger architecture.
- Chainlink VRF for game randomness; ERC-4337 for gasless onboarding.
- Native Kotlin for the data-collection app.
- Leave-one-subject-out / leave-one-device-out / leave-one-attack-type-out evaluation splits.
- False blocks per device-hour as the headline metric, not accuracy.
- No SMOTE before splitting; no resampling on-phone; no OS step events as training labels.
- No mechanical phone rigs; hand shaking and mock GPS are the attack sources.
- Raw data never on GitHub; S3 via pre-signed URLs.

**Things you must NEVER do or suggest:**

- Random train/test splits
- SMOTE before evaluation
- AWS credentials in the Android app
- Accuracy alone to evaluate fraud detection
- GitHub for raw sensor/GPS data
- Claim that any code has been implemented — nothing has been
- Re-open architecture decisions listed as finalised above
- Invent paper details, metric values, or implementation specifics
- Write "no research on M2E fraud" — Lee 2025 (Sensors 25(4):1002) exists and is directly on point

**Your role:** execute the game plan in this document. The two prompts in section 10 are ready to give to a coding agent. When something is ambiguous, say so rather than guessing. When the conversation history here conflicts with this document, this document reflects the latest confirmed decisions.
