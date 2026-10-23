package com.usmanzade.timetable.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.LessonCategory
import com.usmanzade.timetable.ui.LocalTr
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private val READY_COLORS = listOf(
    0xFFE94634, 0xFFF97316, 0xFFF59E0B, 0xFFEAB308, 0xFF84CC16, 0xFF2DB86A,
    0xFF10B981, 0xFF14B8A6, 0xFF06B6D4, 0xFF0EA5E9, 0xFF3482FF, 0xFF6366F1,
    0xFF8B5CF6, 0xFFA855F7, 0xFFD946EF, 0xFFEC4899, 0xFFF43F5E, 0xFF9CA3AF,
    0xFF64748B, 0xFF475569, 0xFF78716C, 0xFF92400E, 0xFF1E3A8A, 0xFF111827,
)

private fun toHex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

private fun parseHex(t: String): Color? {
    val h = t.trim().removePrefix("#")
    if (h.length != 6) return null
    val v = h.toLongOrNull(16) ?: return null
    return Color((0xFF000000L or v).toInt())
}

@Composable
fun ColorEditorDialog(show: Boolean, category: LessonCategory?, onDismiss: () -> Unit) {
    val tr = LocalTr.current
    val cat = category
    OverlayDialog(
        show = show && cat != null,
        title = cat?.let { tr(it.ru, it.en) },
        summary = tr("Цвет этого вида занятий", "Color for this class type"),
        onDismissRequest = onDismiss,
    ) {
        if (cat == null) return@OverlayDialog
        key(cat) { EditorBody(cat, onDismiss) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorBody(cat: LessonCategory, onDismiss: () -> Unit) {
    val tr = LocalTr.current
    val s = Graph.settings
    var color by remember { mutableStateOf(Color(s.typeColors.value[cat] ?: cat.defaultColor.toInt())) }
    var tab by remember { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) }
    val hex = rememberTextFieldState(toHex(color))

    LaunchedEffect(color) {
        if (parseHex(hex.text.toString()) != color) hex.setTextAndPlaceCursorAtEnd(toHex(color))
    }

    LaunchedEffect(Unit) {
        snapshotFlow { hex.text.toString() }.collect { text ->
            parseHex(text)?.let { c ->
                if (c != color) {
                    color = c
                    version++
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape),
            )
            TextField(
                state = hex,
                label = "HEX",
                useLabelAsPlaceholder = true,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        TabRowWithContour(
            tabs = listOf(tr("Готовые", "Presets"), tr("Палитра", "Palette"), "HSV", "RGB"),
            selectedTabIndex = tab,
            onTabSelected = { tab = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            when (tab) {
                0 -> FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    READY_COLORS.forEach { argb ->
                        val c = Color(argb.toInt())
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    3.dp,
                                    if (c == color) MiuixTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape,
                                )
                                .clickable { color = c; version++ },
                        )
                    }
                }
                1 -> key(version) {
                    ColorPalette(color = color, onColorChanged = { color = it }, modifier = Modifier.fillMaxWidth())
                }
                2 -> key(version) {
                    ColorPicker(color = color, onColorChanged = { color = it }, modifier = Modifier.fillMaxWidth())
                }
                else -> Column {
                    RgbSlider("R", color.red) { color = Color((it * 255).roundToInt(), (color.green * 255).roundToInt(), (color.blue * 255).roundToInt()) }
                    RgbSlider("G", color.green) { color = Color((color.red * 255).roundToInt(), (it * 255).roundToInt(), (color.blue * 255).roundToInt()) }
                    RgbSlider("B", color.blue) { color = Color((color.red * 255).roundToInt(), (color.green * 255).roundToInt(), (it * 255).roundToInt()) }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    color = Color(cat.defaultColor.toInt())
                    version++
                },
                modifier = Modifier.weight(1f),
            ) { Text(tr("Сбросить", "Reset"), color = MiuixTheme.colorScheme.onSurface) }
            Button(
                onClick = {
                    s.setTypeColor(cat, color.toArgb())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.weight(1f),
            ) { Text(tr("Готово", "Done"), color = MiuixTheme.colorScheme.onPrimary) }
        }
    }
}

@Composable
private fun RgbSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f, modifier = Modifier.weight(1f))
        Text("${(value * 255).roundToInt()}", fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
    }
}
