package com.usmanzade.timetable.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GlassNavBar(
    items: List<NavItem>,
    selected: Int,
    unread: Int,
    backdrop: LayerBackdrop,
    onSelect: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(36.dp)
    val itemW = 66.dp
    val itemH = 52.dp
    val indicatorX by animateDpAsState(
        targetValue = itemW * selected,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow),
        label = "indicator",
    )
    val blurColors = BlurDefaults.blurColors(
        blendColors = listOf(BlendColorEntry(color = colors.surfaceContainer.copy(alpha = if (dark) 0.5f else 0.6f))),
    )

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .textureBlur(
                    backdrop = backdrop,
                    shape = shape,
                    blurRadius = 28f,
                    colors = blurColors,
                    highlight = if (dark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight,
                )
                .padding(6.dp),
        ) {
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .size(itemW, itemH)
                    .clip(RoundedCornerShape(30.dp))
                    .background(colors.primary.copy(alpha = 0.18f)),
            )
            Row {
                items.forEachIndexed { i, item ->
                    val isSel = i == selected
                    val tint by animateColorAsState(
                        targetValue = if (isSel) colors.primary else colors.onSurfaceContainer.copy(alpha = 0.62f),
                        label = "tint",
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isSel) 1.16f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                        label = "scale",
                    )
                    Box(
                        Modifier
                            .size(itemW, itemH)
                            .clip(RoundedCornerShape(30.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isSel) item.iconSelected else item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier
                                .size(26.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                        if (i == 2 && unread > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 17.dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(colors.error),
                            )
                        }
                    }
                }
            }
        }
    }
}
