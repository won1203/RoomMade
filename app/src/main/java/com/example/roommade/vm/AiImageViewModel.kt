package com.example.roommade.vm

import android.content.Context
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
import com.example.roommade.network.FirebaseImageGenClient
import com.example.roommade.util.ImageFetcher
import com.example.roommade.util.ImageSaver
import com.example.roommade.util.ImageSharer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    private val imageFetcher: ImageFetcher = ImageFetcher()
) : ViewModel() {

    var uiState by mutableStateOf<AiImageUiState>(AiImageUiState.Idle)
        private set
    var latestSavedUri by mutableStateOf<Uri?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set

    private var generateJob: Job? = null
    private var latestImageUrl: String? = null

    fun generate(
        plan: FloorPlan,
        concept: String,
        styleTags: Set<String>,
        roomCategory: RoomCategory,
        baseRoomImage: String? = null
    ) {
        generateJob?.cancel()
        uiState = AiImageUiState.Generating
        latestSavedUri = null

        generateJob = viewModelScope.launch {
            try {
                val url = useCase(plan, concept, styleTags, roomCategory, baseRoomImage)
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
                val bitmap = imageFetcher.fetchBitmap(url)
                val uri = saver.savePngToGallery(
                    context = context,
                    bitmap = bitmap,
                    displayName = "roommade_${System.currentTimeMillis()}.png"
                )
                latestSavedUri = uri
            } catch (t: Throwable) {
                uiState = AiImageUiState.Error(t.message ?: "이미지를 저장하는 데 실패했어요.")
            } finally {
                isSaving = false
            }
        }
    }

    fun share(context: Context) {
        val uri = latestSavedUri ?: return
        sharer.shareImage(context, uri)
    }

    fun shareOrSave(context: Context) {
        val url = latestImageUrl ?: return
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            try {
                val uri = latestSavedUri ?: run {
                    val bitmap = imageFetcher.fetchBitmap(url)
                    saver.savePngToGallery(
                        context = context,
                        bitmap = bitmap,
                        displayName = "roommade_${System.currentTimeMillis()}.png"
                    )
                }
                latestSavedUri = uri
                sharer.shareImage(context, uri)
            } catch (t: Throwable) {
                uiState = AiImageUiState.Error(t.message ?: "이미지를 공유하는 데 실패했어요.")
            } finally {
                isSaving = false
            }
        }
    }

    fun clearError() {
        if (uiState is AiImageUiState.Error) {
            uiState = AiImageUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        generateJob?.cancel()
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AiImageViewModel::class.java)) {
                        val backendClient = FirebaseImageGenClient()
                        val useCase = GenerateRoomImageUseCase(backendClient)
                        val saver = ImageSaver()
                        val sharer = ImageSharer()
                        val fetcher = ImageFetcher()
                        @Suppress("UNCHECKED_CAST")
                        return AiImageViewModel(
                            useCase = useCase,
                            saver = saver,
                            sharer = sharer,
                            imageFetcher = fetcher
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
                }
            }
    }
}
