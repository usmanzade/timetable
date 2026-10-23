package com.usmanzade.timetable.ui.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.ChangeType
import com.usmanzade.timetable.label
import com.usmanzade.timetable.ui.Chip
import com.usmanzade.timetable.ui.Green
import com.usmanzade.timetable.ui.LocalTr
import com.usmanzade.timetable.ui.Orange
import com.usmanzade.timetable.ui.Purple
import com.usmanzade.timetable.ui.Red
import com.usmanzade.timetable.work.Notifier
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NotificationsScreen(bottomInset: Dp) {
    val tr = LocalTr.current
    val settings = Graph.settings
    val context = LocalContext.current
    val changes by settings.changes.collectAsState()
    val interval by settings.intervalMinutes.collectAsState()
    var granted by remember { mutableStateOf(Notifier.canNotify(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    DisposableEffect(Unit) {
        onDispose { settings.markAllRead() }
    }

    Column(Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = tr("Уведомления", "Notifications"),
            actions = {
                if (changes.isNotEmpty()) {
                    IconButton(onClick = { settings.clearChanges() }) {
                        Icon(MiuixIcons.Delete, contentDescription = tr("Очистить", "Clear"), tint = MiuixTheme.colorScheme.onSurface)
                    }
                }
            },
        )
        LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = bottomInset + 12.dp)) {
            if (!granted && Build.VERSION.SDK_INT >= 33) {
                item(key = "permission") {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), cornerRadius = 18.dp) {
                        Text(
                            tr("Разрешите уведомления, чтобы узнавать об изменениях в расписании.", "Allow notifications to be told about timetable changes."),
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(tr("Разрешить", "Allow"), color = MiuixTheme.colorScheme.onPrimary) }
                    }
                }
            }
            if (changes.isEmpty()) {
                item(key = "empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            MiuixIcons.Alarm,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(60.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(tr("Изменений пока нет", "No changes yet"), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            tr(
                                "Мы проверяем расписание избранных групп примерно раз в $interval мин и сообщим о переносах, отменах и заменах.",
                                "We check your favorite timetables about every $interval min and tell you about moves, cancellations and substitutions.",
                            ),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            items(changes, key = { it.id }) { c ->
                val color = when (c.type) {
                    ChangeType.ADDED -> Green
                    ChangeType.REMOVED, ChangeType.CANCELLED -> Red
                    ChangeType.TIME -> Orange
                    ChangeType.LOCATION -> MiuixTheme.colorScheme.primary
                    ChangeType.EDUCATOR -> Purple
                }
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), cornerRadius = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Chip(c.type.label(tr), color)
                        Spacer(Modifier.weight(1f))
                        if (!c.read) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(MiuixTheme.colorScheme.primary))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(c.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Notifier.dateLabel(c.date, tr.ru)} · ${c.subjectName}",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    if (c.detail.isNotBlank()) {
                        Text(c.detail, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
