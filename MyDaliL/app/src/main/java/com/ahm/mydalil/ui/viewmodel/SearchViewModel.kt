package com.ahm.mydalil.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahm.mydalil.data.local.datastore.UserPreferences
import com.ahm.mydalil.data.local.room.Bookmark
import com.ahm.mydalil.data.repository.VerseRepository
import com.ahm.mydalil.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI State Definitions ---

sealed interface SearchUiState {
    data object EmptyQuery : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val results: List<VerseRepository.VerseSearchResult>,
        val totalResults: Int,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean,
        val query: String
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class BookmarkUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: VerseRepository,
    private val prefs: UserPreferences
) : ViewModel()
{

    // --- Helper classes for reactive stream ---
    private sealed class DisplayMode {
        object Search : DisplayMode()
        data class Surah(val surahName: String) : DisplayMode()
    }

    private data class SearchParams(
        val filters: Set<String>,
        val allWordsRequired: Boolean,
        val loadHadiths: Boolean,
        val loadVerses: Boolean
    )

    private data class FullTrigger(val query: String, val params: SearchParams, val mode: DisplayMode)

    private sealed class PageFetchResult {
        data object Loading : PageFetchResult()
        data class Success(val results: VerseRepository.PagedResults, val page: Int) : PageFetchResult()
        data class Error(val error: Throwable) : PageFetchResult()
    }


    // --- Input Flows from UI ---
    val searchQuery = MutableStateFlow("")
    val surahFilters = prefs.selectedSurahs.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val allWordsRequired = prefs.allWordsRequired.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val loadHadiths = prefs.loadHadiths.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val loadVerses = prefs.loadVerses.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val page = MutableStateFlow(0)
    private val displayMode = MutableStateFlow<DisplayMode>(DisplayMode.Search)


    // --- State for the UI ---
    val bookmarkedVerseIds = repository.getBookmarkedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val bookmarkUiState = repository.getAllBookmarks()
        .map { bookmarks -> BookmarkUiState(bookmarks = bookmarks, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkUiState(isLoading = true))

    // First, combine the search parameters into a single flow.
    private val searchParamsFlow = combine(
        surahFilters,
        allWordsRequired,
        loadHadiths,
        loadVerses,
    ) { filters, allWords, hadiths, verses ->
        SearchParams(filters, allWords, hadiths, verses)
    }

    // The main reactive pipeline that produces the search UI state
    @OptIn(
        ExperimentalCoroutinesApi::class
    )
    val uiState: StateFlow<SearchUiState> = combine(
        searchQuery.debounce(Constants.SEARCH_DEBOUNCE_MS),
        searchParamsFlow, // Use the combined parameters flow
        displayMode
    ) { query, params, mode ->
        FullTrigger(query, params, mode)
    }
        .distinctUntilChanged()
        .flatMapLatest { trigger ->
            when (val mode = trigger.mode) {
                is DisplayMode.Search -> {
                    // Standard search logic with pagination
                    page.flatMapConcat { pageNum ->
                        flow<PageFetchResult> {
                            try {
                                val results = repository.search(
                                    query = trigger.query,
                                    surahFilters = trigger.params.filters,
                                    allWordsRequired = trigger.params.allWordsRequired,
                                    page = pageNum,
                                    pageSize = Constants.PAGINATION_PAGE_SIZE,
                                    loadHadiths = trigger.params.loadHadiths,
                                    loadVerses = trigger.params.loadVerses
                                )
                                emit(PageFetchResult.Success(results, pageNum))
                            } catch (e: Exception) {
                                Log.e("SearchViewModel", "Search failed for page $pageNum", e)
                                emit(PageFetchResult.Error(e))
                            }
                        }.onStart { emit(PageFetchResult.Loading) }
                    }.scan(if (trigger.query.isBlank()) SearchUiState.EmptyQuery else SearchUiState.Loading) { currentState, result ->
                        when (result) {
                            is PageFetchResult.Loading -> {
                                if (currentState is SearchUiState.Success) {
                                    currentState.copy(isLoadingMore = true)
                                } else {
                                    SearchUiState.Loading
                                }
                            }
                            is PageFetchResult.Error -> {
                                SearchUiState.Error("Search failed: ${result.error.message}")
                            }
                            is PageFetchResult.Success -> {
                                val currentResults = (currentState as? SearchUiState.Success)?.results ?: emptyList()
                                val newItems = result.results.items

                                val combinedResults = if (result.page == 0) {
                                    newItems // For the first page, replace the list
                                } else {
                                    (currentResults + newItems).distinctBy { it.id }
                                }

                                SearchUiState.Success(
                                    results = combinedResults,
                                    totalResults = result.results.totalCount,
                                    canLoadMore = result.results.hasNextPage,
                                    isLoadingMore = false,
                                    query = trigger.query
                                )
                            }
                        }
                    }
                }
                is DisplayMode.Surah -> {
                    // Logic to display a single surah
                    flow {
                        emit(SearchUiState.Loading)
                        try {
                            val verses = repository.getVersesForSurah(mode.surahName)
                            val successState = SearchUiState.Success(
                                results = verses,
                                totalResults = verses.size,
                                canLoadMore = false,
                                isLoadingMore = false,
                                query = "" // Empty query to avoid highlighting
                            )
                            emit(successState)
                        } catch (e: Exception) {
                            Log.e("SearchViewModel", "Failed to load surah ${mode.surahName}", e)
                            emit(SearchUiState.Error("Failed to load surah: ${e.message}"))
                        }
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState.EmptyQuery
        )

    // --- UI Event Handlers ---

    fun onQueryChange(query: String) {
        displayMode.value = DisplayMode.Search
        searchQuery.value = query
        page.value = 0 // Reset pagination on new query
    }

    fun onSurahFilterChange(updatedFilters: Set<String>) {
        displayMode.value = DisplayMode.Search
        viewModelScope.launch { prefs.saveSelectedSurahs(updatedFilters) }
        page.value = 0
    }

    fun onAllWordsRequiredChange(isRequired: Boolean) {
        displayMode.value = DisplayMode.Search
        viewModelScope.launch { prefs.saveAllWordsRequired(isRequired) }
        page.value = 0
    }

    fun onFileSelectionChange(loadHadiths: Boolean, loadVerses: Boolean) {
        displayMode.value = DisplayMode.Search
        viewModelScope.launch { prefs.saveFileSelection(loadHadiths, loadVerses) }
        page.value = 0
    }

    fun onDisplaySurah(surahName: String) {
        displayMode.value = DisplayMode.Surah(surahName)
    }

    fun onLoadMore() {
        val current = uiState.value
        if (current is SearchUiState.Success && current.canLoadMore && !current.isLoadingMore) {
            page.value++
        }
    }

    fun toggleBookmark(verseResult: VerseRepository.VerseSearchResult) {
        viewModelScope.launch {
            val query = (uiState.value as? SearchUiState.Success)?.query ?: ""
            repository.toggleBookmark(verseResult, query)
        }
    }

    suspend fun getVersesForSurah(surahName: String): List<VerseRepository.VerseSearchResult> {
        return repository.getVersesForSurah(surahName)
    }

    // --- Public read-only values for UI ---
    val allSurahNames: StateFlow<List<String>> = repository.allSurahNames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hadithSurahNames: StateFlow<Set<String>> = repository.hadithSurahNames.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val verseSurahNames: StateFlow<Set<String>> = repository.verseSurahNames.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
}