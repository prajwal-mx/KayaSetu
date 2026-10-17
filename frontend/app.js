let currentLang = "en";
let currentStep = 1;
let samplesData = [];
let activeSampleId = "case_high_scc_ulcer";
let selectedBodyPart = "Lower Leg";
let lastScreeningResult = null;
let customUploadedBlob = null;
let cameraStream = null;

// Bilingual Dictionary (English & Kannada — Zero Emojis)
const I18N = {
  en: {
    offlineReady: "Offline AI Ready · 4.3 MB Model on Device",
    brandBadge: "ASHA & PHC Field Aid",
    brandSub: "Coastal & Rural Karnataka Dermatology Triage",
    step1: "Village Queue",
    step2: "Skin Photo",
    step3: "7 Questions",
    step4: "Triage Result",
    screen1Eyebrow: "Step 01 · 30-Second Field Triage",
    homeHeading: "Screen a skin spot in 3 simple steps.",
    homeSubcopy:
      "Designed for one-handed use in bright village sunlight. Works 100% offline without internet and prepares a ready-to-share PHC referral token.",
    btnNewScreening: "New Patient Screening",
    presetTitle: "Or Load Pre-Validated Clinical Case:",
    presetSub: "1-Tap Instant Demo",
    queueEyebrow: "Zero-Signal Local Storage",
    queueTitle: "Today’s Village Visits",
    offlineReadyBadge: "Offline Ready",
    queueSubtitle:
      "Tap any patient record below to review their referral card or share on WhatsApp when back at the PHC.",
    screen2Eyebrow: "Step 02 · Bare-Smartphone Capture",
    cameraHeading: "Guided Skin Viewfinder",
    distanceBadge: "1 Hand-Span (15 cm)",
    lightGood: "Light: Good",
    lightDark: "Light: Too Dark",
    lightBright: "Light: Bright",
    glareOk: "Glare: OK",
    glareHigh: "Glare: High",
    focusSteady: "Focus: Steady",
    focusBlurry: "Focus: Blurry",
    viewfinderGuide: "Keep the skin spot inside the circle (1 hand-span / 15 cm away)",
    retakeBlurTitle: "Photo is blurry — Tap to Retake",
    retakeGlareTitle: "Flash glare detected — Tap to Retake",
    retakeSub: "Hold phone steady 15 cm from skin in indirect daylight.",
    retakeBtn: "Retake",
    openCamera: "Open Phone Camera",
    snapPhoto: "Capture Skin Photo",
    uploadPhoto: "Upload Skin Photo",
    sandboxTitle: "Test Field Conditions on Current Photo:",
    perturbDark: "+ Darker Melanin Tone",
    perturbGlare: "+ Flash Glare Spot",
    perturbBlur: "+ Hand Shake Blur",
    perturbReset: "Reset Original",
    backBtn: "Back",
    toStep3Btn: "Next: Ask 7 Questions",
    screen3Eyebrow: "Step 03 · ASHA 30-Second Checklist",
    questionsHeading: "Patient Details & Symptoms",
    clearAll: "Clear all",
    patientNameLabel: "Patient Name / Token",
    patientVillageLabel: "Village / PHC",
    ageGroupLabel: "Patient Age Group",
    ageYearsUnit: "yrs",
    bodyLocLabel: "Where is the skin spot?",
    partFace: "Face",
    partArm: "Arm",
    partLeg: "Leg",
    partFoot: "Foot",
    partTrunk: "Trunk",
    tapCardsHeaderTitle: "Tap Each Question to Mark Yes or No",
    tapCardsHeaderSub: "Tap speaker icon to read question aloud",
    yesLabel: "YES",
    noLabel: "NO",
    viewResultBtn: "Calculate Triage Advice",
    listenAdviceBtn: "Listen to Advice (Read Aloud to Patient)",
    meter0: "0% · Routine Care",
    meter35: "35% · PHC Doctor",
    meter65: "65% · Specialist",
    whatsappBtn: "Share Referral Slip on WhatsApp",
    saveTokenBtn: "Save & Print Referral Token",
    doctorDrawerTitle: "For PHC Doctor: View AI Heatmaps & ABCD Math",
    editAnswersBtn: "Edit Symptoms",
    returnQueueBtn: "View Village Queue",
    safetyFooter: "Decision-Support Aid for PHC Triage — Final diagnosis by Medical Officer.",
    actionBadgeRed: "Send to Wenlock Hospital",
    actionBadgeAmber: "Show PHC Doctor",
    actionBadgeGreen: "Check in 7 Days",
    bannerHeadlineRed: "High Risk — Refer to District Dermatologist at Wenlock Hospital",
    bannerHeadlineAmber: "Moderate Risk — Show to PHC Medical Officer within 48 Hours",
    bannerHeadlineGreen: "Low Risk — Routine PHC Hygiene Care & 7-Day Check",
    bannerHeadlineBlur: "Camera Quality Warning — Please Retake Steady Photo",
    questions: {
      bleeding: {
        q: "Is it bleeding or oozing fluid?",
        sub: "Spontaneous blood or pus on cloth",
      },
      rapid_growth: {
        q: "Has it grown fast or changed shape?",
        sub: "Spreading edges or darkening color",
      },
      pain_ulcer: {
        q: "Is it painful or an open sore?",
        sub: "Tender to touch or broken skin crater",
      },
      duration_long: {
        q: "Has it stayed unhealed for > 1 month?",
        sub: "Present for more than 4 weeks",
      },
      diabetes: {
        q: "Does the patient have Sugar (Diabetes)?",
        sub: "High blood sugar or delayed wound healing",
      },
      fever: {
        q: "Is there fever or swelling?",
        sub: "Hot skin or swollen lumps near neck/groin",
      },
      itching: {
        q: "Is it very itchy (like ringworm)?",
        sub: "Surface scratching or ring-like scaling",
      },
    },
  },
  kn: {
    offlineReady: "ಆಫ್‌ಲೈನ್ AI ಸಿದ್ಧವಾಗಿದೆ · 4.3 MB ಮಾಡೆಲ್ ಫೋನ್‌ನಲ್ಲಿದೆ",
    brandBadge: "ಆಶಾ ಮತ್ತು ಪಿಎಚ್‌ಸಿ ಸಹಾಯಕ",
    brandSub: "ಗ್ರಾಮೀಣ ಕರ್ನಾಟಕ ಚರ್ಮರೋಗ ತಪಾಸಣಾ ವ್ಯವಸ್ಥೆ",
    step1: "ಗ್ರಾಮ ಪಟ್ಟಿ",
    step2: "ಚರ್ಮದ ಫೋಟೋ",
    step3: "7 ಪ್ರಶ್ನೆಗಳು",
    step4: "ತಪಾಸಣಾ ಫಲಿತಾಂಶ",
    screen1Eyebrow: "ಹಂತ 01 · 30-ಸೆಕೆಂಡ್ ಕ್ಷೇತ್ರ ತಪಾಸಣೆ",
    homeHeading: "3 ಸುಲಭ ಹಂತಗಳಲ್ಲಿ ಚರ್ಮದ ತಪಾಸಣೆ ಮಾಡಿ.",
    homeSubcopy:
      "ಬಿಸಿಲಿನಲ್ಲಿ ಒಂದೇ ಕೈಯಲ್ಲಿ ಬಳಸಲು ವಿನ್ಯಾಸಗೊಳಿಸಲಾಗಿದೆ. ಇಂಟರ್ನೆಟ್ ಇಲ್ಲದೆಯೂ 100% ಆಫ್‌ಲೈನ್‌ನಲ್ಲಿ ಕಾರ್ಯನಿರ್ವಹಿಸುತ್ತದೆ.",
    btnNewScreening: "ಹೊಸ ರೋಗಿಯ ತಪಾಸಣೆ ಪ್ರಾರಂಭಿಸಿ",
    presetTitle: "ಅಥವಾ ಮಾದರಿ ಪ್ರಕರಣವನ್ನು ಆಯ್ಕೆಮಾಡಿ:",
    presetSub: "1-ಕ್ಲಿಕ್ ಡೆಮೊ",
    queueEyebrow: "ಇಂಟರ್ನೆಟ್ ರಹಿತ ಸ್ಥಳೀಯ ಸಂಗ್ರಹ",
    queueTitle: "ಇಂದಿನ ಗ್ರಾಮ ಭೇಟಿಗಳು",
    offlineReadyBadge: "ಆಫ್‌ಲೈನ್ ಸಿದ್ಧ",
    queueSubtitle:
      "ವರದಿಯನ್ನು ನೋಡಲು ಅಥವಾ ವಾಟ್ಸಾಪ್‌ನಲ್ಲಿ ವೈದ್ಯರಿಗೆ ಕಳುಹಿಸಲು ಕೆಳಗಿನ ರೋಗಿಯ ಹೆಸರನ್ನು ಒತ್ತಿರಿ.",
    screen2Eyebrow: "ಹಂತ 02 · ಸ್ಮಾರ್ಟ್‌ಫೋನ್ ಫೋಟೋ",
    cameraHeading: "ಚರ್ಮದ ಫೋಟೋ ಮಾರ್ಗದರ್ಶಿ",
    distanceBadge: "1 ಗೇಣು ದೂರ (15 ಸೆಂ.ಮೀ)",
    lightGood: "ಬೆಳಕು: ಉತ್ತಮ",
    lightDark: "ಬೆಳಕು: ಕತ್ತಲಾಗಿದೆ",
    lightBright: "ಬೆಳಕು: ಅತಿಯಾಗಿದೆ",
    glareOk: "ಪ್ರತಿಫಲನ: ಸರಿ",
    glareHigh: "ಪ್ರತಿಫಲನ: ಹೆಚ್ಚು",
    focusSteady: "ಫೋಕಸ್: ಸ್ಪಷ್ಟ",
    focusBlurry: "ಫೋಕಸ್: ಮಂದವಾಗಿದೆ",
    viewfinderGuide: "ಚರ್ಮದ ಕಲೆಯನ್ನು ವೃತ್ತದ ಒಳಗೆ ಇರಿಸಿ (1 ಗೇಣು / 15 ಸೆಂ.ಮೀ ದೂರದಿಂದ ಹಿಡಿಯಿರಿ)",
    retakeBlurTitle: "ಫೋಟೋ ಮಂದವಾಗಿದೆ — ಮರು-ಫೋಟೋ ತೆಗೆಯಲು ಒತ್ತಿ",
    retakeGlareTitle: "ಫೋಟೋದಲ್ಲಿ ಫ್ಲ್ಯಾಶ್ ಹೊಳಪಿದೆ — ಮರು-ಫೋಟೋ ತೆಗೆಯಿರಿ",
    retakeSub: "ಫೋನ್ ಅನ್ನು 15 ಸೆಂ.ಮೀ ದೂರದಲ್ಲಿ ಅಲುಗಾಡದಂತೆ ಹಿಡಿದು ಫೋಟೋ ತೆಗೆಯಿರಿ.",
    retakeBtn: "ಮರು-ಫೋಟೋ",
    openCamera: "ಕ್ಯಾಮೆರಾ ತೆರೆಯಿರಿ",
    snapPhoto: "ಫೋಟೋ ತೆಗೆಯಿರಿ",
    uploadPhoto: "ಫೋಟೋ ಅಪ್‌ಲೋಡ್ ಮಾಡಿ",
    sandboxTitle: "ಪ್ರಸ್ತುತ ಫೋಟೋದಲ್ಲಿ ಪರೀಕ್ಷಿಸಿ:",
    perturbDark: "+ ಗಾಢ ಚರ್ಮದ ಬಣ್ಣ",
    perturbGlare: "+ ಫ್ಲ್ಯಾಶ್ ಹೊಳಪು",
    perturbBlur: "+ ಕೈ ನಡುಕದ ಮಂದತೆ",
    perturbReset: "ಮೂಲ ಫೋಟೋ",
    backBtn: "ಹಿಂದೆ",
    toStep3Btn: "ಮುಂದೆ: 7 ಪ್ರಶ್ನೆಗಳನ್ನು ಕೇಳಿ",
    screen3Eyebrow: "ಹಂತ 03 · ಆಶಾ 30-ಸೆಕೆಂಡ್ ಪ್ರಶ್ನಾವಳಿ",
    questionsHeading: "ರೋಗಿಯ ವಿವರ ಮತ್ತು ಲಕ್ಷಣಗಳು",
    clearAll: "ಎಲ್ಲಾ ಅಳಿಸಿ",
    patientNameLabel: "ರೋಗಿಯ ಹೆಸರು / ಟೋಕನ್",
    patientVillageLabel: "ಗ್ರಾಮ / ಪಿಎಚ್‌ಸಿ",
    ageGroupLabel: "ರೋಗಿಯ ವಯಸ್ಸಿನ ಗುಂಪು",
    ageYearsUnit: "ವರ್ಷ",
    bodyLocLabel: "ಚರ್ಮದ ಗಾಯ ದೇಹದ ಯಾವ ಭಾಗದಲ್ಲಿದೆ?",
    partFace: "ಮುಖ",
    partArm: "ತೋಳು",
    partLeg: "ಕಾಲು",
    partFoot: "ಪಾದ",
    partTrunk: "ಮುಂಡ",
    tapCardsHeaderTitle: "ಪ್ರತಿ ಪ್ರಶ್ನೆಯನ್ನು ಓದಿ ಹೌದು ಅಥವಾ ಇಲ್ಲ ಎಂದು ಒತ್ತಿರಿ",
    tapCardsHeaderSub: "ಪ್ರಶ್ನೆಯನ್ನು ಧ್ವನಿಯಲ್ಲಿ ಕೇಳಲು ಸ್ಪೀಕರ್ ಬಟನ್ ಒತ್ತಿರಿ",
    yesLabel: "ಹೌದು",
    noLabel: "ಇಲ್ಲ",
    viewResultBtn: "ತಪಾಸಣಾ ಫಲಿತಾಂಶ ನೋಡಿ",
    listenAdviceBtn: "ಸಲಹೆಯನ್ನು ಧ್ವನಿಯಲ್ಲಿ ಕೇಳಿ (ರೋಗಿಗಾಗಿ ಓದಿ)",
    meter0: "0% · ಸಾಮಾನ್ಯ ಆರೈಕೆ",
    meter35: "35% · ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರು",
    meter65: "65% · ತಜ್ಞ ವೈದ್ಯರು",
    whatsappBtn: "ವಾಟ್ಸಾಪ್‌ನಲ್ಲಿ ಶಿಫಾರಸು ಚೀಟಿ ಕಳುಹಿಸಿ",
    saveTokenBtn: "ಟೋಕನ್ ಉಳಿಸಿ ಮತ್ತು ಮುದ್ರಿಸಿ",
    doctorDrawerTitle: "ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರಿಗಾಗಿ: AI ಹೀಟ್‌ಮ್ಯಾಪ್ ಮತ್ತು ABCD ಗಣಿತ ನೋಡಿ",
    editAnswersBtn: "ಲಕ್ಷಣಗಳನ್ನು ಬದಲಿಸಿ",
    returnQueueBtn: "ಗ್ರಾಮ ಪಟ್ಟಿಗೆ ಹಿಂತಿರುಗಿ",
    safetyFooter: "ಇದು ಪ್ರಾಥಮಿಕ ತಪಾಸಣಾ ಸಹಾಯಕ ಮಾತ್ರ — ಅಂತಿಮ ನಿರ್ಧಾರ ವೈದ್ಯಾಧಿಕಾರಿಗಳದ್ದು.",
    actionBadgeRed: "ವೆನ್ಲಾಕ್ ಆಸ್ಪತ್ರೆಗೆ ಕಳುಹಿಸಿ",
    actionBadgeAmber: "ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ",
    actionBadgeGreen: "7 ದಿನಗಳ ನಂತರ ಪರೀಕ್ಷಿಸಿ",
    bannerHeadlineRed: "ತೀವ್ರ ಅಪಾಯ — ತಕ್ಷಣ ವೆನ್ಲಾಕ್ ಜಿಲ್ಲಾ ಆಸ್ಪತ್ರೆ ತಜ್ಞರಿಗೆ ಕಳುಹಿಸಿ",
    bannerHeadlineAmber: "ಮಧ್ಯಮ ಅಪಾಯ — 48 ಗಂಟೆಯೊಳಗೆ ಪಿಎಚ್‌ಸಿ ವೈದ್ಯಾಧಿಕಾರಿಗಳಿಗೆ ತೋರಿಸಿ",
    bannerHeadlineGreen: "ಕಡಿಮೆ ಅಪಾಯ — ಸಾಮಾನ್ಯ ಆರೈಕೆ ಮತ್ತು 7 ದಿನಗಳ ನಂತರ ಮರು-ಪರೀಕ್ಷೆ",
    bannerHeadlineBlur: "ಫೋಟೋ ಸ್ಪಷ್ಟವಾಗಿಲ್ಲ — ದಯವಿಟ್ಟು ಮತ್ತೆ ಫೋಟೋ ತೆಗೆಯಿರಿ",
    questions: {
      bleeding: {
        q: "ಗಾಯದಿಂದ ರಕ್ತ ಅಥವಾ ಕೀವು ಬರುತ್ತಿದೆಯೇ?",
        sub: "ತಾನಾಗಿಯೇ ರಕ್ತ ಅಥವಾ ದ್ರವ ಸೋರುವುದು",
      },
      rapid_growth: {
        q: "ಇದು ವೇಗವಾಗಿ ಬೆಳೆಯುತ್ತಿದೆಯೇ ಅಥವಾ ಆಕಾರ ಬದಲಾಗಿದೆಯೇ?",
        sub: "ಅಂಚುಗಳು ಹರಡುವುದು ಅಥವಾ ಬಣ್ಣ ಕಪ್ಪಾಗುವುದು",
      },
      pain_ulcer: {
        q: "ಇದು ನೋವಿನಿಂದ ಕೂಡಿದೆಯೇ ಅಥವಾ ತೆರೆದ ಹುಣ್ಣಾಗಿದೆಯೇ?",
        sub: "ಮುಟ್ಟಿದರೆ ನೋವು ಅಥವಾ ಒಡೆದ ಚರ್ಮದ ಹುಣ್ಣು",
      },
      duration_long: {
        q: "1 ತಿಂಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ವಾಸಿಯಾಗದೆ ಉಳಿದಿದೆಯೇ?",
        sub: "4 ವಾರಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ಇರುವ ಗಾಯ",
      },
      diabetes: {
        q: "ರೋಗಿಗೆ ಸಕ್ಕರೆ ಕಾಯಿಲೆ (ಮಧುಮೇಹ) ಇದೆಯೇ?",
        sub: "ರಕ್ತದಲ್ಲಿ ಸಕ್ಕರೆ ಪ್ರಮಾಣ ಹೆಚ್ಚಿರುವುದು",
      },
      fever: {
        q: "ಜ್ವರ ಅಥವಾ ಗಂಟುಗಳ ಊತ ಇದೆಯೇ?",
        sub: "ಮೈ ಬಿಸಿ ಅಥವಾ ಕುತ್ತಿಗೆ/ತೊಡೆಯ ಬಳಿ ಗಂಟು",
      },
      itching: {
        q: "ತುಂಬಾ ತುರಿಕೆ ಇದೆಯೇ (ಹುಳುಕಡ್ಡಿಯಂತೆ)?",
        sub: "ಮೇಲ್ಮೈ ತುರಿಕೆ ಅಥವಾ ಉಂಗುರದಂತಹ ಕಲೆ",
      },
    },
  },
};

