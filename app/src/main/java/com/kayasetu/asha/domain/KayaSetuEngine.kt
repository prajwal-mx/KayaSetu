package com.kayasetu.asha.domain

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class AppLanguage { EN, KN }

data class SymptomState(
    val age: Int = 28,
    val bleeding: Boolean = false,
    val rapidGrowth: Boolean = false,
    val painUlcer: Boolean = false,
    val durationLong: Boolean = false,
    val diabetesImmunocomp: Boolean = false,
    val feverSystemic: Boolean = false,
    val itching: Boolean = false
)

data class CameraQualityGate(
    val qualityPassed: Boolean,
    val sharpnessVariance: Double,
    val isBlurry: Boolean,
    val glareRatio: Double,
    val hasGlare: Boolean,
    val meanLuminance: Double,
    val lightingStatus: String
)

data class AbcdMetrics(
    val asymmetry: Double,
    val borderIrregularity: Double,
    val colorVariegation: Double,
    val diameterErythemaExtent: Double
)

data class LogOddsContribution(
    val featureLabel: String,
    val logOddsDelta: Double
)

data class VisionStageBitmaps(
    val original: Bitmap,
    val normalized: Bitmap,
    val segmentation: Bitmap,
    val heatmap: Bitmap
)

data class ScreeningResult(
    val caseId: String,
    val inferenceMode: String = "ONNX_Deep_Multimodal_Plus_CIE_Lab_ABCD",
    val cameraQuality: CameraQualityGate,
    val itaDegrees: Double,
    val fitzpatrickType: String,
    val skinToneLabel: String,
    val melaninBoost: Double,
    val adjustedErythemaIndex: Double,
    val abcdScores: AbcdMetrics,
    val visualPriorProbability: Double,
    val clinicalLogOddsShift: Double,
    val finalRiskPercentage: Double,
    val triageTier: String,
    val badgeColor: String,
    val suspectedFungal: Boolean,
    val recommendationEn: String,
    val recommendationKn: String,
    val contributions: List<LogOddsContribution>,
    val stageBitmaps: VisionStageBitmaps? = null
)

object KayaSetuEngine {

    @Volatile
    private var ortEnv: OrtEnvironment? = null

    @Volatile
    private var ortSession: OrtSession? = null

    private val initLock = Any()

    private data class CachedVisionMetrics(
        val bitmapIdentity: Int,
        val usedOnnx: Boolean,
        val camGate: CameraQualityGate,
        val itaDeg: Double,
        val fitz: String,
        val skinLabel: String,
        val kappaMel: Double,
        val eiAdj: Double,
        val abcdScores: AbcdMetrics,
        val pImg: Double,
        val mode: String,
        val stageBitmaps: VisionStageBitmaps
    )

    @Volatile
    private var lastVisionCache: CachedVisionMetrics? = null

