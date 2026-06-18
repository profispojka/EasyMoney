package cz.heller.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import cz.heller.core.money.Money

/**
 * Částka v CZK. Příjem/výdaj se na E-Ink rozlišuje znaménkem a tučností, ne barvou.
 * @param amountMinor částka v haléřích (kladná = příjem, záporná = výdaj, je-li [withSign]).
 */
@Composable
fun MoneyAmount(
    amountMinor: Long,
    modifier: Modifier = Modifier,
    withSign: Boolean = true,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Text(
        text = Money.format(amountMinor, withSign),
        style = style,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}
