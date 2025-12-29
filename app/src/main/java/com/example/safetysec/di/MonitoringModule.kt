package com.example.safetysec.di

import com.example.safetysec.data.repository.MonitoringRepositoryImpl
import com.example.safetysec.domain.repository.MonitoringRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Monitoring Dependency Injection Module
 *
 * Provides monitoring-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MonitoringModule {

    @Binds
    @Singleton
    abstract fun bindMonitoringRepository(
        impl: MonitoringRepositoryImpl
    ): MonitoringRepository
}