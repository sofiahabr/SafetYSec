package com.example.safetysec.di

import com.example.safetysec.data.repository.AssociationRepositoryImpl
import com.example.safetysec.data.repository.AuthRepositoryImpl
import com.example.safetysec.domain.repository.AssociationRepository
import com.example.safetysec.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, firestore)

    @Provides
    @Singleton
    fun provideAssociationRepository(
        firestore: FirebaseFirestore
    ): AssociationRepository = AssociationRepositoryImpl(firestore)
}