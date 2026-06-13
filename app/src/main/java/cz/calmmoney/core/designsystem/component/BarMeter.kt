package cz.calmmoney.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vodorovný monochromatický ukazatel (rozpočet, žebříček). Track = rámeček,
 * výplň = plná černá dle [fraction] (0..1). Bez animace (E-Ink).
 */
@Composable
fun BarMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall),
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.onSurface),
        )
    }
}
