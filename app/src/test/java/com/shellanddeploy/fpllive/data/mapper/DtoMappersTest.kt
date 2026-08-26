package com.shellanddeploy.fpllive.data.mapper

import com.shellanddeploy.fpllive.data.model.BootstrapDto
import com.shellanddeploy.fpllive.data.model.ElementDto
import com.shellanddeploy.fpllive.data.model.EventDto
import com.shellanddeploy.fpllive.data.model.TeamDto
import com.shellanddeploy.fpllive.data.model.ElementTypeDto
import com.shellanddeploy.fpllive.data.model.FixtureDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoMappersTest {

    @Test
    fun `element maps typed fields and parses numeric strings`() {
        val dto = ElementDto(
            id = 19,
            webName = "Saka",
            elementType = 3,
            team = 1,
            nowCost = 85,
            form = "7.5",
            totalPoints = 142,
            pointsPerGame = "6.1",
            selectedByPercent = "38.4",
            epNext = "3.2",
            ictIndex = "120.5",
            chanceOfPlayingNextRound = 100,
        )
        val p = dto.toDomain()
        assertEquals(19, p.id)
        assertEquals(3, p.elementTypeId)
        assertEquals(1, p.teamId)
        assertEquals(85, p.nowCost)
        assertEquals(7.5, p.form, 0.0001)
        assertEquals(142, p.totalPoints)
        assertEquals(6.1, p.pointsPerGame, 0.0001)
        assertEquals(38.4, p.selectedByPercent, 0.0001)
        assertEquals(3.2, p.epNext, 0.0001)
        assertEquals(120.5, p.ictIndex, 0.0001)
        assertEquals(100, p.chanceOfPlayingNextRound)
    }

    @Test
    fun `non-numeric string fields parse to zero`() {
        val dto = ElementDto(id = 1, form = "n/a", pointsPerGame = "", selectedByPercent = "-")
        val p = dto.toDomain()
        assertEquals(0.0, p.form, 0.0001)
        assertEquals(0.0, p.pointsPerGame, 0.0001)
        assertEquals(0.0, p.selectedByPercent, 0.0001)
    }

    @Test
    fun `bootstrap maps all collections`() {
        val dto = BootstrapDto(
            elements = listOf(ElementDto(id = 1)),
            teams = listOf(TeamDto(id = 1, name = "Arsenal", shortName = "ARS", code = 3)),
            elementTypes = listOf(ElementTypeDto(id = 1, singularName = "Goalkeeper", singularNameShort = "GKP", pluralName = "Goalkeepers")),
            events = listOf(EventDto(id = 1, name = "Gameweek 1", isCurrent = true)),
        )
        val b = dto.toDomain()
        assertEquals(1, b.players.size)
        assertEquals(1, b.teams.size)
        assertEquals(1, b.positions.size)
        assertEquals(1, b.gameweeks.size)
        assertEquals("Gameweek 1", b.currentGameweek?.name)
        assertEquals("ARS", b.teamsById[1]?.shortName)
    }

    @Test
    fun `fixture maps nullable scores`() {
        val dto = FixtureDto(id = 10, teamH = 1, teamA = 2, teamHScore = 2, teamAScore = null, started = true)
        val f = dto.toDomain()
        assertEquals(2, f.teamHScore)
        assertEquals(null, f.teamAScore)
        assertTrue(f.started)
    }
}
