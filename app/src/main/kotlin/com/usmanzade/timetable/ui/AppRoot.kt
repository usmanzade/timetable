package com.usmanzade.timetable.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.usmanzade.timetable.data.EducatorInfo
import com.usmanzade.timetable.data.EducatorMatch
import com.usmanzade.timetable.data.EducatorResolution
import com.usmanzade.timetable.data.SpbuApi
import com.usmanzade.timetable.data.Subject
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.Tr
import com.usmanzade.timetable.ui.add.AddSubjectScreen
import com.usmanzade.timetable.ui.favorites.FavoritesScreen
import com.usmanzade.timetable.ui.notifications.NotificationsScreen
import com.usmanzade.timetable.ui.schedule.EducatorPreviewScreen
import com.usmanzade.timetable.ui.schedule.ScheduleScreen
import com.usmanzade.timetable.ui.settings.SettingsScreen
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Weeks
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

data class NavItem(val icon: ImageVector, val iconSelected: ImageVector, val label: String)

@Composable
fun AppRoot(
    requestedTab: Int?,
    onTabHandled: () -> Unit,
    onDarkChanged: (Boolean) -> Unit,
) {
    val settings = Graph.settings
    val theme by settings.theme.collectAsState()
    val monet by settings.monet.collectAsState()
    val lang by settings.language.collectAsState()
    val tr = remember(lang) { Tr(settings.isRussian()) }

    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val useMonet = monet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val mode = when {
        useMonet && theme == 1 -> ColorSchemeMode.MonetLight
        useMonet && theme == 2 -> ColorSchemeMode.MonetDark
        useMonet -> ColorSchemeMode.MonetSystem
        theme == 1 -> ColorSchemeMode.Light
        theme == 2 -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.System
    }
    val controller = remember(mode) { ThemeController(mode) }

    DisposableEffect(dark) {
        onDarkChanged(dark)
        onDispose { }
    }

    CompositionLocalProvider(LocalTr provides tr) {
        MiuixTheme(controller = controller) {
            Root(requestedTab, onTabHandled)
        }
    }
}

