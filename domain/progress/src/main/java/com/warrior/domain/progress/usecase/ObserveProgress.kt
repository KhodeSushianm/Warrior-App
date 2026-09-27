package com.warrior.domain.progress.usecase

import com.warrior.domain.progress.ProgressCalculator
import com.warrior.domain.progress.model.ProgressSnapshot
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.training.usecase.GetTrainingHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Live Progress screen stream (Phase 8): every Room change re-derives the
 * whole snapshot — charts render from this derived flow only, never from
 * stored metrics (§13.4). Ownership-scoped by [userId].
 */
class ObserveProgress @Inject constructor(
    private val getTrainingHistory: GetTrainingHistory,
    private val calculator: ProgressCalculator,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(userId: Long): Flow<ProgressSnapshot> =
        getTrainingHistory(userId).map { sessions ->
            calculator.progressScreen(sessions, timeProvider.nowMillis())
        }
}
