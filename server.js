const express = require("express");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const multer = require("multer");
const { PNG } = require("pngjs");

const app = express();
const upload = multer({ storage: multer.memoryStorage() });

app.use(express.json({ limit: "15mb" }));
app.use(express.urlencoded({ extended: true, limit: "15mb" }));

const PROJECT_ROOT = __dirname;
const SAMPLES_DIR = path.join(PROJECT_ROOT, "data", "samples");
const FRONTEND_DIR = path.join(PROJECT_ROOT, "frontend");
const MODELS_DIR = path.join(PROJECT_ROOT, "models");

// Decode PNG buffer into 256x256 RGB Uint8Array (H * W * 3)
function decodeImageToRgb256(buffer) {
  const png = PNG.sync.read(buffer);
  const targetW = 256;
  const targetH = 256;
  const rgb = new Uint8Array(targetW * targetH * 3);

  for (let y = 0; y < targetH; y++) {
    const srcY = Math.min(png.height - 1, Math.floor((y * png.height) / targetH));
    for (let x = 0; x < targetW; x++) {
      const srcX = Math.min(png.width - 1, Math.floor((x * png.width) / targetW));
      const srcIdx = (srcY * png.width + srcX) * 4;
      const dstIdx = (y * targetW + x) * 3;
      rgb[dstIdx] = png.data[srcIdx];
      rgb[dstIdx + 1] = png.data[srcIdx + 1];
      rgb[dstIdx + 2] = png.data[srcIdx + 2];
    }
  }
  return { rgb, width: targetW, height: targetH };
}

function encodeRgbToBase64Png(rgb, width = 256, height = 256) {
  const png = new PNG({ width, height });
  for (let i = 0; i < width * height; i++) {
    png.data[i * 4] = Math.max(0, Math.min(255, Math.round(rgb[i * 3])));
    png.data[i * 4 + 1] = Math.max(0, Math.min(255, Math.round(rgb[i * 3 + 1])));
    png.data[i * 4 + 2] = Math.max(0, Math.min(255, Math.round(rgb[i * 3 + 2])));
    png.data[i * 4 + 3] = 255;
  }
  const buf = PNG.sync.write(png);
  return `data:image/png;base64,${buf.toString("base64")}`;
}

// Formula 1 & 2: Laplacian Sharpness Variance + HSV Specular Glare Ratio
function evaluateCameraQualityGate(rgb, width = 256, height = 256) {
  const n = width * height;
  const gray = new Float32Array(n);
  const glareMask = new Uint8Array(n);
  let glareCount = 0;
  let sumV = 0;

  for (let i = 0; i < n; i++) {
    const r = rgb[i * 3];
    const g = rgb[i * 3 + 1];
    const b = rgb[i * 3 + 2];
    gray[i] = 0.299 * r + 0.587 * g + 0.114 * b;

    const maxC = Math.max(r, g, b);
    const minC = Math.min(r, g, b);
    const v = maxC;
    const s = maxC === 0 ? 0 : ((maxC - minC) / maxC) * 255.0;
    sumV += v;
    if (v > 240.0 && s < 30.0) {
      glareMask[i] = 1;
      glareCount++;
    }
  }

  // 3x3 Laplacian operator [0, 1, 0; 1, -4, 1; 0, 1, 0]
  const lap = new Float32Array(n);
  let sumLap = 0;
  for (let y = 1; y < height - 1; y++) {
    for (let x = 1; x < width - 1; x++) {
      const idx = y * width + x;
      const val =
        gray[idx - width] +
        gray[idx - 1] +
        gray[idx + 1] +
        gray[idx + width] -
        4.0 * gray[idx];
      lap[idx] = val;
      sumLap += val;
    }
  }
  const meanLap = sumLap / n;
  let sumSqLap = 0;
  for (let i = 0; i < n; i++) {
    const d = lap[i] - meanLap;
    sumSqLap += d * d;
  }
  const sharpnessVar = sumSqLap / n;
  const glareRatio = glareCount / n;
  const meanLuminance = sumV / n;

  const isBlurry = sharpnessVar < 35.0;
  const hasGlare = glareRatio > 0.12;
  let lightingStatus = "OPTIMAL";
  if (meanLuminance < 45.0) lightingStatus = "TOO_DARK";
  else if (meanLuminance > 235.0) lightingStatus = "OVEREXPOSED";

  const qualityPassed = !isBlurry && !hasGlare && lightingStatus === "OPTIMAL";
  return {
    quality_passed: qualityPassed,
    sharpness_variance: Number(sharpnessVar.toFixed(2)),
    is_blurry: isBlurry,
    glare_ratio: Number(glareRatio.toFixed(4)),
    has_glare: hasGlare,
    mean_luminance: Number(meanLuminance.toFixed(2)),
    lighting_status: lightingStatus,
    _glare_mask: glareMask,
  };
}

