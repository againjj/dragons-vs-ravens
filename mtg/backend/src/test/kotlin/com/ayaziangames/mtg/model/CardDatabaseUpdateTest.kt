package com.ayaziangames.mtg.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class CardDatabaseUpdateTest {
    @Test
    fun updateReturnsSameCardDefinitionWhenFieldsAreUnchanged() {
        val definition = CardDefinition(
            name = "Grizzly Bears",
            superTypes = listOf("Basic"),
            types = listOf("Creature"),
            subTypes = listOf("Bear"),
            manaCost = listOf(ManaSymbol.M1, ManaSymbol.G),
            power = 2,
            toughness = 2
        )

        assertSame(definition, definition.update())
    }

    @Test
    fun updateReturnsNewCardDefinitionWhenFieldsChange() {
        val definition = CardDefinition(name = "Forest")

        val updated = definition.update(types = listOf("Land"))

        assertNotSame(definition, updated)
        assertEquals(listOf("Land"), updated.types)
    }

    @Test
    fun updateReturnsSameCardDatabaseWhenMapIsUnchanged() {
        val definition = CardDefinition(name = "Forest")
        val database = CardDatabase(
            name = "cards",
            cardsByName = mapOf(definition.name to definition)
        )

        assertSame(database, database.update())
    }

    @Test
    fun updateReturnsNewCardDatabaseWhenMapChanges() {
        val forest = CardDefinition(name = "Forest")
        val mountain = CardDefinition(name = "Mountain")
        val database = CardDatabase(
            name = "cards",
            cardsByName = mapOf(forest.name to forest)
        )

        val updated = database.update(cardsByName = mapOf(mountain.name to mountain))

        assertNotSame(database, updated)
        assertSame(mountain, updated.cardNamed("Mountain"))
    }
}
