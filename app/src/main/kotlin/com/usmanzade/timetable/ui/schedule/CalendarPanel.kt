package com.usmanzade.timetable.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.data.CalendarMode
import com.usmanzade.timetable.data.weekMonday
import com.usmanzade.timetable.ui.LocalTr
import com.usmanzade.timetable.ui.locale
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Stable
class CollapseState {
    var progress by mutableFloatStateOf(0f)

    var rangePx: Float = 1f

    fun reset() {
        progress = 0f
    }

    fun dragBy(pixels: Float): Float {
        if (rangePx <= 1f) return 0f
        val next = (progress + pixels / rangePx).coerceIn(0f, 1f)
        val used = (next - progress) * rangePx
        progress = next
        return used
    }

    suspend fun settle() {
        val target = if (progress > 0.5f) 1f else 0f
        if (progress == target) return
        androidx.compose.animation.core.animate(
            initialValue = progress,
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
        ) { v, _ -> progress = v }
    }
}

@Composable
fun CalendarPanel(
    mode: CalendarMode,
    selected: LocalDate,
    countFor: (LocalDate) -> Int,
    onSelect: (LocalDate) -> Unit,
    onModeChange: (CalendarMode) -> Unit,
    collapse: CollapseState,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    showTabs: Boolean = true,
) {
    val tr = LocalTr.current
    val locale = tr.locale()
    val today = LocalDate.now()
    val scope = rememberCoroutineScope()

    fun shift(dir: Int) {
        val weekly = mode == CalendarMode.WEEK || (mode == CalendarMode.MONTH && collapse.progress > 0.5f)
        onSelect(
            when {
                mode == CalendarMode.DAY -> selected.plusDays(dir.toLong())
                weekly -> selected.plusWeeks(dir.toLong())
                else -> selected.plusMonths(dir.toLong())
            },
        )
    }

    val header = when (mode) {
        CalendarMode.MONTH -> selected.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        CalendarMode.WEEK -> {
            val m = selected.weekMonday()
            val e = m.plusDays(6)
            val f = DateTimeFormatter.ofPattern("d MMM", locale)
            "${m.format(f)} – ${e.format(f)}"
        }
        CalendarMode.DAY -> selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
    }.replaceFirstChar { it.uppercase() }

    Column(modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(start = 4.dp)) {
                AnimatedContent(
                    targetState = header,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                    label = "header",
                ) { text ->
                    Text(text = text, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
            IconButton(onClick = { shift(-1) }) {
                Icon(MiuixIcons.ChevronBackward, contentDescription = null, tint = MiuixTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { shift(1) }) {
                Icon(MiuixIcons.ChevronForward, contentDescription = null, tint = MiuixTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(6.dp))
        if (showTabs) {
            TabRowWithContour(
                tabs = listOf(tr("День", "Day"), tr("Неделя", "Week"), tr("Месяц", "Month")),
                selectedTabIndex = mode.ordinal,
                onTabSelected = { onModeChange(CalendarMode.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(8.dp))
        if (mode != CalendarMode.DAY) WeekdayLabels()

        Box(
            Modifier
                .fillMaxWidth()
                .pointerInput(mode, showTabs) {
                    if (mode != CalendarMode.MONTH || !showTabs) return@pointerInput
                    detectVerticalDragGestures(
                        onDragEnd = { scope.launch { collapse.settle() } },
                        onDragCancel = { scope.launch { collapse.settle() } },
                    ) { _, amount -> collapse.dragBy(-amount) }
                }
                .pointerInput(mode, selected) {
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = {
                            if (dx > 110f) shift(-1) else if (dx < -110f) shift(1)
                        },
                    ) { _, amount -> dx += amount }
                },
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
                label = "mode",
            ) { m ->
                when (m) {
                    CalendarMode.MONTH -> MonthGrid(selected, today, countFor, onSelect, collapse, large)
                    CalendarMode.WEEK -> WeekRow(selected, today, countFor, onSelect, large)
                    CalendarMode.DAY -> DayHero(selected, countFor(selected))
                }
            }
        }
    }
}

@Composable
private fun WeekdayLabels() {
    val tr = LocalTr.current
    val days = java.time.DayOfWeek.entries
    Row(Modifier.fillMaxWidth()) {
        days.forEach { d ->
            Text(
                text = d.getDisplayName(TextStyle.SHORT, tr.locale()).replaceFirstChar { it.uppercase() },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    selected: LocalDate,
    today: LocalDate,
    countFor: (LocalDate) -> Int,
    onSelect: (LocalDate) -> Unit,
    collapse: CollapseState,
    large: Boolean,
) {
    AnimatedContent(
        targetState = YearMonth.from(selected),
        transitionSpec = {
            val dir = if (targetState > initialState) 1 else -1
            (slideInHorizontally(animationSpec = tween(300)) { it / 4 * dir } + fadeIn(tween(240))) togetherWith
                (slideOutHorizontally(animationSpec = tween(240)) { -it / 4 * dir } + fadeOut(tween(140)))
        },
        label = "month",
    ) { ym ->
        val first = ym.atDay(1)
        val gridStart = first.weekMonday()
        val offset = first.dayOfWeek.value - 1
        val rows = (offset + ym.lengthOfMonth() + 6) / 7

        val selRow = if (YearMonth.from(selected) == ym) (selected.dayOfMonth - 1 + offset) / 7 else 0

        Column(
            Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val rowH = placeable.height / rows
                    collapse.rangePx = (placeable.height - rowH).toFloat()
                    val p = collapse.progress
                    val height = (placeable.height - (placeable.height - rowH) * p).toInt()
                    val dy = -(selRow * rowH * p).toInt()
                    layout(placeable.width, height) { placeable.placeRelative(0, dy) }
                },
        ) {
            repeat(rows) { r ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { c ->
                        val d = gridStart.plusDays((r * 7 + c).toLong())
                        if (YearMonth.from(d) == ym) {
                            DayCell(
                                date = d,
                                selected = d == selected,
                                today = d == today,
                                count = countFor(d),
                                onClick = { onSelect(d) },
                                modifier = Modifier.weight(1f),
                                large = large,
                            )
                        } else {
                            Spacer(Modifier.weight(1f).height(if (large) 64.dp else 46.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekRow(
    selected: LocalDate,
    today: LocalDate,
    countFor: (LocalDate) -> Int,
    onSelect: (LocalDate) -> Unit,
    large: Boolean,
) {
    AnimatedContent(
        targetState = selected.weekMonday(),
        transitionSpec = {
            val dir = if (targetState > initialState) 1 else -1
            (slideInHorizontally(animationSpec = tween(300)) { it / 4 * dir } + fadeIn(tween(240))) togetherWith
                (slideOutHorizontally(animationSpec = tween(240)) { -it / 4 * dir } + fadeOut(tween(140)))
        },
        label = "week",
    ) { monday ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { c ->
                val d = monday.plusDays(c.toLong())
                DayCell(
                    date = d,
                    selected = d == selected,
                    today = d == today,
                    count = countFor(d),
                    onClick = { onSelect(d) },
                    modifier = Modifier.weight(1f),
                    large = large,
                )
            }
        }
    }
}

@Composable
private fun DayHero(selected: LocalDate, count: Int) {
    val tr = LocalTr.current
    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            val dir = if (targetState > initialState) 1 else -1
            (slideInHorizontally(animationSpec = tween(280)) { it / 3 * dir } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(animationSpec = tween(220)) { -it / 3 * dir } + fadeOut(tween(120)))
        },
        label = "day",
    ) { d ->
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = d.dayOfMonth.toString(),
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.primary,
            )
            Text(
                text = if (count == 0) tr("Занятий нет", "No classes")
                else tr("Занятий: $count", if (count == 1) "1 class" else "$count classes"),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    val hasLessons = count > 0
    val bg by animateColorAsState(if (selected) colors.primary else Color.Transparent, tween(260), label = "bg")
    val fg by animateColorAsState(
        when {
            selected -> colors.onPrimary
            hasLessons -> colors.primary
            else -> colors.onSurface
        },
        tween(200),
        label = "fg",
    )
    val scale by animateFloatAsState(
        if (selected) 1f else 0.92f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale",
    )

    Box(
        modifier
            .height(if (large) 64.dp else 46.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (large) 52.dp else 38.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(bg)
                .then(if (today && !selected) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = fg,
                fontSize = if (large) 20.sp else 16.sp,
                fontWeight = if (selected || today || hasLessons) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