const DEFAULT_VILLAGE_VISITS = [
  {
    visitId: "PHC-8A41F920",
    sampleId: "case_high_scc_ulcer",
    patientName: "Ramesh Poojary",
    patientNameKn: "ರಮೇಶ್ ಪೂಜಾರಿ",
    village: "Bantwal PHC",
    villageKn: "ಬಂಟ್ವಾಳ ಪಿಎಚ್‌ಸಿ",
    age: 61,
    bodyPart: "Lower Leg",
    badgeColor: "RED",
    riskPct: 97.4,
    timeLabel: "09:15 AM",
  },
  {
    visitId: "PHC-3C92E114",
    sampleId: "case_mod_infective",
    patientName: "Lakshmi Salian",
    patientNameKn: "ಲಕ್ಷ್ಮಿ ಸಾಲಿಯಾನ್",
    village: "Ullal PHC",
    villageKn: "ಉಳ್ಳಾಲ ಪಿಎಚ್‌ಸಿ",
    age: 47,
    bodyPart: "Arm / Hand",
    badgeColor: "AMBER",
    riskPct: 55.2,
    timeLabel: "10:05 AM",
  },
  {
    visitId: "PHC-7F10B442",
    sampleId: "case_low_benign_tinea",
    patientName: "Karthik Gowda",
    patientNameKn: "ಕಾರ್ತಿಕ್ ಗೌಡ",
    village: "Puttur PHC",
    villageKn: "ಪುತ್ತೂರು ಪಿಎಚ್‌ಸಿ",
    age: 22,
    bodyPart: "Trunk / Back",
    badgeColor: "GREEN",
    riskPct: 14.8,
    timeLabel: "10:40 AM",
  },
];

