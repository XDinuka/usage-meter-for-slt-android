package com.xdinuka.sltusagemeter.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdinuka.sltusagemeter.data.model.UsageDetail
import com.xdinuka.sltusagemeter.ui.theme.ProgressBlue

@Composable
fun UsageProgressBar(
    usage: UsageDetail,
    color: Color = ProgressBlue,
    modifier: Modifier = Modifier
) {
    val progress = (usage.percentage / 100f).coerceIn(0f, 1f)
    val isUnlimited = usage.limit == null

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = usage.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isUnlimited) {
                val used = usage.used ?: "0"
                val unit = usage.volumeUnit ?: "GB"
                Text(
                    text = "$used $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.padding(start = 4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0x269C27B0)
                ) {
                    Text(
                        text = "Unlimited",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                val used = usage.used ?: "0"
                val limit = usage.limit ?: "0"
                val unit = usage.volumeUnit ?: "GB"
                Text(
                    text = "$used / $limit $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (!isUnlimited) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = color,
                trackColor = Color.Gray.copy(alpha = 0.1f)
            )

            Spacer(Modifier.height(4.dp))

            Row {
                usage.expiryDate?.let { expiry ->
                    Text(
                        text = "Expires: $expiry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                usage.remaining?.let { remaining ->
                    val unit = usage.volumeUnit ?: "GB"
                    Text(
                        text = "Remaining: $remaining $unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.padding(start = 4.dp))
                }
                val remainingPct = (100 - usage.percentage).coerceIn(0, 100)
                Text(
                    text = "$remainingPct%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        } else {
            // Unlimited — just show expiry if available, no progress bar needed
            usage.expiryDate?.let { expiry ->
                Text(
                    text = "Expires: $expiry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PackageSummaryBar(
    title: String,
    used: String,
    limit: String?,       // null = unlimited
    unit: String,
    progress: Float,
    color: Color,
    remaining: String?,   // null = unlimited
    modifier: Modifier = Modifier
) {
    val isUnlimited = limit == null
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.05f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                if (isUnlimited) {
                    Text(
                        text = "$used $unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.padding(start = 4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x269C27B0)
                    ) {
                        Text(
                            text = "Unlimited",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "$used / $limit $unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isUnlimited) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = color,
                    trackColor = Color.Gray.copy(alpha = 0.1f)
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    remaining?.let {
                        Text(
                            text = "Remaining: $it $unit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.padding(start = 4.dp))
                    }
                    val usedVal = used.toFloatOrNull() ?: 0f
                    val limitVal = limit?.toFloatOrNull() ?: 1f
                    val pct = if (limitVal > 0) ((1f - usedVal / limitVal) * 100).toInt().coerceIn(0, 100) else 0
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
        }
    }
}
