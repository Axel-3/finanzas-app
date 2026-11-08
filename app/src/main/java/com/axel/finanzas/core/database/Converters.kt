package com.axel.finanzas.core.database

import androidx.room.TypeConverter
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.CategoryType
import com.axel.finanzas.core.database.entity.MovementSource
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.database.entity.NotificationInboxStatus
import com.axel.finanzas.core.database.entity.NotificationRuleKind
import com.axel.finanzas.core.database.entity.RecurrenceFrequency
import java.time.Instant
import java.time.LocalDate

/** Room no sabe guardar Instant/LocalDate ni enums directamente: los pasamos a Long/String. */
class Converters {

    @TypeConverter
    fun instantToEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun recurrenceFrequencyToString(value: RecurrenceFrequency?): String? = value?.name

    @TypeConverter
    fun stringToRecurrenceFrequency(value: String?): RecurrenceFrequency? = value?.let(RecurrenceFrequency::valueOf)

    @TypeConverter
    fun inboxStatusToString(value: NotificationInboxStatus?): String? = value?.name

    @TypeConverter
    fun stringToInboxStatus(value: String?): NotificationInboxStatus? = value?.let(NotificationInboxStatus::valueOf)

    @TypeConverter
    fun accountTypeToString(value: AccountType?): String? = value?.name

    @TypeConverter
    fun stringToAccountType(value: String?): AccountType? = value?.let(AccountType::valueOf)

    @TypeConverter
    fun categoryTypeToString(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun stringToCategoryType(value: String?): CategoryType? = value?.let(CategoryType::valueOf)

    @TypeConverter
    fun movementTypeToString(value: MovementType?): String? = value?.name

    @TypeConverter
    fun stringToMovementType(value: String?): MovementType? = value?.let(MovementType::valueOf)

    @TypeConverter
    fun movementSourceToString(value: MovementSource?): String? = value?.name

    @TypeConverter
    fun stringToMovementSource(value: String?): MovementSource? = value?.let(MovementSource::valueOf)

    @TypeConverter
    fun notificationRuleKindToString(value: NotificationRuleKind?): String? = value?.name

    @TypeConverter
    fun stringToNotificationRuleKind(value: String?): NotificationRuleKind? = value?.let(NotificationRuleKind::valueOf)
}
