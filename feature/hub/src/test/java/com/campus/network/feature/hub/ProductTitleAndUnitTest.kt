package com.campus.network.feature.hub

import com.campus.network.feature.hub.market.extractCleanTitleAndUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductTitleAndUnitTest {

    @Test
    fun testWeightInTitleStrippedAndUnitExtracted() {
        val result1 = extractCleanTitleAndUnit("Organic Kenya Roast Coffee Beans 1kg", "Groceries")
        assertEquals("Organic Kenya Roast Coffee Beans", result1.cleanTitle)
        assertEquals("kg", result1.baseUnit)
        assertEquals("1 kg", result1.formattedQuantity(1))
        assertEquals("3 kg", result1.formattedQuantity(3))

        val result2 = extractCleanTitleAndUnit("Organic Kenya Roast Coffee Beans (1 kilogram)", "Groceries")
        assertEquals("Organic Kenya Roast Coffee Beans", result2.cleanTitle)
        assertEquals("kg", result2.baseUnit)

        val result3 = extractCleanTitleAndUnit("Fresh Apples - 1 kg", "Groceries")
        assertEquals("Fresh Apples", result3.cleanTitle)
        assertEquals("kg", result3.baseUnit)
    }

    @Test
    fun testPieceAndItemInTitleStrippedAndUnitExtracted() {
        val result1 = extractCleanTitleAndUnit("Drafting Board 1 piece", "Stationery")
        assertEquals("Drafting Board", result1.cleanTitle)
        assertEquals("piece", result1.baseUnit)
        assertEquals("1 piece", result1.formattedQuantity(1))
        assertEquals("3 pieces", result1.formattedQuantity(3))

        val result2 = extractCleanTitleAndUnit("Engineering Drawing Board (1 pc)", "Stationery")
        assertEquals("Engineering Drawing Board", result2.cleanTitle)
        assertEquals("piece", result2.baseUnit)
    }

    @Test
    fun testLiquidAndPackInTitleStripped() {
        val result1 = extractCleanTitleAndUnit("Fresh Whole Milk 1L", "Groceries")
        assertEquals("Fresh Whole Milk", result1.cleanTitle)
        assertEquals("liter", result1.baseUnit)
        assertEquals("2 liters", result1.formattedQuantity(2))

        val result2 = extractCleanTitleAndUnit("Highlighter Pens 1 pack", "Stationery")
        assertEquals("Highlighter Pens", result2.cleanTitle)
        assertEquals("pack", result2.baseUnit)
        assertEquals("4 packs", result2.formattedQuantity(4))
    }

    @Test
    fun testCleanTitlePreservedWithoutUnits() {
        val result = extractCleanTitleAndUnit("TI-84 Plus CE Calculator", "Electronics")
        assertEquals("TI-84 Plus CE Calculator", result.cleanTitle)
        assertEquals("item", result.baseUnit)
        assertEquals("1 item", result.formattedQuantity(1))
        assertEquals("2 items", result.formattedQuantity(2))
    }
}
