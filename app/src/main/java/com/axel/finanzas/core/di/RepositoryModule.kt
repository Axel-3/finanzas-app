package com.axel.finanzas.core.di

import com.axel.finanzas.data.repository.AccountRepositoryImpl
import com.axel.finanzas.data.repository.CardRepositoryImpl
import com.axel.finanzas.data.repository.CategoryRepositoryImpl
import com.axel.finanzas.data.repository.MovementRepositoryImpl
import com.axel.finanzas.domain.repository.AccountRepository
import com.axel.finanzas.domain.repository.CardRepository
import com.axel.finanzas.domain.repository.CategoryRepository
import com.axel.finanzas.domain.repository.MovementRepository
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
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindMovementRepository(impl: MovementRepositoryImpl): MovementRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository
}
