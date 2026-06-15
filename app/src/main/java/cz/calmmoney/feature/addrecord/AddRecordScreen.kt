package cz.calmmoney.feature.addrecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmChip
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.money.Money
import cz.calmmoney.data.db.AccountEntity
import cz.calmmoney.data.db.CategoryEntity
import cz.calmmoney.data.db.CategoryType
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategorizationRepository
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class AddRecordUiState(
    val type: RecordType = RecordType.EXPENSE,
    val amountText: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedToAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val note: String = "",
    val dateTimeMillis: Long = 0L,
) {
    val amountMinor: Long get() = Money.parseToMinor(amountText) ?: 0L
    val canSave: Boolean
        get() = amountMinor > 0 && when (type) {
            RecordType.TRANSFER -> selectedAccountId != null && selectedToAccountId != null &&
                selectedAccountId != selectedToAccountId
            else -> selectedAccountId != null
        }
}

data class PendingSiblings(val categoryId: String, val payee: String, val count: Int)

@HiltViewModel
class AddRecordViewModel @Inject constructor(
    accountsRepo: AccountRepository,
    categoriesRepo: CategoryRepository,
    private val records: RecordRepository,
    private val categorization: CategorizationRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {

    private val editingId: String? = savedStateHandle.get<String>("recordId")
    val isEditing: Boolean get() = editingId != null

    private val _pendingSiblings = MutableStateFlow<PendingSiblings?>(null)
    val pendingSiblings: StateFlow<PendingSiblings?> = _pendingSiblings

    private data class Form(
        val type: RecordType = RecordType.EXPENSE,
        val amountText: String = "",
        val accountId: String? = null,
        val toAccountId: String? = null,
        val categoryId: String? = null,
        val note: String = "",
        val payee: String? = null,
        val dateTimeMillis: Long = System.currentTimeMillis(),
    )

    private val form = MutableStateFlow(Form())

    init {
        val id = editingId
        if (id != null) {
            viewModelScope.launch {
                records.getById(id)?.let { r ->
                    form.value = Form(
                        type = r.type,
                        amountText = Money.toPlainAmount(r.amountMinor),
                        accountId = r.accountId,
                        categoryId = r.categoryId,
                        note = r.note ?: "",
                        payee = r.payee,
                        dateTimeMillis = r.dateTime,
                    )
                }
            }
        }
    }

    val state: StateFlow<AddRecordUiState> = combine(
        form, accountsRepo.observeActive(), categoriesRepo.observeAll(),
    ) { f, accs, cats ->
        val typeCats = when (f.type) {
            RecordType.INCOME -> cats.filter { it.type.name == "INCOME" }
            RecordType.EXPENSE -> cats.filter { it.type.name == "EXPENSE" }
            RecordType.TRANSFER -> emptyList()
        }
        AddRecordUiState(
            type = f.type,
            amountText = f.amountText,
            accounts = accs,
            categories = typeCats,
            selectedAccountId = f.accountId ?: accs.firstOrNull()?.id,
            selectedToAccountId = f.toAccountId ?: accs.getOrNull(1)?.id,
            selectedCategoryId = f.categoryId,
            note = f.note,
            dateTimeMillis = f.dateTimeMillis,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddRecordUiState())

    fun setType(type: RecordType) = form.update { it.copy(type = type, categoryId = null) }
    fun setAmount(text: String) = form.update { it.copy(amountText = text) }
    fun setCategory(id: String?) = form.update { it.copy(categoryId = id?.ifEmpty { null }) }
    fun selectAccount(id: String) = form.update { it.copy(accountId = id) }
    fun selectToAccount(id: String) = form.update { it.copy(toAccountId = id) }
    fun selectCategory(id: String) = form.update { it.copy(categoryId = if (it.categoryId == id) null else id) }
    fun setNote(s: String) = form.update { it.copy(note = s) }
    fun setDateTime(millis: Long) = form.update { it.copy(dateTimeMillis = millis) }

    fun save(onDone: () -> Unit) {
        val s = state.value
        if (!s.canSave) return
        val id = editingId
        viewModelScope.launch {
            when {
                id != null -> records.updateEntry(
                    id = id,
                    type = s.type,
                    accountId = s.selectedAccountId!!,
                    categoryId = s.selectedCategoryId,
                    amountMinor = s.amountMinor,
                    dateTime = s.dateTimeMillis,
                    payee = form.value.payee,
                    note = s.note,
                )
                s.type == RecordType.TRANSFER -> records.addTransfer(
                    fromAccountId = s.selectedAccountId!!,
                    toAccountId = s.selectedToAccountId!!,
                    amountMinor = s.amountMinor,
                    dateTime = s.dateTimeMillis,
                    note = s.note,
                )
                else -> records.addEntry(
                    type = s.type,
                    accountId = s.selectedAccountId!!,
                    categoryId = s.selectedCategoryId,
                    amountMinor = s.amountMinor,
                    dateTime = s.dateTimeMillis,
                    note = s.note,
                )
            }
            // Po ručním zařazení záznamu s obchodníkem nabídni „použít na všechny stejné".
            val payee = form.value.payee
            val cat = s.selectedCategoryId
            if (id != null && cat != null && !payee.isNullOrBlank()) {
                val count = categorization.countUncategorizedForMerchant(payee)
                if (count > 0) {
                    _pendingSiblings.value = PendingSiblings(cat, payee, count)
                    return@launch
                }
            }
            onDone()
        }
    }

    fun applySiblings(onDone: () -> Unit) {
        val p = _pendingSiblings.value ?: return onDone()
        viewModelScope.launch {
            categorization.applyToMerchant(p.payee, p.categoryId)
            _pendingSiblings.value = null
            onDone()
        }
    }

    fun skipSiblings(onDone: () -> Unit) {
        _pendingSiblings.value = null
        onDone()
    }
}

private fun typeLabel(type: RecordType): String = when (type) {
    RecordType.EXPENSE -> "Výdaj"
    RecordType.INCOME -> "Příjem"
    RecordType.TRANSFER -> "Převod"
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(
    onClose: () -> Unit,
    onPickCategory: (CategoryType) -> Unit,
    pickedCategoryId: String? = null,
    onPickedConsumed: () -> Unit = {},
    vm: AddRecordViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pending by vm.pendingSiblings.collectAsStateWithLifecycle()

    LaunchedEffect(pickedCategoryId) {
        if (pickedCategoryId != null) {
            vm.setCategory(pickedCategoryId)
            onPickedConsumed()
        }
    }

    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (vm.isEditing) "Upravit záznam" else "Nový záznam",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Zavřít") }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Typ
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordType.entries.forEach { t ->
                    CalmChip(
                        label = typeLabel(t),
                        selected = state.type == t,
                        onClick = { vm.setType(t) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Částka — běžný číselný input (po kliknutí naskočí systémová klávesnice)
            OutlinedTextField(
                value = state.amountText,
                onValueChange = vm::setAmount,
                label = { Text("Částka (Kč)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.type == RecordType.TRANSFER) {
                SelectorRow("Z účtu", accountName(state, state.selectedAccountId)) { pickFrom = true }
                SelectorRow("Na účet", accountName(state, state.selectedToAccountId)) { pickTo = true }
            } else {
                SelectorRow("Účet", accountName(state, state.selectedAccountId)) { pickFrom = true }
                SelectorRow("Kategorie", categoryName(state)) {
                    onPickCategory(if (state.type == RecordType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE)
                }
            }

            SelectorRow("Datum a čas", formatDateTime(state.dateTimeMillis)) { showDate = true }

            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = { Text("Poznámka") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            CalmPrimaryButton(text = "Uložit", enabled = state.canSave, onClick = { vm.save(onClose) })
        }
    }

    if (pickFrom) {
        AccountPickerDialog(state.accounts, onPick = { vm.selectAccount(it); pickFrom = false }, onDismiss = { pickFrom = false })
    }
    if (pickTo) {
        AccountPickerDialog(state.accounts, onPick = { vm.selectToAccount(it); pickTo = false }, onDismiss = { pickTo = false })
    }

    if (showDate) {
        val localDate = Instant.ofEpochMilli(state.dateTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val initialUtc = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dpState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDate = dpState.selectedDateMillis ?: initialUtc
                    showDate = false
                    showTime = true
                }) { Text("Dál") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Zrušit") } },
        ) { DatePicker(state = dpState) }
    }

    if (showTime) {
        val zdt = Instant.ofEpochMilli(state.dateTimeMillis).atZone(ZoneId.systemDefault())
        val tpState = rememberTimePickerState(initialHour = zdt.hour, initialMinute = zdt.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Čas") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    val base = pickedDate ?: zdt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    vm.setDateTime(combineDateTime(base, tpState.hour, tpState.minute))
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Zrušit") } },
        )
    }

    pending?.let { p ->
        val catName = state.categories.firstOrNull { it.id == p.categoryId }?.name ?: "tuhle kategorii"
        AlertDialog(
            onDismissRequest = { vm.skipSiblings(onClose) },
            title = { Text("Použít na všechny stejné?") },
            text = {
                Text("Máš ještě ${p.count} nezařazených plateb od „${p.payee}“. Zařadit je taky jako „$catName“?")
            },
            confirmButton = { TextButton(onClick = { vm.applySiblings(onClose) }) { Text("Použít na všechny") } },
            dismissButton = { TextButton(onClick = { vm.skipSiblings(onClose) }) { Text("Jen tuhle") } },
        )
    }
}

private fun accountName(state: AddRecordUiState, id: String?): String =
    state.accounts.firstOrNull { it.id == id }?.name ?: "—"

private fun categoryName(state: AddRecordUiState): String =
    state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name ?: "Bez kategorie"

private val addRecordDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d. M. yyyy H:mm", Locale.forLanguageTag("cs-CZ"))

private fun formatDateTime(millis: Long): String =
    addRecordDateFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

private fun combineDateTime(dateUtcMillis: Long, hour: Int, minute: Int): Long {
    val date = Instant.ofEpochMilli(dateUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return LocalDateTime.of(date, LocalTime.of(hour, minute))
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@Composable
private fun SelectorRow(label: String, value: String, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("$label: ", style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AccountPickerDialog(
    accounts: List<AccountEntity>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vyber účet") },
        text = {
            Column {
                accounts.forEach { a ->
                    TextButton(onClick = { onPick(a.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(a.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}
