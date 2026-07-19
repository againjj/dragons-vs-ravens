package com.ayaziangames

import com.ayaziangames.game.RavensAndDragonsGameModuleDefinition
import com.ayaziangames.ginrummy.GinRummyGameModuleDefinition
import com.ayaziangames.lunarbase.LunarBaseGameModuleDefinition
import com.ayaziangames.mtg.MtgGameModuleDefinition
import com.ayaziangames.platform.game.GameModuleRegistry
import com.ayaziangames.tictactoe.TicTacToeGameModuleDefinition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.Duration

@SpringBootApplication
@EnableScheduling
class AyazianGamesApplication {
    @Bean
    fun systemClock(): Clock = Clock.systemUTC()

    @Bean
    fun gameModuleRegistry(
        @Value("\${ayazian-games.local-game-modules:}")
        localGameModules: String
    ): GameModuleRegistry =
        GameModuleRegistry(
            buildList {
                add(TicTacToeGameModuleDefinition)
                add(GinRummyGameModuleDefinition)
                add(LunarBaseGameModuleDefinition)
                if (localGameModules.asModuleSet().contains(MtgGameModuleDefinition.identity.slug)) {
                    add(MtgGameModuleDefinition)
                }
                add(RavensAndDragonsGameModuleDefinition)
            }
        )

    @Bean("staleGameCleanupDelay")
    fun staleGameCleanupDelay(
        @Value("\${platform.games.stale-threshold:1008h}")
        staleGameThreshold: Duration
    ): Duration =
        staleGameThreshold.dividedBy(10).takeIf { !it.isZero } ?: Duration.ofMillis(1)

    private fun String.asModuleSet(): Set<String> =
        split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
}

fun main(args: Array<String>) {
    runApplication<AyazianGamesApplication>(*args)
}
