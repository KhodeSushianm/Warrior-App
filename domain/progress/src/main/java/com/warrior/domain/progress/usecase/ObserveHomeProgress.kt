package com.warrior.domain.progress.usecase

import com.warrior.domain.progress.ProgressCalculator
import com.warrior.domain.progress.model.HomeProgress
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.training.usecase.GetTrainingHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Live Home dashboard stream: every Room change re-derives the whole snapshot
 * (never cached across emissions, §13.4). Ownership-scoped by [userId] through
 * [GetTrainingHistory].
 */
class ObserveHomeProgress @Inject constructor(
    private val getTrainingHistory: GetTrainingHistory,
    private val calculator: ProgressCalculator,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(userId: Long, jalali: Boolean = false): Flow<HomeProgress> =
        getTrainingHistory(userId).map { sessions ->
            calculator.homeProgress(sessions, timeProvider.nowMillis(), jalali)
        }
}
