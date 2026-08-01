package com.gaintrack.personal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gaintrack.personal.R

private data class TourSpec(
    val title: String,
    val caption: String,
    val points: List<String>,
)

private val tourSpecs = mapOf(
    "today" to TourSpec(
        "Your daily cockpit",
        "Everything important, without the noise.",
        listOf(
            "Swipe through dates",
            "Log meals and training",
            "Record your meditation minutes",
        ),
    ),
    "logbook" to TourSpec(
        "Your calendar history",
        "A clean month view keeps past days easy to revisit.",
        listOf("Tap any day", "Today stays highlighted", "Import or export CSV"),
    ),
    "progress" to TourSpec(
        "Trends you can act on",
        "Your weight journey stays visible from Day 1.",
        listOf("Daily weight trend", "Actual vs target", "One check-in each week"),
    ),
    "shopping" to TourSpec(
        "Your simple shopping list",
        "Add only what you need and tick it off.",
        listOf("Add a name", "Quantity and price are optional", "Edit or delete any item"),
    ),
    "plan" to TourSpec(
        "Your editable plan",
        "Overview, meals, training and safety stay separated.",
        listOf("Switch horizontal tabs", "Edit meals and exercises", "Open form guidance"),
    ),
    "settings" to TourSpec(
        "Your controls",
        "Update your profile, safety and app preferences here.",
        listOf("Rebuild personalization", "Replay all tours", "Your data stays local"),
    ),
)

@Composable
fun ScreenTour(tourKey: String, onDone: () -> Unit) {
    val spec = tourSpecs[tourKey] ?: return
    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.padding(18.dp)) {
                Image(
                    painter = painterResource(R.drawable.art_coach),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                )
                Text(
                    spec.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    spec.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    spec.points.forEachIndexed { index, point ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(30.dp),
                                shape = CircleShape,
                                color = when (index) {
                                    0 -> MaterialTheme.colorScheme.primaryContainer
                                    1 -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.tertiaryContainer
                                },
                            ) {
                                Text(
                                    "${index + 1}",
                                    modifier = Modifier.padding(top = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(
                                point,
                                modifier = Modifier.padding(start = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                ) {
                    Icon(Icons.Outlined.Check, null)
                    Text(" Got it")
                }
            }
        }
    }
}
