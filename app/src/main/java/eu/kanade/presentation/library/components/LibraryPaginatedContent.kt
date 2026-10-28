package eu.kanade.presentation.library.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.LibraryNavigation
import eu.kanade.tachiyomi.ui.library.LibraryPageInfo
import kotlinx.coroutines.flow.filterNotNull
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryPageButtons
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

@Composable
internal fun LibraryPaginatedContent(
    items: List<LibraryItem>,
    navigation: LibraryNavigation,
    active: Boolean,
    listState: LazyListState,
    gridState: LazyGridState,
    displayMode: LibraryDisplayMode,
    columns: Int,
    requestedRows: Int,
    buttons: LibraryPageButtons,
    contentPadding: PaddingValues,
    searchQuery: String?,
    hasActiveFilters: Boolean,
    onGlobalSearchClicked: () -> Unit,
    content: @Composable (List<LibraryItem>, Dp) -> Unit,
) {
    val isList = displayMode == LibraryDisplayMode.List
    var anchorId by rememberSaveable(searchQuery, hasActiveFilters) { mutableStateOf<Long?>(null) }
    var anchorIndex by rememberSaveable(searchQuery, hasActiveFilters) { mutableIntStateOf(0) }
    var pageSize by remember(displayMode, columns, requestedRows, buttons) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var maxItemWidth by remember(viewport, displayMode, columns, density) { mutableStateOf(Dp.Infinity) }
    val gridPadding = with(density) { 16.dp.roundToPx() }
    val columnSpacing = with(density) { CommonMangaItemDefaults.GridHorizontalSpacer.roundToPx() }
    val adaptiveColumnWidth = with(density) { 128.dp.roundToPx() }
    val itemPadding = with(density) { 8.dp.roundToPx() }

    LaunchedEffect(viewport, displayMode, columns, requestedRows, buttons, density) {
        if (isList) listState.scrollToItem(0) else gridState.scrollToItem(0)
        snapshotFlow {
            if (isList) {
                val info = listState.layoutInfo
                val row = info.visibleItemsInfo.firstOrNull { it.contentType == "library_list_item" }
                row?.let {
                    libraryPageCapacity(
                        info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding,
                        it.size,
                        0,
                        1,
                        requestedRows,
                    ) to null
                }
            } else {
                val info = gridState.layoutInfo
                val row = info.visibleItemsInfo.firstOrNull()
                val columnCount = if (columns > 0) {
                    columns
                } else {
                    (
                        (info.viewportSize.width - gridPadding + columnSpacing) /
                            (adaptiveColumnWidth + columnSpacing)
                        ).coerceAtLeast(1)
                }
                row?.let {
                    val height = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
                    val widthLimit = if (height > 0 && it.size.height > height) {
                        libraryPageItemWidth(height, it.size.height, it.size.width, itemPadding)
                    } else {
                        null
                    }
                    libraryPageCapacity(
                        height,
                        it.size.height,
                        info.mainAxisItemSpacing,
                        columnCount,
                        requestedRows,
                    ) to widthLimit
                }
            }
        }.filterNotNull().collect { (capacity, widthLimit) ->
            pageSize = capacity
            if (widthLimit != null) {
                maxItemWidth = minOf(maxItemWidth, with(density) { widthLimit.toDp() })
            }
        }
    }

    val size = pageSize.coerceAtLeast(1)
    fun currentPageStart(): Int {
        val currentAnchor = items.indexOfFirst { it.id == anchorId }.takeIf { it >= 0 } ?: anchorIndex
        return libraryPageStart(currentAnchor, size, items.size)
    }
    val start = currentPageStart()
    val page = start / size
    val pageCount = (items.size - 1) / size + 1
    val onPageChange: (Int) -> Unit = { direction ->
        val next = libraryPageStart(currentPageStart() + direction * size, size, items.size)
        anchorIndex = next
        anchorId = items[next].id
    }
    val currentOnPageChange by rememberUpdatedState(onPageChange)
    val handler: (Int) -> Unit = remember { { currentOnPageChange(it) } }
    SideEffect {
        if (active && pageSize > 0) navigation.pageInfo = LibraryPageInfo(page, pageCount)
    }
    DisposableEffect(navigation, active) {
        if (active) navigation.onPageChange = handler
        onDispose {
            if (navigation.onPageChange === handler) {
                navigation.onPageChange = null
                navigation.pageInfo = null
            }
        }
    }

    val swipeThreshold = with(density) { 48.dp.toPx() }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        if (!searchQuery.isNullOrEmpty()) {
            GlobalSearchItem(searchQuery = searchQuery, onClick = onGlobalSearchClicked)
        }
        Row(Modifier.weight(1f)) {
            if (buttons == LibraryPageButtons.Left) {
                LibraryPageControls(page, pageCount, true, onPageChange)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (buttons == LibraryPageButtons.Top) {
                    LibraryPageControls(page, pageCount, false, onPageChange)
                }
                Box(
                    Modifier.weight(1f)
                        .onSizeChanged { viewport = it }
                        .alpha(if (pageSize == 0) 0f else 1f)
                        .pointerInput(swipeThreshold) {
                            var distance = 0f
                            detectVerticalDragGestures(
                                onDragStart = { distance = 0f },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    distance += amount
                                },
                                onDragEnd = {
                                    if (abs(distance) >= swipeThreshold) {
                                        currentOnPageChange(if (distance < 0) 1 else -1)
                                    }
                                },
                            )
                        },
                ) {
                    content(items.subList(start, (start + size).coerceAtMost(items.size)), maxItemWidth)
                }
                if (buttons == LibraryPageButtons.Bottom) {
                    LibraryPageControls(page, pageCount, false, onPageChange)
                }
            }
            if (buttons == LibraryPageButtons.Right) {
                LibraryPageControls(page, pageCount, true, onPageChange)
            }
        }
    }
}

