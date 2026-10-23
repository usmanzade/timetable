package com.usmanzade.timetable.ui.add

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.Division
import com.usmanzade.timetable.data.EducatorInfo
import com.usmanzade.timetable.data.GroupInfo
import com.usmanzade.timetable.data.SpbuApi
import com.usmanzade.timetable.data.StudyLevel
import com.usmanzade.timetable.data.StudyProgram
import com.usmanzade.timetable.data.StudyYear
import com.usmanzade.timetable.data.Subject
import com.usmanzade.timetable.ui.LocalTr
import com.usmanzade.timetable.work.Scheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private sealed interface GStep {
    data object Divisions : GStep
    data class Programs(val div: Division) : GStep
    data class Years(val div: Division, val program: StudyProgram) : GStep
    data class Groups(val div: Division, val program: StudyProgram, val year: StudyYear) : GStep
}

private sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val value: T) : Load<T>
    data class Err(val message: String) : Load<Nothing>
}

@Composable
fun AddSubjectScreen(onClose: () -> Unit, onAdded: () -> Unit) {
    val tr = LocalTr.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var tabIndex by remember { mutableIntStateOf(0) }
    var step by remember { mutableStateOf<GStep>(GStep.Divisions) }

    fun parentStep(s: GStep): GStep? = when (s) {
        GStep.Divisions -> null
        is GStep.Programs -> GStep.Divisions
        is GStep.Years -> GStep.Programs(s.div)
        is GStep.Groups -> GStep.Years(s.div, s.program)
    }

    BackHandler {
        val parent = if (tabIndex == 0) parentStep(step) else null
        if (parent != null) step = parent else onClose()
    }

    fun add(subject: Subject) {
        Graph.settings.addFavorite(subject, makeActive = true)
        Scheduler.apply(context)
        onAdded()
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        SmallTopAppBar(
            title = tr("Добавить расписание", "Add timetable"),
            navigationIcon = {
                IconButton(onClick = {
                    val parent = if (tabIndex == 0) parentStep(step) else null
                    if (parent != null) step = parent else onClose()
                }) {
                    Icon(MiuixIcons.Back, contentDescription = tr("Назад", "Back"), tint = MiuixTheme.colorScheme.onSurface)
                }
            },
        )
        TabRowWithContour(
            tabs = listOf(tr("Группы", "Groups"), tr("Преподаватели", "Educators")),
            selectedTabIndex = tabIndex,
            onTabSelected = { tabIndex = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        )
        AnimatedContent(
            targetState = tabIndex,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(260)) { it / 4 * dir } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 4 * dir } + fadeOut(tween(120)))
            },
            label = "addTab",
        ) { t ->
            if (t == 0) GroupFlow(step = step, onStep = { step = it }, onPick = { g -> add(Subject(Subject.Kind.GROUP, g.id.toString(), g.name)) })
            else EducatorSearch(onPick = { e -> add(Subject(Subject.Kind.EDUCATOR, e.id.toString(), e.name)) })
        }
    }
}

@Composable
private fun <T> Loader(
    key: Any,
    fetch: suspend () -> T,
    content: @Composable (T) -> Unit,
) {
    val tr = LocalTr.current
    var state by remember(key) { mutableStateOf<Load<T>>(Load.Loading) }
    var attempt by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(key, attempt) {
        state = Load.Loading
        state = try {
            Load.Ok(fetch())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Load.Err(e.message ?: "error")
        }
    }
    when (val s = state) {
        Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
        }
        is Load.Err -> Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(tr("Не удалось загрузить данные", "Couldn't load data"), fontWeight = FontWeight.SemiBold)
            Text(s.message, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(Modifier.height(14.dp))
            Button(onClick = { attempt++ }) { Text(tr("Повторить", "Retry")) }
        }
        is Load.Ok -> content(s.value)
    }
}

@Composable
private fun GroupFlow(step: GStep, onStep: (GStep) -> Unit, onPick: (GroupInfo) -> Unit) {
    val tr = LocalTr.current
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val forward = depth(targetState) >= depth(initialState)
            val dir = if (forward) 1 else -1
            (slideInHorizontally(tween(280)) { it / 3 * dir } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(220)) { -it / 3 * dir } + fadeOut(tween(120)))
        },
        label = "groupStep",
    ) { s ->
        when (s) {
            GStep.Divisions -> Loader(key = "divisions", fetch = { SpbuApi.divisions() }) { list ->
                ListScreen(title = tr("Факультет / институт", "Faculty / institute")) {
                    items(list) { d ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), cornerRadius = 16.dp) {
                            ArrowPreference(title = d.name, onClick = { onStep(GStep.Programs(d)) })
                        }
                    }
                }
            }
            is GStep.Programs -> Loader(key = s.div.alias, fetch = { SpbuApi.levels(s.div.alias) }) { levels: List<StudyLevel> ->
                ListScreen(title = s.div.name) {
                    levels.forEach { level ->
                        item { SmallTitle(level.name) }
                        items(level.programs) { p ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), cornerRadius = 16.dp) {
                                ArrowPreference(
                                    title = p.name,
                                    onClick = { onStep(GStep.Years(s.div, p)) },
                                )
                            }
                        }
                    }
                }
            }
            is GStep.Years -> ListScreen(title = s.program.name) {
                items(s.program.years.filter { !it.empty }) { y ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), cornerRadius = 16.dp) {
                        ArrowPreference(
                            title = y.name,
                            onClick = { onStep(GStep.Groups(s.div, s.program, y)) },
                        )
                    }
                }
            }
            is GStep.Groups -> Loader(key = s.year.programId, fetch = { SpbuApi.groups(s.year.programId) }) { groups ->
                ListScreen(title = "${s.program.name} · ${s.year.name}") {
                    items(groups) { g ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), cornerRadius = 16.dp) {
                            ArrowPreference(
                                title = g.name,
                                summary = g.form.ifBlank { null },
                                onClick = { onPick(g) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun depth(s: GStep): Int = when (s) {
    GStep.Divisions -> 0
    is GStep.Programs -> 1
    is GStep.Years -> 2
    is GStep.Groups -> 3
}

@Composable
private fun ListScreen(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 2,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            content()
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun EducatorSearch(onPick: (EducatorInfo) -> Unit) {
    val tr = LocalTr.current
    val state = rememberTextFieldState()
    var results by remember { mutableStateOf<Load<List<EducatorInfo>>?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshotFlow { state.text.toString().trim() }.collectLatest { q ->
            if (q.length < 2) {
                results = null
                return@collectLatest
            }
            delay(400)
            results = Load.Loading
            results = try {
                Load.Ok(SpbuApi.searchEducators(q))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Err(e.message ?: "error")
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TextField(
            state = state,
            label = tr("Фамилия преподавателя", "Educator's last name"),
            useLabelAsPlaceholder = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when (val r = results) {
            null -> Text(
                tr("Введите минимум 2 буквы фамилии", "Type at least 2 letters of the last name"),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
            Load.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
            }
            is Load.Err -> Text(
                tr("Не удалось выполнить поиск: ", "Search failed: ") + r.message,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            is Load.Ok -> if (r.value.isEmpty()) {
                Text(
                    tr("Ничего не найдено", "Nothing found"),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(r.value) { e ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), cornerRadius = 16.dp) {
                            ArrowPreference(title = e.name, onClick = { scope.launch { onPick(e) } })
                        }
                    }
                }
            }
        }
    }
}
