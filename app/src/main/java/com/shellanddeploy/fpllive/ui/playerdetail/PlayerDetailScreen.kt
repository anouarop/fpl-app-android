package com.shellanddeploy.fpllive.ui.playerdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.domain.model.PlayerFixture
import com.shellanddeploy.fpllive.domain.model.PlayerHistory
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.DifficultyBadge
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.LiveBadge
import com.shellanddeploy.fpllive.ui.components.Pill
import com.shellanddeploy.fpllive.ui.components.SectionTitle
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.components.StatChip
import com.shellanddeploy.fpllive.ui.components.UpdatedLabel
import com.shellanddeploy.fpllive.ui.theme.DifficultyAmber
import com.shellanddeploy.fpllive.ui.theme.DifficultyGreen
import com.shellanddeploy.fpllive.ui.theme.DifficultyRed
import com.shellanddeploy.fpllive.ui.theme.Indigo400
import com.shellanddeploy.fpllive.util.Format

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerDetailScreen(
    viewModel: PlayerDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startPolling()
                Lifecycle.Event.ON_STOP -> viewModel.stopPolling()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.player?.webName ?: "Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBox(Modifier.fillMaxWidth().height(90.dp))
                SkeletonBox(Modifier.fillMaxWidth().height(140.dp))
                SkeletonBox(Modifier.fillMaxWidth().height(120.dp))
                SkeletonBox(Modifier.fillMaxWidth().height(160.dp))
            }
            return@Scaffold
        }

        val player = state.player
        if (player == null) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                ErrorBanner(state.error, Modifier.padding(16.dp))
                if (state.error != null) {
                    TextButton(onClick = viewModel::retry, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Retry")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Header(player = player, state = state)
            Spacer(Modifier.height(12.dp))

            ErrorBanner(state.error)

            if (state.matchInProgress && state.liveFixture != null) {
                LiveMatchSection(state)
                Spacer(Modifier.height(12.dp))
            } else {
                NextMatchSection(player, state)
                Spacer(Modifier.height(12.dp))
            }

            SeasonStatsSection(player)
            Spacer(Modifier.height(12.dp))

            UpcomingFixturesSection(state)
            Spacer(Modifier.height(12.dp))

            LastFiveSection(state)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header(player: Player, state: PlayerDetailUiState) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = player.webName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${state.teamName} · ${state.position}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(player.status)
                    Spacer(Modifier.width(6.dp))
                    Pill(
                        text = Format.price(player.nowCost),
                        container = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Format.percent(player.selectedByPercent),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                )
                Text(
                    text = "Ownership",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (player.news.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = player.news,
                style = MaterialTheme.typography.bodyMedium,
                color = DifficultyAmber,
            )
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (color, label) = when (status) {
        "a" -> DifficultyGreen to "Available"
        "d" -> DifficultyAmber to "Doubtful"
        "i", "u" -> DifficultyRed to Format.statusLabel(status)
        else -> MaterialTheme.colorScheme.surfaceVariant to Format.statusLabel(status)
    }
    Pill(
        text = label,
        container = color,
        content = if (status in setOf("a", "d", "i", "u")) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveMatchSection(state: PlayerDetailUiState) {
    val fixture = state.liveFixture ?: return
    val player = state.player ?: return
    val isHome = fixture.teamH == player.teamId
    val opponent = if (isHome) fixture.teamA else fixture.teamH
    val opponentShort = state.teamShorts[opponent] ?: "?"
    val ourScore = if (isHome) fixture.teamHScore ?: 0 else fixture.teamAScore ?: 0
    val theirScore = if (isHome) fixture.teamAScore ?: 0 else fixture.teamHScore ?: 0
    val venue = if (isHome) "H" else "A"

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.liveNow) LiveBadge()
                    else Pill("FT", container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Format.matchMinute(fixture.minutes),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${state.teamShort} $ourScore — $theirScore $opponentShort",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "$venue · ${state.currentEventName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (state.liveStats?.totalPoints ?: 0).toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Bold,
                        color = Indigo400,
                    ),
                )
                Text("Live points", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val s = state.liveStats
            StatChip("Goals", (s?.goalsScored ?: 0).toString())
            StatChip("Assists", (s?.assists ?: 0).toString())
            if (state.position == "GKP") StatChip("Saves", (s?.saves ?: 0).toString())
            StatChip("Clean sheet", (s?.cleanSheets ?: 0).toString())
            StatChip("Bonus", (s?.bonus ?: 0).toString())
            StatChip("Yellow", (s?.yellowCards ?: 0).toString())
            StatChip("Red", (s?.redCards ?: 0).toString())
            StatChip("Minutes", (s?.minutes ?: 0).toString())
        }
        Spacer(Modifier.height(8.dp))
        UpdatedLabel(state.lastUpdated, state.stale)
    }
}

@Composable
private fun NextMatchSection(player: Player, state: PlayerDetailUiState) {
    Card {
        SectionTitle("Next gameweek")
        val next = state.nextFixture
        if (next != null) {
            val isHome = next.isHome
            val opponent = if (isHome) next.teamA else next.teamH
            val opponentShort = state.teamShorts[opponent] ?: "?"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${if (isHome) "vs" else "at"} $opponentShort",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${next.eventName} · ${Format.kickoffTime(next.kickoffTime)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DifficultyBadge(next.difficulty)
            }
        } else {
            Text("No upcoming fixture", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Projected", Format.decimal(player.epNext))
            val chance = player.chanceOfPlayingNextRound
            if (chance != null) {
                StatChip("Fit", "$chance%")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonStatsSection(player: Player) {
    SectionTitle("Season")
    Card {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatChip("Total pts", player.totalPoints.toString())
            StatChip("PPG", Format.decimal(player.pointsPerGame))
            StatChip("ICT", Format.decimal(player.ictIndex))
            StatChip("Goals", player.goalsScored.toString())
            StatChip("Assists", player.assists.toString())
            StatChip("Clean sheets", player.cleanSheets.toString())
            StatChip("Bonus", player.bonus.toString())
            StatChip("Minutes", player.minutes.toString())
        }
    }
}

@Composable
private fun UpcomingFixturesSection(state: PlayerDetailUiState) {
    val fixtures = state.summary?.fixtures?.filter { !it.finished }?.take(5) ?: emptyList()
    if (fixtures.isEmpty()) return
    SectionTitle("Upcoming fixtures")
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            fixtures.forEachIndexed { index, f ->
                if (index > 0) {
                    androidx.compose.material3.HorizontalDivider()
                }
                FixtureRow(f, state)
            }
        }
    }
}

@Composable
private fun FixtureRow(f: PlayerFixture, state: PlayerDetailUiState) {
    val isHome = f.isHome
    val opponent = if (isHome) f.teamA else f.teamH
    val opponentShort = state.teamShorts[opponent] ?: "?"
    Row(verticalAlignment = Alignment.CenterVertically) {
        DifficultyBadge(f.difficulty)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "${f.eventName ?: "GW${f.event}"} · ${if (isHome) "vs" else "at"} $opponentShort",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = Format.deadline(f.kickoffTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LastFiveSection(state: PlayerDetailUiState) {
    val history: List<PlayerHistory> = state.summary?.history?.takeLast(5) ?: emptyList()
    if (history.isEmpty()) return
    SectionTitle("Last 5 gameweeks")
    Card {
        val max = (history.maxOfOrNull { it.totalPoints } ?: 0).coerceAtLeast(1)
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            history.forEach { h ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = h.totalPoints.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.height(4.dp))
                    val heightFraction = (h.totalPoints.toFloat() / max.toFloat()).coerceIn(0.08f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((80 * heightFraction).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (h.totalPoints > 0) Indigo400 else MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GW${h.round}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
