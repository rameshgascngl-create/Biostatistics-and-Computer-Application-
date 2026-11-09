package com.gasc.zoology.biostat

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * One academic section per book page. The same page definitions render both
 * the paged reader and the continuous accordion, so no lesson content is lost
 * when switching reading modes.
 */
internal data class ChapterPage(
    val title: String,
    val content: @Composable ColumnScope.() -> Unit
)

internal fun chapterPages(
    course: CourseContent, lesson: Lesson, tamil: Boolean
): List<ChapterPage> = buildList {
    if (!tamil) {
        if (lesson.objectives.isNotEmpty()) add(ChapterPage("Learning objectives") { Bullets(lesson.objectives) })
        if (lesson.intro.isNotBlank()) add(ChapterPage("Introduction") { Para(lesson.intro) })
        if (lesson.definition.isNotBlank()) add(ChapterPage("Definition") { Para(lesson.definition) })
        if (lesson.history.isNotBlank()) add(ChapterPage("Background") { Para(lesson.history) })
        if (lesson.need.isNotEmpty()) add(ChapterPage("Why it matters") { Bullets(lesson.need) })
        if (lesson.classification.isNotEmpty()) add(ChapterPage("Classification") {
            lesson.classification.forEach { Para("${it.name}: ${it.text}") }
        })
        if (lesson.explanation.isNotEmpty()) add(ChapterPage("Explanation") {
            lesson.explanation.forEach { Para(it) }
        })
        lesson.formulae.forEach { formula ->
            add(ChapterPage("Formula — ${formula.name}") {
                Mono(formula.formula)
                if (formula.meaning.isNotBlank()) Para(formula.meaning)
                formula.symbols.forEach { (symbol, meaning) -> Para("$symbol — $meaning") }
                if (formula.assumptions.isNotEmpty()) {
                    Para("Assumptions:")
                    Bullets(formula.assumptions)
                }
                if (formula.use.isNotBlank()) Para("Use: ${formula.use}")
                if (formula.avoid.isNotBlank()) Para("Avoid when: ${formula.avoid}")
                if (formula.bio.isNotBlank()) Para("Biological example: ${formula.bio}")
            })
        }
        if (lesson.examples.isNotEmpty()) add(ChapterPage("Worked examples") {
            lesson.examples.forEach { example ->
                var show by androidx.compose.runtime.saveable.rememberSaveable(example.q) {
                    androidx.compose.runtime.mutableStateOf(false)
                }
                Para("${example.level}: ${example.q}")
                if (example.steps.isNotEmpty()) Bullets(example.steps)
                if (show) Para("Answer: ${example.answer}")
                androidx.compose.material3.OutlinedButton(onClick = { show = !show }) {
                    Text(if (show) "Hide answer" else "Show answer")
                }
            }
        })
        if (lesson.applications.isNotEmpty()) add(ChapterPage("Applications") {
            lesson.applications.forEach { (field, notes) -> Para(field); Bullets(notes) }
        })
        if (lesson.advantages.isNotEmpty()) add(ChapterPage("Advantages") { Bullets(lesson.advantages) })
        if (lesson.limitations.isNotEmpty()) add(ChapterPage("Limitations") { Bullets(lesson.limitations) })
        if (lesson.precautions.isNotEmpty()) add(ChapterPage("Precautions") { Bullets(lesson.precautions) })
        if (lesson.misconceptions.isNotEmpty()) add(ChapterPage("Common misconceptions") { Bullets(lesson.misconceptions) })
        if (lesson.confused.isNotEmpty()) add(ChapterPage("Often confused") { Bullets(lesson.confused) })
        if (lesson.exam.isNotEmpty()) add(ChapterPage("Exam guidance") { Bullets(lesson.exam) })
        if (lesson.mnemonic.isNotBlank()) add(ChapterPage("Memory aid") { Para(lesson.mnemonic) })
        if (lesson.summary.isNotEmpty()) add(ChapterPage("Summary") { Bullets(lesson.summary) })
        if (lesson.selfcheck.isNotEmpty()) add(ChapterPage("Self-check") { Bullets(lesson.selfcheck) })
        if (lesson.references.isNotEmpty()) add(ChapterPage("References") { Bullets(lesson.references) })
    } else {
        val translation = course.tamilLessons[lesson.id]
        if (translation == null) {
            add(ChapterPage("உள்ளடக்கம்") { Para("இந்தப் பாடத்திற்கான தமிழாக்கம் இன்னும் கிடைக்கவில்லை.") })
        } else {
            if (translation.overview.isNotBlank()) add(ChapterPage("மேலோட்டம்") { Para(translation.overview) })
            if (translation.definition.isNotBlank()) add(ChapterPage("வரையறை") { Para(translation.definition) })
            if (translation.importance.isNotEmpty()) add(ChapterPage("முக்கியத்துவம்") { Bullets(translation.importance) })
            if (translation.explanation.isNotEmpty()) add(ChapterPage("விளக்கம்") {
                translation.explanation.forEach { Para(it) }
            })
            lesson.formulae.forEach { formula ->
                val tamilFormula = course.extra.formulas["${lesson.id}|${formula.name}"]
                if (tamilFormula != null) add(ChapterPage("சூத்திரம் — ${tamilFormula.name}") {
                    Mono(formula.formula)
                    Para(tamilFormula.meaning)
                    Para("பயன்பாடு: ${tamilFormula.use}")
                })
            }
            if (translation.bioExamples.isNotEmpty()) add(ChapterPage("எடுத்துக்காட்டுகள்") { Bullets(translation.bioExamples) })
            if (translation.points.isNotEmpty()) add(ChapterPage("முக்கியக் குறிப்புகள்") { Bullets(translation.points) })
            if (translation.exam.isNotEmpty()) add(ChapterPage("தேர்வு வழிகாட்டி") { Bullets(translation.exam) })
            if (translation.mistakes.isNotEmpty()) add(ChapterPage("பொதுவான தவறுகள்") { Bullets(translation.mistakes) })
            if (translation.summary.isNotEmpty()) add(ChapterPage("சுருக்கம்") { Bullets(translation.summary) })
            if (translation.selfcheck.isNotEmpty()) add(ChapterPage("சுயசோதனை") { Bullets(translation.selfcheck) })
        }
    }
}
