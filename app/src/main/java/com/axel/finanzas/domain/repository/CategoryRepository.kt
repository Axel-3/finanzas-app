package com.axel.finanzas.domain.repository

import com.axel.finanzas.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeActive(): Flow<List<CategoryEntity>>
}
