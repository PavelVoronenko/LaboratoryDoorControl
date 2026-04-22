package com.antago30.laboratory.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.UserInfo
import com.antago30.laboratory.ui.component.userManagementScreen.DeleteUserConfirmationDialog
import com.antago30.laboratory.ui.component.userManagementScreen.UserAddForm
import com.antago30.laboratory.ui.component.userManagementScreen.UserList
import com.antago30.laboratory.ui.component.userManagementScreen.UserManagementFab
import com.antago30.laboratory.ui.component.userManagementScreen.UserManagementTopBar
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.viewmodel.manageUsersViewModel.kt.UserManagementViewModel

@Composable
fun UserManagementScreen(
    viewModel: UserManagementViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    connectionManager: BleConnectionManager,
    onUserChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showError by viewModel.showError.collectAsState()
    val isAddingUser by viewModel.isAddingUser.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    // Состояния формы
    var showAddForm by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserInfo?>(null) }
    var userId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var selectedUuid by remember { mutableStateOf("") }
    var selectedServiceData by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }
    var rssiThreshold by remember { mutableStateOf("-70") }

    // ✅ Состояния для предупреждений об исчерпании опций
    val areUuidsExhausted by remember(users) {
        mutableStateOf(viewModel.areUuidOptionsExhausted())
    }
    val areServiceDataExhausted by remember(users) {
        mutableStateOf(viewModel.areServiceDataOptionsExhausted())
    }

    // Состояние подключения
    val isConnected by connectionManager.connectionStateFlow.collectAsState(
        ConnectionState.DISCONNECTED
    )

    // ✅ Обработка системной кнопки "Назад"
    BackHandler(enabled = showAddForm) {
        showAddForm = false
    }

    // ✅ Условный callback для кнопки в топбаре
    val onBackClick = {
        if (showAddForm) {
            showAddForm = false
        } else {
            onBack()
        }
    }

    // Обработка ошибок
    showError?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearError()
        }
    }

    // ✅ Авто-генерация данных при открытии формы
    LaunchedEffect(showAddForm, users) {
        if (showAddForm) {
            if (userId.isBlank()) {
                userId = viewModel.getNextAvailableId().toString()
            }
            if (selectedUuid.isBlank()) {
                selectedUuid = viewModel.getNextAvailableUuid()
            }
            if (selectedServiceData.isBlank()) {
                selectedServiceData = viewModel.getNextAvailableServiceData()
            }
        }
    }

    Scaffold(
        topBar = {
            UserManagementTopBar(
                onBack = onBackClick,
                title = if (showAddForm) "Новый пользователь" else "Пользователи"
            )
        },
        floatingActionButton = {
            // ✅ Анимированное появление/исчезновение FAB
            AnimatedContent(
                targetState = !showAddForm,
                label = "fabAnimation",
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(
                        animationSpec = tween(
                            150
                        )
                    )
                }
            ) { isVisible ->
                if (isVisible) {
                    UserManagementFab(
                        onClick = {
                            showAddForm = true
                            userName = ""
                            macAddress = "AABBCCDDEEFF"
                            rssiThreshold = "-70"
                            userId = viewModel.getNextAvailableId().toString()
                            if ((userId.toIntOrNull() ?: 1) > 10) userId = "10"
                            selectedUuid = viewModel.getNextAvailableUuid()
                            selectedServiceData = viewModel.getNextAvailableServiceData()
                        },
                        enabled = !showAddForm && isConnected == ConnectionState.READY
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        // ✅ Основной анимированный контент
        AnimatedContent(
            targetState = showAddForm,
            label = "screenTransition",
            transitionSpec = {
                val slideDirection = if (targetState) -1 else 1
                slideInVertically(animationSpec = tween(300)) { height ->
                    slideDirection * height
                } + fadeIn(animationSpec = tween(200)) togetherWith
                        slideOutVertically(animationSpec = tween(300)) { height ->
                            -slideDirection * height
                        } + fadeOut(animationSpec = tween(200))
            }
        ) { isFormVisible ->
            if (isFormVisible) {
                UserAddForm(
                    userId = userId,
                    userName = userName,
                    selectedUuid = selectedUuid,
                    selectedServiceData = selectedServiceData,
                    macAddress = macAddress,
                    rssiThreshold = rssiThreshold,
                    isLoading = isLoading,
                    isAddingUser = isAddingUser,
                    onUserNameChange = { userName = it },
                    isConnected = isConnected == ConnectionState.READY,
                    onMacAddressChange = { input ->
                        macAddress = input
                    },
                    onAdd = { params ->
                        viewModel.addUser(params)
                        showAddForm = false
                        userId = ""; userName = ""; macAddress = ""; rssiThreshold = "-70"
                        selectedUuid = ""; selectedServiceData = ""
                    },
                    onError = { viewModel.setError(it) },
                    getNextAvailableId = { viewModel.getNextAvailableId() },
                    areUuidsExhausted = areUuidsExhausted,
                    areServiceDataExhausted = areServiceDataExhausted,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    UserList(
                        users = users,
                        onDeleteClick = { id -> 
                            userToDelete = users.find { it.id == id }
                        },
                        isConnected = isConnected == ConnectionState.READY,
                        currentUserId = currentUserId,
                        onUserSelected = { userInfo ->
                            viewModel.selectCurrentUser(userInfo)
                            onUserChanged()
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Окно подтверждения удаления
                    userToDelete?.let { user ->
                        DeleteUserConfirmationDialog(
                            user = user,
                            onConfirm = {
                                viewModel.deleteUser(user.id)
                                userToDelete = null
                            },
                            onDismiss = { userToDelete = null }
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding( bottom = 24.dp),
                            color = Primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
        }
    }
}
