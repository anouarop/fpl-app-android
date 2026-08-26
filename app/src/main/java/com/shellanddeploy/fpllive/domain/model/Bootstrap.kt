package com.shellanddeploy.fpllive.domain.model

/** The bootstrap index: everything the app needs to identify players, teams and gameweeks. */
data class Bootstrap(
    val players: List<Player>,
    val teams: List<Team>,
    val positions: List<Position>,
    val gameweeks: List<Gameweek>,
) {
    val teamsById: Map<Int, Team> get() = teams.associateBy { it.id }
    val positionsById: Map<Int, Position> get() = positions.associateBy { it.id }
    val playersById: Map<Int, Player> get() = players.associateBy { it.id }
    val currentGameweek: Gameweek? get() = gameweeks.firstOrNull { it.isCurrent } ?: gameweeks.firstOrNull { it.isNext }
}
