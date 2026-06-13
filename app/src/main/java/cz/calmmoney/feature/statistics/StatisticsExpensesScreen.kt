package cz.calmmoney.feature.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.CategoryIcons
import cz.calmmoney.core.designsystem.component.BarMeter
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.core.designsystem.theme.Gray300
import cz.calmmoney.core.designsystem.theme.Gray500
import cz.calmmoney.core.designsystem.theme.Gray700
import cz.calmmoney.core.designsystem.theme.Ink
import cz.calmmoney.core.money.Money
import cz.calmmoney.core.time.Periods
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.RecordRepository
import cz.calmmoney.feature.budgets.groupIdOf
import cz.calmmoney.feature.records.RecordRowItem
import cz.calmmoney.feature.records.RecordRowUi
import cz.calmmoney.feature.records.toRowUi
import dagger.hilt.android.lifecycle.HiltViewModel
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
                    name = g?.name ?: "Bez kategorie",
                    icon = g?.icon ?: "more_horiz",
                    amountMinor = sum,
                    pct = if (total > 0) (sum * 100 / total).toInt() else 0,
                    transactions = list.sortedByDescending { it.dateTime }.map { it.toRowUi(byId, accMap) },
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
        cz.calmmoney.core.designsystem.component.CalmTopBar("Výdaje", onBack = onBack)

        if (state.groups.isEmpty()) {
            Text(
                "Tento měsíc žádné výdaje.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        val selected = state.groups.firstOrNull { it.groupId == selectedId }
        val maxMinor = state.groups.firstOrNull()?.amountMinor ?: 0L

        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "month") {
                Text(
                    state.monthName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item(key = "donut") {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    ExpenseDonut(
                        groups = state.groups,
                        totalMinor = state.totalMinor,
                        selectedId = selectedId,
                        centerLabel = selected?.name ?: "Výdaje",
                        centerAmountMinor = selected?.amountMinor ?: state.totalMinor,
                    )
                }
            }
            item(key = "hint") {
                Text(
                    "Klepni na kategorii pro její transakce.",
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

/** Monochromatický prstencový graf. Slice = odstín šedi, vybraná kategorie = plná černá. */
@Composable
private fun ExpenseDonut(
    groups: List<ExpenseGroupItem>,
    totalMinor: Long,
    selectedId: String?,
    centerLabel: String,
    centerAmountMinor: Long,
) {
    val palette = listOf(Ink, Gray700, Gray500, Gray300)
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            if (totalMinor <= 0) return@Canvas
            val stroke = size.minDimension * 0.18f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            val gap = 2.5f
            var startAngle = -90f
            groups.forEachIndexed { i, g ->
                val sweep = g.amountMinor.toFloat() / totalMinor * 360f
                val color: Color = when {
                    selectedId == null -> palette[i % palette.size]
                    g.groupId == selectedId -> Ink
                    else -> Gray300
                }
                drawArc(
                    color = color,
                    startAngle = startAngle + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                Money.format(centerAmountMinor),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
