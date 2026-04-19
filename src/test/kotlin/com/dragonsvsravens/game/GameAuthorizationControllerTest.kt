package com.dragonsvsravens.game

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class GameAuthorizationControllerTest : AbstractGameControllerTestSupport() {

    @Test
    fun `anonymous users cannot load a game`() {
        val game = createGame()

        anonymousGetGame(game.id).andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `anonymous users cannot load a game view`() {
        val game = createGame()

        mockMvc.get("/api/games/${game.id}/view") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `anonymous users cannot open a game stream`() {
        val game = createGame()

        mockMvc.get("/api/games/${game.id}/stream") {
            accept = MediaType.TEXT_EVENT_STREAM
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `anonymous users cannot submit commands`() {
        val game = createGame()

        anonymousPostGameCommand(game.id, command(game.version, "start-game")).andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `authenticated user can claim an open side`() {
        val game = createGame()
        assignSides(game.id, null, null)

        claimSide(game.id, Side.dragons, defaultTestUserId).andExpect {
            status { isOk() }
            jsonPath("$.dragonsPlayerUserId", equalTo(defaultTestUserId))
        }
    }

    @Test
    fun `authenticated user can claim both open sides and others cannot steal them`() {
        val game = createGame()
        assignSides(game.id, null, null)

        claimSide(game.id, Side.dragons, defaultTestUserId).andExpect {
            status { isOk() }
            jsonPath("$.dragonsPlayerUserId", equalTo(defaultTestUserId))
        }

        claimSide(game.id, Side.ravens, defaultTestUserId).andExpect {
            status { isOk() }
            jsonPath("$.dragonsPlayerUserId", equalTo(defaultTestUserId))
            jsonPath("$.ravensPlayerUserId", equalTo(defaultTestUserId))
        }

        claimSide(game.id, Side.dragons, alternateTestUserId).andExpect {
            status { isForbidden() }
            jsonPath("$.message", equalTo("Dragons is already claimed."))
        }
    }

    @Test
    fun `spectator cannot submit commands`() {
        val game = createGame()
        assignSides(game.id, defaultTestUserId, alternateTestUserId)
        seedUser("spectator-user", "Spectator")

        authenticatedPostGameCommand(
            game.id,
            command(game.version, "start-game"),
            userId = "spectator-user"
        ).andExpect {
            status { isForbidden() }
            jsonPath("$.message", equalTo("You must claim a side before submitting commands."))
        }
    }

    @Test
    fun `wrong side cannot move when it is not their turn`() {
        seedUser("third-user", "Third Player")
        val game = createGame()
        assignSides(game.id, defaultTestUserId, alternateTestUserId)

        authenticatedPostGameCommand(
            game.id,
            command(game.version, "start-game"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(1, "end-setup"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(2, "move-piece", origin = "a1", destination = "a2"),
            userId = alternateTestUserId
        ).andExpect {
            status { isForbidden() }
            jsonPath("$.message", equalTo("It is not your turn."))
        }
    }

    @Test
    fun `free play setup allows both players to place pieces when dragons start`() {
        val game = createGame()
        assignSides(game.id, defaultTestUserId, alternateTestUserId)

        authenticatedPostGameCommand(
            game.id,
            command(game.version, "start-game"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(1, "cycle-setup", square = "a1"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
            jsonPath("$.snapshot.board.a1", equalTo("dragon"))
        }

        authenticatedPostGameCommand(
            game.id,
            command(2, "cycle-setup", square = "b1"),
            userId = alternateTestUserId
        ).andExpect {
            status { isOk() }
            jsonPath("$.snapshot.board.a1", equalTo("dragon"))
            jsonPath("$.snapshot.board.b1", equalTo("dragon"))
        }
    }

    @Test
    fun `free play setup allows both players to place pieces when ravens start`() {
        val game = createGame()
        assignSides(game.id, defaultTestUserId, alternateTestUserId)

        authenticatedPostGameCommand(
            game.id,
            command(game.version, "select-starting-side", side = Side.ravens),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(1, "start-game"),
            userId = alternateTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(2, "cycle-setup", square = "a1"),
            userId = alternateTestUserId
        ).andExpect {
            status { isOk() }
            jsonPath("$.snapshot.board.a1", equalTo("dragon"))
        }

        authenticatedPostGameCommand(
            game.id,
            command(3, "cycle-setup", square = "b1"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
            jsonPath("$.snapshot.board.a1", equalTo("dragon"))
            jsonPath("$.snapshot.board.b1", equalTo("dragon"))
        }
    }

    @Test
    fun `player who made the last move can undo after the turn passes`() {
        val game = createGame()
        assignSides(game.id, defaultTestUserId, alternateTestUserId)

        authenticatedPostGameCommand(
            game.id,
            command(game.version, "start-game"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(1, "cycle-setup", square = "a1"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(2, "end-setup"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(3, "move-piece", origin = "a1", destination = "a2"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(4, "undo"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `current player cannot undo the opponent last move`() {
        val game = createGame()
        assignSides(game.id, defaultTestUserId, alternateTestUserId)

        authenticatedPostGameCommand(
            game.id,
            command(game.version, "start-game"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(1, "cycle-setup", square = "a1"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(2, "end-setup"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(3, "move-piece", origin = "a1", destination = "a2"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(4, "undo"),
            userId = alternateTestUserId
        ).andExpect {
            status { isForbidden() }
            jsonPath("$.message", equalTo("Only the player who made the last move may undo."))
        }
    }

    @Test
    fun `player who owns both sides can undo after either side moves`() {
        val game = createGame()

        authenticatedPostGameCommand(
            game.id,
            command(game.version, "start-game"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        setupDragonAt(game.id, "a1")
        setupRavenAt(game.id, "b1")
        endSetup(game.id)

        authenticatedPostGameCommand(
            game.id,
            command(currentVersion(game.id), "move-piece", origin = "a1", destination = "a2"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(currentVersion(game.id), "skip-capture"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        authenticatedPostGameCommand(
            game.id,
            command(currentVersion(game.id), "move-piece", origin = "b1", destination = "b2"),
            userId = defaultTestUserId
        ).andExpect {
            status { isOk() }
        }

        val undone = executeGameCommand(game.id, command(currentVersion(game.id), "undo"))

        assertAll(
            { assertEquals(Phase.move, undone.snapshot.phase) },
            { assertEquals(Side.ravens, undone.snapshot.activeSide) },
            { assertEquals(Piece.raven, undone.snapshot.board["b1"]) },
            { assertEquals(null, undone.snapshot.board["b2"]) }
        )
    }

    @Test
    fun `game view response includes viewer role and seat info`() {
        val game = createGame()
        assignSides(game.id, defaultTestUserId, alternateTestUserId)

        mockMvc.get("/api/games/${game.id}/view") {
            with(authenticated(game.id, defaultTestUserId))
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.viewerRole", equalTo("dragons"))
            jsonPath("$.currentUser.id", equalTo(defaultTestUserId))
            jsonPath("$.dragonsPlayer.id", equalTo(defaultTestUserId))
            jsonPath("$.ravensPlayer.id", equalTo(alternateTestUserId))
        }
    }
}
