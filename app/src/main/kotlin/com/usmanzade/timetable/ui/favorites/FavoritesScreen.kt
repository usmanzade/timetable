package com.usmanzade.timetable.ui.favorites

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.Subject
import com.usmanzade.timetable.ui.AppIcons
import com.usmanzade.timetable.ui.LocalTr
import com.usmanzade.timetable.ui.isTablet
import com.usmanzade.timetable.work.Scheduler
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val CARD_HEIGHT = 88.dp

@Composable
fun FavoritesScreen(bottomInset: Dp, onAdd: () -> Unit, onOpen: () -> Unit) {
    val tr = LocalTr.current
    val settings = Graph.settings
    val favorites by settings.favorites.collectAsState()
    val activeKey by settings.activeKey.collectAsState()
    val context = LocalContext.current

    var renaming by remember { mutableStateOf<Subject?>(null) }
    var renameShown by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = tr("Избранное", "Favorites"),
            actions = {
                IconButton(onClick = onAdd) {
                    Icon(MiuixIcons.Add, contentDescription = tr("Добавить", "Add"), tint = MiuixTheme.colorScheme.onSurface)
                }
            },
        )

        if (favorites.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp).padding(bottom = bottomInset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    MiuixIcons.Favorites,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(tr("Пока пусто", "Nothing here yet"), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    tr(
                        "Добавьте несколько групп или преподавателей, чтобы быстро переключаться между расписаниями.",
                        "Add groups or educators to switch between timetables quickly.",
                    ),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColorsPrimary()) {
                    Text(tr("Добавить", "Add"), color = MiuixTheme.colorScheme.onPrimary)
                }
            }
        } else {

            LazyVerticalGrid(
                columns = if (isTablet()) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 6.dp, bottom = bottomInset + 12.dp),
            ) {
                items(favorites, key = { it.key }) { s ->
                    FavoriteRow(
                        subject = s,
                        active = s.key == activeKey,
                        onClick = {
                            settings.setActive(s.key)
                            onOpen()
                        },
                        onRename = {
                            renaming = s
                            renameShown = true
                        },
                        onDelete = {
                            settings.removeFavorite(s.key)
                            Graph.repo.forget(s)
                            Scheduler.apply(context)
                        },
                    )
                }
            }
        }
    }

    RenameDialog(
        show = renameShown,
        subject = renaming,
        onDismiss = { renameShown = false },
        onSave = { key, name ->
            settings.renameFavorite(key, name)
            renameShown = false
        },
    )
}

@Composable
private fun FavoriteRow(
    subject: Subject,
    active: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val tr = LocalTr.current
    val colors = MiuixTheme.colorScheme
    val isGroup = subject.kind == Subject.Kind.GROUP
    val ring by animateColorAsState(if (active) colors.primary else colors.tertiaryContainer, tween(250), label = "ring")
    val iconTint by animateColorAsState(if (active) colors.onPrimary else colors.onTertiaryContainer, tween(250), label = "tint")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        cornerRadius = 22.dp,
        onClick = onClick,
        showIndication = true,
    ) {
        Row(
            Modifier.fillMaxWidth().height(CARD_HEIGHT).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(ring),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isGroup) AppIcons.Group else AppIcons.Educator,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    subject.displayName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val kind = if (isGroup) tr("Группа", "Group") else tr("Преподаватель", "Educator")
                Text(
                    text = if (subject.customName.isNullOrBlank()) kind else "$kind · ${subject.name}",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRename) {
                Icon(MiuixIcons.Rename, contentDescription = tr("Переименовать", "Rename"), tint = colors.onSurfaceVariantSummary)
            }
            IconButton(onClick = onDelete) {
                Icon(MiuixIcons.Delete, contentDescription = tr("Удалить", "Remove"), tint = colors.onSurfaceVariantSummary)
            }
        }
    }
}

@Composable
private fun RenameDialog(show: Boolean, subject: Subject?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val tr = LocalTr.current
    val s = subject
    val state = remember(s?.key, show) { TextFieldState(s?.displayName.orEmpty()) }
    OverlayDialog(
        show = show && s != null,
        title = tr("Переименовать", "Rename"),
        summary = s?.name,
        onDismissRequest = onDismiss,
    ) {
        if (s == null) return@OverlayDialog
        Column {
            TextField(
                state = state,
                label = tr("Своё название", "Custom name"),
                useLabelAsPlaceholder = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                tr("Пустое поле вернёт исходное название.", "Leave empty to restore the original name."),
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(tr("Отмена", "Cancel"), color = MiuixTheme.colorScheme.onSurface)
                }
                Button(
                    onClick = { onSave(s.key, state.text.toString()) },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) { Text(tr("Сохранить", "Save"), color = MiuixTheme.colorScheme.onPrimary) }
            }
        }
    }
}
