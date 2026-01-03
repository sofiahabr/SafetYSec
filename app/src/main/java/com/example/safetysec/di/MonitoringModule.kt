package com.example.safetysec.di

import com.example.safetysec.data.repository.MonitoringRepositoryImpl
import com.example.safetysec.domain.repository.MonitoringRepository
import com.example.safetysec.service.AlertNotificationService
import com.example.safetysec.service.VideoRecordingService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
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

    companion object {
        @Provides
        @Singleton
        fun provideVideoRecordingService(
            storage: FirebaseStorage
        ): VideoRecordingService {
            return VideoRecordingService(storage)
        }

        @Provides
        @Singleton
        fun provideAlertNotificationService(
            firestore: FirebaseFirestore,
            messaging: FirebaseMessaging
        ): AlertNotificationService {
            return AlertNotificationService(firestore, messaging)
        }
    }
}