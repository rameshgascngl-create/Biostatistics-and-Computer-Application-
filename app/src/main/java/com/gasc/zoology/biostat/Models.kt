package com.gasc.zoology.biostat

import kotlinx.serialization.Serializable

@Serializable data class Study(val read: Int = 0, val practice: Int = 0, val revise: Int = 0)
@Serializable data class NamedText(val name: String = "", val text: String = "")
@Serializable data class Example(
    val level: String = "", val q: String = "",
    val steps: List<String> = emptyList(), val answer: String = ""
)
@Serializable data class Formula(
    val name: String = "", val formula: String = "", val meaning: String = "",
    val symbols: Map<String, String> = emptyMap(),
    val assumptions: List<String> = emptyList(),
    val use: String = "", val avoid: String = "", val bio: String = ""
)

@Serializable data class Lesson(
    val id: String,
    val unit: Int = 0,
    val sequence: Int = 0,
    val title: String = "",
    val status: String = "",
    val co: String = "",
    val difficulty: String = "",
    val study: Study = Study(),
    val weight: String = "",
    val prereq: List<String> = emptyList(),
    val related: List<String> = emptyList(),
    val objectives: List<String> = emptyList(),
    val intro: String = "",
    val history: String = "",
    val definition: String = "",
    val need: List<String> = emptyList(),
    val classification: List<NamedText> = emptyList(),
    val explanation: List<String> = emptyList(),
    val applications: Map<String, List<String>> = emptyMap(),
    val advantages: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
    val precautions: List<String> = emptyList(),
    val examples: List<Example> = emptyList(),
    val misconceptions: List<String> = emptyList(),
    val confused: List<String> = emptyList(),
    val exam: List<String> = emptyList(),
    val mnemonic: String = "",
    val summary: List<String> = emptyList(),
    val formulae: List<Formula> = emptyList(),
    val selfcheck: List<String> = emptyList(),
    val references: List<String> = emptyList()
)

@Serializable data class TamilLesson(
    val title: String = "",
    val overview: String = "",
    val definition: String = "",
    val importance: List<String> = emptyList(),
    val explanation: List<String> = emptyList(),
    val bioExamples: List<String> = emptyList(),
    val points: List<String> = emptyList(),
    val exam: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val summary: List<String> = emptyList(),
    val selfcheck: List<String> = emptyList()
)

@Serializable data class Question(
    val id: String, val lesson: String = "", val unit: String = "", val type: String = "MCQ",
    val q: String, val o: List<String>, val a: Int, val e: String = ""
)
@Serializable data class TamilQuestion(val q: String = "", val o: List<String> = emptyList(), val e: String = "")
@Serializable data class Flashcard(val id: String, val lesson: String = "", val term: String, val definition: String)
@Serializable data class SyllabusUnit(
    val unit: String, val title: String, val status: String = "", val topics: List<String> = emptyList()
)

@Serializable data class TamilFormula(val name: String = "", val meaning: String = "", val use: String = "")
@Serializable data class TamilCard(val term: String = "", val definition: String = "")
@Serializable data class TamilSyllabus(val title: String = "", val topics: List<String> = emptyList())
@Serializable data class ToolkitText(
    val title: String = "", val phrase: String = "", val text: String = "", val data: String = "", val task: String = "",
    val lines: List<String> = emptyList()
)
@Serializable data class ToolkitItem(val en: ToolkitText = ToolkitText(), val ta: ToolkitText = ToolkitText())
@Serializable data class Toolkit(
    val mnemonics: List<ToolkitItem> = emptyList(), val problems: List<ToolkitItem> = emptyList(),
    val cheat: List<ToolkitItem> = emptyList()
)
/** Tamil-mode content for parts of the course that the lesson JSON does not carry (formulae, cards, syllabus, toolkit). */
@Serializable data class TamilExtra(
    val formulas: Map<String, TamilFormula> = emptyMap(), val flashcards: Map<String, TamilCard> = emptyMap(),
    val syllabus: List<TamilSyllabus> = emptyList(), val toolkit: Toolkit = Toolkit()
)

data class CourseContent(
    val lessons: List<Lesson>,
    val tamilLessons: Map<String, TamilLesson>,
    val questions: List<Question>,
    val tamilQuestions: Map<String, TamilQuestion>,
    val flashcards: List<Flashcard>,
    val syllabus: List<SyllabusUnit>,
    val extra: TamilExtra = TamilExtra()
) {
    fun lesson(id: String) = lessons.firstOrNull { it.id == id }
}
