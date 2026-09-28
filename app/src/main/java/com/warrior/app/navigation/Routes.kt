package com.warrior.app.navigation

import kotlinx.serialization.Serializable

@Serializable
data object AuthRoute

@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data class HistoryDetailRoute(val sessionId: Long)

@Serializable
data object ProgressRoute

@Serializable
data object ProfileRoute

@Serializable
data class WorkoutRoute(val sessionId: Long? = null)

/** Athlete body metrics + 3D profile (Season 2 / Phase 12). */
@Serializable
data object BodyRoute
