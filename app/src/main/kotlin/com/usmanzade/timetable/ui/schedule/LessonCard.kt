package com.usmanzade.timetable.ui.schedule

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.data.Lesson
import com.usmanzade.timetable.ui.Chip
import com.usmanzade.timetable.ui.Green
import com.usmanzade.timetable.ui.LocalTr
import com.usmanzade.timetable.ui.Orange
import com.usmanzade.timetable.ui.Red
import com.usmanzade.timetable.ui.categoryColor
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val HM = DateTimeFormatter.ofPattern("HH:mm")

val LocalEntrance = compositionLocalOf { 0 }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonCard(
    lesson: Lesson,
    index: Int,
    now: LocalDateTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tr = LocalTr.current
    val colors = MiuixTheme.colorScheme
    val accent = if (lesson.cancelled) Red else categoryColor(lesson.category)
    val running = !lesson.cancelled && !lesson.allDay && now >= lesson.startDt && now <= lesson.endDt

    val entrance = LocalEntrance.current
    val appear = remember { Animatable(if (entrance == 0) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (entrance != 0) {
            delay((index * 70L).coerceAtMost(350L))
            appear.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .graphicsLayer {
                alpha = appear.value
                translationY = entrance * (1f - appear.value) * 26.dp.toPx()
            },
        cornerRadius = 20.dp,
        onClick = onClick,
        showIndication = true,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(start = 8.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Column(
                Modifier.width(64.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (lesson.allDay) {
                    Text(
                        tr("Весь день", "All day"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(lesson.startDt.format(HM), fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(
                        lesson.endDt.format(HM),
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = lesson.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (lesson.cancelled) TextDecoration.LineThrough else null,
                    color = if (lesson.cancelled) colors.onSurfaceVariantSummary else colors.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    lesson.type?.let { Chip(it.replaceFirstChar { c -> c.uppercase() }, accent) }
                    if (running) Chip(tr("Идёт сейчас", "In progress"), Green)
                    if (lesson.cancelled) Chip(tr("Отменено", "Cancelled"), Red)
                    if (lesson.timeChanged || lesson.locationChanged || lesson.educatorChanged) {
                        Chip(tr("Есть изменения", "Changed"), Orange)
                    }
                }
            }
        }
    }
}
