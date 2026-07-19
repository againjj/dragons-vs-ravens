package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.CardDatabase
import com.ayaziangames.mtg.model.CardDefinition
import com.ayaziangames.mtg.model.ManaSymbol

@MtgDsl
class CardDatabaseBuilder internal constructor() {
    private val assignedName = SingleAssignment<String?>("name", null)
    private val cards = mutableListOf<CardDefinition>()

    var name: String?
        get() = assignedName.value
        set(value) = assignedName.set(value)

    fun cardDefinition(init: @MtgDsl CardDefinitionBuilder.() -> Unit) {
        cards += CardDefinitionBuilder().apply(init).build()
    }

    internal fun build(): CardDatabase {
        val duplicates = cards.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) {
            "Duplicate card definitions: ${duplicates.joinToString()}."
        }
        return CardDatabase(
            name = requireNotNull(name) { "Card definitions require a name." },
            cardsByName = cards.associateBy { it.name }
        )
    }
}

@MtgDsl
class CardDefinitionBuilder internal constructor() {
    private val assignedName = SingleAssignment<String?>("name", null)
    private val assignedSuperTypes = SingleAssignment("superTypes", emptyList<String>())
    private val assignedTypes = SingleAssignment("types", emptyList<String>())
    private val assignedSubTypes = SingleAssignment("subTypes", emptyList<String>())
    private val assignedManaCost = SingleAssignment("manaCost", emptyList<ManaSymbol>())
    private val assignedPower = SingleAssignment<Int?>("power", null)
    private val assignedToughness = SingleAssignment<Int?>("toughness", null)

    var name: String?
        get() = assignedName.value
        set(value) = assignedName.set(value)
    var superTypes: List<String>
        get() = assignedSuperTypes.value
        set(value) = assignedSuperTypes.set(value)
    var types: List<String>
        get() = assignedTypes.value
        set(value) = assignedTypes.set(value)
    var subTypes: List<String>
        get() = assignedSubTypes.value
        set(value) = assignedSubTypes.set(value)
    var manaCost: List<ManaSymbol>
        get() = assignedManaCost.value
        set(value) = assignedManaCost.set(value)
    var power: Int?
        get() = assignedPower.value
        set(value) = assignedPower.set(value)
    var toughness: Int?
        get() = assignedToughness.value
        set(value) = assignedToughness.set(value)

    internal fun build(): CardDefinition {
        require((power == null) == (toughness == null)) {
            "Card definition must specify both power and toughness, or neither."
        }
        return CardDefinition(
            name = requireNotNull(name) { "Card definition requires a name." },
            superTypes = superTypes.toList(),
            types = types.toList(),
            subTypes = subTypes.toList(),
            manaCost = manaCost.toList(),
            power = power,
            toughness = toughness
        )
    }
}

fun cardDefinitions(init: @MtgDsl CardDatabaseBuilder.() -> Unit): CardDatabase =
    CardDatabaseBuilder().apply(init).build()
