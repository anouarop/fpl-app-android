package com.shellanddeploy.fpllive.util

import com.shellanddeploy.fpllive.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerListLogicTest {

    private fun player(id: Int, name: String, positionId: Int, points: Int, cost: Int, form: Double, selected: Double) =
        Player(
            id = id, code = 0, webName = name, firstName = "", secondName = "",
            elementTypeId = positionId, teamId = 1, nowCost = cost, form = form,
            totalPoints = points, pointsPerGame = 0.0, selectedByPercent = selected,
            status = "a", epNext = 0.0, epThis = 0.0, chanceOfPlayingNextRound = null,
            goalsScored = 0, assists = 0, cleanSheets = 0, bonus = 0, minutes = 0,
            saves = 0, yellowCards = 0, redCards = 0, ictIndex = 0.0, news = "",
        )

    private val players = listOf(
        player(1, "Saka", 3, 120, 90, 7.0, 40.0),
        player(2, "Haaland", 4, 200, 150, 9.0, 80.0),
        player(3, "Salah", 3, 180, 130, 8.0, 60.0),
        player(4, "Pickford", 1, 60, 50, 4.0, 10.0),
    )

    @Test
    fun `sorts by total points by default`() {
        val result = PlayerListLogic.apply(players, "", null, PlayerListLogic.Sort.POINTS)
        assertEquals(listOf(2, 3, 1, 4), result.map { it.id })
    }

    @Test
    fun `filters by position`() {
        val result = PlayerListLogic.apply(players, "", 3, PlayerListLogic.Sort.POINTS)
        assertEquals(listOf(3, 1), result.map { it.id })
    }

    @Test
    fun `filters by name query case-insensitive`() {
        val result = PlayerListLogic.apply(players, "SA", null, PlayerListLogic.Sort.POINTS)
        assertEquals(listOf(3, 1), result.map { it.id })
    }

    @Test
    fun `sorts by price`() {
        val result = PlayerListLogic.apply(players, "", null, PlayerListLogic.Sort.PRICE)
        assertEquals(listOf(2, 3, 1, 4), result.map { it.id })
    }

    @Test
    fun `sorts by selected percent`() {
        val result = PlayerListLogic.apply(players, "", null, PlayerListLogic.Sort.SELECTED)
        assertEquals(2, result.first().id)
    }

    @Test
    fun `no matches returns empty`() {
        assertEquals(emptyList<Player>(), PlayerListLogic.apply(players, "zzz", null, PlayerListLogic.Sort.POINTS))
    }
}
