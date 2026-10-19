package com.kayasetu.asha

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kayasetu.asha.data.KayaSetuDatabase
import com.kayasetu.asha.data.VillageVisitEntity
import com.kayasetu.asha.data.VillageVisitRepository
import com.kayasetu.asha.domain.AppLanguage
import com.kayasetu.asha.ui.AppScreen
import com.kayasetu.asha.ui.AshaTriageViewModel
import com.kayasetu.asha.ui.KayaSetuVoiceSpeaker
import com.kayasetu.asha.ui.RearMainCameraViewfinder
import com.kayasetu.asha.ui.WizardUiState
import com.kayasetu.asha.ui.rememberRearMainCameraController
import com.kayasetu.asha.ui.theme.BorderMedium
import com.kayasetu.asha.ui.theme.BorderSubtle
import com.kayasetu.asha.ui.theme.CalmSage
import com.kayasetu.asha.ui.theme.CreamBackground
import com.kayasetu.asha.ui.theme.DeepNavy
import com.kayasetu.asha.ui.theme.IvorySurface
import com.kayasetu.asha.ui.theme.KayaSetuTheme
import com.kayasetu.asha.ui.theme.SoftBlueGray
import com.kayasetu.asha.ui.theme.SoftGold
import com.kayasetu.asha.ui.theme.SoftPeachContainer
import com.kayasetu.asha.ui.theme.SoftSageContainer
import com.kayasetu.asha.ui.theme.SoftSandContainer
import com.kayasetu.asha.ui.theme.StatusHighBg
import com.kayasetu.asha.ui.theme.StatusHighRed
import com.kayasetu.asha.ui.theme.StatusLowBg
import com.kayasetu.asha.ui.theme.StatusLowGreen
import com.kayasetu.asha.ui.theme.StatusModAmber
import com.kayasetu.asha.ui.theme.StatusModBg
import com.kayasetu.asha.ui.theme.TextMuted
import com.kayasetu.asha.ui.theme.TextSecondary
import com.kayasetu.asha.ui.theme.WarmCoral
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
                    factory = AshaTriageViewModel.Factory(applicationContext, repository)
                )
                KayaSetuMinimalApp(viewModel = vm)
            }
        }
    }
}

/**
 * Sanitizes text before passing to Android TextToSpeech so it never reads out
 * brackets, slashes, dashes, or awkward abbreviations in English or Kannada.
 */
private fun cleanTextForNaturalSpeech(rawText: String, isKn: Boolean): String {
    var cleaned = rawText
        // Remove parenthetical notes so TTS never says "bracket" or repeats mixed-language words
        .replace(Regex("\\([^)]*\\)"), " ")
        .replace(Regex("\\[[^]]*]"), " ")
        .replace("/", if (isKn) " ಅಥವಾ " else " or ")
        .replace("—", ". ")
        .replace("–", " to ")
        .replace("·", ". ")
        .replace(">", if (isKn) "ಕ್ಕಿಂತ ಹೆಚ್ಚು " else "more than ")
        .replace("<", if (isKn) "ಕ್ಕಿಂತ ಕಡಿಮೆ " else "under ")

    if (isKn) {
        cleaned = cleaned
            .replace("PHC", "ಪ್ರಾಥಮಿಕ ಆರೋಗ್ಯ ಕೇಂದ್ರ")
            .replace("1", "ಒಂದು")
            .replace("2", "ಎರಡು")
            .replace("4", "ನಾಲ್ಕು")
            .replace("7", "ಏಳು")
            .replace("48", "ನಲವತ್ತೆಂಟು")
    } else {
        cleaned = cleaned
            .replace(Regex("\\bPHC\\b"), "Primary Health Centre")
            .replace(Regex("\\bhrs\\b", RegexOption.IGNORE_CASE), "hours")
            .replace(Regex("\\byrs\\b", RegexOption.IGNORE_CASE), "years")
    }

    return cleaned.replace(Regex("\\s+"), " ").trim()
}

/**
 * Shares the referral message directly to WhatsApp if installed on the device,
 * and falls back gracefully to the system share chooser if WhatsApp is not installed.
 */
