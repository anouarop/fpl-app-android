package com.shellanddeploy.fpllive.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.LiveMatch
import com.shellanddeploy.fpllive.domain.model.LiveMatchEvent
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.LiveBadge
import com.shellanddeploy.fpllive.ui.components.Pill
import com.shellanddeploy.fpllive.ui.components.SectionTitle
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.components.UpdatedLabel
import com.shellanddeploy.fpllive.ui.theme.DifficultyAmber
import com.shellanddeploy.fpllive.ui.theme.DifficultyGreen
import com.shellanddeploy.fpllive.ui.theme.DifficultyRed

@Composable
fun LiveScreen(viewModel: LiveViewModel) {
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
        bottomBar = {
            val events = state.feed?.matches.orEmpty()
                .filter { it.live || it.events.isNotEmpty() }
                .flatMap { it.events }
                .distinctBy { "${it.type}-${it.playerName}-${it.minute}" }
                .sortedByDescending { it.minute }
            if (events.isNotEmpty()) {
                LiveEventTicker(events = events)
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Live", Modifier.weight(1f))
                if (state.feed?.matches?.any { it.live } == true) {
                    LiveBadge()
                }
            }

            ErrorBanner(state.error)

            val matches = state.feed?.matches.orEmpty()
            when {
                !state.configured -> CenteredMessage(
                    "Live unavailable",
                    subtitle = "The live-events service isn't configured.",
                )
                state.loading && matches.isEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(5) { SkeletonBox(Modifier.fillMaxWidth().height(96.dp)) }
                }
                matches.isEmpty() -> CenteredMessage(
                    "No fixtures",
                    subtitle = "No matches for this gameweek.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(matches, key = { it.id }) { match ->
                        LiveMatchCard(match)
                    }
                    item {
                        UpdatedLabel(state.lastUpdated, stale = false, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveMatchCard(match: LiveMatch) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamSide(match.homeShort, Alignment.Start, Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${match.homeScore} - ${match.awayScore}",
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                )
                if (match.live) {
                    LiveBadge()
                } else {
                    Text(
                        text = match.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TeamSide(match.awayShort, Alignment.End, Modifier.weight(1f))
        }

        if (match.events.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            match.events.forEach { event ->
                LiveEventRow(event)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun TeamSide(short: String, alignment: Alignment.Horizontal, modifier: Modifier) {
    Column(horizontalAlignment = alignment, modifier = modifier) {
        Text(short, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LiveEventRow(event: LiveMatchEvent) {
    val (label, color) = eventLabel(event.type)
    val playerPoints = formatPoints(event.playerPoints)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(label, color)
            Spacer(Modifier.width(8.dp))
            Text("${event.minute}'", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${event.playerName}${if (event.detail == "penalty") " (pen)" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = playerPoints,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (event.playerPoints > 0) DifficultyGreen else DifficultyRed,
            )
        }
        event.assistName?.let { assistName ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "assist $assistName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                event.assistPoints?.let { ap ->
                    Text(
                        text = formatPoints(ap),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DifficultyGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun eventLabel(type: String): Pair<String, Color> = when (type) {
    "goal" -> "Goal" to DifficultyGreen
    "ownGoal" -> "Own goal" to DifficultyRed
    "yellow" -> "Yellow" to DifficultyAmber
    "secondYellow" -> "2nd yellow" to DifficultyRed
    "red" -> "Red" to DifficultyRed
    "cleanSheet" -> "Clean sheet" to DifficultyGreen
    else -> type to MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatPoints(p: Int): String = if (p > 0) "+$p" else "$p"

@Composable
private fun LiveEventTicker(events: List<LiveMatchEvent>) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = "LIVE EVENTS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            events.forEach { event ->
                LiveEventChip(event)
            }
        }
    }
}

@Composable
private fun LiveEventChip(event: LiveMatchEvent) {
    val (label, color) = eventLabel(event.type)
    val points = formatPoints(event.playerPoints)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Pill(label, color)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${event.playerName}${if (event.detail == "penalty") " (pen)" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = points,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (event.playerPoints > 0) DifficultyGreen else DifficultyRed,
        )
    }
}
