package com.couchlist.app.core.data.di

import com.couchlist.app.core.data.remote.provider.EpisodicMetadataProvider
import com.couchlist.app.core.data.remote.provider.MediaMetadataProvider
import com.couchlist.app.core.data.remote.provider.TmdbMetadataProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds
    @IntoSet
    abstract fun bindMetadataProvider(provider: TmdbMetadataProvider): MediaMetadataProvider

    @Binds
    @IntoSet
    abstract fun bindEpisodicMetadataProvider(provider: TmdbMetadataProvider): EpisodicMetadataProvider
}
