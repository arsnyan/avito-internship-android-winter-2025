package com.arsnyan.musicapp.di

import com.arsnyan.musicapp.api.DeezerTrackDataSource
import com.arsnyan.tracklist.network.repository.DeezerTracks
import com.arsnyan.musicapp.local.LocalTrackDataSource
import com.arsnyan.tracklist.network.repository.LocalTracks
import com.arsnyan.tracklist.network.repository.TrackDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class DataSourceModule {
    @Binds
    @ViewModelScoped
    @DeezerTracks
    abstract fun bindDeezerTrackDataSource(dataSource: DeezerTrackDataSource): TrackDataSource

    @Binds
    @ViewModelScoped
    @LocalTracks
    abstract fun bindLocalTrackDataSource(dataSource: LocalTrackDataSource): TrackDataSource
}