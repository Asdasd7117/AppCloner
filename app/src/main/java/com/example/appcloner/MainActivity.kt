package com.example.appcloner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appcloner.admin.ProfileManager
import com.example.appcloner.ui.navigation.NavGraph
import com.example.appcloner.ui.theme.AppClonerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var profileManager: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppClonerTheme {
                NavGraph(profileManager = profileManager)
            }
        }
    }
}