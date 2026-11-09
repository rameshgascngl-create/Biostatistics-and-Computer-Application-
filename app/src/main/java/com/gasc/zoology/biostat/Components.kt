package com.gasc.zoology.biostat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    tamil: Boolean = false,
    body: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFD8E4F2)),
        colors = CardDefaults.cardColors(
            containerColor = if (tamil) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(9.dp))
            body()
        }
    }
}

@Composable
fun Bullets(items: List<String>) {
    items.forEach {
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Text("•", modifier = Modifier.width(20.dp), color = MaterialTheme.colorScheme.secondary)
            Text(it, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 1.48.em),
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Justify)
        }
    }
}

/** Textbook paragraphs use true native Android paragraph justification (not WebView). */
@Composable
fun Para(text: String) {
    Text(text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 1.52.em),
        textAlign = TextAlign.Justify,
        color = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun Mono(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.45.em),
        fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 5.dp))
}
