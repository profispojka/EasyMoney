package cz.calmmoney.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.calmmoney.data.db.AccountType
import cz.calmmoney.data.repo.AccountRepository
import cz.calmmoney.feature.accounts.AccountForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val accounts: AccountRepository,
) : ViewModel() {
    fun createFirstAccount(name: String, type: AccountType, initialCents: Long) {
        viewModelScope.launch {
            accounts.create(name.ifBlank { "Hotovost" }, type, initialCents, "wallet")
        }
    }
}

@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "Vítej v CalmMoney",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            text = "Začni vytvořením prvního účtu. Měna je vždy CZK.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        AccountForm(
            submitLabel = "Začít",
            onSubmit = { name, type, cents, _ -> vm.createFirstAccount(name, type, cents) },
            showBusinessToggle = false,
        )
    }
}
