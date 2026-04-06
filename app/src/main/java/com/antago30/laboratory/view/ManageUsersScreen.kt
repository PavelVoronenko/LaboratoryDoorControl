package com.antago30.laboratory.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.ui.theme.Primary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import com.antago30.laboratory.viewmodel.manageUsersViewModel.kt.ManageUsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    viewModel: ManageUsersViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasSelection = users.any { it.isSelected }

    LaunchedEffect(Unit) { viewModel.fetchUsers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление пользователями") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            if (hasSelection && !isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.deleteSelected() },
                    icon = { Icon(Icons.Default.CheckCircle, "Удалить") },
                    text = { Text("Удалить выбранные") },
                    containerColor = Primary.copy(alpha = 0.9f)
                )
            }
        },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Список пользователей пуст или не загружен")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 🔥 УНИКАЛЬНЫЙ КЛЮЧ: комбинация id + mac гарантирует отсутствие дублей
                    items(users, key = { "${it.id}_${it.macAddress}" }) { user ->
                        UserListItem(user = user, onCheck = { viewModel.toggleSelection(user.id) })
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun UserListItem(
    user: com.antago30.laboratory.model.UserInfo,
    onCheck: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCheck
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = user.isSelected,
                onCheckedChange = { onCheck() }
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Text(user.macAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}