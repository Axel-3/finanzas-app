package com.axel.finanzas.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.SavingsGoalDao
import com.axel.finanzas.core.database.entity.SavingsContributionEntity
import com.axel.finanzas.core.database.entity.SavingsGoalEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalDao: SavingsGoalDao,
) : ViewModel() {

    val goals: StateFlow<List<GoalUi>> = combine(
        goalDao.observeActive(),
        goalDao.observeAllContributions(),
    ) { goals, contributions ->
        val byGoal = contributions.groupBy { it.goalId }
        goals.map { goal ->
            GoalUi(
                id = goal.id,
                name = goal.name,
                targetAmountCents = goal.targetAmountCents,
                currentAmountCents = byGoal[goal.id]?.sumOf { it.amountCents } ?: 0L,
                targetDate = goal.targetDate,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(name: String, targetAmountCents: Long, targetDate: LocalDate?) {
        viewModelScope.launch {
            goalDao.insert(SavingsGoalEntity(name = name, targetAmountCents = targetAmountCents, targetDate = targetDate))
        }
    }

    fun addContribution(goalId: Long, amountCents: Long, note: String?) {
        viewModelScope.launch {
            goalDao.insertContribution(
                SavingsContributionEntity(goalId = goalId, amountCents = amountCents, date = LocalDate.now(), note = note),
            )
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch { goalDao.deactivate(goalId) }
    }
}