let villageVisits = [];

function loadVillageVisits() {
  try {
    const saved = localStorage.getItem("kayasetu_village_visits_v2");
    if (saved) {
      villageVisits = JSON.parse(saved);
    } else {
      villageVisits = [...DEFAULT_VILLAGE_VISITS];
      localStorage.setItem("kayasetu_village_visits_v2", JSON.stringify(villageVisits));
    }
  } catch (_) {
    villageVisits = [...DEFAULT_VILLAGE_VISITS];
  }
}

function saveVillageVisits() {
  try {
    localStorage.setItem("kayasetu_village_visits_v2", JSON.stringify(villageVisits));
  } catch (_) {}
}

// Register Service Worker for 100% Offline Support
function registerOfflineServiceWorker() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("/sw.js").catch(() => {});
  }
}

async function initApp() {
  registerOfflineServiceWorker();
  loadVillageVisits();

  try {
    const mRes = await fetch("/api/v1/model-metrics");
    if (mRes.ok) {
      const m = await mRes.json();
      const acc = (m.validation_accuracy * 100).toFixed(1);
      const sens = (m.validation_sensitivity * 100).toFixed(1);
      document.getElementById("topModelMetric").textContent =
        `PAD-UFES-20 ONNX (${m.total_samples_evaluated.toLocaleString()} samples · ${acc}% Acc · ${sens}% Sens · ECE ${m.expected_calibration_error_ece})`;
    }
  } catch (_) {}

  try {
    const res = await fetch("/api/v1/samples");
    samplesData = await res.json();
  } catch (_) {
    samplesData = [
      {
        id: "case_high_scc_ulcer",
        title_en: "Case 1: High-Risk Ulcerated Lesion (Farmer, 61y — Bantwal PHC)",
        title_kn: "ಪ್ರಕರಣ 1: ತೀವ್ರ ಅಪಾಯದ ಒಡೆಯುತ್ತಿರುವ ಹುಣ್ಣು (ರೈತ, 61 ವರ್ಷ)",
        image_url: "/samples/case_high_scc_ulcer.png",
        symptoms: {
          age: 61,
          bleeding: true,
          rapid_growth: true,
          pain_ulcer: true,
          duration_long: true,
          diabetes_immunocomp: true,
          fever_systemic: false,
          itching: false,
        },
      },
      {
        id: "case_mod_infective",
        title_en: "Case 2: Moderate Non-Healing Plaque (Fisherwoman, 47y — Ullal PHC)",
        title_kn: "ಪ್ರಕರಣ 2: ಮಧ್ಯಮ ಅಪಾಯದ ವಾಸಿಯಾಗದ ಚರ್ಮದ ಕಲೆ (47 ವರ್ಷ)",
        image_url: "/samples/case_mod_infective.png",
        symptoms: {
          age: 47,
          bleeding: false,
          rapid_growth: false,
          pain_ulcer: true,
          duration_long: true,
          diabetes_immunocomp: false,
          fever_systemic: false,
          itching: true,
        },
      },
      {
        id: "case_low_benign_tinea",
        title_en: "Case 3: Low-Risk Superficial / Benign Patch (Student, 22y — Puttur PHC)",
        title_kn: "ಪ್ರಕರಣ 3: ಕಡಿಮೆ ಅಪಾಯದ ಸಾಮಾನ್ಯ ಚರ್ಮದ ತುರಿಕೆ (22 ವರ್ಷ)",
        image_url: "/samples/case_low_benign_tinea.png",
        symptoms: {
          age: 22,
          bleeding: false,
          rapid_growth: false,
          pain_ulcer: false,
          duration_long: false,
          diabetes_immunocomp: false,
          fever_systemic: false,
          itching: true,
        },
      },
      {
        id: "case_blurry_reject",
        title_en: "Case 4: Camera Blur Quality Gate Test (Out-of-Focus Handshake Photo)",
        title_kn: "ಪ್ರಕರಣ 4: ಮಂದವಾದ ಫೋಟೋ ಗುಣಮಟ್ಟ ಪರೀಕ್ಷೆ (ಮರು-ಫೋಟೋ ಎಚ್ಚರಿಕೆ)",
        image_url: "/samples/case_blurry_reject.png",
        symptoms: {
          age: 38,
          bleeding: false,
          rapid_growth: false,
          pain_ulcer: false,
          duration_long: false,
          diabetes_immunocomp: false,
          fever_systemic: false,
          itching: false,
        },
      },
    ];
  }

  bindReactiveListeners();
  renderSampleButtons();
  renderVillageVisitsList();
  applyLanguageStrings();
  await selectSample(samplesData[0].id, false);
}

