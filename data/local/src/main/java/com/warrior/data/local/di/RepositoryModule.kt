package com.warrior.data.local.di

import com.warrior.data.local.repository.RoomTrainingRepository
import com.warrior.domain.training.TrainingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTrainingRepository(impl: RoomTrainingRepository): TrainingRepository
}
