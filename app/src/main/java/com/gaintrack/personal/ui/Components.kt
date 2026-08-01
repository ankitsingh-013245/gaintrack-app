package com.gaintrack.personal.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun SectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        action?.invoke()
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
fun TonalGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .56f),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    supporting: String? = null,
) {
    Column(modifier) {
        AnimatedContent(targetState = value, label = "metric") { animatedValue ->
            Text(
                animatedValue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!supporting.isNullOrBlank()) {
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Surface(
        modifier = modifier.then(clickable),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(14.dp)) {
            if (icon != null) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = tint.copy(alpha = .13f),
                ) {
                    Icon(
                        icon,
                        null,
                        tint = tint,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Metric(label, value, tint = tint, supporting = supporting)
        }
    }
}

@Composable
fun ProgressBlock(title: String, value: Int, total: Int, subtitle: String) {
    val motion = LocalMotionEnabled.current
    val target = if (total == 0) 0f else value.toFloat() / total
    val progress by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(if (motion) 500 else 0, easing = FastOutSlowInEasing),
        label = "progress",
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text("$value/$total", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
    )
    Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun GoalRing(
    progress: Float,
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier,
    size: Dp = 146.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val motion = LocalMotionEnabled.current
    val animated = androidx.compose.runtime.remember { Animatable(0f) }
    LaunchedEffect(progress, motion) {
        if (motion) {
            animated.animateTo(
                progress.coerceIn(0f, 1f),
                animationSpec = tween(700, easing = FastOutSlowInEasing),
            )
        } else {
            animated.snapTo(progress.coerceIn(0f, 1f))
        }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val track = MaterialTheme.colorScheme.surfaceVariant
        val primary = tint
        Canvas(Modifier.matchParentSize().padding(8.dp)) {
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 13.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 360f * animated.value,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 13.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(primaryText, style = MaterialTheme.typography.headlineSmall)
            Text(
                secondaryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = .13f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun ExpandableSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    SectionCard(Modifier.clickable { onExpandedChange(!expanded) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onExpandedChange(!expanded) }) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    if (expanded) "Collapse" else "Expand",
                )
            }
        }
        if (LocalMotionEnabled.current) {
            androidx.compose.animation.AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 14.dp))
                    content()
                }
            }
        } else if (expanded) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 14.dp))
                content()
            }
        }
    }
}

@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val motion = LocalMotionEnabled.current
    val alpha = if (motion) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val animatedAlpha by transition.animateFloat(
            initialValue = .42f,
            targetValue = .82f,
            animationSpec = infiniteRepeatable(
                animation = tween(850),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "shimmer alpha",
        )
        animatedAlpha
    } else {
        .65f
    }
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(MaterialTheme.shapes.medium)
            .background(color)
    )
}
