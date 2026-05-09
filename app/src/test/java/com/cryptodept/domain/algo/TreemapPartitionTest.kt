package com.cryptodept.domain.algo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreemapPartitionTest {
    private val partition = TreemapPartition()

    @Test
    fun `squarify empty list returns empty list`() {
        val result = partition.squarify(emptyList(), 100f, 100f)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `squarify single item takes full area`() {
        val items = listOf(TreemapItem("BTC", 1000.0, 5.0))
        val result = partition.squarify(items, 100f, 100f)

        assertEquals(1, result.size)
        assertEquals(0f, result[0].x)
        assertEquals(0f, result[0].y)
        assertEquals(100f, result[0].width)
        assertEquals(100f, result[0].height)
    }

    @Test
    fun `squarify items sum up to total area`() {
        val items =
            listOf(
                TreemapItem("BTC", 500.0, 2.0),
                TreemapItem("ETH", 300.0, -1.0),
                TreemapItem("SOL", 200.0, 5.0),
            )
        val width = 1000f
        val height = 1000f
        val result = partition.squarify(items, width, height)

        assertEquals(3, result.size)
        val totalCalculatedArea = result.sumOf { (it.width * it.height).toDouble() }
        assertEquals((width * height).toDouble(), totalCalculatedArea, 0.01)
    }
}
