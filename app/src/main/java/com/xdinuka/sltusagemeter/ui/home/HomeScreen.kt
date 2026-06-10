package com.xdinuka.sltusagemeter.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val hasNoProfiles by viewModel.hasNoProfiles.collectAsStateWithLifecycle()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Empty state (no profiles at all) ────────────────────────────────
        AnimatedVisibility(
            visible = hasNoProfiles,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            EmptyState(
                loginState = loginState,
                onLogin = viewModel::login,
                onResetLoginState = viewModel::resetLoginState
            )
        }

        // ── Account cards ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !hasNoProfiles,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = { viewModel.refreshAll() }
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // Inline add-account card at the top
                    item {
                        AddAccountCard(
                            loginState = loginState,
                            onLogin = viewModel::login,
                            onResetLoginState = viewModel::resetLoginState
                        )
                    }

                    items(
                        items = cards,
                        key = { "${it.profileId}/${it.telephoneNo}" }
                    ) { card ->
                        AccountUsageCard(
                            card = card,
                            onRefresh = { viewModel.refresh(card.profileId, card.telephoneNo) },
                            onRemove = { viewModel.removeProfile(card.profileId) }
                        )
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    loginState: com.xdinuka.sltusagemeter.ui.login.LoginUiState,
    onLogin: (String, String) -> Unit,
    onResetLoginState: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "No accounts yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add your MySLT account to start tracking\nyour broadband usage.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        Spacer(Modifier.height(32.dp))
        // Inline add-account card in the empty state too
        AddAccountCard(
            loginState = loginState,
            onLogin = onLogin,
            onResetLoginState = onResetLoginState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
