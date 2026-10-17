package com.kayasetu.asha

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kayasetu.asha.data.KayaSetuDatabase
import com.kayasetu.asha.data.VillageVisitEntity
import com.kayasetu.asha.data.VillageVisitRepository
import com.kayasetu.asha.domain.AppLanguage
import com.kayasetu.asha.ui.AshaTriageViewModel
import com.kayasetu.asha.ui.WizardUiState
import com.kayasetu.asha.ui.theme.BorderStrong
import com.kayasetu.asha.ui.theme.CharcoalPrimary
import com.kayasetu.asha.ui.theme.ClinicalAmber
import com.kayasetu.asha.ui.theme.ClinicalAmberBg
import com.kayasetu.asha.ui.theme.ClinicalCrimson
import com.kayasetu.asha.ui.theme.ClinicalCrimsonBg
import com.kayasetu.asha.ui.theme.ClinicalForest
import com.kayasetu.asha.ui.theme.ClinicalForestBg
import com.kayasetu.asha.ui.theme.CreamBackground
import com.kayasetu.asha.ui.theme.IvorySurface
import com.kayasetu.asha.ui.theme.KayaSetuTheme
import com.kayasetu.asha.ui.theme.SandContainer
import com.kayasetu.asha.ui.theme.StoneSecondary
import com.kayasetu.asha.ui.theme.WhatsAppTeal
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = KayaSetuDatabase.getInstance(applicationContext)
        val repository = VillageVisitRepository(database.villageVisitDao())

        setContent {
            KayaSetuTheme {
                val vm: AshaTriageViewModel = viewModel(
                    factory = AshaTriageViewModel.Factory(repository)
                )
                KayaSetuAshaApp(viewModel = vm)
            }
        }
    }
}

