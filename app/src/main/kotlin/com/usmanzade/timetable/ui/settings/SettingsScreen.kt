package com.usmanzade.timetable.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.ChangeType
import com.usmanzade.timetable.data.LessonCategory
import com.usmanzade.timetable.label
import com.usmanzade.timetable.ui.Links
import com.usmanzade.timetable.ui.LocalTr
import com.usmanzade.timetable.ui.openUrl
import com.usmanzade.timetable.work.Scheduler
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val INTERVALS = listOf(15, 30, 60, 180, 360)

@Composable
fun SettingsScreen(bottomInset: Dp) {
    val tr = LocalTr.current
    val s = Graph.settings
    val context = LocalContext.current
    val theme by s.theme.collectAsState()
    val monet by s.monet.collectAsState()
    val lang by s.language.collectAsState()
    val customColors by s.customColors.collectAsState()
    val typeColors by s.typeColors.collectAsState()
    val notify by s.notifyEnabled.collectAsState()
    val interval by s.intervalMinutes.collectAsState()
    val types by s.enabledTypes.collectAsState()

    var editFor by remember { mutableStateOf<LessonCategory?>(null) }
    var editShown by remember { mutableStateOf(false) }

    val cardMod = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    val enter = fadeIn(tween(220)) + expandVertically(tween(300))
    val exit = fadeOut(tween(150)) + shrinkVertically(tween(240))

    Column(Modifier.fillMaxSize()) {
        SmallTopAppBar(title = tr("Настройки", "Settings"))
        LazyColumn(contentPadding = PaddingValues(bottom = bottomInset + 12.dp)) {

            item {
                SmallTitle(tr("Внешний вид", "Appearance"))
                Card(cardMod, cornerRadius = 18.dp, insideMargin = PaddingValues(0.dp)) {
                    OverlayDropdownPreference(
                        title = tr("Язык", "Language"),
                        items = listOf(tr("Системный", "System"), "Русский", "English"),
                        selectedIndex = lang,
                        onSelectedIndexChange = { s.setLanguage(it) },
                    )
                    OverlayDropdownPreference(
                        title = tr("Тема", "Theme"),
                        items = listOf(tr("Системная", "System"), tr("Светлая", "Light"), tr("Тёмная", "Dark")),
                        selectedIndex = theme,
                        onSelectedIndexChange = { s.setTheme(it) },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SwitchPreference(
                            title = tr("Цвета системы", "System colors"),
                            checked = monet,
                            onCheckedChange = { s.setMonet(it) },
                        )
                    }
                    SwitchPreference(
                        title = tr("Цвета занятий", "Class colors"),
                        summary = tr("Свой цвет для каждого вида занятий", "Your own color for each class type"),
                        checked = customColors,
                        onCheckedChange = { s.setCustomColors(it) },
                    )
                    AnimatedVisibility(visible = customColors, enter = enter, exit = exit) {
                        Column {
                            LessonCategory.entries.forEach { c ->
                                val color = Color(typeColors[c] ?: c.defaultColor.toInt())
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editFor = c
                                            editShown = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        tr(c.ru, c.en),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Box(Modifier.size(28.dp).clip(CircleShape).background(color))
                                }
                            }
                        }
                    }
                }
            }

            item {
                SmallTitle(tr("Уведомления", "Notifications"))
                Card(cardMod, cornerRadius = 18.dp, insideMargin = PaddingValues(0.dp)) {
                    SwitchPreference(
                        title = tr("Проверять расписание", "Check timetable"),
                        summary = tr("Для всех групп и преподавателей из избранного", "For every favorite group and educator"),
                        checked = notify,
                        onCheckedChange = {
                            s.setNotifyEnabled(it)
                            Scheduler.apply(context)
                        },
                    )
                    AnimatedVisibility(visible = notify, enter = enter, exit = exit) {
                        Column {
                            OverlayDropdownPreference(
                                title = tr("Период проверки", "Check interval"),
                                items = INTERVALS.map { if (it < 60) tr("$it мин", "$it min") else tr("${it / 60} ч", "${it / 60} h") },
                                selectedIndex = INTERVALS.indexOf(interval).coerceAtLeast(0),
                                onSelectedIndexChange = {
                                    s.setInterval(INTERVALS[it])
                                    Scheduler.apply(context)
                                },
                            )
                            ArrowPreference(
                                title = tr("Проверить сейчас", "Check now"),
                                onClick = { Scheduler.checkNow(context) },
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = notify, enter = enter, exit = exit) {
                    Column {
                        SmallTitle(tr("О каких изменениях сообщать", "Notify me about"))
                        Card(cardMod, cornerRadius = 18.dp, insideMargin = PaddingValues(0.dp)) {
                            ChangeType.entries.forEach { t ->
                                SwitchPreference(
                                    title = t.label(tr),
                                    checked = t in types,
                                    onCheckedChange = { s.setTypeEnabled(t, it) },
                                )
                            }
                        }
                    }
                }
            }

            item {
                SmallTitle(tr("О приложении", "About"))
                Card(cardMod, cornerRadius = 18.dp, insideMargin = PaddingValues(0.dp)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(tr("Автор приложения", "App author"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            tr("Мухаммадсоли Усманов", "Mukhammadsoli Usmanov"),
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    ArrowPreference(
                        title = tr("Обновления приложения", "App updates"),
                        summary = Links.APP_REPO,
                        onClick = { openUrl(context, Links.RELEASES) },
                    )
                    ArrowPreference(
                        title = tr("Другие проекты автора", "Author's other projects"),
                        summary = "github.com/usmanzade",
                        onClick = { openUrl(context, Links.AUTHOR_GITHUB) },
                    )
                }
            }
        }
    }

    ColorEditorDialog(show = editShown, category = editFor, onDismiss = { editShown = false })
}
