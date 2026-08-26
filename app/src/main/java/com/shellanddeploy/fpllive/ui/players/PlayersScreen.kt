package com.shellanddeploy.fpllive.ui.players

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.Pill
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.theme.Indigo400
import com.shellanddeploy.fpllive.util.Format
import com.shellanddeploy.fpllive.util.PlayerListLogic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    viewModel: PlayersViewModel,
    onPlayerClick: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PlayerSearchField(query = state.query, onQueryChange = viewModel::setQuery)
            FilterRow(state, onPositionSelect = viewModel::setPosition, onSortSelect = viewModel::setSort)

            if (state.loading && state.bootstrap == null) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(8) { SkeletonBox(Modifier.fillMaxWidth().height(64.dp)) }
                }
            } else if (state.players.isEmpty()) {
                CenteredMessage("No players", subtitle = "Try clearing filters.")
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.players, key = { it.id }) { player ->
                        PlayerRow(
                            player = player,
                            teamName = state.teamNames[player.teamId] ?: "?",
                            position = state.positions.firstOrNull { it.id == player.elementTypeId }?.singularNameShort ?: "?",
                            onClick = { onPlayerClick(player.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Filter by name…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun FilterRow(
    state: PlayersUiState,
    onPositionSelect: (Int?) -> Unit,
    onSortSelect: (PlayerListLogic.Sort) -> Unit,
) {
    var sortExpanded by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.positionId == null,
                onClick = { onPositionSelect(null) },
                label = { Text("All") },
            )
            state.positions.forEach { pos ->
                FilterChip(
                    selected = state.positionId == pos.id,
                    onClick = { onPositionSelect(pos.id) },
                    label = { Text(pos.singularNameShort) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(onClick = { sortExpanded = true }) {
                Text("Sort: ${state.sort.label()}")
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                PlayerListLogic.Sort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.label()) },
                        onClick = {
                            sortExpanded = false
                            onSortSelect(sort)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun PlayerListLogic.Sort.label(): String = when (this) {
    PlayerListLogic.Sort.POINTS -> "Total points"
    PlayerListLogic.Sort.PRICE -> "Price"
    PlayerListLogic.Sort.FORM -> "Form"
    PlayerListLogic.Sort.SELECTED -> "Selected by"
}

@Composable
private fun PlayerRow(
    player: Player,
    teamName: String,
    position: String,
    onClick: () -> Unit,
) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    player.webName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(teamName, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Pill(position, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        Format.price(player.nowCost),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    player.totalPoints.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold, color = Indigo400),
                )
                Text(
                    "Form ${Format.decimal(player.form)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
