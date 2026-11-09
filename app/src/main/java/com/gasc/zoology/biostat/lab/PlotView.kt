package com.gasc.zoology.biostat.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.gasc.zoology.biostat.tr

private fun niceRange(lo: Double, hi: Double): Pair<Double, Double> {
    if (hi - lo < 1e-12) return (lo - 1) to (hi + 1)
    val pad = (hi - lo) * 0.06
    return (lo - pad) to (hi + pad)
}

@Composable
fun PlotView(plot: Plot, modifier: Modifier = Modifier) {
    val axis = Color(0xFF55707A); val fill = MaterialTheme.colorScheme.primary; val line = Color(0xFFD1495B)
    val xLabel: String; val yLabel: String
    when (plot) {
        is Plot.Hist -> { xLabel = tr("Value", "மதிப்பு"); yLabel = tr("Frequency", "நிகழ்வெண்") }
        is Plot.Scatter -> { xLabel = plot.xLabel; yLabel = plot.yLabel }
    }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val l = 44f; val b = 30f; val t = 10f; val r = 10f
            val w = size.width - l - r; val h = size.height - b - t
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 22f; isAntiAlias = true }
            fun tick(v: Double) = fmt(v, 3)
            when (plot) {
                is Plot.Hist -> {
                    val lo = plot.edges.first(); val hi = plot.edges.last(); val top = (plot.counts.max()).toDouble().coerceAtLeast(1.0)
                    fun px(x: Double) = l + ((x - lo) / (hi - lo)).toFloat() * w
                    plot.counts.forEachIndexed { i, c ->
                        val x0 = px(plot.edges[i]); val x1 = px(plot.edges[i + 1]); val y = t + h - (c / top).toFloat() * h
                        drawRect(fill, Offset(x0 + 1f, y), Size(x1 - x0 - 2f, t + h - y))
                    }
                    drawLine(axis, Offset(l, t + h), Offset(l + w, t + h), 2f); drawLine(axis, Offset(l, t), Offset(l, t + h), 2f)
                    drawContext.canvas.nativeCanvas.drawText(tick(lo), l, size.height - 6f, paint)
                    drawContext.canvas.nativeCanvas.drawText(tick(hi), l + w - 50f, size.height - 6f, paint)
                    drawContext.canvas.nativeCanvas.drawText(top.toInt().toString(), 8f, t + 18f, paint)
                }
                is Plot.Scatter -> {
                    val (x0, x1) = niceRange(plot.xs.min(), plot.xs.max()); val (y0, y1) = niceRange(plot.ys.min(), plot.ys.max())
                    fun px(x: Double) = l + ((x - x0) / (x1 - x0)).toFloat() * w
                    fun py(y: Double) = t + h - ((y - y0) / (y1 - y0)).toFloat() * h
                    drawLine(axis, Offset(l, t + h), Offset(l + w, t + h), 2f); drawLine(axis, Offset(l, t), Offset(l, t + h), 2f)
                    plot.line?.let { ln -> drawLine(line, Offset(px(ln[0].first), py(ln[0].second)), Offset(px(ln[1].first), py(ln[1].second)), 3f) }
                    plot.xs.indices.forEach { i -> drawCircle(fill, 6f, Offset(px(plot.xs[i]), py(plot.ys[i]))) }
                    drawContext.canvas.nativeCanvas.drawText(tick(x0 + (x1 - x0) * 0.06), l, size.height - 6f, paint)
                    drawContext.canvas.nativeCanvas.drawText(tick(x1 - (x1 - x0) * 0.06), l + w - 50f, size.height - 6f, paint)
                    drawContext.canvas.nativeCanvas.drawText(tick(y1 - (y1 - y0) * 0.06), 2f, t + 18f, paint)
                    drawContext.canvas.nativeCanvas.drawText(tick(y0 + (y1 - y0) * 0.06), 2f, t + h - 4f, paint)
                }
            }
        }
        Text("$xLabel  ·  $yLabel", style = MaterialTheme.typography.labelSmall)
    }
}
