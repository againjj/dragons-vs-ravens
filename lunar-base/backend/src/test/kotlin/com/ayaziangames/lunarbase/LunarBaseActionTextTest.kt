package com.ayaziangames.lunarbase

import com.ayaziangames.lunarbase.cards.LunarBaseAnyNumberFlipStationAmount
import com.ayaziangames.lunarbase.cards.LunarBaseActionScope
import com.ayaziangames.lunarbase.cards.LunarBaseBuildAction
import com.ayaziangames.lunarbase.cards.LunarBaseChooseOneAction
import com.ayaziangames.lunarbase.cards.LunarBaseDiscardAction
import com.ayaziangames.lunarbase.cards.LunarBaseDoAllAction
import com.ayaziangames.lunarbase.cards.LunarBaseDrawAction
import com.ayaziangames.lunarbase.cards.LunarBaseDraftAction
import com.ayaziangames.lunarbase.cards.LunarBaseFlipStationAction
import com.ayaziangames.lunarbase.cards.LunarBaseFlipStationToAction
import com.ayaziangames.lunarbase.cards.LunarBaseGainCreditsAction
import com.ayaziangames.lunarbase.cards.LunarBaseHandSizeAmount
import com.ayaziangames.lunarbase.cards.LunarBaseInfluenceCountAmount
import com.ayaziangames.lunarbase.cards.LunarBaseLiteralAmount
import com.ayaziangames.lunarbase.cards.LunarBaseLiteralFlipStationAmount
import com.ayaziangames.lunarbase.cards.LunarBaseLoseCreditsAction
import com.ayaziangames.lunarbase.cards.LunarBasePlayerReference
import com.ayaziangames.lunarbase.cards.LunarBaseResellAction
import com.ayaziangames.lunarbase.cards.LunarBaseScopedAction
import com.ayaziangames.lunarbase.cards.LunarBaseSelfFlipStationAmount
import com.ayaziangames.lunarbase.cards.LunarBaseStationSide
import com.ayaziangames.lunarbase.cards.LunarBaseStealCreditsAction
import com.ayaziangames.lunarbase.cards.LunarBaseStealModuleAction
import com.ayaziangames.lunarbase.cards.LunarBaseStaticCardEffect
import com.ayaziangames.lunarbase.cards.LunarBaseStaticEffect
import com.ayaziangames.lunarbase.cards.LunarBaseTrigger
import com.ayaziangames.lunarbase.cards.LunarBaseTriggeredCardEffect
import com.ayaziangames.lunarbase.cards.LunarBaseChooseOpponentAction
import com.ayaziangames.lunarbase.cards.LunarBaseViewHandAction
import kotlin.test.Test
import kotlin.test.assertEquals

class LunarBaseActionTextTest {
    @Test
    fun topLevelChoiceUsesLineBreaksBetweenChoices() {
        val text = listOf(
            LunarBaseChooseOneAction(
                listOf(
                    LunarBaseDraftAction(LunarBaseLiteralAmount(1)),
                    LunarBaseDrawAction(LunarBaseLiteralAmount(2))
                )
            )
        ).toActionText()

        assertEquals("Choose one:\nDraft 1 card\nDraw 2 cards", text)
    }

    @Test
    fun chooseOneInsideTopLevelSequenceUsesInlineChoices() {
        val text = listOf(
            LunarBaseChooseOpponentAction,
            LunarBaseViewHandAction(LunarBasePlayerReference.CHOSEN_PLAYER),
            LunarBaseChooseOneAction(
                listOf(
                    LunarBaseDrawAction(LunarBaseLiteralAmount(1)),
                    LunarBaseScopedAction(
                        LunarBaseActionScope.CHOSEN_PLAYER,
                        listOf(LunarBaseDiscardAction(LunarBaseLiteralAmount(1)))
                    )
                )
            )
        ).toActionText()

        assertEquals(
            "Choose an opponent\n" +
                "View chosen player's hand\n" +
                "Choose one: Draw 1 card or Chosen player: Discard 1 card",
            text
        )
    }

    @Test
    fun topLevelDoAllUsesLineBreaksBetweenActions() {
        val text = listOf(
            LunarBaseDoAllAction(
                listOf(
                    LunarBaseBuildAction(LunarBaseLiteralAmount(1)),
                    LunarBaseDiscardAction(LunarBaseLiteralAmount(2))
                )
            )
        ).toActionText()

        assertEquals("Build 1 module\nDiscard 2 cards", text)
    }

    @Test
    fun nonNumericAmountsNameTheRulePhrase() {
        val text = listOf(
            LunarBaseDiscardAction(LunarBaseHandSizeAmount),
            LunarBaseResellAction(LunarBaseInfluenceCountAmount),
            LunarBaseGainCreditsAction(LunarBaseHandSizeAmount)
        ).toActionText()

        assertEquals(
            "Discard cards equal to your hand size\n" +
                "Resell cards equal to the number of influences in the supply\n" +
                "Gain credits equal to your hand size",
            text
        )
    }

    @Test
    fun countedActionsUseTheRightThingNames() {
        val text = listOf(
            LunarBaseDraftAction(LunarBaseLiteralAmount(1)),
            LunarBaseDrawAction(LunarBaseLiteralAmount(2)),
            LunarBaseBuildAction(LunarBaseLiteralAmount(1)),
            LunarBaseLoseCreditsAction(LunarBaseLiteralAmount(1)),
            LunarBaseStealCreditsAction(LunarBaseLiteralAmount(2)),
            LunarBaseStealModuleAction("Satellite")
        ).toActionText()

        assertEquals(
            "Draft 1 card\n" +
                "Draw 2 cards\n" +
                "Build 1 module\n" +
                "Lose 1 credit\n" +
                "Steal 2 credits\n" +
                "Steal a Satellite",
            text
        )
    }

    @Test
    fun flipStationActionsUseStationPhrases() {
        val text = listOf(
            LunarBaseFlipStationAction(LunarBaseLiteralFlipStationAmount(1)),
            LunarBaseFlipStationAction(LunarBaseLiteralFlipStationAmount(2)),
            LunarBaseFlipStationAction(LunarBaseAnyNumberFlipStationAmount),
            LunarBaseFlipStationAction(LunarBaseSelfFlipStationAmount),
            LunarBaseFlipStationToAction(LunarBaseStationSide.AGENDA_SIDE)
        ).toActionText()

        assertEquals(
            "Flip 1 station\n" +
                "Flip 2 stations\n" +
                "Flip any number of stations\n" +
                "Flip your station\n" +
                "Flip your station to Agenda Side",
            text
        )
    }

    @Test
    fun effectsUseReadableCatalogText() {
        assertEquals(
            "Red orbs gain credits as well as yellow orbs",
            LunarBaseStaticCardEffect(LunarBaseStaticEffect.RED_ORBS_GAIN_CREDITS).toEffectText()
        )
        assertEquals(
            "When this influence is discarded:\nDraw 4 cards; Discard 3 cards",
            LunarBaseTriggeredCardEffect(
                LunarBaseTrigger.DISCARD_THIS_INFLUENCE,
                listOf(
                    LunarBaseDrawAction(LunarBaseLiteralAmount(4)),
                    LunarBaseDiscardAction(LunarBaseLiteralAmount(3))
                )
            ).toEffectText()
        )
    }
}
