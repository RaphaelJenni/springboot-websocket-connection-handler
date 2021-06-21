package ch.rjenni.examscheduler.domain.collections

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UsernameToSessionsMapTest {
    private lateinit var usernameToSessionsMap: UsernameToSessionsMap

    @BeforeEach
    fun beforeEach() {
        usernameToSessionsMap = UsernameToSessionsMap()
    }

    @Test
    fun mapSessionToUser() {
        // arrange
        val sessionId = "sessionId-123"
        val user = "username1"

        // act
        assertTrue(usernameToSessionsMap.allSessionsForUserAreClosed(user))
        usernameToSessionsMap.mapSessionToUser(sessionId, user)

        // assert
        assertFalse(usernameToSessionsMap.allSessionsForUserAreClosed(user))
    }

    @Test
    fun mapSessionToUser_shouldCreateNewEntryInMap() {
        // arrange
        val sessionId = "sessionId-123"
        val user = "username1"

        assertTrue(usernameToSessionsMap.isEmpty())

        // act
        usernameToSessionsMap.mapSessionToUser(sessionId, user)

        // assert
        assertFalse(usernameToSessionsMap.isEmpty())
    }

    @Test
    fun unmapSessionFromUser() {
        // arrange
        val sessionId = "sessionId-123"
        val user = "username1"

        // act
        assertTrue(usernameToSessionsMap.allSessionsForUserAreClosed(user))
        usernameToSessionsMap.mapSessionToUser(sessionId, user)
        assertFalse(usernameToSessionsMap.allSessionsForUserAreClosed(user))
        usernameToSessionsMap.unmapSessionFromUser(sessionId, user)

        // assert
        assertTrue(usernameToSessionsMap.allSessionsForUserAreClosed(user))
    }

    @Test
    fun unmapSessionFromUser_shouldDeleteUsersWithNoSessions() {
        // arrange
        val sessionId = "sessionId-123"
        val user = "username1"

        assertTrue(usernameToSessionsMap.isEmpty())
        usernameToSessionsMap.mapSessionToUser(sessionId, user)
        assertFalse(usernameToSessionsMap.isEmpty())

        // act
        usernameToSessionsMap.unmapSessionFromUser(sessionId, user)

        // assert
        assertTrue(usernameToSessionsMap.isEmpty())
    }

    @Test
    fun allSessionsForUserAreClosed() {
        // arrange
        val sessionId = "sessionId-123"
        val user = "username1"

        // act && assert
        assertTrue(usernameToSessionsMap.allSessionsForUserAreClosed(user))
        usernameToSessionsMap.mapSessionToUser(sessionId, user)
        assertFalse(usernameToSessionsMap.allSessionsForUserAreClosed(user))
        usernameToSessionsMap.unmapSessionFromUser(sessionId, user)
        assertTrue(usernameToSessionsMap.allSessionsForUserAreClosed(user))
    }
}
