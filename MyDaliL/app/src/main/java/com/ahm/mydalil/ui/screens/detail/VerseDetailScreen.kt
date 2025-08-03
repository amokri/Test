package com.ahm.mydalil.ui.screens.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahm.mydalil.ui.components.VerseContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerseDetailScreen(
    items: List<VerseContent>,
    initialIndex: Int,
    isBookmarked: (index: Int) -> Boolean,
    onToggleBookmark: (index: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { items.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // This will react to page changes and provide the current item's data to the controls
    val currentPage = pagerState.currentPage
    val currentVerseContent = items.getOrNull(currentPage)

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize().clickable(enabled = false, onClick = {}), // Consume clicks
        color = MaterialTheme.colorScheme.surface
    ) {
        if (currentVerseContent == null) {
            // Handle case where list is empty or index is out of bounds.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Content not available.")
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
            return@Surface
        }

        val fullTextToCopy = remember(currentVerseContent) {
            buildString {
                append(currentVerseContent.verseText)
                append("\n\n${currentVerseContent.surahName} - ${currentVerseContent.verseNumber}")
            }
        }

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
                    text = if (pagerState.pageCount > 0) "${currentPage + 1} of ${pagerState.pageCount}" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(fullTextToCopy))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = { onToggleBookmark(currentPage) }) {
                        val bookmarked = isBookmarked(currentPage)
                        Icon(
                            imageVector = if (bookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (bookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pager Content Area
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                key = { page -> items[page].hashCode() } // Use a stable key if available
            ) { pageIndex ->
                VersePageContent(items[pageIndex])
            }


            // Bottom Navigation Buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                    },
                    enabled = currentPage > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                    Spacer(Modifier.width(8.dp))
                    Text("Previous")
                }
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    },
                    enabled = currentPage < pagerState.pageCount - 1,
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

@Composable
private fun VersePageContent(verseContent: VerseContent) {
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