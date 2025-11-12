package com.example.roommade.ml

import android.content.Context
import java.text.Normalizer
import java.util.Locale

class WordPieceTokenizer private constructor(
    private val vocab: Map<String, Int>,
    private val unkId: Int,
    private val clsId: Int,
    private val sepId: Int,
    private val padId: Int,
    private val maxSeqLen: Int,
    private val doLowerCase: Boolean
) {

    data class Encoding(
        val inputIds: IntArray,
        val attentionMask: IntArray,
        val tokenTypeIds: IntArray
    )

    fun encode(rawText: String): Encoding {
        val text = rawText.trim()
        val tokens = mutableListOf<Int>()
        tokens += clsId

        if (text.isNotEmpty()) {
            val basicTokens = basicTokenize(text)
            val contentBudget = (maxSeqLen - 2).coerceAtLeast(0)
            var used = 0
            for (token in basicTokens) {
                if (used >= contentBudget) break
                val pieces = wordPiece(token)
                if (used + pieces.size > contentBudget) {
                    val remain = contentBudget - used
                    if (remain <= 0) break
                    tokens += pieces.take(remain)
                    used = contentBudget
                    break
                } else {
                    tokens += pieces
                    used += pieces.size
                }
            }
        }

        tokens += sepId

        val padded = IntArray(maxSeqLen) { padId }
        val mask = IntArray(maxSeqLen)
        val types = IntArray(maxSeqLen)
        val limit = minOf(tokens.size, maxSeqLen)
        for (i in 0 until limit) {
            padded[i] = tokens[i]
            mask[i] = 1
        }
        if (limit < maxSeqLen) {
            // ensure mask is zero for padding (already 0)
        }
        return Encoding(
            inputIds = padded,
            attentionMask = mask,
            tokenTypeIds = types
        )
    }

    private fun basicTokenize(text: String): List<String> {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val result = mutableListOf<String>()
        val builder = StringBuilder()

        fun flushBuilder() {
            if (builder.isNotEmpty()) {
                result += builder.toString()
                builder.clear()
            }
        }

        for (ch in normalized) {
            when {
                ch.isWhitespace() -> flushBuilder()
                ch.isPunctuation() -> {
                    flushBuilder()
                    result += ch.toString()
                }
                else -> builder.append(ch)
            }
        }
        flushBuilder()
        return result
    }

    private fun wordPiece(token: String): List<Int> {
        if (token.isEmpty()) return listOf(unkId)
        val variants = if (doLowerCase) {
            listOf(token, token.lowercase(Locale.US)).distinct()
        } else {
            listOf(token)
        }
        for (variant in variants) {
            val ids = mutableListOf<Int>()
            var start = 0
            var failed = false
            val length = variant.length
            while (start < length) {
                var end = length
                var matched: Int? = null
                while (start < end) {
                    val substr = variant.substring(start, end)
                    val candidate = if (start == 0) substr else "##$substr"
                    val id = vocab[candidate]
                    if (id != null) {
                        matched = id
                        break
                    }
                    end--
                }
                if (matched == null) {
                    failed = true
                    break
                }
                ids += matched
                start = end
            }
            if (!failed && ids.isNotEmpty()) {
                return ids
            }
        }
        return listOf(unkId)
    }

    private fun Char.isWhitespace(): Boolean {
        val type = Character.getType(this)
        return this == ' ' ||
            this == '\t' ||
            this == '\n' ||
            this == '\r' ||
            type == Character.SPACE_SEPARATOR.toInt() ||
            type == Character.LINE_SEPARATOR.toInt() ||
            type == Character.PARAGRAPH_SEPARATOR.toInt()
    }

    private fun Char.isPunctuation(): Boolean {
        val type = Character.getType(this)
        return when (type) {
            Character.CONNECTOR_PUNCTUATION.toInt(),
            Character.DASH_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt(),
            Character.END_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
            Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt() -> true
            else -> "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~·…‘’“”｡､。".contains(this)
        }
    }

    companion object {
        private const val VOCAB_FILE = "vocab.txt"

        fun fromAssets(
            context: Context,
            vocabFileName: String = VOCAB_FILE,
            maxSeqLen: Int,
            doLowerCase: Boolean = true
        ): WordPieceTokenizer {
            val vocab = loadVocab(context, vocabFileName)
            val clsId = vocab["[CLS]"] ?: error("vocab.txt is missing [CLS]")
            val padId = vocab["[PAD]"] ?: error("vocab.txt is missing [PAD]")
            val sepId = vocab["[SEP]"] ?: error("vocab.txt is missing [SEP]")
            val unkId = vocab["[UNK]"] ?: error("vocab.txt is missing [UNK]")
            require(maxSeqLen >= 2) { "Sequence length must be >= 2" }
            return WordPieceTokenizer(
                vocab = vocab,
                unkId = unkId,
                clsId = clsId,
                sepId = sepId,
                padId = padId,
                maxSeqLen = maxSeqLen,
                doLowerCase = doLowerCase
            )
        }

        private fun loadVocab(context: Context, vocabFileName: String): Map<String, Int> {
            context.assets.open(vocabFileName).bufferedReader(Charsets.UTF_8).useLines { lines ->
                val map = HashMap<String, Int>()
                var index = 0
                lines.forEach { raw ->
                    val token = raw.trim()
                    if (token.isEmpty() || map.containsKey(token)) return@forEach
                    map[token] = index++
                }
                return map
            }
        }
    }
}
