package cz.heller.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cz.heller.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.money.AppCurrency
import cz.heller.core.money.Money
import cz.heller.data.db.AccountType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.settings.SettingsRepository
import cz.heller.feature.accounts.AccountForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    fun createFirstAccount(name: String, type: AccountType, initialCents: Long, currencyCode: String) {
        viewModelScope.launch {
            settings.setCurrency(currencyCode)
            Money.applyCurrency(currencyCode)
            accounts.create(name.ifBlank { "Hotovost" }, type, initialCents, "wallet")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    var currency by remember { mutableStateOf(AppCurrency.CZK) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )

        Text(stringResource(R.string.onboarding_currency), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        ) {
            AppCurrency.entries.forEach { c ->
                CalmChip(
                    label = "${c.symbol} (${c.code})",
                    selected = currency == c,
                    onClick = { currency = c },
                )
            }
        }

        AccountForm(
            submitLabel = stringResource(R.string.onboarding_start),
            onSubmit = { name, type, cents, _ -> vm.createFirstAccount(name, type, cents, currency.code) },
            showBusinessToggle = false,
        )
    }
}
