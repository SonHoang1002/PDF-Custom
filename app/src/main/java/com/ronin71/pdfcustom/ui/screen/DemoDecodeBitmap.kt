package com.ronin71.pdfcustom.ui.screen


import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ronin71.pdfcustom.util.DemoPdfThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DemoDecodeBitmap() {
    val context = LocalContext.current;
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var customBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLightMode by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) {
        if (it != null) {
            selectedBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            customBitmap = selectedBitmap
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
            DemoBuildButtons(
                selectedBitmap = selectedBitmap,
                isLightMode = isLightMode,
                onChangeLightMode = {
                    /// Xử lý Bitmap để thay đổi theme mode của bitmap
                    isLightMode = it
                    scope.launch(Dispatchers.Default) {
                        if (it) {
                            customBitmap = selectedBitmap
                        } else {
                            selectedBitmap?.let {
                                customBitmap = DemoPdfThemeMode().applyDarkModeToBitmap(it)
                            }
                        }
                    }

                },
                onClick = {
                    if (selectedBitmap == null) {
                        launcher.launch(
                            PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                .setMaxItems(2)
                                .build()
                        )
                    } else {
                        selectedBitmap = null
                        customBitmap = null
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DemoBuildPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                customBitmap = customBitmap,
            )
        }
    }
}

@Composable
fun DemoBuildButtons(
    selectedBitmap: Bitmap?,
    onClick: () -> Unit,
    isLightMode: Boolean,
    onChangeLightMode: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier.weight(1f),
            ) {
                if (selectedBitmap == null) {
                    Icon(Icons.Default.Add, null)
                } else {
                    Icon(Icons.Default.Delete, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (selectedBitmap == null) {
                    Text("Import Image")
                } else {
                    Text("Clear Image")
                }
            }
        }

        if (selectedBitmap != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!isLightMode) "Dark Mode" else "Light Mode",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = isLightMode,
                    onCheckedChange = onChangeLightMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun DemoBuildPreview(
    modifier: Modifier,
    customBitmap: Bitmap?,
) {
    Box(
        modifier = modifier
    ) {
        if (customBitmap != null) {
            Image(
                customBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No Image Selected")
            }
        }


    }
}
