package com.ronin71.pdfcustom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.Context
import android.util.Log
import com.ronin71.pdfcustom.model.MyPageResult
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.model.MyPdfPage
import com.ronin71.pdfcustom.util.MyPdfAnalyzer
import java.io.File

class MyPdfViewModel : ViewModel() {

    private val TAG  = "MyPdfViewModel"
    private val _loadedPages = MutableStateFlow<List<MyPdfPage>>(emptyList())
    val loadedPages: StateFlow<List<MyPdfPage>> = _loadedPages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()

    private val _nextPageIndex = MutableStateFlow(0)
    val nextPageIndex: StateFlow<Int> = _nextPageIndex.asStateFlow()


    // Constants
    private val pageSize = 20

    // Analyzer instance
    private var _pdfAnalyzer: MyPdfAnalyzer? = null

    fun initAnalyzer(pdfCacheFile: File, context: Context, uri: Uri) {
        if (_pdfAnalyzer == null) {
            _pdfAnalyzer = MyPdfAnalyzer(pdfCacheFile = pdfCacheFile, context = context, uri = uri)
        }
    }

    fun loadTotalPages() {
        viewModelScope.launch {
            _pdfAnalyzer?.let { analyzer ->
                _totalPages.value = analyzer.getTotalPages()
            }
        }
    }

    fun loadFromCache(cacheModel: MyPdfModelMain) {
        _loadedPages.value = cacheModel.pages
        _totalPages.value = cacheModel.pages.size
        _nextPageIndex.value = cacheModel.pages.size
        _hasMorePages.value = false
        _isLoading.value = false
    }



    fun loadInitialPages() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _loadedPages.value = emptyList()
            _nextPageIndex.value = 0

            try {
                _pdfAnalyzer?.extractPdfFlow(startPage = 0, pageCount = pageSize)
                    ?.collect { result ->
                        when (result) {
                            is MyPageResult.Page -> {
                                _loadedPages.value = _loadedPages.value + result.page
                                _currentPage.value = result.pageIndex + 1
                                _nextPageIndex.value = result.pageIndex + 1
                            }
                            is MyPageResult.Progress -> {
                                // Có thể update progress nếu cần
                            }
                            else -> {}
                        }
                    }

                _hasMorePages.value = _nextPageIndex.value < _totalPages.value

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMorePages() {
        if (_isLoadingMore.value || !_hasMorePages.value || _isLoading.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                _pdfAnalyzer?.extractPdfFlow(startPage = _nextPageIndex.value, pageCount = pageSize)
                    ?.collect { result ->
                        when (result) {
                            is MyPageResult.Page -> {
                                _loadedPages.value = _loadedPages.value + result.page
                                _currentPage.value = result.pageIndex + 1
                                _nextPageIndex.value = result.pageIndex + 1
                            }
                            else -> {}
                        }
                    }

                _hasMorePages.value = _nextPageIndex.value < _totalPages.value

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun loadPages(startIndex: Int, count: Int) {
        _pdfAnalyzer?.let { analyzer ->
            val endIndex = minOf(startIndex + count, _totalPages.value)

            for (i in startIndex until endIndex) {
                val page = analyzer.generatePage(i)
                _loadedPages.value = _loadedPages.value.toMutableList().apply {
                    add(page)
                }
                _currentPage.value = i + 1
                _nextPageIndex.value = i + 1
            }

            _hasMorePages.value = _nextPageIndex.value < _totalPages.value
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun reset() {
        _loadedPages.value = emptyList()
        _isLoading.value = false
        _isLoadingMore.value = false
        _currentPage.value = 0
        _totalPages.value = 0
        _error.value = null
        _hasMorePages.value = true
        _nextPageIndex.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        _pdfAnalyzer?.close()
        _pdfAnalyzer = null
    }
}