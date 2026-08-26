package com.shellanddeploy.fpllive.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.LiveBadge
import com.shellanddeploy.fpllive.ui.components.Pill
import com.shellanddeploy.fpllive.ui.components.SectionTitle
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.theme.Indigo400
import com.shellanddeploy.fpllive.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenGameweeks: () -> Unit,
    onOpenStandings: () -> Unit,
    onOpenTransfers: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenLeagues: () -> Unit,
    onOpenTeam: (Int) -> Unit,
    onOpenPlayer: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(
                        "FPL Live",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                item { ErrorBanner(state.error) }

                if (state.loading && state.bootstrap == null) {
                    items(5) {
                        SkeletonBox(Modifier.fillMaxWidth().height(88.dp))
                    }
                } else {
                    item { GameweekCard(state) }

                    state.entry?.let { entry ->
                        item { ManagerCard(entry, onClick = { onOpenTeam(entry.id) }) }
                    }

                    if (state.topPlayers.isNotEmpty()) {
                        item { SectionTitle("Top points") }
                        items(state.topPlayers, key = { it.id }) { player ->
                            TopPlayerRow(player, onClick = { onOpenPlayer(player.id) })
                        }
                    }

                    item { SectionTitle("Explore") }
                    item {
                        QuickLink(Icons.Filled.DateRange, "Gameweeks", "Fixtures, deadlines & scores", onOpenGameweeks)
                    }
                    item {
                        QuickLink(Icons.Filled.Leaderboard, "Standings", "Your rank over the season", onOpenStandings)
                    }
                    item {
                        QuickLink(Icons.Filled.SwapHoriz, "Transfers", "Transfer history & plans", onOpenTransfers)
                    }
                    item {
                        QuickLink(Icons.Filled.History, "History", "Full season history", onOpenHistory)
                    }
                    item {
                        QuickLink(Icons.Filled.Groups, "Leagues", "Private mini-leagues", onOpenLeagues)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameweekCard(state: HomeUiState) {
    val gw = state.currentGameweek
    Card {
        if (gw == null) {
            CenteredMessage("No gameweek info", subtitle = "Pull to refresh.")
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(gw.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Deadline ${Format.deadline(gw.deadlineTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    gw.isCurrent -> Pill("Current", MaterialTheme.colorScheme.primary)
                    gw.isNext -> Pill("Next", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    gw.finished -> Pill("Finished", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.liveFixtures > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveBadge()
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${
                            if (state.liveFixtures == 1) "1 match" else "${state.liveFixtures} matches"
                        } in play",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagerCard(entry: com.shellanddeploy.fpllive.domain.model.Entry, onClick: () -> Unit) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${entry.playerFirstName} ${entry.playerLastName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.ordinal(entry.summaryOverallRank),
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold, color = Indigo400),
                )
                Text("Overall rank", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TopPlayerRow(player: Player, onClick: () -> Unit) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        ) {
            Column(Modifier.weight(1f)) {
                Text(player.webName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    Format.price(player.nowCost),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                player.totalPoints.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold, color = Indigo400),
            )
        }
    }
}

@Composable
private fun QuickLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
