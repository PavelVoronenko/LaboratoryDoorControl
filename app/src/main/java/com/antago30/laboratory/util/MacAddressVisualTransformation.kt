package com.antago30.laboratory.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MacAddressVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val cleaned = text.text.replace(":", "")
        val trimmed = cleaned.take(12).uppercase()

        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 2 == 1 && i < 11) out += ":"
        }

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val hexCount = minOf(offset, 12)
                val colonsBefore = if (hexCount > 0) (hexCount - 1) / 2 else 0
                return hexCount + colonsBefore
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val hexPositions = offset - (offset - 1) / 2
                return minOf(hexPositions, 12)
            }
        }

        return TransformedText(AnnotatedString(out), offsetTranslator)
    }

}

/**
 * Форматирует MAC-адрес с двоеточиями: AABBCCDDEEFF -> AA:BB:CC:DD:EE:FF
 */
fun formatMacAddress(mac: String): String {
    val cleaned = mac.uppercase().replace(":", "")
    return buildString {
        for (i in cleaned.indices step 2) {
            if (isNotEmpty()) append(":")
            append(cleaned.substring(i, minOf(i + 2, cleaned.length)))
        }
    }
}