package com.gaintrack.personal

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gaintrack.personal.ui.GainTrackTheme
import com.gaintrack.personal.ui.LocalMotionEnabled
import com.gaintrack.personal.ui.ScreenTour
import com.gaintrack.personal.ui.screens.LogbookScreen
import com.gaintrack.personal.ui.screens.OnboardingScreen
import com.gaintrack.personal.ui.screens.PlanScreen
import com.gaintrack.personal.ui.screens.ProgressScreen
import com.gaintrack.personal.ui.screens.SettingsScreen
import com.gaintrack.personal.ui.screens.ShoppingScreen
import com.gaintrack.personal.ui.screens.TodayScreen
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onResume() {
        super.onResume()
        viewModel.repository.selectDate(LocalDate.now())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cream = android.graphics.Color.rgb(255, 249, 242)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(cream, cream),
            navigationBarStyle = SystemBarStyle.light(cream, cream),
        )
        setContent {
            GainTrackTheme {
                val permission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                GainTrackApp(viewModel)
            }
        }
    }
}

private data class Destination(val label: String, val icon: ImageVector)

@Composable
private fun GainTrackApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.profile.onboardingComplete) {
        OnboardingScreen(state.profile, viewModel)
        return
    }
    var selected by remember { mutableIntStateOf(0) }
    var previousSelected by remember { mutableIntStateOf(0) }
    var settingsOpen by remember { mutableIntStateOf(0) }
    val motion = LocalMotionEnabled.current
    val destinations = listOf(
        Destination("Today", Icons.Outlined.Home),
        Destination("Logbook", Icons.Outlined.CalendarMonth),
        Destination("Progress", Icons.Outlined.Insights),
        Destination("Shopping", Icons.Outlined.ShoppingBasket),
        Destination("Plan", Icons.Outlined.EditNote),
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = androidx.compose.ui.unit.Dp.Hairline,
            ) {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selected == index && settingsOpen == 0,
                        onClick = {
                            previousSelected = selected
                            selected = index
                            settingsOpen = 0
                        },
                        icon = { Icon(destination.icon, destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = if (settingsOpen == 1) 5 else selected,
            modifier = Modifier.padding(padding),
            transitionSpec = {
                if (!motion) {
                    ContentTransform(fadeIn(tween(0)), fadeOut(tween(0)))
                } else {
                    val movingForward = targetState > initialState ||
                        (targetState == selected && selected >= previousSelected)
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(tween(220)) {
                            if (movingForward) it / 7 else -it / 7
                        } + fadeIn(tween(180)),
                        initialContentExit = slideOutHorizontally(tween(180)) {
                            if (movingForward) -it / 9 else it / 9
                        } + fadeOut(tween(140)),
                    )
                }
            },
            label = "screen transition",
        ) { destination ->
            when (destination) {
                0 -> TodayScreen(state, viewModel, onSettings = { settingsOpen = 1 })
                1 -> LogbookScreen(state, viewModel)
                2 -> ProgressScreen(state, viewModel)
                3 -> ShoppingScreen(state, viewModel)
                4 -> PlanScreen(state, viewModel)
                else -> SettingsScreen(state, viewModel, onBack = { settingsOpen = 0 })
            }
        }
    }
    val tourKey = if (settingsOpen == 1) {
        "settings"
    } else {
        listOf("today", "logbook", "progress", "shopping", "plan")[selected]
    }
    if (tourKey !in state.seenTours) {
        ScreenTour(tourKey) { viewModel.repository.markTourSeen(tourKey) }
    }
}
