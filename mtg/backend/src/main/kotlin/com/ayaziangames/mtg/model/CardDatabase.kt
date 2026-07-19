package com.ayaziangames.mtg.model

class CardDefinition(
    val name: String,
    val superTypes: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val subTypes: List<String> = emptyList(),
    val manaCost: List<ManaSymbol> = emptyList(),
    val power: Int? = null,
    val toughness: Int? = null
) {
    fun update(
        name: String = this.name,
        superTypes: List<String> = this.superTypes,
        types: List<String> = this.types,
        subTypes: List<String> = this.subTypes,
        manaCost: List<ManaSymbol> = this.manaCost,
        power: Int? = this.power,
        toughness: Int? = this.toughness
    ): CardDefinition =
        if (
            name === this.name &&
            superTypes === this.superTypes &&
            types === this.types &&
            subTypes === this.subTypes &&
            manaCost === this.manaCost &&
            power == this.power &&
            toughness == this.toughness
        ) {
            this
        } else {
            CardDefinition(
                name = name,
                superTypes = superTypes,
                types = types,
                subTypes = subTypes,
                manaCost = manaCost,
                power = power,
                toughness = toughness
            )
        }
}

class CardDatabase(
    val cardsByName: Map<String, CardDefinition>
) {
    fun cardNamed(name: String): CardDefinition =
        cardsByName[name] ?: error("Unknown card definition: $name.")

    fun update(
        cardsByName: Map<String, CardDefinition> = this.cardsByName
    ): CardDatabase =
        if (cardsByName === this.cardsByName) {
            this
        } else {
            CardDatabase(cardsByName)
        }
}
