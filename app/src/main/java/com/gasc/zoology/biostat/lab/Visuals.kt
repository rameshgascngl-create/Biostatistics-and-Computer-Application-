package com.gasc.zoology.biostat.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gasc.zoology.biostat.LocalTamil
import com.gasc.zoology.biostat.SectionCard
import com.gasc.zoology.biostat.stats.Dist
import com.gasc.zoology.biostat.stats.mean
import com.gasc.zoology.biostat.stats.sd
import com.gasc.zoology.biostat.tr
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

private val HIST_DATA = listOf(12.0, 13.0, 13.0, 14.0, 14.0, 14.0, 15.0, 15.0, 16.0, 17.0, 18.0, 18.0, 19.0, 20.0, 21.0, 23.0, 24.0, 25.0, 26.0, 28.0)

@Composable
fun VisualsScreen() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Interactive visuals", "ஊடாடும் காட்சிகள்"), style = MaterialTheme.typography.titleLarge)
        SamplingVisual(); HistogramVisual(); NormalVisual(); CltVisual()
    }
}

@Composable
private fun SamplingVisual() {
    var selected by rememberSaveable { mutableStateOf(listOf(3, 8, 12, 19, 25)) }
    var tapped by rememberSaveable { mutableIntStateOf(-1) }
    var seed by rememberSaveable { mutableIntStateOf(1) }
    SectionCard(tr("Population → sample", "மக்கள்தொகை → மாதிரி")) {
        Text(tr("Tap a unit. Blue units are the population; amber units form a random sample of 5.", "ஒரு அலகைத் தொடவும். நீல அலகுகள் மக்கள்தொகை; அம்பர் அலகுகள் 5 அலகுகளுள்ள சமவாய்ப்பு மாதிரி."), style = MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().height(170.dp).testTag("sampling-canvas").pointerInput(Unit) {
            detectTapGestures { o ->
                val cw = size.width / 10f; val ch = size.height / 3f
                val c = (o.x / cw).toInt().coerceIn(0, 9); val r = (o.y / ch).toInt().coerceIn(0, 2); tapped = r * 10 + c
            }
        }) {
            val cw = size.width / 10f; val ch = size.height / 3f
            for (i in 0 until 30) {
                val sel = i in selected
                drawCircle(if (sel) Color(0xFFF0A202) else Color(0xFF2E86AB), minOf(cw, ch) * 0.34f, Offset((i % 10 + 0.5f) * cw, (i / 10 + 0.5f) * ch))
                if (i == tapped) drawCircle(Color(0xFF17324D), minOf(cw, ch) * 0.42f, Offset((i % 10 + 0.5f) * cw, (i / 10 + 0.5f) * ch), style = Stroke(4f))
            }
        }
        Text(when {
            tapped < 0 -> tr("Select a unit.", "ஓர் அலகைத் தேர்ந்தெடுக்கவும்.")
            tapped in selected -> tr("Unit ${tapped + 1} belongs to the selected sample.", "அலகு ${tapped + 1} தேர்ந்தெடுக்கப்பட்ட மாதிரியைச் சேர்ந்தது.")
            else -> tr("Unit ${tapped + 1} belongs to the population but was not selected.", "அலகு ${tapped + 1} மக்கள்தொகையில் உள்ளது; ஆனால் தேர்ந்தெடுக்கப்படவில்லை.")
        }, modifier = Modifier.testTag("sampling-text"))
        Button(onClick = { seed++; selected = (0 until 30).shuffled(Random(seed)).take(5); tapped = -1 }) { Text(tr("Draw a new random sample", "புதிய சமவாய்ப்பு மாதிரியை எடு")) }
    }
}