private fun shareReferralOnWhatsAppOrChooser(context: Context, shareText: String) {
    val pm = context.packageManager
    val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
    val installedWhatsappPkg = whatsappPackages.firstOrNull { pkg ->
        try {
            pm.getPackageInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    if (installedWhatsappPkg != null) {
        try {
            val waIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(installedWhatsappPkg)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(waIntent)
            return
        } catch (_: Exception) {
            // Fall through to standard chooser if direct launch fails
        }
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Skin Check Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}

@Composable
fun KayaSetuMinimalApp(viewModel: AshaTriageViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedVisits by viewModel.savedVisits.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    viewModel.onPhotoCaptured(bmp)
                }
            }
        }
    }

    val fallbackCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            viewModel.onPhotoCaptured(bmp)
        } else {
            viewModel.useDefaultSkinPhotoForCheck()
        }
    }

    val voiceSpeaker = remember(context) {
        KayaSetuVoiceSpeaker(context)
    }

    DisposableEffect(voiceSpeaker) {
        onDispose {
            voiceSpeaker.shutdown()
        }
    }

    if (state.currentScreen != AppScreen.HOME) {
        BackHandler {
            viewModel.navigateBack()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.currentScreen != AppScreen.HOME) {
                        Surface(
                            shape = CircleShape,
                            color = SoftSandContainer,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("back_button")
                                .clickable { viewModel.navigateBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DeepNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        // Calm Minimalist Brand Logo Badge matching the app's soft theme
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SoftPeachContainer,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .clip(CircleShape)
                            ) {
                                val w = size.width
                                val h = size.height

                                // Warm cream sky circle
                                drawRect(color = CreamBackground)

                                // Pastel sun
                                drawCircle(
                                    color = SoftGold,
                                    radius = h * 0.22f,
                                    center = Offset(w * 0.66f, h * 0.34f)
                                )

                                // Soft blue-gray distant hill
                                val backHill = Path().apply {
                                    moveTo(0f, h * 0.68f)
                                    quadraticTo(w * 0.35f, h * 0.44f, w, h * 0.62f)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(backHill, color = SoftBlueGray)

                                // Warm coral foreground hill
                                val frontHill = Path().apply {
                                    moveTo(0f, h * 0.78f)
                                    quadraticTo(w * 0.48f, h * 0.56f, w, h * 0.75f)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(frontHill, color = WarmCoral)

                                // Deep navy calm base curve
                                val baseCurve = Path().apply {
                                    moveTo(0f, h * 0.86f)
                                    quadraticTo(w * 0.5f, h * 0.74f, w, h * 0.88f)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(baseCurve, color = DeepNavy)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = if (isKn) "ಕಾಯಸೇತು" else "KayaSetu",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepNavy,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Symmetrical, Pixel-Centered English | ಕನ್ನಡ Pill Toggle (Single-line locked for OnePlus/OxygenOS)
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SoftSandContainer)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (!isKn) DeepNavy else Color.Transparent)
                            .testTag("lang_en_button")
                            .clickable { viewModel.setLanguage(AppLanguage.EN) }
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "English",
                            color = if (!isKn) IvorySurface else TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isKn) DeepNavy else Color.Transparent)
                            .testTag("lang_kn_button")
                            .clickable { viewModel.setLanguage(AppLanguage.KN) }
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "ಕನ್ನಡ",
                            color = if (isKn) IvorySurface else TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
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
            AnimatedContent(
                targetState = state.currentScreen,
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> MinimalHomeScreen(
                        visits = savedVisits,
                        isKn = isKn,
                        onStartCheck = { viewModel.startNewCheck() },
                        onDeleteVisit = { viewModel.deleteSavedRecord(it) },
                        onSpeakGreeting = {
                            voiceSpeaker.speak(
                                rawText = if (isKn) {
                                    "ಚರ್ಮದ ಆರೋಗ್ಯ ತಪಾಸಣೆ ಪ್ರಾರಂಭಿಸಲು ಕೆಳಗಿನ ಬಟನ್ ಒತ್ತಿರಿ. ಫೋಟೋ ತೆಗೆಯಿರಿ ಮತ್ತು ಏಳು ಸರಳ ಪ್ರಶ್ನೆಗಳಿಗೆ ಉತ್ತರಿಸಿ."
                                } else {
                                    "Tap Start Skin Check below to take a photo and answer seven simple questions."
                                },
                                isKn = isKn
                            )
                        }
                    )
                    AppScreen.PHOTO -> MinimalPhotoScreen(
                        state = state,
                        isKn = isKn,
                        onPhotoCaptured = { bmp -> viewModel.onPhotoCaptured(bmp, cropCircleFromViewfinder = true) },
                        onRetakePhoto = { viewModel.clearCapturedPhoto() },
                        onFallbackCamera = { fallbackCameraLauncher.launch(null) },
                        onPickGallery = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onContinue = { viewModel.proceedFromPhotoToBodyAndAge() },
                        onSpeakGuide = {
                            voiceSpeaker.speak(
                                rawText = if (isKn) {
                                    "ಚರ್ಮದ ಗಾಯವನ್ನು ವೃತ್ತದ ಒಳಗೆ ಇರಿಸಿ ಮತ್ತು ಸ್ಪಷ್ಟವಾದ ಫೋಟೋ ತೆಗೆಯಲು ಕ್ಯಾಮೆರಾ ಬಟನ್ ಒತ್ತಿರಿ."
                                } else {
                                    "Keep the skin spot inside the circle and tap the Camera button to take a clear photo."
                                },
                                isKn = isKn
                            )
                        }
                    )
                    AppScreen.BODY_AND_AGE -> MinimalBodyAndAgeScreen(
                        state = state,
                        isKn = isKn,
                        onSelectBodyPart = { viewModel.selectBodyPart(it) },
                        onSelectAgeGroup = { label, midpoint -> viewModel.selectAgeGroup(label, midpoint) },
                        onUpdateName = { viewModel.updatePatientName(it) },
                        onNext = { viewModel.proceedToQuestions() },
                        onSpeakPrompt = {
                            voiceSpeaker.speak(
                                rawText = if (isKn) {
                                    "ಚರ್ಮದ ಗಾಯ ದೇಹದ ಯಾವ ಭಾಗದಲ್ಲಿದೆ ಮತ್ತು ರೋಗಿಯ ವಯಸ್ಸಿನ ಗುಂಪು ಯಾವುದು ಎಂದು ಆಯ್ಕೆಮಾಡಿ."
                                } else {
                                    "Select where the skin spot is located on the body and tap the patient age group."
                                },
                                isKn = isKn
                            )
                        }
                    )
                    AppScreen.QUESTION_STEP -> SingleQuestionCardScreen(
                        state = state,
                        isKn = isKn,
                        onAnswer = { answerYes -> viewModel.answerCurrentQuestion(answerYes) },
                        onSpeakQuestion = { spokenText -> voiceSpeaker.speak(spokenText, isKn = isKn) }
                    )
                    AppScreen.RESULT -> MinimalResultScreen(
                        state = state,
                        isKn = isKn,
                        onListenAdvice = { spokenAdvice -> voiceSpeaker.speak(spokenAdvice, isKn = isKn) },
                        onShareWhatsApp = { shareMsg ->
                            shareReferralOnWhatsAppOrChooser(context, shareMsg)
                        },
                        onSaveRecord = { viewModel.saveRecordAndReturnHome() },
                        onOpenDoctorSheet = { viewModel.setDoctorDetailsSheetVisible(true) },
                        onStartNew = { viewModel.startNewCheck() }
                    )
                }
            }
        }
    }

    if (state.showDoctorDetailsSheet) {
        DoctorTechnicalDialog(
            state = state,
            isKn = isKn,
            onDismiss = { viewModel.setDoctorDetailsSheetVisible(false) }
        )
    }
}

