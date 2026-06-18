package cz.heller.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.heller.core.designsystem.theme.Gray100
import cz.heller.core.designsystem.theme.Gray500

/** Primární akce: plná černá výplň, bílý text. */
@Composable
fun CalmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = Gray100,
            disabledContentColor = Gray500,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
