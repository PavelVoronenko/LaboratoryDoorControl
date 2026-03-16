package com.antago30.laboratory.ui.component.settingsScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSection(
    staffNames: List<String>,
    onAddUser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedUser by remember { mutableStateOf(staffNames.first()) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D35)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Добавить событие для:",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ExposedDropdownMenuBox(
                expanded = isMenuExpanded,
                onExpandedChange = { isMenuExpanded = it }
            ) {
                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(0xFF1E1E25),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { isMenuExpanded = true }
                        .padding(12.dp)
                ) {
                    Text(selectedUser, fontSize = 16.sp, color = Color.White)
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    staffNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name, color = Color.White) },
                            onClick = {
                                selectedUser = name
                                isMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = { onAddUser(selectedUser) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DBAFF)),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("Добавить пользователя", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}