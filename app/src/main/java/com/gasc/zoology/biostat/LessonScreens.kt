package com.gasc.zoology.biostat

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.background

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LessonListScreen(content: CourseContent, store: ProgressStore, onOpen: (String) -> Unit) {
    val ta = store.tamil
    var unit by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var onlyMarked by rememberSaveable { mutableStateOf(false) }
    val shown = content.lessons.filter { l ->
        (unit == 0 || l.unit == unit) && (!onlyMarked || l.id in store.bookmarks) &&
            (query.isBlank() || (if (ta) content.tamilLessons[l.id]?.title.orEmpty() else l.title).contains(query.trim(), ignoreCase = true))
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("lesson-search"),
                label = { Text(tr("Search lessons", "பாடங்களைத் தேடு")) })
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..6).toList()) { u ->
                    FilterChip(selected = unit == u, onClick = { unit = u },
                        label = { Text(if (u == 0) tr("All", "அனைத்தும்") else tr("Unit $u", "அலகு $u")) })
                }
                item { FilterChip(selected = onlyMarked, onClick = { onlyMarked = !onlyMarked }, label = { Text(tr("★ Bookmarks", "★ புத்தகக்குறிகள்")) }) }
            }
        }
        if (shown.isEmpty()) item { Para(tr("No lessons match.", "பொருந்தும் பாடங்கள் இல்லை.")) }
        items(shown, key = { it.id }) { l ->
            val done = l.id in store.completed
            val title = if (ta) content.tamilLessons[l.id]?.title.orEmpty().ifBlank { l.id } else l.title
            SectionCard(
                title = (if (done) "✓ " else "") + (if (l.id in store.bookmarks) "★ " else "") + title,
                modifier = Modifier.clickable { onOpen(l.id) }.testTag("lesson-${l.id}")
            ) {
                Para(if (ta) "அலகு ${l.unit} · ${l.study.read} நிமிட வாசிப்பு" else "Unit ${l.unit} · ${l.difficulty} · ${l.study.read} min read")
            }
        }
    }
}

