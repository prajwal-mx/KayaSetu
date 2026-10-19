package com.kayasetu.asha.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kayasetu.asha.R
import com.kayasetu.asha.data.VillageVisitEntity
import com.kayasetu.asha.data.VillageVisitRepository
import com.kayasetu.asha.domain.AppLanguage
import com.kayasetu.asha.domain.KayaSetuEngine
import com.kayasetu.asha.domain.ScreeningResult
import com.kayasetu.asha.domain.SymptomState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    HOME,
    PHOTO,
    BODY_AND_AGE,
    QUESTION_STEP,
    RESULT
}

data class WizardUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val currentQuestionIndex: Int = 0, // 0 to 6 (1 screen per question)
    val language: AppLanguage = AppLanguage.EN,
    val capturedBitmap: Bitmap? = null,
    val hasPhoto: Boolean = false,
    val patientName: String = "",
    val bodyPart: String = "Arm",
    val ageGroup: String = "20–35",
    val symptoms: SymptomState = SymptomState(
        age = 28,
        bleeding = false,
        rapidGrowth = false,
        painUlcer = false,
        durationLong = false,
        diabetesImmunocomp = false,
        feverSystemic = false,
        itching = false
    ),
    val screeningResult: ScreeningResult = KayaSetuEngine.evaluateCase(
        "case_mod_infective",
        SymptomState(28, false, false, false, false, false, false, false)
    ),
    val showDoctorDetailsSheet: Boolean = false,
    val recordSavedToast: Boolean = false
)