// ============================================================================
// SCREEN 1: MINIMAL HOME SCREEN
// ============================================================================
@Composable
private fun MinimalHomeScreen(
    visits: List<VillageVisitEntity>,
    isKn: Boolean,
    onStartCheck: () -> Unit,
    onDeleteVisit: (String) -> Unit,
    onSpeakGreeting: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SoftPeachContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CreamBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        drawCircle(
                            color = SoftGold.copy(alpha = 0.75f),
                            radius = h * 0.26f,
                            center = Offset(w * 0.72f, h * 0.34f)
                        )

                        val backHill = Path().apply {
                            moveTo(0f, h * 0.72f)
                            quadraticTo(w * 0.28f, h * 0.42f, w * 0.62f, h * 0.68f)
                            quadraticTo(w * 0.82f, h * 0.80f, w, h * 0.58f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(backHill, color = SoftBlueGray)

                        val frontHill = Path().apply {
                            moveTo(0f, h * 0.82f)
                            quadraticTo(w * 0.42f, h * 0.56f, w, h * 0.78f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(frontHill, color = WarmCoral.copy(alpha = 0.82f))

                        val baseCurve = Path().apply {
                            moveTo(0f, h * 0.88f)
                            quadraticTo(w * 0.5f, h * 0.76f, w, h * 0.90f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(baseCurve, color = DeepNavy)
                    }

                    Surface(
                        shape = CircleShape,
                        color = IvorySurface,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(48.dp)
                            .testTag("home_listen_button")
                            .clickable { onSpeakGreeting() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen",
                                tint = DeepNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = if (isKn) "ಚರ್ಮದ ಆರೋಗ್ಯ ತಪಾಸಣೆ" else "Skin Health Check",
                    style = MaterialTheme.typography.displaySmall,
                    color = DeepNavy,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isKn) {
                        "ಫೋಟೋ ತೆಗೆಯಿರಿ ಮತ್ತು 7 ಸರಳ ಪ್ರಶ್ನೆಗಳಿಗೆ ಉತ್ತರಿಸಿ"
                    } else {
                        "Take a photo and answer 7 simple questions"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onStartCheck,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("start_skin_check_button")
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = IvorySurface,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isKn) "ತಪಾಸಣೆ ಪ್ರಾರಂಭಿಸಿ" else "Start Skin Check",
                        style = MaterialTheme.typography.titleLarge,
                        color = IvorySurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Explicit Clinical Decision-Support Disclaimer on Home Screen (PS 3.1 Deliverable 3)
        ClinicalDecisionSupportDisclaimer(
            isKn = isKn,
            testTagName = "home_disclaimer_banner"
        )

        // Saved Checks Section (Displays exact selected Age Group like <20, 20–35, etc.)
        if (visits.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isKn) "ಉಳಿಸಿದ ತಪಾಸಣೆಗಳು" else "Recent Checks",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepNavy,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                visits.forEach { visit ->
                    val statusColor = when (visit.badgeColor) {
                        "RED" -> StatusHighRed
                        "AMBER" -> StatusModAmber
                        else -> StatusLowGreen
                    }
                    val statusLabel = when (visit.badgeColor) {
                        "RED" -> if (isKn) "ಜಿಲ್ಲಾ ಆಸ್ಪತ್ರೆಗೆ ಕಳುಹಿಸಿ" else "Visit Hospital"
                        "AMBER" -> if (isKn) "ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ" else "See PHC Doctor"
                        else -> if (isKn) "ಸಾಮಾನ್ಯ ಆರೈಕೆ" else "Normal Care"
                    }
                    val ageDisplay = visit.ageGroup.ifBlank { "${visit.age}" }
                    val ageUnit = if (isKn) "ವರ್ಷ" else "yrs"

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = IvorySurface),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "${visit.patientName} ($ageDisplay $ageUnit)",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = "${visit.bodyPart} · $statusLabel",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = statusColor
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteVisit(visit.visitId) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 2: MINIMAL PHOTO SCREEN (Locks onto Primary 1x Rear Main Camera)
// ============================================================================
@Composable
private fun MinimalPhotoScreen(
    state: WizardUiState,
    isKn: Boolean,
    onPhotoCaptured: (android.graphics.Bitmap) -> Unit,
    onRetakePhoto: () -> Unit,
    onFallbackCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onContinue: () -> Unit,
    onSpeakGuide: () -> Unit
) {
    val context = LocalContext.current
    val q = state.screeningResult.cameraQuality
    val bmp = state.capturedBitmap

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hardwareCameraAvailable by remember { mutableStateOf(true) }
    val rearCameraController = rememberRearMainCameraController()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onFallbackCamera()
        }
    }

    fun triggerCaptureFromRearMainCamera() {
        if (bmp != null) {
            // User tapped Retake to return to live Rear Main Camera
            onRetakePhoto()
            return
        }
        if (hasCameraPermission && hardwareCameraAvailable) {
            val captured = rearCameraController.captureBitmap()
            if (captured != null) {
                onPhotoCaptured(captured)
            } else {
                onFallbackCamera()
            }
        } else if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            onFallbackCamera()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isKn) "ಚರ್ಮದ ಫೋಟೋ ತೆಗೆಯಿರಿ" else "Take Skin Photo",
                    style = MaterialTheme.typography.displaySmall,
                    color = DeepNavy
                )
                Text(
                    text = if (isKn) "ಗಾಯವನ್ನು ವೃತ್ತದ ಒಳಗೆ ಇರಿಸಿ" else "Center the spot inside the circle",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
            Surface(
                shape = CircleShape,
                color = SoftPeachContainer,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onSpeakGuide() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = DeepNavy)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = IvorySurface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 3 Uniform Single-Line Status Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleStatusChip(
                        label = if (isKn) "ಬೆಳಕು" else "Light",
                        ok = q.lightingStatus == "OPTIMAL",
                        modifier = Modifier.weight(1f)
                    )
                    SimpleStatusChip(
                        label = if (isKn) "ಹೊಳಪು ಇಲ್ಲ" else "No Glare",
                        ok = !q.hasGlare,
                        modifier = Modifier.weight(1f)
                    )
                    SimpleStatusChip(
                        label = if (isKn) "ಸ್ಪಷ್ಟ ಫೋಕಸ್" else "Clear Focus",
                        ok = !q.isBlurry,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Viewfinder Box (Live 1x Rear Main Camera Preview or Cropped Circle Photo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(SoftSandContainer)
                        .clickable { triggerCaptureFromRearMainCamera() },
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Captured skin photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (hasCameraPermission && hardwareCameraAvailable) {
                        RearMainCameraViewfinder(
                            controller = rearCameraController,
                            onHardwareUnavailable = { hardwareCameraAvailable = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SoftPeachContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = WarmCoral,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isKn) "ಫೋಟೋ ತೆಗೆಯಲು ಒತ್ತಿ" else "Tap to take photo",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = DeepNavy,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }

                    // Dashed Target Circle Ring (Sized so inner prompt never touches the ring)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = DeepNavy.copy(alpha = 0.45f),
                            radius = size.minDimension * 0.40f,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 14f), 0f)
                            )
                        )
                    }
                }

                // Camera / Retake & Gallery Action Row (Single-line non-wrapping buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { triggerCaptureFromRearMainCamera() },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.5.dp, BorderMedium),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("open_camera_button")
                    ) {
                        Icon(
                            if (bmp != null) Icons.Default.Refresh else Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (bmp != null) {
                                if (isKn) "ಮತ್ತೆ ತೆಗೆಯಿರಿ" else "Retake"
                            } else {
                                if (isKn) "ಕ್ಯಾಮೆರಾ" else "Camera"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                            color = DeepNavy,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onPickGallery,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.5.dp, BorderMedium),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("pick_gallery_button")
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKn) "ಗ್ಯಾಲರಿ" else "Gallery",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                            color = DeepNavy,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                if (bmp == null && hasCameraPermission && hardwareCameraAvailable) {
                    rearCameraController.captureBitmap()?.let { onPhotoCaptured(it) }
                }
                onContinue()
            },
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("continue_from_photo_button")
        ) {
            Text(
                text = if (isKn) "ಮುಂದೆ" else "Continue",
                style = MaterialTheme.typography.titleLarge,
                color = IvorySurface
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = IvorySurface)
        }
    }
}