@Composable
fun KayaSetuAshaApp(viewModel: AshaTriageViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val visits by viewModel.villageVisits.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale("en", "IN")
            }
        }
        instance
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun speakText(text: String, lang: AppLanguage) {
        tts?.language = if (lang == AppLanguage.KN) Locale("kn", "IN") else Locale("en", "IN")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kayasetu_tts")
    }

    if (state.currentStep > 1) {
        BackHandler {
            viewModel.setStep(state.currentStep - 1)
        }
    }

    val isKn = state.language == AppLanguage.KN

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = CreamBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IvorySurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Offline AI Ready Header Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SandContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ClinicalForest)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKn) "ಆಫ್‌ಲೈನ್ AI ಸಿದ್ಧ · 4.3 MB ಮಾಡೆಲ್" else "Offline AI Ready · 4.3 MB Model on Device",
                            style = MaterialTheme.typography.labelMedium,
                            color = CharcoalPrimary
                        )
                    }

                    // Bilingual Toggle (English | Kannada)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SandContainer)
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (!isKn) CharcoalPrimary else Color.Transparent,
                            modifier = Modifier
                                .testTag("lang_en_button")
                                .clickable { viewModel.setLanguage(AppLanguage.EN) }
                        ) {
                            Text(
                                text = "English",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (!isKn) IvorySurface else StoneSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isKn) CharcoalPrimary else Color.Transparent,
                            modifier = Modifier
                                .testTag("lang_kn_button")
                                .clickable { viewModel.setLanguage(AppLanguage.KN) }
                        ) {
                            Text(
                                text = "ಕನ್ನಡ",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (isKn) IvorySurface else StoneSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4-Step Wizard Navigation Bar
                val stepLabels = if (isKn) {
                    listOf("1. ಗ್ರಾಮ ಪಟ್ಟಿ", "2. ಫೋಟೋ", "3. 7 ಪ್ರಶ್ನೆಗಳು", "4. ಫಲಿತಾಂಶ")
                } else {
                    listOf("1. Queue", "2. Photo", "3. Questions", "4. Result")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stepLabels.forEachIndexed { idx, label ->
                        val stepNum = idx + 1
                        val active = state.currentStep == stepNum
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) CharcoalPrimary else SandContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_step_$stepNum")
                                .clickable { viewModel.setStep(stepNum) }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                color = if (active) IvorySurface else CharcoalPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state.currentStep) {
                    1 -> Screen1HomeVillageQueue(
                        state = state,
                        visits = visits,
                        isKn = isKn,
                        onStartNewScreening = { viewModel.setStep(2) },
                        onSelectPreset = { viewModel.selectSampleCase(it, navigateToCamera = true) },
                        onSelectVisit = { visit ->
                            viewModel.selectSampleCase(visit.sampleId, navigateToCamera = false)
                            viewModel.setStep(4)
                        }
                    )
                    2 -> Screen2GuidedViewfinder(
                        state = state,
                        isKn = isKn,
                        onPerturb = { viewModel.applyPerturbation(it) },
                        onNext = { viewModel.setStep(3) }
                    )
                    3 -> Screen3SymptomTapCards(
                        state = state,
                        isKn = isKn,
                        onSelectAge = { viewModel.selectAgeGroup(it) },
                        onSelectBodyPart = { viewModel.selectBodyPart(it) },
                        onToggleSymptom = { viewModel.toggleSymptom(it) },
                        onSpeakQuestion = { text -> speakText(text, state.language) },
                        onClearAll = { viewModel.clearAllSymptoms() },
                        onCalculateResult = {
                            viewModel.saveCurrentVisitToQueue()
                            viewModel.setStep(4)
                        }
                    )
                    4 -> Screen4TwoLayerResult(
                        state = state,
                        isKn = isKn,
                        onListenAdvice = {
                            val text = if (isKn) state.screeningResult.recommendationKn else state.screeningResult.recommendationEn
                            speakText(text, state.language)
                        },
                        onShareWhatsApp = {
                            val res = state.screeningResult
                            val shareBody = buildString {
                                appendLine("*KAYASETU PHC REFERRAL SLIP*")
                                appendLine("Token: ${res.caseId}")
                                appendLine("Patient: ${state.patientName} (${state.symptoms.age} yrs) - ${state.villageName}")
                                appendLine("Risk Score: ${res.finalRiskPercentage}% (${res.triageTier})")
                                appendLine("Advice (EN): ${res.recommendationEn}")
                                appendLine("ಸಲಹೆ (KN): ${res.recommendationKn}")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share PHC Referral Slip"))
                        },
                        onSaveToken = { viewModel.saveCurrentVisitToQueue() },
                        onToggleDoctorDrawer = { viewModel.toggleDoctorDrawer() }
                    )
                }
            }
        }
    }
}

