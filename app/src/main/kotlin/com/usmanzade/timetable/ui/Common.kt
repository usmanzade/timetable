package com.usmanzade.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.Tr
import com.usmanzade.timetable.data.LessonCategory
import top.yukonga.miuix.kmp.basic.Text
import java.util.Locale

val LocalTr = staticCompositionLocalOf { Tr(true) }

class EducatorNav(val open: (name: String, lesson: com.usmanzade.timetable.data.Lesson?) -> Unit)

val LocalEducatorNav = staticCompositionLocalOf<EducatorNav?> { null }

fun Tr.locale(): Locale = if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH

@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text = text, color = color, fontSize = 11.sp)
    }
}

val Green = Color(0xFF2DB86A)
val Orange = Color(0xFFF59E0B)
val Red = Color(0xFFE94634)
val Purple = Color(0xFF8B5CF6)

@Composable
fun categoryColor(c: LessonCategory): Color {
    val map by Graph.settings.typeColors.collectAsState()
    val custom by Graph.settings.customColors.collectAsState()
    return Color(if (custom) map[c] ?: c.defaultColor.toInt() else c.defaultColor.toInt())
}

@Composable
fun isTablet(): Boolean {
    val c = LocalConfiguration.current
    return c.screenWidthDp >= 600 && c.screenHeightDp >= 480
}

@Composable
fun isTwoPane(): Boolean {
    val c = LocalConfiguration.current
    return isTablet() && c.screenWidthDp > c.screenHeightDp
}
