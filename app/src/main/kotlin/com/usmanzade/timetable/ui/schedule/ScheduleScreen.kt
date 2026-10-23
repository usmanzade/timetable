package com.usmanzade.timetable.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.usmanzade.timetable.ui.locale
import com.usmanzade.timetable.ui.isTwoPane
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.CalendarMode
import com.usmanzade.timetable.data.Lesson
import com.usmanzade.timetable.data.Subject
import com.usmanzade.timetable.data.weekMonday
import com.usmanzade.timetable.ui.LocalTr
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Weeks
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private const val PAGE_HALF = 3650
private const val PAGE_COUNT = PAGE_HALF * 2 + 1

@Composable
fun ScheduleScreen(bottomInset: androidx.compose.ui.unit.Dp, onAdd: () -> Unit) {
    val settings = Graph.settings
    val favorites by settings.favorites.collectAsState()
    val activeKey by settings.activeKey.collectAsState()
    val subject = favorites.firstOrNull { it.key == activeKey } ?: favorites.firstOrNull()

    var refreshTick by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize()) {
        ScheduleTopBar(
            showToday = selected != today.toString(),
            onToday = { selected = today.toString() },
            onRefresh = { refreshTick++ },
        )
        if (subject == null) {
            EmptyWelcome(onAdd)
        } else {
            FavoriteChips(
                favorites = favorites,
                activeKey = subject.key,
                onPick = { settings.setActive(it) },
                onAdd = onAdd,
            )
            ScheduleBody(
                subject = subject,
                selected = selected,
                onSelected = { selected = it },
                refreshTick = refreshTick,
                bottomInset = bottomInset,
            )
        }
    }
}

@Composable
fun EducatorPreviewScreen(subject: Subject, onClose: () -> Unit, bottomInset: androidx.compose.ui.unit.Dp) {
    val tr = LocalTr.current
    val settings = Graph.settings
    val context = androidx.compose.ui.platform.LocalContext.current
    val favorites by settings.favorites.collectAsState()
    val saved = favorites.firstOrNull { it.key == subject.key }
    val shown = saved ?: subject

    var refreshTick by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize()) {
        PreviewTopBar(
            title = shown.displayName,
            subtitle = tr("Преподаватель", "Educator"),
            isFavorite = saved != null,
            showToday = selected != today.toString(),
            onBack = onClose,
            onToday = { selected = today.toString() },
            onToggleFavorite = {
                if (saved != null) {
                    settings.removeFavorite(subject.key)
                    Graph.repo.forget(subject)
                } else {
                    settings.addFavorite(subject, makeActive = false)
                }
                com.usmanzade.timetable.work.Scheduler.apply(context)
            },
            onRefresh = { refreshTick++ },
        )
        ScheduleBody(
            subject = shown,
            selected = selected,
            onSelected = { selected = it },
            refreshTick = refreshTick,
            bottomInset = bottomInset,
        )
    }
}

@Composable
fun ColumnScope.ScheduleBody(
    subject: Subject,
    selected: String,
    onSelected: (String) -> Unit,
    refreshTick: Int,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    val tr = LocalTr.current
    val settings = Graph.settings
    val mode by settings.calendarMode.collectAsState()
    val selectedDate = LocalDate.parse(selected)

    val holder = remember(subject.key) { ScheduleHolder(subject) }
    val collapse = remember { CollapseState() }
    var detail by remember { mutableStateOf<Lesson?>(null) }
    var detailShown by remember { mutableStateOf(false) }

    val tablet = isTwoPane()
    val needed = remember(selectedDate, mode, tablet) {
        when (if (tablet) CalendarMode.MONTH else mode) {
            CalendarMode.MONTH -> {
                val ym = YearMonth.from(selectedDate)
                val start = ym.atDay(1).weekMonday()
                val end = ym.atEndOfMonth().weekMonday()
                generateSequence(start) { it.plusWeeks(1) }.takeWhile { it <= end }.toList()
            }
            else -> {
                val m = selectedDate.weekMonday()
                listOf(m.minusWeeks(1), m, m.plusWeeks(1))
            }
        }
    }
    LaunchedEffect(holder, needed) { holder.ensure(needed) }
    LaunchedEffect(refreshTick) { if (refreshTick > 0) holder.ensure(needed, force = true) }

    AnimatedVisibility(visible = holder.offline, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Text(
            text = tr("Нет связи с сервером. Показано сохранённое расписание.", "Can't reach the server. Showing saved timetable."),
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }

    if (tablet) {

        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
            Column(Modifier.weight(2f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                CalendarPanel(
                    mode = CalendarMode.MONTH,
                    selected = selectedDate,
                    countFor = { holder.countOn(it) },
                    onSelect = { onSelected(it.toString()) },
                    onModeChange = {},
                    collapse = collapse,
                    large = true,
                    showTabs = false,
                )
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 4.dp)) {
                DayHeader(selectedDate, holder.countOn(selectedDate))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LessonsPager(
                        selected = selectedDate,
                        onSelect = { onSelected(it.toString()) },
                        holder = holder,
                        bottomInset = bottomInset,
                        onLessonClick = {
                            detail = it
                            detailShown = true
                        },
                    )
                }
            }
        }
    } else {

        CalendarPanel(
            mode = mode,
            selected = selectedDate,
            countFor = { holder.countOn(it) },
            onSelect = { onSelected(it.toString()) },
            onModeChange = {
                settings.setCalendarMode(it)
                collapse.reset()
            },
            collapse = collapse,
        )

        val connection = remember(mode) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (mode != CalendarMode.MONTH || available.y >= 0f) return Offset.Zero
                    val used = collapse.dragBy(-available.y)
                    return Offset(0f, -used)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (mode != CalendarMode.MONTH || available.y <= 0f) return Offset.Zero
                    val used = collapse.dragBy(-available.y)
                    return Offset(0f, -used)
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    collapse.settle()
                    return Velocity.Zero
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().nestedScroll(connection)) {
            LessonsPager(
                selected = selectedDate,
                onSelect = { onSelected(it.toString()) },
                holder = holder,
                bottomInset = bottomInset,
                onLessonClick = {
                    detail = it
                    detailShown = true
                },
            )
        }
    }

    LessonDetailsDialog(show = detailShown, lesson = detail, onDismiss = { detailShown = false })
}

