package com.ayaziangames.mtg.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class CardUpdateTest {
    @Test
    fun exposesTheSupportedManaSymbolVocabulary() {
        assertEquals(
            listOf(
                "W", "U", "B", "R", "G", "C", "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9",
                "X", "WU", "WB", "UB", "UR", "BR", "BG", "RG", "RW", "GW", "GU", "W2", "U2", "B2",
                "R2", "G2", "CW", "CU", "CB", "CR", "CG", "WP", "UP", "BP", "RP", "GP", "WUP",
                "WBP", "UBP", "URP", "BRP", "BGP", "RGP", "RWP", "GWP", "GUP", "S"
            ),
            ManaSymbol.entries.map { it.name }
        )
    }

    @Test
    fun updateReturnsSameCardWhenDefinitionIsUnchanged() {
        val definition = CardDefinition(name = "Forest")
        val card = Card(definition)

        assertSame(card, card.update())
    }

    @Test
    fun updateReturnsNewCardWhenDefinitionChanges() {
        val forest = CardDefinition(name = "Forest")
        val mountain = CardDefinition(name = "Mountain")
        val card = Card(forest)

        val updated = card.update(definition = mountain)

        assertNotSame(card, updated)
        assertSame(mountain, updated.definition)
    }
}
