package com.shellanddeploy.fpllive.ui.fixtures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.domain.model.Gameweek
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.FixtureCard
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.components.UpdatedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixturesScreen(viewModel: FixturesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            EventSelector(
                events = state.events,
                selectedId = state.selectedEventId,
                onSelect = viewModel::selectEvent,
            )
            ErrorBanner(state.error, Modifier.padding(horizontal = 16.dp))
            if (state.loading) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(8) { SkeletonBox(Modifier.fillMaxWidth().height(64.dp)) }
                }
            } else if (state.fixtures.isEmpty()) {
                CenteredMessage("No fixtures", subtitle = "No matches for this gameweek.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.fixtures, key = { it.id }) { fixture ->
                        FixtureCard(
                            fixture = fixture,
                            homeShort = state.teamShorts[fixture.teamH] ?: "?",
                            awayShort = state.teamShorts[fixture.teamA] ?: "?",
                        )
                    }
                    item {
                        UpdatedLabel(state.lastUpdated, state.stale, Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EventSelector(events: List<Gameweek>, selectedId: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = events.firstOrNull { it.id == selectedId }

    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected?.name ?: "Gameweek")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            events.forEach { e ->
                DropdownMenuItem(
                    text = {
                        Text("${e.name}${if (e.isCurrent) " · current" else if (e.isNext) " · next" else ""}")
                    },
                    onClick = {
                        expanded = false
                        onSelect(e.id)
                    },
                )
            }
        }
    }
}
