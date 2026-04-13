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

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // original — позиция в тексте без двоеточий (0-12 символов)
                val hexPos = minOf(offset, 12)
                // Сколько двоеточий будет перед этой позицией
                val colons = if (hexPos > 0) (hexPos - 1) / 2 else 0
                return hexPos + colons
            }

            override fun transformedToOriginal(offset: Int): Int {
                // transformed — позиция в тексте с двоеточиями (0-17 символов)
                if (offset <= 0) return 0
                // Сколько hex-символов до этой позиции
                // Каждые 3 символа в transformed = 2 hex + 1 двоеточие
                val groups = offset / 3
                val remainder = offset % 3
                val hexCount = groups * 2 + minOf(remainder, 2)
                return minOf(hexCount, 12)
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
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
