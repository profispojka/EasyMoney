package cz.calmmoney.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.AccountTypeUi
import cz.calmmoney.core.designsystem.component.CalmCard
import cz.calmmoney.core.designsystem.component.CalmTopBar
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.core.designsystem.component.SectionHeader
import cz.calmmoney.core.time.PlannedPayments
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.PlannedPaymentRepository
import cz.calmmoney.data.repo.RecordRepository
import cz.calmmoney.feature.accounts.AccountRow
import java.time.LocalDate
import cz.calmmoney.feature.records.RecordRowItem
import cz.calmmoney.feature.records.RecordRowUi
import cz.calmmoney.feature.records.toRowUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class UpcomingPayment(val name: String, val dateText: String, val signedAmountMinor: Long)

data class DashboardUiState(
    val netWorthMinor: Long = 0,
    val accounts: List<AccountRow> = emptyList(),
    val recent: List<RecordRowUi> = emptyList(),
    val upcoming: List<UpcomingPayment> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    accounts: AccountRepository,
    records: RecordRepository,
    categories: CategoryRepository,
    planned: PlannedPaymentRepository,
) : ViewModel() {

    private val base = combine(
        accounts.observeNetWorthMinor(),
        accounts.observeActive(),
        accounts.observeBalances(),
        records.observeRecent(6),
        categories.observeAll(),
    ) { net, accs, balances, recent, cats ->
        val balMap = balances.associate { it.accountId to it.balanceMinor }
        val catMap = cats.associateBy { it.id }
        val accMap = accs.associateBy { it.id }
        DashboardUiState(
            netWorthMinor = net,
            accounts = accs.map { AccountRow(it, balMap[it.id] ?: it.initialBalanceMinor) },
            recent = recent.map { it.toRowUi(catMap, accMap) },
        )
    }

    val state: StateFlow<DashboardUiState> = combine(base, planned.observeAll()) { dash, payments ->
        val today = LocalDate.now()
        val upcoming = payments
            .mapNotNull { p ->
                PlannedPayments.nextOccurrence(p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay, today)
                    ?.let { date -> p to date }
            }
            .sortedBy { it.second }
            .take(3)
            .map { (p, date) ->
                UpcomingPayment(
                    name = p.name,
                    dateText = PlannedPayments.formatDate(date),
                    signedAmountMinor = if (p.type == RecordType.EXPENSE) -p.amountMinor else p.amountMinor,
                )
            }
        dash.copy(upcoming = upcoming)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

@Composable
fun DashboardScreen(
    onOpenRecord: (String) -> Unit,
    onOpenPlanned: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        CalmTopBar("Přehled")
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CalmCard(Modifier.fillMaxWidth()) {
                SectionHeader("Čisté jmění")
                MoneyAmount(
                    amountMinor = state.netWorthMinor,
                    withSign = false,
                    style = MaterialTheme.typography.displayLarge,
                )
            }

            Column {
                SectionHeader("Účty")
                CalmCard(Modifier.fillMaxWidth()) {
                    state.accounts.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(AccountTypeUi.icon(row.account.type), contentDescription = null, modifier = Modifier.size(24.dp))
                            Text(row.account.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            MoneyAmount(row.balanceMinor, withSign = false, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (state.upcoming.isNotEmpty()) {
                Column {
                    SectionHeader("Nadcházející platby")
                    CalmCard(Modifier.fillMaxWidth()) {
                        state.upcoming.forEachIndexed { index, p ->
                            if (index > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenPlanned)
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(p.dateText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                MoneyAmount(p.signedAmountMinor, withSign = true, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            Column {
                SectionHeader("Poslední záznamy")
                if (state.recent.isEmpty()) {
                    Text(
                        "Zatím žádné záznamy. Přidej první tlačítkem +.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    CalmCard(Modifier.fillMaxWidth()) {
                        state.recent.forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            RecordRowItem(row, onClick = { onOpenRecord(row.id) })
                        }
                    }
                }
            }
        }
    }
}
