package com.ronin71.pdfcustom.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.util.SizeF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ronin71.pdfcustom.common.aspectRatio
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.model.MyPdfPage
import com.ronin71.pdfcustom.util.MyPdfAnalyzer
import com.ronin71.pdfcustom.util.MyPdfFilter
import com.ronin71.pdfcustom.util.MyRectUtil
import com.ronin71.pdfcustom.viewmodel.MyPdfViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File


val TAG = "PreviewScreen"

@SuppressLint("UnusedBoxWithConstraintsScope", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onBack: () -> Boolean,
    uri: Uri,
    cacheSharedViewModel: MyPdfModelMain?,
    cacheUri: Uri?,
    pdfCacheFile: File,
    viewModel: MyPdfViewModel = viewModel()
) {
    val thumbSize = 40.dp
    val scrollThumbWidth = 6.dp
    val scrollThumbHeight = 100.dp

    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()

    // Collect states from ViewModel
    val loadedPages by viewModel.loadedPages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val error by viewModel.error.collectAsState()
    val hasMorePages by viewModel.hasMorePages.collectAsState()
    var sliderPositionPercent by remember { mutableStateOf(Pair(0.5f, 0.5f)) }

    val coroutineScope = rememberCoroutineScope()

    val sliderWidth = 1.dp
    val context = LocalContext.current

    // Initialize analyzer
    LaunchedEffect(uri) {
        coroutineScope.launch {
            viewModel.initAnalyzer(pdfCacheFile, context, uri)
        }
    }

    // Load total pages
    LaunchedEffect(uri) {

        coroutineScope.launch{ viewModel.loadTotalPages() }
    }

    // Load initial data
    LaunchedEffect(uri) {
        coroutineScope.launch {
            if (uri == cacheUri && cacheSharedViewModel != null) {
                viewModel.loadFromCache(cacheSharedViewModel)
            } else {
                viewModel.loadInitialPages()
            }
        }
    }

    // Theo dõi scroll để load thêm
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo }
            .collect { layoutInfo ->
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty() && hasMorePages && !isLoadingMore && !isLoading) {
                    val lastVisibleItem = visibleItems.last()
                    if (lastVisibleItem.index >= loadedPages.size - 5) {
                        viewModel.loadMorePages()
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview And Edit") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val maxWidthDp = maxWidth
            val maxHeightDp = maxHeight
            Log.d(TAG, "PreviewScreen: maxWidthDp = $maxWidthDp, maxHeightDp = $maxHeightDp")
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
//                // Thông báo tiến độ
                if (isLoading || loadedPages.isNotEmpty()) {
                    ProgressHeader(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        isLoading = isLoading
                    )
                }

                // Hiển thị danh sách các page
                if (loadedPages.isNotEmpty()) {
                    PagesList(
                        lazyListState = lazyListState,
                        loadedPages = loadedPages,
                        totalPages = totalPages,
                        maxHeightDp = maxHeightDp,
                        maxWidthDp = maxWidthDp,
                        sliderPosPercentX = sliderPositionPercent.first,
                        isLoadingMore = isLoadingMore,
                        density = density,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 15.dp, bottom = 15.dp)
                    )
                } else if (isLoading) {
                    LoadingState(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                } else if (error != null) {
                    ErrorState(
                        error = error!!, modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            }

            // Slider bar và thumb điều khiển
            if (loadedPages.isNotEmpty()) {
                SliderControls(
                    maxWidthDp = maxWidthDp,
                    maxHeightDp = maxHeightDp,
                    sliderWidth = sliderWidth,
                    thumbSize = thumbSize,
                    sliderPositionPercent = sliderPositionPercent,
                    density = density,
                    onSliderDrag = { newX, newY ->
                        sliderPositionPercent = Pair(newX, newY)
                    }
                )
            }
        }
    }
}

@Composable
fun ProgressHeader(
    currentPage: Int,
    totalPages: Int,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = Color.Blue.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoading) "Đang tải..." else "Đã tải xong",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$currentPage / $totalPages",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PagesList(
    lazyListState: LazyListState,
    loadedPages: List<MyPdfPage>,
    totalPages: Int,
    maxHeightDp: Dp,
    maxWidthDp: Dp,
    sliderPosPercentX: Float,
    isLoadingMore: Boolean,
    density: Density,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        itemsIndexed(loadedPages) { i, page ->
            PdfPreviewPage(
                density = density,
                page = page,
                pageNumber = i + 1,
                totalPages = totalPages,
                maxHeightDp = maxHeightDp,
                maxWidthDp = maxWidthDp,
                sliderPosPercentX = sliderPosPercentX
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingState(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (totalPages > 0)
                    "Đang tải trang $currentPage/$totalPages"
                else "Đang xử lý PDF...",
                color = Color.White
            )
        }
    }
}

