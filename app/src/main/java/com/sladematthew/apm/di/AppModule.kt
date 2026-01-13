package com.sladematthew.apm.di

import android.content.Context
import com.sladematthew.apm.AuthRepository
import com.sladematthew.apm.repository.PasswordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing application-wide dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePasswordRepository(
        @ApplicationContext context: Context
    ): PasswordRepository {
        return PasswordRepository(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepository(context)
    }
}
