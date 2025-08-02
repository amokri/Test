package com.ahm.mydalil.ui.screens.detail

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahm.mydalil.ui.components.VerseContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint(
    "UnusedContentLambdaTargetStateParameter"
)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VerseDetailScreen(
    verseContent: VerseContent,
    currentIndex: Int,
    totalCount: Int,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
//    val fullTextToCopy = remember(verseContent) {
//        "${verseContent.verseText}\n\n${verseContent.surahName} - ${verseContent.verseNumber}"
//    }
    val (mainText, reference) =
        remember(
            verseContent.verseText
        ) {
            verseContent.verseText.split(
                "(HR.",
                limit = 2
            )
                .let { parts ->
                    if (parts.size > 1) parts[0].trim() to "(HR.${parts[1].trim()}"
                    else parts[0].trim() to null
                }
        }
    val fullTextToCopy =
        remember(
            mainText,
            reference,
            verseContent
        ) {
            buildString {
                append(
                    mainText
                )
//                reference?.let {
//                    append(
//                        "\n($it)"
//                    )
//                }
                append(
                    "\n${verseContent.surahName} - ${verseContent.verseNumber}"
                )
            }
        }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize().clickable(enabled = false, onClick = {}), // Consume clicks
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // Top Control Bar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text(
                    "${currentIndex + 1} of $totalCount",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(fullTextToCopy))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Animated Content Area
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
//                    .pointerInput(Unit) {
//                        detectHorizontalDragGestures { _, dragAmount ->
//                            if (dragAmount > 50) onNavigatePrevious()
//                            if (dragAmount < -50) onNavigateNext()
//                        }
//                    },
//                contentAlignment = Alignment.Center
                    .swipeToNavigate(
                        onSwipeLeft = onNavigateNext,
                        onSwipeRight = onNavigatePrevious
                    )
            ) {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        slideInHorizontally(animationSpec = tween(30)) { it * direction } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween(30)) { -it * direction } + fadeOut() using
                                SizeTransform(clip = false)
//                        val direction = if (targetState > initialState) 1 else -1
//                        val animSpec = tween<IntOffset>(
//                                durationMillis = 300
//                            )
//                        (slideInHorizontally(
//                            animSpec
//                        ) { it * direction } +
//                                fadeIn()) togetherWith
//                                (slideOutHorizontally(
//                                    animSpec
//                                ) { -it * direction } +
//                                        fadeOut()) using
//                                SizeTransform(
//                                    clip = false
//                                )
                    },
                    label = "VerseContentAnimation"
                ) {
//                val pagerState = rememberPagerState(
//                    initialPage = currentIndex,
//                    pageCount = { totalCount }
//                )
//
//                HorizontalPager(state = pagerState) { pageIndex ->
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SelectionContainer {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    verseContent.verseText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                                Text(
                                    "${verseContent.surahName} - ${verseContent.verseNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                if (verseContent.extraInfo.isNotBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text(
                                            verseContent.extraInfo,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                Modifier.fillMaxWidth(),//.padding(vertical = 8.dp),//.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigatePrevious,
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                    Spacer(Modifier.width(8.dp))
                    Text("Previous")
                }
                FilledTonalButton(
                    onClick = onNavigateNext,
                    enabled = currentIndex < totalCount - 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                }
            }
        }
    }
}

fun Modifier.swipeToNavigate(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    swipeThreshold: Dp = 100.dp,
    maxSwipeDistance: Dp = 150.dp,
    sensitivity: Float = 1f
): Modifier =
    composed {
        val swipeThresholdPx =
            with(
                LocalDensity.current
            ) { swipeThreshold.toPx() }
        val maxSwipeDistancePx =
            with(
                LocalDensity.current
            ) { maxSwipeDistance.toPx() }

        var offsetX by remember {
            mutableStateOf(
                0f
            )
        }
        val offsetXState =
            animateFloatAsState(
                targetValue = offsetX,
                label = "swipeOffset"
            )

        val scale =
            remember(
                offsetX
            ) {
                val progress =
                    (1f - abs(
                        offsetX
                    ) / maxSwipeDistancePx).coerceIn(
                        0.9f,
                        1f
                    )
                progress
            }

        this
            .offset {
                IntOffset(
                    x = offsetXState.value.roundToInt(),
                    y = 0
                )
            }
            .scale(
                scale
            )
            .pointerInput(
                Unit
            ) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        val newOffset =
                            offsetX + (dragAmount * sensitivity)
                        offsetX =
                            newOffset.coerceIn(
                                -maxSwipeDistancePx,
                                maxSwipeDistancePx
                            )
                        change.consume()
                    },
                    onDragEnd = {
                        val wasSwipedRight =
                            offsetX > swipeThresholdPx
                        val wasSwipedLeft =
                            offsetX < -swipeThresholdPx

                        when {
                            wasSwipedRight -> onSwipeRight?.invoke()
                            wasSwipedLeft -> onSwipeLeft?.invoke()
                        }

                        // Animate back to center
                        offsetX =
                            0f
                    }
                )
            }
    }