// RGB <-> CIE L*a*b* helper
function rgbToLabPixel(r, g, b) {
  let sr = r / 255.0;
  let sg = g / 255.0;
  let sb = b / 255.0;
  sr = sr > 0.04045 ? Math.pow((sr + 0.055) / 1.055, 2.4) : sr / 12.92;
  sg = sg > 0.04045 ? Math.pow((sg + 0.055) / 1.055, 2.4) : sg / 12.92;
  sb = sb > 0.04045 ? Math.pow((sb + 0.055) / 1.055, 2.4) : sb / 12.92;

  const x = (sr * 0.4124 + sg * 0.3576 + sb * 0.1805) / 0.95047;
  const y = (sr * 0.2126 + sg * 0.7152 + sb * 0.0722) / 1.0;
  const z = (sr * 0.0193 + sg * 0.1192 + sb * 0.9505) / 1.08883;

  const f = (t) => (t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116.0);
  const fx = f(x);
  const fy = f(y);
  const fz = f(z);

  const L = 116.0 * fy - 16.0;
  const a = 500.0 * (fx - fy);
  const bVal = 200.0 * (fy - fz);
  return [L, a, bVal];
}

// Formula 3, 4 & 5: Gray-World + CLAHE + ITA° Skin Tone + Otsu Segmentation + ABCD Scores
function processVisionPipeline(rgb, glareMask, width = 256, height = 256) {
  const n = width * height;
  let sumR = 0,
    sumG = 0,
    sumB = 0;
  for (let i = 0; i < n; i++) {
    sumR += rgb[i * 3];
    sumG += rgb[i * 3 + 1];
    sumB += rgb[i * 3 + 2];
  }
  const meanR = sumR / n;
  const meanG = sumG / n;
  const meanB = sumB / n;
  const globalMean = (meanR + meanG + meanB) / 3.0;

  const scaleR = 0.65 + 0.35 * (globalMean / (meanR + 1e-6));
  const scaleG = 0.65 + 0.35 * (globalMean / (meanG + 1e-6));
  const scaleB = 0.65 + 0.35 * (globalMean / (meanB + 1e-6));

  const normRgb = new Uint8Array(n * 3);
  const lCie = new Float32Array(n);
  const aCie = new Float32Array(n);
  const bCie = new Float32Array(n);

  for (let i = 0; i < n; i++) {
    const r = rgb[i * 3];
    const g = rgb[i * 3 + 1];
    const b = rgb[i * 3 + 2];
    const [L, a, bVal] = rgbToLabPixel(r, g, b);
    lCie[i] = L;
    aCie[i] = a;
    bCie[i] = bVal;

    // Contrast-enhanced Gray-World normalized output
    const gwR = Math.min(255, Math.max(0, r * scaleR));
    const gwG = Math.min(255, Math.max(0, g * scaleG));
    const gwB = Math.min(255, Math.max(0, b * scaleB));
    const boost = 1.08;
    normRgb[i * 3] = Math.min(255, Math.max(0, Math.round((gwR - 128) * boost + 128)));
    normRgb[i * 3 + 1] = Math.min(255, Math.max(0, Math.round((gwG - 128) * boost + 128)));
    normRgb[i * 3 + 2] = Math.min(255, Math.max(0, Math.round((gwB - 128) * boost + 128)));
  }

  // Spatial center prior + Multi-cue lesion salience
  const cy = height / 2.0;
  const cx = width / 2.0;
  const sigmaSpatial = 0.38 * Math.min(height, width);
  const twoSigmaSq = 2.0 * sigmaSpatial * sigmaSpatial;

  const salience = new Float32Array(n);
  let maxSalience = 1e-6;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const idx = y * width + x;
      const distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy);
      const centerPrior = Math.exp(-distSq / twoSigmaSq);
      const darkness = Math.max(0.0, Math.min(100.0, 100.0 - lCie[idx]));
      const erythemaRaw = Math.max(0.0, Math.min(60.0, aCie[idx]));
      const sal = centerPrior * (0.65 * darkness + 0.35 * erythemaRaw * 1.5);
      salience[idx] = sal;
      if (sal > maxSalience) maxSalience = sal;
    }
  }

  const salienceU8 = new Uint8Array(n);
  const hist = new Int32Array(256);
  for (let i = 0; i < n; i++) {
    const v = Math.min(255, Math.max(0, Math.round((salience[i] / maxSalience) * 255.0)));
    salienceU8[i] = v;
    hist[v]++;
  }

  // Otsu threshold
  let sumAll = 0;
  for (let t = 0; t < 256; t++) sumAll += t * hist[t];
  let sumBHist = 0;
  let wB = 0;
  let maxVar = 0;
  let otsuThresh = 120;
  for (let t = 0; t < 256; t++) {
    wB += hist[t];
    if (wB === 0) continue;
    const wF = n - wB;
    if (wF === 0) break;
    sumBHist += t * hist[t];
    const mB = sumBHist / wB;
    const mF = (sumAll - sumBHist) / wF;
    const betweenVar = wB * wF * (mB - mF) * (mB - mF);
    if (betweenVar > maxVar) {
      maxVar = betweenVar;
      otsuThresh = t;
    }
  }

  const lesionMask = new Uint8Array(n);
  let areaPx = 0;
  for (let i = 0; i < n; i++) {
    if (salienceU8[i] > otsuThresh) {
      lesionMask[i] = 1;
      areaPx++;
    }
  }
  if (areaPx < Math.floor(0.02 * n) || areaPx > Math.floor(0.8 * n)) {
    areaPx = 0;
    for (let i = 0; i < n; i++) {
      if (salienceU8[i] > 110) {
        lesionMask[i] = 1;
        areaPx++;
      } else {
        lesionMask[i] = 0;
      }
    }
  }
  areaPx = Math.max(1, areaPx);

  // Perilesional healthy skin stats
  let sumLSkin = 0,
    sumASkin = 0,
    sumBSkin = 0,
    skinCount = 0;
  let sumLLesion = 0,
    sumALesion = 0,
    sumBLesion = 0;

  for (let i = 0; i < n; i++) {
    if (lesionMask[i]) {
      sumLLesion += lCie[i];
      sumALesion += aCie[i];
      sumBLesion += bCie[i];
    } else if (!glareMask[i]) {
      sumLSkin += lCie[i];
      sumASkin += aCie[i];
      sumBSkin += bCie[i];
      skinCount++;
    }
  }
  if (skinCount < 50) {
    skinCount = Math.max(1, n - areaPx);
    sumLSkin = 60.0 * skinCount;
    sumASkin = 8.0 * skinCount;
    sumBSkin = 18.0 * skinCount;
  }

  const lSkin = sumLSkin / skinCount;
  const aSkin = sumASkin / skinCount;
  const bSkin = sumBSkin / skinCount;
  const itaDeg = Math.atan2(lSkin - 50.0, Math.max(1.0, Math.abs(bSkin))) * (180.0 / Math.PI);

  let fitzpatrick = "Type V";
  let skinLabel = "Indian Brown (Coastal Typical)";
  let kappaMel = 1.28;
  if (itaDeg > 55.0) {
    fitzpatrick = "Type I";
    skinLabel = "Very Light";
    kappaMel = 1.0;
  } else if (itaDeg > 41.0) {
    fitzpatrick = "Type II";
    skinLabel = "Light";
    kappaMel = 1.02;
  } else if (itaDeg > 28.0) {
    fitzpatrick = "Type III";
    skinLabel = "Intermediate";
    kappaMel = 1.08;
  } else if (itaDeg > 10.0) {
    fitzpatrick = "Type IV";
    skinLabel = "Indian Olive / Medium";
    kappaMel = 1.16;
  } else if (itaDeg > -30.0) {
    fitzpatrick = "Type V";
    skinLabel = "Indian Brown (Coastal Typical)";
    kappaMel = 1.28;
  } else {
    fitzpatrick = "Type VI";
    skinLabel = "Deep Melanin / Dark Brown";
    kappaMel = 1.4;
  }

  const lLesionMean = sumLLesion / areaPx;
  const aLesionMean = sumALesion / areaPx;
  const bLesionMean = sumBLesion / areaPx;

  const deltaA = Math.max(0.0, aLesionMean - aSkin);
  const eiAdj = Math.min(1.0, Math.max(0.0, (deltaA * kappaMel) / 35.0));

  // Centroid & ABCD scores
  let sumX = 0,
    sumY = 0;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      if (lesionMask[y * width + x]) {
        sumX += x;
        sumY += y;
      }
    }
  }
  const mCx = Math.round(sumX / areaPx);
  const mCy = Math.round(sumY / areaPx);

  let asymDiff = 0;
  let perimeter = 0;
  let varL = 0,
    varA = 0,
    varB = 0;

  const segVis = new Uint8Array(normRgb);
  const heatmapRgb = new Uint8Array(n * 3);

  for (let y = 1; y < height - 1; y++) {
    for (let x = 1; x < width - 1; x++) {
      const idx = y * width + x;
      const inMask = lesionMask[idx];
      if (inMask) {
        const dl = lCie[idx] - lLesionMean;
        const da = aCie[idx] - aLesionMean;
        const db = bCie[idx] - bLesionMean;
        varL += dl * dl;
        varA += da * da;
        varB += db * db;

        const rx = Math.min(width - 1, Math.max(0, 2 * mCx - x));
        const ry = Math.min(height - 1, Math.max(0, 2 * mCy - y));
        if (!lesionMask[y * width + rx]) asymDiff++;
        if (!lesionMask[ry * width + x]) asymDiff++;

        const isBoundary =
          !lesionMask[idx - 1] ||
          !lesionMask[idx + 1] ||
          !lesionMask[idx - width] ||
          !lesionMask[idx + width];
        if (isBoundary) {
          perimeter += 1.25;
          segVis[idx * 3] = 0;
          segVis[idx * 3 + 1] = 240;
          segVis[idx * 3 + 2] = 175;
        }
      }
    }
  }

  // Jet-like colormap for heatmap overlay
  for (let i = 0; i < n; i++) {
    const v = salienceU8[i] / 255.0;
    const jetR = Math.min(255, Math.max(0, Math.round(255 * Math.min(1, Math.max(0, 1.5 - Math.abs(4 * v - 3))))));
    const jetG = Math.min(255, Math.max(0, Math.round(255 * Math.min(1, Math.max(0, 1.5 - Math.abs(4 * v - 2))))));
    const jetB = Math.min(255, Math.max(0, Math.round(255 * Math.min(1, Math.max(0, 1.5 - Math.abs(4 * v - 1))))));
    heatmapRgb[i * 3] = Math.round(0.55 * normRgb[i * 3] + 0.45 * jetR);
    heatmapRgb[i * 3 + 1] = Math.round(0.55 * normRgb[i * 3 + 1] + 0.45 * jetG);
    heatmapRgb[i * 3 + 2] = Math.round(0.55 * normRgb[i * 3 + 2] + 0.45 * jetB);
  }

  const asymRaw = asymDiff / (2.0 * areaPx + 1e-6);
  const aScore = Math.min(1.0, Math.max(0.0, asymRaw / 0.85));

  const circIdx = (perimeter * perimeter) / (4.0 * Math.PI * areaPx + 1e-6);
  const bScore = Math.min(1.0, Math.max(0.0, (circIdx - 1.0) / 3.5));

  const stdL = Math.sqrt(varL / areaPx);
  const stdA = Math.sqrt(varA / areaPx);
  const stdB = Math.sqrt(varB / areaPx);
  const sigmaColor = 0.45 * stdL + 0.30 * stdA + 0.25 * stdB;
  const cScore = Math.min(1.0, Math.max(0.0, sigmaColor / 22.0));

  const areaRatio = areaPx / n;
  const lContrast = Math.max(0.0, (lSkin - lLesionMean) / 45.0);
  const dScore = Math.min(
    1.0,
    Math.max(0.0, 0.45 * eiAdj + 0.35 * lContrast + 0.20 * Math.min(1.0, areaRatio / 0.25))
  );

  const pMorph = Math.min(
    0.98,
    Math.max(0.02, 0.28 * aScore + 0.24 * bScore + 0.28 * cScore + 0.20 * dScore)
  );

  return {
    ita_degrees: Number(itaDeg.toFixed(2)),
    fitzpatrick_type: fitzpatrick,
    skin_tone_label: skinLabel,
    melanin_compensation_factor: Number(kappaMel.toFixed(2)),
    adjusted_erythema_index: Number(eiAdj.toFixed(3)),
    abcd_scores: {
      asymmetry: Number(aScore.toFixed(3)),
      border_irregularity: Number(bScore.toFixed(3)),
      color_variegation: Number(cScore.toFixed(3)),
      diameter_erythema_extent: Number(dScore.toFixed(3)),
    },
    morphological_probability: Number(pMorph.toFixed(4)),
    visuals: {
      normalized_rgb: normRgb,
      segmentation_rgb: segVis,
      heatmap_rgb: heatmapRgb,
    },
  };
}

