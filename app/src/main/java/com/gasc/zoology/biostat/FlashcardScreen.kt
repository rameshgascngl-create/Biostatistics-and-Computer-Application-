package com.gasc.zoology.biostat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun FlashcardScreen(content: CourseContent, store: ProgressStore) {
    val ta = store.tamil
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
                    label = { Text(if (u == 0) tr("All", "அனைத்தும்") else tr("Unit $u", "அலகு $u")) })
            }
        }
        if (cards.isEmpty()) {
            Text(tr("No flashcards for this unit.", "இந்த அலகிற்கு அட்டைகள் இல்லை."))
        } else {
            val c = cards[safe]
            val tc = content.extra.flashcards[c.id]
            val term = if (ta) tc?.term?.ifBlank { null } ?: content.tamilLessons[c.lesson]?.title.orEmpty() else c.term
            val definition = if (ta) tc?.definition.orEmpty() else c.definition
            val known = c.id in store.known
            Text(tr("Card ${safe + 1} of ${cards.size}", "அட்டை ${safe + 1} / ${cards.size}") + if (known) "  ✓" else "", style = MaterialTheme.typography.labelLarge)
            SectionCard(if (flipped) tr("Definition", "வரையறை") else tr("Term", "சொல்"),
                modifier = Modifier.clickable { flipped = !flipped }.testTag("card")) {
                Text(if (flipped) definition else term, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text(tr("Tap the card to flip", "அட்டையைத் திருப்ப தொடவும்"), style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { index = (safe - 1).coerceAtLeast(0); flipped = false }) { Text(tr("← Previous", "← முந்தையது")) }
                OutlinedButton(onClick = { index = (safe + 1).coerceAtMost(cards.size - 1); flipped = false }) { Text(tr("Next →", "அடுத்தது →")) }
            }
            Button(onClick = { store.toggleKnown(c.id) }, modifier = Modifier.testTag("known")) {
                Text(if (known) tr("Marked as known — undo", "அறிந்ததாகக் குறிக்கப்பட்டது — மாற்று") else tr("I know this", "இது எனக்குத் தெரியும்"))
            }
        }
    }
}
