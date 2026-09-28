package com.warrior.data.local.di

import com.warrior.data.local.repository.RoomAuthRepository
import com.warrior.data.local.session.LocalSessionManager
import com.warrior.data.local.settings.DataStoreAppPreferences
import com.warrior.domain.auth.AppPreferences
import com.warrior.domain.auth.AuthRepository
import com.warrior.domain.auth.LocalSession
import com.warrior.domain.training.TimerPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: RoomAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAppPreferences(impl: DataStoreAppPreferences): AppPreferences

    @Binds
    @Singleton
    abstract fun bindTimerPreferences(impl: DataStoreAppPreferences): TimerPreferences

    @Binds
    @Singleton
    abstract fun bindLocalSession(impl: LocalSessionManager): LocalSession
}
