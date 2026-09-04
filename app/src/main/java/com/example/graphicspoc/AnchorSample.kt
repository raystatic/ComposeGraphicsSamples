package com.example.graphicspoc

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class AnchoredDraggableSwipeDismissValue { DismissedStart, Resting, DismissedEnd }

@Preview
@Composable
fun AnchoredDraggableSwipeToDismissSample() {
    // Custom SwipeToDismiss component that allows dragging/swiping an item away

    @Composable
    fun SwipeToDismiss(
        state: AnchoredDraggableState<AnchoredDraggableSwipeDismissValue>,
        modifier: Modifier = Modifier,
        background: @Composable () -> Unit,
        dismissContent: @Composable () -> Unit
    ) {
        val overscrollEffect = rememberOverscrollEffect()

        Box(
            modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth()) {
                background()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val anchors = DraggableAnchors {
                            AnchoredDraggableSwipeDismissValue.DismissedStart at -placeable.width.toFloat()
                            AnchoredDraggableSwipeDismissValue.Resting at 0f
                            AnchoredDraggableSwipeDismissValue.DismissedEnd at placeable.width.toFloat()
                        }
                        state.updateAnchors(anchors)
                        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                    }
                    .overscroll(overscrollEffect)
                    .anchoredDraggable(
                        state,
                        Orientation.Horizontal,
                        overscrollEffect = overscrollEffect
                    )
            ) {
                dismissContent()
            }
        }
    }

    // Use rememberSaveable with AnchoredDraggableState's saver to persist and restore the current settled value across configuration changes. For example, if a user drags the item to DismissedStart and then changes the screen orientation or battery saver gets enabled, the item will still be set to DismissedStart
    val state =
        rememberSaveable(saver = AnchoredDraggableState.Saver()) {
            AnchoredDraggableState(initialValue = AnchoredDraggableSwipeDismissValue.Resting)
        }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SwipeToDismiss(
            state = state,
            modifier = Modifier.height(56.dp),
            background = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Red), contentAlignment = Alignment.Center
                ) {
                    val scope = rememberCoroutineScope()
                    Button(
                        onClick = {
                            scope.launch { state.animateTo(AnchoredDraggableSwipeDismissValue.Resting) }
                        }
                    ) {
                        Text("Click to animate the state back to center")
                    }
                }
            },
            dismissContent = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .shadow(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("Drag me to dismiss!")
                }
            }
        )

    }

}