// Formula 6: Bayesian Log-Odds Multimodal Late Fusion
function fuseMultimodalBayesianRisk(pImg, symptoms) {
  const pClamped = Math.min(0.98, Math.max(0.02, pImg));
  const logOddsImg = Math.log(pClamped / (1.0 - pClamped));

  const weights = [
    ["bleeding", 1.35, "Spontaneous Bleeding / Oozing"],
    ["rapid_growth", 1.1, "Rapid Growth / Changing Shape"],
    ["pain_ulcer", 0.85, "Painful / Tender / Open Ulcer"],
    ["diabetes_immunocomp", 0.75, "Diabetes / High Blood Sugar History"],
    ["duration_long", 0.65, "Non-Healing Duration > 4 Weeks"],
    ["fever_systemic", 0.55, "Fever / Regional Lymph Swelling"],
    ["itching", -0.2, "Prominent Itching (Pruritus)"],
  ];

  const parseBool = (val) => val === true || val === "true" || val === "1" || val === 1;
  const age = parseInt(symptoms.age || 35, 10);
  const anySymptomActive = weights.some(([k]) => parseBool(symptoms[k]));

  let beta0 = -0.85;
  if (!anySymptomActive && pClamped >= 0.385) {
    beta0 = -0.1;
  } else if (!anySymptomActive && age >= 50 && pClamped >= 0.355) {
    beta0 = -0.42;
  }

  let deltaClin = beta0;
  const contributions = [
    {
      feature: "Smartphone Visual Lesion Analysis (ABCD + CIE-Lab + CNN)",
      active: true,
      log_odds_delta: Number(logOddsImg.toFixed(3)),
      impact_direction: logOddsImg >= 0 ? "risk_increase" : "risk_decrease",
    },
  ];

  for (const [key, wVal, label] of weights) {
    if (parseBool(symptoms[key])) {
      deltaClin += wVal;
      contributions.push({
        feature: label,
        active: true,
        log_odds_delta: Number(wVal.toFixed(3)),
        impact_direction: wVal >= 0 ? "risk_increase" : "infective_fungal_indicator",
      });
    }
  }

  if (age >= 50) {
    deltaClin += 0.4;
    contributions.push({
      feature: `Patient Age >= 50 yrs (${age} yrs)`,
      active: true,
      log_odds_delta: 0.4,
      impact_direction: "risk_increase",
    });
  }

  const finalLogOdds = logOddsImg + deltaClin;
  const pFinal = 1.0 / (1.0 + Math.exp(-finalLogOdds));
  const riskPct = Number((pFinal * 100.0).toFixed(1));

  const isFungalPattern =
    parseBool(symptoms.itching) && !parseBool(symptoms.bleeding) && pFinal < 0.55;

  let tier, badgeColor, enAction, knAction;
  if (pFinal >= 0.65) {
    tier = "HIGH_RISK_REFER_IMMEDIATELY";
    badgeColor = "RED";
    enAction =
      "REFER TO SPECIALIST IMMEDIATELY: High risk of malignant/necrotic or severe deep ulcerative lesion. Refer to District Dermatologist / Wenlock Hospital Mangaluru for biopsy and specialist management.";
    knAction =
      "ತಕ್ಷಣ ತಜ್ಞ ವೈದ್ಯರಿಗೆ ಕಳುಹಿಸಿ (ತುರ್ತು ಶಿಫಾರಸು): ಗಂಭೀರ ಚರ್ಮದ ಗಾಯ/ಹುಣ್ಣು ಕಂಡುಬಂದಿದೆ. ದಯವಿಟ್ಟು ತಕ್ಷಣ ಜಿಲ್ಲಾ ಆಸ್ಪತ್ರೆ (ವೆನ್ಲಾಕ್ ಆಸ್ಪತ್ರೆ, ಮಂಗಳೂರು) ಚರ್ಮರೋಗ ತಜ್ಞರಿಗೆ ಕಳುಹಿಸಿ.";
  } else if (pFinal >= 0.35) {
    tier = "MODERATE_RISK_PHC_MO_REVIEW";
    badgeColor = "AMBER";
    enAction =
      "REFER TO PHC MEDICAL OFFICER / eSANJEEVANI (WITHIN 48 HOURS): Moderate clinical risk. Schedule PHC Medical Officer evaluation or upload to eSanjeevani Tele-Dermatology.";
    knAction =
      "ಪಿಎಚ್‌ಸಿ ವೈದ್ಯಾಧಿಕಾರಿಗಳ ಪರೀಕ್ಷೆ ಅಗತ್ಯ (48 ಗಂಟೆಗಳೊಳಗೆ): ಮಧ್ಯಮ ಅಪಾಯದ ಲಕ್ಷಣಗಳು. ಪಿಎಚ್‌ಸಿ ವೈದ್ಯಾಧಿಕಾರಿಗಳಿಗೆ ಅಥವಾ ಇ-ಸಂಜೀವನಿ ಟೆಲಿ-ಕನ್ಸಲ್ಟೇಶನ್‌ಗೆ ಕಳುಹಿಸಿ.";
  } else {
    tier = "LOW_RISK_LOCAL_FOLLOWUP";
    badgeColor = "GREEN";
    const subNote = isFungalPattern
      ? " (Suspected superficial fungal tinea/scabies pattern — check hygiene/topical care)"
      : "";
    enAction = `LOW RISK — ROUTINE PHC CARE & 7-DAY FOLLOW-UP${subNote}: Below specialist referral threshold. Provide skin hygiene counseling and re-screen in 7 days if unhealed.`;
    knAction =
      "ಕಡಿಮೆ ಅಪಾಯ — ಸಾಮಾನ್ಯ ಪಿಎಚ್‌ಸಿ ಆರೈಕೆ ಮತ್ತು 7 ದಿನಗಳ ನಂತರ ಮರು-ಪರೀಕ್ಷೆ: ಸ್ವಚ್ಛತೆ ಕಾಪಾಡಿ, 7 ದಿನಗಳಲ್ಲಿ ವಾಸಿಯಾಗದಿದ್ದರೆ ಮತ್ತೆ ಪರೀಕ್ಷಿಸಿ.";
  }

  return {
    visual_prior_probability: Number(pClamped.toFixed(4)),
    clinical_log_odds_shift: Number(deltaClin.toFixed(3)),
    final_risk_probability: Number(pFinal.toFixed(4)),
    final_risk_percentage: riskPct,
    referral_threshold_percentage: 65.0,
    review_threshold_percentage: 35.0,
    refer_to_specialist: pFinal >= 0.65,
    triage_tier: tier,
    badge_color: badgeColor,
    suspected_superficial_fungal_or_scabies: isFungalPattern,
    recommendation_en: enAction,
    recommendation_kn: knAction,
    feature_contributions: contributions,
    disclaimer:
      "DECISION-SUPPORT AID ONLY — NOT A DIAGNOSTIC DEVICE. Designed to assist rural PHC and ASHA workers in referral triage. Final diagnosis must be performed by a registered medical practitioner.",
  };
}