@Composable
private fun HistogramVisual() {
    var bins by rememberSaveable { mutableIntStateOf(5) }
    SectionCard(tr("Histogram class width", "அலைவெண் வரைபட வகுப்பு அகலம்")) {
        Text(tr("Number of bins: $bins", "வகுப்புகளின் எண்ணிக்கை: $bins"))
        Slider(value = bins.toFloat(), onValueChange = { bins = it.toInt() }, valueRange = 3f..8f, steps = 4, modifier = Modifier.testTag("bins-slider"))
        val lo = HIST_DATA.min(); val w = (HIST_DATA.max() - lo) / bins
        val counts = IntArray(bins); HIST_DATA.forEach { counts[minOf(bins - 1, ((it - lo) / w).toInt())]++ }
        PlotView(Plot.Hist((0..bins).map { lo + it * w }, counts.toList()))
        Text(tr("Changing the class width changes the visual detail; a poor choice can hide or exaggerate structure.", "வகுப்பு அகலத்தை மாற்றினால் காட்சி விவரம் மாறும்; தவறான தேர்வு அமைப்பை மறைக்கலாம் அல்லது மிகைப்படுத்தலாம்."), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NormalVisual() {
    var z by rememberSaveable { mutableStateOf(1.0f) }
    SectionCard(tr("Normal curve and tail area", "இயல்பு வளைவும் வால் பரப்பும்")) {
        Text("z = ${fmt(z.toDouble(), 3)}")
        Slider(value = z, onValueChange = { z = it }, valueRange = -3f..3f, modifier = Modifier.testTag("z-slider"))
        val shade = MaterialTheme.colorScheme.secondary; val lineC = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            fun px(x: Double) = ((x + 4) / 8 * size.width).toFloat()
            fun py(x: Double) = (size.height - 12f - (exp(-x * x / 2) / sqrt(2 * PI)) / 0.4 * (size.height - 24f)).toFloat()
            val fillPath = Path().apply {
                moveTo(px(-4.0), size.height - 12f)
                var x = -4.0; while (x <= z + 1e-9) { lineTo(px(x), py(x)); x += 0.05 }
                lineTo(px(z.toDouble()), py(z.toDouble())); lineTo(px(z.toDouble()), size.height - 12f); close()
            }
            drawPath(fillPath, shade.copy(alpha = 0.45f))
            val curve = Path().apply { moveTo(px(-4.0), py(-4.0)); var x = -4.0; while (x <= 4.0) { lineTo(px(x), py(x)); x += 0.05 } }
            drawPath(curve, lineC, style = Stroke(4f))
            drawLine(Color.Gray, Offset(0f, size.height - 12f), Offset(size.width, size.height - 12f), 2f)
        }
        val lower = Dist.normCdf(z.toDouble())
        Text(tr("P(Z ≤ z) = ${fmt(lower, 4)}   ·   P(Z ≥ z) = ${fmt(1 - lower, 4)}", "P(Z ≤ z) = ${fmt(lower, 4)}   ·   P(Z ≥ z) = ${fmt(1 - lower, 4)}"), modifier = Modifier.testTag("z-text"))
    }
}

@Composable
private fun CltVisual() {
    var n by rememberSaveable { mutableIntStateOf(10) }
    var seed by rememberSaveable { mutableIntStateOf(0) }
    val ta = LocalTamil.current
    val means = remember(n, seed) {
        val rnd = Random(seed * 7919 + n)
        List(300) { List(n) { -Math.log(1 - rnd.nextDouble()) }.average() }   // skewed (exponential) population, mean 1, SD 1
    }
    SectionCard(tr("Central limit theorem", "மையவரம்புத் தேற்றம்")) {
        Text(tr("The population is strongly right-skewed (exponential, μ = 1, σ = 1). Each bar shows 300 sample means of size n = $n.", "மக்கள்தொகை வலப்பக்கம் மிகவும் கோட்டமானது (அடுக்குக்குறி, μ = 1, σ = 1). ஒவ்வொரு தண்டும் n = $n அளவுள்ள 300 மாதிரிச் சராசரிகளைக் காட்டுகிறது."), style = MaterialTheme.typography.bodySmall)
        Slider(value = n.toFloat(), onValueChange = { n = it.toInt().coerceAtLeast(1) }, valueRange = 1f..60f, modifier = Modifier.testTag("n-slider"))
        val lo = means.min(); val hi = means.max(); val k = 14; val w = (hi - lo) / k
        val c = IntArray(k); means.forEach { c[minOf(k - 1, ((it - lo) / w).toInt())]++ }
        PlotView(Plot.Hist((0..k).map { lo + it * w }, c.toList()))
        Text(tr("SD of the sample means = ${fmt(sd(means), 3)}; theory σ/√n = ${fmt(1 / sqrt(n.toDouble()), 3)}; mean of means = ${fmt(mean(means), 3)}.",
            "மாதிரிச் சராசரிகளின் திட்ட விலக்கம் = ${fmt(sd(means), 3)}; கோட்பாட்டு σ/√n = ${fmt(1 / sqrt(n.toDouble()), 3)}; சராசரிகளின் சராசரி = ${fmt(mean(means), 3)}."), modifier = Modifier.testTag("clt-text"))
        Text(tr("As n grows the distribution of the mean becomes narrower and more symmetric, which is why t-based methods work for moderately large samples.", "n அதிகரிக்கும்போது சராசரியின் பரவல் குறுகி, சமச்சீராகிறது; இதனால்தான் மிதமான பெரிய மாதிரிகளில் t அடிப்படை முறைகள் செயல்படுகின்றன."), style = MaterialTheme.typography.bodySmall)
        Button(onClick = { seed++ }) { Text(tr("Draw 300 new samples", "300 புதிய மாதிரிகளை எடு")) }
    }
}
