package cz.heller.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cz.heller.core.designsystem.component.CalmDialogDismissButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.R
import cz.heller.core.designsystem.component.BarMeter
import cz.heller.core.designsystem.component.CalmCard
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.designsystem.component.CalmPrimaryButton
import cz.heller.core.designsystem.component.CalmTopBar
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.core.money.Money
import cz.heller.core.time.Periods
import cz.heller.data.db.BudgetEntity
import cz.heller.data.db.BudgetPeriod
import cz.heller.data.db.RecordType
import cz.heller.data.repo.BudgetRepository
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.RecordRepository
import cz.heller.feature.analytics.AnalyticsContent
import cz.heller.feature.analytics.AnalyticsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetProgress(
    val budget: BudgetEntity,
    val spentMinor: Long,
) {
    val limitMinor: Long get() = budget.amountMinor
    val fraction: Float get() = if (limitMinor > 0) spentMinor.toFloat() / limitMinor else 0f
    val over: Boolean get() = spentMinor > limitMinor
    val remainingMinor: Long get() = limitMinor - spentMinor
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgets: BudgetRepository,
    records: RecordRepository,
    categories: CategoryRepository,
) : ViewModel() {

    val items: StateFlow<List<BudgetProgress>> = combine(
        budgets.observeAll(), records.observeAll(), categories.observeAll(),
    ) { buds, recs, cats ->
        val byId = cats.associateBy { it.id }
        buds.map { b ->
            val (start, end) = Periods.currentWindow(b.period)
            val spent = recs.asSequence()
                .filter { it.type == RecordType.EXPENSE && it.dateTime in start until end }
                .filter { r ->
                    b.categoryGroupIds.isEmpty() || groupIdOf(r.categoryId, byId) in b.categoryGroupIds
                }
                .sumOf { it.amountMinor }
            BudgetProgress(b, spent)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(budget: BudgetEntity) {
        viewModelScope.launch { budgets.delete(budget) }
    }
}

@StringRes
fun periodLabel(p: BudgetPeriod): Int = when (p) {
    BudgetPeriod.WEEK -> R.string.budget_period_week
    BudgetPeriod.MONTH -> R.string.budget_period_month
    BudgetPeriod.YEAR -> R.string.budget_period_year
}

@Composable
fun BudgetsScreen(
    onAddBudget: () -> Unit,
    onBack: (() -> Unit)? = null,
    vm: BudgetsViewModel = hiltViewModel(),
    analyticsVm: AnalyticsViewModel = hiltViewModel(),
) {
    var showAnalytics by rememberSaveable { mutableStateOf(false) }
    val items by vm.items.collectAsStateWithLifecycle()
    val analytics by analyticsVm.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<BudgetEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        CalmTopBar(stringResource(R.string.nav_budgets), onBack = onBack)

        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip(stringResource(R.string.nav_budgets), selected = !showAnalytics, onClick = { showAnalytics = false }, modifier = Modifier.weight(1f))
            CalmChip(stringResource(R.string.analytics_tab), selected = showAnalytics, onClick = { showAnalytics = true }, modifier = Modifier.weight(1f))
        }

        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (showAnalytics) {
                AnalyticsContent(analytics)
            } else {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CalmPrimaryButton(stringResource(R.string.budget_new), onClick = onAddBudget)
                    if (items.isEmpty()) {
                        Text(
                            stringResource(R.string.budgets_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        items.forEach { bp -> BudgetCard(bp, onDelete = { toDelete = bp.budget }) }
                    }
                }
            }
        }
    }

    val b = toDelete
    if (b != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.budget_delete_title)) },
            text = { Text(stringResource(R.string.budget_delete_message, b.name)) },
            confirmButton = { TextButton(onClick = { vm.delete(b); toDelete = null }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { CalmDialogDismissButton(onClick = { toDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun BudgetCard(bp: BudgetProgress, onDelete: () -> Unit) {
    CalmCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(bp.budget.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(periodLabel(bp.budget.period)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete)) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${Money.format(bp.spentMinor)} / ${Money.format(bp.limitMinor)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BarMeter(fraction = bp.fraction, height = 14.dp)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (bp.over) {
                Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.budget_over, Money.format(-bp.remainingMinor)), style = MaterialTheme.typography.labelLarge)
            } else {
                Text(stringResource(R.string.budget_remaining, Money.format(bp.remainingMinor)), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