@Composable
private fun Screen1HomeVillageQueue(
    state: WizardUiState,
    visits: List<VillageVisitEntity>,
    isKn: Boolean,
    onStartNewScreening: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onSelectVisit: (VillageVisitEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = IvorySurface),
        border = BorderStroke(1.dp, BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (isKn) "3 ಸುಲಭ ಹಂತಗಳಲ್ಲಿ ಚರ್ಮದ ತಪಾಸಣೆ ಮಾಡಿ." else "Screen a skin spot in 3 simple steps.",
                style = MaterialTheme.typography.displaySmall,
                color = CharcoalPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isKn) {
                    "ಬಿಸಿಲಿನಲ್ಲಿ ಒಂದೇ ಕೈಯಲ್ಲಿ ಬಳಸಲು ವಿನ್ಯಾಸಗೊಳಿಸಲಾಗಿದೆ. ಇಂಟರ್ನೆಟ್ ಇಲ್ಲದೆಯೂ 100% ಆಫ್‌ಲೈನ್‌ನಲ್ಲಿ ಕಾರ್ಯನಿರ್ವಹಿಸುತ್ತದೆ."
                } else {
                    "Designed for one-handed use in bright village sunlight. Works 100% offline without internet."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = StoneSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Giant Matte-Charcoal Pill CTA in Thumb Zone
            Button(
                onClick = onStartNewScreening,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("new_patient_screening_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isKn) "ಹೊಸ ರೋಗಿಯ ತಪಾಸಣೆ ಪ್ರಾರಂಭಿಸಿ" else "New Patient Screening",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Start")
            }
        }
    }

    // Today's Village Visits List
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = IvorySurface),
        border = BorderStroke(1.dp, BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKn) "ಇಂದಿನ ಗ್ರಾಮ ಭೇಟಿಗಳು" else "Today’s Village Visits",
                    style = MaterialTheme.typography.headlineMedium
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ClinicalForestBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Offline Ready",
                            tint = ClinicalForest,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isKn) "ಆಫ್‌ಲೈನ್ ಸಿದ್ಧ" else "Offline Ready",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClinicalForest
                        )
                    }
                }
            }

            visits.forEach { visit ->
                val badgeLabel = when (visit.badgeColor) {
                    "RED" -> if (isKn) "ವೆನ್ಲಾಕ್ ಆಸ್ಪತ್ರೆಗೆ ಕಳುಹಿಸಿ" else "Send to Wenlock Hospital"
                    "AMBER" -> if (isKn) "ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ" else "Show PHC Doctor"
                    else -> if (isKn) "7 ದಿನಗಳ ನಂತರ ಪರೀಕ್ಷಿಸಿ" else "Check in 7 Days"
                }
                val dotColor = when (visit.badgeColor) {
                    "RED" -> ClinicalCrimson
                    "AMBER" -> ClinicalAmber
                    else -> ClinicalForest
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CreamBackground,
                    border = BorderStroke(1.dp, BorderStrong),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("visit_item_${visit.visitId}")
                        .clickable { onSelectVisit(visit) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${visit.patientName} (${visit.age}y)",
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalPrimary
                                )
                                Text(
                                    text = "${visit.villageName} · ${visit.bodyPart}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StoneSecondary
                                )
                            }
                        }
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = dotColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Screen2GuidedViewfinder(
    state: WizardUiState,
    isKn: Boolean,
    onPerturb: (String?) -> Unit,
    onNext: () -> Unit
) {
    val q = state.screeningResult.cameraQuality
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = IvorySurface),
        border = BorderStroke(1.dp, BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (isKn) "ಚರ್ಮದ ಫೋಟೋ ಮಾರ್ಗದರ್ಶಿ" else "Guided Camera Viewfinder",
                style = MaterialTheme.typography.headlineMedium
            )

            // 3 Live Traffic-Light Pills
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TrafficLightPill(
                    text = if (isKn) "ಬೆಳಕು: ಉತ್ತಮ" else "Light: Good",
                    passed = true,
                    modifier = Modifier.weight(1f)
                )
                TrafficLightPill(
                    text = if (!q.hasGlare) (if (isKn) "ಪ್ರತಿಫಲನ: ಸರಿ" else "Glare: OK") else (if (isKn) "ಪ್ರತಿಫಲನ: ಹೆಚ್ಚು" else "Glare: High"),
                    passed = !q.hasGlare,
                    modifier = Modifier.weight(1f)
                )
                TrafficLightPill(
                    text = if (!q.isBlurry) (if (isKn) "ಫೋಕಸ್: ಸ್ಪಷ್ಟ" else "Focus: Steady") else (if (isKn) "ಫೋಕಸ್: ಮಂದ" else "Focus: Blurry"),
                    passed = !q.isBlurry,
                    modifier = Modifier.weight(1f)
                )
            }

            // Circular Target Ring Viewfinder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF27272A)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension * 0.34f
                    drawCircle(
                        color = Color(0xFF9A6B56),
                        radius = radius * 1.15f
                    )
                    drawCircle(
                        color = Color(0xFF5A231E),
                        radius = radius * 0.42f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = CharcoalPrimary.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isKn) {
                            "ಚರ್ಮದ ಕಲೆಯನ್ನು ವೃತ್ತದ ಒಳಗೆ ಇರಿಸಿ (1 ಗೇಣು / 15 ಸೆಂ.ಮೀ ದೂರ)"
                        } else {
                            "Keep the skin spot inside the circle (1 hand-span / 15 cm away)"
                        },
                        color = IvorySurface,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (!q.qualityPassed) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ClinicalAmberBg,
                    border = BorderStroke(1.5.dp, ClinicalAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("retake_warning_banner")
                        .clickable { onPerturb(null) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = ClinicalAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isKn) "ಫೋಟೋ ಮಂದವಾಗಿದೆ — ಮರು-ಫೋಟೋ ತೆಗೆಯಲು ಒತ್ತಿ" else "Photo is blurry — Tap to Retake",
                            fontWeight = FontWeight.Bold,
                            color = ClinicalAmber
                        )
                    }
                }
            }

            // Unseen Perturbation Sandbox Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { onPerturb("darker_skin") },
                    modifier = Modifier.weight(1f).testTag("perturb_darker_button")
                ) {
                    Text(if (isKn) "+ ಗಾಢ ಚರ್ಮ" else "+ Melanin", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { onPerturb("glare_flash") },
                    modifier = Modifier.weight(1f).testTag("perturb_glare_button")
                ) {
                    Text(if (isKn) "+ ಹೊಳಪು" else "+ Glare", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { onPerturb("camera_shake") },
                    modifier = Modifier.weight(1f).testTag("perturb_blur_button")
                ) {
                    Text(if (isKn) "+ ಮಂದತೆ" else "+ Blur", style = MaterialTheme.typography.labelMedium)
                }
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("proceed_to_questions_button")
            ) {
                Text(if (isKn) "ಮುಂದೆ: 7 ಪ್ರಶ್ನೆಗಳನ್ನು ಕೇಳಿ" else "Next: Ask 7 Questions")
            }
        }
    }
}

