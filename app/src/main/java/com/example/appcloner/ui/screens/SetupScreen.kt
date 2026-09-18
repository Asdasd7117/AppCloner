package com.example.appcloner.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.appcloner.ui.viewmodel.SetupViewModel

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val hasWorkProfileState by viewModel.hasWorkProfile.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.checkWorkProfile()
            onSetupComplete()
        }
    }

    LaunchedEffect(hasWorkProfileState) {
        if (hasWorkProfileState) {
            onSetupComplete()
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "يتطلب التطبيق إعداد Work Profile لنسخ التطبيقات.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Button(
                    onClick = {
                        val intent = viewModel.createWorkProfileIntent()
                        launcher.launch(intent)
                    }
                ) {
                    Text("إنشاء Work Profile")
                }
            }
        }
    }
}
