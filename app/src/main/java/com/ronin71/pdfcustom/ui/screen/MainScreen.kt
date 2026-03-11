package com.ronin71.pdfcustom.ui.screen


import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ronin71.pdfcustom.viewmodel.PdfViewModel

@Composable
fun MainScreen(
    viewModel: PdfViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            BuildHeader(selectedUri)

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            BuildButtons(
                selectedUri = selectedUri,
                viewModel = viewModel,
                isLoading = isLoading,
                launcher = launcher,
                onChangeSelectedUri = {
                    selectedUri = it
                })

            Spacer(modifier = Modifier.height(16.dp))

            BuildPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = Color.DarkGray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                selectedUri = selectedUri,
                viewModel = viewModel
            )

        }
    }
}

@Composable
fun MainScreen1(
    viewModel: PdfViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "PDF Custom",
                fontSize = 26.sp,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { launcher.launch("application/pdf") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import PDF")
                }

                if (selectedUri != null) {
                    Button(
                        onClick = {
                            viewModel.clearPdf()
                            selectedUri = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PDF Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = Color.DarkGray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                if (selectedUri != null) {
                    PdfPreviewWithPaging(
                        viewModel = viewModel,
                        uri = selectedUri!!,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No PDF Selected")
                    }
                }
            }
        }
    }
}

@Composable
fun BuildHeader(
    selectedUri: Uri?,
) {
    Text(
        text = "PDF Custom",
        fontSize = 26.sp,
        style = MaterialTheme.typography.headlineMedium
    )
    if (selectedUri != null) {
        // Hiển thị nút setting ở đây, khi chạm vào thì mở ra 1 portal dialog gồm các setting sau:
        //     + switch dark - light ( dựa vào phân tích màu chữ và màu nền để đưa ra mặc định là light hay dark mode )
        //     + slider để tăng hoặc giảm độ tương phản của pdf
        //     + Thay đổi font của chữ của từng page
        //     + Thay đổi background cho toàn bộ pdf
        // Dưới cùng là hai nút Discard và Apply
        // Khi Apply sẽ xử lý từng page theo toàn bộ yêu cầu
        // Khi Discard sẽ huỷ những thay đổi đã chỉnh
    }
}

@Composable
fun BuildButtons(
    selectedUri: Uri?,
    launcher: ManagedActivityResultLauncher<String, Uri?>,
    viewModel: PdfViewModel,
    isLoading: Boolean,
    onChangeSelectedUri: (Uri?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedUri == null) {
            Button(
                onClick = { launcher.launch("application/pdf") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import PDF")
            }
        }

        if (selectedUri != null) {
            Button(
                onClick = {
                    viewModel.clearPdf()
                    onChangeSelectedUri(null)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear")
            }
        }
    }
}

@Composable
fun BuildPreview(
    modifier: Modifier,
    selectedUri: Uri?,
    viewModel: PdfViewModel,
) {
    Box(
        modifier = modifier
    ) {
        if (selectedUri != null) {
            PdfPreviewWithPaging(
                viewModel = viewModel,
                uri = selectedUri,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No PDF Selected")
            }
        }
    }
}

@Composable
fun PdfPreviewWithPaging(
    viewModel: PdfViewModel,
    uri: Uri,
) {
    val context = LocalContext.current
    val pages by viewModel.pages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val info by viewModel.info.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val loadedCount by viewModel.loadedCount.collectAsState()
    val hasMorePages by viewModel.hasMorePages.collectAsState()

    val listState = rememberLazyListState()

    // Theo dõi scroll position để load thêm
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty()) {
                    val lastVisibleIndex = visibleItems.last().index
                    val loadedSize = pages.size

                    if (loadedSize > 0 && hasMorePages && !isLoading) {
                        val thresholdIndex = loadedSize - 4
                        if (lastVisibleIndex >= thresholdIndex) {
                            viewModel.loadNextPages(context, uri)
                        }
                    }
                }
            }
    }

    // Bắt đầu load lần đầu
    LaunchedEffect(Unit) {
        viewModel.loadPdf(context, uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PdfHeader(
                loadedCount = loadedCount,
                totalPages = totalPages,
                info = info
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Các page đã load
                itemsIndexed(
                    items = pages,
                    key = { index, page -> "page_${page.pageNumber}" }
                ) { index, page ->
                    PdfPageItemWithFlow(
                        bitmap = page.bitmap,
                        pageNumber = page.pageNumber,
                        totalPages = totalPages,
                        pageText = page.text,
                        isLatest = index == pages.lastIndex
                    )
                }

                // Loading indicator ở cuối
                if (isLoading || hasMorePages) {
                    item {
                        PdfLoadingIndicator(
                            loadedCount = loadedCount,
                            totalPages = totalPages,
                            isLoading = isLoading
                        )
                    }
                }
//
//                // Error indicator
//                if (error != null) {
//                    item {
//                        PdfErrorItem(
//                            error = error!!,
//                            onRetry = {
//                                viewModel.loadNextPages(context, uri)
//                            }
//                        )
//                    }
//                }
            }
        }

    }
}

