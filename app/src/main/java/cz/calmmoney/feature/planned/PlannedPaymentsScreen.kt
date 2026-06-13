package cz.calmmoney.feature.planned

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cz.calmmoney.core.designsystem.component.CalmCard
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.designsystem.component.CalmTopBar
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.core.designsystem.component.SectionHeader
import cz.calmmoney.core.time.PlannedPayments
import cz.calmmoney.data.db.PlannedPaymentEntity
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.PlannedPaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class PlannedRow(
    val payment: PlannedPaymentEntity,
    val frequencyLabel: String,
    val nextDateText: String,
    val signedAmountMinor: Long,
    val occursNextMonth: Boolean,
)

data class PlannedUiState(
    val rows: List<PlannedRow> = emptyList(),
    val nextMonthLabel: String = "",
    val nextMonthExpenseMinor: Long = 0,
    val nextMonthIncomeMinor: Long = 0,
)

@HiltViewModel
class PlannedPaymentsViewModel @Inject constructor(
    private val planned: PlannedPaymentRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
) : ViewModel() {

    private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("cs-CZ"))

    val state: StateFlow<PlannedUiState> = combine(
        planned.observeAll(), accounts.observeActive(), categories.observeAll(),
    ) { pays, _, _ ->
        val nextMonth = YearMonth.now().plusMonths(1)
        var exp = 0L
        var inc = 0L
        val rows = pays.map { p ->
            val occ = PlannedPayments.occurrencesInMonth(
                p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay, nextMonth,
            )
            val amountNextMonth = occ * p.amountMinor
            if (p.type == RecordType.EXPENSE) exp += amountNextMonth else inc += amountNextMonth
            val next = PlannedPayments.nextOccurrence(p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay)
            PlannedRow(
                payment = p,
                frequencyLabel = PlannedPayments.frequencyLabel(p.frequencyUnit, p.frequencyCount),
                nextDateText = next?.let { "Příště " + PlannedPayments.formatDate(it) } ?: "Ukončeno",
                signedAmountMinor = if (p.type == RecordType.EXPENSE) -p.amountMinor else p.amountMinor,
                occursNextMonth = occ > 0,
            )
        }
        PlannedUiState(rows, capitalize(monthFormatter.format(nextMonth)), exp, inc)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannedUiState())

    fun delete(payment: PlannedPaymentEntity) {
        viewModelScope.launch { planned.delete(payment) }
    }

    private fun capitalize(s: String) = s.replaceFirstChar { it.uppercaseChar() }
}

@Composable
fun PlannedPaymentsScreen(
    onAdd: () -> Unit,
    onOpenDetail: (String) -> Unit,
    vm: PlannedPaymentsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<PlannedPaymentEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        CalmTopBar("Plánované platby")

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CalmPrimaryButton("+ Nová platba", onClick = onAdd)

            CalmCard(Modifier.fillMaxWidth()) {
                SectionHeader("Příští měsíc — ${state.nextMonthLabel}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Zaplatíš", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyAmount(-state.nextMonthExpenseMinor, withSign = true)
                    }
                    if (state.nextMonthIncomeMinor > 0) {
                        Column(Modifier.weight(1f)) {
                            Text("Přijmeš", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyAmount(state.nextMonthIncomeMinor, withSign = true)
                        }
                    }
                }
            }
        }

        if (state.rows.isEmpty()) {
            Text(
                "Zatím žádné plánované platby. Přidej první přes „+ Nová platba“.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.rows, key = { it.payment.id }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDetail(row.payment.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(row.payment.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${row.frequencyLabel} · ${row.nextDateText}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MoneyAmount(row.signedAmountMinor, withSign = true)
                        IconButton(onClick = { toDelete = row.payment }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Smazat", modifier = Modifier.size(22.dp))
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    val p = toDelete
    if (p != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Smazat plánovanou platbu?") },
            text = { Text("Platba „${p.name}“ bude odstraněna.") },
            confirmButton = { TextButton(onClick = { vm.delete(p); toDelete = null }) { Text("Smazat") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Zrušit") } },
        )
    }
}
