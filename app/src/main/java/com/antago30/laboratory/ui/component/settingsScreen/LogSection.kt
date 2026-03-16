package com.antago30.laboratory.ui.component.settingsScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.ui.component.settingsScreen.model.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogItem(
    log: LogEntry,
    modifier: Modifier = Modifier
) {
    val sdfTime = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val sdfDate = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val time = sdfTime.format(Date(log.timestamp))
    val date = sdfDate.format(Date(log.timestamp))

    val sourceColor = when (log.source) {
        "SYSTEM" -> Color(0xFF8888FF) // синий
        "SENSOR" -> Color(0xFF88FF88) // зелёный
        "BLE"    -> Color(0xFFFFAA88) // оранжевый
        else     -> Color(0xFF888888)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D35)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(time, fontSize = 13.sp, color = Color(0xFFAAAAAA), modifier = Modifier.padding(top = 0.dp))
                Text(date, fontSize = 13.sp, color = Color(0xFFAAAAAA), modifier = Modifier.padding(top = 0.dp))
            }
            Text(
                log.message,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}