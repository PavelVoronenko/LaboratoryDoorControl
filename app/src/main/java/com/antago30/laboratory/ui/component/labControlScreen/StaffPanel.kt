package com.antago30.laboratory.ui.component.labControlScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.R
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.ui.component.labControlScreen.model.StaffItem
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.Primary

@Composable
fun StaffPanel(
    staffList: List<StaffMember>,
    onStaffClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.06f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.employees),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = Modifier
                .padding(8.dp)
                .height(if (staffList.size > 4) 320.dp else Dp.Unspecified),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(staffList) { member ->
                StaffItem(
                    member = member,
                    enabled = enabled,
                    onClick = { onStaffClicked(member.id) }
                )
            }
        }
    }
}