@Composable
private fun SimpleStatusChip(label: String, ok: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (ok) StatusLowBg else StatusModBg,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (ok) StatusLowGreen else StatusModAmber)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = if (ok) StatusLowGreen else StatusModAmber,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// SCREEN 3: VISUAL BODY AREA & AGE GROUP SCREEN
// ============================================================================
@Composable
private fun MinimalBodyAndAgeScreen(
    state: WizardUiState,
    isKn: Boolean,
    onSelectBodyPart: (String) -> Unit,
    onSelectAgeGroup: (String, Int) -> Unit,
    onUpdateName: (String) -> Unit,
    onNext: () -> Unit,
    onSpeakPrompt: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isKn) "ಮೂಲ ವಿವರಗಳು" else "Basic Details",
                    style = MaterialTheme.typography.displaySmall,
                    color = DeepNavy
                )
                Text(
                    text = if (isKn) "ಗಾಯದ ಸ್ಥಳ ಮತ್ತು ವಯಸ್ಸನ್ನು ಆರಿಸಿ" else "Tap body area and age group",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
            Surface(
                shape = CircleShape,
                color = SoftPeachContainer,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onSpeakPrompt() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = DeepNavy)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = IvorySurface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isKn) "ಗಾಯ ದೇಹದ ಯಾವ ಭಾಗದಲ್ಲಿದೆ?" else "Where is the spot?",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepNavy
                )

                val legIcon = ImageVector.vectorResource(R.drawable.ic_body_leg)
                val footIcon = ImageVector.vectorResource(R.drawable.ic_body_foot)
                val torsoIcon = ImageVector.vectorResource(R.drawable.ic_body_torso)

                val bodyParts = listOf(
                    Triple("Face", if (isKn) "ಮುಖ" else "Face", Icons.Default.Face),
                    Triple("Arm", if (isKn) "ತೋಳು" else "Arm", Icons.Default.PanTool),
                    Triple("Leg", if (isKn) "ಕಾಲು" else "Leg", legIcon),
                    Triple("Foot", if (isKn) "ಪಾದ" else "Foot", footIcon),
                    Triple("Body", if (isKn) "ಮೈ" else "Body", torsoIcon)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bodyParts.forEach { (key, label, iconVec) ->
                        val selected = state.bodyPart == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("body_part_$key")
                                .clickable { onSelectBodyPart(key) }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (selected) DeepNavy else SoftPeachContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        iconVec,
                                        contentDescription = label,
                                        tint = if (selected) IvorySurface else DeepNavy,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) DeepNavy else TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Age Group Selector Card (Stores exact selected Age Group string like "<20", "20–35", etc.)
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = IvorySurface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isKn) "ವಯಸ್ಸಿನ ಗುಂಪು" else "Age Group",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepNavy
                )

                val ageGroups = listOf(
                    "<20" to 16,
                    "20–35" to 28,
                    "36–49" to 42,
                    "50–65" to 58,
                    "65+" to 70
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ageGroups.forEach { (label, midpointVal) ->
                        val selected = state.ageGroup == label
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) DeepNavy else SoftSandContainer,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("age_pill_$midpointVal")
                                .clickable { onSelectAgeGroup(label, midpointVal) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) IvorySurface else DeepNavy,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.patientName,
                    onValueChange = onUpdateName,
                    label = {
                        Text(if (isKn) "ಹೆಸರು (ಐಚ್ಛಿಕ)" else "Patient Name (Optional)")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = onNext,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .testTag("start_questions_button")
        ) {
            Text(
                text = if (isKn) "ಪ್ರಶ್ನೆಗಳನ್ನು ಪ್ರಾರಂಭಿಸಿ" else "Next: Simple Questions",
                style = MaterialTheme.typography.titleLarge,
                color = IvorySurface
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = IvorySurface)
        }
    }
}

