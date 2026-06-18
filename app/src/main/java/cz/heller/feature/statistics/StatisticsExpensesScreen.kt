package cz.heller.feature.statistics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.core.designsystem.CategoryIcons
import cz.heller.core.designsystem.component.BarMeter
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.core.time.Periods
import cz.heller.data.db.RecordType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.RecordRepository
import cz.heller.feature.budgets.groupIdOf
import cz.heller.feature.records.RecordRowItem
import cz.heller.feature.records.RecordRowUi
import cz.heller.feature.records.toRowUi
import android.content.Context
import cz.heller.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class ExpenseGroupItem(
    val groupId: String,
    val name: String,
    val icon: String,
    val amountMinor: Long,
    val pct: Int,
    val transactions: List<RecordRowUi>,
)

data class ExpensesUiState(
    val monthName: String = "",
    val totalMinor: Long = 0,
    val groups: List<ExpenseGroupItem> = emptyList(),
)

@HiltViewModel
class StatisticsExpensesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    records: RecordRepository,
    categories: CategoryRepository,
    accounts: AccountRepository,
) : ViewModel() {

    private val ym: YearMonth =
        runCatching { YearMonth.parse(savedStateHandle.get<String>("ym")) }.getOrDefault(YearMonth.now())

    val state: StateFlow<ExpensesUiState> = combine(
        records.observeAll(), categories.observeAll(), accounts.observeActive(),
    ) { recs, cats, accs ->
        val byId = cats.associateBy { it.id }
        val groups = cats.filter { it.parentId == null }.associateBy { it.id }
        val accMap = accs.associateBy { it.id }
        val (start, end) = Periods.monthWindow(ym)

        val monthExpenses = recs.filter { it.type == RecordType.EXPENSE && it.dateTime in start until end }
        val total = monthExpenses.sumOf { it.amountMinor }

        val items = monthExpenses
            .groupBy { groupIdOf(it.categoryId, byId) ?: "__none" }
            .map { (gid, list) ->
                val g = groups[gid]
                val sum = list.sumOf { it.amountMinor }
                ExpenseGroupItem(
                    groupId = gid,
                    name = g?.name ?: context.getString(R.string.no_category),
                    icon = g?.icon ?: "more_horiz",
                    amountMinor = sum,
                    pct = if (total > 0) (sum * 100 / total).toInt() else 0,
                    transactions = list.sortedByDescending { it.dateTime }.map { it.toRowUi(byId, accMap, context) },
                )
            }
            .sortedByDescending { it.amountMinor }

        ExpensesUiState(Periods.monthName(ym), total, items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())
}

@Composable
fun StatisticsExpensesScreen(
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit,
    vm: StatisticsExpensesViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        cz.heller.core.designsystem.component.CalmTopBar(stringResource(R.string.records_expenses), onBack = onBack)

        if (state.groups.isEmpty()) {
            Text(
                stringResource(R.string.stats_no_expenses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        val maxMinor = state.groups.firstOrNull()?.amountMinor ?: 0L

        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "total") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        state.monthName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.records_expenses), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyAmount(state.totalMinor, withSign = false, style = MaterialTheme.typography.headlineMedium)
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }
            item(key = "hint") {
                Text(
                    stringResource(R.string.stats_category_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            state.groups.forEach { g ->
                item(key = "row_${g.groupId}") {
                    GroupRow(
                        item = g,
                        maxMinor = maxMinor,
                        selected = g.groupId == selectedId,
                        onClick = { selectedId = if (selectedId == g.groupId) null else g.groupId },
                    )
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (g.groupId == selectedId) {
                    items(g.transactions, key = { "tx_${it.id}" }) { tx ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                            RecordRowItem(tx, onClick = { onOpenRecord(tx.id) })
                        }
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    item: ExpenseGroupItem,
    maxMinor: Long,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(CategoryIcons.forKey(item.icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${item.pct} %",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyAmount(-item.amountMinor, withSign = false, style = MaterialTheme.typography.bodyLarge)
        }
        BarMeter(fraction = if (maxMinor > 0) item.amountMinor.toFloat() / maxMinor else 0f, height = 8.dp)
    }
}
