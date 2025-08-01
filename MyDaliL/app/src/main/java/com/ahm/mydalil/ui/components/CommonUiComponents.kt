package com.ahm.mydalil.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ahm.mydalil.data.local.room.Bookmark
import com.ahm.mydalil.data.repository.VerseRepository
import java.text.DateFormat
import java.util.Date

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBookmarkClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = onBookmarkClick) { Icon(Icons.Default.Star, "Bookmarks") }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, "Clear search")
                    }
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        IconButton(onClick = onFilterClick) { Icon(Icons.Default.ArrowDropDown, "Filters") }
    }
}


// --- Component: SearchOptions & SourceSelector ---
@Composable
fun SearchOptions(
    showFilters: Boolean,
    allWordsRequired: Boolean,
    onAllWordsRequiredChange: (Boolean) -> Unit,
    loadHadiths: Boolean,
    loadVerses: Boolean,
    onFileSelectionChange: (Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showFilters
    ) {
        Row(
            modifier = modifier.fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                ).padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.toggleable(
                    value = allWordsRequired,
                    onValueChange = onAllWordsRequiredChange,
                    role = Role.Checkbox
                )
                    .padding(end = 8.dp), //.padding(vertical = 6.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = allWordsRequired, onCheckedChange = null, modifier = Modifier.size(20.dp))
                Text("Match all words", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyMedium)
            }

            SourceSelector(
                loadHadiths = loadHadiths,
                loadVerses = loadVerses,
                onSelectionChange = onFileSelectionChange
            )
        }
    }
}

@Composable
fun RoundCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            .clickable(onClick = { onCheckedChange?.invoke(!checked) }),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun SourceSelector(
    loadHadiths: Boolean,
    loadVerses: Boolean,
    onSelectionChange: (hadiths: Boolean, verses: Boolean) -> Unit,
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape)
            .clip(CircleShape)
    ) {
        // Hadiths Toggle
        Row(
            modifier = Modifier.toggleable(
                value = loadHadiths,
                onValueChange = {
                    if (!it && !loadVerses) {
                        Toast.makeText(context, "At least one source must be selected", Toast.LENGTH_SHORT).show()
                    } else {
                        onSelectionChange(it, loadVerses)
                    }
                },
                role = Role.Checkbox
            ).padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = loadHadiths, onCheckedChange = null, modifier = Modifier.size(20.dp))
            Text("Hadiths", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp))
        }
        // Verses Toggle
        Row(
            modifier = Modifier.toggleable(
                value = loadVerses,
                onValueChange = {
                    if (!it && !loadHadiths) {
                        Toast.makeText(context, "At least one source must be selected", Toast.LENGTH_SHORT).show()
                    } else {
                        onSelectionChange(loadHadiths, it)
                    }
                },
                role = Role.Checkbox
            ).padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = loadVerses, onCheckedChange = null, modifier = Modifier.size(20.dp))
            Text("Verses", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp))
        }
    }
}


// --- Component: FilterPanel ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterPanel(
    allSurahNames: List<String>,
    hadithSurahNames: Set<String>,
    verseSurahNames: Set<String>,
    selectedSurahs: Set<String>,
    onFilterChanged: (Set<String>) -> Unit,
    onSurahLongPress: (String) -> Unit,
    loadHadiths: Boolean,
    loadVerses: Boolean
) {
    var filterText by remember { mutableStateOf("") }
    val filteredList = remember(filterText, allSurahNames) {
        if (filterText.isBlank()) allSurahNames else allSurahNames.filter { it.contains(filterText, ignoreCase = true) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Filter by Book/Surah", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                label = { Text("Search books...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.extraLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Button(onClick = { onFilterChanged(allSurahNames.toSet()) }, modifier = Modifier.weight(1f)) { Text("All") }
                Button(onClick = { onFilterChanged(emptySet()) }, modifier = Modifier.weight(1f)) { Text("None") }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                items(filteredList) { surahName ->
                    val isFromHadith = surahName in hadithSurahNames
                    val isFromVerse = surahName in verseSurahNames
                    if ((isFromHadith && loadHadiths) || (isFromVerse && loadVerses)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        val newSelection = selectedSurahs.toMutableSet()
                                        if (selectedSurahs.contains(surahName)) newSelection.remove(
                                            surahName
                                        )
                                        else newSelection.add(surahName)
                                        onFilterChanged(newSelection)
                                    },
                                    onLongClick = { onSurahLongPress(surahName) }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selectedSurahs.contains(surahName), onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(surahName, modifier = Modifier.weight(1f))
                            SourceIndicator(isFromHadith, isFromVerse)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceIndicator(isFromHadith: Boolean, isFromVerse: Boolean) {
    val hadithColor = Color(0xFF4CAF50)
    val verseColor = Color(0xFF2196F3)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (isFromHadith) {
            Box(Modifier.size(8.dp).background(hadithColor, CircleShape))
        }
        if (isFromVerse) {
            Box(Modifier.size(8.dp).background(verseColor, CircleShape))
        }
    }
}


// --- Component: VerseCard ---
@Composable
fun VerseCard(
    result: VerseRepository.VerseSearchResult,
    isBookmarked: Boolean,
    highlightKeywords: List<String>,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    "${result.surahNumber}.${result.verse.verseNumber} ${result.surahName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onToggleBookmark, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HighlightedText(
                text = result.verse.verseText,
                keywords = highlightKeywords,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// --- Component: HighlightedText ---
@Composable
fun HighlightedText(text: String, keywords: List<String>, style: TextStyle) {
    if (keywords.isEmpty() || keywords.all { it.isBlank() }) {
        Text(text, style = style)
        return
    }

    val pattern = remember(keywords) {
        keywords.filter { it.isNotBlank() }.joinToString("|") { Regex.escape(it) }.toRegex(RegexOption.IGNORE_CASE)
    }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        pattern.findAll(text).forEach { matchResult ->
            if (matchResult.range.first > lastIndex) {
                append(text.substring(lastIndex, matchResult.range.first))
            }
            withStyle(style = SpanStyle(
                background = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            ) {
                append(matchResult.value)
            }
            lastIndex = matchResult.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    Text(annotatedString, style = style)
}


// --- Interfaces & Extensions for Content Conversion ---
interface VerseContent {
    val verseText: String
    val surahName: String
    val verseNumber: Int
    val extraInfo: String get() = ""
}

fun VerseRepository.VerseSearchResult.toVerseContent(): VerseContent = object : VerseContent {
    override val verseText: String = this@toVerseContent.verse.verseText
    override val surahName: String = this@toVerseContent.surahName
    override val verseNumber: Int = this@toVerseContent.verse.verseNumber
}

fun Bookmark.toVerseContent(): VerseContent = object : VerseContent {
    override val verseText: String = this@toVerseContent.verseText
    override val surahName: String = this@toVerseContent.surahName
    override val verseNumber: Int = this@toVerseContent.verseNumber
    override val extraInfo: String = buildString {
        if (this@toVerseContent.query.isNotEmpty()) {
            appendLine("Found with search: \"${this@toVerseContent.query}\"")
        }
        append("Bookmarked on: ${DateFormat.getDateTimeInstance().format(
            Date(
                this@toVerseContent.createdAt
            )
        )}")
    }
}