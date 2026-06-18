package cz.heller.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import cz.heller.R
import cz.heller.core.designsystem.CategoryIcons
import cz.heller.core.designsystem.component.BarMeter
import cz.heller.core.designsystem.component.CalmCard
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.core.designsystem.component.SectionHeader
import cz.heller.core.money.Money
import cz.heller.core.time.Periods
import cz.heller.data.db.RecordType
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.RecordRepository
import cz.heller.feature.budgets.groupIdOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class LeaderItem(val name: String, val icon: String, val amountMinor: Long)
data class CashFlowMonth(val label: String, val incomeMinor: Long, val expenseMinor: Long)

data class AnalyticsUiState(
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val leaderboard: List<LeaderItem> = emptyList(),
    val maxLeaderMinor: Long = 0,
    val cashflow: List<CashFlowMonth> = emptyList(),
    val maxCashflowMinor: Long = 0,
) {
    val savingMinor: Long get() = incomeMinor - expenseMinor
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    records: RecordRepository,
    categories: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<AnalyticsUiState> = combine(
        records.observeAll(), categories.observeAll(),
    ) { recs, cats ->
        val byId = cats.associateBy { it.id }
        val groups = cats.filter { it.parentId == null }.associateBy { it.id }
        val (mStart, mEnd) = Periods.monthWindow(YearMonth.now())
        val monthRecs = recs.filter { it.dateTime in mStart until mEnd }

        val income = monthRecs.filter { it.type == RecordType.INCOME }.sumOf { it.amountMinor }
        val expense = monthRecs.filter { it.type == RecordType.EXPENSE }.sumOf { it.amountMinor }

        val byGroup = LinkedHashMap<String, Long>()
        monthRecs.filter { it.type == RecordType.EXPENSE }.forEach { r ->
            val gid = groupIdOf(r.categoryId, byId) ?: "__none"
            byGroup[gid] = (byGroup[gid] ?: 0L) + r.amountMinor
        }
        val leaderboard = byGroup.entries
            .sortedByDescending { it.value }
            .map { (gid, amount) ->
                val g = groups[gid]
                LeaderItem(
                    name = g?.name ?: context.getString(R.string.no_category),
                    icon = g?.icon ?: "more_horiz",
                    amountMinor = amount,
                )
            }
        val maxLeader = leaderboard.maxOfOrNull { it.amountMinor } ?: 0L

        val cashflow = Periods.lastMonths(6).map { ym ->
            val (s, e) = Periods.monthWindow(ym)
            val mr = recs.filter { it.dateTime in s until e }
            CashFlowMonth(
                label = "${ym.monthValue}/${ym.year % 100}",
                incomeMinor = mr.filter { it.type == RecordType.INCOME }.sumOf { it.amountMinor },
                expenseMinor = mr.filter { it.type == RecordType.EXPENSE }.sumOf { it.amountMinor },
            )
        }
        val maxCash = cashflow.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) } ?: 0L

        AnalyticsUiState(income, expense, leaderboard, maxLeader, cashflow, maxCash)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())
}

@Composable
fun AnalyticsContent(state: AnalyticsUiState, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

        CalmCard(Modifier.fillMaxWidth()) {
            SectionHeader(stringResource(R.string.this_month))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCell(stringResource(R.string.records_income), state.incomeMinor, Modifier.weight(1f))
                SummaryCell(stringResource(R.string.records_expenses), -state.expenseMinor, Modifier.weight(1f))
                SummaryCell(stringResource(R.string.analytics_net), state.savingMinor, Modifier.weight(1f))
            }
        }

        Column {
            SectionHeader(stringResource(R.string.analytics_by_category))
            if (state.leaderboard.isEmpty()) {
                Text(
                    stringResource(R.string.stats_no_expenses),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.leaderboard.forEach { item ->
                        val pct = if (state.expenseMinor > 0) item.amountMinor * 100 / state.expenseMinor else 0
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(CategoryIcons.forKey(item.icon), contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(item.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("$pct %", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyAmount(-item.amountMinor, withSign = false, style = MaterialTheme.typography.bodyMedium)
                            }
                            BarMeter(fraction = if (state.maxLeaderMinor > 0) item.amountMinor.toFloat() / state.maxLeaderMinor else 0f)
                        }
                    }
                }
            }
        }

        Column {
            SectionHeader(stringResource(R.string.analytics_cashflow))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.cashflow.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(m.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.size(width = 56.dp, height = 20.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            BarMeter(fraction = if (state.maxCashflowMinor > 0) m.incomeMinor.toFloat() / state.maxCashflowMinor else 0f, height = 8.dp)
                            BarMeter(fraction = if (state.maxCashflowMinor > 0) m.expenseMinor.toFloat() / state.maxCashflowMinor else 0f, height = 8.dp)
                        }
                    }
                }
                Text(
                    stringResource(R.string.analytics_cashflow_legend),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, amountMinor: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            Money.format(amountMinor, withSign = true),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
        )
    }
}
