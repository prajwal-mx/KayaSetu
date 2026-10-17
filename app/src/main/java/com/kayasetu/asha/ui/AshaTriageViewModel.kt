package com.kayasetu.asha.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kayasetu.asha.data.VillageVisitEntity
import com.kayasetu.asha.data.VillageVisitRepository
import com.kayasetu.asha.domain.AppLanguage
import com.kayasetu.asha.domain.KayaSetuEngine
import com.kayasetu.asha.domain.ScreeningResult
import com.kayasetu.asha.domain.SymptomState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WizardUiState(
    val currentStep: Int = 1,
    val language: AppLanguage = AppLanguage.EN,
    val activeSampleId: String = "case_high_scc_ulcer",
    val perturbation: String? = null,
    val patientName: String = "Ramesh Poojary",
    val villageName: String = "Bantwal PHC",
    val bodyPart: String = "Lower Leg",
    val symptoms: SymptomState = SymptomState(),
    val screeningResult: ScreeningResult = KayaSetuEngine.evaluateCase("case_high_scc_ulcer", SymptomState()),
    val isDoctorDrawerExpanded: Boolean = false
)

class AshaTriageViewModel(
    private val repository: VillageVisitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    val villageVisits: StateFlow<List<VillageVisitEntity>> = repository.allVisits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.seedInitialCoastalVisitsIfEmpty()
        }
    }

    fun setStep(step: Int) {
        _uiState.update { it.copy(currentStep = step.coerceIn(1, 4)) }
    }

    fun setLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(language = lang) }
    }

    fun selectSampleCase(sampleId: String, navigateToCamera: Boolean = false) {
        val defaultSymptoms = when (sampleId) {
            "case_high_scc_ulcer" -> SymptomState(61, true, true, true, true, true, false, false)
            "case_mod_infective" -> SymptomState(47, false, false, true, true, false, false, true)
            "case_low_benign_tinea" -> SymptomState(22, false, false, false, false, false, false, true)
            else -> SymptomState(38, false, false, false, false, false, false, false)
        }
        val (pName, pVillage, pBody) = when (sampleId) {
            "case_high_scc_ulcer" -> Triple("Ramesh Poojary", "Bantwal PHC", "Lower Leg")
            "case_mod_infective" -> Triple("Lakshmi Salian", "Ullal PHC", "Arm / Hand")
            "case_low_benign_tinea" -> Triple("Karthik Gowda", "Puttur PHC", "Trunk / Back")
            else -> Triple("Suresh Naik", "Sullia PHC", "Arm / Hand")
        }
        val res = KayaSetuEngine.evaluateCase(sampleId, defaultSymptoms, null)
        _uiState.update {
            it.copy(
                activeSampleId = sampleId,
                perturbation = null,
                patientName = pName,
                villageName = pVillage,
                bodyPart = pBody,
                symptoms = defaultSymptoms,
                screeningResult = res,
                currentStep = if (navigateToCamera) 2 else it.currentStep
            )
        }
    }

    fun applyPerturbation(mode: String?) {
        _uiState.update { current ->
            val res = KayaSetuEngine.evaluateCase(current.activeSampleId, current.symptoms, mode)
            current.copy(perturbation = mode, screeningResult = res)
        }
    }

    fun updatePatientMeta(name: String, village: String) {
        _uiState.update { it.copy(patientName = name, villageName = village) }
    }

    fun selectAgeGroup(age: Int) {
        _uiState.update { current ->
            val updated = current.symptoms.copy(age = age)
            current.copy(
                symptoms = updated,
                screeningResult = KayaSetuEngine.evaluateCase(current.activeSampleId, updated, current.perturbation)
            )
        }
    }

    fun selectBodyPart(part: String) {
        _uiState.update { it.copy(bodyPart = part) }
    }

    fun toggleSymptom(key: String) {
        _uiState.update { current ->
            val s = current.symptoms
            val updated = when (key) {
                "bleeding" -> s.copy(bleeding = !s.bleeding)
                "rapid_growth" -> s.copy(rapidGrowth = !s.rapidGrowth)
                "pain_ulcer" -> s.copy(painUlcer = !s.painUlcer)
                "duration_long" -> s.copy(durationLong = !s.durationLong)
                "diabetes" -> s.copy(diabetesImmunocomp = !s.diabetesImmunocomp)
                "fever" -> s.copy(feverSystemic = !s.feverSystemic)
                "itching" -> s.copy(itching = !s.itching)
                else -> s
            }
            current.copy(
                symptoms = updated,
                screeningResult = KayaSetuEngine.evaluateCase(current.activeSampleId, updated, current.perturbation)
            )
        }
    }

    fun clearAllSymptoms() {
        _uiState.update { current ->
            val updated = SymptomState(age = current.symptoms.age, bleeding = false, rapidGrowth = false, painUlcer = false, durationLong = false, diabetesImmunocomp = false, feverSystemic = false, itching = false)
            current.copy(
                symptoms = updated,
                screeningResult = KayaSetuEngine.evaluateCase(current.activeSampleId, updated, current.perturbation)
            )
        }
    }

    fun toggleDoctorDrawer() {
        _uiState.update { it.copy(isDoctorDrawerExpanded = !it.isDoctorDrawerExpanded) }
    }

    fun saveCurrentVisitToQueue() {
        val s = _uiState.value
        viewModelScope.launch {
            repository.saveVisit(
                VillageVisitEntity(
                    visitId = s.screeningResult.caseId,
                    sampleId = s.activeSampleId,
                    patientName = s.patientName,
                    villageName = s.villageName,
                    age = s.symptoms.age,
                    bodyPart = s.bodyPart,
                    badgeColor = s.screeningResult.badgeColor,
                    riskPercentage = s.screeningResult.finalRiskPercentage
                )
            )
        }
    }

    class Factory(private val repository: VillageVisitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AshaTriageViewModel(repository) as T
        }
    }
}
