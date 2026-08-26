package com.shellanddeploy.fpllive.ui.gameweeks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.ui.components.CenteredMessage
import com.shellanddeploy.fpllive.ui.components.ErrorBanner
import com.shellanddeploy.fpllive.ui.components.FixtureCard
import com.shellanddeploy.fpllive.ui.components.SkeletonBox
import com.shellanddeploy.fpllive.ui.components.UpdatedLabel
import com.shellanddeploy.fpllive.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameweekFixturesScreen(
    viewModel: GameweekFixturesViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.gameweekName)
                        if (state.deadlineTime.isNotBlank()) {
                            Text(
                                "Deadline ${Format.deadline(state.deadlineTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ErrorBanner(state.error, Modifier.padding(horizontal = 16.dp))
            if (state.error != null && state.fixtures.isEmpty()) {
                TextButton(onClick = viewModel::retry, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Retry")
                }
            }
            if (state.loading && state.fixtures.isEmpty()) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(8) { SkeletonBox(Modifier.fillMaxWidth().height(64.dp)) }
                }
            } else if (state.fixtures.isEmpty()) {
                CenteredMessage("No fixtures", subtitle = "No matches scheduled for this gameweek.")
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
