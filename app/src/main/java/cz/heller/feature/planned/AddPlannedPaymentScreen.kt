package cz.heller.feature.planned

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.R
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.designsystem.component.CalmPrimaryButton
import cz.heller.core.money.Money
import cz.heller.core.time.PlannedPayments
import cz.heller.data.db.AccountEntity
import cz.heller.data.db.CategoryEntity
import cz.heller.data.db.CategoryType
import cz.heller.data.db.FrequencyUnit
import cz.heller.data.db.PlannedPaymentEntity
import cz.heller.data.db.RecordType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.PlannedPaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class FreqPreset(@StringRes val labelRes: Int, val unit: FrequencyUnit, val count: Int)

val FREQ_PRESETS = listOf(
    FreqPreset(R.string.freq_preset_weekly, FrequencyUnit.WEEK, 1),
    FreqPreset(R.string.freq_preset_monthly, FrequencyUnit.MONTH, 1),
    FreqPreset(R.string.freq_preset_quarterly, FrequencyUnit.MONTH, 3),
    FreqPreset(R.string.freq_preset_3x_year, FrequencyUnit.MONTH, 4),
    FreqPreset(R.string.freq_preset_2x_year, FrequencyUnit.MONTH, 6),
    FreqPreset(R.string.freq_preset_yearly, FrequencyUnit.YEAR, 1),
)

private fun presetIndexFor(unit: FrequencyUnit, count: Int): Int =
    FREQ_PRESETS.indexOfFirst { it.unit == unit && it.count == count }.let { if (it >= 0) it else 1 }

data class PlannedOptions(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
)

