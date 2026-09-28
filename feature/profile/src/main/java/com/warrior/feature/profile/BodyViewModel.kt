package com.warrior.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.core.common.time.TimeUtils
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.model.BodyErrorCode
import com.warrior.domain.progress.model.BodyMetric
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.progress.usecase.AddBodyMetric
import com.warrior.domain.progress.usecase.BodyValidationException
import com.warrior.domain.progress.usecase.DeleteBodyMetric
import com.warrior.domain.progress.usecase.ObserveBodyMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Athlete body screen (Season 2 / Phase 12): live metric history, weight trend
 * normalized for the Canvas chart, add/delete with domain validation surfaced
 * as stable [BodyErrorCode]s (i18n-ready).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val observeSession: ObserveSession,
    observeBodyMetrics: ObserveBodyMetrics,
    private val addBodyMetric: AddBodyMetric,
    private val deleteBodyMetric: DeleteBodyMetric,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    data class UiState(
        val loaded: Boolean = false,
        val metrics: List<BodyMetric> = emptyList(),
        val latest: BodyMetric? = null,
        val previousWeightKg: Float? = null,
        val trendPoints: List<Float> = emptyList(),
        val showDialog: Boolean = false,
        val weightText: String = "",
        val heightText: String = "",
        val reachText: String = "",
        val bodyFatText: String = "",
        val heartRateText: String = "",
        val errorCodes: List<BodyErrorCode> = emptyList(),
        val pendingDelete: Long? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession()
                .flatMapLatest { userId ->
                    if (userId == null) flowOf(emptyList()) else observeBodyMetrics(userId)
                }
                .collect { list ->
                    // DAO sorts date DESC — chronological is the reverse.
                    val chronological = list.asReversed().takeLast(TREND_WINDOW)
                    _state.update {
                        it.copy(
                            loaded = true,
                            metrics = list,
                            latest = list.firstOrNull(),
                            previousWeightKg = list.getOrNull(1)?.weightKg,
                            trendPoints = trendPoints(chronological.map { m -> m.weightKg }),
                        )
                    }
                }
        }
    }

    fun onAddOpen() = _state.update {
        it.copy(showDialog = true, errorCodes = emptyList(), weightText = "", heightText = "", reachText = "", bodyFatText = "", heartRateText = "")
    }

    fun onWeightChange(value: String) = _state.update { it.copy(weightText = value, errorCodes = emptyList()) }

    fun onHeightChange(value: String) = _state.update { it.copy(heightText = value, errorCodes = emptyList()) }

    fun onReachChange(value: String) = _state.update { it.copy(reachText = value, errorCodes = emptyList()) }

    fun onBodyFatChange(value: String) = _state.update { it.copy(bodyFatText = value, errorCodes = emptyList()) }

    fun onHeartRateChange(value: String) = _state.update { it.copy(heartRateText = value, errorCodes = emptyList()) }

    fun onDialogCancel() = _state.update { it.copy(showDialog = false, errorCodes = emptyList()) }

    fun onSave() {
        viewModelScope.launch {
            val userId = observeSession().first() ?: return@launch
            val snapshot = _state.value
            val weight = snapshot.weightText.trim().replace(',', '.').toFloatOrNull()
            if (weight == null) {
                _state.update { it.copy(errorCodes = listOf(BodyErrorCode.WEIGHT_RANGE)) }
                return@launch
            }
            val metric = BodyMetric(
                userId = userId,
                date = TimeUtils.localDayMidnightUtcMillis(timeProvider.nowMillis()),
                weightKg = weight,
                heightCm = snapshot.heightText.parseOptionalFloat(),
                reachCm = snapshot.reachText.parseOptionalFloat(),
                bodyFatPercent = snapshot.bodyFatText.parseOptionalFloat(),
                restingHeartRate = snapshot.heartRateText.trim().toIntOrNull(),
            )
            addBodyMetric(userId, metric)
                .onSuccess { _state.update { it.copy(showDialog = false, errorCodes = emptyList()) } }
                .onFailure { error ->
                    val codes = (error as? BodyValidationException)?.codes ?: listOf(BodyErrorCode.UNEXPECTED)
                    _state.update { it.copy(errorCodes = codes) }
                }
        }
    }

    fun onDeleteRequest(metricId: Long) = _state.update { it.copy(pendingDelete = metricId) }

    fun onDeleteDismiss() = _state.update { it.copy(pendingDelete = null) }

    fun onDeleteConfirm() {
        val metricId = _state.value.pendingDelete ?: return
        viewModelScope.launch {
            val userId = observeSession().first() ?: return@launch
            deleteBodyMetric(userId, metricId)
            _state.update { it.copy(pendingDelete = null) }
        }
    }

    private fun String.parseOptionalFloat(): Float? {
        val text = trim().replace(',', '.')
        if (text.isEmpty()) return null
        return text.toFloatOrNull() ?: Float.NaN // NaN fails range validation -> code surfaced
    }

    private fun trendPoints(weights: List<Float>): List<Float> {
        if (weights.size < 2) return emptyList()
        val min = weights.min()
        val max = weights.max()
        if (max - min < 0.05f) return List(weights.size) { 0.5f }
        return weights.map { (it - min) / (max - min) }
    }

    companion object {
        const val TREND_WINDOW = 20
    }
}
