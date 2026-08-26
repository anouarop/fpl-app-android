package com.shellanddeploy.fpllive.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.data.namesearch.ManagerMatch
import com.shellanddeploy.fpllive.domain.model.Entry
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.theme.DifficultyAmber
import com.shellanddeploy.fpllive.ui.theme.DifficultyGreen

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Find your team", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your team ID (or your name) to see your live points, rank, squad and history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            ModeSelector(state.mode, viewModel::setMode)
            Spacer(Modifier.height(16.dp))

            when (state.mode) {
                OnboardingMode.TeamId -> TeamIdSection(state, viewModel)
                OnboardingMode.Name -> NameSection(state, viewModel)
            }

            state.preview?.let { entry ->
                Spacer(Modifier.height(16.dp))
                PreviewCard(entry, onConfirm = viewModel::confirm)
            }

            state.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = DifficultyAmber, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "You can change your team at any time in Settings.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ModeSelector(selected: OnboardingMode, onSelect: (OnboardingMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeButton("Team ID", selected == OnboardingMode.TeamId) { onSelect(OnboardingMode.TeamId) }
        ModeButton("Name", selected == OnboardingMode.Name) { onSelect(OnboardingMode.Name) }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun TeamIdSection(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    OutlinedTextField(
        value = state.teamIdText,
        onValueChange = viewModel::setTeamId,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Team ID") },
        placeholder = { Text("e.g. 9166708 or your team URL") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
    )
    if (state.preview == null) {
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = viewModel::lookup,
            enabled = !state.loading && state.teamIdText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.height(20.dp))
            else Text("Look up my team")
        }
    }
}

@Composable
private fun NameSection(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    if (!state.nameSearchAvailable) {
        Text(
            "Name search needs the companion service. Enter your team ID instead — it works instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = DifficultyAmber,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }

    OutlinedTextField(
        value = state.nameQuery,
        onValueChange = viewModel::setNameQuery,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Manager or team name") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        enabled = state.nameSearchAvailable,
    )

    if (state.loading) {
        Spacer(Modifier.height(12.dp))
        CircularProgressIndicator()
    }

    if (state.searchResults.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        state.searchResults.forEach { match ->
            MatchRow(match, onClick = { viewModel.selectMatch(match) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MatchRow(match: ManagerMatch, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Column {
            Text(match.managerName, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (match.teamName.isNotBlank()) {
                    Text(match.teamName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                match.rank?.let { rank ->
                    if (rank > 0) {
                        Text("#$rank", style = MaterialTheme.typography.labelMedium, color = DifficultyGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(entry: Entry, onConfirm: () -> Unit) {
    Card {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("${entry.playerFirstName} ${entry.playerLastName}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(entry.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                "Overall: ${entry.summaryOverallPoints} pts · Rank ${entry.summaryOverallRank}",
                style = MaterialTheme.typography.labelMedium,
                color = DifficultyGreen,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text("That's my team — continue")
            }
        }
    }
}
