package com.shellanddeploy.fpllive.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingViewModelTest {

    @Test
    fun `extracts bare numeric id`() {
        assertEquals(9166708, OnboardingViewModel.extractTeamId("9166708"))
    }

    @Test
    fun `extracts id from full team url`() {
        assertEquals(
            9166708,
            OnboardingViewModel.extractTeamId("https://fantasy.premierleague.com/entry/9166708/event/1"),
        )
    }

    @Test
    fun `extracts id from url with query`() {
        assertEquals(
            42,
            OnboardingViewModel.extractTeamId("https://fantasy.premierleague.com/entry/42/"),
        )
    }

    @Test
    fun `returns null for garbage`() {
        assertNull(OnboardingViewModel.extractTeamId("not a team"))
        assertNull(OnboardingViewModel.extractTeamId(""))
    }

    @Test
    fun `ignores surrounding whitespace`() {
        assertEquals(123, OnboardingViewModel.extractTeamId("  123  "))
    }
}