/**
 * A native two-mode textbook: the default book mode shows one complete teaching section
 * per page, and the alternative continuous mode preserves the full expandable chapter.
 * Both use the same ChapterPage data; no content is duplicated or silently omitted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonScreen(
    content: CourseContent, l: Lesson, store: ProgressStore,
    onOpen: (String) -> Unit, onOpenContents: () -> Unit = {}
) {
    val ta = store.tamil
    val translation = content.tamilLessons[l.id]
    val idx = content.lessons.indexOfFirst { it.id == l.id }
    val previous = content.lessons.getOrNull(idx - 1)
    val next = content.lessons.getOrNull(idx + 1)
    val title = if (ta) translation?.title.orEmpty().ifBlank { l.title } else l.title
    val pages = remember(content, l.id, ta) { chapterPages(content, l, ta) }
    var bookMode by rememberSaveable(l.id) { mutableStateOf(true) }
    var requestedPage by rememberSaveable(l.id) { mutableIntStateOf(0) }
    val totalPages = pages.size + 1 // Last page is the workbook: notes and chapter completion.
    val activePage = requestedPage.coerceIn(0, totalPages - 1)
    val listState = rememberLazyListState()
    LaunchedEffect(bookMode, activePage, l.id, ta) {
        listState.scrollToItem(0)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF4F0E8)).testTag("reader-scroll"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item(key = "chapter-cover") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A4A7D))
            ) {
                Column(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(
                            Color(0xFF12365F), Color(0xFF245FA0), Color(0xFF087982)
                        ))
                    ).padding(19.dp)
                ) {
                    Text(
                        tr("BIOSTATISTICS • TEXTBOOK", "உயிரிப் புள்ளியியல் • பாடநூல்"),
                        color = Color(0xFFD2F4F2),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("UNIT ${l.unit}  •  CHAPTER ${idx + 1} / ${content.lessons.size}",
                            "அலகு ${l.unit}  •  பாடம் ${idx + 1} / ${content.lessons.size}"),
                        color = Color(0xFFE1ECFF),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        title,
                        color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        lineHeight = MaterialTheme.typography.headlineSmall.lineHeight
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (ta) "${l.study.read} நிமிட வாசிப்பு  •  ${l.study.practice} நிமிடப் பயிற்சி"
                        else "${l.study.read} min read  •  ${l.study.practice} min practice",
                        color = Color(0xFFE2EEFA),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(11.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenContents,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .67f)),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("reader-contents")
                        ) { Text(tr("Contents", "பொருளடக்கம்"), fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { store.toggleBookmark(l.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .67f)),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("bookmark")
                        ) {
                            Text(
                                if (l.id in store.bookmarks)
                                    tr("★ Bookmarked", "★ புத்தகக்குறியிடப்பட்டது")
                                else tr("☆ Bookmark", "☆ புத்தகக்குறி"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        item(key = "reading-switch") {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    tr("READING STYLE", "வாசிப்பு முறை"),
                    color = Color(0xFF53657A), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = bookMode,
                        onClick = { bookMode = true },
                        modifier = Modifier.testTag("reader-mode-pages"),
                        label = { Text(tr("Book pages", "புத்தகப் பக்கங்கள்")) }
                    )
                    FilterChip(
                        selected = !bookMode,
                        onClick = { bookMode = false },
                        modifier = Modifier.testTag("reader-mode-continuous"),
                        label = { Text(tr("Full chapter", "முழுப் பாடம்")) }
                    )
                }
            }
        }
        if (bookMode) {
            item(key = "active-page") {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("reader-page"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF6)),
                    border = BorderStroke(1.dp, Color(0xFFE0D8CB)),
                    shape = RoundedCornerShape(15.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tr("CHAPTER ${idx + 1}", "பாடம் ${idx + 1}"),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF335D82),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                tr("PAGE ${activePage + 1} / $totalPages",
                                    "பக்கம் ${activePage + 1} / $totalPages"),
                                modifier = Modifier.testTag("reader-page-number"),
                                color = Color(0xFF375F7B),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFDFD7CA))
                        Spacer(Modifier.height(13.dp))
                        if (activePage < pages.size) {
                            val section = pages[activePage]
                            LessonSection(
                                section.title, initiallyOpenOverride = true
                            ) {
                                section.content(this)
                            }
                        } else {
                            ChapterNotebook(l, store)
                            Spacer(Modifier.height(12.dp))
                            ChapterCompletion(l, store)
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFDFD7CA))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            tr("Biostatistics Mastery • Page ${activePage + 1}",
                                "உயிரிப் புள்ளியியல் • பக்கம் ${activePage + 1}"),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF68758A),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            item(key = "page-navigation") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { requestedPage = (activePage - 1).coerceAtLeast(0) },
                        enabled = activePage > 0,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                            .testTag("reader-previous-page")
                    ) { Text(tr("← Previous page", "← முந்தைய பக்கம்")) }
                    Button(
                        onClick = { requestedPage = (activePage + 1).coerceAtMost(totalPages - 1) },
                        enabled = activePage < totalPages - 1,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                            .testTag("reader-next-page")
                    ) { Text(tr("Next page →", "அடுத்த பக்கம் →")) }
                }
            }
            if (activePage == totalPages - 1) {
                item(key = "book-chapter-navigation") {
                    ChapterNavigation(previous, next, onOpen)
                }
            }
        } else {
            items(pages.indices.toList(), key = { "chapter-section-" + it }) { index ->
                val section = pages[index]
                LessonSection(section.title) {
                    section.content(this)
                }
            }
            item(key = "continuous-notes") { ChapterNotebook(l, store) }
            item(key = "continuous-completion") { ChapterCompletion(l, store) }
            item(key = "continuous-chapter-navigation") {
                ChapterNavigation(previous, next, onOpen)
            }
        }
    }
}

@Composable
private fun ChapterNotebook(l: Lesson, store: ProgressStore) {
    var note by remember(l.id) { mutableStateOf(store.notes[l.id].orEmpty()) }
    SectionCard(tr("My notes • Study journal", "என் குறிப்புகள் • கற்றல் பதிவு")) {
        OutlinedTextField(
            value = note,
            onValueChange = {
                note = it.take(ProgressStore.MAX_NOTE)
                store.setNote(l.id, note)
            },
            modifier = Modifier.fillMaxWidth().testTag("note"),
            minLines = 3,
            label = { Text(tr("Write a note for this chapter", "இப்பாடத்திற்கான குறிப்பை எழுதுக")) }
        )
    }
}

@Composable
private fun ChapterCompletion(l: Lesson, store: ProgressStore) {
    val done = l.id in store.completed
    Button(
        onClick = { store.toggleLesson(l.id) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("complete")
    ) {
        Text(if (done) tr("Completed — tap to undo", "முடிந்தது — மாற்ற தொடவும்")
            else tr("Mark lesson as complete", "பாடத்தை முடிந்ததாகக் குறி"))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterNavigation(
    previous: Lesson?, next: Lesson?, onOpen: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (previous != null) OutlinedButton(
            onClick = { onOpen(previous.id) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("reader-previous-chapter")
        ) { Text(tr("← Previous chapter", "← முந்தைய பாடம்")) }
        if (next != null) Button(
            onClick = { onOpen(next.id) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("reader-next-chapter")
        ) { Text(tr("Next chapter →", "அடுத்த பாடம் →")) }
    }
}
