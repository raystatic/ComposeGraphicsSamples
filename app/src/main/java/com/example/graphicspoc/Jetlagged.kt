package com.example.graphicspoc

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch

enum class DrawerState {
    Open,
    Closed
}

enum class ScreenState {
    Home,
    Sleep,
    Leaderboard,
    Settings
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JetLaggedNavigationDrawer(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val drawerWidth = with(density) { 260.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf(ScreenState.Home) }

    // --- EXACT CODE FROM DOC / SLIDE 63 ---
    val anchors = DraggableAnchors {
        DrawerState.Open at drawerWidth
        DrawerState.Closed at 0f
    }
    val state = remember {
        AnchoredDraggableState(
            initialValue = DrawerState.Closed,
            anchors = anchors,
        )
    }

    fun toggleDrawerState() {
        coroutineScope.launch {
            if (state.currentValue == DrawerState.Open) {
                state.animateTo(DrawerState.Closed)
            } else {
                state.animateTo(DrawerState.Open)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreenDrawer(
            selectedScreen = screenState,
            onScreenSelected = { screen ->
                screenState = screen
                coroutineScope.launch { state.animateTo(DrawerState.Closed) }
            }
        )

        // --- EXACT CODE FROM DOC / SLIDE 63 ---
        ScreenContents(
            selectedScreen = screenState,
            onDrawerClicked = ::toggleDrawerState,
            modifier = Modifier
                .graphicsLayer {
                    this.translationX = state.requireOffset()
                    val scale = lerp(1f, 0.8f, state.requireOffset() / drawerWidth)
                    this.scaleX = scale
                    this.scaleY = scale
                    val cornerRadius = lerp(0f, 32f, state.requireOffset() / drawerWidth)
                    this.shape = RoundedCornerShape(cornerRadius.dp)
                    this.clip = true
                }
                .anchoredDraggable(state, Orientation.Horizontal)
        )
    }
}
// [START_EXCLUDE]
@Composable
fun HomeScreenDrawer(
    modifier: Modifier = Modifier,
    selectedScreen: ScreenState = ScreenState.Home,
    onScreenSelected: (ScreenState) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F0))
            .padding(top = 48.dp, start = 24.dp, end = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(
            icon = Icons.Default.Home,
            label = "Home",
            selected = selectedScreen == ScreenState.Home,
            onClick = { onScreenSelected(ScreenState.Home) }
        )
        DrawerItem(
            icon = Icons.Default.Bedtime,
            label = "Sleep",
            selected = selectedScreen == ScreenState.Sleep,
            onClick = { onScreenSelected(ScreenState.Sleep) }
        )
        DrawerItem(
            icon = Icons.Default.Leaderboard,
            label = "Leaderboard",
            selected = selectedScreen == ScreenState.Leaderboard,
            onClick = { onScreenSelected(ScreenState.Leaderboard) }
        )
        DrawerItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            selected = selectedScreen == ScreenState.Settings,
            onClick = { onScreenSelected(ScreenState.Settings) }
        )
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .padding(vertical = 8.dp)
            .background(
                color = if (selected) Color(0xFFFFD54F) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF2E2E2E),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color(0xFF2E2E2E),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ScreenContents(
    modifier: Modifier = Modifier,
    selectedScreen: ScreenState = ScreenState.Home,
    onDrawerClicked: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 36.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDrawerClicked) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Toggle Drawer"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "JetLagged",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Current Screen: ${selectedScreen.name}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    }
}
// [END_EXCLUDE]