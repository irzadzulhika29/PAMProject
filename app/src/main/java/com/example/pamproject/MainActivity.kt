package com.example.pamproject

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.pamproject.api.SupabaseConfig
import com.example.pamproject.navigation.AppNavigation
import com.example.pamproject.ui.theme.PAMProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            Log.i("MainActivity", "REST endpoint: ${SupabaseConfig.BASE_REST_URL}")
        }.start()
        enableEdgeToEdge()
        setContent {
            PAMProjectTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
