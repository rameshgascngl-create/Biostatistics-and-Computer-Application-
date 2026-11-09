package com.gasc.zoology.biostat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FlashcardScreen(content: CourseContent, @Suppress("UNUSED_PARAMETER") store: ProgressStore) {
    var unit by rememberSaveable { mutableIntStateOf(0) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var flipped by rememberSaveable { mutableStateOf(false) }
    val unitOf = content.lessons.associate { it.id to it.unit }
    val cards = content.flashcards.filter { unit == 0 || unitOf[it.lesson] == unit }
    val safe = if (cards.isEmpty()) 0 else index.coerceIn(0, cards.size - 1)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0..6).toList()) { u ->
                FilterChip(selected = unit == u, onClick = { unit = u; index = 0; flipped = false },
                    label = { Text(if (u == 0) "All" else "Unit $u") })
            }
        }
        if (cards.isEmpty()) {
            Text("No flashcards for this unit.")
        } else {
            val c = cards[safe]
            Text("Card ${safe + 1} of ${cards.size}", style = MaterialTheme.typography.labelLarge)
            SectionCard(if (flipped) "Definition" else "Term",
                modifier = Modifier.clickable { flipped = !flipped }) {
                Text(if (flipped) c.definition else c.term, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("Tap the card to flip", style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { index = (safe - 1).coerceAtLeast(0); flipped = false }) { Text("← Previous") }
                OutlinedButton(onClick = { index = (safe + 1).coerceAtMost(cards.size - 1); flipped = false }) { Text("Next →") }
            }
        }
    }
}
