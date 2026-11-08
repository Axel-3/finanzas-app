package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class RecurrenceFrequency { WEEKLY, BIWEEKLY, MONTHLY, YEARLY }

fun RecurrenceFrequency.advance(date: LocalDate): LocalDate = when (this) {
    RecurrenceFrequency.WEEKLY -> date.plusWeeks(1)
    RecurrenceFrequency.BIWEEKLY -> date.plusWeeks(2)
    RecurrenceFrequency.MONTHLY -> date.plusMonths(1)
    RecurrenceFrequency.YEARLY -> date.plusYears(1)
}

@Entity(
    tableName = "recurring_payments",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("categoryId")],
)
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val frequency: RecurrenceFrequency,
    val accountId: Long,
    val categoryId: Long? = null,
    val type: MovementType = MovementType.EXPENSE,
    val nextDueDate: LocalDate,
    val reminderDaysBefore: Int = 2,
    val isActive: Boolean = true,
)