@Composable
fun PdfHeader(
    loadedCount: Int,
    totalPages: Int,
    info: String?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (totalPages > 0)
                    "Loaded $loadedCount/$totalPages pages"
                else
                    "Loading PDF...",
                style = MaterialTheme.typography.titleMedium
            )

            if (info != null) {
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun PdfLoadingIndicator(
    loadedCount: Int,
    totalPages: Int,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            if (isLoading) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(24.dp),
//                    strokeWidth = 2.dp
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//            }

            Text(
                text = if (totalPages > 0)
                    "Loading more pages... ($loadedCount/$totalPages)"
                else
                    "Analyzing PDF...",
                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PdfErrorItem(
    error: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun PdfPageItemWithFlow(
    bitmap: Bitmap,
    pageNumber: Int,
    totalPages: Int,
    pageText: String = "",
    isLatest: Boolean = false
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Đếm số ngón tay đang chạm
    var pointerCount by remember { mutableStateOf(0) }

//    val alpha = remember { Animatable(0f) }
//    val slideOffset = remember { Animatable(50f) }
//
//    LaunchedEffect(isLatest) {
//        if (isLatest) {
//            alpha.animateTo(
//                targetValue = 1f,
//                animationSpec = tween(durationMillis = 500)
//            )
//            slideOffset.animateTo(
//                targetValue = 0f,
//                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//            )
//        } else {
//            alpha.snapTo(1f)
//            slideOffset.snapTo(0f)
//        }
//    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
//            .alpha(alpha.value)
//            .offset(y = slideOffset.value.dp)
        ,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color =
                    MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page $pageNumber of $totalPages",
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
//                    if (isLatest) {
//                        Surface(
//                            color = MaterialTheme.colorScheme.errorContainer,
//                            shape = MaterialTheme.shapes.small
//                        ) {
//                            Text(
//                                text = "NEW",
//                                style = MaterialTheme.typography.labelSmall,
//                                color = MaterialTheme.colorScheme.onErrorContainer,
//                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
//                            )
//                        }
//                    }

                    if (scale > 1f) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (pageText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "${pageText.length} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }


                }
            }

            // Page content - CHỈ SCALE KHI CÓ 2 NGÓN TAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        // Theo dõi số lượng ngón tay
                        awaitEachGesture {
                            pointerCount = 1

                            // Chờ thêm pointer hoặc kết thúc gesture
                            do {
                                val event = awaitPointerEvent()
                                pointerCount = event.changes.size

                                // Xử lý scale khi có 2 ngón
                                if (pointerCount == 2) {
                                    val change1 = event.changes[0]
                                    val change2 = event.changes[1]

                                    val currentDistance =
                                        (change1.position - change2.position).getDistance()
                                    val previousDistance =
                                        (change1.previousPosition - change2.previousPosition).getDistance()

                                    // Tính zoom
                                    val zoomFactor = currentDistance / previousDistance
                                    scale = (scale * zoomFactor).coerceIn(1f, 5f)

                                    // Tính pan khi zoom
                                    if (scale > 1f) {
                                        val center = (change1.position + change2.position) / 2f
                                        val previousCenter =
                                            (change1.previousPosition + change2.previousPosition) / 2f
                                        val pan = center - previousCenter

                                        val maxX = (size.width * (scale - 1)) / 2
                                        val maxY = (size.height * (scale - 1)) / 2

                                        offset = Offset(
                                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                            y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                        )
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            pointerCount = 0
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF Page $pageNumber",
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = offset.x
                            translationY = offset.y
                            scaleX = scale
                            scaleY = scale
                        },
                    contentScale = ContentScale.FillWidth
                )
            }

            // Nút reset zoom
//            if (scale > 1f) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(bottom = 8.dp),
//                    horizontalArrangement = Arrangement.Center
//                ) {
//                    AssistChip(
//                        onClick = {
//                            scale = 1f
//                            offset = Offset.Zero
//                        },
//                        label = { Text("Reset zoom") },
//                        leadingIcon = {
//                            Icon(
//                                imageVector = Icons.Filled.Check,
//                                contentDescription = null,
//                                modifier = Modifier.size(16.dp)
//                            )
//                        }
//                    )
//                }
//            }
        }
    }
}
