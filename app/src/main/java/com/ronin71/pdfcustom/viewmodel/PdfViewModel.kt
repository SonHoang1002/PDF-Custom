package com.ronin71.pdfcustom.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin71.pdfcustom.model.PdfPageResult
import com.ronin71.pdfcustom.util.PdfExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PdfViewModel : ViewModel() {

    // State lưu các page đã load
    private val _pages = MutableStateFlow<List<PdfPageResult.Page>>(emptyList())
    val pages: StateFlow<List<PdfPageResult.Page>> = _pages.asStateFlow()

    // State cho text
    private val _allTexts = MutableStateFlow<List<String>>(emptyList())
    val allTexts: StateFlow<List<String>> = _allTexts.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Progress state
    private val _progress = MutableStateFlow<PdfPageResult.Progress?>(null)
    val progress: StateFlow<PdfPageResult.Progress?> = _progress.asStateFlow()

    // Info state
    private val _info = MutableStateFlow<String?>(null)
    val info: StateFlow<String?> = _info.asStateFlow()

    // Total pages
    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    // Current loaded count
    private val _loadedCount = MutableStateFlow(0)
    val loadedCount: StateFlow<Int> = _loadedCount.asStateFlow()

    // Có còn trang để load không
    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()

    private var loadJob: Job? = null
    private var currentPageIndex = 0
    private val pageSize = 20 // Load 20 trang mỗi lần

    fun loadPdf(context: Context, uri: Uri) {
        // Reset state
        _pages.value = emptyList()
        _allTexts.value = emptyList()
        _error.value = null
        _progress.value = null
        _info.value = null
        _loadedCount.value = 0
        _hasMorePages.value = true
        currentPageIndex = 0

        // Bắt đầu load trang đầu tiên
        loadNextPages(context, uri)
    }

    fun loadNextPages(context: Context, uri: Uri) {
        // Nếu đang load hoặc không còn trang thì return
        if (_isLoading.value || !_hasMorePages.value) return

        _isLoading.value = true
        _error.value = null

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val startPage = currentPageIndex
                val endPage = startPage + pageSize

                _info.value = "Loading pages ${startPage + 1} to $endPage..."

                PdfExtractor().extractPagesWithFlow(
                    context = context,
                    uri = uri,
                    startPage = startPage,
                    pageCount = pageSize
                ).catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }.collect { result ->
                    when (result) {
                        is PdfPageResult.Page -> {
                            // Thêm page mới vào danh sách
                            _pages.update { currentList ->
                                (currentList + result).sortedBy { it.index }
                            }
                            _allTexts.update { currentList ->
                                (currentList + result.text)
                            }
                            _loadedCount.value = _pages.value.size
                            currentPageIndex = _pages.value.size

                            // Kiểm tra xem còn trang không
                            if (result.pageNumber >= result.totalPages) {
                                _hasMorePages.value = false
                                _info.value = "All ${result.totalPages} pages loaded"
                            }
                        }
                        is PdfPageResult.Progress -> {
                            _progress.value = result
                        }
                        is PdfPageResult.Info -> {
                            _info.value = result.message
                        }
                        is PdfPageResult.TotalPages -> {
                            _totalPages.value = result.totalPages
                        }
                        is PdfPageResult.Complete -> {
                            _isLoading.value = false
                        }
                        is PdfPageResult.Error -> {
                            _error.value = result.message
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    // Hàm này sẽ được gọi từ UI khi cuộn đến threshold
    fun onScrollPosition(lastVisibleIndex: Int) {
        val loadedCount = _pages.value.size
        val totalPages = _totalPages.value

        // Nếu đã load hết hoặc đang load thì return
        if (_isLoading.value || loadedCount >= totalPages) return

        // Threshold: khi cuộn đến trang thứ 4 từ dưới lên
        // lastVisibleIndex là index của item cuối cùng đang hiển thị
        val thresholdIndex = loadedCount - 4

        if (lastVisibleIndex >= thresholdIndex && lastVisibleIndex > 0) {
            // Cần load thêm
            // Truyền context và uri - cần lưu lại từ đầu
        }
    }

    fun cancelLoading() {
        loadJob?.cancel()
        _isLoading.value = false
    }

    fun clearPdf() {
        loadJob?.cancel()
        _pages.value = emptyList()
        _allTexts.value = emptyList()
        _error.value = null
        _progress.value = null
        _info.value = null
        _isLoading.value = false
        _hasMorePages.value = true
        _loadedCount.value = 0
        _totalPages.value = 0
        currentPageIndex = 0
    }
}