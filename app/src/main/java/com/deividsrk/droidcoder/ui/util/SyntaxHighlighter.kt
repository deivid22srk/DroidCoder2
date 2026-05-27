package com.deividsrk.droidcoder.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

fun highlightCode(text: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        val length = text.length

        while (index < length) {
            val char = text[index]

            // 1. Comments (Line comments //)
            if (char == '/' && index + 1 < length && text[index + 1] == '/') {
                val commentText = text.substring(index)
                withStyle(SpanStyle(color = Color(0xFF6272A4))) { // Dracula comment gray
                    append(commentText)
                }
                break
            }

            // 2. String Literals
            if (char == '"' || char == '\'') {
                val quote = char
                val start = index
                index++
                var escaped = false
                while (index < length) {
                    val c = text[index]
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == quote) {
                        index++
                        break
                    }
                    index++
                }
                val stringVal = text.substring(start, minOf(index, length))
                withStyle(SpanStyle(color = Color(0xFFA6E22E))) { // String green
                    append(stringVal)
                }
                continue
            }

            // 3. Numbers
            if (char.isDigit()) {
                val start = index
                while (index < length && (text[index].isDigit() || text[index] == '.' || text[index] == 'f' || text[index] == 'L')) {
                    index++
                }
                val numVal = text.substring(start, index)
                withStyle(SpanStyle(color = Color(0xFFBD93F9))) { // Number purple
                    append(numVal)
                }
                continue
            }

            // 4. Identifiers (Keywords, Types, Annotations)
            if (char.isLetter() || char == '_' || char == '@') {
                val start = index
                if (text[index] == '@') {
                    index++
                }
                while (index < length && (text[index].isLetterOrDigit() || text[index] == '_')) {
                    index++
                }
                val word = text.substring(start, index)

                val isKeyword = word in setOf(
                    "class", "interface", "fun", "val", "var", "import", "package", "return", 
                    "if", "else", "while", "for", "in", "object", "private", "public", "protected",
                    "internal", "external", "suspend", "null", "true", "false", "this", "super",
                    "override", "const", "companion", "data", "void", "int", "float", "double", 
                    "bool", "char", "include", "define", "struct", "template", "typename"
                )

                val isType = word.isNotEmpty() && word[0].isUpperCase() && !word.startsWith("@")
                val isAnnotation = word.startsWith("@")

                when {
                    isKeyword -> {
                        withStyle(SpanStyle(color = Color(0xFFFF79C6), fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    }
                    isType -> {
                        withStyle(SpanStyle(color = Color(0xFF8BE9FD))) {
                            append(word)
                        }
                    }
                    isAnnotation -> {
                        withStyle(SpanStyle(color = Color(0xFFFFB86C))) {
                            append(word)
                        }
                    }
                    else -> {
                        withStyle(SpanStyle(color = Color(0xFFF8F8F2))) {
                            append(word)
                        }
                    }
                }
                continue
            }

            // 5. General characters (Operators / brackets)
            withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                append(char)
            }
            index++
        }
    }
}

class DraculaSyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = highlightCode(text.text),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
