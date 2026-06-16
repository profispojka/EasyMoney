package cz.calmmoney.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.calmmoney.core.designsystem.theme.Gray100

/**
 * Monochromatický spojnicový graf s jemnou šedou výplní (E-Ink). Hodnoty zleva (nejstarší)
 * doprava (nejnovější), popisky pod osou. Bez barev, bez animací.
 */
@Composable
fun TrendChart(
    points: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val lineColor = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (points.size < 2) return@Canvas
            val maxV = (points.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
            val padX = 8.dp.toPx()
            val topPad = size.height * 0.12f
            val w = size.width - 2 * padX
            val h = size.height - topPad
            val n = points.size
            fun px(i: Int) = padX + w * i / (n - 1)
            fun py(v: Long) = topPad + h - (v.toFloat() / maxV) * h

            val line = Path()
            points.forEachIndexed { i, v ->
                if (i == 0) line.moveTo(px(i), py(v)) else line.lineTo(px(i), py(v))
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(px(n - 1), size.height)
                lineTo(px(0), size.height)
                close()
            }
            drawPath(fill, color = Gray100)
            drawPath(line, color = lineColor, style = Stroke(width = 2.dp.toPx()))
            points.forEachIndexed { i, v ->
                drawCircle(lineColor, radius = 2.5.dp.toPx(), center = Offset(px(i), py(v)))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
