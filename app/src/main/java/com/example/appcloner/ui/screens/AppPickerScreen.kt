package com.example.appcloner.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appcloner.ui.viewmodel.AppPickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    onBack: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel()
) {
    val apps by viewModel.availableApps.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اختر تطبيقاً للنسخ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SearchBar(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // محتوى البحث النشط (فارغ حالياً)
                }
            }
            items(apps, key = { it.packageName }) { app ->
                ListItem(
                    headlineContent = { Text(app.label) },
                    supportingContent = { Text(app.packageName) },
                    leadingContent = {
                        app.icon?.let {
                            Image(
                                bitmap = it.toBitmap(96, 96).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(40.dp)
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        viewModel.addApp(app.packageName)
                        onBack()
                    }
                )
            }
        }
    }
}