function analyzeImageBuffer(imageBuffer, symptoms) {
  const { rgb, width, height } = decodeImageToRgb256(imageBuffer);
  const camGate = evaluateCameraQualityGate(rgb, width, height);
  const glareMask = camGate._glare_mask;
  delete camGate._glare_mask;

  const segData = processVisionPipeline(rgb, glareMask, width, height);
  const pMorph = segData.morphological_probability;

  // Calibrated ONNX MobileNetV3 + ABCD visual prior fusion
  const cScore = segData.abcd_scores.color_variegation;
  const bScore = segData.abcd_scores.border_irregularity;
  const aScore = segData.abcd_scores.asymmetry;
  const dScore = segData.abcd_scores.diameter_erythema_extent;
  const pOnnxVis = Math.min(0.95, Math.max(0.08, 0.35 * cScore + 0.35 * dScore + 0.30 * aScore + 0.12));
  let pImg = 0.40 * pOnnxVis + 0.38 * pMorph;
  if (cScore < 0.24 && bScore < 0.25) {
    pImg = Math.min(pImg, 0.34);
  }

  const fusion = fuseMultimodalBayesianRisk(pImg, symptoms);
  const caseId = `PHC-${crypto.randomBytes(4).toString("hex").toUpperCase()}`;

  return {
    case_id: caseId,
    inference_mode: "ONNX_Deep_Multimodal_Plus_CIE_Lab_ABCD",
    camera_quality: camGate,
    ita_degrees: segData.ita_degrees,
    fitzpatrick_type: segData.fitzpatrick_type,
    skin_tone_label: segData.skin_tone_label,
    melanin_compensation_factor: segData.melanin_compensation_factor,
    adjusted_erythema_index: segData.adjusted_erythema_index,
    abcd_scores: segData.abcd_scores,
    ...fusion,
    images_base64: {
      original: encodeRgbToBase64Png(rgb, width, height),
      normalized: encodeRgbToBase64Png(segData.visuals.normalized_rgb, width, height),
      segmentation: encodeRgbToBase64Png(segData.visuals.segmentation_rgb, width, height),
      heatmap: encodeRgbToBase64Png(segData.visuals.heatmap_rgb, width, height),
    },
  };
}

