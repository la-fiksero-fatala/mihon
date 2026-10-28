package eu.kanade.tachiyomi.ui.library

import android.view.KeyEvent

class LibraryNavigation {
    var onPageChange: ((Int) -> Unit)? = null
}

internal fun libraryPageDirection(keyCode: Int, volumeKeysEnabled: Boolean): Int? = when (keyCode) {
    KeyEvent.KEYCODE_PAGE_UP -> -1
    KeyEvent.KEYCODE_PAGE_DOWN -> 1
    KeyEvent.KEYCODE_VOLUME_UP -> if (volumeKeysEnabled) -1 else null
    KeyEvent.KEYCODE_VOLUME_DOWN -> if (volumeKeysEnabled) 1 else null
    else -> null
}
