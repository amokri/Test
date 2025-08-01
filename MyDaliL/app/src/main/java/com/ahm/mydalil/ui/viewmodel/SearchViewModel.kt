package com.ahm.mydalil.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahm.mydalil.data.local.datastore.UserPreferences
import com.ahm.mydalil.data.local.room.Bookmark
import com.ahm.mydalil.data.repository.VerseRepository
import com.ahm.mydalil.util.Constants
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
class SearchViewModel(
    private val repository: VerseRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    // --- Input Flows from UI ---
    val searchQuery = MutableStateFlow("")
    val surahFilters = prefs.selectedSurahs.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val allWordsRequired = prefs.allWordsRequired.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val loadHadiths = prefs.loadHadiths.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val loadVerses = prefs.loadVerses.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val page = MutableStateFlow(0)

    // --- State for the UI ---
    val bookmarkedVerseIds = repository.getBookmarkedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val bookmarkUiState = repository.getAllBookmarks()
        .map { bookmarks -> BookmarkUiState(bookmarks = bookmarks, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarkUiState(isLoading = true))

    // The main reactive pipeline that produces the search UI state
    val uiState: StateFlow<SearchUiState> = combine(
        searchQuery.debounce(Constants.SEARCH_DEBOUNCE_MS),
        surahFilters,
        allWordsRequired,
        loadHadiths,
        loadVerses,
    ) { query, filters, allWords, hadiths, verses ->
        if (query.isBlank()) null else SearchTrigger(query, SearchParams(filters, allWords, hadiths, verses))
    }
        .distinctUntilChanged()
        .flatMapLatest { trigger ->
            if (trigger == null) {
                return@flatMapLatest flowOf(SearchUiState.EmptyQuery)
            }

            // This inner flow manages pagination for a single search trigger.
            // It's restarted by flatMapLatest whenever the trigger changes.
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
            }.scan(SearchUiState.EmptyQuery as SearchUiState) { currentState, result ->
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
                            // Append to existing, ensuring no duplicates if flow restarts
                            currentResults + newItems.filterNot { currentResults.any { old -> old.id == it.id } }
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
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState.EmptyQuery
        )

    // --- Helper data classes for the reactive stream ---

    private data class SearchParams(
        val filters: Set<String>,
        val allWordsRequired: Boolean,
        val loadHadiths: Boolean,
        val loadVerses: Boolean
    )

    private data class SearchTrigger(val query: String, val params: SearchParams)

    private sealed class PageFetchResult {
        data object Loading : PageFetchResult()
        data class Success(val results: VerseRepository.PagedResults, val page: Int) : PageFetchResult()
        data class Error(val error: Throwable) : PageFetchResult()
    }


    // --- UI Event Handlers ---

    fun onQueryChange(query: String) {
        searchQuery.value = query
        page.value = 0 // Reset pagination on new query
    }

    fun onSurahFilterChange(updatedFilters: Set<String>) {
        viewModelScope.launch { prefs.saveSelectedSurahs(updatedFilters) }
        page.value = 0
    }

    fun onAllWordsRequiredChange(isRequired: Boolean) {
        viewModelScope.launch { prefs.saveAllWordsRequired(isRequired) }
        page.value = 0
    }

    fun onFileSelectionChange(loadHadiths: Boolean, loadVerses: Boolean) {
        viewModelScope.launch { prefs.saveFileSelection(loadHadiths, loadVerses) }
        page.value = 0
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
    val allSurahNames: List<String> = repository.allSurahNames
    val hadithSurahNames: Set<String> = repository.hadithSurahNames
    val verseSurahNames: Set<String> = repository.verseSurahNames
}

class SearchViewModelFactory(
    private val repository: VerseRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}