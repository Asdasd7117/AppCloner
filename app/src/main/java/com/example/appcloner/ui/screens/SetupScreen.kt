package com.example.appcloner.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appcloner.admin.ProfileManager

@Composable
fun SetupScreen(
    profileManager: ProfileManager,
    onSetupComplete: () -> Unit
) {
    var hasProfile by remember { mutableStateOf(profileManager.hasWorkProfile()) }

    // مشغل الواجهة الخاصة بإعداد Work Profile المعيارية من نظام Android
    val setupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        hasProfile = profileManager.hasWorkProfile()
        if (result.resultCode == Activity.RESULT_OK || hasProfile) {
            onSetupComplete()
        }
    }

    LaunchedEffect(hasProfile) {
        if (hasProfile) {
            onSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "إعداد البيئة المعزولة",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "الحالة الحالية:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                Text("• Work Profile: ${if (hasProfile) "✅ مفعل" else "❌ غير مفعل"}")
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!hasProfile) {
            Text(
                text = "إنشاء بيئة عمل معزولة",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "اضغط على الزر أدناه لبدء إنشاء Work Profile مباشرة عبر النظام وبدون كمبيوتر.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val intent = profileManager.createWorkProfileIntent()
                    setupLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إنشاء البيئة المعزولة الآن")
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSetupComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("متابعة على أي حال")
        }
    }
}