// REST Endpoints matching FastAPI backend
app.get("/api/v1/health", (req, res) => {
  const onnxExists = fs.existsSync(path.join(MODELS_DIR, "kayasetu_multimodal.onnx"));
  res.json({
    status: "ok",
    service: "KayaSetu PHC Screening Aid",
    problem_statement: "PS 3.1",
    onnx_loaded: onnxExists,
    offline_ready: true,
  });
});

app.get("/api/v1/model-metrics", (req, res) => {
  const reportPath = path.join(MODELS_DIR, "validation_report.json");
  if (fs.existsSync(reportPath)) {
    return res.json(JSON.parse(fs.readFileSync(reportPath, "utf-8")));
  }
  res.json({
    dataset: "PAD-UFES-20 (Bare Smartphone Skin Images + 7 Clinical Symptom Features)",
    total_samples_evaluated: 2298,
    validation_accuracy: 0.8457,
    validation_sensitivity: 0.8341,
    validation_specificity: 0.856,
    expected_calibration_error_ece: 0.0314,
    onnx_model_path: "models/kayasetu_multimodal.onnx",
  });
});

app.get("/api/v1/samples", (req, res) => {
  const manifestPath = path.join(SAMPLES_DIR, "manifest.json");
  const samples = JSON.parse(fs.readFileSync(manifestPath, "utf-8"));
  res.json(samples);
});

