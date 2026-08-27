package com.shellanddeploy.fpllive.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shellanddeploy.fpllive.data.namesearch.ManagerMatch
import com.shellanddeploy.fpllive.ui.components.Card
import com.shellanddeploy.fpllive.ui.components.SectionTitle
import com.shellanddeploy.fpllive.ui.theme.DifficultyAmber
import com.shellanddeploy.fpllive.ui.theme.DifficultyGreen
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cleared by viewModel.cleared.collectAsStateWithLifecycle()
    val nameSearchState by viewModel.nameSearchState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Notification permission denied") }
        }
    }

    val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(cleared) {
        if (cleared) {
            snackbarHostState.showSnackbar("Cache cleared")
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            SectionTitle("Appearance")
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text("Dark theme", style = MaterialTheme.typography.titleMedium)
                        Text("Deep slate, easy on the eyes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.darkTheme,
                        onCheckedChange = viewModel::setDarkTheme,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            SectionTitle("Live updates")
            Card {
                Text("Poll interval", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20, 30, 60).forEach { seconds ->
                        val selected = state.pollIntervalSeconds == seconds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .selectable(
                                    selected = selected,
                                    onClick = { viewModel.setPollInterval(seconds) },
                                ),
                        ) {
                            RadioButton(selected = selected, onClick = { viewModel.setPollInterval(seconds) })
                            Text("${seconds}s", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Text(
                    "FPL refreshes live stats roughly every 20–60s; polling faster is polite but no more accurate.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionTitle("Notifications")
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text("Gameweek reminders", style = MaterialTheme.typography.titleMedium)
                        Text("Notify before deadlines (local)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setNotificationsEnabled(enabled)
                            }
                        },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "A background worker checks upcoming deadlines and posts a local reminder within 24 hours of each gameweek.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionTitle("Default team")
            Card {
                var text by remember(state.defaultTeamId) { mutableStateOf(state.defaultTeamId.toString()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Team ID") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { text.toIntOrNull()?.let { viewModel.setDefaultTeamId(it); scope.launch { snackbarHostState.showSnackbar("Default team saved") } } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }

                if (nameSearchState.available) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "…or search by manager or team name",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameSearchState.query,
                        onValueChange = viewModel::setNameQuery,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Manager or team name") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )

                    if (nameSearchState.loading) {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator()
                    }

                    nameSearchState.error?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(message, style = MaterialTheme.typography.bodySmall, color = DifficultyAmber)
                    }

                    if (nameSearchState.results.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        nameSearchState.results.forEach { match ->
                            ManagerResultRow(match) {
                                viewModel.selectMatch(match)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Switched to ${match.teamName.ifBlank { match.managerName }}")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            SectionTitle("Storage")
            Card {
                OutlinedButton(onClick = viewModel::clearCache) {
                    Text("Clear cache")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Log out (forget my team)")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Removes your linked team and returns to onboarding. Your team isn't deleted on FPL.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ManagerResultRow(match: ManagerMatch, onClick: () -> Unit) {
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