@Composable
private fun TrafficLightPill(text: String, passed: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (passed) ClinicalForestBg else ClinicalCrimsonBg,
        border = BorderStroke(1.dp, if (passed) ClinicalForest else ClinicalCrimson),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = if (passed) ClinicalForest else ClinicalCrimson,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
        )
    }
}

@Composable
private fun Screen3SymptomTapCards(
    state: WizardUiState,
    isKn: Boolean,
    onSelectAge: (Int) -> Unit,
    onSelectBodyPart: (String) -> Unit,
    onToggleSymptom: (String) -> Unit,
    onSpeakQuestion: (String) -> Unit,
    onClearAll: () -> Unit,
    onCalculateResult: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = IvorySurface),
        border = BorderStroke(1.dp, BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKn) "ರೋಗಿಯ ವಿವರ ಮತ್ತು ಲಕ್ಷಣಗಳು" else "Patient Details & Symptoms",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (isKn) "ಎಲ್ಲಾ ಅಳಿಸಿ" else "Clear all",
                    style = MaterialTheme.typography.labelMedium,
                    color = StoneSecondary,
                    modifier = Modifier
                        .testTag("clear_all_symptoms_button")
                        .clickable { onClearAll() }
                )
            }

            // Quick-Tap Age Group Pills
            Text(
                text = if (isKn) "ರೋಗಿಯ ವಯಸ್ಸಿನ ಗುಂಪು (${state.symptoms.age} ವರ್ಷ)" else "Patient Age Group (${state.symptoms.age} yrs)",
                fontWeight = FontWeight.Bold
            )
            val ageGroups = listOf("<20" to 16, "20–35" to 28, "36–49" to 42, "50–65" to 61, "65+" to 72)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ageGroups.forEach { (label, ageVal) ->
                    val selected = state.symptoms.age == ageVal
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) CharcoalPrimary else SandContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("age_pill_$ageVal")
                            .clickable { onSelectAge(ageVal) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                color = if (selected) IvorySurface else CharcoalPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Body-Location Quick Tap
            val bodyParts = listOf(
                "Face" to (if (isKn) "ಮುಖ" else "Face"),
                "Arm" to (if (isKn) "ತೋಳು" else "Arm"),
                "Lower Leg" to (if (isKn) "ಕಾಲು" else "Leg"),
                "Foot" to (if (isKn) "ಪಾದ" else "Foot"),
                "Trunk" to (if (isKn) "ಮುಂಡ" else "Trunk")
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                bodyParts.forEach { (key, label) ->
                    val selected = state.bodyPart == key
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) CharcoalPrimary else SandContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("body_part_$key")
                            .clickable { onSelectBodyPart(key) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                color = if (selected) IvorySurface else CharcoalPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // 7 Large Yes/No Tap Cards (56dp+ height) with Audio Speaker Button
            val items = listOf(
                Triple("bleeding", state.symptoms.bleeding, if (isKn) "ಗಾಯದಿಂದ ರಕ್ತ ಅಥವಾ ಕೀವು ಬರುತ್ತಿದೆಯೇ?" else "Is it bleeding or oozing fluid?"),
                Triple("rapid_growth", state.symptoms.rapidGrowth, if (isKn) "ಇದು ವೇಗವಾಗಿ ಬೆಳೆಯುತ್ತಿದೆಯೇ ಅಥವಾ ಆಕಾರ ಬದಲಾಗಿದೆಯೇ?" else "Has it grown fast or changed shape?"),
                Triple("pain_ulcer", state.symptoms.painUlcer, if (isKn) "ಇದು ನೋವಿನಿಂದ ಕೂಡಿದೆಯೇ ಅಥವಾ ತೆರೆದ ಹುಣ್ಣಾಗಿದೆಯೇ?" else "Is it painful or an open sore?"),
                Triple("duration_long", state.symptoms.durationLong, if (isKn) "1 ತಿಂಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ವಾಸಿಯಾಗದೆ ಉಳಿದಿದೆಯೇ?" else "Has it stayed unhealed for > 1 month?"),
                Triple("diabetes", state.symptoms.diabetesImmunocomp, if (isKn) "ರೋಗಿಗೆ ಸಕ್ಕರೆ ಕಾಯಿಲೆ (ಮಧುಮೇಹ) ಇದೆಯೇ?" else "Does the patient have Sugar (Diabetes)?"),
                Triple("fever", state.symptoms.feverSystemic, if (isKn) "ಜ್ವರ ಅಥವಾ ಗಂಟುಗಳ ಊತ ಇದೆಯೇ?" else "Is there fever or swelling?"),
                Triple("itching", state.symptoms.itching, if (isKn) "ತುಂಬಾ ತುರಿಕೆ ಇದೆಯೇ (ಹುಳುಕಡ್ಡಿಯಂತೆ)?" else "Is it very itchy (like ringworm)?")
            )

            items.forEach { (key, checked, questionText) ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (checked) IvorySurface else CreamBackground,
                    border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) CharcoalPrimary else BorderStrong),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .testTag("symptom_card_$key")
                        .clickable { onToggleSymptom(key) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = questionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onSpeakQuestion(questionText) },
                            modifier = Modifier.testTag("speak_symptom_$key")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read question aloud",
                                tint = CharcoalPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (checked) CharcoalPrimary else SandContainer
                        ) {
                            Text(
                                text = if (checked) (if (isKn) "ಹೌದು" else "YES") else (if (isKn) "ಇಲ್ಲ" else "NO"),
                                color = if (checked) IvorySurface else StoneSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onCalculateResult,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("calculate_triage_result_button")
            ) {
                Text(if (isKn) "ತಪಾಸಣಾ ಫಲಿತಾಂಶ ನೋಡಿ" else "Calculate Triage Advice")
            }
        }
    }
}

