package com.shellanddeploy.fpllive.testutil

import com.shellanddeploy.fpllive.data.api.FplApi
import com.shellanddeploy.fpllive.data.model.BootstrapDto
import com.shellanddeploy.fpllive.data.model.ElementDto
import com.shellanddeploy.fpllive.data.model.ElementSummaryDto
import com.shellanddeploy.fpllive.data.model.ElementTypeDto
import com.shellanddeploy.fpllive.data.model.EntryDto
import com.shellanddeploy.fpllive.data.model.EventDto
import com.shellanddeploy.fpllive.data.model.FixtureDto
import com.shellanddeploy.fpllive.data.model.HistoryDto
import com.shellanddeploy.fpllive.data.model.LiveEventDto
import com.shellanddeploy.fpllive.data.model.LeagueStandingsDto
import com.shellanddeploy.fpllive.data.model.PicksDto
import com.shellanddeploy.fpllive.data.model.TeamDto
import com.shellanddeploy.fpllive.data.model.TransferDto
import java.io.IOException

class FakeFplApi : FplApi {

    var bootstrapCalls = 0
    var failBootstrap = false

    override suspend fun bootstrap(): BootstrapDto {
        bootstrapCalls++
        if (failBootstrap) throw IOException("network down")
        return sampleBootstrap()
    }

    override suspend fun eventLive(id: Int): LiveEventDto = LiveEventDto()

    override suspend fun fixtures(event: Int?): List<FixtureDto> = emptyList()

    override suspend fun entry(id: Int): EntryDto = EntryDto(id = id, name = "Sample FC")

    override suspend fun picks(id: Int, event: Int): PicksDto = PicksDto()

    override suspend fun history(id: Int): HistoryDto = HistoryDto()

    override suspend fun elementSummary(id: Int): ElementSummaryDto = ElementSummaryDto()

    override suspend fun transfers(id: Int): List<TransferDto> = emptyList()

    override suspend fun leaguesClassicStandings(id: Int): LeagueStandingsDto = LeagueStandingsDto()

    companion object {
        fun sampleBootstrap() = BootstrapDto(
            elements = listOf(ElementDto(id = 1, webName = "Saka", elementType = 3, team = 1, totalPoints = 100)),
            teams = listOf(TeamDto(id = 1, name = "Arsenal", shortName = "ARS", code = 3)),
            elementTypes = listOf(ElementTypeDto(id = 3, singularName = "Midfielder", singularNameShort = "MID", pluralName = "Midfielders")),
            events = listOf(EventDto(id = 1, name = "Gameweek 1", isCurrent = true)),
        )
    }
}