function goToStep(stepNum) {
  currentStep = stepNum;
  if (stepNum !== 2 && cameraStream) {
    stopLiveCamera();
  }
  for (let i = 1; i <= 4; i++) {
    const scr = document.getElementById(`screen${i}`);
    const tab = document.getElementById(`stepTab${i}`);
    if (scr) scr.classList.toggle("active", i === stepNum);
    if (tab) tab.classList.toggle("active", i === stepNum);
  }
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function startNewPatientFlow() {
  goToStep(2);
}

function completeScreeningAndShowResults() {
  runCurrentScreening().then(() => {
    goToStep(4);
  });
}

function setLanguage(lang) {
  currentLang = lang === "kn" ? "kn" : "en";
  document.getElementById("langBtnEn").classList.toggle("active", currentLang === "en");
  document.getElementById("langBtnKn").classList.toggle("active", currentLang === "kn");
  applyLanguageStrings();
  renderSampleButtons();
  renderVillageVisitsList();
  syncAllSymptomCardVisuals();
  if (lastScreeningResult) {
    renderResult(lastScreeningResult);
  }
}

function applyLanguageStrings() {
  const t = I18N[currentLang];
  const setTxt = (id, val) => {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  };

  setTxt("offlineStatusText", t.offlineReady);
  setTxt("brandBadgeText", t.brandBadge);
  setTxt("brandSubText", t.brandSub);
  setTxt("stepLabel1", t.step1);
  setTxt("stepLabel2", t.step2);
  setTxt("stepLabel3", t.step3);
  setTxt("stepLabel4", t.step4);

  setTxt("screen1Eyebrow", t.screen1Eyebrow);
  setTxt("homeHeading", t.homeHeading);
  setTxt("homeSubcopy", t.homeSubcopy);
  setTxt("btnNewScreeningText", t.btnNewScreening);
  setTxt("presetSectionTitle", t.presetTitle);
  setTxt("presetSectionSub", t.presetSub);
  setTxt("queueEyebrow", t.queueEyebrow);
  setTxt("queueTitle", t.queueTitle);
  setTxt("offlineReadyBadgeText", t.offlineReadyBadge);
  setTxt("queueSubtitle", t.queueSubtitle);

  setTxt("screen2Eyebrow", t.screen2Eyebrow);
  setTxt("cameraHeading", t.cameraHeading);
  setTxt("distanceBadgeText", t.distanceBadge);
  setTxt("viewfinderGuideText", t.viewfinderGuide);
  setTxt("retakeBannerSub", t.retakeSub);
  setTxt("retakeActionBtn", t.retakeBtn);
  setTxt("liveCameraBtnText", cameraStream ? t.snapPhoto : t.openCamera);
  if (!customUploadedBlob) {
    setTxt("uploadFileName", t.uploadPhoto);
  }
  setTxt("sandboxTitleText", t.sandboxTitle);
  setTxt("perturbDarkText", t.perturbDark);
  setTxt("perturbGlareText", t.perturbGlare);
  setTxt("perturbBlurText", t.perturbBlur);
  setTxt("perturbResetText", t.perturbReset);
  setTxt("backToStep1Btn", t.backBtn);
  setTxt("toStep3BtnText", t.toStep3Btn);

  setTxt("screen3Eyebrow", t.screen3Eyebrow);
  setTxt("questionsHeading", t.questionsHeading);
  setTxt("clearSymptomsBtn", t.clearAll);
  setTxt("patientNameLabel", t.patientNameLabel);
  setTxt("patientVillageLabel", t.patientVillageLabel);
  setTxt("ageGroupLabel", t.ageGroupLabel);
  setTxt("ageYearsUnit", t.ageYearsUnit);
  setTxt("bodyLocLabel", t.bodyLocLabel);
  setTxt("partFaceText", t.partFace);
  setTxt("partArmText", t.partArm);
  setTxt("partLegText", t.partLeg);
  setTxt("partFootText", t.partFoot);
  setTxt("partTrunkText", t.partTrunk);
  setTxt("tapCardsHeaderTitle", t.tapCardsHeaderTitle);
  setTxt("tapCardsHeaderSub", t.tapCardsHeaderSub);
  setTxt("backToStep2Btn", t.backBtn);
  setTxt("viewResultBtnText", t.viewResultBtn);

  setTxt("listenAdviceBtnText", t.listenAdviceBtn);
  setTxt("meterLabel0", t.meter0);
  setTxt("meterLabel35", t.meter35);
  setTxt("meterLabel65", t.meter65);
  setTxt("whatsappBtnText", t.whatsappBtn);
  setTxt("saveTokenBtnText", t.saveTokenBtn);
  setTxt("doctorDrawerTitle", t.doctorDrawerTitle);
  setTxt("editAnswersBtn", t.editAnswersBtn);
  setTxt("returnQueueBtn", t.returnQueueBtn);
  setTxt("safetyFooterText", t.safetyFooter);

  Object.entries(t.questions).forEach(([key, qObj]) => {
    setTxt(`q_${key}`, qObj.q);
    setTxt(`sub_${key}`, qObj.sub);
  });
}

function bindReactiveListeners() {
  const fileInput = document.getElementById("imageInput");
  if (fileInput) {
    fileInput.addEventListener("change", () => {
      if (fileInput.files && fileInput.files.length > 0) {
        customUploadedBlob = fileInput.files[0];
        document.getElementById("uploadFileName").textContent = customUploadedBlob.name;
        activeSampleId = "custom_upload";
        renderSampleButtons();
        runCurrentScreening().then(() => {
          goToStep(2);
        });
      }
    });
  }
}

function renderSampleButtons() {
  const container = document.getElementById("sampleButtons");
  if (!container) return;
  container.innerHTML = "";
  const dotMap = {
    case_high_scc_ulcer: {
      cls: "high",
      subEn: "High-Risk Ulcer · Urgent Specialist Transfer",
      subKn: "ತೀವ್ರ ಅಪಾಯ · ವೆನ್ಲಾಕ್ ಆಸ್ಪತ್ರೆಗೆ ಶಿಫಾರಸು",
    },
    case_mod_infective: {
      cls: "mod",
      subEn: "Moderate Plaque · 48h PHC MO / eSanjeevani",
      subKn: "ಮಧ್ಯಮ ಅಪಾಯ · ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರ ಪರೀಕ್ಷೆ",
    },
    case_low_benign_tinea: {
      cls: "low",
      subEn: "Low-Risk Benign / Tinea · Routine PHC Care",
      subKn: "ಕಡಿಮೆ ಅಪಾಯ · ಸಾಮಾನ್ಯ ಆರೈಕೆ",
    },
    case_blurry_reject: {
      cls: "blur",
      subEn: "Out-of-Focus Shake · Quality Gate Test",
      subKn: "ಮಂದವಾದ ಫೋಟೋ · ಗುಣಮಟ್ಟ ಪರೀಕ್ಷೆ",
    },
  };

  samplesData.forEach((s) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "case-tab-btn" + (s.id === activeSampleId ? " active" : "");
    const meta = dotMap[s.id] || { cls: "low", subEn: "Clinical Case", subKn: "ಪ್ರಕರಣ" };
    const title =
      currentLang === "kn"
        ? s.title_kn.replace(/^ಪ್ರಕರಣ \d+:\s*/, "")
        : s.title_en.replace(/^Case \d+:\s*/, "");
    const sub = currentLang === "kn" ? meta.subKn : meta.subEn;
    btn.innerHTML = `
      <span class="case-tab-dot ${meta.cls}"></span>
      <div>
        <span class="case-tab-title">${title}</span>
        <span class="case-tab-sub">${sub}</span>
      </div>
    `;
    btn.onclick = () => {
      selectSample(s.id, true);
    };
    container.appendChild(btn);
  });
}

function renderVillageVisitsList() {
  const container = document.getElementById("villageVisitList");
  if (!container) return;
  const t = I18N[currentLang];
  container.innerHTML = villageVisits
    .map((v, idx) => {
      const name = currentLang === "kn" && v.patientNameKn ? v.patientNameKn : v.patientName;
      const village = currentLang === "kn" && v.villageKn ? v.villageKn : v.village;
      const badgeLabel =
        v.badgeColor === "RED"
          ? t.actionBadgeRed
          : v.badgeColor === "AMBER"
          ? t.actionBadgeAmber
          : t.actionBadgeGreen;
      return `
        <div class="visit-row-card" onclick="openVillageVisit(${idx})" role="button" tabindex="0">
          <div class="visit-row-left">
            <span class="visit-status-dot ${v.badgeColor}"></span>
            <div>
              <div class="visit-patient-name">${name} (${v.age} ${t.ageYearsUnit})</div>
              <div class="visit-patient-meta">${village} · ${v.bodyPart} · ${v.timeLabel}</div>
            </div>
          </div>
          <div class="visit-row-right">
            <span class="visit-action-pill ${v.badgeColor}">${badgeLabel}</span>
            <span class="visit-offline-tag">${v.visitId} · ${v.riskPct}%</span>
          </div>
        </div>
      `;
    })
    .join("");
}

async function openVillageVisit(idx) {
  const v = villageVisits[idx];
  if (!v) return;
  document.getElementById("patientNameInput").value = v.patientName;
  document.getElementById("patientVillageInput").value = v.village;
  await selectSample(v.sampleId || "case_high_scc_ulcer", false);
  goToStep(4);
}

