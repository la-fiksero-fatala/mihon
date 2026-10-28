package eu.kanade.presentation.library.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibraryPaginationTest {

    @Test
    fun `automatic capacity excludes partial rows and includes spacing`() {
        libraryPageCapacity(height = 350, rowHeight = 100, spacing = 4, columns = 3, requestedRows = 0) shouldBe 9
        libraryPageCapacity(height = 308, rowHeight = 100, spacing = 4, columns = 3, requestedRows = 0) shouldBe 9
        libraryPageCapacity(height = 307, rowHeight = 100, spacing = 4, columns = 3, requestedRows = 0) shouldBe 6
    }

    @Test
    fun `requested rows are limited to complete rows that fit`() {
        libraryPageCapacity(height = 350, rowHeight = 100, spacing = 4, columns = 3, requestedRows = 2) shouldBe 6
        libraryPageCapacity(height = 350, rowHeight = 100, spacing = 4, columns = 3, requestedRows = 10) shouldBe 9
    }

    @Test
    fun `list capacity uses one entry per row`() {
        libraryPageCapacity(height = 330, rowHeight = 56, spacing = 0, columns = 1, requestedRows = 0) shouldBe 5
    }

    @Test
    fun `last page stays aligned when it is incomplete`() {
        libraryPageStart(anchorIndex = 12, pageSize = 6, itemCount = 14) shouldBe 12
        libraryPageStart(anchorIndex = 18, pageSize = 6, itemCount = 14) shouldBe 12
        libraryPageStart(anchorIndex = -6, pageSize = 6, itemCount = 14) shouldBe 0
    }

    @Test
    fun `capacity changes retain the page containing the anchor`() {
        libraryPageStart(anchorIndex = 12, pageSize = 9, itemCount = 40) shouldBe 9
        libraryPageStart(anchorIndex = 12, pageSize = 4, itemCount = 40) shouldBe 12
    }

    @Test
    fun `shrinking and empty results clamp the current page`() {
        libraryPageStart(anchorIndex = 24, pageSize = 6, itemCount = 8) shouldBe 6
        libraryPageStart(anchorIndex = 24, pageSize = 6, itemCount = 0) shouldBe 0
    }

    @Test
    fun `oversized covers shrink while retaining space for the title`() {
        libraryPageItemWidth(height = 350, rowHeight = 650, itemWidth = 400, padding = 8) shouldBe 200
        libraryPageItemWidth(height = 650, rowHeight = 650, itemWidth = 400, padding = 8) shouldBe 400
    }
}
