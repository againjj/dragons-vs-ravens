package com.ayaziangames.mtg.model

enum class ManaSymbol {
    W,
    U,
    B,
    R,
    G,
    C,
    M1,
    M2,
    M3,
    M4,
    M5,
    M6,
    M7,
    M8,
    M9,
    X,
    WU,
    WB,
    UB,
    UR,
    BR,
    BG,
    RG,
    RW,
    GW,
    GU,
    W2,
    U2,
    B2,
    R2,
    G2,
    CW,
    CU,
    CB,
    CR,
    CG,
    WP,
    UP,
    BP,
    RP,
    GP,
    WUP,
    WBP,
    UBP,
    URP,
    BRP,
    BGP,
    RGP,
    RWP,
    GWP,
    GUP,
    S
}

class Card(
    val definition: CardDefinition
) {
    fun serialize(): String =
        "card(${definition.name.serializeString()})"

    fun update(
        definition: CardDefinition = this.definition
    ): Card =
        if (definition === this.definition) {
            this
        } else {
            Card(definition)
        }
}
