package com.shellanddeploy.fpllive.data.db

import com.shellanddeploy.fpllive.domain.model.GameweekScore
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.domain.model.Team
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {

    private val player = Player(
        id = 5,
        code = 100,
        webName = "Haaland",
        firstName = "Erling",
        secondName = "Haaland",
        elementTypeId = 4,
        teamId = 12,
        nowCost = 150,
        form = 8.9,
        totalPoints = 200,
        pointsPerGame = 9.1,
        selectedByPercent = 72.5,
        status = "a",
        epNext = 4.1,
        epThis = 4.1,
        chanceOfPlayingNextRound = null,
        goalsScored = 15,
        assists = 5,
        cleanSheets = 0,
        bonus = 20,
        minutes = 900,
        saves = 0,
        yellowCards = 1,
        redCards = 0,
        ictIndex = 150.0,
        news = "",
    )

    @Test
    fun `player round-trips through entity`() {
        val entity = player.toEntity()
        val restored = entity.toDomain()
        assertEquals(player, restored)
    }

    @Test
    fun `team round-trips through entity`() {
        val team = Team(id = 1, name = "Arsenal", shortName = "ARS", code = 3)
        assertEquals(team, team.toEntity().toDomain())
    }

    @Test
    fun `gameweek score round-trips preserving chip`() {
        val score = GameweekScore(
            event = 5,
            points = 72,
            totalPoints = 400,
            rank = 500_000,
            overallRank = 1_000_000,
            eventTransfers = 2,
            eventTransfersCost = 4,
            pointsOnBench = 9,
            chip = "wildcard",
        )
        assertEquals(score, score.toEntity(entryId = 7).toDomain())
    }
}
