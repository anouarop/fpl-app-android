package com.shellanddeploy.fpllive.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.util.Format

/** A squad player owned by the manager, shown under their club in a fixture. */
data class OwnedPlayer(
    val name: String,
    val isCaptain: Boolean = false,
    val isViceCaptain: Boolean = false,
)

@Composable
fun FixtureCard(
    fixture: Fixture,
    homeShort: String,
    awayShort: String,
    homePlayers: List<OwnedPlayer> = emptyList(),
    awayPlayers: List<OwnedPlayer> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val inProgress = fixture.started && !fixture.finished
    val liveNow = inProgress && !fixture.finishedProvisional

    Card(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                DifficultyBadge(fixture.teamHDifficulty)
                Spacer(Modifier.height(4.dp))
                Text(homeShort, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OwnedPlayers(homePlayers)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (fixture.started) {
                    Text(
                        text = "${fixture.teamHScore ?: 0} - ${fixture.teamAScore ?: 0}",
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                    )
                    if (liveNow) {
                        LiveBadge()
                    } else {
                        Pill("FT", container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(
                        text = Format.kickoffTime(fixture.kickoffTime),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    )
                    Text(
                        text = Format.deadline(fixture.kickoffTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                DifficultyBadge(fixture.teamADifficulty)
                Spacer(Modifier.height(4.dp))
                Text(awayShort, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OwnedPlayers(awayPlayers)
            }
        }
    }
}

@Composable
private fun OwnedPlayers(players: List<OwnedPlayer>) {
    if (players.isEmpty()) return
    Spacer(Modifier.height(6.dp))
    players.forEach { player ->
        Text(
            text = when {
                player.isCaptain -> "C ${player.name}"
                player.isViceCaptain -> "VC ${player.name}"
                else -> player.name
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (player.isCaptain || player.isViceCaptain) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
