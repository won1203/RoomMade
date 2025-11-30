package com.example.roommade.vm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roommade.domain.GenerateRoomImageUseCase
import com.example.roommade.model.FloorPlan
import com.example.roommade.model.RoomCategory
import com.example.roommade.network.ReplicateClient
import com.example.roommade.util.ImageSaver
import com.example.roommade.util.ImageSharer
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface AiImageUiState {
    data object Idle : AiImageUiState
    data object Generating : AiImageUiState
    data class Success(val imageUrl: String) : AiImageUiState
    data class Error(val message: String) : AiImageUiState
}

class AiImageViewModel(
    private val useCase: GenerateRoomImageUseCase,
    private val saver: ImageSaver,
    private val sharer: ImageSharer,
    private val imageHttpClient: OkHttpClient = defaultImageHttpClient()
) : ViewModel() {

    var uiState by mutableStateOf<AiImageUiState>(AiImageUiState.Idle)
        private set
    var latestSavedUri by mutableStateOf<Uri?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set

    private var generateJob: Job? = null
    private var latestImageUrl: String? = null

    fun generate(plan: FloorPlan, concept: String, styleTags: Set<String>, roomCategory: RoomCategory) {
        generateJob?.cancel()
        uiState = AiImageUiState.Generating
        latestSavedUri = null

        generateJob = viewModelScope.launch {
            try {
                val url = useCase(plan, concept, styleTags, roomCategory)
                latestImageUrl = url
                uiState = AiImageUiState.Success(url)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                uiState = AiImageUiState.Error(t.message ?: "이미지 생성에 실패했습니다.")
            }
        }
    }

    fun saveToGallery(context: Context) {
        val url = latestImageUrl ?: return
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            try {
                val bitmap = fetchBitmap(url)
                val uri = saver.savePngToGallery(
                    context = context,
                    bitmap = bitmap,
                    displayName = "roommade_${System.currentTimeMillis()}.png"
                )
                latestSavedUri = uri
            } catch (t: Throwable) {
                uiState = AiImageUiState.Error(t.message ?: "이미지 저장에 실패했습니다.")
            } finally {
                isSaving = false
            }
        }
    }

    fun share(context: Context) {
        val uri = latestSavedUri ?: return
        sharer.shareImage(context, uri)
    }

    fun clearError() {
        if (uiState is AiImageUiState.Error) {
            uiState = AiImageUiState.Idle
        }
    }

    private suspend fun fetchBitmap(url: String): Bitmap {
        val request = Request.Builder().url(url).build()
        val bytes = withContext(Dispatchers.IO) {
            imageHttpClient.newCall(request).execute()
        }.use { response ->
            if (!response.isSuccessful) {
                throw IOException("이미지 다운로드 실패 (${response.code})")
            }
            response.body?.bytes() ?: throw IOException("이미지 데이터가 비어 있습니다.")
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("이미지 디코딩에 실패했습니다.")
    }

    override fun onCleared() {
        super.onCleared()
        generateJob?.cancel()
    }

    companion object {
        private fun defaultImageHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

        fun provideFactory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AiImageViewModel::class.java)) {
                        val replicateClient = ReplicateClient()
                        val useCase = GenerateRoomImageUseCase(replicateClient)
                        val saver = ImageSaver()
                        val sharer = ImageSharer()
                        @Suppress("UNCHECKED_CAST")
                        return AiImageViewModel(
                            useCase = useCase,
                            saver = saver,
                            sharer = sharer
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
                }
            }
    }
}