@HiltViewModel
class AddPlannedPaymentViewModel @Inject constructor(
    private val planned: PlannedPaymentRepository,
    accountsRepo: AccountRepository,
    categoriesRepo: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plannedId: String? = savedStateHandle.get<String>("plannedId")
    val isEditing: Boolean get() = plannedId != null

    val options: StateFlow<PlannedOptions> = combine(
        accountsRepo.observeActive(), categoriesRepo.observeAll(),
    ) { accs, cats -> PlannedOptions(accs, cats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannedOptions())

    val editing: StateFlow<PlannedPaymentEntity?> =
        (if (plannedId != null) planned.observeById(plannedId) else flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(
        name: String,
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        preset: FreqPreset,
        startDay: Long,
        endDay: Long?,
        note: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            if (plannedId != null) {
                planned.update(
                    id = plannedId, name = name, type = type, accountId = accountId, categoryId = categoryId,
                    amountMinor = amountMinor, frequencyUnit = preset.unit, frequencyCount = preset.count,
                    startEpochDay = startDay, endEpochDay = endDay, note = note,
                )
            } else {
                planned.create(
                    name = name, type = type, accountId = accountId, categoryId = categoryId,
                    amountMinor = amountMinor, frequencyUnit = preset.unit, frequencyCount = preset.count,
                    startEpochDay = startDay, endEpochDay = endDay, note = note,
                )
            }
            onDone()
        }
    }
}

@Composable
fun AddPlannedPaymentScreen(
    onClose: () -> Unit,
    onPickCategory: (CategoryType) -> Unit,
    pickedCategoryId: String? = null,
    onPickedConsumed: () -> Unit = {},
    vm: AddPlannedPaymentViewModel = hiltViewModel(),
) {
    val options by vm.options.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(if (vm.isEditing) R.string.planned_edit_title else R.string.planned_add_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close)) }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (vm.isEditing && editing == null) {
            Text(stringResource(R.string.account_loading), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        } else {
            PlannedPaymentForm(
                options = options,
                initial = editing,
                onPickCategory = onPickCategory,
                pickedCategoryId = pickedCategoryId,
                onPickedConsumed = onPickedConsumed,
                onSubmit = { name, type, accountId, categoryId, cents, preset, startDay, endDay, note ->
                    vm.save(name, type, accountId, categoryId, cents, preset, startDay, endDay, note, onClose)
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlannedPaymentForm(
    options: PlannedOptions,
    initial: PlannedPaymentEntity?,
    onPickCategory: (CategoryType) -> Unit,
    pickedCategoryId: String?,
    onPickedConsumed: () -> Unit,
    onSubmit: (String, RecordType, String, String?, Long, FreqPreset, Long, Long?, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: RecordType.EXPENSE) }
    var accountId by rememberSaveable { mutableStateOf(initial?.accountId) }
    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId) }
    var amountText by rememberSaveable { mutableStateOf(initial?.let { Money.toPlainAmount(it.amountMinor) } ?: "") }
    var freqIndex by rememberSaveable { mutableStateOf(initial?.let { presetIndexFor(it.frequencyUnit, it.frequencyCount) } ?: 1) }
    var startDay by rememberSaveable { mutableStateOf(initial?.startEpochDay ?: LocalDate.now().toEpochDay()) }
    var endDay by rememberSaveable { mutableStateOf(initial?.endEpochDay) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }

    var showAccount by remember { mutableStateOf(false) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    LaunchedEffect(pickedCategoryId) {
        if (pickedCategoryId != null) {
            categoryId = pickedCategoryId.ifEmpty { null }
            onPickedConsumed()
        }
    }

    val effectiveAccountId = accountId ?: options.accounts.firstOrNull()?.id
    val catType = if (type == RecordType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    val categoryName = options.categories.firstOrNull { it.id == categoryId }?.name ?: stringResource(R.string.no_category)
    val amountCents = Money.parseToMinor(amountText) ?: 0L
    val canSave = name.isNotBlank() && amountCents > 0 && effectiveAccountId != null

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text(stringResource(R.string.planned_name_label)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip(stringResource(R.string.type_expense), type == RecordType.EXPENSE, onClick = { type = RecordType.EXPENSE; categoryId = null }, modifier = Modifier.weight(1f))
            CalmChip(stringResource(R.string.type_income), type == RecordType.INCOME, onClick = { type = RecordType.INCOME; categoryId = null }, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = amountText, onValueChange = { amountText = it },
            label = { Text(stringResource(R.string.field_amount_currency, Money.currencySymbol)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        SelectorRow(stringResource(R.string.detail_account), options.accounts.firstOrNull { it.id == effectiveAccountId }?.name ?: "—") { showAccount = true }

        SelectorRow(stringResource(R.string.detail_category), categoryName) { onPickCategory(catType) }

        Text(stringResource(R.string.detail_frequency), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FREQ_PRESETS.forEachIndexed { i, p ->
                CalmChip(stringResource(p.labelRes), freqIndex == i, onClick = { freqIndex = i })
            }
        }

        SelectorRow(stringResource(R.string.planned_start_label), PlannedPayments.formatDate(LocalDate.ofEpochDay(startDay))) { showStart = true }
        SelectorRow(stringResource(R.string.planned_end_label), endDay?.let { PlannedPayments.formatDate(LocalDate.ofEpochDay(it)) } ?: stringResource(R.string.no_end)) { showEnd = true }
        if (endDay != null) {
            CalmChip(stringResource(R.string.planned_clear_end), selected = false, onClick = { endDay = null })
        }

        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text(stringResource(R.string.field_note)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )

        CalmPrimaryButton(
            text = stringResource(R.string.action_save),
            enabled = canSave,
            onClick = {
                onSubmit(name, type, effectiveAccountId!!, categoryId, amountCents, FREQ_PRESETS[freqIndex], startDay, endDay, note)
            },
        )
    }

    if (showAccount) {
        AlertDialog(
            onDismissRequest = { showAccount = false },
            title = { Text(stringResource(R.string.pick_account)) },
            text = {
                Column {
                    options.accounts.forEach { a ->
                        TextButton(onClick = { accountId = a.id; showAccount = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(a.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAccount = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (showStart) {
        DatePickerSheet(initialEpochDay = startDay, onPick = { startDay = it; showStart = false }, onDismiss = { showStart = false })
    }
    if (showEnd) {
        DatePickerSheet(initialEpochDay = endDay ?: startDay, onPick = { endDay = it; showEnd = false }, onDismiss = { showEnd = false })
    }
}

@Composable
private fun SelectorRow(label: String, value: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initialEpochDay: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val initialUtc = LocalDate.ofEpochDay(initialEpochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val dpState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = dpState.selectedDateMillis ?: initialUtc
                val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                onPick(day)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) { DatePicker(state = dpState) }
}
