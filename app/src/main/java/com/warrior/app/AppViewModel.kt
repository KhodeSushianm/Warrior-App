package com.warrior.app

import androidx.lifecycle.ViewModel
import com.warrior.domain.auth.usecase.ObserveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Root-level session observer: drives Auth vs Main entry point (Architecture §14.2). */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeSession: ObserveSession,
) : ViewModel() {
    val currentUserId: Flow<Long?> = observeSession()
}