// ============================================================================
// SCREEN 4: ONE QUESTION PER SCREEN (Natural Spoken Text for English & Kannada)
// ============================================================================
private data class QuestionStepInfo(
    val icon: ImageVector,
    val accentBg: Color,
    val accentTint: Color,
    val questionEn: String,
    val hintEn: String,
    val spokenEn: String,
    val questionKn: String,
    val hintKn: String,
    val spokenKn: String
)

@Composable
private fun SingleQuestionCardScreen(
    state: WizardUiState,
    isKn: Boolean,
    onAnswer: (Boolean) -> Unit,
    onSpeakQuestion: (String) -> Unit
) {
    val questions = remember {
        listOf(
            QuestionStepInfo(
                icon = Icons.Default.WaterDrop,
                accentBg = SoftPeachContainer,
                accentTint = WarmCoral,
                questionEn = "Is it bleeding or oozing fluid?",
                hintEn = "Blood or fluid coming from the spot",
                spokenEn = "Is it bleeding or oozing fluid? Check if blood or fluid is coming from the skin spot.",
                questionKn = "ಗಾಯದಿಂದ ರಕ್ತ ಅಥವಾ ಕೀವು ಬರುತ್ತಿದೆಯೇ?",
                hintKn = "ತಾನಾಗಿಯೇ ರಕ್ತ ಅಥವಾ ದ್ರವ ಸೋರುವುದು",
                spokenKn = "ಗಾಯದಿಂದ ರಕ್ತ ಅಥವಾ ಕೀವು ಬರುತ್ತಿದೆಯೇ? ತಾನಾಗಿಯೇ ರಕ್ತ ಅಥವಾ ದ್ರವ ಸೋರುತ್ತಿದೆಯೇ ಎಂದು ಗಮನಿಸಿ."
            ),
            QuestionStepInfo(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentBg = SoftBlueGray,
                accentTint = DeepNavy,
                questionEn = "Has it grown fast or changed shape?",
                hintEn = "Getting bigger or changing color",
                spokenEn = "Has it grown fast or changed shape? Check if the spot is getting bigger or changing color.",
                questionKn = "ಇದು ವೇಗವಾಗಿ ಬೆಳೆಯುತ್ತಿದೆಯೇ?",
                hintKn = "ಅಂಚುಗಳು ಹರಡುವುದು ಅಥವಾ ಬಣ್ಣ ಬದಲಾಗುವುದು",
                spokenKn = "ಇದು ವೇಗವಾಗಿ ಬೆಳೆಯುತ್ತಿದೆಯೇ? ಗಾಯದ ಅಂಚುಗಳು ಹರಡುತ್ತಿವೆಯೇ ಅಥವಾ ಬಣ್ಣ ಬದಲಾಗುತ್ತಿದೆಯೇ ಎಂದು ಕೇಳಿ."
            ),
            QuestionStepInfo(
                icon = Icons.Default.Bolt,
                accentBg = SoftPeachContainer,
                accentTint = WarmCoral,
                questionEn = "Is it painful or an open sore?",
                hintEn = "Hurts when touched or has an open wound",
                spokenEn = "Is it painful or an open sore? Ask if it hurts when touched or has an open wound.",
                questionKn = "ಇದು ನೋವಿನಿಂದ ಕೂಡಿದೆಯೇ ಅಥವಾ ತೆರೆದ ಹುಣ್ಣಾಗಿದೆಯೇ?",
                hintKn = "ಮುಟ್ಟಿದರೆ ನೋವು ಅಥವಾ ಒಡೆದ ಚರ್ಮದ ಹುಣ್ಣು",
                spokenKn = "ಇದು ನೋವಿನಿಂದ ಕೂಡಿದೆಯೇ ಅಥವಾ ತೆರೆದ ಹುಣ್ಣಾಗಿದೆಯೇ? ಮುಟ್ಟಿದರೆ ನೋವು ಇದೆಯೇ ಎಂದು ಕೇಳಿ."
            ),
            QuestionStepInfo(
                icon = Icons.Default.CalendarToday,
                accentBg = SoftSageContainer,
                accentTint = CalmSage,
                questionEn = "Has it stayed for more than 1 month?",
                hintEn = "Not healed for over 4 weeks",
                spokenEn = "Has it stayed for more than one month? Ask if the spot has not healed for over four weeks.",
                questionKn = "ಒಂದು ತಿಂಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ವಾಸಿಯಾಗದೆ ಉಳಿದಿದೆಯೇ?",
                hintKn = "ನಾಲ್ಕು ವಾರಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ಇರುವ ಗಾಯ",
                spokenKn = "ಈ ಗಾಯವು ಒಂದು ತಿಂಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ವಾಸಿಯಾಗದೆ ಉಳಿದಿದೆಯೇ? ನಾಲ್ಕು ವಾರಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ಇದೆಯೇ ಎಂದು ಕೇಳಿ."
            ),
            QuestionStepInfo(
                icon = Icons.Default.MonitorHeart,
                accentBg = SoftBlueGray,
                accentTint = DeepNavy,
                questionEn = "Does the patient have Diabetes or high sugar?",
                hintEn = "Known high blood sugar history",
                spokenEn = "Does the patient have diabetes or high blood sugar?",
                questionKn = "ರೋಗಿಗೆ ಸಕ್ಕರೆ ಕಾಯಿಲೆ ಅಥವಾ ಮಧುಮೇಹ ಇದೆಯೇ?",
                hintKn = "ರಕ್ತದಲ್ಲಿ ಸಕ್ಕರೆ ಪ್ರಮಾಣ ಹೆಚ್ಚಿರುವುದು",
                spokenKn = "ರೋಗಿಗೆ ಸಕ್ಕರೆ ಕಾಯಿಲೆ ಅಥವಾ ಮಧುಮೇಹ ಇದೆಯೇ?"
            ),
            QuestionStepInfo(
                icon = Icons.Default.Thermostat,
                accentBg = SoftPeachContainer,
                accentTint = WarmCoral,
                questionEn = "Is there fever or swelling?",
                hintEn = "Warm skin or swelling nearby",
                spokenEn = "Is there fever or swelling? Check for warm skin or swelling near the spot.",
                questionKn = "ಜ್ವರ ಅಥವಾ ಗಂಟುಗಳ ಊತ ಇದೆಯೇ?",
                hintKn = "ಮೈ ಬಿಸಿ ಅಥವಾ ಹತ್ತಿರದಲ್ಲಿ ಊತ",
                spokenKn = "ರೋಗಿಗೆ ಜ್ವರ ಅಥವಾ ಗಂಟುಗಳ ಊತ ಇದೆಯೇ? ಮೈ ಬಿಸಿ ಅಥವಾ ಹತ್ತಿರದಲ್ಲಿ ಊತ ಇದೆಯೇ ಎಂದು ಗಮನಿಸಿ."
            ),
            QuestionStepInfo(
                icon = Icons.Default.PanTool,
                accentBg = SoftSageContainer,
                accentTint = CalmSage,
                questionEn = "Is it very itchy?",
                hintEn = "Feels like ringworm or constant itching",
                spokenEn = "Is it very itchy? Ask if there is constant surface itching.",
                questionKn = "ತುಂಬಾ ತುರಿಕೆ ಇದೆಯೇ?",
                hintKn = "ಹುಳುಕಡ್ಡಿಯಂತೆ ಮೇಲ್ಮೈ ತುರಿಕೆ",
                spokenKn = "ಚರ್ಮದಲ್ಲಿ ತುಂಬಾ ತುರಿಕೆ ಇದೆಯೇ? ಹುಳುಕಡ್ಡಿಯಂತೆ ಮೇಲ್ಮೈ ತುರಿಕೆ ಇದೆಯೇ ಎಂದು ಕೇಳಿ."
            )
        )
    }

    val idx = state.currentQuestionIndex.coerceIn(0, questions.lastIndex)
    val q = questions[idx]
    val mainQuestion = if (isKn) q.questionKn else q.questionEn
    val subHint = if (isKn) q.hintKn else q.hintEn
    val naturalSpoken = if (isKn) q.spokenKn else q.spokenEn

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isKn) "ಪ್ರಶ್ನೆ ${idx + 1} / 7" else "Question ${idx + 1} of 7",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            LinearProgressIndicator(
                progress = { (idx + 1) / 7f },
                color = WarmCoral,
                trackColor = SoftSandContainer,
                modifier = Modifier
                    .width(160.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
        }

        Card(
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = IvorySurface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = q.accentBg,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            q.icon,
                            contentDescription = null,
                            tint = q.accentTint,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Text(
                    text = mainQuestion,
                    style = MaterialTheme.typography.displaySmall,
                    color = DeepNavy,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subHint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SoftSandContainer,
                    modifier = Modifier
                        .testTag("speak_current_question_button")
                        .clickable { onSpeakQuestion(naturalSpoken) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen",
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isKn) "ಪ್ರಶ್ನೆಯನ್ನು ಧ್ವನಿಯಲ್ಲಿ ಕೇಳಿ" else "Listen to Question",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepNavy
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(
                onClick = { onAnswer(false) },
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(2.dp, BorderMedium),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = IvorySurface),
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .testTag("answer_no_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = DeepNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isKn) "ಇಲ್ಲ" else "No",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepNavy
                )
            }

            Button(
                onClick = { onAnswer(true) },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .testTag("answer_yes_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = IvorySurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isKn) "ಹೌದು" else "Yes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = IvorySurface
                )
            }
        }
    }
}

