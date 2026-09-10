package com.example.androidsampleapp.di

import com.example.androidsampleapp.data.AirconRepositoryImpl
import com.example.androidsampleapp.data.DeviceRepositoryImpl
import com.example.androidsampleapp.data.NoticeRepositoryImpl
import com.example.androidsampleapp.domain.repository.AirconRepository
import com.example.androidsampleapp.domain.repository.DeviceRepository
import com.example.androidsampleapp.domain.repository.NoticeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * domain の interface に data の実装を結び付ける唯一の場所。
 * ui / domain 側は実装クラスを知らない。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindAirconRepository(impl: AirconRepositoryImpl): AirconRepository

    @Binds
    @Singleton
    abstract fun bindNoticeRepository(impl: NoticeRepositoryImpl): NoticeRepository
}
