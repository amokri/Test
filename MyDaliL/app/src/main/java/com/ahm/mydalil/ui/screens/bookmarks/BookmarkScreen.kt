package com.ahm.mydalil.ui.screens.bookmarks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahm.mydalil.data.local.room.Bookmark
import com.ahm.mydalil.data.repository.VerseRepository
import com.ahm.mydalil.ui.components.*
import com.ahm.mydalil.ui.screens.detail.VerseDetailScreen
import com.ahm.mydalil.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(viewModel: SearchViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.bookmarkUiState.collectAsStateWithLifecycle()
    var selectedBookmark by remember { mutableStateOf<Bookmark?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.bookmarks.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("You have no bookmarks yet.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = uiState.bookmarks, key = { it.stableId }) { bookmark ->
                        BookmarkItem(
                            bookmark = bookmark,
                            onBookmarkClick = { selectedBookmark = it },
                            onRemoveBookmark = { bm ->
                                // Create a dummy search result to toggle the bookmark
                                val verseResult = VerseRepository.VerseSearchResult(
                                    verse = com.ahm.mydalil.data.model.Verse(bm.verseNumber, bm.verseText),
                                    surahName = bm.surahName,
                                    surahNumber = bm.surahNumber,
                                    source = bm.source
                                )
                                viewModel.toggleBookmark(verseResult)
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Screen Overlay
    AnimatedVisibility(
        visible = selectedBookmark != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        selectedBookmark?.let { bookmark ->
            val bookmarks = uiState.bookmarks
            val currentIndex = bookmarks.indexOf(bookmark)

            if (currentIndex != -1) {
                VerseDetailScreen(
                    items = bookmarks.map { it.toVerseContent() },
                    initialIndex = currentIndex,
                    isBookmarked = { true }, // All items on this screen are bookmarked
                    onToggleBookmark = { indexToToggle ->
                        val bookmarkToToggle = bookmarks[indexToToggle]
                        val verseResult = VerseRepository.VerseSearchResult(
                            verse = com.ahm.mydalil.data.model.Verse(bookmarkToToggle.verseNumber, bookmarkToToggle.verseText),
                            surahName = bookmarkToToggle.surahName,
                            surahNumber = bookmarkToToggle.surahNumber,
                            source = bookmarkToToggle.source
                        )
                        viewModel.toggleBookmark(verseResult)
                        // Always dismiss after a toggle action from the bookmark screen for simplicity,
                        // as the list of items is about to change.
                        selectedBookmark = null
                    },
                    onDismiss = { selectedBookmark = null }
                )
            }
        }
    }
}

@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    onBookmarkClick: (Bookmark) -> Unit,
    onRemoveBookmark: (Bookmark) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onBookmarkClick(bookmark) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${bookmark.surahNumber}.${bookmark.verseNumber} ${bookmark.surahName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    SourceIndicator(
                        isFromHadith = bookmark.source == "hadith",
                        isFromVerse = bookmark.source == "verse"
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bookmark.verseText.take(120) + if (bookmark.verseText.length > 120) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )
                if (bookmark.query.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Found with: \"${bookmark.query}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onRemoveBookmark(bookmark) }) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Remove bookmark",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}