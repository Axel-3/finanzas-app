package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmountCents: Long,
    val targetDate: LocalDate? = null,
    val colorHex: String = "#4F7A6C",
    val icon: String = "savings",
    val isActive: Boolean = true,
)
