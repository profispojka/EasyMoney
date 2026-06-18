package cz.calmmoney.feature.dashboard

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cz.calmmoney.core.designsystem.component.CalmChip
import cz.calmmoney.core.designsystem.component.CalmTopBar
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.core.designsystem.component.SectionHeader
import cz.calmmoney.core.designsystem.component.TrendChart
import cz.calmmoney.core.time.PlannedPayments
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategorizationRepository
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.PlannedPaymentRepository
import cz.calmmoney.data.repo.RecordRepository
import cz.calmmoney.data.settings.SettingsRepository
import cz.calmmoney.feature.accounts.AccountRow
import java.time.LocalDate
import cz.calmmoney.feature.records.RecordRowItem
import cz.calmmoney.feature.records.RecordRowUi
import cz.calmmoney.feature.records.toRowUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpcomingPayment(
    val plannedId: String,
    val name: String,
    val dateText: String,
    val signedAmountMinor: Long,
    val overdue: Boolean,
)

/** Změna výdajů oproti minulému období: kladná = víc se utratilo. */
private fun formatChangePct(pct: Int): String = when {
    pct > 0 -> "+$pct %"
    pct < 0 -> "− ${-pct} %"
    else -> "0 %"
}

data class DashboardUiState(
    val netWorthMinor: Long = 0,
    val accounts: List<AccountRow> = emptyList(),
    val recent: List<RecordRowUi> = emptyList(),
    val upcoming: List<UpcomingPayment> = emptyList(),
    val period: TrendPeriod = TrendPeriod.MONTHS_6,
    val trendPoints: List<Long> = emptyList(),
    val trendLabels: List<String> = emptyList(),
    val trendTotalMinor: Long = 0,
    val trendChangePct: Int? = null,
    // Vývoj příjmů — jen když je napojený podnikatelský účet.
    val showIncomeTrend: Boolean = false,
    val incomeTrendPoints: List<Long> = emptyList(),
    val incomeTrendLabels: List<String> = emptyList(),
    val incomeTrendTotalMinor: Long = 0,
    val incomeTrendChangePct: Int? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    accounts: AccountRepository,
    records: RecordRepository,
    categories: CategoryRepository,
    planned: PlannedPaymentRepository,
    settings: SettingsRepository,
    private val categorization: CategorizationRepository,
) : ViewModel() {

    private val periodFlow = MutableStateFlow(TrendPeriod.MONTHS_6)

    init {
        viewModelScope.launch {
            // Dožeň nezařazené záznamy (po vylepšení pravidel) a napáruj plánované platby.
            categorization.recategorizeUncategorized()
            planned.reconcileAll()
        }
    }

    fun setPeriod(period: TrendPeriod) {
        periodFlow.value = period
    }

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

    private val trend = combine(records.observeAll(), periodFlow) { allRecs, period ->
        Triple(
            period,
            ExpenseTrend.compute(period, allRecs, type = RecordType.EXPENSE),
            ExpenseTrend.compute(period, allRecs, type = RecordType.INCOME),
        )
    }

    /** Je napojený (Fio) účet typu podnikatelský? → ukaž graf vývoje příjmů. */
    private val hasBusinessAccount = combine(accounts.observeActive(), settings.fioConnections) { accs, conns ->
        val connectedIds = conns.map { it.accountId }.toSet()
        accs.any { it.isBusiness && it.id in connectedIds }
    }

    val state: StateFlow<DashboardUiState> = combine(
        base, planned.observeAll(), trend, hasBusinessAccount,
    ) { dash, payments, td, hasBiz ->
        val today = LocalDate.now()
        val upcoming = payments
            .mapNotNull { p ->
                PlannedPayments.dueOccurrence(p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay, p.paidThroughEpochDay)
                    ?.let { date -> p to date }
            }
            .sortedBy { it.second }
            .take(5)
            .map { (p, date) ->
                UpcomingPayment(
                    plannedId = p.id,
                    name = p.name,
                    dateText = PlannedPayments.formatDate(date),
                    signedAmountMinor = if (p.type == RecordType.EXPENSE) -p.amountMinor else p.amountMinor,
                    overdue = date.isBefore(today),
                )
            }
        val (period, expense, income) = td
        dash.copy(
            upcoming = upcoming,
            period = period,
            trendPoints = expense.points,
            trendLabels = expense.axisLabels,
            trendTotalMinor = expense.totalMinor,
            trendChangePct = expense.changePct,
            showIncomeTrend = hasBiz,
            incomeTrendPoints = income.points,
            incomeTrendLabels = income.axisLabels,
            incomeTrendTotalMinor = income.totalMinor,
            incomeTrendChangePct = income.changePct,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

@Composable
fun DashboardScreen(
    onOpenRecord: (String) -> Unit,
    onOpenPlanned: () -> Unit,
    onMatchPayment: (String) -> Unit,
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

            if (state.trendPoints.any { it > 0 }) {
                TrendCard(
                    title = "Vývoj výdajů",
                    periodLabel = state.period.label,
                    displayAmountMinor = -state.trendTotalMinor,
                    displayWithSign = false,
                    changePct = state.trendChangePct,
                    points = state.trendPoints,
                    labels = state.trendLabels,
                    period = state.period,
                    onPeriod = vm::setPeriod,
                )
            }

            // Vývoj příjmů — jen u napojeného podnikatelského účtu.
            if (state.showIncomeTrend && state.incomeTrendPoints.any { it > 0 }) {
                TrendCard(
                    title = "Vývoj příjmů",
                    periodLabel = state.period.label,
                    displayAmountMinor = state.incomeTrendTotalMinor,
                    displayWithSign = true,
                    changePct = state.incomeTrendChangePct,
                    points = state.incomeTrendPoints,
                    labels = state.incomeTrendLabels,
                    period = state.period,
                    onPeriod = vm::setPeriod,
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
                                    .clickable { onMatchPayment(p.plannedId) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                    if (p.overdue) {
                                        OverdueBadge(p.dateText)
                                    } else {
                                        Text(p.dateText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
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

/** Monochromatický štítek „Po splatnosti" + datum splatnosti (E-Ink, bez barvy). */
@Composable
private fun OverdueBadge(dateText: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 2.dp),
    ) {
        Text(
            "Po splatnosti",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
        Text(dateText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Karta vývoje (výdajů/příjmů): součet za období, % změna, spojnicový graf a přepínač období. */
@Composable
private fun TrendCard(
    title: String,
    periodLabel: String,
    displayAmountMinor: Long,
    displayWithSign: Boolean,
    changePct: Int?,
    points: List<Long>,
    labels: List<String>,
    period: TrendPeriod,
    onPeriod: (TrendPeriod) -> Unit,
) {
    Column {
        SectionHeader(title)
        CalmCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        periodLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MoneyAmount(
                        amountMinor = displayAmountMinor,
                        withSign = displayWithSign,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                changePct?.let { pct ->
                    Text(
                        formatChangePct(pct),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TrendChart(
                points = points,
                labels = labels,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendPeriod.entries.forEach { p ->
                    CalmChip(
                        label = p.label,
                        selected = period == p,
                        onClick = { onPeriod(p) },
                    )
                }
            }
        }
    }
}
