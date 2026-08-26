package com.shellanddeploy.fpllive.data.mapper

import com.shellanddeploy.fpllive.data.model.LeagueInfoDto
import com.shellanddeploy.fpllive.data.model.LeaguePageDto
import com.shellanddeploy.fpllive.data.model.LeagueRowDto
import com.shellanddeploy.fpllive.data.model.LeagueStandingsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeagueMapperTest {

    @Test
    fun `public league maps with rows and isPrivate false`() {
        val dto = LeagueStandingsDto(
            league = LeagueInfoDto(id = 313, name = "Overall", leagueType = "x", codePrivacy = "v"),
            standings = LeaguePageDto(
                results = listOf(
                    LeagueRowDto(rank = 1, entry = 9166708, entryName = "My Team", playerName = "John Doe", total = 200),
                ),
            ),
        )
        val domain = dto.toDomain()
        assertEquals("Overall", domain.league.name)
        assertFalse(domain.league.isPrivate)
        assertEquals(1, domain.rows.size)
        assertEquals("My Team", domain.rows.first().entryName)
        assertEquals(200, domain.rows.first().total)
    }

    @Test
    fun `private league is flagged private`() {
        val dto = LeagueStandingsDto(
            league = LeagueInfoDto(id = 1, name = "Friends", leagueType = "s", codePrivacy = "p"),
            standings = LeaguePageDto(results = emptyList()),
        )
        val domain = dto.toDomain()
        assertTrue(domain.league.isPrivate)
        assertTrue(domain.rows.isEmpty())
    }
}
