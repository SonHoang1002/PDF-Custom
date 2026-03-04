package com.ronin71.pdfcustom.ui.screen


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                withContext(Dispatchers.Main){
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
                text = "Print To Size",
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

            Spacer(modifier = Modifier.height(32.dp))

            // ===== Preview Area =====
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
                    selectedMyPdfModel == null -> {
                        Text("No PDF Selected")
                    }

                    isGenerating -> {
                        Text("Processing PDF...")
                    }

                    else -> {
                        Text("PDF Ready Preview Here")
                    }
                }
            }
        }
    }
}