package com.shellanddeploy.fpllive.ui.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.Pick
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.DifficultyBadge
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.LiveBadge
import com.shellanddeploy.fpllive.ui.components.Pill
import com.shellanddeploy.fpllive.ui.components.SectionTitle
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.components.UpdatedLabel
import com.shellanddeploy.fpllive.ui.theme.DifficultyAmber
import com.shellanddeploy.fpllive.ui.theme.Indigo400
import com.shellanddeploy.fpllive.util.Format

@Composable
fun TeamScreen(
    viewModel: TeamViewModel,
    onPlayerClick: (Int) -> Unit,
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

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            TeamIdInput(
                currentId = state.teamId,
                onSubmit = viewModel::loadTeam,
            )
            Spacer(Modifier.height(12.dp))

            ErrorBanner(state.error)

            if (state.loading) {
                SkeletonBox(Modifier.fillMaxWidth().height(100.dp))
                Spacer(Modifier.height(8.dp))
                SkeletonBox(Modifier.fillMaxWidth().height(200.dp))
                return@Scaffold
            }

            val entry = state.entry
            if (entry == null) {
                CenteredMessage("No team found", subtitle = "Check the team ID and try again.")
                return@Scaffold
            }

            TeamHeader(state)
            Spacer(Modifier.height(12.dp))

            if (state.picks.isEmpty()) {
                CenteredMessage("No squad yet", subtitle = "This team has no picks for ${state.currentEventName}.")
            } else {
                SectionTitle("Starting XI${if (state.liveNow) "" else ""}")
                if (state.liveNow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveBadge()
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Live total: ${state.liveTotal}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold, color = Indigo400),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.xi.forEach { pick ->
                            SquadRow(pick, state, onPlayerClick)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                SectionTitle("Bench")
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.bench.forEach { pick ->
                            SquadRow(pick, state, onPlayerClick)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (state.history.isNotEmpty()) {
                SectionTitle("Recent gameweeks")
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.history.reversed().forEachIndexed { i, h ->
                            if (i > 0) HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "GW${h.event}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${h.points} pts",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Rank ${Format.ordinal(h.rank)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            UpdatedLabel(state.lastUpdated, state.stale)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TeamIdInput(currentId: Int, onSubmit: (Int) -> Unit) {
    var text by remember(currentId) { mutableStateOf(currentId.toString()) }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                modifier = Modifier.weight(1f),
                label = { Text("FPL team ID") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { text.toIntOrNull()?.let(onSubmit) },
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("View")
            }
        }
    }
}

@Composable
private fun TeamHeader(state: TeamUiState) {
    val entry = state.entry ?: return
    Card {
        Text(
            text = entry.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Manager: ${entry.playerFirstName} ${entry.playerLastName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderStat("Overall rank", Format.ordinal(entry.summaryOverallRank), Modifier.weight(1f))
            HeaderStat("Total points", entry.summaryOverallPoints.toString(), Modifier.weight(1f))
            HeaderStat(
                "Bank",
                Format.teamValue(state.entryHistory?.bank ?: entry.lastDeadlineBank),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeaderStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold, color = Indigo400),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SquadRow(pick: Pick, state: TeamUiState, onPlayerClick: (Int) -> Unit) {
    val player: Player? = state.players[pick.element]
    val name = player?.webName ?: "Player ${pick.element}"
    val team = state.teamShorts[player?.teamId ?: 0] ?: ""
    val position = state.positionNames[pick.elementType ?: player?.elementTypeId ?: 0] ?: ""
    val livePoints = state.livePoints[pick.element] ?: 0
    val nextFixture = state.nextFixturesByTeam[player?.teamId ?: 0]
    val nextOpponent = nextFixture?.let {
        state.teamShorts[if (it.teamH == player?.teamId) it.teamA else it.teamH]
    } ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "${pick.position}.",
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(26.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onPlayerClick(pick.element) },
            )
            Text(
                text = "$team · $position · ${Format.priceShort(player?.nowCost)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (pick.isCaptain) {
            Pill("C", container = DifficultyAmber)
            Spacer(Modifier.width(4.dp))
        }
        if (pick.isViceCaptain) {
            Pill("VC", container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
        }
        if (nextOpponent.isNotEmpty()) {
            Text(
                text = "vs $nextOpponent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Text(
            text = if (state.liveNow) livePoints.toString() else (livePoints.takeIf { livePoints > 0 }?.toString() ?: "—"),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
            color = if (state.liveNow && livePoints > 0) Indigo400 else MaterialTheme.colorScheme.onSurface,
        )
    }
}
