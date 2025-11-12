package com.example.roommade.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

class StyleAnalyzer private constructor(
    private val tokenizer: WordPieceTokenizer,
    private val interpreter: Interpreter,
    private val labelNames: List<String>,
    private val labelTagMap: Map<String, Set<String>>,
    private val sequenceLength: Int
) {

    data class StyleProbability(
        val label: String,
        val probability: Float
    )

    suspend fun analyze(text: String): List<StyleProbability> = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext emptyList()
        val encoding = tokenizer.encode(text)
        val inputArrays = buildInputArrays(encoding)
        val outputs = hashMapOf<Int, Any>()
        val scores = Array(1) { FloatArray(labelNames.size) }
        outputs[0] = scores

        synchronized(interpreterLock) {
            interpreter.runForMultipleInputsOutputs(inputArrays, outputs)
        }

        val probs = scores[0].softmax()
        labelNames.indices.map { idx ->
            StyleProbability(
                label = labelNames.getOrElse(idx) { "Style${idx + 1}" },
                probability = probs.getOrElse(idx) { 0f }
            )
        }.sortedByDescending { it.probability }
    }

    private fun buildInputArrays(encoding: WordPieceTokenizer.Encoding): Array<Any> {
        val ids = arrayOf(encoding.inputIds.copyOf(sequenceLength))
        val masks = arrayOf(encoding.attentionMask.copyOf(sequenceLength))
        val types = arrayOf(encoding.tokenTypeIds.copyOf(sequenceLength))
        val count = interpreter.inputTensorCount
        return when (count) {
            1 -> arrayOf(ids)
            2 -> arrayOf(ids, masks)
            3 -> arrayOf(ids, masks, types)
            else -> Array(count) { index ->
                when (index) {
                    0 -> ids
                    1 -> masks
                    2 -> types
                    else -> types
                }
            }
        }
    }

    private fun FloatArray.softmax(): FloatArray {
        if (isEmpty()) return this
        val max = maxOrNull() ?: 0f
        val expValues = FloatArray(size)
        var sum = 0.0
        for (i in indices) {
            val value = exp((this[i] - max).toDouble())
            expValues[i] = value.toFloat()
            sum += value
        }
        if (sum == 0.0) return expValues
        for (i in expValues.indices) {
            expValues[i] = (expValues[i] / sum).toFloat()
        }
        return expValues
    }

    fun close() {
        try {
            interpreter.close()
        } catch (ignored: Exception) {
        }
    }

    fun styleTagsFor(label: String): Set<String> = labelTagMap[label].orEmpty()

    companion object {
        private const val MODEL_FILE = "interior_model.tflite"
        private val interpreterLock = Any()
        private data class StyleLabelDefinition(
            val displayName: String,
            val styleTags: Set<String>
        )

        private val styleLabelDefinitions = listOf(
            StyleLabelDefinition("내추럴", setOf("Natural", "Warm")),
            StyleLabelDefinition("모던", setOf("Modern")),
            StyleLabelDefinition("미니멀", setOf("Minimal")),
            StyleLabelDefinition("클래식", setOf("Vintage")),
            StyleLabelDefinition("코지", setOf("Cozy", "Warm"))
        )

        fun create(context: Context): StyleAnalyzer {
            val appContext = context.applicationContext
            val buffer = loadModelFile(appContext, MODEL_FILE)
            val interpreter = Interpreter(buffer, Interpreter.Options().apply {
                val cores = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
                setNumThreads(cores)
                setUseNNAPI(false)
            })
            val seqLen = interpreter.getInputTensor(0).shape().lastOrNull() ?: 128
            val tokenizer = WordPieceTokenizer.fromAssets(
                context = appContext,
                maxSeqLen = seqLen,
                doLowerCase = true
            )
            val outputTensor = interpreter.getOutputTensor(0)
            val outputSize = outputTensor.shape().lastOrNull() ?: styleLabelDefinitions.size
            val defs = ArrayList<StyleLabelDefinition>(outputSize).apply {
                addAll(styleLabelDefinitions.take(minOf(styleLabelDefinitions.size, outputSize)))
                var nextIndex = size + 1
                while (size < outputSize) {
                    add(
                        StyleLabelDefinition(
                            displayName = "스타일$nextIndex",
                            styleTags = emptySet()
                        )
                    )
                    nextIndex++
                }
            }
            val names = defs.map { it.displayName }
            val tagMap = defs.associate { it.displayName to it.styleTags }
            return StyleAnalyzer(tokenizer, interpreter, names, tagMap, seqLen)
        }

        private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
            context.assets.openFd(modelFileName).use { assetDescriptor ->
                FileInputStream(assetDescriptor.fileDescriptor).use { input ->
                    val channel = input.channel
                    return channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        assetDescriptor.startOffset,
                        assetDescriptor.declaredLength
                    )
                }
            }
        }
    }
}

object StyleAnalyzerProvider {
    @Volatile
    private var analyzer: StyleAnalyzer? = null

    fun getOrNull(context: Context): StyleAnalyzer? {
        val cached = analyzer
        if (cached != null) return cached
        return synchronized(this) {
            analyzer ?: runCatching {
                StyleAnalyzer.create(context.applicationContext)
            }.onFailure {
                Log.e("StyleAnalyzer", "Failed to init analyzer", it)
            }.getOrNull().also { created ->
                if (created != null) {
                    analyzer = created
                }
            }
        }
    }
}