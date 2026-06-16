package cz.calmmoney.feature.fio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmCard
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.designsystem.component.CalmTopBar
import cz.calmmoney.core.fio.FioSyncScheduler
import cz.calmmoney.core.recurring.RecurringDetector
import cz.calmmoney.data.db.AccountEntity
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.data.repo.FioRepository
import cz.calmmoney.data.repo.FioSyncResult
import cz.calmmoney.data.repo.RecurringRepository
import cz.calmmoney.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

data class FioStatus(val busy: Boolean = false, val message: String? = null)

/** Nabídka pravidelných plateb po synchronizaci (modal). */
data class RecurringPrompt(
    val accountId: String,
    val candidates: List<RecurringDetector.Candidate>,
)

@HiltViewModel
class FioViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val fio: FioRepository,
    private val recurring: RecurringRepository,
    private val syncScheduler: FioSyncScheduler,
    accounts: AccountRepository,
) : ViewModel() {

    val prefs: StateFlow<FioPrefs> = combine(
        settings.fioToken, settings.fioAccountId, settings.fioLastSync, accounts.observeActive(),
    ) { token, accId, last, accs ->
        FioPrefs(token, accId, last, accs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FioPrefs())

    private val _status = MutableStateFlow(FioStatus())
    val status: StateFlow<FioStatus> = _status

    private val _recurringPrompt = MutableStateFlow<RecurringPrompt?>(null)
    val recurringPrompt: StateFlow<RecurringPrompt?> = _recurringPrompt

    init {
        // Připojený účet → zajisti, že denní synchronizace běží (idempotentní).
        viewModelScope.launch {
            if (settings.fioLastSync.first() > 0L) syncScheduler.scheduleDaily()
        }
    }

    fun saveAndSync(token: String, accountId: String) = launchSync(token, accountId, daysBack = 90)

    /** Odpojí Fio (zastaví automatickou synchronizaci). Naimportované záznamy zůstanou. */
    fun disconnect() {
        viewModelScope.launch {
            syncScheduler.cancel()
            settings.clearFio()
            _status.value = FioStatus()
            _recurringPrompt.value = null
        }
    }

    private fun launchSync(token: String, accountId: String, daysBack: Long) {
        viewModelScope.launch {
            settings.setFioConnection(token, accountId)
            _status.value = FioStatus(busy = true)
            val r = fio.sync(token.trim(), accountId, daysBack)
            if (r is FioSyncResult.Success) {
                settings.setFioLastSync(System.currentTimeMillis())
                syncScheduler.scheduleDaily()
            }
            _status.value = FioStatus(busy = false, message = messageFor(r))
            if (r is FioSyncResult.Success) {
                val cands = recurring.detectNew(accountId)
                if (cands.isNotEmpty()) _recurringPrompt.value = RecurringPrompt(accountId, cands)
            }
        }
    }

    /** „Ano" v modalu — přidá všechny nalezené pravidelné platby. */
    fun addAllRecurring() {
        val p = _recurringPrompt.value ?: return
        viewModelScope.launch {
            val n = recurring.addAsPlanned(p.accountId, p.candidates)
            _recurringPrompt.value = null
            _status.value = _status.value.copy(message = "Přidáno $n plánovaných plateb.")
        }
    }

    fun dismissRecurring() {
        _recurringPrompt.value = null
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
    val recurringPrompt by vm.recurringPrompt.collectAsStateWithLifecycle()

    var token by rememberSaveable(prefs.savedToken) { mutableStateOf(prefs.savedToken) }
    var accountId by remember(prefs.accountId, prefs.accounts) {
        mutableStateOf(prefs.accountId ?: prefs.accounts.firstOrNull()?.id)
    }
    val selectedAccount = prefs.accounts.firstOrNull { it.id == accountId }
    var confirmDisconnect by remember { mutableStateOf(false) }
    val connected = prefs.lastSyncMillis > 0

    Column(Modifier.fillMaxSize()) {
        CalmTopBar("Fio – import", onBack = onBack)

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!connected) {
                // --- Před prvním připojením: token + první synchronizace ---
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

                CalmPrimaryButton(
                    text = if (status.busy) "Synchronizuji…" else "Uložit a synchronizovat",
                    onClick = { accountId?.let { vm.saveAndSync(token, it) } },
                    enabled = token.isNotBlank() && accountId != null && !status.busy,
                )
            } else {
                // --- Připojeno: automatická denní synchronizace, ruční sync už netřeba ---
                CalmCard(Modifier.fillMaxWidth()) {
                    Text("Připojeno k Fio", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Účet: ${selectedAccount?.name ?: "—"}. Synchronizuje se automaticky každý " +
                            "den na pozadí. Poslední: ${lastSyncLabel(prefs.lastSyncMillis)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            if (status.busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(2.dp))
                    Text("Stahuji pohyby z Fia…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            status.message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            if (connected) {
                TextButton(
                    onClick = onOpenRecurring,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Najít opakované platby (trvalé příkazy)")
                }
                OutlinedButton(
                    onClick = { confirmDisconnect = true },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Zrušit synchronizaci", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Zrušit synchronizaci?") },
            text = {
                Text(
                    "Přestane se automaticky stahovat z Fia. Naimportované záznamy zůstanou. " +
                        "Pro opětovné připojení zadáš token znovu.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDisconnect = false; vm.disconnect() }) {
                    Text("Zrušit synchronizaci")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Zpět") } },
        )
    }

    recurringPrompt?.let { prompt ->
        Dialog(
            onDismissRequest = { vm.dismissRecurring() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Pravidelné platby", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Našel jsem ${prompt.candidates.size} plateb, co se opakují každý měsíc " +
                                "(trvalé příkazy / předplatné). Přidat je mezi plánované platby?",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { vm.dismissRecurring(); onOpenRecurring() }) {
                            Text("Raději vybrat ručně")
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalmPrimaryButton(text = "Ano, přidat", onClick = { vm.addAllRecurring() })
                        OutlinedButton(
                            onClick = { vm.dismissRecurring() },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text("Ne", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
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
