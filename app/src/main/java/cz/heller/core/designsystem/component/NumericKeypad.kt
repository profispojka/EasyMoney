package cz.heller.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.heller.R

/**
 * Číselná klávesnice pro zadání částky (plní se zprava jako na POS).
 * onDigit(0..9), onDoubleZero (00), onBackspace.
 */
@Composable
fun NumericKeypad(
    onDigit: (Int) -> Unit,
    onDoubleZero: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("00", "0", "⌫"),
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspace()
                                "00" -> onDoubleZero()
                                else -> onDigit(key.toInt())
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(0.dp),
                    ) {
                        if (key == "⌫") {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        } else {
                            Text(key, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
