package cz.calmmoney.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.designsystem.component.CalmTopBar
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.core.recurring.RecurringDetector
import cz.calmmoney.data.repo.CategoryRepository
import cz.calmmoney.data.repo.RecurringRepository
import cz.calmmoney.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringRowUi(
    val candidate: RecurringDetector.Candidate,
    val categoryName: String,
)

data class RecurringUiState(
    val rows: List<RecurringRowUi> = emptyList(),
    val loading: Boolean = true,
    val accountId: String? = null,
    val message: String? = null,
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val recurring: RecurringRepository,
    private val categories: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecurringUiState())
    val state: StateFlow<RecurringUiState> = _state

    init {
        viewModelScope.launch { _state.value = compute(null) }
    }

    /** Přidá vybrané jako plánované platby a **ukončí** wizard (zavře obrazovku). */
    fun add(selected: List<RecurringDetector.Candidate>, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value.accountId?.let { recurring.addAsPlanned(it, selected) }
            onDone()
        }
    }

    private suspend fun compute(message: String?): RecurringUiState {
        val accId = settings.fioAccountId.first()
        val catMap = categories.observeAll().first().associateBy { it.id }
        val rows = recurring.detectNew(accId)
            .map { RecurringRowUi(it, catMap[it.categoryId]?.name ?: "Bez kategorie") }
        return RecurringUiState(rows = rows, loading = false, accountId = accId, message = message)
    }
}

@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    vm: RecurringViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val checked = remember { mutableStateMapOf<String, Boolean>() }

    Column(Modifier.fillMaxSize()) {
        CalmTopBar("Opakované platby", onBack = onBack)

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
            state.rows.isEmpty() -> Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    state.message ?: "Nenašel jsem žádné pravidelné platby. Stáhni nejdřív pohyby z Fia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                Text(
                    "Tyhle platby se opakují každý měsíc. Vyber, které chceš přidat mezi plánované.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(state.rows, key = { it.candidate.key + it.candidate.amountMinor }) { row ->
                        val k = row.candidate.key + row.candidate.amountMinor
                        val isChecked = checked[k] ?: true
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = { checked[k] = it })
                            Column(Modifier.weight(1f)) {
                                Text(row.candidate.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "měsíčně · ${row.categoryName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            MoneyAmount(-row.candidate.amountMinor, withSign = false, style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                val selected = state.rows.filter { (checked[it.candidate.key + it.candidate.amountMinor] ?: true) }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.message?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    CalmPrimaryButton(
                        text = "Přidat vybrané (${selected.size})",
                        onClick = { vm.add(selected.map { it.candidate }, onBack) },
                        enabled = selected.isNotEmpty(),
                    )
                }
            }
        }
    }
}
