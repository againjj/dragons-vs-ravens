package com.ayaziangames.mtg.dsl

import com.ayaziangames.mtg.model.Card
import com.ayaziangames.mtg.model.CardDatabase

@MtgDsl
class DeckBuilder internal constructor(
    private val availableCards: CardDatabase
) {
    private val cards = mutableListOf<Card>()

    fun card(name: String) {
        cards += Card(availableCards.cardNamed(name))
    }

    internal fun build(): List<Card> = cards.toList()
}
