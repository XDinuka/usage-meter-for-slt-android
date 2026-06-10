package com.xdinuka.sltusagemeter.ui.usage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xdinuka.sltusagemeter.data.model.PackageSummary
import com.xdinuka.sltusagemeter.data.model.UsageSummaryBundle
import com.xdinuka.sltusagemeter.ui.components.PackageSummaryBar
import com.xdinuka.sltusagemeter.ui.components.UsageProgressBar
import com.xdinuka.sltusagemeter.ui.components.statusColor
import com.xdinuka.sltusagemeter.ui.theme.ProgressBlue
import com.xdinuka.sltusagemeter.ui.theme.ProgressGreen
import com.xdinuka.sltusagemeter.ui.theme.ProgressOrange
import com.xdinuka.sltusagemeter.ui.theme.ProgressPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
    profileId: String,
    subscriberID: String,
    viewModel: UsageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(profileId, subscriberID) {
        viewModel.loadUsage(profileId, subscriberID)
    }

    when (val s = state) {
        is UsageUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Fetching usage data...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is UsageUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = ProgressOrange,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(s.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadUsage(profileId, subscriberID) }) {
                        Text("Retry")
                    }
                }
            }
        }

        is UsageUiState.Success -> {
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = { viewModel.loadUsage(profileId, subscriberID) }
            ) {
                UsageContent(summary = s.summary, vasBundles = s.vasBundles)
            }
        }
    }
}

@Composable
private fun UsageContent(
    summary: UsageSummaryBundle,
    vasBundles: List<com.xdinuka.sltusagemeter.data.model.UsageDetail>
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { StatusCard(summary = summary) }

        summary.myPackageInfo?.let { pkgInfo ->
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = pkgInfo.packageName ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(pkgInfo.usageDetails ?: emptyList()) { usage ->
                UsageProgressBar(usage = usage, color = ProgressBlue)
                Spacer(Modifier.height(12.dp))
            }
        }

        summary.bonusDataSummary?.let { bonus ->
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Text(
                    text = "Bonus Data",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                SummaryBar(summary = bonus, color = ProgressPurple, title = "Bonus Data")
            }
        }

        summary.extraGbDataSummary?.let { extra ->
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Extra GB",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                SummaryBar(summary = extra, color = ProgressOrange, title = "Extra GB")
            }
        }

        if (vasBundles.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Text(
                    text = "Add-on Bundles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(vasBundles) { bundle ->
                UsageProgressBar(usage = bundle, color = ProgressGreen)
                Spacer(Modifier.height(12.dp))
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatusCard(summary: UsageSummaryBundle) {
    val status = summary.status ?: ""
    val color = statusColor(status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SummaryBar(summary: PackageSummary, color: Color, title: String) {
    val usedStr = summary.used ?: "0"
    val unitStr = summary.volumeUnit ?: "GB"
    val isUnlimited = summary.limit == null

    if (isUnlimited) {
        PackageSummaryBar(
            title = title,
            used = usedStr,
            limit = null,
            unit = unitStr,
            progress = 0f,
            color = color,
            remaining = null
        )
        return
    }

    val limitStr = summary.limit!!
    val usedVal = usedStr.toFloatOrNull() ?: 0f
    val limitVal = limitStr.toFloatOrNull() ?: 1f
    val progress = if (limitVal > 0) usedVal / limitVal else 0f
    val remaining = "%.2f".format((limitVal - usedVal).coerceAtLeast(0f))

    PackageSummaryBar(
        title = title,
        used = usedStr,
        limit = limitStr,
        unit = unitStr,
        progress = progress,
        color = color,
        remaining = remaining
    )
}