@Composable
private fun Root(requestedTab: Int?, onTabHandled: () -> Unit) {
    val tr = LocalTr.current
    val settings = Graph.settings
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val changes by settings.changes.collectAsState()
    val unread = changes.count { !it.read }

    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tab = requestedTab.coerceIn(0, 3)
            onTabHandled()
        }
    }

    var previewSubject by remember { mutableStateOf<Subject?>(null) }
    var previewVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var candidates by remember { mutableStateOf<List<EducatorInfo>>(emptyList()) }
    var chooserShown by remember { mutableStateOf(false) }
    val educatorNav = remember {
        EducatorNav(
            open = { name, lesson ->
                scope.launch {
                    val r = EducatorMatch.resolve(
                        name = name,
                        lesson = lesson,
                        search = { SpbuApi.searchEducators(it) },
                        eventsOf = { e, from, to ->
                            SpbuApi.events(Subject(Subject.Kind.EDUCATOR, e.id.toString(), e.name), from, to)
                        },
                    )
                    when (r) {
                        is EducatorResolution.One -> {
                            previewSubject = Subject(Subject.Kind.EDUCATOR, r.educator.id.toString(), r.educator.name)
                            previewVisible = true
                        }
                        is EducatorResolution.Many -> {
                            candidates = r.candidates
                            chooserShown = true
                        }
                        EducatorResolution.Failed -> {
                            candidates = emptyList()
                            chooserShown = true
                        }
                    }
                }
            },
        )
    }

    BackHandler(enabled = tab != 0 && !showAdd && !previewVisible) { tab = 0 }
    BackHandler(enabled = previewVisible) { previewVisible = false }

    val items = listOf(
        NavItem(MiuixIcons.Weeks, MiuixIcons.Weeks, tr("Расписание", "Schedule")),
        NavItem(MiuixIcons.Favorites, MiuixIcons.FavoritesFill, tr("Избранное", "Favorites")),
        NavItem(MiuixIcons.Alarm, MiuixIcons.Alarm, tr("Уведомления", "Notifications")),
        NavItem(MiuixIcons.Settings, MiuixIcons.Settings, tr("Настройки", "Settings")),
    )

    val surface = MiuixTheme.colorScheme.surface
    val glass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isRuntimeShaderSupported()
    val backdrop = rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }

    CompositionLocalProvider(LocalEducatorNav provides educatorNav) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (glass) {
                    GlassNavBar(
                        items = items,
                        selected = tab,
                        unread = unread,
                        backdrop = backdrop,
                        onSelect = {
                            tab = it
                            previewVisible = false
                        },
                    )
                } else {
                    NavigationBar(mode = NavigationBarDisplayMode.IconOnly) {
                        items.forEachIndexed { i, item ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = {
                                    tab = i
                                    previewVisible = false
                                },
                                icon = if (tab == i) item.iconSelected else item.icon,
                                label = item.label,
                            )
                        }
                    }
                }
            },
        ) { inner ->
            val bottomInset = inner.calculateBottomPadding()
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(animationSpec = tween(320)) { it / 5 * dir } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(220)) { -it / 6 * dir } + fadeOut(tween(160)))
                    },
                    label = "tabs",
                ) { t ->
                    when (t) {
                        0 -> ScheduleScreen(bottomInset = bottomInset, onAdd = { showAdd = true })
                        1 -> FavoritesScreen(
                            bottomInset = bottomInset,
                            onAdd = { showAdd = true },
                            onOpen = { tab = 0 },
                        )
                        2 -> CenteredColumn { NotificationsScreen(bottomInset = bottomInset) }
                        else -> CenteredColumn { SettingsScreen(bottomInset = bottomInset) }
                    }
                }

                EducatorChooser(
                    show = chooserShown,
                    candidates = candidates,
                    onDismiss = { chooserShown = false },
                    onPick = {
                        chooserShown = false
                        previewSubject = Subject(Subject.Kind.EDUCATOR, it.id.toString(), it.name)
                        previewVisible = true
                    },
                )

                AnimatedVisibility(
                    visible = previewVisible,
                    enter = slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(tween(220)),
                    exit = slideOutHorizontally(animationSpec = tween(260)) { it } + fadeOut(tween(180)),
                ) {
                    Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
                        previewSubject?.let { sub ->
                            androidx.compose.runtime.key(sub.key) {
                                EducatorPreviewScreen(subject = sub, onClose = { previewVisible = false }, bottomInset = bottomInset)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showAdd,
            enter = slideInVertically(animationSpec = tween(360)) { it } + fadeIn(tween(240)),
            exit = slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(200)),
        ) {
            Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
                CenteredColumn {
                    AddSubjectScreen(
                        onClose = { showAdd = false },
                        onAdded = {
                            showAdd = false
                            tab = 0
                        },
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun EducatorChooser(
    show: Boolean,
    candidates: List<EducatorInfo>,
    onDismiss: () -> Unit,
    onPick: (EducatorInfo) -> Unit,
) {
    val tr = LocalTr.current
    OverlayDialog(
        show = show,
        title = if (candidates.isEmpty()) tr("Преподаватель не найден", "Educator not found")
        else tr("Выберите преподавателя", "Choose an educator"),
        summary = if (candidates.isEmpty()) tr("Проверьте соединение или найдите его во вкладке «Избранное».", "Check your connection or search in Favorites.")
        else tr("По записи в расписании нашлось несколько человек.", "Several people match this timetable entry."),
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            candidates.forEach { e ->
                Text(
                    text = e.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(e) }.padding(vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Закрыть", "Close"), color = MiuixTheme.colorScheme.onSurface) }
        }
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    if (!isTablet()) {
        content()
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 680.dp).fillMaxSize()) { content() }
    }
}