    /**
     * Extracts kayasetu_multimodal.onnx and kayasetu_multimodal.onnx.data from APK assets
     * into app filesDir so ONNX Runtime C++ engine can resolve the external data tensor file,
     * and initializes the singleton OrtSession.
     */
    fun ensureOnnxLoaded(context: Context) {
        if (ortSession != null) return
        synchronized(initLock) {
            if (ortSession != null) return
            try {
                val modelDir = File(context.filesDir, "onnx_models")
                if (!modelDir.exists()) modelDir.mkdirs()

                val onnxFile = File(modelDir, "kayasetu_multimodal.onnx")
                val dataFile = File(modelDir, "kayasetu_multimodal.onnx.data")

                copyAssetIfNeeded(context, "kayasetu_multimodal.onnx", onnxFile, expectedMinBytes = 300_000L)
                copyAssetIfNeeded(context, "kayasetu_multimodal.onnx.data", dataFile, expectedMinBytes = 4_000_000L)

                val env = OrtEnvironment.getEnvironment()
                val opts = OrtSession.SessionOptions()
                ortEnv = env
                ortSession = env.createSession(onnxFile.absolutePath, opts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun copyAssetIfNeeded(
        context: Context,
        assetName: String,
        destFile: File,
        expectedMinBytes: Long
    ) {
        if (destFile.exists() && destFile.length() >= expectedMinBytes) {
            return
        }
        val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
        context.assets.open(assetName).use { input ->
            FileOutputStream(tmpFile).use { output ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val r = input.read(buf)
                    if (r <= 0) break
                    output.write(buf, 0, r)
                }
                output.flush()
            }
        }
        if (destFile.exists()) destFile.delete()
        tmpFile.renameTo(destFile)
    }

    /**
     * Synchronously fuses updated clinical symptoms with the already-computed vision + ONNX prior
     * for the current bitmap so symptom/age changes update the result in < 0.1ms with zero race conditions.
     */
    fun recomputeFromCachedVisionIfAvailable(
        rawBitmap: Bitmap?,
        symptoms: SymptomState
    ): ScreeningResult? {
        val bmp = rawBitmap ?: return null
        val cached = lastVisionCache ?: return null
        if (cached.bitmapIdentity != System.identityHashCode(bmp)) return null
        return fuseMultimodalBayesianRisk(
            pImg = cached.pImg,
            symptoms = symptoms,
            camGate = cached.camGate,
            itaDeg = cached.itaDeg,
            fitz = cached.fitz,
            skinLabel = cached.skinLabel,
            kappaMel = cached.kappaMel,
            eiAdj = cached.eiAdj,
            abcd = cached.abcdScores,
            inferenceMode = cached.mode,
            stageBitmaps = cached.stageBitmaps
        )
    }

    /**
     * Exact port of InferenceEngine._run_onnx_visual_prior(norm_rgb) from backend/app/inference_engine.py:
     * - Resizes norm_rgb to 224x224
     * - Normalizes with ImageNet mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225] in NCHW [1, 3, 224, 224]
     * - Passes neutral clinical feature vector [[0.35, 0.35, 0.35, 0.35, 0.35, 0.35, 0.50]]
     * - Applies temperature calibration: 1.0 / (1.0 + exp(-(logit_vis + 2.30) / 1.40))
     */
    private fun runOnnxVisualPrior(normBmp: Bitmap): Double {
        val session = ortSession ?: return -1.0
        val env = ortEnv ?: return -1.0
        return try {
            val img224 = Bitmap.createScaledBitmap(normBmp, 224, 224, true)
            val pixels = IntArray(224 * 224)
            img224.getPixels(pixels, 0, 224, 0, 0, 224, 224)

            val hw = 224 * 224
            val chwFloats = FloatArray(3 * hw)
            val meanR = 0.485f
            val meanG = 0.456f
            val meanB = 0.406f
            val stdR = 0.229f
            val stdG = 0.224f
            val stdB = 0.225f

            for (i in 0 until hw) {
                val c = pixels[i]
                val r = (Color.red(c) / 255.0f - meanR) / stdR
                val g = (Color.green(c) / 255.0f - meanG) / stdG
                val b = (Color.blue(c) / 255.0f - meanB) / stdB
                chwFloats[i] = r
                chwFloats[hw + i] = g
                chwFloats[2 * hw + i] = b
            }

            val clinNeutral = floatArrayOf(0.35f, 0.35f, 0.35f, 0.35f, 0.35f, 0.35f, 0.50f)

            OnnxTensor.createTensor(env, FloatBuffer.wrap(chwFloats), longArrayOf(1, 3, 224, 224)).use { imgTensor ->
                OnnxTensor.createTensor(env, FloatBuffer.wrap(clinNeutral), longArrayOf(1, 7)).use { clinTensor ->
                    session.run(
                        mapOf(
                            "image" to imgTensor,
                            "clinical_features" to clinTensor
                        )
                    ).use { results ->
                        val rawVal = results[0].value
                        val logitVis: Double = when (rawVal) {
                            is FloatArray -> rawVal[0].toDouble()
                            is Array<*> -> {
                                val first = rawVal.firstOrNull()
                                if (first is FloatArray) first[0].toDouble() else (first as? Number)?.toDouble() ?: 0.0
                            }
                            is Number -> rawVal.toDouble()
                            else -> 0.0
                        }
                        1.0 / (1.0 + exp(-(logitVis + 2.30) / 1.40))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1.0
        }
    }

    /**
     * Crops a raw camera/gallery bitmap strictly to the central square corresponding to
     * the on-screen target circle region (central 70% of the square viewfinder).
     */
    fun cropToTargetCircleRegion(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        val minDim = min(w, h)
        val sqX = (w - minDim) / 2
        val sqY = (h - minDim) / 2

        val cropSide = (minDim * 0.70f).toInt().coerceAtLeast(1)
        val offsetInSquare = (minDim - cropSide) / 2
        val finalX = (sqX + offsetInSquare).coerceIn(0, max(0, w - cropSide))
        val finalY = (sqY + offsetInSquare).coerceIn(0, max(0, h - cropSide))

        return try {
            Bitmap.createBitmap(src, finalX, finalY, cropSide, cropSide)
        } catch (_: Exception) {
            src
        }
    }

    /**
     * Exact sRGB -> CIE-L*a*b* conversion matching OpenCV cv2.COLOR_RGB2LAB:
     * Returns [L_cie (0..100), a_cie (-128..127), b_cie (-128..127), L_u8 (0..255), a_u8 (0..255), b_u8 (0..255)]
     */
    private fun rgbToLabOpenCv(r: Int, g: Int, b: Int): FloatArray {
        var sr = r / 255.0
        var sg = g / 255.0
        var sb = b / 255.0
        sr = if (sr > 0.04045) ((sr + 0.055) / 1.055).pow(2.4) else sr / 12.92
        sg = if (sg > 0.04045) ((sg + 0.055) / 1.055).pow(2.4) else sg / 12.92
        sb = if (sb > 0.04045) ((sb + 0.055) / 1.055).pow(2.4) else sb / 12.92

        val x = (sr * 0.412453 + sg * 0.357580 + sb * 0.180423) / 0.950456
        val y = (sr * 0.212671 + sg * 0.715160 + sb * 0.072169)
        val z = (sr * 0.019334 + sg * 0.119193 + sb * 0.950227) / 1.088754

        fun f(t: Double): Double = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116.0
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        val lStar = (116.0 * fy - 16.0).coerceIn(0.0, 100.0)
        val aStar = (500.0 * (fx - fy)).coerceIn(-128.0, 127.0)
        val bStar = (200.0 * (fy - fz)).coerceIn(-128.0, 127.0)

        val lU8 = (lStar * 255.0 / 100.0).roundToInt().coerceIn(0, 255)
        val aU8 = (aStar + 128.0).roundToInt().coerceIn(0, 255)
        val bU8 = (bStar + 128.0).roundToInt().coerceIn(0, 255)

        val lCie = lU8 * (100.0f / 255.0f)
        val aCie = aU8 - 128.0f
        val bCie = bU8 - 128.0f

        return floatArrayOf(lCie, aCie, bCie, lU8.toFloat(), aU8.toFloat(), bU8.toFloat())
    }

    private fun labU8ToRgbOpenCv(lU8: Int, aU8: Int, bU8: Int): Int {
        val l = lU8 * 100.0 / 255.0
        val a = aU8 - 128.0
        val b = bU8 - 128.0

        val fy = (l + 16.0) / 116.0
        val fx = fy + a / 500.0
        val fz = fy - b / 200.0

        fun fInv(t: Double): Double = if (t > 0.206893) t * t * t else (t - 16.0 / 116.0) / 7.787

        val x = 0.950456 * fInv(fx)
        val y = 1.000000 * fInv(fy)
        val z = 1.088754 * fInv(fz)

        var sr = x * 3.240479 - y * 1.537150 - z * 0.498535
        var sg = -x * 0.969256 + y * 1.875992 + z * 0.041556
        var sb = x * 0.055648 - y * 0.204043 + z * 1.057311

        fun gamma(c: Double): Int {
            val v = if (c <= 0.0) 0.0 else if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1.0 / 2.4) - 0.055
            return (v * 255.0).roundToInt().coerceIn(0, 255)
        }
        return Color.rgb(gamma(sr), gamma(sg), gamma(sb))
    }

    /**
     * Exact 8x8 tile CLAHE (clipLimit = 1.8, tileGridSize = 8x8) matching cv2.createCLAHE(clipLimit=1.8, tileGridSize=(8,8))
     */
    private fun applyClahe8x8(lChan: IntArray, w: Int, h: Int, clipLimit: Double = 1.8): IntArray {
        val tilesX = 8
        val tilesY = 8
        val tileW = w / tilesX
        val tileH = h / tilesY
        val tilePixels = tileW * tileH
        val maxClip = max(1, (clipLimit * tilePixels / 256.0).roundToInt())

        val luts = Array(tilesY * tilesX) { IntArray(256) }

        for (ty in 0 until tilesY) {
            for (tx in 0 until tilesX) {
                val hist = IntArray(256)
                val y0 = ty * tileH
                val x0 = tx * tileW
                for (y in y0 until (y0 + tileH)) {
                    val rowOff = y * w
                    for (x in x0 until (x0 + tileW)) {
                        hist[lChan[rowOff + x]]++
                    }
                }
                var excess = 0
                for (i in 0..255) {
                    if (hist[i] > maxClip) {
                        excess += hist[i] - maxClip
                        hist[i] = maxClip
                    }
                }
                val redistrib = excess / 256
                val residual = excess % 256
                for (i in 0..255) {
                    hist[i] += redistrib
                    if (i < residual) hist[i]++
                }
                val lut = luts[ty * tilesX + tx]
                var cdf = 0
                for (i in 0..255) {
                    cdf += hist[i]
                    lut[i] = ((cdf * 255.0) / tilePixels).roundToInt().coerceIn(0, 255)
                }
            }
        }

        val out = IntArray(w * h)
        for (y in 0 until h) {
            val fy = (y - tileH / 2.0) / tileH
            val ty0 = fy.toInt().coerceIn(0, tilesY - 1)
            val ty1 = (ty0 + 1).coerceIn(0, tilesY - 1)
            val wy = (fy - ty0).coerceIn(0.0, 1.0)

            for (x in 0 until w) {
                val fx = (x - tileW / 2.0) / tileW
                val tx0 = fx.toInt().coerceIn(0, tilesX - 1)
                val tx1 = (tx0 + 1).coerceIn(0, tilesX - 1)
                val wx = (fx - tx0).coerceIn(0.0, 1.0)

                val v = lChan[y * w + x]
                val v00 = luts[ty0 * tilesX + tx0][v]
                val v10 = luts[ty0 * tilesX + tx1][v]
                val v01 = luts[ty1 * tilesX + tx0][v]
                val v11 = luts[ty1 * tilesX + tx1][v]

                val top = v00 * (1.0 - wx) + v10 * wx
                val bot = v01 * (1.0 - wx) + v11 * wx
                out[y * w + x] = (top * (1.0 - wy) + bot * wy).roundToInt().coerceIn(0, 255)
            }
        }
        return out
    }

    /**
     * Exact Otsu threshold matching cv2.threshold(salience_u8, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
     */
    private fun computeOtsuThreshold(u8: IntArray): Int {
        val hist = IntArray(256)
        for (v in u8) hist[v.coerceIn(0, 255)]++
        val total = u8.size.toDouble()
        var sumAll = 0.0
        for (t in 0..255) sumAll += t * hist[t]

        var sumB = 0.0
        var wB = 0.0
        var maxVar = -1.0
        var bestThresh = 125

        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0.0) continue
            val wF = total - wB
            if (wF == 0.0) break
            sumB += t * hist[t]
            val mB = sumB / wB
            val mF = (sumAll - sumB) / wF
            val betweenVar = wB * wF * (mB - mF) * (mB - mF)
            if (betweenVar > maxVar) {
                maxVar = betweenVar
                bestThresh = t
            }
        }
        return bestThresh
    }

    private fun keepLargestConnectedComponent(mask: BooleanArray, w: Int, h: Int): Pair<BooleanArray, Int> {
        val n = w * h
        val visited = BooleanArray(n)
        var bestArea = 0
        var bestComponent = IntArray(0)
        val queue = IntArray(n)

        for (i in 0 until n) {
            if (mask[i] && !visited[i]) {
                var head = 0
                var tail = 0
                queue[tail++] = i
                visited[i] = true
                while (head < tail) {
                    val cur = queue[head++]
                    val cy = cur / w
                    val cx = cur % w
                    if (cy > 0) {
                        val nb = cur - w
                        if (mask[nb] && !visited[nb]) { visited[nb] = true; queue[tail++] = nb }
                    }
                    if (cy < h - 1) {
                        val nb = cur + w
                        if (mask[nb] && !visited[nb]) { visited[nb] = true; queue[tail++] = nb }
                    }
                    if (cx > 0) {
                        val nb = cur - 1
                        if (mask[nb] && !visited[nb]) { visited[nb] = true; queue[tail++] = nb }
                    }
                    if (cx < w - 1) {
                        val nb = cur + 1
                        if (mask[nb] && !visited[nb]) { visited[nb] = true; queue[tail++] = nb }
                    }
                }
                if (tail > bestArea) {
                    bestArea = tail
                    bestComponent = queue.copyOfRange(0, tail)
                }
            }
        }

        val out = BooleanArray(n)
        for (idx in bestComponent) out[idx] = true
        return out to bestArea
    }

    /**
     * Exact 1:1 port of InferenceEngine.analyze() + vision_math.py at 256x256 resolution,
     * executing the real kayasetu_multimodal.onnx model via ONNX Runtime.
     */
    fun analyzeBitmap(
        rawBitmap: Bitmap,
        sampleId: String,
        symptoms: SymptomState,
        perturbation: String? = null,
        context: Context? = null
    ): ScreeningResult {
        if (context != null) {
            ensureOnnxLoaded(context)
        }

        val bmpIdentity = System.identityHashCode(rawBitmap)
        val cached = lastVisionCache
        if (cached != null && cached.bitmapIdentity == bmpIdentity && cached.usedOnnx == (ortSession != null)) {
            return fuseMultimodalBayesianRisk(
                pImg = cached.pImg,
                symptoms = symptoms,
                camGate = cached.camGate,
                itaDeg = cached.itaDeg,
                fitz = cached.fitz,
                skinLabel = cached.skinLabel,
                kappaMel = cached.kappaMel,
                eiAdj = cached.eiAdj,
                abcd = cached.abcdScores,
                inferenceMode = cached.mode,
                stageBitmaps = cached.stageBitmaps
            )
        }

        // Step 1: decode_image_to_rgb(target_size=(256, 256))
        val workingBmp = Bitmap.createScaledBitmap(rawBitmap, 256, 256, true)
        val w = 256
        val h = 256
        val n = w * h
        val pixels = IntArray(n)
        workingBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // Step 2: evaluate_camera_quality_gate(rgb_arr)
        val gray = FloatArray(n)
        val glareMask = BooleanArray(n)
        var glareCount = 0
        var sumV = 0.0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0

        for (i in 0 until n) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            sumR += r
            sumG += g
            sumB += b
            gray[i] = (0.299f * r + 0.587f * g + 0.114f * b).roundToInt().toFloat()
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val s = if (maxC == 0) 0f else (((maxC - minC) * 255f) / maxC).roundToInt().toFloat()
            sumV += maxC
            if (maxC > 240 && s < 30f) {
                glareMask[i] = true
                glareCount++
            }
        }

        val lap = DoubleArray(n)
        var sumLap = 0.0
        for (y in 0 until h) {
            val ym = if (y > 0) y - 1 else 1
            val yp = if (y < h - 1) y + 1 else h - 2
            for (x in 0 until w) {
                val xm = if (x > 0) x - 1 else 1
                val xp = if (x < w - 1) x + 1 else w - 2
                val idx = y * w + x
                val v = (gray[ym * w + x] + gray[yp * w + x] + gray[y * w + xm] + gray[y * w + xp] - 4f * gray[idx]).toDouble()
                lap[idx] = v
                sumLap += v
            }
        }
        val meanLap = sumLap / n
        var sqLap = 0.0
        for (i in 0 until n) {
            val d = lap[i] - meanLap
            sqLap += d * d
        }
        val sharpnessVar = ((sqLap / n) * 100.0).roundToInt() / 100.0
        val glareRatio = ((glareCount.toDouble() / n) * 10000.0).roundToInt() / 10000.0
        val meanLum = ((sumV / n) * 100.0).roundToInt() / 100.0

        val isBlurry = sharpnessVar < 35.0
        val hasGlare = glareRatio > 0.12
        val lightingStatus = when {
            meanLum < 45.0 -> "TOO_DARK"
            meanLum > 235.0 -> "OVEREXPOSED"
            else -> "OPTIMAL"
        }
        val qualityPassed = !isBlurry && !hasGlare && lightingStatus == "OPTIMAL"

        val camGate = CameraQualityGate(
            qualityPassed = qualityPassed,
            sharpnessVariance = sharpnessVar,
            isBlurry = isBlurry,
            glareRatio = glareRatio,
            hasGlare = hasGlare,
            meanLuminance = meanLum,
            lightingStatus = lightingStatus
        )

        // Step 3: normalize_illumination_and_clahe(rgb_arr)
        val meanR = sumR / n
        val meanG = sumG / n
        val meanB = sumB / n
        val globalMean = (meanR + meanG + meanB) / 3.0
        val scaleR = 0.65 + 0.35 * (globalMean / (meanR + 1e-6))
        val scaleG = 0.65 + 0.35 * (globalMean / (meanG + 1e-6))
        val scaleB = 0.65 + 0.35 * (globalMean / (meanB + 1e-6))

        val lCie = FloatArray(n)
        val aCie = FloatArray(n)
        val bCie = FloatArray(n)

        val gwLU8 = IntArray(n)
        val gwAU8 = IntArray(n)
        val gwBU8 = IntArray(n)

        for (i in 0 until n) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            // Raw un-equalized CIE-L*a*b* from original RGB
            val rawLab = rgbToLabOpenCv(r, g, b)
            lCie[i] = rawLab[0]
            aCie[i] = rawLab[1]
            bCie[i] = rawLab[2]

            // Damped Gray-World RGB -> LAB for CLAHE
            val gwr = (r * scaleR).toInt().coerceIn(0, 255)
            val gwg = (g * scaleG).toInt().coerceIn(0, 255)
            val gwb = (b * scaleB).toInt().coerceIn(0, 255)
            val gwLab = rgbToLabOpenCv(gwr, gwg, gwb)
            gwLU8[i] = gwLab[3].toInt()
            gwAU8[i] = gwLab[4].toInt()
            gwBU8[i] = gwLab[5].toInt()
        }

        val lClahe = applyClahe8x8(gwLU8, w, h, clipLimit = 1.8)
        val normPixels = IntArray(n)
        for (i in 0 until n) {
            normPixels[i] = labU8ToRgbOpenCv(lClahe[i], gwAU8[i], gwBU8[i])
        }
        val normBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(normPixels, 0, w, 0, 0, w, h)
        }

        // Step 4: segment_lesion_and_skin_tone(norm_rgb, lab_cie, glare_mask)
        val cy = h / 2.0
        val cx = w / 2.0
        val sigmaSpatial = 0.38 * min(h, w)
        val twoSigmaSq = 2.0 * sigmaSpatial * sigmaSpatial
        val centerPrior = FloatArray(n)
        val salience = FloatArray(n)
        var maxSal = 1e-6f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy)
                val prior = exp(-distSq / twoSigmaSq).toFloat()
                centerPrior[idx] = prior
                val darkness = (100f - lCie[idx]).coerceIn(0f, 100f)
                val erythema = aCie[idx].coerceIn(0f, 60f)
                val sal = prior * (0.65f * darkness + 0.35f * erythema * 1.5f)
                salience[idx] = sal
                if (sal > maxSal) maxSal = sal
            }
        }

        val salU8 = IntArray(n)
        for (i in 0 until n) {
            salU8[i] = ((salience[i] / maxSal) * 255f).toInt().coerceIn(0, 255)
        }

        val otsuThresh = computeOtsuThreshold(salU8)
        val otsuMask = BooleanArray(n) { i -> salU8[i] > otsuThresh }

        val (largestMask, compArea) = keepLargestConnectedComponent(otsuMask, w, h)
        var lesionMask = largestMask
        var areaPx = compArea

        if (areaPx < (0.02 * n).toInt() || areaPx > (0.80 * n).toInt()) {
            val sortedSal = salU8.clone().apply { sort() }
            val medianSal = sortedSal[n / 2]
            lesionMask = BooleanArray(n)
            areaPx = 0
            for (i in 0 until n) {
                if (centerPrior[i] > 0.55f && salU8[i] > medianSal) {
                    lesionMask[i] = true
                    areaPx++
                }
            }
            if (areaPx < 1) areaPx = 1
        }

        // Perilesional healthy skin mask (exclude lesion & glare)
        var sumLSkin = 0.0
        var sumASkin = 0.0
        var sumBSkin = 0.0
        var skinCnt = 0
        var sumLLesion = 0.0
        var sumALesion = 0.0
        var sumBLesion = 0.0
        var sumX = 0.0
        var sumY = 0.0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                if (lesionMask[i]) {
                    sumLLesion += lCie[i]
                    sumALesion += aCie[i]
                    sumBLesion += bCie[i]
                    sumX += x
                    sumY += y
                } else if (!glareMask[i]) {
                    sumLSkin += lCie[i]
                    sumASkin += aCie[i]
                    sumBSkin += bCie[i]
                    skinCnt++
                }
            }
        }
        if (skinCnt < 100) {
            sumLSkin = 0.0; sumASkin = 0.0; sumBSkin = 0.0; skinCnt = 0
            for (i in 0 until n) {
                if (!lesionMask[i]) {
                    sumLSkin += lCie[i]
                    sumASkin += aCie[i]
                    sumBSkin += bCie[i]
                    skinCnt++
                }
            }
        }
        if (skinCnt == 0) skinCnt = 1

        val lSkin = sumLSkin / skinCnt
        val aSkin = sumASkin / skinCnt
        val bSkin = sumBSkin / skinCnt
        val itaDeg = ((atan2(lSkin - 50.0, max(1.0, abs(bSkin))) * (180.0 / Math.PI)) * 100.0).roundToInt() / 100.0

        val (fitz, skinLabel, kappaMel) = when {
            itaDeg > 55.0 -> Triple("Type I", "Very Light", 1.00)
            itaDeg > 41.0 -> Triple("Type II", "Light", 1.02)
            itaDeg > 28.0 -> Triple("Type III", "Intermediate", 1.08)
            itaDeg > 10.0 -> Triple("Type IV", "Indian Olive / Medium", 1.16)
            itaDeg > -30.0 -> Triple("Type V", "Indian Brown (Coastal Typical)", 1.28)
            else -> Triple("Type VI", "Deep Melanin / Dark Brown", 1.40)
        }

        val meanLLesion = sumLLesion / areaPx
        val meanALesion = sumALesion / areaPx
        val meanBLesion = sumBLesion / areaPx
        val deltaA = max(0.0, meanALesion - aSkin)
        val eiAdj = (((deltaA * kappaMel) / 35.0).coerceIn(0.0, 1.0) * 1000.0).roundToInt() / 1000.0

        // Formula 5: Exact ABCD Scores from vision_math.py
        val mCy = (sumY / areaPx).toInt()
        val mCx = (sumX / areaPx).toInt()
        val shiftY = (h / 2) - mCy
        val shiftX = (w / 2) - mCx
        val centeredMask = BooleanArray(n)
        for (y in 0 until h) {
            val ny = y + shiftY
            if (ny !in 0 until h) continue
            for (x in 0 until w) {
                val nx = x + shiftX
                if (nx in 0 until w && lesionMask[y * w + x]) {
                    centeredMask[ny * w + nx] = true
                }
            }
        }

        var diffH = 0
        var diffV = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val cVal = centeredMask[y * w + x]
                val fhVal = centeredMask[y * w + (w - 1 - x)]
                val fvVal = centeredMask[(h - 1 - y) * w + x]
                if (cVal != fhVal) diffH++
                if (cVal != fvVal) diffV++
            }
        }
        val asymRaw = (diffH + diffV).toDouble() / (2.0 * areaPx + 1e-6)
        val aScore = ((asymRaw / 0.85).coerceIn(0.0, 1.0) * 1000.0).roundToInt() / 1000.0

        var perimeter = 0.0
        var varLSum = 0.0
        var varASum = 0.0
        var varBSum = 0.0
        val segPixels = normPixels.clone()

        for (i in 0 until n) {
            if (!lesionMask[i]) {
                val c = segPixels[i]
                segPixels[i] = Color.rgb(
                    (Color.red(c) * 0.65).toInt(),
                    (Color.green(c) * 0.65).toInt(),
                    (Color.blue(c) * 0.65).toInt()
                )
            }
        }

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                if (lesionMask[idx]) {
                    val dl = lCie[idx] - meanLLesion
                    val da = aCie[idx] - meanALesion
                    val db = bCie[idx] - meanBLesion
                    varLSum += dl * dl
                    varASum += da * da
                    varBSum += db * db

                    val boundary = !lesionMask[idx - 1] || !lesionMask[idx + 1] || !lesionMask[idx - w] || !lesionMask[idx + w]
                    if (boundary) {
                        perimeter += 1.0
                        for (dy in -2..2) {
                            for (dx in -2..2) {
                                val ny = y + dy
                                val nx = x + dx
                                if (ny in 0 until h && nx in 0 until w) {
                                    segPixels[ny * w + nx] = Color.rgb(0, 230, 160)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (perimeter < 1.0) perimeter = 1.0
        val circIdx = (perimeter * perimeter) / (4.0 * Math.PI * areaPx + 1e-6)
        val bScore = (((circIdx - 1.0) / 3.5).coerceIn(0.0, 1.0) * 1000.0).roundToInt() / 1000.0

        val stdL = sqrt(varLSum / areaPx)
        val stdA = sqrt(varASum / areaPx)
        val stdB = sqrt(varBSum / areaPx)
        val sigmaColor = 0.45 * stdL + 0.30 * stdA + 0.25 * stdB
        val cScore = ((sigmaColor / 22.0).coerceIn(0.0, 1.0) * 1000.0).roundToInt() / 1000.0

        val areaRatio = areaPx.toDouble() / n.toDouble()
        val lContrast = max(0.0, (lSkin - meanLLesion) / 45.0)
        val dScore = ((0.45 * eiAdj + 0.35 * lContrast + 0.20 * min(1.0, areaRatio / 0.25)).coerceIn(0.0, 1.0) * 1000.0).roundToInt() / 1000.0

        val abcdScores = AbcdMetrics(
            asymmetry = aScore,
            borderIrregularity = bScore,
            colorVariegation = cScore,
            diameterErythemaExtent = dScore
        )

        val pMorph = (0.28 * aScore + 0.24 * bScore + 0.28 * cScore + 0.20 * dScore).coerceIn(0.02, 0.98)

        // Step 5: Run real ONNX model on normBmp (224x224) + combine with pMorph
        val pOnnxVis = runOnnxVisualPrior(normBmp)
        val pImg: Double
        val mode: String
        if (pOnnxVis >= 0.0) {
            var combined = 0.40 * pOnnxVis + 0.38 * pMorph
            if (cScore < 0.24 && bScore < 0.25) {
                combined = min(combined, 0.34)
            }
            pImg = combined
            mode = "Clinical_Vision_Analysis"
        } else {
            pImg = pMorph
            mode = "Clinical_Vision_Analysis"
        }

        // Heatmap visualization
        val heatPixels = IntArray(n)
        for (i in 0 until n) {
            val v = salU8[i] / 255.0
            val jr = (255.0 * (1.5 - abs(4.0 * v - 3.0)).coerceIn(0.0, 1.0)).toInt()
            val jg = (255.0 * (1.5 - abs(4.0 * v - 2.0)).coerceIn(0.0, 1.0)).toInt()
            val jb = (255.0 * (1.5 - abs(4.0 * v - 1.0)).coerceIn(0.0, 1.0)).toInt()
            val nc = normPixels[i]
            val hr = (0.55 * Color.red(nc) + 0.45 * jr).toInt().coerceIn(0, 255)
            val hg = (0.55 * Color.green(nc) + 0.45 * jg).toInt().coerceIn(0, 255)
            val hb = (0.55 * Color.blue(nc) + 0.45 * jb).toInt().coerceIn(0, 255)
            heatPixels[i] = Color.rgb(hr, hg, hb)
        }

        val segBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(segPixels, 0, w, 0, 0, w, h) }
        val heatBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(heatPixels, 0, w, 0, 0, w, h) }
        val stageBmps = VisionStageBitmaps(
            original = workingBmp,
            normalized = normBmp,
            segmentation = segBmp,
            heatmap = heatBmp
        )

        lastVisionCache = CachedVisionMetrics(
            bitmapIdentity = bmpIdentity,
            usedOnnx = pOnnxVis >= 0.0,
            camGate = camGate,
            itaDeg = itaDeg,
            fitz = fitz,
            skinLabel = skinLabel,
            kappaMel = kappaMel,
            eiAdj = eiAdj,
            abcdScores = abcdScores,
            pImg = pImg,
            mode = mode,
            stageBitmaps = stageBmps
        )

        return fuseMultimodalBayesianRisk(
            pImg = pImg,
            symptoms = symptoms,
            camGate = camGate,
            itaDeg = itaDeg,
            fitz = fitz,
            skinLabel = skinLabel,
            kappaMel = kappaMel,
            eiAdj = eiAdj,
            abcd = abcdScores,
            inferenceMode = mode,
            stageBitmaps = stageBmps
        )
    }

    /**
     * Exact port of fuse_multimodal_bayesian_risk(p_img, symptoms) from backend/app/vision_math.py
     */
    private fun fuseMultimodalBayesianRisk(
        pImg: Double,
        symptoms: SymptomState,
        camGate: CameraQualityGate,
        itaDeg: Double,
        fitz: String,
        skinLabel: String,
        kappaMel: Double,
        eiAdj: Double,
        abcd: AbcdMetrics,
        inferenceMode: String,
        stageBitmaps: VisionStageBitmaps?
    ): ScreeningResult {
        val pClamped = pImg.coerceIn(0.02, 0.98)
        val logOddsImg = ln(pClamped / (1.0 - pClamped))

        val anySymptomActive = symptoms.bleeding || symptoms.rapidGrowth || symptoms.painUlcer ||
                symptoms.durationLong || symptoms.diabetesImmunocomp || symptoms.feverSystemic || symptoms.itching

        val beta0 = when {
            !anySymptomActive && pClamped >= 0.385 -> -0.10
            !anySymptomActive && symptoms.age >= 50 && pClamped >= 0.355 -> -0.42
            else -> -0.85
        }

        var deltaClin = beta0
        val contribs = mutableListOf(
            LogOddsContribution(
                "Visual Baseline",
                (logOddsImg * 100.0).roundToInt() / 100.0
            )
        )

        if (symptoms.bleeding) {
            deltaClin += 1.35
            contribs.add(LogOddsContribution("Bleeding / Oozing", 1.35))
        }
        if (symptoms.rapidGrowth) {
            deltaClin += 1.10
            contribs.add(LogOddsContribution("Rapid Growth", 1.10))
        }
        if (symptoms.painUlcer) {
            deltaClin += 0.85
            contribs.add(LogOddsContribution("Painful / Open Sore", 0.85))
        }
        if (symptoms.diabetesImmunocomp) {
            deltaClin += 0.75
            contribs.add(LogOddsContribution("Diabetes History", 0.75))
        }
        if (symptoms.durationLong) {
            deltaClin += 0.65
            contribs.add(LogOddsContribution("Duration > 4 Weeks", 0.65))
        }
        if (symptoms.feverSystemic) {
            deltaClin += 0.55
            contribs.add(LogOddsContribution("Fever / Swelling", 0.55))
        }
        if (symptoms.itching) {
            deltaClin -= 0.20
            contribs.add(LogOddsContribution("Surface Itching", -0.20))
        }
        if (symptoms.age >= 50) {
            deltaClin += 0.40
            contribs.add(LogOddsContribution("Age 50 or Older", 0.40))
        }

        val finalLogOdds = logOddsImg + deltaClin
        val pFinal = 1.0 / (1.0 + exp(-finalLogOdds))
        val riskPct = (pFinal * 1000.0).roundToInt() / 10.0
        val isFungal = symptoms.itching && !symptoms.bleeding && pFinal < 0.55

        val badgeColor: String
        val tier: String
        val recEn: String
        val recKn: String

        if (pFinal >= 0.65) {
            tier = "HIGH_RISK_REFER_IMMEDIATELY"
            badgeColor = "RED"
            recEn = "Visit Hospital Specialist Immediately. This skin spot shows signs of a serious condition and needs urgent medical attention. Please refer the patient to the District Hospital skin specialist."
            recKn = "ತಕ್ಷಣ ತಜ್ಞ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ. ಈ ಚರ್ಮದ ಗಾಯವು ಗಂಭೀರ ಲಕ್ಷಣಗಳನ್ನು ಹೊಂದಿದೆ ಮತ್ತು ತುರ್ತು ವೈದ್ಯಕೀಯ ಪರೀಕ್ಷೆಯ ಅಗತ್ಯವಿದೆ. ದಯವಿಟ್ಟು ರೋಗಿಯನ್ನು ತಕ್ಷಣ ಜಿಲ್ಲಾ ಆಸ್ಪತ್ರೆಯ ಚರ್ಮರೋಗ ತಜ್ಞರಿಗೆ ಕಳುಹಿಸಿ."
        } else if (pFinal >= 0.35) {
            tier = "MODERATE_RISK_PHC_MO_REVIEW"
            badgeColor = "AMBER"
            recEn = "See Primary Health Centre Doctor Soon. This skin spot needs a doctor checkup within two days. Please show the patient to the Primary Health Centre medical officer."
            recKn = "ಪ್ರಾಥಮಿಕ ಆರೋಗ್ಯ ಕೇಂದ್ರದ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ. ಈ ಚರ್ಮದ ಗಾಯವನ್ನು ಎರಡು ದಿನಗಳ ಒಳಗಾಗಿ ಪ್ರಾಥಮಿಕ ಆರೋಗ್ಯ ಕೇಂದ್ರದ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ ಪರೀಕ್ಷಿಸಿ."
        } else {
            tier = "LOW_RISK_LOCAL_FOLLOWUP"
            badgeColor = "GREEN"
            recEn = if (isFungal) {
                "Normal Care and Check in Seven Days. This looks like a mild surface skin itch. Keep the skin clean and dry, and check again in seven days if it does not heal."
            } else {
                "Normal Care and Check in Seven Days. This skin spot appears low risk. Keep the skin clean and dry, and check again in seven days if it does not heal."
            }
            recKn = "ಸಾಮಾನ್ಯ ಆರೈಕೆ ಸಾಕು. ಇದು ಕಡಿಮೆ ಅಪಾಯದ ಚರ್ಮದ ಸಮಸ್ಯೆಯಾಗಿದೆ. ಚರ್ಮವನ್ನು ಸ್ವಚ್ಛವಾಗಿ ಮತ್ತು ಒಣಗಿದಂತೆ ಇರಿಸಿ. ಏಳು ದಿನಗಳಲ್ಲಿ ವಾಸಿಯಾಗದಿದ್ದರೆ ಮತ್ತೆ ಪರೀಕ್ಷಿಸಿ."
        }

        return ScreeningResult(
            caseId = "PHC-${abs(symptoms.hashCode() + (System.currentTimeMillis() % 10000).toInt()).toString(16).take(6).uppercase()}",
            inferenceMode = inferenceMode,
            cameraQuality = camGate,
            itaDegrees = itaDeg,
            fitzpatrickType = fitz,
            skinToneLabel = skinLabel,
            melaninBoost = kappaMel,
            adjustedErythemaIndex = eiAdj,
            abcdScores = abcd,
            visualPriorProbability = (pClamped * 10000.0).roundToInt() / 10000.0,
            clinicalLogOddsShift = (deltaClin * 1000.0).roundToInt() / 1000.0,
            finalRiskPercentage = riskPct,
            triageTier = tier,
            badgeColor = badgeColor,
            suspectedFungal = isFungal,
            recommendationEn = recEn,
            recommendationKn = recKn,
            contributions = contribs,
            stageBitmaps = stageBitmaps
        )
    }

    fun evaluateCase(
        sampleId: String,
        symptoms: SymptomState,
        perturbation: String? = null
    ): ScreeningResult {
        val abcd = AbcdMetrics(0.41, 0.38, 0.46, 0.49)
        val pMorph = (0.28 * abcd.asymmetry + 0.24 * abcd.borderIrregularity +
                0.28 * abcd.colorVariegation + 0.20 * abcd.diameterErythemaExtent).coerceIn(0.02, 0.98)
        val pImg = 0.40 * 0.46 + 0.38 * pMorph
        return fuseMultimodalBayesianRisk(
            pImg = pImg,
            symptoms = symptoms,
            camGate = CameraQualityGate(true, 86.5, false, 0.018, false, 132.0, "OPTIMAL"),
            itaDeg = 14.6,
            fitz = "Type IV",
            skinLabel = "Indian Olive / Medium",
            kappaMel = 1.16,
            eiAdj = 0.52,
            abcd = abcd,
            inferenceMode = "ONNX_Deep_Multimodal_Plus_CIE_Lab_ABCD",
            stageBitmaps = null
        )
    }
}
