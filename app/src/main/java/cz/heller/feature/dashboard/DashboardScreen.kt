package cz.heller.feature.dashboard

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cz.heller.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.core.designsystem.AccountTypeUi
import cz.heller.core.designsystem.component.CalmCard
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.designsystem.component.CalmTopBar
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.core.designsystem.component.SectionHeader
import cz.heller.core.designsystem.component.TrendChart
import cz.heller.core.time.PlannedPayments
import cz.heller.data.db.RecordType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.repo.CategorizationRepository
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.PlannedPaymentRepository
import cz.heller.data.repo.RecordRepository
import cz.heller.data.settings.SettingsRepository
import cz.heller.feature.accounts.AccountRow
import java.time.LocalDate
import java.time.YearMonth
import cz.heller.core.time.Periods
import cz.heller.feature.records.RecordRowItem
import cz.heller.feature.records.RecordRowUi
import cz.heller.feature.records.toRowUi
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    // Balance (net) trend across all accounts
    val balanceTrendPoints: List<Long> = emptyList(),
    val balanceTrendLabels: List<String> = emptyList(),
    val balanceTrendTotalMinor: Long = 0,
    val balanceTrendChangePct: Int? = null,
    // Vývoj příjmů — jen když je napojený podnikatelský účet.
    val showIncomeTrend: Boolean = false,
    val incomeTrendPoints: List<Long> = emptyList(),
    val incomeTrendLabels: List<String> = emptyList(),
    val incomeTrendTotalMinor: Long = 0,
    val incomeTrendChangePct: Int? = null,
    // Top expenses this month: pairs (categoryName, amountMinor)
    val topExpenses: List<Pair<String, Long>> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
            recent = recent.map { it.toRowUi(catMap, accMap, context) },
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

    // First combine: base state + planned + trend + business flag
    val intermediate: StateFlow<DashboardUiState> = combine(
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

    // Enrich intermediate state with balance trend and top expenses using full records and categories
    val state: StateFlow<DashboardUiState> = combine(
        intermediate, records.observeAll(), categories.observeAll(),
    ) { dash, recordsAll, cats ->
        val today = LocalDate.now()

        // Compute balance trend from dash.trendPoints (expense) and dash.incomeTrendPoints
        val expPoints = dash.trendPoints
        val incPoints = dash.incomeTrendPoints
        val bucketsCount = maxOf(expPoints.size, incPoints.size)
        val netPerBucket = (0 until bucketsCount).map { i -> (incPoints.getOrNull(i) ?: 0L) - (expPoints.getOrNull(i) ?: 0L) }
        val suffixSums = LongArray(bucketsCount)
        var acc = 0L
        for (i in bucketsCount - 1 downTo 0) {
            suffixSums[i] = acc
            acc += netPerBucket[i]
        }
        val balancePoints = (0 until bucketsCount).map { i -> dash.netWorthMinor - suffixSums[i] }
        val balanceLabels = dash.trendLabels
        val balanceTotal = dash.netWorthMinor

        // Top 3 expense categories for current month
        val (mStart, mEnd) = Periods.monthWindow(YearMonth.from(today))
        val catMap = cats.associateBy { it.id }
        val top = recordsAll.asSequence()
            .filter { it.type == RecordType.EXPENSE && it.dateTime in mStart until mEnd }
            .groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amountMinor } }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { kv -> (catMap[kv.key]?.name ?: context.getString(R.string.no_category)) to kv.value }

        dash.copy(
            balanceTrendPoints = balancePoints,
            balanceTrendLabels = balanceLabels,
            balanceTrendTotalMinor = balanceTotal,
            balanceTrendChangePct = null,
            topExpenses = top,
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
        CalmTopBar(stringResource(R.string.nav_dashboard))
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Account tiles (2 columns) showing account balances
            SectionHeader(stringResource(R.string.accounts_title))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.accounts.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { row ->
                            CalmCard(Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(AccountTypeUi.icon(row.account.type), contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text(
                                            row.account.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                                        )
                                    }
                                    MoneyAmount(row.balanceMinor, withSign = false, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Balance trend (combined across all accounts)
            if (state.balanceTrendPoints.any { it > 0 }) {
                TrendCard(
                    title = stringResource(R.string.dashboard_balance_trend),
                    periodLabel = stringResource(state.period.labelRes),
                    displayAmountMinor = state.balanceTrendTotalMinor,
                    displayWithSign = true,
                    changePct = state.balanceTrendChangePct,
                    points = state.balanceTrendPoints,
                    labels = state.balanceTrendLabels,
                    period = state.period,
                    onPeriod = vm::setPeriod,
                )
            } else {
                // Fallback: show expense trend
                if (state.trendPoints.any { it > 0 }) {
                    TrendCard(
                        title = stringResource(R.string.dashboard_expense_trend),
                        periodLabel = stringResource(state.period.labelRes),
                        displayAmountMinor = -state.trendTotalMinor,
                        displayWithSign = false,
                        changePct = state.trendChangePct,
                        points = state.trendPoints,
                        labels = state.trendLabels,
                        period = state.period,
                        onPeriod = vm::setPeriod,
                    )
                }
            }

            // Top 3 expenses this month
            Column {
                SectionHeader(stringResource(R.string.dashboard_top_expenses))
                CalmCard(Modifier.fillMaxWidth()) {
                    if (state.topExpenses.isEmpty()) {
                        Text(stringResource(R.string.stats_no_expenses), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.topExpenses.forEachIndexed { index, (name, amount) ->
                            if (index > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                MoneyAmount(amount, withSign = false, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            if (state.upcoming.isNotEmpty()) {
                Column {
                    SectionHeader(stringResource(R.string.dashboard_upcoming))
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
                SectionHeader(stringResource(R.string.dashboard_recent))
                if (state.recent.isEmpty()) {
                    Text(
                        stringResource(R.string.dashboard_no_records),
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
            stringResource(R.string.dashboard_overdue),
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
                        label = stringResource(p.labelRes),
                        selected = period == p,
                        onClick = { onPeriod(p) },
                    )
                }
            }
        }
    }
}
