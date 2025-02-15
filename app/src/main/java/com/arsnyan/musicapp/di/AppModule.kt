package com.arsnyan.musicapp.di

import android.content.Context
import android.content.SharedPreferences
import com.arsnyan.musicapp.api.DeezerApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    }
}