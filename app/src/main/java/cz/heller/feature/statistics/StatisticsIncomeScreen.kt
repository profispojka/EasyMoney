package cz.heller.feature.statistics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.core.designsystem.component.CalmTopBar
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.core.time.Periods
import cz.heller.data.db.RecordType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.RecordRepository
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

data class IncomeUiState(
    val monthName: String = "",
    val totalMinor: Long = 0,
    val records: List<RecordRowUi> = emptyList(),
)

@HiltViewModel
class StatisticsIncomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    records: RecordRepository,
    categories: CategoryRepository,
    accounts: AccountRepository,
) : ViewModel() {

    private val ym: YearMonth =
        runCatching { YearMonth.parse(savedStateHandle.get<String>("ym")) }.getOrDefault(YearMonth.now())

    val state: StateFlow<IncomeUiState> = combine(
        records.observeAll(), categories.observeAll(), accounts.observeActive(),
    ) { recs, cats, accs ->
        val byId = cats.associateBy { it.id }
        val accMap = accs.associateBy { it.id }
        val (start, end) = Periods.monthWindow(ym)
        val monthIncome = recs
            .filter { it.type == RecordType.INCOME && it.dateTime in start until end }
            .sortedByDescending { it.dateTime }
        IncomeUiState(
            monthName = Periods.monthName(ym),
            totalMinor = monthIncome.sumOf { it.amountMinor },
            records = monthIncome.map { it.toRowUi(byId, accMap, context) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncomeUiState())
}

@Composable
fun StatisticsIncomeScreen(
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit,
    vm: StatisticsIncomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        CalmTopBar(stringResource(R.string.records_income), onBack = onBack)

        if (state.records.isEmpty()) {
            Text(
                stringResource(R.string.stats_no_income),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "total") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        state.monthName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.records_income), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyAmount(state.totalMinor, withSign = false, style = MaterialTheme.typography.headlineMedium)
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }
            items(state.records, key = { it.id }) { r ->
                RecordRowItem(r, onClick = { onOpenRecord(r.id) })
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
