package cz.calmmoney.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Potvrzovací modal vyjíždějící zespodu (styl Mudita) — titulek na střed a dvě tlačítka přes celou
 * šířku (potvrzení + „Zrušit"). Horní hrana má černý 3 dp border, tlačítka kulaté rohy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmConfirmSheet(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        shape = RectangleShape,
        dragHandle = null,
    ) {
        val buttonShape = RoundedCornerShape(16.dp)
        Column(Modifier.fillMaxWidth()) {
            // Horní border modalu (3 dp, černá).
            HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.onBackground)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                Button(
                    onClick = onConfirm,
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(confirmLabel, style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = buttonShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Zrušit", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
