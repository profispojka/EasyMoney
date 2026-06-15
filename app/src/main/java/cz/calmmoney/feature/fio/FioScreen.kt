package cz.calmmoney.feature.fio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmCard
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.designsystem.component.CalmTopBar
import cz.calmmoney.core.fio.FioSample
import cz.calmmoney.data.db.AccountEntity
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.CategorizationRepository
import cz.calmmoney.data.repo.FioRepository
import cz.calmmoney.data.repo.FioSyncResult
import cz.calmmoney.data.repo.RecurringRepository
import cz.calmmoney.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

data class FioPrefs(
    val savedToken: String = "",
    val accountId: String? = null,
    val lastSyncMillis: Long = 0,
    val accounts: List<AccountEntity> = emptyList(),
)

data class FioStatus(val busy: Boolean = false, val message: String? = null, val recurringFound: Int = 0)

@HiltViewModel
class FioViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val fio: FioRepository,
    private val categorization: CategorizationRepository,
    private val recurring: RecurringRepository,
    accounts: AccountRepository,
) : ViewModel() {

    val prefs: StateFlow<FioPrefs> = combine(
        settings.fioToken, settings.fioAccountId, settings.fioLastSync, accounts.observeActive(),
    ) { token, accId, last, accs ->
        FioPrefs(token, accId, last, accs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FioPrefs())

    private val _status = MutableStateFlow(FioStatus())
    val status: StateFlow<FioStatus> = _status

    fun saveAndSync(token: String, accountId: String) {
        viewModelScope.launch {
            settings.setFioConnection(token, accountId)
            _status.value = FioStatus(busy = true)
            val r = fio.sync(token.trim(), accountId)
            if (r is FioSyncResult.Success) settings.setFioLastSync(System.currentTimeMillis())
            val found = if (r is FioSyncResult.Success) recurring.detectNew(accountId).size else 0
            _status.value = FioStatus(busy = false, message = messageFor(r), recurringFound = found)
        }
    }

    fun importDemo(accountId: String) {
        viewModelScope.launch {
            _status.value = FioStatus(busy = true)
            val r = fio.importJson(FioSample.JSON, accountId)
            _status.value = FioStatus(busy = false, message = messageFor(r))
        }
    }

    /** Znovu projede nezařazené záznamy podle aktuálních pravidel (po naučení/úpravě seedu). */
    fun recategorize() {
        viewModelScope.launch {
            _status.value = FioStatus(busy = true)
            val n = categorization.recategorizeUncategorized()
            _status.value = FioStatus(busy = false, message = "Dodatečně zařazeno $n záznamů.")
        }
    }

    private fun messageFor(r: FioSyncResult): String = when (r) {
        is FioSyncResult.Success -> "Hotovo: přidáno ${r.added} nových z ${r.total} pohybů, zařazeno ${r.categorized}."
        FioSyncResult.RateLimited -> "Fio dovolí jen 1 dotaz za 30 s. Zkus to za chvíli."
        is FioSyncResult.Error -> r.message
    }
}

@Composable
fun FioScreen(
    onBack: () -> Unit,
    onOpenRecurring: () -> Unit,
    vm: FioViewModel = hiltViewModel(),
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()

    var token by rememberSaveable(prefs.savedToken) { mutableStateOf(prefs.savedToken) }
    var accountId by remember(prefs.accountId, prefs.accounts) {
        mutableStateOf(prefs.accountId ?: prefs.accounts.firstOrNull()?.id)
    }
    val selectedAccount = prefs.accounts.firstOrNull { it.id == accountId }

    Column(Modifier.fillMaxSize()) {
        CalmTopBar("Fio – import", onBack = onBack)

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CalmCard(Modifier.fillMaxWidth()) {
                Text("Read-only napojení", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ve Fio internetbankingu: Nastavení → API → Přidat nový token → oprávnění " +
                        "„Pouhé monitorování účtu“. Token vlož níže. Je jen pro čtení — platbu přes " +
                        "něj poslat nelze.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Fio token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text("Importovat do účtu", style = MaterialTheme.typography.labelLarge)
                AccountSelector(
                    accounts = prefs.accounts,
                    selected = selectedAccount,
                    onSelect = { accountId = it.id },
                )
            }

            val canSync = token.isNotBlank() && accountId != null && !status.busy
            CalmPrimaryButton(
                text = if (status.busy) "Synchronizuji…" else "Uložit a synchronizovat",
                onClick = { accountId?.let { vm.saveAndSync(token, it) } },
                enabled = canSync,
            )

            if (status.busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(2.dp))
                    Text("Stahuji pohyby z Fia…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            status.message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            if (status.recurringFound > 0) {
                CalmCard(Modifier.fillMaxWidth()) {
                    Text("Pravidelné platby", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Našel jsem ${status.recurringFound} plateb, co se opakují každý měsíc " +
                            "(trvalé příkazy / předplatné). Chceš je přidat mezi plánované platby?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    CalmPrimaryButton(text = "Zobrazit a přidat", onClick = onOpenRecurring)
                }
            }

            Text(
                "Poslední synchronizace: " + lastSyncLabel(prefs.lastSyncMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                "Pohyby se při importu rovnou kategorizují (podle pravidel a podle toho, jak je " +
                    "sám zařazuješ). Co nejde určit, zůstane bez kategorie. Opakovaná synchronizace " +
                    "nevytvoří duplicity (dedup dle ID pohybu).",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { vm.recategorize() },
                enabled = !status.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Překategorizovat nezařazené")
            }
            TextButton(
                onClick = onOpenRecurring,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Najít opakované platby (trvalé příkazy)")
            }

            CalmCard(Modifier.fillMaxWidth()) {
                Text("Vyzkoušet bez tokenu", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Naimportuje 4 ukázkové pohyby do vybraného účtu, ať vidíš, jak to vypadá. " +
                        "Klidně je pak smaž v Záznamech.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                CalmPrimaryButton(
                    text = "Importovat ukázková data",
                    onClick = { accountId?.let { vm.importDemo(it) } },
                    enabled = accountId != null && !status.busy,
                )
            }
        }
    }
}

@Composable
private fun AccountSelector(
    accounts: List<AccountEntity>,
    selected: AccountEntity?,
    onSelect: (AccountEntity) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selected?.name ?: "Vyber účet",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text(acc.name) },
                    onClick = { onSelect(acc); open = false },
                )
            }
        }
    }
}

private val syncFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d. M. yyyy HH:mm", Locale.forLanguageTag("cs-CZ"))

private fun lastSyncLabel(millis: Long): String =
    if (millis <= 0L) "zatím nikdy"
    else syncFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
