package com.ahm.mydalil.ui.screens.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahm.mydalil.data.repository.VerseRepository
import com.ahm.mydalil.ui.components.FilterPanel
import com.ahm.mydalil.ui.components.SearchBar
import com.ahm.mydalil.ui.components.SearchOptions
import com.ahm.mydalil.ui.components.VerseCard
import com.ahm.mydalil.ui.components.toVerseContent
import com.ahm.mydalil.ui.screens.detail.VerseDetailScreen
import com.ahm.mydalil.ui.viewmodel.SearchUiState
import com.ahm.mydalil.ui.viewmodel.SearchViewModel
import com.ahm.mydalil.util.Constants
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@SuppressLint("DiscouragedApi", "InternalInsetResource")
fun getStatusBarHeight(context: Context): Float {
    val resources: Resources = context.resources
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    return (if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }).toFloat()
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToBookmarks: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val surahFilters by viewModel.surahFilters.collectAsStateWithLifecycle()
    val allWordsRequired by viewModel.allWordsRequired.collectAsStateWithLifecycle()
    val loadHadiths by viewModel.loadHadiths.collectAsStateWithLifecycle()
    val loadVerses by viewModel.loadVerses.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedVerseIds.collectAsStateWithLifecycle()

    val allSurahNames by viewModel.allSurahNames.collectAsStateWithLifecycle()
    val hadithSurahNames by viewModel.hadithSurahNames.collectAsStateWithLifecycle()
    val verseSurahNames by viewModel.verseSurahNames.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }

    // This is the list of verses that the detail screen will navigate through.
    // It can be populated by either the search results or a full surah listing.
    var detailViewVerseList by remember { mutableStateOf<List<VerseRepository.VerseSearchResult>>(emptyList()) }
    // This is the specific verse from the list that is currently being displayed.
    var selectedVerse by remember { mutableStateOf<VerseRepository.VerseSearchResult?>(null) }

    // This is the data from the main search query OR the surah display.
    val searchResults = (uiState as? SearchUiState.Success)?.results ?: emptyList()

    BackHandler(enabled = showFilters, onBack = { showFilters = false } )

    Scaffold(
        topBar = {
            Column {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onQueryChange,
                    onBookmarkClick = {
                        keyboardController?.hide()
                        onNavigateToBookmarks()
                    },
                    onFilterClick = { showFilters = !showFilters }
                )
                SearchOptions(
                    showFilters = showFilters,
                    allWordsRequired = allWordsRequired,
                    onAllWordsRequiredChange = viewModel::onAllWordsRequiredChange,
                    loadHadiths = loadHadiths,
                    loadVerses = loadVerses,
                    onFileSelectionChange = viewModel::onFileSelectionChange,
                )
                AnimatedVisibility(
                    visible = showFilters,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    FilterPanel(
                        allSurahNames = allSurahNames,
                        hadithSurahNames = hadithSurahNames,
                        verseSurahNames = verseSurahNames,
                        selectedSurahs = surahFilters,
                        onFilterChanged = viewModel::onSurahFilterChange,
                        onSurahLongPress = { surahName ->
                            viewModel.onDisplaySurah(surahName)
                            keyboardController?.hide()
                            showFilters = false // Close filter panel after selection
                        },
                        loadHadiths = loadHadiths,
                        loadVerses = loadVerses
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            ResultContent(
                uiState = uiState,
                bookmarkedIds = bookmarkedIds,
                onVerseClick = { result ->
                    keyboardController?.hide()
                    detailViewVerseList = searchResults
                    selectedVerse = result
                },
                onToggleBookmark = viewModel::toggleBookmark,
                onLoadMore = viewModel::onLoadMore
            )
        }
    }

    // Detail Screen Overlay
    AnimatedVisibility(
        visible = selectedVerse != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        selectedVerse?.let { verse ->
            val currentIndex = detailViewVerseList.indexOfFirst { it.id == verse.id }
            if (currentIndex != -1) {
                VerseDetailScreen(
                    items = detailViewVerseList.map { it.toVerseContent() },
                    initialIndex = currentIndex,
                    isBookmarked = { index -> bookmarkedIds.contains(detailViewVerseList[index].id) },
                    onToggleBookmark = { index -> viewModel.toggleBookmark(detailViewVerseList[index]) },
                    onDismiss = { selectedVerse = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultContent(
    uiState: SearchUiState,
    bookmarkedIds: Set<Int>,
    onVerseClick: (VerseRepository.VerseSearchResult) -> Unit,
    onToggleBookmark: (VerseRepository.VerseSearchResult) -> Unit,
    onLoadMore: () -> Unit
) {
    when (uiState) {
        is SearchUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SearchUiState.Success -> {
            val listState = rememberLazyListState()

            // Pagination trigger
            LaunchedEffect(listState, uiState.canLoadMore) {
                if (!uiState.canLoadMore) return@LaunchedEffect

                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .filterNotNull()
                    .map { lastIndex -> lastIndex >= uiState.results.size - Constants.PAGINATION_PREFETCH_DISTANCE }
                    .distinctUntilChanged()
                    .filter { shouldLoadMore -> shouldLoadMore }
                    .collect { onLoadMore() }
            }

            if (uiState.results.isEmpty() && uiState.query.isNotBlank()) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No results found for \"${uiState.query}\"")
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.results.isNotEmpty()) {
                        item {
                            val resultsText = if (uiState.query.isBlank() && uiState.results.isNotEmpty() && !uiState.canLoadMore) {
                                // This is likely a surah display (no query, not paginated)
                                "${uiState.totalResults} verses"
                            } else {
                                "${uiState.totalResults} results found"
                            }
                            Text(
                                text = resultsText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    items(items = uiState.results, key = { it.id }) { result ->
                        VerseCard(
                            result = result,
                            isBookmarked = bookmarkedIds.contains(result.id),
                            highlightKeywords = uiState.query.split(Regex("\\s+")),
                            onClick = { onVerseClick(result) },
                            onToggleBookmark = { onToggleBookmark(result) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 3.dp)
                            }
                        }
                    }
                }
            }
        }
        is SearchUiState.Error -> {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is SearchUiState.EmptyQuery -> {
            // This state is now used when the query is blank. We can show instructions
            // or a subset of data. Currently, the search shows all results.
            // If you want a message, you can add it here based on the total results.
            ResultContent( // Recursively call with a Success state to show all items
                uiState = SearchUiState.Success(emptyList(), 0, canLoadMore = true, isLoadingMore = false, query = ""),
                bookmarkedIds = bookmarkedIds,
                onVerseClick = onVerseClick,
                onToggleBookmark = onToggleBookmark,
                onLoadMore = onLoadMore
            )
        }
    }
}