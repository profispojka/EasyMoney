package cz.calmmoney.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.fio.FioSyncScheduler
import cz.calmmoney.core.recurring.RecurringDetector
import cz.calmmoney.data.repo.FioRepository
import cz.calmmoney.data.repo.FioSyncResult
import cz.calmmoney.data.repo.RecurringRepository
import cz.calmmoney.data.settings.FioConnection
import cz.calmmoney.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class AccountFioStatus(val busy: Boolean = false, val message: String? = null)

data class AccountRecurringPrompt(
    val accountId: String,
    val candidates: List<RecurringDetector.Candidate>,
)

@HiltViewModel
class AccountFioViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val fio: FioRepository,
    private val recurring: RecurringRepository,
    private val syncScheduler: FioSyncScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val accountId: String? = savedStateHandle.get<String>("accountId")

    val connection: StateFlow<FioConnection?> = settings.fioConnections
        .map { list -> list.firstOrNull { it.accountId == accountId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _status = MutableStateFlow(AccountFioStatus())
    val status: StateFlow<AccountFioStatus> = _status

    private val _recurringPrompt = MutableStateFlow<AccountRecurringPrompt?>(null)
    val recurringPrompt: StateFlow<AccountRecurringPrompt?> = _recurringPrompt

    init {
        viewModelScope.launch {
            settings.migrateLegacyFio()
            if (settings.fioConnections.first().isNotEmpty()) syncScheduler.scheduleDaily()
        }
    }

    /** Napojí tento účet na Fio zadaným tokenem a hned synchronizuje. */
    fun connect(token: String) {
        val accId = accountId ?: return
        viewModelScope.launch {
            settings.upsertFioConnection(token, accId)
            _status.value = AccountFioStatus(busy = true)
            val r = fio.sync(token.trim(), accId, 90)
            if (r is FioSyncResult.Success) {
                settings.setFioLastSync(accId, System.currentTimeMillis())
                syncScheduler.scheduleDaily()
            }
            _status.value = AccountFioStatus(busy = false, message = messageFor(r))
            if (r is FioSyncResult.Success) {
                val cands = recurring.detectNew(accId)
                if (cands.isNotEmpty()) _recurringPrompt.value = AccountRecurringPrompt(accId, cands)
            }
        }
    }

    /** Odpojí tento účet od Fia. Naimportované záznamy zůstanou. */
    fun disconnect() {
        val accId = accountId ?: return
        viewModelScope.launch {
            settings.removeFioConnection(accId)
            if (settings.fioConnections.first().isEmpty()) syncScheduler.cancel()
            _status.value = AccountFioStatus()
            _recurringPrompt.value = null
        }
    }

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

/** Sekce „Napojení na Fio" uvnitř detailu účtu (jen u existujícího účtu). */
@Composable
fun AccountFioSection(
    onOpenRecurring: () -> Unit,
    vm: AccountFioViewModel = hiltViewModel(),
) {
    val connection by vm.connection.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val recurringPrompt by vm.recurringPrompt.collectAsStateWithLifecycle()

    var token by rememberSaveable(connection == null) { mutableStateOf("") }
    var confirmDisconnect by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Text("Napojení na Fio", style = MaterialTheme.typography.titleMedium)

        if (connection == null) {
            Text(
                "Zadej read-only Fio token a tenhle účet se bude automaticky plnit pohyby z banky. " +
                    "Token vytvoříš ve Fio internetbankingu: Nastavení → API → Přidat nový token → " +
                    "„Pouhé monitorování účtu“. Je jen pro čtení — platbu přes něj poslat nelze.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Fio token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CalmPrimaryButton(
                text = if (status.busy) "Synchronizuji…" else "Napojit a synchronizovat",
                onClick = { vm.connect(token) },
                enabled = token.isNotBlank() && !status.busy,
            )
        } else {
            Text(
                "Připojeno k Fio. Synchronizuje se automaticky každý den na pozadí. " +
                    "Poslední: ${lastSyncLabel(connection!!.lastSyncMillis)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenRecurring, modifier = Modifier.fillMaxWidth()) {
                Text("Najít opakované platby (trvalé příkazy)")
            }
            OutlinedButton(
                onClick = { confirmDisconnect = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Odpojit od Fio", style = MaterialTheme.typography.titleMedium)
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
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Odpojit od Fio?") },
            text = {
                Text(
                    "Tenhle účet se přestane z Fia automaticky stahovat. Naimportované záznamy zůstanou. " +
                        "Pro opětovné připojení zadáš token znovu.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDisconnect = false; vm.disconnect() }) { Text("Odpojit") }
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

private val syncFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d. M. yyyy HH:mm", Locale.forLanguageTag("cs-CZ"))

private fun lastSyncLabel(millis: Long): String =
    if (millis <= 0L) "zatím nikdy"
    else syncFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
