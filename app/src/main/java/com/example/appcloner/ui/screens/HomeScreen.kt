package com.example.appcloner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appcloner.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToPicker: () -> Unit
) {
    val clonedApps by viewModel.clonedApps.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToPicker) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (clonedApps.isEmpty()) {
                Text(
                    text = "لا توجد تطبيقات منسوخة حالياً",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clonedApps) { pkgName ->
                        AppItem(
                            packageName = pkgName,
                            onLaunch = { viewModel.launchApp(pkgName) },
                            onStop = { viewModel.stopApp(pkgName) },
                            onRemove = { viewModel.removeApp(pkgName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(
    packageName: String,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = packageName, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onLaunch) {
                    Text("فتح")
                }
                OutlinedButton(onClick = onStop) {
                    Text("إيقاف")
                }
                TextButton(onClick = onRemove) {
                    Text("حذف")
                }
            }
        }
    }
}
