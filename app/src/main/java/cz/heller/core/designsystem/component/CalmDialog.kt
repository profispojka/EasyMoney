package cz.heller.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Cancel/dismiss button styling shared by confirmation dialogs.
 *
 * Matches the desired dialog style with 2 dp top padding and 2 dp outline border.
 */
@Composable
fun CalmDialogDismissButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.padding(top = 2.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        enabled = enabled,
    ) {
        content()
    }
}
