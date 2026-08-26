package com.shellanddeploy.fpllive.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.shellanddeploy.fpllive.domain.model.GameweekScore
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.Pill
import com.shellanddeploy.fpllive.ui.components.SectionTitle
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.theme.Indigo400
import com.shellanddeploy.fpllive.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.entry?.name ?: "History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item { ErrorBanner(state.error) }

            if (state.loading && state.history == null) {
                items(6) { SkeletonBox(Modifier.fillMaxWidth().height(56.dp)) }
                return@LazyColumn
            }

            val history = state.history
            if (history == null) {
                item { CenteredMessage("No history", subtitle = "Could not load season history.") }
                return@LazyColumn
            }

            if (history.seasons.isNotEmpty()) {
                item { SectionTitle("Past seasons") }
                item {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            history.seasons.forEachIndexed { i, season ->
                                if (i > 0) HorizontalDivider()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(season.seasonName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        "${season.totalPoints} pts",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Rank ${Format.ordinal(season.rank)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (history.gameweeks.isNotEmpty()) {
                item { SectionTitle("Gameweeks") }
                item {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            history.gameweeks.reversed().forEachIndexed { i, gw ->
                                if (i > 0) HorizontalDivider()
                                GameweekHistoryRow(gw)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameweekHistoryRow(gw: GameweekScore) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "GW${gw.event}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(52.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                "${gw.points} pts",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold, color = Indigo400),
            )
            Text(
                "Total ${gw.totalPoints}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "Rank ${Format.ordinal(gw.rank)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (gw.eventTransfers > 0) {
                Text(
                    "${gw.eventTransfers} transfer${if (gw.eventTransfers == 1) "" else "s"}${if (gw.eventTransfersCost > 0) " (-${gw.eventTransfersCost})" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        gw.chip?.let { chip ->
            Spacer(Modifier.width(8.dp))
            Pill(chip, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
