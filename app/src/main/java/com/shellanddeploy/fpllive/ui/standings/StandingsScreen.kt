package com.shellanddeploy.fpllive.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.GameweekScore
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.SectionTitle
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.theme.Indigo400
import com.shellanddeploy.fpllive.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    viewModel: StandingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Standings") },
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

            if (state.loading && state.entry == null) {
                items(4) { SkeletonBox(Modifier.fillMaxWidth().height(80.dp)) }
                return@LazyColumn
            }

            state.entry?.let { entry ->
                item {
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Overall rank", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Format.ordinal(entry.summaryOverallRank),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold, color = Indigo400),
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    entry.summaryOverallPoints.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }
            }

            if (state.gameweeks.isNotEmpty()) {
                item { SectionTitle("Rank by gameweek") }
                item {
                    Card {
                        val best = (state.gameweeks.maxOfOrNull { it.overallRank } ?: 0).coerceAtLeast(1)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.gameweeks.reversed().take(20).forEach { gw ->
                                RankBar(gw, best)
                            }
                        }
                    }
                }
            }

            if (state.seasons.isNotEmpty()) {
                item { SectionTitle("Past seasons") }
                item {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.seasons.forEachIndexed { i, season ->
                                if (i > 0) HorizontalDivider()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(season.seasonName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text("Rank ${Format.ordinal(season.rank)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(12.dp))
                                    Text("${season.totalPoints} pts", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            if (state.entry == null && !state.loading) {
                item { CenteredMessage("No data", subtitle = "Could not load standings.") }
            }
        }
    }
}

@Composable
private fun RankBar(gw: GameweekScore, best: Int) {
    val fraction = (gw.overallRank.toFloat() / best.toFloat()).coerceIn(0.02f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "GW${gw.event}",
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.width(48.dp),
        )
        Box(
            Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Indigo400),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            Format.ordinal(gw.overallRank),
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
        )
    }
}
