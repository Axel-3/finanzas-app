package com.axel.finanzas.data.repository

import com.axel.finanzas.core.database.dao.CategoryDao
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeActive(): Flow<List<CategoryEntity>> = categoryDao.observeActive()
}
