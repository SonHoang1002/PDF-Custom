package com.ronin71.pdfcustom

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.ui.screen.HomeScreen
import com.ronin71.pdfcustom.ui.screen.PreviewScreen
import com.ronin71.pdfcustom.util.MyPdfAnalyzer
import com.ronin71.pdfcustom.util.copyUriToCache
import java.io.File

@Composable
fun AppNavigation(navController: NavHostController) {
    var uri: Uri? by remember { mutableStateOf(null) }
    var pdfCacheFile: File? by remember {mutableStateOf(null)}
    var cacheUri: Uri? by remember { mutableStateOf(null) }
    var sharedViewModel by remember { mutableStateOf<MyPdfModelMain?>(null) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToPreview = { it ->
                    Log.d("AppNavigation", "AppNavigation to preview")
                    uri = it
                    pdfCacheFile = copyUriToCache(navController.context, it)
                    navController.navigate("preview")
                }
            )
        }

        composable("preview") {
            uri?.let { it ->
                PreviewScreen(
                    uri = it,
                    cacheUri = cacheUri,
                    pdfCacheFile = pdfCacheFile!!,
                    cacheSharedViewModel = sharedViewModel,
                    onBack = {
                        cacheUri = it
                        navController.popBackStack("home", inclusive = false)
                        true
                    },
                )
            }
        }
    }
}
