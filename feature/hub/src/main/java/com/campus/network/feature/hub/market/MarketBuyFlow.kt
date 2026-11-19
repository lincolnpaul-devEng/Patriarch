package com.campus.network.feature.hub.market

import com.campus.network.core.model.MarketItem

/**
 * A buyer's chosen configuration for one listing: quantity + note, carried into Safe Trade escrow.
 */
data class ProductBasketConfig(
    val item: MarketItem,
    val handoverOptionTitle: String,
    val handoverFee: Double,
    val packageOptionTitle: String,
    val packageExtraPrice: Double,
    val quantity: Int,
    val buyerNote: String,
    val isOrderConfirmedInEscrow: Boolean = false,
    val orderReference: String = "ORD-${(1000..9999).random()}",
    /** Populated only after PaymentProvider successfully creates a hold. */
    val escrowOrderId: String? = null,
    val providerHoldId: String? = null,
    val paymentMethodId: String? = null,
    val escrowStatus: String? = null
) {
    val unitPriceWithOptions: Double
        get() = item.price + handoverFee + packageExtraPrice

    val totalAmount: Double
        get() = unitPriceWithOptions * quantity
}

/**
 * Encapsulates the separated clean title and dynamic measurement unit.
 */
data class ProductTitleAndUnit(
    val cleanTitle: String,
    val baseUnit: String, // e.g. "kg", "g", "liter", "piece", "item", "pack", "set", "pair", "bottle"
    val unitPlural: String // e.g. "kg", "g", "liters", "pieces", "items", "packs", "sets", "pairs", "bottles"
) {
    fun formattedQuantity(quantity: Int): String {
        return if (quantity == 1) "1 $baseUnit" else "$quantity $unitPlural"
    }

    fun unitDisplayLabel(quantity: Int): String {
        return if (quantity == 1) baseUnit else unitPlural
    }
}

/**
 * Separates specific weight, volume, or quantity references from the product title
 * so the title describes the item itself while quantity/units are handled dynamically
 * in the interactive action controls.
 */
fun extractCleanTitleAndUnit(rawTitle: String, category: String = ""): ProductTitleAndUnit {
    val trimmed = rawTitle.trim()

    // 1. Check parenthetical units like "(1 kg)", "(1 kilogram)", "(1kg)", "(500g)", "(1 piece)", "(1pc)"
    val parenPattern = Regex(
        """(?i)\s*\(\s*(?:\b\d+(?:\.\d+)?\s*)?(kg|kilograms?|kilo|kilos|grams?|g|liters?|litres?|l|ml|pcs?|pieces?|units?|packs?|sets?|pairs?|bottles?|boxes?|box)\s*\)"""
    )
    val parenMatch = parenPattern.find(trimmed)
    if (parenMatch != null) {
        val rawUnit = parenMatch.groupValues[1].lowercase()
        val cleaned = trimmed.replace(parenPattern, "").replace(Regex("""\s{2,}"""), " ").trim()
        val (base, plural) = mapUnitToLabels(rawUnit)
        if (cleaned.isNotBlank()) {
            return ProductTitleAndUnit(cleaned, base, plural)
        }
    }

    // 2. Check suffix unit patterns like " - 1kg", " 1 kilogram", " 1 kg", " 1000g", " 1 piece", " 1 pc", " 1L"
    val suffixPattern = Regex(
        """(?i)(?:[-–—\s,]+|\s*\()(?:\b\d+(?:\.\d+)?\s*)?(kg|kilograms?|kilo|kilos|grams?|g|liters?|litres?|l|ml|pcs?|pieces?|units?|packs?|sets?|pairs?|bottles?|boxes?|box)\b\s*\)?$"""
    )
    val suffixMatch = suffixPattern.find(trimmed)
    if (suffixMatch != null) {
        val rawUnit = suffixMatch.groupValues[1].lowercase()
        val cleaned = trimmed.substring(0, suffixMatch.range.first).trim().trimEnd('-', '–', '—', ',', '(').trim()
        val (base, plural) = mapUnitToLabels(rawUnit)
        if (cleaned.isNotBlank()) {
            return ProductTitleAndUnit(cleaned, base, plural)
        }
    }

    // 3. Fallback based on category
    val cat = category.lowercase()
    val (defaultBase, defaultPlural) = when {
        cat.contains("grocer") || cat.contains("food") || cat.contains("produce") -> "kg" to "kg"
        cat.contains("beverage") || cat.contains("drink") -> "bottle" to "bottles"
        cat.contains("book") || cat.contains("course") -> "copy" to "copies"
        else -> "item" to "items"
    }

    return ProductTitleAndUnit(
        cleanTitle = trimmed,
        baseUnit = defaultBase,
        unitPlural = defaultPlural
    )
}

private fun mapUnitToLabels(raw: String): Pair<String, String> {
    return when {
        raw.startsWith("kg") || raw.startsWith("kilo") -> "kg" to "kg"
        raw.startsWith("gram") || raw == "g" -> "g" to "g"
        raw.startsWith("liter") || raw.startsWith("litre") || raw == "l" -> "liter" to "liters"
        raw.startsWith("ml") -> "ml" to "ml"
        raw.startsWith("pack") -> "pack" to "packs"
        raw.startsWith("set") -> "set" to "sets"
        raw.startsWith("pair") -> "pair" to "pairs"
        raw.startsWith("box") -> "box" to "boxes"
        raw.startsWith("bottle") -> "bottle" to "bottles"
        raw.startsWith("pc") || raw.startsWith("piece") -> "piece" to "pieces"
        else -> "item" to "items"
    }
}