class AshaTriageViewModel(
    private val appContext: Context,
    private val repository: VillageVisitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    val savedVisits: StateFlow<List<VillageVisitEntity>> = repository.allVisits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var recomputeJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            KayaSetuEngine.ensureOnnxLoaded(appContext)
            recomputeAnalysis()
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(language = lang) }
    }

    fun startNewCheck() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.PHOTO,
                currentQuestionIndex = 0,
                capturedBitmap = null,
                hasPhoto = false,
                patientName = "",
                bodyPart = "Arm",
                ageGroup = "20–35",
                symptoms = SymptomState(
                    age = 28,
                    bleeding = false,
                    rapidGrowth = false,
                    painUlcer = false,
                    durationLong = false,
                    diabetesImmunocomp = false,
                    feverSystemic = false,
                    itching = false
                ),
                showDoctorDetailsSheet = false,
                recordSavedToast = false
            )
        }
    }

    fun navigateBack() {
        val current = _uiState.value
        when (current.currentScreen) {
            AppScreen.HOME -> Unit
            AppScreen.PHOTO -> _uiState.update { it.copy(currentScreen = AppScreen.HOME) }
            AppScreen.BODY_AND_AGE -> _uiState.update { it.copy(currentScreen = AppScreen.PHOTO) }
            AppScreen.QUESTION_STEP -> {
                if (current.currentQuestionIndex > 0) {
                    _uiState.update { it.copy(currentQuestionIndex = current.currentQuestionIndex - 1) }
                } else {
                    _uiState.update { it.copy(currentScreen = AppScreen.BODY_AND_AGE) }
                }
            }
            AppScreen.RESULT -> _uiState.update {
                it.copy(currentScreen = AppScreen.QUESTION_STEP, currentQuestionIndex = 6)
            }
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap, cropCircleFromViewfinder: Boolean = false) {
        val processedBmp = if (cropCircleFromViewfinder) {
            KayaSetuEngine.cropToTargetCircleRegion(bitmap)
        } else {
            bitmap
        }
        _uiState.update {
            it.copy(
                capturedBitmap = processedBmp,
                hasPhoto = true
            )
        }
        recomputeAnalysis()
    }

    fun clearCapturedPhoto() {
        _uiState.update {
            it.copy(
                capturedBitmap = null,
                hasPhoto = false
            )
        }
    }

    fun useDefaultSkinPhotoForCheck() {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(appContext.resources, R.drawable.case_mod_infective, opts)
            }
            if (bmp != null) {
                onPhotoCaptured(bmp, cropCircleFromViewfinder = false)
            }
        }
    }

    fun proceedFromPhotoToBodyAndAge() {
        if (!_uiState.value.hasPhoto) {
            useDefaultSkinPhotoForCheck()
        }
        _uiState.update { it.copy(currentScreen = AppScreen.BODY_AND_AGE) }
    }

    fun selectBodyPart(part: String) {
        _uiState.update { it.copy(bodyPart = part) }
    }

    fun selectAgeGroup(groupLabel: String, numericMidpoint: Int) {
        _uiState.update { state ->
            val newSymptoms = state.symptoms.copy(age = numericMidpoint)
            val instantResult = KayaSetuEngine.recomputeFromCachedVisionIfAvailable(
                state.capturedBitmap,
                newSymptoms
            )
            state.copy(
                ageGroup = groupLabel,
                symptoms = newSymptoms,
                screeningResult = instantResult ?: state.screeningResult
            )
        }
        recomputeAnalysis()
    }

    fun updatePatientName(name: String) {
        _uiState.update { it.copy(patientName = name) }
    }

    fun proceedToQuestions() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.QUESTION_STEP,
                currentQuestionIndex = 0
            )
        }
    }

    fun answerCurrentQuestion(answerYes: Boolean) {
        val current = _uiState.value
        val s = current.symptoms
        val idx = current.currentQuestionIndex

        val updatedSymptoms = when (idx) {
            0 -> s.copy(bleeding = answerYes)
            1 -> s.copy(rapidGrowth = answerYes)
            2 -> s.copy(painUlcer = answerYes)
            3 -> s.copy(durationLong = answerYes)
            4 -> s.copy(diabetesImmunocomp = answerYes)
            5 -> s.copy(feverSystemic = answerYes)
            6 -> s.copy(itching = answerYes)
            else -> s
        }

        _uiState.update { state ->
            val instantResult = KayaSetuEngine.recomputeFromCachedVisionIfAvailable(
                state.capturedBitmap,
                updatedSymptoms
            )
            state.copy(
                symptoms = updatedSymptoms,
                screeningResult = instantResult ?: state.screeningResult
            )
        }
        recomputeAnalysis()

        if (idx < 6) {
            _uiState.update { it.copy(currentQuestionIndex = idx + 1) }
        } else {
            _uiState.update { it.copy(currentScreen = AppScreen.RESULT) }
        }
    }

    private fun recomputeAnalysis() {
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch {
            val snap = _uiState.value
            val result = withContext(Dispatchers.Default) {
                val opts = BitmapFactory.Options().apply { inScaled = false }
                val bmp = snap.capturedBitmap ?: BitmapFactory.decodeResource(
                    appContext.resources,
                    R.drawable.case_mod_infective,
                    opts
                )
                if (bmp != null) {
                    KayaSetuEngine.analyzeBitmap(
                        rawBitmap = bmp,
                        sampleId = "case_mod_infective",
                        symptoms = _uiState.value.symptoms,
                        perturbation = null,
                        context = appContext
                    )
                } else {
                    KayaSetuEngine.evaluateCase("case_mod_infective", _uiState.value.symptoms, null)
                }
            }
            _uiState.update { it.copy(screeningResult = result) }
        }
    }

    fun setDoctorDetailsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showDoctorDetailsSheet = visible) }
    }

    fun saveRecordAndReturnHome() {
        val s = _uiState.value
        viewModelScope.launch {
            repository.saveVisit(
                VillageVisitEntity(
                    visitId = "CHK-${System.currentTimeMillis()}",
                    patientName = s.patientName.ifBlank {
                        if (s.language == AppLanguage.KN) "ರೋಗಿ" else "Patient"
                    },
                    ageGroup = s.ageGroup,
                    age = s.symptoms.age,
                    bodyPart = s.bodyPart,
                    badgeColor = s.screeningResult.badgeColor,
                    riskPercentage = s.screeningResult.finalRiskPercentage
                )
            )
            _uiState.update {
                it.copy(
                    recordSavedToast = true,
                    currentScreen = AppScreen.HOME
                )
            }
        }
    }

    fun deleteSavedRecord(visitId: String) {
        viewModelScope.launch {
            repository.deleteVisit(visitId)
        }
    }

    class Factory(
        private val appContext: Context,
        private val repository: VillageVisitRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AshaTriageViewModel(appContext, repository) as T
        }
    }
}
