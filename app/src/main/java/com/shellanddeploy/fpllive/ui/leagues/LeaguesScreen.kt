package com.shellanddeploy.fpllive.ui.leagues

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.LeagueRow
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.SectionTitle
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.components.UpdatedLabel
import com.shellanddeploy.fpllive.ui.theme.DifficultyAmber
import com.shellanddeploy.fpllive.ui.theme.Indigo400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguesScreen(
    viewModel: LeaguesViewModel,
    onBack: () -> Unit,
    onEntryClick: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leagues") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = DifficultyAmber)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Enter a classic league ID to view its standings. Listing your own (private) leagues requires an authenticated session, which the public API does not expose.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { LeagueIdInput(onSubmit = viewModel::loadLeague) }

            item { ErrorBanner(state.error) }

            if (state.loading) {
                items(6) { SkeletonBox(Modifier.fillMaxWidth().height(56.dp)) }
                return@LazyColumn
            }

            val standings = state.standings
            when {
                standings == null -> item { CenteredMessage("No league loaded", subtitle = "Enter a league ID above.") }
                standings.league.isPrivate && standings.rows.isEmpty() ->
                    item { CenteredMessage("Private league", subtitle = "This league requires authentication to view.") }
                standings.rows.isEmpty() ->
                    item { CenteredMessage("No standings", subtitle = "This league has no standings yet.") }
                else -> {
                    item { SectionTitle(standings.league.name) }
                    items(standings.rows, key = { it.entry }) { row ->
                        LeagueRowCard(row, onClick = { onEntryClick(row.entry) })
                    }
                    item { UpdatedLabel(state.lastUpdated, state.stale) }
                }
            }
        }
    }
}

@Composable
private fun LeagueIdInput(onSubmit: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                modifier = Modifier.weight(1f),
                label = { Text("League ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { text.toIntOrNull()?.let(onSubmit) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("View")
            }
        }
    }
}

@Composable
private fun LeagueRowCard(row: LeagueRow, onClick: () -> Unit) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        ) {
            Text(
                row.rank.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold, color = Indigo400),
                modifier = Modifier.width(40.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    row.entryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    row.playerName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                row.total.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
            )
        }
    }
}
