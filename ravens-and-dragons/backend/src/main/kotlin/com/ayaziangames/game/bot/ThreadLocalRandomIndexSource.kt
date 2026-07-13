package com.ayaziangames.game.bot

import com.ayaziangames.game.persistence.*
import com.ayaziangames.game.session.*
import com.ayaziangames.game.bot.machine.*
import com.ayaziangames.game.bot.strategy.*
import com.ayaziangames.game.model.*
import com.ayaziangames.game.rules.*


import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

@Component
class ThreadLocalRandomIndexSource : RandomIndexSource {
    override fun nextInt(bound: Int): Int = ThreadLocalRandom.current().nextInt(bound)
}
