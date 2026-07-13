package com.ayaziangames

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.Duration

@SpringBootApplication
@EnableScheduling
class TestAyazianGamesApplication {
    @Bean
    fun systemClock(): Clock = Clock.systemUTC()

    @Bean("staleGameCleanupDelay")
    fun staleGameCleanupDelay(
        @Value("\${platform.games.stale-threshold:1008h}")
        staleGameThreshold: Duration
    ): Duration =
        staleGameThreshold.dividedBy(10).takeIf { !it.isZero } ?: Duration.ofMillis(1)
}
