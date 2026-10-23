package com.usmanzade.timetable.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.usmanzade.timetable.ui.LocalTr
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScheduleTopBar(showToday: Boolean, onToday: () -> Unit, onRefresh: () -> Unit) {
    val tr = LocalTr.current
    val colors = MiuixTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
    ) {
        Box(Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) {
            AnimatedVisibility(
                visible = showToday,
                enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), initialScale = 0.6f) + expandHorizontally(),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.6f) + shrinkHorizontally(),
            ) {
                Text(
                    text = tr("Сегодня", "Today"),
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.primary.copy(alpha = 0.14f))
                        .clickable(onClick = onToday)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        Text(
            text = tr("Расписание", "Timetable"),
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) {
            IconButton(onClick = onRefresh) {
                Icon(MiuixIcons.Refresh, contentDescription = tr("Обновить", "Refresh"), tint = colors.onSurface)
            }
        }
    }
}

@Composable
fun PreviewTopBar(
    title: String,
    subtitle: String,
    isFavorite: Boolean,
    showToday: Boolean,
    onBack: () -> Unit,
    onToday: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRefresh: () -> Unit,
) {
    val tr = LocalTr.current
    val colors = MiuixTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(MiuixIcons.Back, contentDescription = tr("Назад", "Back"), tint = colors.onSurface)
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = colors.onSurfaceVariantSummary)
        }
        AnimatedVisibility(
            visible = showToday,
            enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), initialScale = 0.6f),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.6f),
        ) {
            Text(
                text = tr("Сегодня", "Today"),
                color = colors.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.primary.copy(alpha = 0.14f))
                    .clickable(onClick = onToday)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                contentDescription = tr("В избранное", "Favorite"),
                tint = if (isFavorite) colors.primary else colors.onSurface,
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(MiuixIcons.Refresh, contentDescription = tr("Обновить", "Refresh"), tint = colors.onSurface)
        }
    }
}
