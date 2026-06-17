package cz.calmmoney.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cz.calmmoney.core.designsystem.AccountTypeUi
import cz.calmmoney.core.designsystem.component.CalmChip
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.core.money.Money
import cz.calmmoney.data.db.AccountType

/** Sdílený formulář účtu (onboarding i přidání účtu). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountForm(
    submitLabel: String,
    onSubmit: (name: String, type: AccountType, initialCents: Long, isBusiness: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialType: AccountType = AccountType.CASH,
    initialBalanceText: String = "",
    initialIsBusiness: Boolean = false,
    showBusinessToggle: Boolean = true,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var type by rememberSaveable { mutableStateOf(initialType) }
    var balanceText by rememberSaveable { mutableStateOf(initialBalanceText) }
    var isBusiness by rememberSaveable(initialIsBusiness) { mutableStateOf(initialIsBusiness) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Název účtu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Typ účtu", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountType.entries.forEach { t ->
                CalmChip(
                    label = AccountTypeUi.label(t),
                    selected = t == type,
                    onClick = { type = t },
                    icon = AccountTypeUi.icon(t),
                )
            }
        }

        OutlinedTextField(
            value = balanceText,
            onValueChange = { balanceText = it },
            label = { Text("Počáteční zůstatek (Kč)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        if (showBusinessToggle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isBusiness, onCheckedChange = { isBusiness = it })
                Column(Modifier.padding(start = 4.dp)) {
                    Text("Podnikatelský účet", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Příchozí platby se berou jako příjem (neřeší se od koho).",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        CalmPrimaryButton(
            text = submitLabel,
            enabled = name.isNotBlank(),
            onClick = {
                val cents = Money.parseToMinor(balanceText) ?: 0L
                onSubmit(name.trim(), type, cents, isBusiness)
            },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
