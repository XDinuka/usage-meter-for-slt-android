package com.xdinuka.sltusagemeter.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.xdinuka.sltusagemeter.data.model.PackageSummary
import com.xdinuka.sltusagemeter.data.model.UsageSummaryBundle
import com.xdinuka.sltusagemeter.ui.components.PackageSummaryBar
import com.xdinuka.sltusagemeter.ui.components.UsageProgressBar
import com.xdinuka.sltusagemeter.ui.components.statusColor
import com.xdinuka.sltusagemeter.ui.login.LoginUiState
import com.xdinuka.sltusagemeter.ui.theme.ProgressBlue
import com.xdinuka.sltusagemeter.ui.theme.ProgressGreen
import com.xdinuka.sltusagemeter.ui.theme.ProgressOrange
import com.xdinuka.sltusagemeter.ui.theme.ProgressPurple
import com.xdinuka.sltusagemeter.ui.usage.UsageUiState

// ── Inline add-account card ───────────────────────────────────────────────────

@Composable
fun AddAccountCard(
    loginState: LoginUiState,
    onLogin: (String, String) -> Unit,
    onResetLoginState: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isLoading = loginState is LoginUiState.Loading

    // Track loading→idle transition to detect success and auto-collapse
    var wasLoading by remember { mutableStateOf(false) }
    LaunchedEffect(loginState) {
        if (wasLoading && loginState is LoginUiState.Idle) {
            expanded = false
            email = ""
            password = ""
        }
        wasLoading = isLoading
    }

    fun collapse() {
        expanded = false
        onResetLoginState()
        email = ""
        password = ""
    }

    OutlinedCard(
        // Tapping anywhere on the collapsed card expands it; expanded card is not clickable
        onClick = { if (!expanded && !isLoading) expanded = true },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column {
            // ── Collapsed header — hidden once the form is open ─────────────
            AnimatedVisibility(
                visible = !expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Add account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Login form — shown while expanded ──────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onLogin(email, password)
                            }
                        ),
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (loginState is LoginUiState.Error) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = loginState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Circular cancel button
                        IconButton(
                            onClick = { focusManager.clearFocus(); collapse() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Sign in button
                        Button(
                            onClick = { focusManager.clearFocus(); onLogin(email, password) },
                            enabled = !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Signing in…")
                            } else {
                                Text("Sign In", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Use your MySLT portal credentials (myslt.slt.lk)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Account usage card ────────────────────────────────────────────────────────

@Composable
fun AccountUsageCard(
    card: AccountCardState,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (card.telephoneNo.isNotEmpty()) {
                        Text(
                            text = card.telephoneNo,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.username,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        card.lastFetchedAt?.let { ts ->
                            Text(
                                text = "  ·  ${relativeTime(ts)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                if (card.usageState is UsageUiState.Success) {
                    val status = card.usageState.summary.status ?: ""
                    if (status.isNotBlank()) StatusBadge(status)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.padding(4.dp))
                }
                IconButton(onClick = { showRemoveConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove account",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            HorizontalDivider()

            when (val s = card.usageState) {
                is UsageUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                is UsageUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ProgressOrange)
                        Spacer(Modifier.height(8.dp))
                        Text(s.message, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }
                is UsageUiState.Success -> {
                    CardUsageContent(summary = s.summary, vasBundles = s.vasBundles)
                }
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove account?") },
            text = { Text("This will remove \"${card.username}\" and all its data from the app.") },
            confirmButton = {
                TextButton(onClick = { showRemoveConfirm = false; onRemove() }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
    val color = statusColor(status)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CardUsageContent(
    summary: UsageSummaryBundle,
    vasBundles: List<com.xdinuka.sltusagemeter.data.model.UsageDetail>
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        val status = summary.status ?: ""
        if (status.isNotBlank()) {
            val color = statusColor(status)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(status, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
            }
        }
        summary.myPackageInfo?.let { pkg ->
            pkg.packageName?.let { name ->
                if (name.isNotBlank()) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            pkg.usageDetails?.forEach { detail ->
                UsageProgressBar(usage = detail, color = ProgressBlue)
                Spacer(Modifier.height(10.dp))
            }
        }
        summary.bonusDataSummary?.let { bonus ->
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            SummaryBar(summary = bonus, color = ProgressPurple, title = "Bonus Data")
            Spacer(Modifier.height(8.dp))
        }
        summary.extraGbDataSummary?.let { extra ->
            SummaryBar(summary = extra, color = ProgressOrange, title = "Extra GB")
            Spacer(Modifier.height(8.dp))
        }
        if (vasBundles.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Text(
                text = "Add-on Bundles",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            vasBundles.forEach { vas ->
                UsageProgressBar(usage = vas, color = ProgressGreen)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SummaryBar(summary: PackageSummary, color: Color, title: String) {
    val used = summary.used ?: "0"
    val unit = summary.volumeUnit ?: "GB"
    if (summary.limit == null) {
        PackageSummaryBar(title, used, null, unit, 0f, color, null)
        return
    }
    val limitStr = summary.limit!!
    val usedVal = used.toFloatOrNull() ?: 0f
    val limitVal = limitStr.toFloatOrNull() ?: 1f
    val progress = if (limitVal > 0) usedVal / limitVal else 0f
    val remaining = "%.2f".format((limitVal - usedVal).coerceAtLeast(0f))
    PackageSummaryBar(title, used, limitStr, unit, progress, color, remaining)
}

// ── Utilities ─────────────────────────────────────────────────────────────────

fun relativeTime(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
