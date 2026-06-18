package cz.heller.feature.planned

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import cz.heller.R
import cz.heller.core.designsystem.component.CalmConfirmSheet
import cz.heller.core.designsystem.component.CalmPrimaryButton
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.core.time.PlannedPayments
import cz.heller.data.db.PlannedPaymentEntity
import cz.heller.data.db.RecordType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.PlannedPaymentRepository
import cz.heller.data.repo.RecordRepository
import cz.heller.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    /** Účet platby se synchronizuje z Fia → „Zaplatit teď" (ruční záznam) skryj. */
    val accountIsFioSynced: Boolean = false,
)

@HiltViewModel
class PlannedPaymentDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val planned: PlannedPaymentRepository,
    private val records: RecordRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    settings: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plannedId: String = checkNotNull(savedStateHandle["plannedId"])

    val state: StateFlow<PlannedDetailUiState> = combine(
        planned.observeById(plannedId), accounts.observeActive(), categories.observeAll(), settings.fioConnections,
    ) { p, accs, cats, conns ->
        if (p == null) return@combine PlannedDetailUiState()
        val next = PlannedPayments.nextOccurrence(p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay)
        PlannedDetailUiState(
            payment = p,
            typeLabel = context.getString(if (p.type == RecordType.INCOME) R.string.type_income else R.string.type_expense),
            signedAmountMinor = if (p.type == RecordType.EXPENSE) -p.amountMinor else p.amountMinor,
            accountName = accs.firstOrNull { it.id == p.accountId }?.name,
            categoryName = p.categoryId?.let { id -> cats.firstOrNull { it.id == id }?.name },
            frequencyLabel = PlannedPayments.frequencyLabel(context, p.frequencyUnit, p.frequencyCount),
            nextDateText = next?.let { PlannedPayments.formatDate(it) } ?: context.getString(R.string.planned_ended),
            endText = p.endEpochDay?.let { PlannedPayments.formatDate(LocalDate.ofEpochDay(it)) } ?: context.getString(R.string.no_end),
            accountIsFioSynced = conns.any { it.accountId == p.accountId },
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.planned_detail_title), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (payment == null) {
            Text(stringResource(R.string.planned_not_found), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            return
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(payment.name, style = MaterialTheme.typography.titleMedium)
            MoneyAmount(state.signedAmountMinor, withSign = true, style = MaterialTheme.typography.displayLarge, modifier = Modifier.fillMaxWidth())

            DetailRow(stringResource(R.string.detail_type), state.typeLabel)
            DetailRow(stringResource(R.string.detail_account), state.accountName ?: "—")
            DetailRow(stringResource(R.string.detail_category), state.categoryName ?: stringResource(R.string.no_category))
            DetailRow(stringResource(R.string.detail_frequency), state.frequencyLabel)
            DetailRow(stringResource(R.string.detail_next), state.nextDateText)
            DetailRow(stringResource(R.string.detail_end), state.endText)
            payment.note?.let { DetailRow(stringResource(R.string.detail_note), it) }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // U Fio účtu se platba zaúčtuje sama při synchronizaci → „Zaplatit teď" (ruční záznam) nenabízej.
            if (!state.accountIsFioSynced) {
                CalmPrimaryButton(stringResource(R.string.planned_pay_now), onClick = { vm.payNow(onBack) })
            }
            OutlinedButton(onClick = { onEdit(payment.id) }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.action_edit), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = { confirmDelete = true }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.action_delete), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (confirmDelete && payment != null) {
        CalmConfirmSheet(
            title = stringResource(R.string.planned_delete_title),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; vm.delete(onBack) },
            onDismiss = { confirmDelete = false },
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
