package com.warrior.domain.progress

import com.warrior.domain.progress.model.HomeProgress
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.progress.usecase.ObserveHomeProgress
import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.usecase.GetTrainingHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveHomeProgressTest {

    private val now = instant(UTC, 2026, 9, 23, hour = 12)
    private val fixedClock = TimeProvider { now }

    private fun useCase(repository: FakeTrainingRepository) = ObserveHomeProgress(
        getTrainingHistory = GetTrainingHistory(repository),
        calculator = ProgressCalculator(WeekBoundaryProvider(UTC)),
        timeProvider = fixedClock,
    )

    private fun cardioSession(date: Long) = TrainingSession(
        userId = 1,
        date = date,
        overallIntensity = 7,
        overallFeeling = Feeling.GOOD,
        activities = listOf(activity(WorkoutType.CARDIO, 30, FocusArea.CONDITIONING)),
    )

    @Test
    fun emitsSnapshot_andRederivesOnEveryRepositoryChange() = runTest {
        val repository = FakeTrainingRepository()
        val observe = useCase(repository)
        repository.createSession(cardioSession(instant(UTC, 2026, 9, 19)))

        val emissions = mutableListOf<HomeProgress>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observe(1).collect { emissions.add(it) }
        }
        assertEquals(1, emissions.size)
        assertEquals(1, emissions.last().thisWeek.sessionCount)
        assertEquals(30L, emissions.last().thisWeek.trainingMinutes)

        // A repository change re-derives the whole snapshot (never stored, §13.4).
        repository.createSession(cardioSession(instant(UTC, 2026, 9, 21)))
        assertEquals(2, emissions.size)
        assertEquals(2, emissions.last().thisWeek.sessionCount)
        assertEquals(60L, emissions.last().thisWeek.trainingMinutes)
    }

    @Test
    fun scopedToTheGivenUser() = runTest {
        val repository = FakeTrainingRepository()
        repository.createSession(cardioSession(instant(UTC, 2026, 9, 19)))

        val snapshot = useCase(repository)(userId = 2).first()
        assertTrue(snapshot.records.isEmpty)
        assertEquals(0, snapshot.thisWeek.sessionCount)
        assertEquals(0, snapshot.records.totalSessions)
    }
}
