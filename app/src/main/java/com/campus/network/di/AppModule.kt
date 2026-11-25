package com.campus.network.di

import android.content.Context
import com.campus.network.BuildConfig
import com.campus.network.CampusApplication
import com.campus.network.core.data.CampusEventsRepository
import com.campus.network.core.data.CampusInboxNotifier
import com.campus.network.core.data.FlashMeetRepository
import com.campus.network.core.data.LiveLocationRepository
import com.campus.network.core.data.PlacesRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.database.dao.FriendDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Bridges existing AppContainer repositories into Hilt's SingletonComponent
 * so HiltViewModels (such as MapViewModel) share the exact app-scoped singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFlashMeetRepository(@ApplicationContext context: Context): FlashMeetRepository =
        (context.applicationContext as CampusApplication).container.flashMeetRepository

    @Provides
    @Singleton
    fun providePlacesRepository(@ApplicationContext context: Context): PlacesRepository =
        (context.applicationContext as CampusApplication).container.placesRepository

    @Provides
    @Singleton
    fun provideStudentProfileRepository(@ApplicationContext context: Context): StudentProfileRepository =
        (context.applicationContext as CampusApplication).container.studentProfileRepository

    @Provides
    @Singleton
    fun provideCampusEventsRepository(@ApplicationContext context: Context): CampusEventsRepository =
        (context.applicationContext as CampusApplication).container.campusEventsRepository

    @Provides
    @Singleton
    fun provideLiveLocationRepository(@ApplicationContext context: Context): LiveLocationRepository =
        (context.applicationContext as CampusApplication).container.liveLocationRepository

    @Provides
    @Singleton
    fun provideCampusInboxNotifier(@ApplicationContext context: Context): CampusInboxNotifier =
        (context.applicationContext as CampusApplication).container.campusInboxNotifier

    @Provides
    @Singleton
    fun provideFriendDao(@ApplicationContext context: Context): FriendDao =
        (context.applicationContext as CampusApplication).container.database.friendDao()

    @Provides
    @Named("mapboxAccessToken")
    fun provideMapboxAccessToken(): String = BuildConfig.MAPBOX_ACCESS_TOKEN
}
