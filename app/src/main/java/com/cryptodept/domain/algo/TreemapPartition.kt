package com.cryptodept.domain.algo

data class TreemapItem(
    val symbol: String,
    val value: Double, // typically market cap
    val change24h: Double,
)

data class TreemapRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val item: TreemapItem,
)

class TreemapPartition {
    /**
     * Squarified Treemap Algorithm.
     * Divides the given rectangle into sub-rectangles based on item values.
     */
    fun squarify(
        items: List<TreemapItem>,
        width: Float,
        height: Float,
    ): List<TreemapRect> {
        if (items.isEmpty() || width <= 0 || height <= 0) return emptyList()

        // Sort items by value descending for better aspect ratios
        val sortedItems = items.sortedByDescending { it.value }
        val totalValue = sortedItems.sumOf { it.value }

        // Normalize values to area
        val normalizedItems = sortedItems.map { it to (it.value / totalValue * (width * height)) }

        val results = mutableListOf<TreemapRect>()
        process(normalizedItems, width, height, 0f, 0f, results)
        return results
    }

    private fun process(
        items: List<Pair<TreemapItem, Double>>,
        w: Float,
        h: Float,
        x: Float,
        y: Float,
        results: MutableList<TreemapRect>,
    ) {
        if (items.isEmpty()) return

        val isHorizontal = w >= h
        val side = if (isHorizontal) h else w

        var i = 1
        while (i <= items.size) {
            val currentGroup = items.subList(0, i)
            val nextGroup = if (i < items.size) items.subList(0, i + 1) else null

            if (nextGroup == null || worseAspect(currentGroup, nextGroup, side)) {
                // Layout current group
                val groupArea = currentGroup.sumOf { it.second }
                val groupWidth = if (isHorizontal) (groupArea / side).toFloat() else side
                val groupHeight = if (isHorizontal) side else (groupArea / side).toFloat()

                var currentX = x
                var currentY = y

                for (itemPair in currentGroup) {
                    val itemArea = itemPair.second
                    val itemW = if (isHorizontal) groupWidth else (itemArea / groupHeight).toFloat()
                    val itemH = if (isHorizontal) (itemArea / groupWidth).toFloat() else groupHeight

                    results.add(TreemapRect(currentX, currentY, itemW, itemH, itemPair.first))

                    if (isHorizontal) {
                        currentY += itemH
                    } else {
                        currentX += itemW
                    }
                }

                // Recurse with remaining items
                val remainingItems = items.subList(i, items.size)
                if (isHorizontal) {
                    process(remainingItems, w - groupWidth, h, x + groupWidth, y, results)
                } else {
                    process(remainingItems, w, h - groupHeight, x, y + groupHeight, results)
                }
                break
            }
            i++
        }
    }

    private fun worseAspect(
        current: List<Pair<TreemapItem, Double>>,
        next: List<Pair<TreemapItem, Double>>,
        side: Float,
    ): Boolean = aspect(current, side) < aspect(next, side)

    private fun aspect(
        items: List<Pair<TreemapItem, Double>>,
        side: Float,
    ): Double {
        if (items.isEmpty()) return Double.MAX_VALUE
        val sum = items.sumOf { it.second }
        val max = items.maxOf { it.second }
        val min = items.minOf { it.second }
        return kotlin.math.max((side * side * max) / (sum * sum), (sum * sum) / (side * side * min))
    }
}
