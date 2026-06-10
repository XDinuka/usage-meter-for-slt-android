package com.xdinuka.sltusagemeter.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdinuka.sltusagemeter.ui.theme.StatusNormal
import com.xdinuka.sltusagemeter.ui.theme.StatusThrottled
import com.xdinuka.sltusagemeter.ui.theme.StatusUnknown
import com.xdinuka.sltusagemeter.ui.theme.StatusWarning

fun statusColor(status: String): Color = when (status.uppercase()) {
    "NORMAL", "ACTIVE" -> StatusNormal
    "THROTTLED" -> StatusThrottled
    "WARNING" -> StatusWarning
    else -> StatusUnknown
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = status,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
