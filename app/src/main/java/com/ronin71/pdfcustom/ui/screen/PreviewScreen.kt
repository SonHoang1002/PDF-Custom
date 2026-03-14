package com.ronin71.pdfcustom.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.util.SizeF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronin71.pdfcustom.common.aspectRatio
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.model.MyPdfPage
import com.ronin71.pdfcustom.util.MyPdfFilter
import com.ronin71.pdfcustom.util.MyRectUtil


val TAG = "PreviewScreen"

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    myPdfModelMain: MyPdfModelMain,
    onBack: () -> Boolean
) {
    val density = LocalDensity.current
    // Scroll state dùng chung cho cả 2 danh sách
    val lazyListState = rememberLazyListState()
    // Vị trí slider (0f -> 1f)
    var sliderPosition by remember { mutableStateOf(PointF(0.5f, 0.5f)) }
    val sliderWidth = 1.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Preview") },
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
            // Lưu lại maxHeight và maxWidth để dùng trong các Box con
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val thumbSize = 40.dp
            val thumbSizePx = with(density) { thumbSize.toPx() }

            // Danh sách gốc (hiển thị đầy đủ)
            LazyColumn(
                state = lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 15.dp, bottom = 15.dp)
            ) {
                itemsIndexed(myPdfModelMain.pages) { i, page ->
                    PdfPreviewPage(
                        density = density,
                        page = page,
                        pageNumber = i + 1,
                        totalPages = myPdfModelMain.pages.size,
                        maxHeightDp = maxHeight,
                        maxWidthDp = maxWidth,
                        sliderPosX = sliderPosition.x
                    )
                }
            }
            // Slider bar (thanh kéo) - ĐẶT CUỐI CÙNG để hiển thị trên cùng
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * sliderPosition.x - sliderWidth / 2,
                        y = 0.dp
                    )
                    .width(sliderWidth)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.3f))
                    .border(1.dp, Color.Red)
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * sliderPosition.x - thumbSize / 2,
                        y = maxHeight * sliderPosition.y - thumbSize / 2
                    )
                    .size(thumbSize)
                    .background(
                        color = Color.Red,
                        shape = CircleShape
                    )
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()

                                // Tính vị trí X mới
                                val deltaXDp = with(density) { dragAmount.x.toDp() }
                                val newPosX = sliderPosition.x +
                                        (deltaXDp.value / maxWidth.value)
                                // Tính vị trí Y mới (cho thumb)
                                val deltaYDp = with(density) { dragAmount.y.toDp() }
                                val newPosY = sliderPosition.y +
                                        (deltaYDp.value / maxHeight.value)

                                // Giới hạn trong khoảng 0-1
                                sliderPosition = PointF(
                                    newPosX.coerceIn(0f, 1f),
                                    newPosY.coerceIn(0f, 1f)
                                )
                            }
                        )
                    }
            )
        }
    }
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
    sliderPosX: Float, // 0f -> 1f
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
    val clipPositionX = displayWidthPx * sliderPosX // Vị trí clip theo pixel

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
                val clipWidth = maxWidthDp * sliderPosX + 1.dp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(
                            GenericShape { size, _ ->
                                val clipPx = with(density) { clipWidth.toPx() }
                                addRect(
                                    androidx.compose.ui.geometry.Rect(clipPx , 0f, size.width, size.height),

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