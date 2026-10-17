# KayaSetu — Offline Bilingual ASHA & Rural PHC Clinical Triage App

**KayaSetu** is an offline-first, bilingual (**English** & **ಕನ್ನಡ / Kannada**) clinical dermatology referral decision-support application designed for **ASHA (Accredited Social Health Activist) workers** and **Rural Primary Health Centre (PHC) Medical Officers**.

## Key Features

1. **4-Screen "30-Second Wizard" Field UX (Warm Cream `#FAF8F5` High-Contrast Palette)**:
   - **Screen 1 — Home & Village Sync Queue**: One-thumb `New Patient Screening` action, 1-tap clinical benchmark presets, and an offline-persisted `Today’s Village Visits` queue with color-coded referral status badges (`Send to Wenlock Hospital`, `Show PHC Doctor`, `Check in 7 Days`).
   - **Screen 2 — Guided Camera Viewfinder**: Circular target ring (`1 hand-span / 15 cm away`), 3 live traffic-light pills (`Light: Good`, `Glare: OK`, `Focus: Steady`), automatic retake prompt when Laplacian sharpness variance ($\sigma^2_{\text{Lap}} < 35$) or specular glare ($>12\%$) fails, and a 1-click Unseen Perturbation Sandbox.
   - **Screen 3 — Illustrated Yes/No Tap Cards, Age Group Pills & Body-Location Selector**: 7 large tap cards (`68dp` height) with plain-language questions in English & Kannada, individual audio readout buttons (`en-IN` / `kn-IN`), quick-tap age group pills (`<20`, `20–35`, `36–49`, `50–65`, `65+`), and 5 anatomical body-part selectors (`Face`, `Arm`, `Leg`, `Foot`, `Trunk`).
   - **Screen 4 — Two-Layer Action Card + WhatsApp Share + PHC Doctor Technical Drawer**:
     - **ASHA Simple View**: Red / Amber / Green referral banner, prominent `Listen to Advice` native TTS readout, `Share Referral Slip on WhatsApp`, and `Save & Print Referral Token`.
     - **PHC Doctor Technical Drawer**: Collapsible accordion displaying the 4-stage explainable vision pipeline (Original ROI, Gray-World + CIE-L*a*b* CLAHE, Otsu Lesion Contour, Attention Saliency Heatmap), Fitzpatrick $\text{ITA}^\circ$ skin-tone calibration, ABCD morphological scores, and Bayesian log-odds contribution bars.

2. **100% Offline-First Architecture**:
   - Bundled with the `4.3 MB` calibrated multimodal model (`models/kayasetu_multimodal.onnx`), Service Worker offline cache, client-side vision & Bayesian inference engine, and native Android Room database persistence.

## Safety Disclaimer

**Decision-Support Aid for PHC Triage — Final diagnosis by Medical Officer.** This application is a clinical referral triage aid and not a standalone diagnostic device.
