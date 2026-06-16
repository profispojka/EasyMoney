package cz.calmmoney.feature.records

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.data.db.RecordEntity
import cz.calmmoney.data.db.RecordSource
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class RecordDetailUiState(
    val record: RecordEntity? = null,
    val typeLabel: String = "",
    val signedAmountMinor: Long = 0,
    val categoryName: String? = null,
    val accountName: String? = null,
    val transferAccountName: String? = null,
    val dateText: String = "",
) {
    val isTransfer: Boolean get() = record?.type == RecordType.TRANSFER
}

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    private val records: RecordRepository,
    categories: CategoryRepository,
    accounts: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val recordId: String = checkNotNull(savedStateHandle["recordId"])
    private val formatter = DateTimeFormatter.ofPattern("d. M. yyyy H:mm", Locale.forLanguageTag("cs-CZ"))
    private val zone = ZoneId.systemDefault()

    val state: StateFlow<RecordDetailUiState> = combine(
        records.observeById(recordId), categories.observeAll(), accounts.observeActive(),
    ) { r, cats, accs ->
        if (r == null) return@combine RecordDetailUiState()
        val byId = cats.associateBy { it.id }
        val accById = accs.associateBy { it.id }
        val signed = when (r.type) {
            RecordType.INCOME -> r.amountMinor
            RecordType.EXPENSE -> -r.amountMinor
            RecordType.TRANSFER -> if (r.transferOut == true) -r.amountMinor else r.amountMinor
        }
        RecordDetailUiState(
            record = r,
            typeLabel = when (r.type) {
                RecordType.EXPENSE -> "Výdaj"
                RecordType.INCOME -> "Příjem"
                RecordType.TRANSFER -> "Převod"
            },
            signedAmountMinor = signed,
            categoryName = r.categoryId?.let { byId[it]?.name },
            accountName = accById[r.accountId]?.name,
            transferAccountName = r.transferAccountId?.let { accById[it]?.name },
            dateText = formatter.format(Instant.ofEpochMilli(r.dateTime).atZone(zone)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordDetailUiState())

    fun delete(onDone: () -> Unit) {
        val r = state.value.record ?: return
        viewModelScope.launch {
            records.deleteWithPartner(r)
            onDone()
        }
    }
}

@Composable
fun RecordDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    vm: RecordDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val record = state.record

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět") }
            Text("Detail záznamu", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (record == null) {
            Text("Záznam nenalezen.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            return
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyAmount(
                state.signedAmountMinor,
                withSign = true,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.fillMaxWidth(),
            )

            DetailRow("Typ", state.typeLabel)
            if (state.isTransfer) {
                DetailRow("Z účtu", state.accountName ?: "—")
                DetailRow("Na účet", state.transferAccountName ?: "—")
            } else {
                DetailRow("Účet", state.accountName ?: "—")
                DetailRow("Kategorie", state.categoryName ?: "Bez kategorie")
            }
            DetailRow("Datum", state.dateText)
            record.payee?.let { DetailRow("Příjemce", it) }
            record.note?.let { DetailRow("Poznámka", it) }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (!state.isTransfer) {
                CalmPrimaryButton("Upravit", onClick = { onEdit(record.id) })
            }
            // Záznamy z Fio jsou napojené na banku (smazaný se při synchronizaci vrátí) —
            // mazat jdou jen ruční záznamy.
            if (record.source != RecordSource.FIO) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Smazat", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Smazat záznam?") },
            text = { Text(if (state.isTransfer) "Smaže se převod včetně obou stran." else "Záznam bude odstraněn.") },
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
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
    }
}
