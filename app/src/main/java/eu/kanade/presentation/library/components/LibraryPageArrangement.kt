package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
internal class LibraryPageArrangement(override val spacing: Dp = 0.dp) : Arrangement.HorizontalOrVertical {

    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        with(if (sizes.size == 1) Arrangement.Center else Arrangement.SpaceBetween) {
            arrange(totalSize, sizes, outPositions)
        }
    }

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        with(if (sizes.size == 1) Arrangement.Center else Arrangement.SpaceBetween) {
            arrange(totalSize, sizes, layoutDirection, outPositions)
        }
    }
}
