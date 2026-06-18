package cz.heller.feature.accounts

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
import cz.heller.core.designsystem.component.CalmDialogDismissButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import cz.heller.R
import cz.heller.core.designsystem.component.CalmPrimaryButton
import cz.heller.core.fio.FioSyncScheduler
import cz.heller.core.recurring.RecurringDetector
import cz.heller.data.repo.FioRepository
import cz.heller.data.repo.FioSyncResult
import cz.heller.data.repo.RecurringRepository
import cz.heller.data.settings.FioConnection
import cz.heller.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
            _status.value = _status.value.copy(message = context.getString(R.string.fio_added_planned, n))
        }
    }

    fun dismissRecurring() {
        _recurringPrompt.value = null
    }

    private fun messageFor(r: FioSyncResult): String = when (r) {
        is FioSyncResult.Success -> context.getString(R.string.fio_sync_success, r.added, r.total, r.categorized)
        FioSyncResult.RateLimited -> context.getString(R.string.fio_rate_limited)
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
        Text(stringResource(R.string.fio_section_title), style = MaterialTheme.typography.titleMedium)

        val ctx = LocalContext.current
        if (connection == null) {
            Text(
                stringResource(R.string.fio_connect_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(stringResource(R.string.fio_token_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CalmPrimaryButton(
                text = stringResource(if (status.busy) R.string.fio_syncing else R.string.fio_connect_sync),
                onClick = { vm.connect(token) },
                enabled = token.isNotBlank() && !status.busy,
            )
        } else {
            Text(
                stringResource(R.string.fio_connected, lastSyncLabel(ctx, connection!!.lastSyncMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenRecurring, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.fio_find_recurring))
            }
            OutlinedButton(
                onClick = { confirmDisconnect = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.fio_disconnect), style = MaterialTheme.typography.titleMedium)
            }
        }

        if (status.busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(2.dp))
                Text(stringResource(R.string.fio_downloading), style = MaterialTheme.typography.bodyMedium)
            }
        }
        status.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(stringResource(R.string.fio_disconnect_title)) },
            text = {
                Text(stringResource(R.string.fio_disconnect_message))
            },
            confirmButton = {
                TextButton(onClick = { confirmDisconnect = false; vm.disconnect() }) { Text(stringResource(R.string.fio_disconnect_confirm)) }
            },
            dismissButton = { CalmDialogDismissButton(onClick = { confirmDisconnect = false }) { Text(stringResource(R.string.action_back)) } },
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
                        Text(stringResource(R.string.fio_recurring_title), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.fio_recurring_message, prompt.candidates.size),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { vm.dismissRecurring(); onOpenRecurring() }) {
                            Text(stringResource(R.string.fio_recurring_manual))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalmPrimaryButton(text = stringResource(R.string.fio_recurring_yes), onClick = { vm.addAllRecurring() })
                        OutlinedButton(
                            onClick = { vm.dismissRecurring() },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text(stringResource(R.string.action_no), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

private val syncFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d. M. yyyy HH:mm", Locale.getDefault())

private fun lastSyncLabel(context: Context, millis: Long): String =
    if (millis <= 0L) context.getString(R.string.fio_never)
    else syncFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
