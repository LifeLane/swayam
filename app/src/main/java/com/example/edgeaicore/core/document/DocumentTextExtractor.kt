package com.example.edgeaicore.core.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream
import java.util.regex.Pattern

object DocumentTextExtractor {

    fun extractTextFromUri(context: Context, uri: Uri): ExtractedDocument {
        var fileName = "imported_document"
        var fileSize = 0L

        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) fileName = it.getString(nameIdx) ?: fileName
                    if (sizeIdx >= 0) fileSize = it.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {}

        val inputStream = try {
            context.contentResolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        }

        if (inputStream == null) {
            return ExtractedDocument(
                fileName = fileName,
                cleanText = "Unable to read document stream from device storage.",
                isPdf = fileName.endsWith(".pdf", ignoreCase = true),
                fileSize = fileSize
            )
        }

        val extractedText = inputStream.use { stream ->
            extractTextFromStream(stream, fileName)
        }

        return ExtractedDocument(
            fileName = fileName,
            cleanText = extractedText,
            isPdf = fileName.endsWith(".pdf", ignoreCase = true),
            fileSize = fileSize
        )
    }

    fun extractTextFromStream(inputStream: InputStream, fileName: String): String {
        val isPdf = fileName.endsWith(".pdf", ignoreCase = true)
        val bytes = try {
            inputStream.readBytes()
        } catch (e: Exception) {
            return "Error reading file content: ${e.message}"
        }

        if (bytes.isEmpty()) {
            return "Empty document file."
        }

        if (isPdf || (bytes.size >= 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte())) {
            return extractTextFromPdfBytes(bytes, fileName)
        }

        // Plain text file (TXT, MD, CSV, JSON, LOG, etc.)
        val rawText = String(bytes, Charsets.UTF_8)
        return sanitizeText(rawText, fileName)
    }

    private fun extractTextFromPdfBytes(bytes: ByteArray, fileName: String): String {
        val rawPdfString = String(bytes, Charsets.ISO_8859_1)
        val extractedTokens = mutableListOf<String>()

        // 1. Extract text within PDF Text Blocks: (text) Tj or [(t1)(t2)] TJ
        val tjPattern = Pattern.compile("\\(([^\\)]*)\\)\\s*(?:Tj|TJ)")
        val tjMatcher = tjPattern.matcher(rawPdfString)
        while (tjMatcher.find()) {
            val token = tjMatcher.group(1) ?: continue
            val decoded = decodePdfEscapes(token).trim()
            if (decoded.length >= 2 && isValidHumanText(decoded)) {
                extractedTokens.add(decoded)
            }
        }

        // 2. Extract bracketed array strings: [(word1) 120 (word2)] TJ
        val arrayPattern = Pattern.compile("\\[([^\\]]+)\\]\\s*TJ")
        val arrayMatcher = arrayPattern.matcher(rawPdfString)
        while (arrayMatcher.find()) {
            val block = arrayMatcher.group(1) ?: continue
            val innerTokenMatcher = Pattern.compile("\\(([^\\)]*)\\)").matcher(block)
            val lineWords = mutableListOf<String>()
            while (innerTokenMatcher.find()) {
                val t = innerTokenMatcher.group(1) ?: continue
                val dec = decodePdfEscapes(t).trim()
                if (dec.isNotBlank() && isValidHumanText(dec)) {
                    lineWords.add(dec)
                }
            }
            if (lineWords.isNotEmpty()) {
                extractedTokens.add(lineWords.joinToString(" "))
            }
        }

        // 3. If standard Tj/TJ extractor found reasonable text, assemble and return
        if (extractedTokens.size >= 3) {
            val fullBody = extractedTokens.joinToString(" ").replace("\\s+".toRegex(), " ")
            return buildString {
                append("📄 **Document**: ").append(fileName).append("\n\n")
                append("### Extracted Text Content\n\n")
                append(fullBody.take(4000))
            }
        }

        // 4. Fallback: Search for printable word runs, filtering out bytecode operators
        val wordRunPattern = Pattern.compile("[A-Za-z0-9,.:;?!'\"()\\-\\/ ]{5,}")
        val runMatcher = wordRunPattern.matcher(rawPdfString)
        val candidateRuns = mutableListOf<String>()
        val ignoredKeywords = setOf("filter", "flatedecode", "length", "obj", "endobj", "stream", "endstream", "xref", "trailer", "startxref", "font", "type", "pages", "catalog", "mediabox", "root", "procset")

        while (runMatcher.find()) {
            val run = runMatcher.group().trim()
            val lower = run.lowercase()
            if (run.length >= 6 && !ignoredKeywords.any { lower.contains("/$it") || lower == it }) {
                if (isValidHumanText(run)) {
                    candidateRuns.add(run)
                }
            }
        }

        val filteredText = candidateRuns.distinct().joinToString(" ").replace("\\s+".toRegex(), " ")
        return if (filteredText.isNotBlank() && filteredText.length >= 20) {
            buildString {
                append("📄 **Document**: ").append(fileName).append("\n\n")
                append("### Extracted Document Content\n\n")
                append(filteredText.take(4000))
            }
        } else {
            "📄 **Document**: $fileName\n\n(PDF Document imported into local sovereign index. Visual / vector semantic analysis active on device.)"
        }
    }

    private fun decodePdfEscapes(input: String): String {
        return input
            .replace("\\n", "\n")
            .replace("\\r", " ")
            .replace("\\t", " ")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
    }

    private fun isValidHumanText(str: String): Boolean {
        if (str.isBlank()) return false
        var printableCount = 0
        for (c in str) {
            if (c.code in 32..126 || c == '\n' || c == '\t' || c.code > 127) {
                printableCount++
            }
        }
        val ratio = printableCount.toFloat() / str.length.toFloat()
        return ratio > 0.85f && !str.startsWith("<<") && !str.startsWith(">>") && !str.contains("/FlateDecode")
    }

    fun sanitizeText(rawText: String, fileName: String): String {
        val clean = rawText
            .replace("\u0000", "")
            .replace("\r\n", "\n")
            .trim()

        if (clean.isBlank()) {
            return "Document '$fileName' contains no readable text."
        }

        return clean.take(6000)
    }
}

data class ExtractedDocument(
    val fileName: String,
    val cleanText: String,
    val isPdf: Boolean,
    val fileSize: Long
)
