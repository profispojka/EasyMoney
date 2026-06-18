package cz.heller.feature.budgets

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.R
import cz.heller.core.designsystem.CategoryIcons
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.designsystem.component.CalmPrimaryButton
import cz.heller.core.money.Money
import cz.heller.data.db.BudgetPeriod
import cz.heller.data.repo.BudgetRepository
import cz.heller.data.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBudgetViewModel @Inject constructor(
    private val budgets: BudgetRepository,
    categories: CategoryRepository,
) : ViewModel() {
    val groups: StateFlow<List<CategoryGroup>> =
        categories.observeByType(cz.heller.data.db.CategoryType.EXPENSE)
            .map { expenseGroups(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, groupIds: List<String>, amountCents: Long, period: BudgetPeriod, onDone: () -> Unit) {
        viewModelScope.launch {
            budgets.create(name, groupIds, amountCents, period)
            onDone()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddBudgetScreen(onClose: () -> Unit, vm: AddBudgetViewModel = hiltViewModel()) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(BudgetPeriod.MONTH) }
    val selected = remember { mutableStateListOf<String>() }

    val amountCents = Money.parseToMinor(amountText) ?: 0L
    val canSave = name.isNotBlank() && amountCents > 0

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.budget_add_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close)) }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.budget_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.budget_limit_label, Money.currencySymbol)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.budget_period_label), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetPeriod.entries.forEach { p ->
                    CalmChip(stringResource(periodLabel(p)), selected = period == p, onClick = { period = p }, modifier = Modifier.weight(1f))
                }
            }

            Text(stringResource(R.string.budget_categories_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { g ->
                    val isSel = g.id in selected
                    CalmChip(
                        label = g.name,
                        selected = isSel,
                        onClick = { if (isSel) selected.remove(g.id) else selected.add(g.id) },
                        icon = CategoryIcons.forKey(g.icon),
                    )
                }
            }

            CalmPrimaryButton(
                text = stringResource(R.string.action_save),
                enabled = canSave,
                onClick = { vm.create(name, selected.toList(), amountCents, period, onClose) },
            )
        }
    }
}
