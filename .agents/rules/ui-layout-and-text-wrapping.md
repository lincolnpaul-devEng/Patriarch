# Global Rule: UI Layout & Strict Anti-Vertical Text Squeezing Policy

## 1. Zero Vertical-Character Text Wrapping Rule
In Android Jetpack Compose, when multiple elements share a horizontal `Row` or `Arrangement.SpaceBetween`, child components without explicit flex weights or constraints can compress adjacent badge boxes, pills, or text labels down to single-character column widths (e.g. `C\nA\nM\nP\nU\nS\n \nM\nA\nS\nT\nE\nR`).

This is strictly prohibited across the entire application.

## 2. Mandatory Row Layout Pattern
Whenever building a row with a primary label/title and a trailing badge, chip, or status pill:

1. **Title / Primary Content:**
   - Must use `Modifier.weight(1f, fill = false)` or `Modifier.weight(1f)`.
   - Must specify `maxLines = 1` (or fixed 2 lines) and `overflow = TextOverflow.Ellipsis`.
2. **Trailing Badge / Pill / Chip:**
   - Must specify `maxLines = 1`.
   - Must specify `softWrap = false`.
   - May specify `Modifier.widthIn(min = ...)` or explicit padding without allowing narrow letter wrapping.
3. **Price & Numerical Values:**
   - Always set `maxLines = 1, softWrap = false` to guarantee currency symbols and numbers stay intact on one line.
