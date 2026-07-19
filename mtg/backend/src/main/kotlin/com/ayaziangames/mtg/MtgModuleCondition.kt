package com.ayaziangames.mtg

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class MtgModuleCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        context.environment
            .getProperty("ayazian-games.local-game-modules", "")
            .split(',')
            .map { it.trim() }
            .contains(MtgGameModuleDefinition.identity.slug)
}
