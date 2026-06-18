package cz.heller.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cz.heller.core.designsystem.component.CalmDialogDismissButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cz.heller.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.core.designsystem.AccountTypeUi
import cz.heller.core.designsystem.component.CalmPrimaryButton
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.data.db.AccountEntity
import cz.heller.data.repo.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountRow(val account: AccountEntity, val balanceMinor: Long)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accounts: AccountRepository,
) : ViewModel() {
    val rows: StateFlow<List<AccountRow>> =
        combine(accounts.observeActive(), accounts.observeBalances()) { accs, balances ->
            val map = balances.associate { it.accountId to it.balanceMinor }
            accs.map { AccountRow(it, map[it.id] ?: it.initialBalanceMinor) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(account: AccountEntity) {
        viewModelScope.launch { accounts.delete(account) }
    }
}

@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    vm: AccountsViewModel = hiltViewModel(),
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<AccountEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(stringResource(R.string.accounts_title), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(Modifier.padding(16.dp)) {
            CalmPrimaryButton(text = stringResource(R.string.accounts_add), onClick = onAdd)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(rows, key = { it.account.id }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(row.account.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        AccountTypeUi.icon(row.account.type),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(row.account.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(AccountTypeUi.labelRes(row.account.type)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MoneyAmount(row.balanceMinor, withSign = false)
                    IconButton(onClick = { toDelete = row.account }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    val account = toDelete
    if (account != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.account_delete_title)) },
            text = { Text(stringResource(R.string.account_delete_message, account.name)) },
            confirmButton = {
                TextButton(onClick = { vm.delete(account); toDelete = null }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                CalmDialogDismissButton(onClick = { toDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
