package eu.kanade.tachiyomi.ui.library

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LibraryNavigation {
    var onPageChange: ((Int) -> Unit)? = null

    var pageInfo: LibraryPageInfo? by mutableStateOf(null)
}

data class LibraryPageInfo(val index: Int, val count: Int)

internal fun libraryPageDirection(keyCode: Int, volumeKeysEnabled: Boolean): Int? = when (keyCode) {
    KeyEvent.KEYCODE_PAGE_UP -> -1
    KeyEvent.KEYCODE_PAGE_DOWN -> 1
    KeyEvent.KEYCODE_VOLUME_UP -> if (volumeKeysEnabled) -1 else null
    KeyEvent.KEYCODE_VOLUME_DOWN -> if (volumeKeysEnabled) 1 else null
    else -> null
}
