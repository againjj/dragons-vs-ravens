package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.ManaSymbol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CardDatabaseDslTest {
    @Test
    fun buildsCardDefinitionsWithAllConfiguredFields() {
        val cards = cardDefinitions {
            cardDefinition {
                name = "Grizzly Bears"
                types = listOf("Creature")
                subTypes = listOf("Bear")
                manaCost = listOf(ManaSymbol.M1, ManaSymbol.G)
                power = 2
                toughness = 2
            }
            cardDefinition {
                name = "Forest"
                superTypes = listOf("Basic")
                types = listOf("Land")
                subTypes = listOf("Forest")
            }
        }

        val bears = cards.cardNamed("Grizzly Bears")
        assertEquals("Grizzly Bears", bears.name)
        assertEquals(emptyList(), bears.superTypes)
        assertEquals(listOf("Creature"), bears.types)
        assertEquals(listOf("Bear"), bears.subTypes)
        assertEquals(listOf(ManaSymbol.M1, ManaSymbol.G), bears.manaCost)
        assertEquals(2, bears.power)
        assertEquals(2, bears.toughness)

        val forest = cards.cardNamed("Forest")
        assertEquals("Forest", forest.name)
        assertEquals(listOf("Basic"), forest.superTypes)
        assertEquals(listOf("Land"), forest.types)
        assertEquals(listOf("Forest"), forest.subTypes)
        assertEquals(emptyList(), forest.manaCost)
        assertEquals(null, forest.power)
        assertEquals(null, forest.toughness)
    }

    @Test
    fun rejectsDuplicateCardDefinitions() {
        val exception = assertFailsWith<IllegalArgumentException> {
            cardDefinitions {
                cardDefinition {
                    name = "Forest"
                }
                cardDefinition {
                    name = "Forest"
                }
            }
        }

        assertEquals("Duplicate card definitions: Forest.", exception.message)
    }

    @Test
    fun requiresPowerAndToughnessToBeSpecifiedTogether() {
        val exception = assertFailsWith<IllegalArgumentException> {
            cardDefinitions {
                cardDefinition {
                    name = "Grizzly Bears"
                    power = 2
                }
            }
        }

        assertEquals("Card definition must specify both power and toughness, or neither.", exception.message)
    }

    @Test
    fun rejectsCardDefinitionFieldsAssignedMoreThanOnce() {
        assertDuplicateCardDefinitionField("name") {
            name = "Forest"
            name = "Mountain"
        }
        assertDuplicateCardDefinitionField("superTypes") {
            name = "Forest"
            superTypes = listOf("Basic")
            superTypes = listOf("Legendary")
        }
        assertDuplicateCardDefinitionField("types") {
            name = "Forest"
            types = listOf("Land")
            types = listOf("Creature")
        }
        assertDuplicateCardDefinitionField("subTypes") {
            name = "Forest"
            subTypes = listOf("Forest")
            subTypes = listOf("Island")
        }
        assertDuplicateCardDefinitionField("manaCost") {
            name = "Grizzly Bears"
            manaCost = listOf(ManaSymbol.M1, ManaSymbol.G)
            manaCost = listOf(ManaSymbol.M2, ManaSymbol.G)
        }
        assertDuplicateCardDefinitionField("power") {
            name = "Grizzly Bears"
            power = 2
            power = 3
        }
        assertDuplicateCardDefinitionField("toughness") {
            name = "Grizzly Bears"
            toughness = 2
            toughness = 3
        }
    }

    private fun assertDuplicateCardDefinitionField(
        field: String,
        init: @MtgDsl CardDefinitionBuilder.() -> Unit
    ) {
        val exception = assertFailsWith<IllegalArgumentException> {
            cardDefinitions {
                cardDefinition(init)
            }
        }
        assertEquals("Property $field can only be assigned once.", exception.message)
    }
}