// ============================================================================
// SCREEN 5: CLEAN RESULT SCREEN (Natural TTS + WhatsApp + Visible Doctor Button)
// ============================================================================
@Composable
private fun MinimalResultScreen(
    state: WizardUiState,
    isKn: Boolean,
    onListenAdvice: (String) -> Unit,
    onShareWhatsApp: (String) -> Unit,
    onSaveRecord: () -> Unit,
    onOpenDoctorSheet: () -> Unit,
    onStartNew: () -> Unit
) {
    val res = state.screeningResult
    val statusColor = when (res.badgeColor) {
        "RED" -> StatusHighRed
        "AMBER" -> StatusModAmber
        else -> StatusLowGreen
    }
    val statusBg = when (res.badgeColor) {
        "RED" -> StatusHighBg
        "AMBER" -> StatusModBg
        else -> StatusLowBg
    }

    val simpleHeading = when (res.badgeColor) {
        "RED" -> if (isKn) "ತಕ್ಷಣ ತಜ್ಞ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ" else "Visit Hospital Specialist"
        "AMBER" -> if (isKn) "ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ" else "See PHC Doctor Soon"
        else -> if (isKn) "ಸಾಮಾನ್ಯ ಆರೈಕೆ ಸಾಕು" else "Normal Care & Check in 7 Days"
    }

    val simpleAdvice = when (res.badgeColor) {
        "RED" -> if (isKn) {
            "ಈ ಚರ್ಮದ ಗಾಯಕ್ಕೆ ತಕ್ಷಣ ವೈದ್ಯಕೀಯ ಪರೀಕ್ಷೆಯ ಅಗತ್ಯವಿದೆ. ದಯವಿಟ್ಟು ಹತ್ತಿರದ ಜಿಲ್ಲಾ ಆಸ್ಪತ್ರೆಯ ಚರ್ಮರೋಗ ತಜ್ಞರಿಗೆ ಕಳುಹಿಸಿ."
        } else {
            "This skin spot needs urgent medical attention. Please refer the patient to the District Hospital skin specialist."
        }
        "AMBER" -> if (isKn) {
            "ಇದನ್ನು ಎರಡು ದಿನಗಳ ಒಳಗಾಗಿ ಪ್ರಾಥಮಿಕ ಆರೋಗ್ಯ ಕೇಂದ್ರದ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ ಪರೀಕ್ಷಿಸಿ."
        } else {
            "Please have the Primary Health Centre doctor examine this spot within two days."
        }
        else -> if (isKn) {
            "ಚರ್ಮವನ್ನು ಸ್ವಚ್ಛವಾಗಿ ಮತ್ತು ಒಣಗಿದಂತೆ ಇರಿಸಿ. ಏಳು ದಿನಗಳಲ್ಲಿ ವಾಸಿಯಾಗದಿದ್ದರೆ ಮತ್ತೆ ಪರೀಕ್ಷಿಸಿ."
        } else {
            "Keep the skin clean and dry. Check again in seven days if it does not heal."
        }
    }

    // Pure natural spoken sentence without any brackets, numbers, or abbreviations
    val naturalSpokenAdvice = "$simpleHeading. $simpleAdvice"

    val disclaimerText = if (isKn) {
        "ಗಮನಿಸಿ: ಇದು ಪ್ರಾಥಮಿಕ ತಪಾಸಣೆಯ ಸಹಾಯಕ ಸಾಧನ ಮಾತ್ರ, ವೈದ್ಯಕೀಯ ರೋಗನಿರ್ಣಯದ ಸಾಧನವಲ್ಲ."
    } else {
        "Disclaimer: This is a clinical decision-support aid for PHC staff, not a diagnostic device."
    }

    val whatsappShareMessage = buildString {
        appendLine("*KayaSetu Skin Check Report*")
        if (state.patientName.isNotBlank()) {
            appendLine("Patient: ${state.patientName}")
        }
        appendLine("Age Group: ${state.ageGroup} yrs")
        appendLine("Body Area: ${state.bodyPart}")
        appendLine("Risk Score: ${res.finalRiskPercentage}% | Refer to Specialist Threshold: ≥ 65%")
        appendLine("Action: $simpleHeading")
        appendLine()
        appendLine(simpleAdvice)
        appendLine()
        append(disclaimerText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Clean Result Card (Sized so the entire screen + threshold + disclaimer fits without scrolling)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = IvorySurface),
            border = BorderStroke(2.dp, statusColor.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusBg,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }
                }

                Text(
                    text = simpleHeading,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    color = DeepNavy,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = simpleAdvice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                // Explicit 3-Tier Triage Thresholds Box from vision_math.py (<35% Low, 35-64% PHC, >=65% Specialist)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SoftSandContainer,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("specialist_threshold_box")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isKn) "ಅಪಾಯದ ಪ್ರಮಾಣ" else "Risk Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = "${res.finalRiskPercentage}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isKn) "ತಜ್ಞ ವೈದ್ಯರ ಶಿಫಾರಸು ಮಿತಿ" else "Refer to Specialist Threshold",
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = IvorySurface,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = "≥ 65%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusHighRed,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 3-Mode Triage Scale from vision_math.py (<35% Safe, 35-64% PHC, >=65% Specialist)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isLow = res.badgeColor == "GREEN"
                            val isMod = res.badgeColor == "AMBER"
                            val isHigh = res.badgeColor == "RED"

                            TriageTierChip(
                                label = if (isKn) "<35% ಸಾಮಾನ್ಯ" else "<35% Safe",
                                active = isLow,
                                activeBg = StatusLowBg,
                                activeColor = StatusLowGreen,
                                modifier = Modifier.weight(1f)
                            )
                            TriageTierChip(
                                label = if (isKn) "35–64% ಪಿಎಚ್‌ಸಿ" else "35–64% PHC",
                                active = isMod,
                                activeBg = StatusModBg,
                                activeColor = StatusModAmber,
                                modifier = Modifier.weight(1f)
                            )
                            TriageTierChip(
                                label = if (isKn) "≥65% ತಜ್ಞರು" else "≥65% Specialist",
                                active = isHigh,
                                activeBg = StatusHighBg,
                                activeColor = StatusHighRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Button(
                    onClick = { onListenAdvice(naturalSpokenAdvice) },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPeachContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("listen_to_advice_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen",
                        tint = DeepNavy
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isKn) "ಸಲಹೆಯನ್ನು ಧ್ವನಿಯಲ್ಲಿ ಕೇಳಿ" else "Listen to Advice",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepNavy,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Share on WhatsApp Button
        Button(
            onClick = { onShareWhatsApp(whatsappShareMessage) },
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CalmSage),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("share_whatsapp_button")
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = IvorySurface)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isKn) "ವಾಟ್ಸಾಪ್‌ನಲ್ಲಿ ಕಳುಹಿಸಿ" else "Share on WhatsApp",
                style = MaterialTheme.typography.titleMedium,
                color = IvorySurface,
                maxLines = 1,
                softWrap = false
            )
        }

        // Save Record & New Check Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSaveRecord,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("save_referral_token_button")
            ) {
                Text(
                    text = if (isKn) "ವರದಿ ಉಳಿಸಿ" else "Save Record",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = IvorySurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onStartNew,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.5.dp, BorderMedium),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text(
                    text = if (isKn) "ಹೊಸ ತಪಾಸಣೆ" else "New Check",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = DeepNavy,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Clear, Visible Button Card for Doctor Clinical Details
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SoftSandContainer),
            border = BorderStroke(1.5.dp, BorderMedium),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("doctor_drawer_toggle")
                .clickable { onOpenDoctorSheet() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = IvorySurface,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MonitorHeart,
                                contentDescription = null,
                                tint = DeepNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isKn) "ವೈದ್ಯರಿಗಾಗಿ ತಾಂತ್ರಿಕ ವಿವರಗಳು" else "View Clinical Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isKn) "ವೈದ್ಯರ ವರದಿ ಮತ್ತು ಚಿತ್ರ ವಿಶ್ಲೇಷಣೆ" else "For PHC Doctor · Clinical Summary",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = DeepNavy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Doctor View",
                            tint = IvorySurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Explicit Clinical Decision-Support Disclaimer on Result Screen (PS 3.1 Deliverable 3)
        ClinicalDecisionSupportDisclaimer(
            isKn = isKn,
            testTagName = "result_disclaimer_banner"
        )
    }
}