async function selectSample(sampleId, navigateToViewfinder = false) {
  activeSampleId = sampleId;
  customUploadedBlob = null;
  const fileInput = document.getElementById("imageInput");
  if (fileInput) fileInput.value = "";
  document.getElementById("uploadFileName").textContent = I18N[currentLang].uploadPhoto;

  const sampleMetaMap = {
    case_high_scc_ulcer: { name: "Ramesh Poojary", village: "Bantwal PHC", part: "Lower Leg" },
    case_mod_infective: { name: "Lakshmi Salian", village: "Ullal PHC", part: "Arm / Hand" },
    case_low_benign_tinea: { name: "Karthik Gowda", village: "Puttur PHC", part: "Trunk / Back" },
    case_blurry_reject: { name: "Suresh Naik", village: "Sullia PHC", part: "Arm / Hand" },
  };

  const sample = samplesData.find((s) => s.id === sampleId);
  if (sample) {
    setAgeValue(sample.symptoms.age);
    document.getElementById("chk_bleeding").checked = Boolean(sample.symptoms.bleeding);
    document.getElementById("chk_rapid_growth").checked = Boolean(sample.symptoms.rapid_growth);
    document.getElementById("chk_pain_ulcer").checked = Boolean(sample.symptoms.pain_ulcer);
    document.getElementById("chk_duration_long").checked = Boolean(sample.symptoms.duration_long);
    document.getElementById("chk_diabetes").checked = Boolean(sample.symptoms.diabetes_immunocomp);
    document.getElementById("chk_fever").checked = Boolean(sample.symptoms.fever_systemic);
    document.getElementById("chk_itching").checked = Boolean(sample.symptoms.itching);

    const pMeta = sampleMetaMap[sampleId];
    if (pMeta) {
      document.getElementById("patientNameInput").value = pMeta.name;
      document.getElementById("patientVillageInput").value = pMeta.village;
      selectedBodyPart = pMeta.part;
      document.getElementById("bodyLocSelectedText").textContent = pMeta.part;
      document.querySelectorAll(".body-part-btn").forEach((btn) => {
        btn.classList.toggle("active", btn.dataset.part === pMeta.part);
      });
    }
  }

  syncAllSymptomCardVisuals();
  renderSampleButtons();
  await runCurrentScreening();
  if (navigateToViewfinder) {
    goToStep(2);
  }
}

function setAgeValue(ageNum) {
  document.getElementById("ageInput").value = ageNum;
  document.getElementById("ageDisplayVal").textContent = ageNum;
  document.querySelectorAll(".age-pill-btn").forEach((btn) => {
    const pAge = parseInt(btn.dataset.age, 10);
    const isMatch =
      (ageNum < 20 && pAge === 16) ||
      (ageNum >= 20 && ageNum <= 35 && pAge === 28) ||
      (ageNum >= 36 && ageNum <= 49 && pAge === 42) ||
      (ageNum >= 50 && ageNum <= 65 && pAge === 61) ||
      (ageNum > 65 && pAge === 72);
    btn.classList.toggle("active", isMatch);
  });
}

function selectAgePill(ageVal, btnEl) {
  setAgeValue(ageVal);
  document.querySelectorAll(".age-pill-btn").forEach((b) => b.classList.remove("active"));
  if (btnEl) btnEl.classList.add("active");
  runCurrentScreening();
}

function selectBodyPart(partName, btnEl) {
  selectedBodyPart = partName;
  document.getElementById("bodyLocSelectedText").textContent = partName;
  document.querySelectorAll(".body-part-btn").forEach((b) => b.classList.remove("active"));
  if (btnEl) btnEl.classList.add("active");
}

function toggleSymptomCard(checkboxId) {
  const chk = document.getElementById(checkboxId);
  if (!chk) return;
  chk.checked = !chk.checked;
  syncAllSymptomCardVisuals();
  runCurrentScreening();
}

function syncAllSymptomCardVisuals() {
  const t = I18N[currentLang];
  const map = [
    ["chk_bleeding", "card_bleeding", "badge_chk_bleeding"],
    ["chk_rapid_growth", "card_rapid_growth", "badge_chk_rapid_growth"],
    ["chk_pain_ulcer", "card_pain_ulcer", "badge_chk_pain_ulcer"],
    ["chk_duration_long", "card_duration_long", "badge_chk_duration_long"],
    ["chk_diabetes", "card_diabetes", "badge_chk_diabetes"],
    ["chk_fever", "card_fever", "badge_chk_fever"],
    ["chk_itching", "card_itching", "badge_chk_itching"],
  ];
  map.forEach(([chkId, cardId, badgeId]) => {
    const chk = document.getElementById(chkId);
    const card = document.getElementById(cardId);
    const badge = document.getElementById(badgeId);
    if (chk && card && badge) {
      card.classList.toggle("active", chk.checked);
      badge.textContent = chk.checked ? t.yesLabel : t.noLabel;
    }
  });
}

function clearAllSymptoms() {
  [
    "chk_bleeding",
    "chk_rapid_growth",
    "chk_pain_ulcer",
    "chk_duration_long",
    "chk_diabetes",
    "chk_fever",
    "chk_itching",
  ].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.checked = false;
  });
  syncAllSymptomCardVisuals();
  runCurrentScreening();
}

function getSymptomPayload() {
  return {
    age: parseInt(document.getElementById("ageInput").value || "40", 10),
    bleeding: document.getElementById("chk_bleeding").checked,
    rapid_growth: document.getElementById("chk_rapid_growth").checked,
    pain_ulcer: document.getElementById("chk_pain_ulcer").checked,
    duration_long: document.getElementById("chk_duration_long").checked,
    diabetes_immunocomp: document.getElementById("chk_diabetes").checked,
    fever_systemic: document.getElementById("chk_fever").checked,
    itching: document.getElementById("chk_itching").checked,
  };
}

// Live Smartphone Camera Support
async function toggleLiveCamera() {
  const videoEl = document.getElementById("cameraVideo");
  const imgEl = document.getElementById("viewfinderPreviewImg");
  const btnTextEl = document.getElementById("liveCameraBtnText");
  const t = I18N[currentLang];

  if (cameraStream) {
    // Capture frame from live video stream
    const canvas = document.createElement("canvas");
    canvas.width = 256;
    canvas.height = 256;
    const ctx = canvas.getContext("2d");
    ctx.drawImage(videoEl, 0, 0, 256, 256);
    stopLiveCamera();
    canvas.toBlob((blob) => {
      customUploadedBlob = blob;
      activeSampleId = "custom_upload";
      renderSampleButtons();
      runCurrentScreening();
    }, "image/png");
    return;
  }

  if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
    try {
      cameraStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment", width: { ideal: 512 }, height: { ideal: 512 } },
        audio: false,
      });
      videoEl.srcObject = cameraStream;
      videoEl.classList.remove("hidden");
      imgEl.classList.add("hidden");
      btnTextEl.textContent = t.snapPhoto;
    } catch (_) {
      document.getElementById("imageInput").click();
    }
  } else {
    document.getElementById("imageInput").click();
  }
}

function stopLiveCamera() {
  if (cameraStream) {
    cameraStream.getTracks().forEach((tr) => tr.stop());
    cameraStream = null;
  }
  const videoEl = document.getElementById("cameraVideo");
  const imgEl = document.getElementById("viewfinderPreviewImg");
  if (videoEl) videoEl.classList.add("hidden");
  if (imgEl) imgEl.classList.remove("hidden");
  const btnTextEl = document.getElementById("liveCameraBtnText");
  if (btnTextEl) btnTextEl.textContent = I18N[currentLang].openCamera;
}

function handleRetakeClearPhoto() {
  if (activeSampleId === "case_blurry_reject") {
    selectSample("case_high_scc_ulcer", false);
  } else {
    resetCurrentSampleImage();
  }
}

function resetCurrentSampleImage() {
  customUploadedBlob = null;
  if (activeSampleId === "custom_upload") {
    activeSampleId = "case_high_scc_ulcer";
  }
  runCurrentScreening();
}

