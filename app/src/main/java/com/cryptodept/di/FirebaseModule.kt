package com.cryptodept.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
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
    fun provideFirebaseDatabase(): FirebaseDatabase {
        val url = "https://cryptodept-1b2a6-default-rtdb.europe-west1.firebasedatabase.app"
        return FirebaseDatabase.getInstance(url).apply {
            setPersistenceEnabled(true)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        // Use europe-west1 to match RTDB location
        return FirebaseFunctions.getInstance("europe-west1")
    }
}