@Composable
private fun ClinicalDecisionSupportDisclaimer(
    isKn: Boolean,
    testTagName: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftSandContainer,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTagName)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isKn) {
                    "ಗಮನಿಸಿ: ಇದು ಪ್ರಾಥಮಿಕ ತಪಾಸಣೆಯ ಸಹಾಯಕ ಸಾಧನ ಮಾತ್ರ, ವೈದ್ಯಕೀಯ ರೋಗನಿರ್ಣಯದ ಸಾಧನವಲ್ಲ."
                } else {
                    "Disclaimer: This is a clinical decision-support aid for PHC staff, not a diagnostic device."
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                ),
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TriageTierChip(
    label: String,
    active: Boolean,
    activeBg: Color,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) activeBg else IvorySurface,
        border = BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) activeColor.copy(alpha = 0.55f) else BorderSubtle
        ),
        modifier = modifier.height(26.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (active) activeColor else TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// SEPARATE DOCTOR MODAL (Clean, Structured, Clutter-Free Clinical Summary)
// ============================================================================
@Composable
private fun DoctorTechnicalDialog(
    state: WizardUiState,
    isKn: Boolean,
    onDismiss: () -> Unit
) {
    val res = state.screeningResult
    val stageBitmaps = res.stageBitmaps

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = IvorySurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isKn) "ವೈದ್ಯರ ಕ್ಲಿನಿಕಲ್ ವರದಿ" else "PHC Doctor Report",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy
                    )
                    Surface(
                        shape = CircleShape,
                        color = SoftSandContainer,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = DeepNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 1. Clean Patient & Risk Summary Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoftSandContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DoctorMetricRow(
                            label = if (isKn) "ವಯಸ್ಸಿನ ಗುಂಪು" else "Age Group",
                            value = "${state.ageGroup} yrs"
                        )
                        HorizontalDivider(color = BorderSubtle)
                        DoctorMetricRow(
                            label = if (isKn) "ದೇಹದ ಭಾಗ" else "Body Area",
                            value = state.bodyPart
                        )
                        HorizontalDivider(color = BorderSubtle)
                        DoctorMetricRow(
                            label = if (isKn) "ಚರ್ಮದ ಪ್ರಕಾರ" else "Skin Tone",
                            value = res.fitzpatrickType
                        )
                        HorizontalDivider(color = BorderSubtle)
                        DoctorMetricRow(
                            label = if (isKn) "ಅಂದಾಜು ಅಪಾಯ" else "Estimated Risk",
                            value = "${res.finalRiskPercentage}%",
                            highlightValue = true
                        )
                        HorizontalDivider(color = BorderSubtle)
                        DoctorMetricRow(
                            label = if (isKn) "ತಜ್ಞ ವೈದ್ಯರ ಶಿಫಾರಸು ಮಿತಿ" else "Refer to Specialist Threshold",
                            value = "≥ 65%"
                        )
                    }
                }

                // 2. Two Distinct Visual Maps Side-by-Side (Lesion Boundary & Saliency Heatmap)
                if (stageBitmaps != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                bitmap = stageBitmaps.segmentation.asImageBitmap(),
                                contentDescription = "Lesion Boundary",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Text(
                                text = if (isKn) "ಗಾಯದ ಗುರುತು" else "Spot Boundary",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                bitmap = stageBitmaps.heatmap.asImageBitmap(),
                                contentDescription = "Attention Map",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Text(
                                text = if (isKn) "ತೀವ್ರತೆಯ ನಕ್ಷೆ" else "Heat Map",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 3. Clean 2x2 ABCD Feature Grid
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoftSandContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isKn) "ಚರ್ಮದ ಲಕ್ಷಣಗಳು (ABCD)" else "Visual Indicators (ABCD)",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepNavy
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AbcdPillItem(
                                label = "Asymmetry",
                                pct = "${(res.abcdScores.asymmetry * 100).toInt()}%",
                                modifier = Modifier.weight(1f)
                            )
                            AbcdPillItem(
                                label = "Border",
                                pct = "${(res.abcdScores.borderIrregularity * 100).toInt()}%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AbcdPillItem(
                                label = "Color",
                                pct = "${(res.abcdScores.colorVariegation * 100).toInt()}%",
                                modifier = Modifier.weight(1f)
                            )
                            AbcdPillItem(
                                label = "Extent",
                                pct = "${(res.abcdScores.diameterErythemaExtent * 100).toInt()}%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Properly Aligned Clinical Factors Table (No wrapping bullet text)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoftSandContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isKn) "ಪ್ರಮುಖ ಅಂಶಗಳು" else "Risk Factors",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepNavy
                        )
                        res.contributions.forEach { c ->
                            val formattedDelta = String.format(Locale.US, "%+.2f", c.logOddsDelta)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = c.featureLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = IvorySurface
                                ) {
                                    Text(
                                        text = formattedDelta,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepNavy,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (isKn) "ಮುಚ್ಚಿ" else "Close",
                        style = MaterialTheme.typography.labelMedium,
                        color = IvorySurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorMetricRow(
    label: String,
    value: String,
    highlightValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlightValue) WarmCoral else DeepNavy,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun AbcdPillItem(
    label: String,
    pct: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = IvorySurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = pct,
                style = MaterialTheme.typography.titleLarge,
                color = DeepNavy,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

