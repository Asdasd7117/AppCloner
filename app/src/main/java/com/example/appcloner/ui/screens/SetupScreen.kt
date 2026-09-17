package com.example.appcloner.ui.screens

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
    val isDeviceOwner = profileManager.isDeviceOwner()
    val hasProfile = profileManager.hasWorkProfile()

    LaunchedEffect(isDeviceOwner, hasProfile) {
        if (isDeviceOwner && hasProfile) onSetupComplete()
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
                Text("• Device Owner: ${if (isDeviceOwner) "✅" else "❌"}")
                Text("• Work Profile: ${if (hasProfile) "✅" else "❌"}")
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!isDeviceOwner) {
            Text(
                text = "الخطوة 1: فعّل التطبيق كـ Device Owner",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "نفّذ الأمر التالي عبر ADB على الكمبيوتر:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "adb shell dpm set-device-owner\ncom.example.appcloner/.admin.DeviceAdmin",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { /* سيتم إعادة التحقق عند العودة */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تحقق من الحالة")
            }
        } else if (!hasProfile) {
            Text(
                text = "الخطوة 2: أنشئ Work Profile",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "سيتم إنشاء بيئة عمل معزولة على جهازك.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { /* profileManager.createWorkProfile() */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إنشاء البيئة المعزولة")
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