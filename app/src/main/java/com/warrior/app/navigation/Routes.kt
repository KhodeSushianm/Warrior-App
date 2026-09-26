package com.warrior.app.navigation

import kotlinx.serialization.Serializable

@Serializable
data object AuthRoute

@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data object ProgressRoute

@Serializable
data object ProfileRoute

@Serializable
data class WorkoutRoute(val sessionId: Long? = null)
