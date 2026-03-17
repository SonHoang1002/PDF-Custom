package com.ronin71.pdfcustom.model

sealed class MyPageResult {
    data class Progress(val progress: Float) : MyPageResult()
    data class Page(val pageIndex: Int, val page: MyPdfPage) : MyPageResult()
    data class Complete(val model: MyPdfModelMain) : MyPageResult()
    data class Error(val error: String) : MyPageResult()
}