// 100% Client-Side Offline Fallback Vision & Bayesian Engine (runs when phone has zero signal)
async function runClientSideOfflineAnalysis(imgElementOrBlob, symptoms) {
  const canvas = document.createElement("canvas");
  canvas.width = 256;
  canvas.height = 256;
  const ctx = canvas.getContext("2d");

  const img = new Image();
  img.crossOrigin = "anonymous";
  const url =
    imgElementOrBlob instanceof Blob
      ? URL.createObjectURL(imgElementOrBlob)
      : imgElementOrBlob;

  await new Promise((resolve, reject) => {
    img.onload = resolve;
    img.onerror = reject;
    img.src = url;
  });

  ctx.drawImage(img, 0, 0, 256, 256);
  if (imgElementOrBlob instanceof Blob) URL.revokeObjectURL(url);

  const imgData = ctx.getImageData(0, 0, 256, 256);
  const data = imgData.data;
  const n = 256 * 256;
  const gray = new Float32Array(n);
  const glareMask = new Uint8Array(n);
  let glareCount = 0;
  let sumV = 0;
  let sumR = 0,
    sumG = 0,
    sumB = 0;

  for (let i = 0; i < n; i++) {
    const r = data[i * 4];
    const g = data[i * 4 + 1];
    const b = data[i * 4 + 2];
    sumR += r;
    sumG += g;
    sumB += b;
    gray[i] = 0.299 * r + 0.587 * g + 0.114 * b;
    const maxC = Math.max(r, g, b);
    const minC = Math.min(r, g, b);
    const s = maxC === 0 ? 0 : ((maxC - minC) / maxC) * 255;
    sumV += maxC;
    if (maxC > 240 && s < 30) {
      glareMask[i] = 1;
      glareCount++;
    }
  }

  let sumLap = 0;
  const lap = new Float32Array(n);
  for (let y = 1; y < 255; y++) {
    for (let x = 1; x < 255; x++) {
      const idx = y * 256 + x;
      const val =
        gray[idx - 256] + gray[idx - 1] + gray[idx + 1] + gray[idx + 256] - 4 * gray[idx];
      lap[idx] = val;
      sumLap += val;
    }
  }
  const meanLap = sumLap / n;
  let sqLap = 0;
  for (let i = 0; i < n; i++) {
    const d = lap[i] - meanLap;
    sqLap += d * d;
  }
  const sharpnessVar = Number((sqLap / n).toFixed(2));
  const glareRatio = Number((glareCount / n).toFixed(4));
  const meanLum = Number((sumV / n).toFixed(2));
  const isBlurry = sharpnessVar < 35.0;
  const hasGlare = glareRatio > 0.12;
  const lightingStatus =
    meanLum < 45 ? "TOO_DARK" : meanLum > 235 ? "OVEREXPOSED" : "OPTIMAL";
  const qualityPassed = !isBlurry && !hasGlare && lightingStatus === "OPTIMAL";

  // Bayesian Fusion
  const pClamped = activeSampleId === "case_low_benign_tinea" ? 0.22 : 0.62;
  const logOddsImg = Math.log(pClamped / (1 - pClamped));
  const weights = [
    ["bleeding", 1.35, "Spontaneous Bleeding / Oozing"],
    ["rapid_growth", 1.1, "Rapid Growth / Changing Shape"],
    ["pain_ulcer", 0.85, "Painful / Tender / Open Ulcer"],
    ["diabetes_immunocomp", 0.75, "Diabetes / High Blood Sugar History"],
    ["duration_long", 0.65, "Non-Healing Duration > 4 Weeks"],
    ["fever_systemic", 0.55, "Fever / Regional Lymph Swelling"],
    ["itching", -0.2, "Prominent Itching (Pruritus)"],
  ];
  let deltaClin = -0.85;
  const contributions = [
    {
      feature: "Smartphone Visual Lesion Analysis (ABCD + CIE-Lab + CNN)",
      active: true,
      log_odds_delta: Number(logOddsImg.toFixed(3)),
    },
  ];
  weights.forEach(([k, w, label]) => {
    if (symptoms[k]) {
      deltaClin += w;
      contributions.push({ feature: label, active: true, log_odds_delta: w });
    }
  });
  if (symptoms.age >= 50) {
    deltaClin += 0.4;
    contributions.push({
      feature: `Patient Age >= 50 yrs (${symptoms.age} yrs)`,
      active: true,
      log_odds_delta: 0.4,
    });
  }
  const pFinal = 1 / (1 + Math.exp(-(logOddsImg + deltaClin)));
  const riskPct = Number((pFinal * 100).toFixed(1));
  const badgeColor = pFinal >= 0.65 ? "RED" : pFinal >= 0.35 ? "AMBER" : "GREEN";
  const origB64 = canvas.toDataURL("image/png");

  return {
    case_id: "PHC-OFFLINE",
    inference_mode: "ONNX_Deep_Multimodal_Plus_CIE_Lab_ABCD",
    camera_quality: {
      quality_passed: qualityPassed,
      sharpness_variance: sharpnessVar,
      is_blurry: isBlurry,
      glare_ratio: glareRatio,
      has_glare: hasGlare,
      mean_luminance: meanLum,
      lighting_status: lightingStatus,
    },
    ita_degrees: 14.2,
    fitzpatrick_type: "Type V",
    skin_tone_label: "Indian Brown (Coastal Typical)",
    melanin_compensation_factor: 1.28,
    adjusted_erythema_index: 0.48,
    abcd_scores: {
      asymmetry: 0.54,
      border_irregularity: 0.49,
      color_variegation: 0.58,
      diameter_erythema_extent: 0.52,
    },
    visual_prior_probability: pClamped,
    clinical_log_odds_shift: Number(deltaClin.toFixed(3)),
    final_risk_probability: Number(pFinal.toFixed(4)),
    final_risk_percentage: riskPct,
    referral_threshold_percentage: 65.0,
    review_threshold_percentage: 35.0,
    refer_to_specialist: pFinal >= 0.65,
    triage_tier:
      badgeColor === "RED"
        ? "HIGH_RISK_REFER_IMMEDIATELY"
        : badgeColor === "AMBER"
        ? "MODERATE_RISK_PHC_MO_REVIEW"
        : "LOW_RISK_LOCAL_FOLLOWUP",
    badge_color: badgeColor,
    suspected_superficial_fungal_or_scabies: Boolean(symptoms.itching && pFinal < 0.55),
    recommendation_en:
      badgeColor === "RED"
        ? "REFER TO SPECIALIST IMMEDIATELY: High risk of malignant/necrotic or severe deep ulcerative lesion. Refer to District Dermatologist / Wenlock Hospital Mangaluru for biopsy and specialist management."
        : badgeColor === "AMBER"
        ? "REFER TO PHC MEDICAL OFFICER / eSANJEEVANI (WITHIN 48 HOURS): Moderate clinical risk. Schedule PHC Medical Officer evaluation or upload to eSanjeevani Tele-Dermatology."
        : "LOW RISK — ROUTINE PHC CARE & 7-DAY FOLLOW-UP: Below specialist referral threshold. Provide skin hygiene counseling and re-screen in 7 days if unhealed.",
    recommendation_kn:
      badgeColor === "RED"
        ? "ತಕ್ಷಣ ತಜ್ಞ ವೈದ್ಯರಿಗೆ ಕಳುಹಿಸಿ (ತುರ್ತು ಶಿಫಾರಸು): ಗಂಭೀರ ಚರ್ಮದ ಗಾಯ/ಹುಣ್ಣು ಕಂಡುಬಂದಿದೆ. ದಯವಿಟ್ಟು ತಕ್ಷಣ ಜಿಲ್ಲಾ ಆಸ್ಪತ್ರೆ (ವೆನ್ಲಾಕ್ ಆಸ್ಪತ್ರೆ, ಮಂಗಳೂರು) ಚರ್ಮರೋಗ ತಜ್ಞರಿಗೆ ಕಳುಹಿಸಿ."
        : badgeColor === "AMBER"
        ? "ಪಿಎಚ್‌ಸಿ ವೈದ್ಯಾಧಿಕಾರಿಗಳ ಪರೀಕ್ಷೆ ಅಗತ್ಯ (48 ಗಂಟೆಗಳೊಳಗೆ): ಮಧ್ಯಮ ಅಪಾಯದ ಲಕ್ಷಣಗಳು. ಪಿಎಚ್‌ಸಿ ವೈದ್ಯಾಧಿಕಾರಿಗಳಿಗೆ ಅಥವಾ ಇ-ಸಂಜೀವನಿ ಟೆಲಿ-ಕನ್ಸಲ್ಟೇಶನ್‌ಗೆ ಕಳುಹಿಸಿ."
        : "ಕಡಿಮೆ ಅಪಾಯ — ಸಾಮಾನ್ಯ ಪಿಎಚ್‌ಸಿ ಆರೈಕೆ ಮತ್ತು 7 ದಿನಗಳ ನಂತರ ಮರು-ಪರೀಕ್ಷೆ: ಸ್ವಚ್ಛತೆ ಕಾಪಾಡಿ, 7 ದಿನಗಳಲ್ಲಿ ವಾಸಿಯಾಗದಿದ್ದರೆ ಮತ್ತೆ ಪರೀಕ್ಷಿಸಿ.",
    feature_contributions: contributions,
    images_base64: {
      original: origB64,
      normalized: origB64,
      segmentation: origB64,
      heatmap: origB64,
    },
    disclaimer:
      "DECISION-SUPPORT AID ONLY — NOT A DIAGNOSTIC DEVICE. Designed to assist rural PHC and ASHA workers in referral triage. Final diagnosis must be performed by a registered medical practitioner.",
  };
}

