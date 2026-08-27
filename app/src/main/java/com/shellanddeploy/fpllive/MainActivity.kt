package com.shellanddeploy.fpllive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shellanddeploy.fpllive.data.datastore.Settings
import com.shellanddeploy.fpllive.di.fplApp
import com.shellanddeploy.fpllive.di.fplViewModel
import com.shellanddeploy.fpllive.di.fplViewModelWithArgs
import com.shellanddeploy.fpllive.ui.fixtures.FixturesScreen
import com.shellanddeploy.fpllive.ui.fixtures.FixturesViewModel
import com.shellanddeploy.fpllive.ui.gameweeks.GameweekFixturesScreen
import com.shellanddeploy.fpllive.ui.gameweeks.GameweekFixturesViewModel
import com.shellanddeploy.fpllive.ui.gameweeks.GameweeksScreen
import com.shellanddeploy.fpllive.ui.gameweeks.GameweeksViewModel
import com.shellanddeploy.fpllive.ui.history.HistoryScreen
import com.shellanddeploy.fpllive.ui.history.HistoryViewModel
import com.shellanddeploy.fpllive.ui.home.HomeScreen
import com.shellanddeploy.fpllive.ui.home.HomeViewModel
import com.shellanddeploy.fpllive.ui.leagues.LeaguesScreen
import com.shellanddeploy.fpllive.ui.leagues.LeaguesViewModel
import com.shellanddeploy.fpllive.ui.live.LiveScreen
import com.shellanddeploy.fpllive.ui.live.LiveViewModel
import com.shellanddeploy.fpllive.ui.onboarding.OnboardingScreen
import com.shellanddeploy.fpllive.ui.onboarding.OnboardingViewModel
import com.shellanddeploy.fpllive.ui.playerdetail.PlayerDetailScreen
import com.shellanddeploy.fpllive.ui.playerdetail.PlayerDetailViewModel
import com.shellanddeploy.fpllive.ui.players.PlayersScreen
import com.shellanddeploy.fpllive.ui.players.PlayersViewModel
import com.shellanddeploy.fpllive.ui.settings.SettingsScreen
import com.shellanddeploy.fpllive.ui.settings.SettingsViewModel
import com.shellanddeploy.fpllive.ui.standings.StandingsScreen
import com.shellanddeploy.fpllive.ui.standings.StandingsViewModel
import com.shellanddeploy.fpllive.ui.team.TeamScreen
import com.shellanddeploy.fpllive.ui.team.TeamViewModel
import com.shellanddeploy.fpllive.ui.theme.FplLiveTheme
import com.shellanddeploy.fpllive.ui.transfers.TransfersScreen
import com.shellanddeploy.fpllive.ui.transfers.TransfersViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = fplApp()
            val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
            FplLiveTheme(darkTheme = settings.darkTheme) {
                AppRoot()
            }
        }
    }
}

private sealed class Tab(
    val route: String,
    val launchRoute: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : Tab("home", "home", "Home", Icons.Filled.Home)
    data object Live : Tab("live", "live", "Live", Icons.Filled.Bolt)
    data object Players : Tab("players", "players", "Players", Icons.Filled.People)
    data object Fixtures : Tab("fixtures", "fixtures", "Fixtures", Icons.Filled.CalendarMonth)
    data object Team : Tab("team/{teamId}", "team/-1", "Team", Icons.Filled.Groups)
    data object Settings : Tab("settings", "settings", "Settings", Icons.Filled.Settings)
}

private val tabs = listOf(Tab.Home, Tab.Live, Tab.Players, Tab.Fixtures, Tab.Team, Tab.Settings)

@Composable
private fun AppRoot() {
    val app = fplApp()
    val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
    if (!settings.onboardingComplete) {
        val vm: OnboardingViewModel = fplViewModel { OnboardingViewModel(it.repository, it.settings, it.nameSearch) }
        OnboardingScreen(viewModel = vm)
    } else {
        MainScaffold()
    }
}

@Composable
private fun MainScaffold() {
    val app = fplApp()
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            val isTopLevel = tabs.any { it.route == currentRoute }
            if (isTopLevel) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.launchRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                val vm: HomeViewModel = fplViewModel { HomeViewModel(it.repository, it.settings) }
                HomeScreen(
                    viewModel = vm,
                    onOpenGameweeks = { navController.navigate("gameweeks") },
                    onOpenStandings = { navController.navigate("standings") },
                    onOpenTransfers = { navController.navigate("transfers") },
                    onOpenHistory = { navController.navigate("history") },
                    onOpenLeagues = { navController.navigate("leagues") },
                    onOpenTeam = { id -> navController.navigate("team/$id") },
                    onOpenPlayer = { id -> navController.navigate("player/$id") },
                )
            }

            composable("players") {
                val vm: PlayersViewModel = fplViewModel { PlayersViewModel(it.repository) }
                PlayersScreen(viewModel = vm, onPlayerClick = { id -> navController.navigate("player/$id") })
            }

            composable("live") {
                val vm: LiveViewModel = fplViewModel { LiveViewModel(it.liveRepository, it.settings) }
                LiveScreen(viewModel = vm)
            }

            composable("fixtures") {
                val vm: FixturesViewModel = fplViewModel { FixturesViewModel(it.repository, it.settings.settings) }
                FixturesScreen(viewModel = vm)
            }

            composable(
                route = "team/{teamId}",
                arguments = listOf(navArgument("teamId") { type = NavType.IntType; defaultValue = -1 }),
            ) {
                val vm: TeamViewModel = fplViewModelWithArgs { a, handle -> TeamViewModel(a.repository, a.settings, handle) }
                TeamScreen(viewModel = vm, onPlayerClick = { id -> navController.navigate("player/$id") })
            }

            composable("settings") {
                val vm: SettingsViewModel = fplViewModel { SettingsViewModel(it.repository, it.settings, it.reminderScheduler, it.nameSearch) }
                SettingsScreen(viewModel = vm)
            }

            composable(
                route = "player/{playerId}",
                arguments = listOf(navArgument("playerId") { type = NavType.IntType }),
            ) {
                val vm: PlayerDetailViewModel = fplViewModelWithArgs { a, handle -> PlayerDetailViewModel(a.repository, a.settings, handle) }
                PlayerDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable("gameweeks") {
                val vm: GameweeksViewModel = fplViewModel { GameweeksViewModel(it.repository) }
                GameweeksScreen(
                    viewModel = vm,
                    onGameweekClick = { id -> navController.navigate("gameweek/$id") },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "gameweek/{gameweekId}",
                arguments = listOf(navArgument("gameweekId") { type = NavType.IntType }),
            ) {
                val vm: GameweekFixturesViewModel = fplViewModelWithArgs { a, handle -> GameweekFixturesViewModel(a.repository, handle) }
                GameweekFixturesScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable("standings") {
                val vm: StandingsViewModel = fplViewModel { StandingsViewModel(it.repository, it.settings) }
                StandingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable("transfers") {
                val vm: TransfersViewModel = fplViewModel { TransfersViewModel(it.repository, it.settings) }
                TransfersScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable("history") {
                val vm: HistoryViewModel = fplViewModel { HistoryViewModel(it.repository, it.settings) }
                HistoryScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable("leagues") {
                val vm: LeaguesViewModel = fplViewModel { LeaguesViewModel(it.repository) }
                LeaguesScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onEntryClick = { id -> navController.navigate("team/$id") },
                )
            }
        }
    }
}
