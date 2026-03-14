package com.ronin71.pdfcustom.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.viewmodel.MyPdfViewModel

@Composable
fun HomeScreen(
    viewModel: MyPdfViewModel = viewModel(),
    onNavigateToPreview: (MyPdfModelMain) -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val pdfModel by viewModel.pdfModel.collectAsState()
    val error by viewModel.error.collectAsState()

    // Điều hướng khi có pdfModel
    LaunchedEffect(pdfModel) {
        pdfModel?.let {
            Log.d("HomeScreen", "Điều hướng sang Preview: progress = ${progress.toInt()}")
            onNavigateToPreview(it)
            viewModel.clear()

        }
    }

    // Hiển thị lỗi
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Log.d("HomeScreen", "Chọn file PDF: $uri")
            viewModel.extractPdf(context, it)
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
            Text(
                text = "PDF Custom",
                fontSize = 26.sp,
                style = MaterialTheme.typography.headlineMedium
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { launcher.launch("application/pdf") },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import PDF")
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Progress bar
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Phần trăm
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Chi tiết trang
                            if (totalPages > 0) {
                                Text(
                                    text = "Trang $currentPage / $totalPages",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            // Trạng thái
                            Text(
                                text = getProcessingStatus(progress, currentPage, totalPages),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getProcessingStatus(progress: Float, currentPage: Int, totalPages: Int): String {
    return when {
        progress < 0.1f -> "Đang copy file..."
        progress < 0.2f -> "Đang mở file PDF..."
        totalPages > 0 -> "Đang xử lý trang $currentPage/$totalPages..."
        else -> "Đang xử lý..."
    }
}

