package cz.calmmoney.feature.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmChip
import cz.calmmoney.core.designsystem.component.EmptyState
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.data.db.AccountEntity
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.RecordRepository
import cz.calmmoney.feature.budgets.CategoryGroup
import cz.calmmoney.feature.budgets.expenseGroups
import cz.calmmoney.feature.budgets.groupIdOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class DayGroup(val label: String, val items: List<RecordRowUi>, val dayTotalMinor: Long)

data class RecordFilter(
    val type: RecordType? = null,
    val accountId: String? = null,
    val groupId: String? = null,
) {
    val isActive: Boolean get() = type != null || accountId != null || groupId != null
}

data class RecordsUiState(
    val groups: List<DayGroup> = emptyList(),
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val filter: RecordFilter = RecordFilter(),
    val accounts: List<AccountEntity> = emptyList(),
    val categoryGroups: List<CategoryGroup> = emptyList(),
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    records: RecordRepository,
    categories: CategoryRepository,
    accounts: AccountRepository,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d. M. yyyy", Locale.forLanguageTag("cs-CZ"))
    private val filter = MutableStateFlow(RecordFilter())

    val state: StateFlow<RecordsUiState> = combine(
        records.observeAll(),
        categories.observeAll(),
        accounts.observeActive(),
        filter,
    ) { recs, cats, accs, f ->
        val byId = cats.associateBy { it.id }
        val filtered = recs.filter { r ->
            (f.type == null || r.type == f.type) &&
                (f.accountId == null || r.accountId == f.accountId) &&
                (f.groupId == null || groupIdOf(r.categoryId, byId) == f.groupId)
        }
        val accMap = accs.associateBy { it.id }
        val income = filtered.filter { it.type == RecordType.INCOME }.sumOf { it.amountMinor }
        val expense = filtered.filter { it.type == RecordType.EXPENSE }.sumOf { it.amountMinor }

        val groups = filtered
            .groupBy { Instant.ofEpochMilli(it.dateTime).atZone(zone).toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { (date, list) ->
                val rows = list.sortedByDescending { it.dateTime }.map { it.toRowUi(byId, accMap) }
                DayGroup(labelFor(date), rows, rows.sumOf { it.amountMinor })
            }
        RecordsUiState(groups, income, expense, f, accs, expenseGroups(cats))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState())

    fun setTypeFilter(type: RecordType?) = filter.update { it.copy(type = type) }
    fun setAccountFilter(id: String?) = filter.update { it.copy(accountId = id) }
    fun setGroupFilter(id: String?) = filter.update { it.copy(groupId = id) }
    fun clearFilter() = filter.update { RecordFilter() }

    private fun labelFor(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Dnes"
            today.minusDays(1) -> "Včera"
            else -> date.format(formatter)
        }
    }
}

@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit,
    vm: RecordsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět") }
            Text("Záznamy", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Příjmy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyAmount(state.incomeMinor, withSign = true)
            }
            Column(Modifier.weight(1f)) {
                Text("Výdaje", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyAmount(-state.expenseMinor, withSign = true)
            }
            CalmChip(
                label = if (state.filter.isActive) "Filtr •" else "Filtr",
                selected = showFilter || state.filter.isActive,
                onClick = { showFilter = !showFilter },
            )
        }

        if (showFilter) {
            FilterPanel(state, vm)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (state.groups.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = if (state.filter.isActive) "Nic neodpovídá filtru" else "Žádné záznamy",
                subtitle = if (state.filter.isActive) "Zkus upravit nebo vyčistit filtr." else "Příjmy a výdaje se objeví tady.",
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                state.groups.forEach { group ->
                    item(key = "h_${group.label}") {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(group.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                MoneyAmount(group.dayTotalMinor, withSign = true, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    items(group.items.size, key = { group.items[it].id }) { i ->
                        val item = group.items[i]
                        RecordRowItem(item, onClick = { onOpenRecord(item.id) })
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(state: RecordsUiState, vm: RecordsViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Typ", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip("Vše", state.filter.type == null, onClick = { vm.setTypeFilter(null) })
            CalmChip("Výdaj", state.filter.type == RecordType.EXPENSE, onClick = { vm.setTypeFilter(RecordType.EXPENSE) })
            CalmChip("Příjem", state.filter.type == RecordType.INCOME, onClick = { vm.setTypeFilter(RecordType.INCOME) })
            CalmChip("Převod", state.filter.type == RecordType.TRANSFER, onClick = { vm.setTypeFilter(RecordType.TRANSFER) })
        }

        Text("Účet", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip("Vše", state.filter.accountId == null, onClick = { vm.setAccountFilter(null) })
            state.accounts.forEach { a ->
                CalmChip(a.name, state.filter.accountId == a.id, onClick = { vm.setAccountFilter(a.id) })
            }
        }

        Text("Kategorie", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip("Vše", state.filter.groupId == null, onClick = { vm.setGroupFilter(null) })
            state.categoryGroups.forEach { g ->
                CalmChip(g.name, state.filter.groupId == g.id, onClick = { vm.setGroupFilter(g.id) })
            }
        }

        if (state.filter.isActive) {
            CalmChip("Vyčistit filtr", selected = false, onClick = { vm.clearFilter() }, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
