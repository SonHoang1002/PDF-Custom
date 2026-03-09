package com.ronin71.pdfcustom.ui.screen


import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.ronin71.pdfcustom.model.MyPdfModel
import com.ronin71.pdfcustom.pdf_extractor.PdfExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen() {

    val context = LocalContext.current
    var selectedMyPdfModel by remember { mutableStateOf<MyPdfModel?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var isReadyToEdit by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isGenerating = true
            isReadyToEdit = false

            scope.launch {
                val uri1 = uri
                if (uri1 != null) {
                    val result = withContext(Dispatchers.IO) {
                        PdfExtractor().extract(context, uri)
                    }
                    selectedMyPdfModel = result
                }
                withContext(Dispatchers.Main) {
                    isGenerating = false
                    isReadyToEdit = true
                }
            }
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

            // ===== App Name =====
            Text(
                text = "PDF Custom",
                fontSize = 26.sp,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Import / Loading / Edit Button =====
            when {
                isGenerating -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...")
                    }
                }

                isReadyToEdit -> {
                    Button(
                        onClick = {
                            // TODO: navigate to Edit screen
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit PDF")
                    }
                }

                else -> {
                    Button(
                        onClick = { launcher.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import PDF")
                    }
                }
            }

            when (selectedMyPdfModel) {
                null -> {

                }

                else -> {
                    Button(
                        onClick = {
                            selectedMyPdfModel = null
                            isReadyToEdit = false
                            isGenerating = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove PDF")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))


            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = Color.DarkGray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                when {
                    isGenerating -> {
                        Text("Processing PDF...")
                    }

                    selectedMyPdfModel == null -> {
                        Text("No PDF Selected")
                    }

                    else -> {
                        PdfPreview(pdfModel = selectedMyPdfModel!!)
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPreview(pdfModel: MyPdfModel) {
    // State cho zoom và pan của từng item
    val scaleStates = remember {
        pdfModel.images.map {
            mutableStateOf(1f)
        }.toTypedArray()
    }

    val offsetStates = remember {
        pdfModel.images.map {
            mutableStateOf(Offset.Zero)
        }.toTypedArray()
    }

    // Scroll state
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            pdfModel.images.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pages found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                // Column với scroll
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    pdfModel.images.forEachIndexed { index, bitmap ->
                        PdfPageItem(
                            bitmap = bitmap,
                            pageNumber = index + 1,
                            totalPages = pdfModel.images.size,
                            pageText = pdfModel.listText.getOrElse(index) { "" },
                            scaleState = scaleStates[index],
                            offsetState = offsetStates[index]
                        )
                    }
                }
            }
        }

        // Hiển thị thông tin file
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = MaterialTheme.shapes.small,
            tonalElevation = 4.dp
        ) {
            Text(
                text = "${pdfModel.images.size} page${if (pdfModel.images.size > 1) "s" else ""}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun PdfPageItem(
    bitmap: Bitmap,
    pageNumber: Int,
    totalPages: Int,
    pageText: String = "",
    scaleState: MutableState<Float>,
    offsetState: MutableState<Offset>
) {
    var scale by scaleState
    var offset by offsetState

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page header (không scale)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

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

                    // Zoom indicator cho từng page
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
                }
            }

            // Page content - CHỈ SCALE PHẦN NÀY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Giới hạn scale từ 1x đến 5x
                            scale = (scale * zoom).coerceIn(1f, 5f)

                            // Giới hạn pan
                            if (scale > 1f) {
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

            // Nút reset zoom cho từng page (tùy chọn)
            if (scale > 1f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AssistChip(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        label = { Text("Reset zoom") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}