@Composable
private fun Screen4TwoLayerResult(
    state: WizardUiState,
    isKn: Boolean,
    onListenAdvice: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onSaveToken: () -> Unit,
    onToggleDoctorDrawer: () -> Unit
) {
    val res = state.screeningResult
    val bannerColor = when (res.badgeColor) {
        "RED" -> ClinicalCrimson
        "AMBER" -> ClinicalAmber
        else -> ClinicalForest
    }
    val bannerBg = when (res.badgeColor) {
        "RED" -> ClinicalCrimsonBg
        "AMBER" -> ClinicalAmberBg
        else -> ClinicalForestBg
    }

    // Top Layer: ASHA Worker & Patient Action Card
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = bannerBg),
        border = BorderStroke(2.dp, bannerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(999.dp), color = bannerColor) {
                    Text(
                        text = res.triageTier.replace("_", " "),
                        color = IvorySurface,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = "${res.finalRiskPercentage}%",
                    style = MaterialTheme.typography.displaySmall,
                    color = CharcoalPrimary
                )
            }

            Text(
                text = if (isKn) res.recommendationKn else res.recommendationEn,
                style = MaterialTheme.typography.bodyLarge,
                color = CharcoalPrimary
            )

            // Prominent Voice-First Listen to Advice Button
            OutlinedButton(
                onClick = onListenAdvice,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.5.dp, CharcoalPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("listen_to_advice_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen to Advice")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isKn) "ಸಲಹೆಯನ್ನು ಧ್ವನಿಯಲ್ಲಿ ಕೇಳಿ" else "Listen to Advice (Read Aloud)")
            }

            LinearProgressIndicator(
                progress = { (res.finalRiskPercentage.toFloat() / 100f).coerceIn(0f, 1f) },
                color = bannerColor,
                trackColor = SandContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            // Primary Field Actions: Share on WhatsApp + Save Token
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShareWhatsApp,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("share_whatsapp_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share WhatsApp")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isKn) "ವಾಟ್ಸಾಪ್ ಚೀಟಿ" else "WhatsApp Slip")
                }

                Button(
                    onClick = onSaveToken,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("save_referral_token_button")
                ) {
                    Text(if (isKn) "ಟೋಕನ್ ಉಳಿಸಿ" else "Save Token")
                }
            }
        }
    }

    // Bottom Accordion: For PHC Doctor Technical Drawer
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = IvorySurface),
        border = BorderStroke(1.dp, BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SandContainer)
                    .testTag("doctor_drawer_toggle")
                    .clickable { onToggleDoctorDrawer() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKn) {
                        "ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರಿಗಾಗಿ: AI ಹೀಟ್‌ಮ್ಯಾಪ್ ಮತ್ತು ABCD ಗಣಿತ ನೋಡಿ"
                    } else {
                        "For PHC Doctor: View AI Heatmaps & ABCD Math"
                    },
                    fontWeight = FontWeight.Bold,
                    color = CharcoalPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ExpandMore, contentDescription = "Expand Technical View")
            }

            AnimatedVisibility(visible = state.isDoctorDrawerExpanded) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Skin Tone: ${res.fitzpatrickType} (${res.skinToneLabel} · ITA° ${res.itaDegrees}°) · Melanin Boost ×${res.melaninBoost}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "ABCD Scores — A: ${(res.abcdScores.asymmetry * 100).toInt()}% | B: ${(res.abcdScores.borderIrregularity * 100).toInt()}% | C: ${(res.abcdScores.colorVariegation * 100).toInt()}% | D: ${(res.abcdScores.diameterErythemaExtent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    res.contributions.forEach { contrib ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(contrib.featureLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                text = "${if (contrib.logOddsDelta >= 0) "+" else ""}${contrib.logOddsDelta}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Non-Intrusive Always-Visible Safety Footer
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = SandContainer,
        border = BorderStroke(1.dp, BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isKn) {
                "ಇದು ಪ್ರಾಥಮಿಕ ತಪಾಸಣಾ ಸಹಾಯಕ ಮಾತ್ರ — ಅಂತಿಮ ನಿರ್ಧಾರ ವೈದ್ಯಾಧಿಕಾರಿಗಳದ್ದು."
            } else {
                "Decision-Support Aid for PHC Triage — Final diagnosis by Medical Officer."
            },
            style = MaterialTheme.typography.labelMedium,
            color = StoneSecondary,
            modifier = Modifier.padding(12.dp)
        )
    }
}
