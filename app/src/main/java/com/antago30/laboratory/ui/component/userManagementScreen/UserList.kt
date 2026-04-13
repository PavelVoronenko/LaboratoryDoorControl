package com.antago30.laboratory.ui.component.userManagementScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.model.UserInfo
import com.antago30.laboratory.ui.theme.Primary

@Composable
fun UserList(
    modifier: Modifier = Modifier,
    users: List<UserInfo>,
    onDeleteClick: (Int) -> Unit,
    isConnected: Boolean = true,
    currentUserId: String? = null,
    onUserSelected: (UserInfo) -> Unit = {}

) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (users.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Список пользователей пуст",
                                fontSize = 16.sp,
                                color = Color(0xFFA0AEC0),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Нажмите + чтобы добавить",
                                fontSize = 13.sp,
                                color = Primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            items(users, key = { "${it.id}_${it.macAddress}" }) { user ->
                UserCard(
                    user = user,
                    onDeleteClick = { onDeleteClick(user.id) },
                    isConnected = isConnected,
                    isSelected = user.id.toString() == currentUserId,
                    onSelect = { onUserSelected(user) }
                )
            }
        }
    }
}