@Composable
fun ErrorState(
    error: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Lỗi: $error",
            color = Color.Red
        )
    }
}

@Composable
fun SliderControls(
    maxWidthDp: Dp,
    maxHeightDp: Dp,
    sliderWidth: Dp,
    thumbSize: Dp,
    sliderPositionPercent: Pair<Float, Float>,
    density: Density,
    onSliderDrag: (Float, Float) -> Unit
) {

    val positionState = rememberUpdatedState(sliderPositionPercent)
    // Slider bar
    Box(
        modifier = Modifier
            .offset(
                x = maxWidthDp * sliderPositionPercent.first - sliderWidth / 2,
                y = 0.dp
            )
            .width(sliderWidth)
            .height(maxHeightDp)
            .background(Color.White.copy(alpha = 0.3f))
            .border(1.dp, Color.Red)
    )
    // Slider thumb
    Box(
        modifier = Modifier
            .offset(
                x = maxWidthDp * sliderPositionPercent.first - thumbSize / 2,
                y = maxHeightDp * sliderPositionPercent.second - thumbSize / 2
            )
            .size(thumbSize)
            .background(color = Color.Red, shape = CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val currentPos = positionState.value // ✅ Luôn là giá trị mới nhất

                        val deltaXDp = with(density) { dragAmount.x.toDp() }
                        val deltaYDp = with(density) { dragAmount.y.toDp() }

                        val newPosX = currentPos.first + (deltaXDp.value / maxWidthDp.value)
                        val newPosY = currentPos.second + (deltaYDp.value / maxHeightDp.value)

                        onSliderDrag(
                            newPosX.coerceIn(0f, 1f),
                            newPosY.coerceIn(0f, 1f)
                        )
                    }
                )
            }
    )
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PdfPreviewPage(
    density: Density,
    page: MyPdfPage,
    pageNumber: Int,
    totalPages: Int,
    maxHeightDp: Dp,
    maxWidthDp: Dp,
    sliderPosPercentX: Float, // 0f -> 1f
) {
    val pageAspectRatio = page.size.aspectRatio()
    val maxWidthPx = with(density) { maxWidthDp.toPx() }
    val maxHeightPx = with(density) { maxHeightDp.toPx() }

    val displaySize = MyRectUtil().fillSizeWithAspect(
        SizeF(maxWidthPx, maxHeightPx),
        pageAspectRatio
    )

    val displayWidth = with(density) { displaySize.width.toDp() }
    val displayHeight = with(density) { displaySize.height.toDp() }
    val displayWidthPx = displaySize.width

    // State cho bitmap đã edit
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Load bitmap đã edit khi page thay đổi
    LaunchedEffect(page) {
        isLoading = true
        editedBitmap = MyPdfFilter().applyDarkModeToBitmap(page.bitmapPage)
        isLoading = false
    }

    Box(
        modifier = Modifier
            .width(displayWidth)
            .height(displayHeight)
            .shadow(
                elevation = 8.dp,
                clip = false
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.3f),
            )
    ) {
        // 1. Ảnh gốc (hiển thị toàn bộ)
        Image(
            bitmap = page.bitmapPage.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Ảnh đã edit (chỉ hiển thị từ 0 đến clipPositionX)
        if (editedBitmap != null && !isLoading) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxWidthDp = maxWidth
                val clipWidth = maxWidthDp * sliderPosPercentX + 1.dp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(
                            GenericShape { size, _ ->
                                val clipPx = with(density) { clipWidth.toPx() }
                                addRect(
                                    Rect(
                                        clipPx,
                                        0f,
                                        size.width,
                                        size.height
                                    ),
                                )
                            }
                        )
                ) {
                    Image(
                        bitmap = editedBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 3. Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 4. Page number badge
        Box(
            modifier = Modifier
                .absoluteOffset(4.dp, 4.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$pageNumber / $totalPages",
                color = Color.White,
                fontWeight = FontWeight.Light,
                fontSize = 10.sp
            )
        }
    }
}

