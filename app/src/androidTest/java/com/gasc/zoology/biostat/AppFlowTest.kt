package com.gasc.zoology.biostat

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.gasc.zoology.biostat.lab.TOOLS
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end UI checks run on an Android emulator in CI. */
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val rule = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before fun setUp() {
        ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("progress", Context.MODE_PRIVATE).edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitText("Course progress")
        device.executeShellCommand("mkdir -p /sdcard/Download/qa")
    }

    @After fun tearDown() { scenario.close() }

    private fun shot(name: String) { device.executeShellCommand("screencap -p /sdcard/Download/qa/$name.png") }
    /** CI emulators sometimes show a system "isn't responding" dialog that steals focus; press Wait so the test can continue. */
    private fun dismissSystemDialogs() {
        runCatching {
            if (device.currentPackageName == "android" || device.hasObject(By.textContains("isn't responding"))) {
                device.findObject(By.text("Wait"))?.click()
                device.findObject(By.text("WAIT"))?.click()
            }
        }
    }

    private fun fail(what: String, e: Throwable): Nothing {
        shot("FAIL_" + what.filter { it.isLetterOrDigit() }.take(30))
        val pkg = runCatching { device.currentPackageName }.getOrDefault("?")
        val dump = runCatching { java.io.ByteArrayOutputStream().also { device.dumpWindowHierarchy(it) }.toString().replace(Regex("<node[^>]*text=\"([^\"]*)\"[^>]*>"), "[$1]").replace(Regex("<[^>]*>"), "").take(1200) }.getOrDefault("?")
        val tree = "package=$pkg roots=${runCatching { rule.onAllNodes(isRoot()).fetchSemanticsNodes().size }.getOrDefault(-1)} dump=$dump"
        throw AssertionError("Waiting for $what failed: ${e.message}\n$tree", e)
    }
    private fun waitText(t: String, ms: Long = 20_000, substring: Boolean = false) =
        try { rule.waitUntil(ms) { dismissSystemDialogs(); rule.onAllNodesWithText(t, substring = substring).fetchSemanticsNodes().isNotEmpty() } } catch (e: Throwable) { fail("text '$t'", e) }
    private fun waitTag(t: String, ms: Long = 20_000) =
        try { rule.waitUntil(ms) { dismissSystemDialogs(); rule.onAllNodesWithTag(t).fetchSemanticsNodes().isNotEmpty() } } catch (e: Throwable) { fail("tag '$t'", e) }
    private fun tab(label: String) { rule.onNode(hasText(label) and hasClickAction()).performClick(); rule.waitForIdle() }
    private fun tag(t: String) { waitTag(t); val n = rule.onNodeWithTag(t); runCatching { n.performScrollTo() }; n.performClick(); rule.waitForIdle() }
    private fun toTamil() { rule.onNodeWithText("தமிழ்").performClick(); waitText("முகப்பு") }
    private fun toEnglish() { rule.onNodeWithText("English").performClick(); waitText("Home") }
    private fun back() { dismissSystemDialogs(); device.pressBack(); rule.waitForIdle() }

    @Test fun homeShowsProgressAndSyllabus() {
        waitText("0 of 44 lessons completed")
        rule.onNodeWithTag("home-scroll").performScrollToNode(hasText("Syllabus"))
        rule.onNodeWithText("Syllabus").assertExists()
        shot("01_home")
    }

    @Test fun lessonCompleteBookmarkAndNote() {
        tab("Lessons"); tag("lesson-U1_T01"); waitText("Learning objectives"); shot("02_lesson")
        tag("bookmark"); rule.onNodeWithText("★ Bookmarked").assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("note"))
        rule.onNodeWithTag("note").performTextInput("my note"); rule.onNodeWithTag("note").assertTextContains("my note")
        rule.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasTestTag("complete"))
        rule.onNodeWithTag("complete").performClick(); rule.onNodeWithText("Completed — tap to undo").assertExists()
    }

    @Test fun quizRunsToNextQuestion() {
        tab("Quiz"); tag("start-quiz"); tag("option-0"); waitTag("next-question"); shot("03_quiz"); tag("next-question")
        waitText("Question 2 of 10")
    }

    @Test fun flashcardsFlipAndMarkKnown() {
        tab("Cards"); waitTag("card"); rule.onNodeWithTag("card").performClick(); rule.onNodeWithText("Definition").assertExists()
        tag("known"); rule.onNodeWithText("Marked as known — undo").assertExists()
    }

    @Test fun decisionTreeLeadsToWelch() {
        tab("Lab"); tag("hub-tree"); tag("choice-1"); tag("choice-1"); tag("choice-0"); waitTag("leaf")
        rule.onNodeWithText("Welch independent-samples t-test").assertExists(); shot("04_tree")
    }

    @Test fun dataLabRunsGuidedAnalysis() {
        tab("Lab"); tag("hub-data"); tag("analyse-7"); waitTag("result"); rule.onNodeWithTag("result").assertExists(); shot("05_datalab")
    }

    @Test fun visualsRespondToTouch() {
        tab("Lab"); tag("hub-visuals"); waitText("Central limit theorem")
        rule.onNodeWithTag("sampling-canvas").performTouchInput { click(center) }
        rule.onNodeWithTag("sampling-text").assert(!hasText("Select a unit.")); shot("06_visuals")
    }

    @Test fun progressBackupScreenOffersExportImportReset() {
        tab("Lab"); tag("hub-progress"); waitTag("export"); rule.onNodeWithTag("import").assertExists(); rule.onNodeWithTag("reset").assertExists()
    }


    @Test fun malformedProgressImportPreservesSavedRecordsAndLegacyRoundTrip() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val c = courseContent()
        val id = c.lessons.first().id
        val card = c.flashcards.first().id
        val qid = c.questions.first().id
        val progress = ProgressStore(ctx)
        progress.toggleLesson(id)
        progress.toggleBookmark(id)
        progress.toggleKnown(card)
        progress.setNote(id, "குறிப்புகள் — sample note")
        progress.recordAnswer(qid, true)
        val expectedCompleted = progress.completed.toSet()
        val expectedBookmarks = progress.bookmarks.toSet()
        val expectedKnown = progress.known.toSet()
        val expectedNotes = progress.notes.toMap()
        val expectedRight = progress.answeredRight.toSet()
        val expectedAttempts = progress.quizAttempts
        val malformed = listOf(
            """{"completed":{}}""",
            """{"completed":[],"bookmarks":[],"known":[]}""",
            """{"completed":["INVALID"],"bookmarks":[],"known":[],"notes":{}}""",
            """{"completed":[],"bookmarks":[],"known":[],"notes":{"$id":3}}""",
            """{"completed":[],"bookmarks":[],"known":[],"notes":{},"answeredRight":[true]}""",
            """{"completed":[],"bookmarks":[],"known":[],"notes":{},"quizAttempts":"four"}"""
        )
        malformed.forEachIndexed { index, json ->
            assertTrue("malformed file $index accepted", progress.importJson(json, c).isFailure)
            assertEquals(expectedCompleted, progress.completed.toSet())
            assertEquals(expectedBookmarks, progress.bookmarks.toSet())
            assertEquals(expectedKnown, progress.known.toSet())
            assertEquals(expectedNotes, progress.notes.toMap())
            assertEquals(expectedRight, progress.answeredRight.toSet())
            assertEquals(expectedAttempts, progress.quizAttempts)
        }
        // Legacy v3.2 does not have the native-only answeredRight or quizAttempts fields.
        val legacy = """{"completed":["$id"],"bookmarks":["$id"],"known":["$card"],"notes":{"$id":"தமிழ் குறிப்பு"},"settings":{"tamil":true}}"""
        assertTrue(progress.importJson(legacy, c).isSuccess)
        assertEquals("தமிழ் குறிப்பு", progress.notes[id])
        assertTrue(progress.tamil)
        val exported = progress.exportJson()
        progress.reset()
        assertTrue(progress.importJson(exported, c).isSuccess)
        assertEquals("தமிழ் குறிப்பு", progress.notes[id])
        val reopened = ProgressStore(ctx)
        assertEquals(progress.completed.toSet(), reopened.completed.toSet())
        assertEquals(progress.bookmarks.toSet(), reopened.bookmarks.toSet())
        assertEquals(progress.known.toSet(), reopened.known.toSet())
        assertEquals(progress.notes.toMap(), reopened.notes.toMap())
        assertTrue(reopened.tamil)
    }

    private fun runEveryTool(tamil: Boolean) {
        tab(if (tamil) "ஆய்வகம்" else "Lab"); tag("hub-tools")
        for (t in TOOLS) {
          step {
            tag("tool-${t.id}"); tag("run"); waitTag("result")
            if (rule.onAllNodesWithTag("error").fetchSemanticsNodes().isNotEmpty()) fail("tool ${t.id} produced an error", AssertionError("error shown"))
            rule.onAllNodesWithTag("error").assertCountEquals(0)
            if (tamil) rule.onAllNodesWithText("Calculate").assertCountEquals(0)
            if (t.id == "ttest" || t.id == "corr") shot((if (tamil) "ta_" else "en_") + t.id)
            back()
          }
        }
    }

    @Test fun allEighteenToolsRunInEnglish() = runEveryTool(false)

    @Test fun allEighteenToolsRunInTamilWithoutEnglishLabels() { toTamil(); runEveryTool(true) }

    @Test fun badInputShowsLocalisedError() {
        tab("Lab"); tag("hub-tools"); tag("tool-descriptive")
        rule.onNodeWithTag("in-data").performTextReplacement("1, ?"); tag("run"); waitTag("error")
        rule.onNodeWithTag("error").assertTextContains("Non-numeric", substring = true)
        toTamil(); waitText("எண் அல்லாத உள்ளீடு", 10_000, substring = true)
    }

    @Test fun tamilModeShowsNoEnglishLessonHeadings() {
        toTamil(); tab("பாடங்கள்"); tag("lesson-U1_T01"); waitText("வரையறை")
        rule.onAllNodesWithText("Definition").assertCountEquals(0); rule.onAllNodesWithText("Learning objectives").assertCountEquals(0)
        rule.onAllNodesWithText("Introduction").assertCountEquals(0); shot("07_lesson_tamil")
        back(); tab("வினாடி வினா"); tag("start-quiz"); waitText("வினா 1 / 10")
        rule.onAllNodesWithText("Question 1 of 10").assertCountEquals(0)
        toEnglish(); waitText("Question 1 of 10")
    }


    @Test fun homeUnitExpandsIntoActualLessons() {
        rule.onNodeWithTag("home-scroll").performScrollToNode(hasTestTag("home-unit-toggle-1"))
        waitTag("home-lesson-U1_T01")
        tag("home-unit-toggle-1")
        rule.onAllNodesWithTag("home-lesson-U1_T01").assertCountEquals(0)
        tag("home-unit-toggle-1")
        waitTag("home-lesson-U1_T01")
        tag("home-lesson-U1_T01")
        waitTag("bookmark")
        shot("08_home_inline_lesson")
    }

    @Test fun textbookUnitCollapsesAndRestoresItsLessons() {
        tab("Lessons")
        waitTag("lesson-U1_T01")
        tag("textbook-unit-1")
        rule.onAllNodesWithTag("lesson-U1_T01").assertCountEquals(0)
        tag("textbook-unit-1")
        waitTag("lesson-U1_T01")
        tag("lesson-U1_T01")
        waitTag("bookmark")
        shot("09_textbook_inline_lesson")
    }


    @Test fun readerSectionsExpandWithoutLeavingThePage() {
        tab("Lessons")
        tag("lesson-U1_T01")
        rule.onAllNodes(hasScrollToNodeAction())[0]
            .performScrollToNode(hasTestTag("lesson-section-Definition"))
        tag("lesson-section-Definition")
        waitTag("section-body-Definition")
        shot("10_reader_section_open")
        tag("lesson-section-Definition")
        rule.onAllNodesWithTag("section-body-Definition").assertCountEquals(0)
    }

    private fun courseContent() = kotlinx.coroutines.runBlocking { ContentRepository.load(InstrumentationRegistry.getInstrumentation().targetContext) }

    /** CI emulators are slow and occasionally drop a frame (list scroll miss, Compose harness thread check). Retry one step up to 3 times,
     *  returning to the list screen first; a step that fails three times in a row still fails the test, so real regressions are not hidden. */
    private fun step(times: Int = 3, block: () -> Unit) {
        var last: Throwable? = null
        repeat(times) {
            try { block(); return } catch (e: Throwable) {
                if (e !is AssertionError && e !is IllegalArgumentException) throw e
                last = e; runCatching { rule.waitForIdle() }
                repeat(2) { if (listOf("bookmark", "run", "leaf").any { t -> rule.onAllNodesWithTag(t).fetchSemanticsNodes().isNotEmpty() }) back() }
            }
        }
        throw last!!
    }

    private fun openEveryLesson(tamil: Boolean) {
        val c = courseContent()
        tab(if (tamil) "பாடங்கள்" else "Lessons")
        for (l in c.lessons) {
          step {
            rule.waitUntil(20000) { rule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().isNotEmpty() }
            rule.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasTestTag("lesson-${l.id}"))
            tag("lesson-${l.id}")
            waitTag("bookmark")
            val title = if (tamil) c.tamilLessons.getValue(l.id).title else l.title
            rule.onAllNodesWithText(title).assertCountEquals(1)
            rule.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasTestTag("complete"))
            rule.onNodeWithTag("complete").assertExists()
            back()
          }
        }
    }

    @Test fun privacyPolicyIsInAppInBothLanguages() {
        tab("Lab"); tag("hub-privacy"); waitText("Privacy Policy"); waitText("Information we collect")
        rule.onAllNodesWithText("தனியுரிமைக் கொள்கை").assertCountEquals(0)
        back(); toTamil(); tab("ஆய்வகம்"); tag("hub-privacy"); waitText("தனியுரிமைக் கொள்கை")
        rule.onAllNodesWithText("Information we collect").assertCountEquals(0)
    }

    @Test fun all44LessonsOpenInEnglish() = openEveryLesson(false)

    @Test fun all44LessonsOpenInTamil() { toTamil(); openEveryLesson(true) }

    private fun flipEveryCard(tamil: Boolean) {
        val c = courseContent()
        tab(if (tamil) "அட்டைகள்" else "Cards")
        for (i in c.flashcards.indices) {
            waitTag("card")
            rule.onNodeWithTag("card").performClick()
            rule.onNodeWithText(if (tamil) "வரையறை" else "Definition").assertExists()
            if (i < c.flashcards.size - 1) rule.onNodeWithText(if (tamil) "அடுத்தது →" else "Next →").performClick()
        }
    }

    @Test fun all50FlashcardsFlipInEnglish() = flipEveryCard(false)

    @Test fun all50FlashcardsFlipInTamil() { toTamil(); flipEveryCard(true) }
}
