package com.ronin71.pdfcustom

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.ui.screen.HomeScreen
import com.ronin71.pdfcustom.ui.screen.PreviewScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    var sharedViewModel: MyPdfModelMain? = null

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToPreview = { pdfModel ->
                    sharedViewModel = pdfModel // Lưu vào shared VM
                    navController.navigate("preview")
                }
            )
        }

        composable("preview") {
            PreviewScreen(
                myPdfModelMain = sharedViewModel!!,
                onBack = {
                    sharedViewModel = null
                    navController.popBackStack()
                }
            )
        }
    }
}
