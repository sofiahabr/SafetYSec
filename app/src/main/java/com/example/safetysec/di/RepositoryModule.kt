package com.example.safetysec.di

import com.example.safetysec.data.repository.MonitorRepositoryImpl
import com.example.safetysec.data.repository.RuleRepositoryImpl
import com.example.safetysec.domain.repository.MonitorRepository
import com.example.safetysec.domain.repository.RuleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMonitorRepository(
        impl: MonitorRepositoryImpl
    ): MonitorRepository

    @Binds
    @Singleton
    abstract fun bindRuleRepository(
        impl: RuleRepositoryImpl
    ): RuleRepository
}