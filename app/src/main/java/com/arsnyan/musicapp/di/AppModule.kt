package com.arsnyan.musicapp.di

import com.arsnyan.musicapp.api.DeezerApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDeezerApiService(): DeezerApiService {
        return DeezerApiService.create()
    }
}