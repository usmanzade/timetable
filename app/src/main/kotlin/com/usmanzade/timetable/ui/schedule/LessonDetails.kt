package com.usmanzade.timetable.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.data.Lesson
import com.usmanzade.timetable.ui.Chip
import com.usmanzade.timetable.ui.LocalEducatorNav
import com.usmanzade.timetable.ui.LocalTr
import com.usmanzade.timetable.ui.Orange
import com.usmanzade.timetable.ui.Red
import com.usmanzade.timetable.ui.categoryColor
import com.usmanzade.timetable.ui.isMappable
import com.usmanzade.timetable.ui.locale
import com.usmanzade.timetable.ui.openMap
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonDetailsDialog(show: Boolean, lesson: Lesson?, onDismiss: () -> Unit) {
    val tr = LocalTr.current
    val context = LocalContext.current
    val nav = LocalEducatorNav.current
    val l = lesson
    OverlayDialog(
        show = show && l != null,
        title = l?.title,
        onDismissRequest = onDismiss,
    ) {
        if (l == null) return@OverlayDialog
        val accent = categoryColor(l.category)
        val hm = DateTimeFormatter.ofPattern("HH:mm")
        val dateText = l.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", tr.locale()))
            .replaceFirstChar { it.uppercase() }
        val time = if (l.allDay) tr("Весь день", "All day") else "${l.startDt.format(hm)} – ${l.endDt.format(hm)}"
        val places = l.locations.split(";").map { it.trim() }.filter { it.isNotBlank() }
        val fallbackNames = l.educators.split(Regex(";|,(?=\\s*\\p{Lu})")).map { it.trim() }.filter { it.isNotBlank() }

        Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                l.type?.let { Chip(it.replaceFirstChar { c -> c.uppercase() }, accent) }
                if (l.cancelled) Chip(tr("Отменено", "Cancelled"), Red)
                if (l.timeChanged) Chip(tr("Время изменено", "Time changed"), Orange)
                if (l.locationChanged) Chip(tr("Другая аудитория", "Room changed"), Orange)
                if (l.educatorChanged) Chip(tr("Замена преподавателя", "Teacher changed"), Orange)
            }
            InfoRow(tr("Дата", "Date"), dateText)
            InfoRow(tr("Время", "Time"), time)

            if (places.isNotEmpty()) {
                Section(tr("Место и формат", "Place and format")) {
                    places.forEach { place ->
                        if (isMappable(place)) LinkText(place) { openMap(context, place) } else ValueText(place)
                    }
                }
            }

            if (l.educators.isNotBlank() || l.educatorRefs.isNotEmpty()) {
                Section(tr("Преподаватель", "Educator")) {
                    val names = if (l.educatorRefs.isNotEmpty()) {
                        l.educatorRefs.map { it.name.ifBlank { l.educators } }
                    } else {
                        fallbackNames
                    }
                    names.forEach { name ->
                        LinkText(name) {
                            onDismiss()
                            nav?.open(name, l)
                        }
                    }
                }
            }

            if (l.groups.isNotBlank()) InfoRow(tr("Группы", "Groups"), l.groups)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Закрыть", "Close"), color = MiuixTheme.colorScheme.onPrimary) }
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(label, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Section(label) { ValueText(value) }
}

@Composable
private fun ValueText(value: String) {
    Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun LinkText(value: String, onClick: () -> Unit) {
    Text(
        text = value,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.padding(vertical = 3.dp).clickable(onClick = onClick),
    )
}
