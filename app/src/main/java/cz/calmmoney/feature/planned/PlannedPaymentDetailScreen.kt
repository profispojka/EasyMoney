package cz.calmmoney.feature.planned

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.core.time.PlannedPayments
import cz.calmmoney.data.db.PlannedPaymentEntity
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.PlannedPaymentRepository
import cz.calmmoney.data.repo.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PlannedDetailUiState(
    val payment: PlannedPaymentEntity? = null,
    val typeLabel: String = "",
    val signedAmountMinor: Long = 0,
    val accountName: String? = null,
    val categoryName: String? = null,
    val frequencyLabel: String = "",
    val nextDateText: String = "",
    val endText: String = "",
)

@HiltViewModel
class PlannedPaymentDetailViewModel @Inject constructor(
    private val planned: PlannedPaymentRepository,
    private val records: RecordRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plannedId: String = checkNotNull(savedStateHandle["plannedId"])

    val state: StateFlow<PlannedDetailUiState> = combine(
        planned.observeById(plannedId), accounts.observeActive(), categories.observeAll(),
    ) { p, accs, cats ->
        if (p == null) return@combine PlannedDetailUiState()
        val next = PlannedPayments.nextOccurrence(p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay)
        PlannedDetailUiState(
            payment = p,
            typeLabel = if (p.type == RecordType.INCOME) "Příjem" else "Výdaj",
            signedAmountMinor = if (p.type == RecordType.EXPENSE) -p.amountMinor else p.amountMinor,
            accountName = accs.firstOrNull { it.id == p.accountId }?.name,
            categoryName = p.categoryId?.let { id -> cats.firstOrNull { it.id == id }?.name },
            frequencyLabel = PlannedPayments.frequencyLabel(p.frequencyUnit, p.frequencyCount),
            nextDateText = next?.let { PlannedPayments.formatDate(it) } ?: "Ukončeno",
            endText = p.endEpochDay?.let { PlannedPayments.formatDate(LocalDate.ofEpochDay(it)) } ?: "Bez konce",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannedDetailUiState())

    /** Zaplatit teď = vytvoří reálný záznam (dnešní datum). */
    fun payNow(onDone: () -> Unit) {
        val p = state.value.payment ?: return
        viewModelScope.launch {
            records.addEntry(
                type = p.type,
                accountId = p.accountId,
                categoryId = p.categoryId,
                amountMinor = p.amountMinor,
                payee = p.name,
                note = p.note,
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val p = state.value.payment ?: return
        viewModelScope.launch {
            planned.delete(p)
            onDone()
        }
    }
}

@Composable
fun PlannedPaymentDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    vm: PlannedPaymentDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val payment = state.payment

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět") }
            Text("Plánovaná platba", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (payment == null) {
            Text("Platba nenalezena.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            return
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(payment.name, style = MaterialTheme.typography.titleMedium)
            MoneyAmount(state.signedAmountMinor, withSign = true, style = MaterialTheme.typography.displayLarge, modifier = Modifier.fillMaxWidth())

            DetailRow("Typ", state.typeLabel)
            DetailRow("Účet", state.accountName ?: "—")
            DetailRow("Kategorie", state.categoryName ?: "Bez kategorie")
            DetailRow("Frekvence", state.frequencyLabel)
            DetailRow("Příště", state.nextDateText)
            DetailRow("Konec", state.endText)
            payment.note?.let { DetailRow("Poznámka", it) }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            CalmPrimaryButton("Zaplatit teď", onClick = { vm.payNow(onBack) })
            OutlinedButton(onClick = { onEdit(payment.id) }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Upravit", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = { confirmDelete = true }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Smazat", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (confirmDelete && payment != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Smazat plánovanou platbu?") },
            text = { Text("Platba „${payment.name}“ bude odstraněna.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Smazat") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Zrušit") } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
