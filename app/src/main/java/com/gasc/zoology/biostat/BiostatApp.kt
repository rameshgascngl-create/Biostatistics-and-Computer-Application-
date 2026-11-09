package com.gasc.zoology.biostat

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiostatApp(content: CourseContent, store: ProgressStore) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val ta = store.tamil
    val tabs = listOf(
        Tab("home", if (ta) "முகப்பு" else "Home", Icons.Filled.Home),
        Tab("lessons", if (ta) "பாடங்கள்" else "Lessons", Icons.AutoMirrored.Filled.MenuBook),
        Tab("quiz", if (ta) "வினாடி வினா" else "Quiz", Icons.Filled.Quiz),
        Tab("cards", if (ta) "அட்டைகள்" else "Cards", Icons.Filled.Style),
        Tab("lab", if (ta) "ஆய்வகம்" else "Lab", Icons.Filled.Science)
    )
    CompositionLocalProvider(LocalTamil provides ta) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ta) "உயிரிப் புள்ளியியல்" else "Biostatistics Mastery") },
                actions = {
                    FilterChip(
                        selected = store.tamil,
                        onClick = { store.toggleTamil() },
                        label = { Text(if (ta) "English" else "தமிழ்") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    val selected = route == t.route || (t.route == "lessons" && route.startsWith("lesson/")) ||
                        (t.route == "lab" && route.startsWith("lab/"))
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(content, store, onOpenLesson = { nav.navigate("lesson/$it") },
                    onOpenLessons = { nav.navigate("lessons") })
            }
            composable("lessons") {
                LessonListScreen(content, store, onOpen = { nav.navigate("lesson/$it") })
            }
            composable("lesson/{id}") { b ->
                val id = b.arguments?.getString("id").orEmpty()
                val lesson = content.lesson(id)
                if (lesson != null) {
                    LessonScreen(content, lesson, store, onOpen = { nav.navigate("lesson/$it") {
                        popUpTo("lessons") } })
                }
            }
            composable("quiz") { QuizScreen(content, store) }
            composable("cards") { FlashcardScreen(content, store) }
            composable("lab") { LabHubScreen(onOpen = { nav.navigate(it) }) }
            composable("lab/tools") { com.gasc.zoology.biostat.lab.ToolListScreen(onOpen = { nav.navigate("lab/tool/$it") }) }
            composable("lab/tree") { com.gasc.zoology.biostat.lab.DecisionTreeScreen(onOpenTool = { nav.navigate("lab/tool/$it") }) }
            composable("lab/visuals") { com.gasc.zoology.biostat.lab.VisualsScreen() }
            composable("lab/data") { com.gasc.zoology.biostat.lab.DataLabScreen(onOpenLesson = { nav.navigate("lesson/$it") }) }
            composable("lab/progress") { ProgressScreen(content, store) }
            composable("lab/tool/{id}") { b -> com.gasc.zoology.biostat.lab.ToolScreen(b.arguments?.getString("id").orEmpty()) }
        }
    }
    }
}