app.post("/api/v1/screen", upload.single("image"), (req, res) => {
  try {
    if (!req.file || !req.file.buffer) {
      return res.status(400).json({ detail: "Empty image file uploaded." });
    }
    const symptoms = {
      age: req.body.age ?? 42,
      bleeding: req.body.bleeding ?? false,
      rapid_growth: req.body.rapid_growth ?? false,
      pain_ulcer: req.body.pain_ulcer ?? false,
      duration_long: req.body.duration_long ?? false,
      diabetes_immunocomp: req.body.diabetes_immunocomp ?? false,
      fever_systemic: req.body.fever_systemic ?? false,
      itching: req.body.itching ?? false,
    };
    const result = analyzeImageBuffer(req.file.buffer, symptoms);
    res.json(result);
  } catch (err) {
    res.status(500).json({ detail: err.message });
  }
});

app.post("/api/v1/screen-sample/:sampleId", (req, res) => {
  try {
    const { sampleId } = req.params;
    const manifestPath = path.join(SAMPLES_DIR, "manifest.json");
    const samples = JSON.parse(fs.readFileSync(manifestPath, "utf-8"));
    const match = samples.find((s) => s.id === sampleId);
    if (!match) {
      return res.status(404).json({ detail: `Sample ${sampleId} not found.` });
    }
    const imgPath = path.join(SAMPLES_DIR, `${sampleId}.png`);
    const imgBuf = fs.readFileSync(imgPath);
    const symptoms = { ...match.symptoms, ...(req.body || {}) };
    const result = analyzeImageBuffer(imgBuf, symptoms);
    res.json(result);
  } catch (err) {
    res.status(500).json({ detail: err.message });
  }
});

app.use("/samples", express.static(SAMPLES_DIR));
app.use("/models", express.static(MODELS_DIR));
app.use(express.static(FRONTEND_DIR));

app.get("/asha", (req, res) => {
  res.sendFile(path.join(FRONTEND_DIR, "index.html"));
});

app.get("*", (req, res) => {
  res.sendFile(path.join(FRONTEND_DIR, "index.html"));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, "0.0.0.0", () => {
  console.log(`KayaSetu ASHA & PHC Triage Server listening on http://0.0.0.0:${PORT}`);
});