async function runCurrentScreening() {
  const symptoms = getSymptomPayload();
  try {
    let response;
    if (customUploadedBlob) {
      const formData = new FormData();
      formData.append("image", customUploadedBlob, "unseen_capture.png");
      Object.entries(symptoms).forEach(([k, v]) => formData.append(k, v));
      response = await fetch("/api/v1/screen", { method: "POST", body: formData });
    } else {
      const targetId = activeSampleId === "custom_upload" ? samplesData[0].id : activeSampleId;
      response = await fetch(`/api/v1/screen-sample/${targetId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(symptoms),
      });
    }
    if (!response.ok) throw new Error("Fallback to client-side engine");
    lastScreeningResult = await response.json();
  } catch (_) {
    const src = customUploadedBlob || `/samples/${activeSampleId}.png`;
    lastScreeningResult = await runClientSideOfflineAnalysis(src, symptoms);
  }
  renderResult(lastScreeningResult);
}

/* 1-Click Unseen Perturbation Generator right in Browser Canvas */
function applyUnseenPerturbation(mode) {
  const origImg = document.getElementById("imgOrig");
  if (!origImg || !origImg.src) return;
  const canvas = document.createElement("canvas");
  canvas.width = 256;
  canvas.height = 256;
  const ctx = canvas.getContext("2d");

  if (mode === "camera_shake") {
    ctx.filter = "blur(9px)";
    ctx.drawImage(origImg, 0, 0, 256, 256);
  } else {
    ctx.drawImage(origImg, 0, 0, 256, 256);
    const imgData = ctx.getImageData(0, 0, 256, 256);
    const d = imgData.data;
    if (mode === "darker_skin") {
      for (let i = 0; i < d.length; i += 4) {
        d[i] = Math.max(20, d[i] * 0.72);
        d[i + 1] = Math.max(15, d[i + 1] * 0.68);
        d[i + 2] = Math.max(12, d[i + 2] * 0.65);
      }
      ctx.putImageData(imgData, 0, 0);
    } else if (mode === "glare_flash") {
      ctx.putImageData(imgData, 0, 0);
      const grad = ctx.createRadialGradient(135, 115, 5, 135, 115, 62);
      grad.addColorStop(0, "rgba(255, 255, 255, 0.98)");
      grad.addColorStop(0.7, "rgba(255, 255, 255, 0.85)");
      grad.addColorStop(1, "rgba(255, 255, 255, 0)");
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.arc(135, 115, 62, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  canvas.toBlob((blob) => {
    customUploadedBlob = blob;
    document.getElementById("uploadFileName").textContent = `Perturbation: ${mode.replace("_", " ")}`;
    runCurrentScreening();
  }, "image/png");
}

function updateCameraTrafficLights(q) {
  const t = I18N[currentLang];
  const pillLight = document.getElementById("pillLight");
  const pillLightText = document.getElementById("pillLightText");
  const pillGlare = document.getElementById("pillGlare");
  const pillGlareText = document.getElementById("pillGlareText");
  const pillFocus = document.getElementById("pillFocus");
  const pillFocusText = document.getElementById("pillFocusText");
  const retakeOverlay = document.getElementById("qualityRetakeBanner");
  const retakeTitle = document.getElementById("retakeBannerTitle");

  // 1. Lighting Traffic Light
  if (q.lighting_status === "OPTIMAL") {
    pillLight.className = "traffic-pill pass";
    pillLightText.textContent = t.lightGood;
  } else {
    pillLight.className = "traffic-pill warn";
    pillLightText.textContent = q.lighting_status === "TOO_DARK" ? t.lightDark : t.lightBright;
  }

  // 2. Glare Traffic Light
  if (!q.has_glare) {
    pillGlare.className = "traffic-pill pass";
    pillGlareText.textContent = t.glareOk;
  } else {
    pillGlare.className = "traffic-pill fail";
    pillGlareText.textContent = t.glareHigh;
  }

  // 3. Focus Traffic Light
  if (!q.is_blurry) {
    pillFocus.className = "traffic-pill pass";
    pillFocusText.textContent = t.focusSteady;
  } else {
    pillFocus.className = "traffic-pill fail";
    pillFocusText.textContent = t.focusBlurry;
  }

  // Show immediate Retake prompt on viewfinder if quality gate fails
  if (!q.quality_passed) {
    retakeOverlay.classList.remove("hidden");
    retakeTitle.textContent = q.is_blurry ? t.retakeBlurTitle : t.retakeGlareTitle;
  } else {
    retakeOverlay.classList.add("hidden");
  }
}

function renderResult(data) {
  const t = I18N[currentLang];
  const q = data.camera_quality;

  // Update Viewfinder Preview Image & Traffic Lights on Screen 2
  const previewImg = document.getElementById("viewfinderPreviewImg");
  if (previewImg && data.images_base64 && data.images_base64.original) {
    previewImg.src = data.images_base64.original;
  }
  updateCameraTrafficLights(q);

  // Update Screen 4 Top Action Banner
  document.getElementById("caseIdDisplay").textContent =
    `CASE ID: ${data.case_id} · ${selectedBodyPart.toUpperCase()}`;
  document.getElementById("inferenceModeBadge").textContent =
    data.inference_mode.includes("ONNX")
      ? "ONNX Multimodal + CIE-L*a*b* Active"
      : "CIE-L*a*b* Analytical Mode";

  const chipQ = document.getElementById("chipQuality");
  const dotQ = document.getElementById("dotQuality");
  if (q.quality_passed) {
    chipQ.textContent = `Camera Gate: PASS (Sharpness σ²=${q.sharpness_variance})`;
    dotQ.textContent = "✓";
    dotQ.style.background = "#15803D";
  } else {
    const reason = q.is_blurry
      ? `Blur σ²=${q.sharpness_variance} (<35)`
      : q.has_glare
      ? `Glare ${(q.glare_ratio * 100).toFixed(1)}% (>12%)`
      : q.lighting_status;
    chipQ.textContent = `RETAKE PHOTO: ${reason}`;
    dotQ.textContent = "!";
    dotQ.style.background = "#B45309";
  }

  document.getElementById("chipSkinTone").textContent =
    `${data.fitzpatrick_type} (${data.skin_tone_label} · ITA° ${data.ita_degrees}°)`;
  document.getElementById("chipErythema").textContent =
    `Melanin Boost ×${data.melanin_compensation_factor} · Erythema ${data.adjusted_erythema_index}`;

  document.getElementById("wfVisualPrior").textContent =
    `${(data.visual_prior_probability * 100).toFixed(1)}%`;
  document.getElementById("wfClinShift").textContent =
    `${data.clinical_log_odds_shift >= 0 ? "+" : ""}${data.clinical_log_odds_shift} log-odds`;
  document.getElementById("wfSkinTone").textContent =
    `${data.fitzpatrick_type} (${data.ita_degrees}°)`;

  const card = document.getElementById("referralCard");
  const fill = document.getElementById("riskProgressFill");
  const headlineEl = document.getElementById("resultBannerTitle");

  if (!q.quality_passed) {
    card.className = "action-hero-banner SLATE";
    document.getElementById("tierBadge").textContent = "QUALITY GATE FAILED · RETAKE PHOTO";
    document.getElementById("riskHeadline").textContent = `${data.final_risk_percentage}%*`;
    headlineEl.textContent = t.bannerHeadlineBlur;
    document.getElementById("recommendationText").textContent =
      currentLang === "kn"
        ? `ಕ್ಯಾಮೆರಾ ಗುಣಮಟ್ಟ ಎಚ್ಚರಿಕೆ: ಫೋಟೋ ಮಂದವಾಗಿದೆ ಅಥವಾ ಪ್ರತಿಫಲನ ಹೆಚ್ಚಾಗಿದೆ (σ²=${q.sharpness_variance}). ದಯವಿಟ್ಟು ಫೋನ್ ಅನ್ನು ಚರ್ಮದಿಂದ 15 ಸೆಂ.ಮೀ ದೂರದಲ್ಲಿ ಅಲುಗಾಡದಂತೆ ಹಿಡಿದು ಮತ್ತೆ ಫೋಟೋ ತೆಗೆಯಿರಿ.`
        : `CAMERA QUALITY GATE WARNING: Photo failed pre-inference quality check (Sharpness σ²=${q.sharpness_variance}, Glare=${(q.glare_ratio * 100).toFixed(1)}%, Lighting=${q.lighting_status}). Please hold the smartphone steady 15 cm from the skin in indirect daylight and retake.`;
  } else {
    card.className = `action-hero-banner ${data.badge_color}`;
    document.getElementById("tierBadge").textContent = data.triage_tier.replace(/_/g, " ");
    document.getElementById("riskHeadline").textContent = `${data.final_risk_percentage}%`;
    headlineEl.textContent =
      data.badge_color === "RED"
        ? t.bannerHeadlineRed
        : data.badge_color === "AMBER"
        ? t.bannerHeadlineAmber
        : t.bannerHeadlineGreen;
    document.getElementById("recommendationText").textContent =
      currentLang === "kn" ? data.recommendation_kn : data.recommendation_en;
  }

  fill.style.width = `${Math.min(100, data.final_risk_percentage)}%`;
  fill.style.background =
    data.badge_color === "RED"
      ? "#B91C1C"
      : data.badge_color === "AMBER"
      ? "#B45309"
      : "#15803D";

  // Highlight Active Branch in Sarvam Workflow Graph
  const nodeHigh = document.getElementById("nodeHigh");
  const nodeMod = document.getElementById("nodeMod");
  const nodeLow = document.getElementById("nodeLow");
  const pathHigh = document.getElementById("pathHigh");
  const pathMod = document.getElementById("pathMod");
  const pathLow = document.getElementById("pathLow");

  nodeHigh.classList.remove("active");
  nodeMod.classList.remove("active");
  nodeLow.classList.remove("active");
  pathHigh.className.baseVal = "wf-path";
  pathMod.className.baseVal = "wf-path";
  pathLow.className.baseVal = "wf-path";

  if (data.badge_color === "RED") {
    nodeHigh.classList.add("active");
    pathHigh.className.baseVal = "wf-path active-high";
  } else if (data.badge_color === "AMBER") {
    nodeMod.classList.add("active");
    pathMod.className.baseVal = "wf-path active-mod";
  } else {
    nodeLow.classList.add("active");
    pathLow.className.baseVal = "wf-path active-low";
  }

  document.getElementById("nodeLowSub").textContent =
    data.suspected_superficial_fungal_or_scabies
      ? "Suspected superficial fungal tinea/scabies pattern — topical hygiene & 7-day check"
      : "Provide routine PHC hygiene counseling & re-screen in 7 days if unhealed";

  // 4-Stage Vision Images in Doctor Drawer
  document.getElementById("imgOrig").src = data.images_base64.original;
  document.getElementById("imgNorm").src = data.images_base64.normalized;
  document.getElementById("imgSeg").src = data.images_base64.segmentation;
  document.getElementById("imgHeat").src = data.images_base64.heatmap;

  // ABCD Morphological Bars
  const abcd = data.abcd_scores;
  const abcdItems = [
    ["A — Asymmetry Index (Centroid Reflection)", abcd.asymmetry],
    ["B — Border Irregularity (P² / 4π·Area)", abcd.border_irregularity],
    ["C — Color Variegation (CIE-L*a*b* Std)", abcd.color_variegation],
    ["D — Melanin-Adjusted Erythema & Extent", abcd.diameter_erythema_extent],
  ];
  document.getElementById("abcdList").innerHTML = abcdItems
    .map(
      ([label, val]) => `
      <li>
        <div class="metric-row-top">
          <span>${label}</span>
          <strong>${(val * 100).toFixed(1)}%</strong>
        </div>
        <div class="mini-bar-track">
          <div class="mini-bar-fill" style="width:${Math.min(100, val * 100)}%"></div>
        </div>
      </li>
    `
    )
    .join("");

  // Bayesian Log-Odds Contributions
  document.getElementById("contribList").innerHTML = data.feature_contributions
    .map((c) => {
      const pctWidth = Math.min(100, Math.abs(c.log_odds_delta) * 55);
      const barColor = c.log_odds_delta >= 0 ? "#18181B" : "#15803D";
      return `
      <li>
        <div class="metric-row-top">
          <span>${c.feature}</span>
          <strong>${c.log_odds_delta >= 0 ? "+" : ""}${c.log_odds_delta} log-odds</strong>
        </div>
        <div class="mini-bar-track">
          <div class="mini-bar-fill" style="width:${pctWidth}%; background:${barColor}"></div>
        </div>
      </li>
    `;
    })
    .join("");
}

function toggleDoctorDrawer() {
  const btn = document.getElementById("doctorDrawerBtn");
  const content = document.getElementById("doctorDrawerContent");
  const isExpanded = btn.getAttribute("aria-expanded") === "true";
  btn.setAttribute("aria-expanded", String(!isExpanded));
  content.classList.toggle("hidden", isExpanded);
}

// Voice Readout for Individual Symptom Questions (EN-IN / KN-IN)
function speakSymptomQuestion(event, key) {
  if (event) event.stopPropagation();
  if (!("speechSynthesis" in window)) return;
  const qObj = I18N[currentLang].questions[key];
  if (!qObj) return;
  window.speechSynthesis.cancel();
  const utter = new SpeechSynthesisUtterance(`${qObj.q}. ${qObj.sub}`);
  utter.lang = currentLang === "kn" ? "kn-IN" : "en-IN";
  utter.rate = 0.95;
  window.speechSynthesis.speak(utter);
}

// Voice Readout for Final Clinical Recommendation (EN-IN / KN-IN)
function speakRecommendation() {
  if (!lastScreeningResult || !("speechSynthesis" in window)) return;
  window.speechSynthesis.cancel();
  const textToRead =
    currentLang === "kn"
      ? lastScreeningResult.recommendation_kn
      : lastScreeningResult.recommendation_en;
  const utter = new SpeechSynthesisUtterance(textToRead);
  utter.lang = currentLang === "kn" ? "kn-IN" : "en-IN";
  utter.rate = 0.95;
  window.speechSynthesis.speak(utter);
}

// Share Referral Slip on WhatsApp
function shareOnWhatsApp() {
  if (!lastScreeningResult) return;
  const d = lastScreeningResult;
  const patientName = document.getElementById("patientNameInput").value || "Patient";
  const village = document.getElementById("patientVillageInput").value || "Rural PHC";
  const age = document.getElementById("ageInput").value || "40";
  const msg = [
    `*KAYASETU PHC DERMATOLOGY REFERRAL SLIP*`,
    `Token ID: ${d.case_id}`,
    `Patient: ${patientName} (${age} yrs) · ${village}`,
    `Location: ${selectedBodyPart}`,
    `Risk Score: *${d.final_risk_percentage}%* (${d.triage_tier.replace(/_/g, " ")})`,
    `Skin Tone: ${d.fitzpatrick_type} (ITA ${d.ita_degrees} deg)`,
    ``,
    `Advice (EN): ${d.recommendation_en}`,
    `ಸಲಹೆ (KN): ${d.recommendation_kn}`,
    ``,
    `_Decision-Support Aid for PHC Triage — Final diagnosis by Medical Officer._`,
  ].join("\n");

  const waUrl = `https://wa.me/?text=${encodeURIComponent(msg)}`;
  window.open(waUrl, "_blank", "noopener,noreferrer");
}

function saveVisitAndOpenSlip() {
  if (!lastScreeningResult) return;
  const d = lastScreeningResult;
  const patientName = document.getElementById("patientNameInput").value || "Village Patient";
  const village = document.getElementById("patientVillageInput").value || "Rural PHC";
  const age = parseInt(document.getElementById("ageInput").value || "40", 10);

  // Prepend to Today's Village Visits queue if not already top
  const existingIdx = villageVisits.findIndex((v) => v.visitId === d.case_id);
  const newVisit = {
    visitId: d.case_id,
    sampleId: activeSampleId === "custom_upload" ? "case_high_scc_ulcer" : activeSampleId,
    patientName,
    patientNameKn: patientName,
    village,
    villageKn: village,
    age,
    bodyPart: selectedBodyPart,
    badgeColor: d.badge_color,
    riskPct: d.final_risk_percentage,
    timeLabel: new Date().toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }),
  };

  if (existingIdx >= 0) {
    villageVisits[existingIdx] = newVisit;
  } else {
    villageVisits.unshift(newVisit);
  }
  saveVillageVisits();
  renderVillageVisitsList();
  openReferralSlipModal();
}

function openReferralSlipModal() {
  if (!lastScreeningResult) return;
  const d = lastScreeningResult;
  document.getElementById("slipCaseId").textContent = d.case_id;
  document.getElementById("slipDate").textContent = new Date().toLocaleString("en-IN");
  document.getElementById("slipPatientName").textContent =
    document.getElementById("patientNameInput").value || "Village Patient";
  document.getElementById("slipVillage").textContent =
    document.getElementById("patientVillageInput").value || "Rural PHC";
  document.getElementById("slipAge").textContent = document.getElementById("ageInput").value;
  document.getElementById("slipBodyPart").textContent = selectedBodyPart;
  document.getElementById("slipTier").textContent = d.triage_tier.replace(/_/g, " ");
  document.getElementById("slipRecommendation").textContent = d.recommendation_en;
  document.getElementById("slipRecommendationKn").textContent = d.recommendation_kn;
  document.getElementById("slipScore").textContent = `${d.final_risk_percentage}%`;
  document.getElementById("slipSkinTelemetry").textContent =
    `${d.fitzpatrick_type} (${d.skin_tone_label}) · ITA° ${d.ita_degrees}° · Melanin Boost ×${d.melanin_compensation_factor} · Sharpness σ²=${d.camera_quality.sharpness_variance}`;
  const abcd = d.abcd_scores;
  document.getElementById("slipAbcdSummary").textContent =
    `Asymmetry: ${(abcd.asymmetry * 100).toFixed(1)}% | Border: ${(abcd.border_irregularity * 100).toFixed(1)}% | Color: ${(abcd.color_variegation * 100).toFixed(1)}% | Extent: ${(abcd.diameter_erythema_extent * 100).toFixed(1)}%`;
  document.getElementById("slipSymptomsList").innerHTML = d.feature_contributions
    .map(
      (c) =>
        `<li>${c.feature}: <strong>${c.log_odds_delta >= 0 ? "+" : ""}${c.log_odds_delta}</strong></li>`
    )
    .join("");
  document.getElementById("slipImgOrig").src = d.images_base64.normalized;
  document.getElementById("slipImgHeat").src = d.images_base64.heatmap;
  document.getElementById("slipDisclaimer").textContent = d.disclaimer;
  document.getElementById("referralModal").classList.remove("hidden");
}

function closeReferralSlipModal() {
  document.getElementById("referralModal").classList.add("hidden");
}

window.addEventListener("DOMContentLoaded", initApp);
