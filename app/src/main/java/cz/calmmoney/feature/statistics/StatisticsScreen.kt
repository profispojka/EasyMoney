package cz.calmmoney.feature.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmCard
import cz.calmmoney.core.designsystem.component.CalmTopBar
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.core.time.PlannedPayments
import cz.calmmoney.core.time.Periods
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.PlannedPaymentRepository
import cz.calmmoney.data.repo.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

data class StatisticsUiState(
    val ym: YearMonth = YearMonth.now(),
    val monthLabel: String = "",
    val netWorthMinor: Long = 0,
    val expenseMinor: Long = 0,
    val incomeMinor: Long = 0,
    val forecastMinor: Long = 0,
) {
    val cashflowMinor: Long get() = incomeMinor - expenseMinor
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    accounts: AccountRepository,
    records: RecordRepository,
    planned: PlannedPaymentRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<StatisticsUiState> = combine(
        accounts.observeNetWorthMinor(),
        records.observeAll(),
        planned.observeAll(),
        month,
    ) { netWorth, recs, plans, ym ->
        val (start, end) = Periods.monthWindow(ym)
        val monthRecs = recs.filter { it.dateTime in start until end }
        val expense = monthRecs.filter { it.type == RecordType.EXPENSE }.sumOf { it.amountMinor }
        val income = monthRecs.filter { it.type == RecordType.INCOME }.sumOf { it.amountMinor }

        val nextYm = ym.plusMonths(1)
        val forecast = plans
            .filter { it.type == RecordType.EXPENSE }
            .sumOf { p ->
                PlannedPayments.occurrencesInMonth(
                    p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay, nextYm,
                ) * p.amountMinor
            }

        StatisticsUiState(
            ym = ym,
            monthLabel = Periods.monthLabel(ym),
            netWorthMinor = netWorth,
            expenseMinor = expense,
            incomeMinor = income,
            forecastMinor = forecast,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun setMonth(ym: YearMonth) = month.update { ym }
    fun prevMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }
}

@Composable
fun StatisticsScreen(
    onOpenExpenses: (String) -> Unit,
    vm: StatisticsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        CalmTopBar("Statistiky")

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard("Zůstatek", state.netWorthMinor, withSign = false)
            StatCard("Výdaje", -state.expenseMinor, onClick = { onOpenExpenses(state.ym.toString()) })
            StatCard("Příjmy", state.incomeMinor)
            StatCard("Cash flow", state.cashflowMinor)
            StatCard("Výhled", -state.forecastMinor, subtitle = "výdaje příští měsíc")
        }

        MonthSwitcher(
            label = state.monthLabel,
            current = state.ym,
            onPrev = vm::prevMonth,
            onNext = vm::nextMonth,
            onPick = vm::setMonth,
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    amountMinor: Long,
    withSign: Boolean = true,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = Modifier.fillMaxWidth().then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
    CalmCard(cardModifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyAmount(amountMinor, withSign = withSign, style = MaterialTheme.typography.headlineSmall)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun MonthSwitcher(
    label: String,
    current: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPick: (YearMonth) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Předchozí měsíc")
        }
        Box(Modifier.weight(1f)) {
            Surface(
                onClick = { menuOpen = true },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                val options = (1 downTo -11).map { current.plusMonths(it.toLong()) }
                options.forEach { ym ->
                    DropdownMenuItem(
                        text = { Text(Periods.monthLabel(ym)) },
                        onClick = { onPick(ym); menuOpen = false },
                    )
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Další měsíc")
        }
    }
}
