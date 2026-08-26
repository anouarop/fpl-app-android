package com.shellanddeploy.fpllive.ui.transfers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.Transfer
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.Pill
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.components.UpdatedLabel
import com.shellanddeploy.fpllive.ui.theme.DifficultyAmber
import com.shellanddeploy.fpllive.ui.theme.DifficultyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    viewModel: TransfersViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = DifficultyAmber)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Making transfers requires an authenticated FPL session, which the public API does not expose. Only transfer history is shown here. TODO/VERIFY: integrate an authenticated transfer flow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { ErrorBanner(state.error) }

            if (state.loading && state.transfers.isEmpty()) {
                items(6) { SkeletonBox(Modifier.fillMaxWidth().height(56.dp)) }
                return@LazyColumn
            }

            if (state.transfers.isEmpty()) {
                item { CenteredMessage("No transfers", subtitle = "This team has not made any transfers yet.") }
                return@LazyColumn
            }

            items(state.transfers.reversed(), key = { "${it.entry}:${it.event}:${it.time}" }) { transfer ->
                TransferRow(transfer, state.playerNames)
            }

            item {
                UpdatedLabel(state.lastUpdated, state.stale, Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun TransferRow(transfer: Transfer, playerNames: Map<Int, String>) {
    val inName = playerNames[transfer.elementIn] ?: "Player ${transfer.elementIn}"
    val outName = playerNames[transfer.elementOut] ?: "Player ${transfer.elementOut}"

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GW${transfer.event}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("OUT", DifficultyAmber)
                    Spacer(Modifier.width(6.dp))
                    Text(outName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("IN", DifficultyGreen)
                    Spacer(Modifier.width(6.dp))
                    Text(inName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
