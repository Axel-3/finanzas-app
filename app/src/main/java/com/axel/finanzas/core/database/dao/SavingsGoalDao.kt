package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.axel.finanzas.core.database.entity.SavingsContributionEntity
import com.axel.finanzas.core.database.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE isActive = 1 ORDER BY id DESC")
    fun observeActive(): Flow<List<SavingsGoalEntity>>

    @Insert
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Query("UPDATE savings_goals SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("SELECT * FROM savings_contributions ORDER BY date DESC")
    fun observeAllContributions(): Flow<List<SavingsContributionEntity>>

    @Insert
    suspend fun insertContribution(contribution: SavingsContributionEntity): Long
}
