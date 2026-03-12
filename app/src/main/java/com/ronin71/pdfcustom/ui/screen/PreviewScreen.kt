package com.ronin71.pdfcustom.ui.screen
import android.util.Log
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronin71.pdfcustom.model.MyPdfImage
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.model.MyPdfPage
import com.ronin71.pdfcustom.model.MyPdfParagraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    myPdfModelMain: MyPdfModelMain,
    onBack: () -> Boolean
) {
    Log.d("PreviewScreen", "Hiển thị preview với ${myPdfModelMain.pages.size} trang")

    // State cho chế độ hiển thị
    var showBitmapOnly by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("PDF Preview") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Nút chuyển đổi chế độ hiển thị
                    IconButton(onClick = { showBitmapOnly = !showBitmapOnly }) {
                        Icon(
                            if (showBitmapOnly) Icons.Filled.LocationOn
                            else Icons.Filled.Check,
                            contentDescription = if (showBitmapOnly) "Hiển thị cả text/image"
                            else "Chỉ hiển thị bitmap"
                        )
                    }
                    // Hiển thị trạng thái hiện tại
                    Text(
                        text = if (showBitmapOnly) "Bitmap only" else "Full",
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.DarkGray)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(myPdfModelMain.pages) { index, page ->
                    PdfPreviewPage(
                        page = page,
                        pageNumber = index + 1,
                        totalPages = myPdfModelMain.pages.size,
                        showBitmapOnly = showBitmapOnly
                    )
                }
            }
        }
    }
}

@Composable
fun PdfPreviewPage(
    page: MyPdfPage,
    pageNumber: Int,
    totalPages: Int,
    showBitmapOnly: Boolean
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Kích thước màn hình (trừ đi padding)
    val screenWidth = configuration.screenWidthDp.dp - 32.dp // 16.dp padding mỗi bên

    // Kích thước trang gốc (point)
    val originalWidth = page.size.width
    val originalHeight = page.size.height

    // Tính tỷ lệ scale để vừa với màn hình
    val scale = screenWidth.value / originalWidth

    // Kích thước hiển thị sau khi scale
    val displayWidth = screenWidth
    val displayHeight = with(density) { (originalHeight * scale).toDp() }

    Log.d("PdfPreviewPage", "Trang $pageNumber: Gốc ${originalWidth}x$originalHeight, Hiển thị ${displayWidth.value}x${displayHeight.value}, Scale: $scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header: số trang và thông tin
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trang $pageNumber / $totalPages",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // Thông tin kích thước
            Text(
                text = "${originalWidth.toInt()}x${originalHeight.toInt()} pt → ${displayWidth.value.toInt()}x${displayHeight.value.toInt()} dp",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        // Nội dung trang PDF
        Box(
            modifier = Modifier
                .width(displayWidth)
                .height(displayHeight)
                .background(Color.White)
                .border(1.dp, Color.Black)
                .clickable {
                    // Có thể thêm chức năng zoom khi click
                    Log.d("PdfPreviewPage", "Click vào trang $pageNumber")
                }
        ) {
            if (showBitmapOnly) {
                // Chế độ chỉ hiển thị bitmap - để debug
                Image(
                    bitmap = page.bitmapPage.asImageBitmap(),
                    contentDescription = "PDF Page $pageNumber",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                // Overlay thông báo đang ở chế độ bitmap
                Text(
                    text = "BITMAP ONLY",
                    color = Color.Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(8.dp)
                )
            } else {
                // Chế độ hiển thị đầy đủ text và image với scale

                // Vẽ images trước (làm nền)
                page.images.forEachIndexed { index, image ->
                    PdfImageDisplay(
                        image = image,
                        pageSize = page.size,
                        displayScale = scale,
                        index = index
                    )
                }

                // Vẽ text lên trên
                page.texts.forEachIndexed { index, paragraph ->
                    PdfTextDisplay(
                        paragraph = paragraph,
                        pageSize = page.size,
                        displayScale = scale,
                        index = index
                    )
                }

                // Debug: Hiển thị số lượng
                if (page.texts.isEmpty() && page.images.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Trang trống",
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Thông tin debug
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Text: ${page.texts.size}",
                color = Color.Blue,
                fontSize = 12.sp
            )
            Text(
                text = "Images: ${page.images.size}",
                color = Color.Green,
                fontSize = 12.sp
            )
            Text(
                text = "Bitmap: ${page.bitmapPage.width}x${page.bitmapPage.height}",
                color = Color.Magenta,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PdfImageDisplay(
    image: MyPdfImage,
    pageSize: Size,
    displayScale: Float,
    index: Int
) {
    val density = LocalDensity.current

    // Chuyển đổi tọa độ từ PDF (gốc dưới-trái) sang Android (gốc trên-trái) và áp dụng scale
    val left = with(density) { (image.rect.left * displayScale).toDp() }
    val top = with(density) { ((pageSize.height - image.rect.bottom) * displayScale).toDp() }
    val width = with(density) { ((image.rect.right - image.rect.left) * displayScale).toDp() }
    val height = with(density) { ((image.rect.bottom - image.rect.top) * displayScale).toDp() }

    Box(
        modifier = Modifier
            .offset(x = left, y = top)
            .size(width = width, height = height)
            .border(0.5.dp, Color.Green)
    ) {
        // Hiển thị image
        Image(
            bitmap = image.bitmap.asImageBitmap(),
            contentDescription = "Image $index",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Debug: Số thứ tự image và kích thước
        Text(
            text = "$index\n${width.value.toInt()}x${height.value.toInt()}",
            color = Color.Green,
            fontSize = 8.sp,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(2.dp)
        )
    }
}

@Composable
fun PdfTextDisplay(
    paragraph: MyPdfParagraph,
    pageSize: Size,
    displayScale: Float,
    index: Int
) {
    val density = LocalDensity.current

    // Chuyển đổi tọa độ từ PDF sang Android và áp dụng scale
    val left = with(density) { (paragraph.rect.left * displayScale).toDp() }
    val top = with(density) { ((pageSize.height - paragraph.rect.bottom) * displayScale).toDp() }
    val width = with(density) { ((paragraph.rect.right - paragraph.rect.left) * displayScale).toDp() }
    val height = with(density) { ((paragraph.rect.bottom - paragraph.rect.top) * displayScale).toDp() }

    // Kích thước text (chuyển từ point sang sp và áp dụng scale)
    val textSizeSp = with(density) { (paragraph.textSize * displayScale).toSp() }

    Box(
        modifier = Modifier
            .offset(x = left, y = top)
            .size(width = width, height = height)
            .border(0.5.dp, Color.Red)
    ) {
        // Hiển thị text
        Text(
            text = paragraph.text,
            fontSize = textSizeSp,
            color = Color.Black,
            modifier = Modifier.fillMaxSize()
        )

        // Debug: Số thứ tự paragraph
        Text(
            text = "$index",
            color = Color.Red,
            fontSize = 10.sp,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(2.dp)
                .align(Alignment.TopEnd)
        )
    }
}

