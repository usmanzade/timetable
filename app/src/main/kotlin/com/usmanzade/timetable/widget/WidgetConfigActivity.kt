package com.usmanzade.timetable.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.Tr
import com.usmanzade.timetable.data.Subject
import com.usmanzade.timetable.ui.AppIcons
import com.usmanzade.timetable.ui.LocalTr
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        enableEdgeToEdge()

        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val initial = WidgetPrefs.get(this, id)

        setContent {
            val tr = remember { Tr(Graph.settings.isRussian()) }
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            CompositionLocalProvider(LocalTr provides tr) {
                MiuixTheme(controller = controller) {
                    ConfigScreen(
                        initialKey = initial.subjectKey,
                        initialWeek = initial.week,
                        onDone = { key, week ->
                            WidgetPrefs.save(this, id, key, week, 0)
                            TodayWidgetProvider.launchUpdate(this, intArrayOf(id))
                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigScreen(initialKey: String?, initialWeek: Boolean, onDone: (String?, Boolean) -> Unit) {
    val tr = LocalTr.current
    val favorites by Graph.settings.favorites.collectAsState()
    var key by remember {
        mutableStateOf(initialKey ?: Graph.settings.activeKey.value ?: Graph.settings.favorites.value.firstOrNull()?.key)
    }
    var week by remember { mutableStateOf(initialWeek) }
    val colors = MiuixTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        Text(tr("Настройка виджета", "Widget setup"), style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))

        if (favorites.isEmpty()) {
            Text(
                tr(
                    "Сначала добавьте группу или преподавателя в избранное в самом приложении.",
                    "First add a group or an educator to favorites in the app.",
                ),
                color = colors.onSurfaceVariantSummary,
            )
            return@Column
        }

        Text(tr("Расписание", "Timetable"), fontSize = 13.sp, color = colors.onSurfaceVariantSummary)
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(favorites) { s ->
                val selected = s.key == key
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    cornerRadius = 18.dp,
                    onClick = { key = s.key },
                    showIndication = true,
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (s.kind == Subject.Kind.GROUP) AppIcons.Group else AppIcons.Educator,
                            contentDescription = null,
                            tint = if (selected) colors.primary else colors.onSurfaceVariantSummary,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(s.displayName, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                        if (selected) {
                            Icon(MiuixIcons.Ok, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(tr("Показывать", "Show"), fontSize = 13.sp, color = colors.onSurfaceVariantSummary)
        Spacer(Modifier.height(6.dp))
        TabRowWithContour(
            tabs = listOf(tr("День", "Day"), tr("Неделя", "Week")),
            selectedTabIndex = if (week) 1 else 0,
            onTabSelected = { week = it == 1 },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onDone(key, week) },
            colors = ButtonDefaults.buttonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(tr("Готово", "Done"), color = colors.onPrimary) }
    }
}
