package com.ronin71.pdfcustom.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.util.PdfAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MyPdfViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _pdfModel = MutableStateFlow<MyPdfModelMain?>(null)
    val pdfModel: StateFlow<MyPdfModelMain?> = _pdfModel.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var extractJob: Job? = null
    private val pdfAnalyzer = PdfAnalyzer()
    fun clear() {
        _pdfModel.value = null
    }
    fun extractPdf(context: Context, uri: Uri) {
        extractJob?.cancel()

        extractJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _progress.value = 0f
                _currentPage.value = 0
                _error.value = null

                val result = pdfAnalyzer.extractPdf(
                    context = context,
                    uri = uri,
                    onProgress = { progressValue ->
                        _progress.value = progressValue

                        if (progressValue > 0.1f) {
                            val totalPages =  _pdfModel.value?.pages?.size ?: 0
                            if (totalPages > 0) {
                                val processedPages = ((progressValue - 0.1f) / 0.9f * totalPages).toInt()
                                _currentPage.value = processedPages.coerceIn(0, totalPages)
                                _totalPages.value = totalPages
                            }
                        }

                        Log.d("PdfViewModel", "Progress: ${(progressValue * 100).toInt()}% - Trang: ${_currentPage.value}/${_totalPages.value}")
                    }
                )

                _pdfModel.value = result
                Log.d("PdfViewModel", "Extract thành công: ${result.pages.size} trang")

            } catch (e: CancellationException) {
                Log.d("PdfViewModel", "Extraction cancelled")
            } catch (e: Exception) {
                Log.e("PdfViewModel", "Extraction error", e)
                _error.value = "Lỗi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setCurrentPdfModel(model: MyPdfModelMain) {
        _pdfModel.value = model
    }

    fun resetState() {
        _pdfModel.value = null
        _progress.value = 0f
        _currentPage.value = 0
        _totalPages.value = 0
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        extractJob?.cancel()
    }
}