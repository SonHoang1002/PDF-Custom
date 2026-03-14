package com.ronin71.pdfcustom

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.ui.screen.HomeScreen
import com.ronin71.pdfcustom.ui.screen.PreviewScreen
import kotlin.math.log

@Composable
fun AppNavigation(navController: NavHostController) {


    var sharedViewModel by remember { mutableStateOf<MyPdfModelMain?>(null) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToPreview = { pdfModel ->
                    Log.d("AppNavigation", "AppNavigation to preview")
                    sharedViewModel = pdfModel // Lưu vào shared VM
                    navController.navigate("preview")

                }
            )
        }

        composable("preview") {
            sharedViewModel?.let { model ->
                PreviewScreen(
                    myPdfModelMain = model,
                    onBack = {
                        sharedViewModel = null
                        navController.popBackStack("home", inclusive = false)

                        true
                    }
                )
            }
        }
    }
}