@Composable
private fun DayHeader(date: LocalDate, count: Int) {
    val tr = LocalTr.current
    val title = date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", tr.locale()))
        .replaceFirstChar { it.uppercase() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(title, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            text = if (count == 0) tr("Занятий нет", "No classes")
            else tr("Занятий: $count", if (count == 1) "1 class" else "$count classes"),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun FavoriteChips(
    favorites: List<Subject>,
    activeKey: String,
    onPick: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        items(favorites, key = { it.key }) { s ->
            val sel = s.key == activeKey
            val bg by animateColorAsState(if (sel) colors.primary else colors.surfaceContainer, tween(250), label = "chipBg")
            val fg by animateColorAsState(if (sel) colors.onPrimary else colors.onSurface, tween(250), label = "chipFg")
            Text(
                text = s.displayName,
                color = fg,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(s.key) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
        item {
            Text(
                text = "+",
                color = colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceContainer)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAdd() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun LessonsPager(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    holder: ScheduleHolder,
    bottomInset: androidx.compose.ui.unit.Dp,
    onLessonClick: (Lesson) -> Unit,
) {
    val base = remember { LocalDate.now().minusDays(PAGE_HALF.toLong()) }
    fun pageOf(d: LocalDate) = (d.toEpochDay() - base.toEpochDay()).toInt().coerceIn(0, PAGE_COUNT - 1)

    val pager = rememberPagerState(initialPage = pageOf(selected), pageCount = { PAGE_COUNT })

    val enter = remember { Animatable(1f) }
    var direction by remember { mutableIntStateOf(0) }
    LaunchedEffect(selected) {
        val target = pageOf(selected)
        if (pager.currentPage != target) {
            direction = if (target > pager.currentPage) 1 else -1
            pager.scrollToPage(target)
            enter.snapTo(0f)
            enter.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            direction = 0
        }
    }

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val d = base.plusDays(page.toLong())
            if (d != selected) onSelect(d)
        }
    }

    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalDateTime.now()
        }
    }

    CompositionLocalProvider(LocalEntrance provides direction) {
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val e = enter.value
                    alpha = 0.2f + 0.8f * e
                    translationY = direction * (1f - e) * 36.dp.toPx()
                },
        ) { page ->
            val date = base.plusDays(page.toLong())
            DayLessons(date, holder, now, bottomInset, onLessonClick)
        }
    }
}

@Composable
private fun DayLessons(
    date: LocalDate,
    holder: ScheduleHolder,
    now: LocalDateTime,
    bottomInset: androidx.compose.ui.unit.Dp,
    onLessonClick: (Lesson) -> Unit,
) {
    val tr = LocalTr.current
    val lessons = holder.lessonsOn(date)

    if (lessons.isEmpty()) {
        Box(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottomInset),
            contentAlignment = Alignment.Center,
        ) {
            if (!holder.isLoaded(date) && holder.loading > 0) {
                InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        MiuixIcons.Weeks,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = tr("Занятий нет", "No classes"),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 15.sp,
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = bottomInset + 12.dp),
    ) {
        items(lessons.size, key = { "${it}_${lessons[it].start}" }) { i ->
            LessonCard(lessons[i], index = i, now = now, onClick = { onLessonClick(lessons[i]) })
        }
    }
}

@Composable
private fun EmptyWelcome(onAdd: () -> Unit) {
    val tr = LocalTr.current
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            MiuixIcons.Weeks,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            tr("Выберите группу", "Pick your group"),
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            tr(
                "Найдите свою группу или преподавателя — расписание сохранится и будет доступно офлайн.",
                "Find your group or an educator — the timetable is saved and works offline.",
            ),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColorsPrimary()) {
            Text(tr("Выбрать", "Choose"), color = MiuixTheme.colorScheme.onPrimary)
        }
    }
}