@Composable
private fun LibraryPageControls(page: Int, pageCount: Int, vertical: Boolean, onPageChange: (Int) -> Unit) {
    val controls: @Composable () -> Unit = {
        IconButton(onClick = { onPageChange(-1) }, enabled = page > 0) {
            Icon(MaterialSymbols.Rounded.KeyboardArrowUp, stringResource(MR.strings.spen_previous_page))
        }
        LibraryPageNumber(page, pageCount)
        IconButton(onClick = { onPageChange(1) }, enabled = page < pageCount - 1) {
            Icon(MaterialSymbols.Rounded.KeyboardArrowDown, stringResource(MR.strings.spen_next_page))
        }
    }
    if (vertical) {
        Column(
            modifier = Modifier.fillMaxHeight().width(72.dp).padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { controls() }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) { controls() }
    }
}

@Composable
internal fun LibraryPageNumber(page: Int, pageCount: Int, compact: Boolean = false, modifier: Modifier = Modifier) {
    val description = stringResource(MR.strings.library_page_number, page + 1, pageCount)
    val textModifier = if (compact) Modifier.padding(horizontal = 8.dp, vertical = 4.dp) else modifier
    val number: @Composable () -> Unit = {
        Text(
            text = "${page + 1} / $pageCount",
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            modifier = textModifier.semantics { contentDescription = description },
        )
    }
    if (compact) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            number()
        }
    } else {
        number()
    }
}

internal fun libraryPageCapacity(height: Int, rowHeight: Int, spacing: Int, columns: Int, requestedRows: Int): Int {
    val fittingRows = ((height + spacing) / (rowHeight + spacing).coerceAtLeast(1)).coerceAtLeast(1)
    val rows = if (requestedRows > 0) requestedRows.coerceAtMost(fittingRows) else fittingRows
    return rows * columns.coerceAtLeast(1)
}

internal fun libraryPageStart(anchorIndex: Int, pageSize: Int, itemCount: Int): Int {
    val size = pageSize.coerceAtLeast(1)
    return anchorIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0)) / size * size
}

// Only shrink oversized grid entries; retain their cover aspect ratio and measured title height.
internal fun libraryPageItemWidth(height: Int, rowHeight: Int, itemWidth: Int, padding: Int): Int {
    val coverHeight = (itemWidth - padding) * 3 / 2
    val titleHeight = rowHeight - coverHeight
    return ((height - titleHeight) * 2 / 3 + padding).coerceIn(padding + 1, itemWidth.coerceAtLeast(padding + 1))
}
