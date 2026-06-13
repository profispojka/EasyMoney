package cz.calmmoney.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.money.Money
import cz.calmmoney.data.db.AccountEntity
import cz.calmmoney.data.db.AccountType
import cz.calmmoney.data.repo.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val accounts: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val accountId: String? = savedStateHandle.get<String>("accountId")
    val isEditing: Boolean get() = accountId != null

    val account: StateFlow<AccountEntity?> =
        (if (accountId != null) accounts.observeById(accountId) else flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(name: String, type: AccountType, initialCents: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            if (accountId != null) {
                accounts.updateAccount(accountId, name, type, initialCents)
            } else {
                accounts.create(name.ifBlank { "Účet" }, type, initialCents, "wallet")
            }
            onDone()
        }
    }
}

@Composable
fun AddAccountScreen(onClose: () -> Unit, vm: AddAccountViewModel = hiltViewModel()) {
    val account by vm.account.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (vm.isEditing) "Upravit účet" else "Nový účet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Zavřít") }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            if (vm.isEditing && account == null) {
                Text("Načítám…", style = MaterialTheme.typography.bodyMedium)
            } else {
                val a = account
                AccountForm(
                    submitLabel = "Uložit",
                    onSubmit = { name, type, cents -> vm.save(name, type, cents, onClose) },
                    initialName = a?.name ?: "",
                    initialType = a?.type ?: AccountType.CASH,
                    initialBalanceText = a?.let { Money.toPlainAmount(it.initialBalanceMinor) } ?: "",
                )
            }
        }
    }
}
