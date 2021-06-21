package ch.rjenni.examscheduler.domain.collections

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DestinationToSessionsMapTest {
    private lateinit var destinationToSessionsMap: DestinationToSessionsMap

    @BeforeEach
    fun beforeEach() {
        destinationToSessionsMap = DestinationToSessionsMap()
    }

    @Test
    fun mapSessionToDestination() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"

        // act
        assertTrue(destinationToSessionsMap.allSessionsForDestinationAreClosed(destination))
        destinationToSessionsMap.mapSessionToDestination(sessionId, destination)

        // assert
        assertFalse(destinationToSessionsMap.allSessionsForDestinationAreClosed(destination))
    }

    @Test
    fun unmapSessionFromDestination_shouldCreateNewEntryInMap() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"

        assertTrue(destinationToSessionsMap.isEmpty())

        // act
        destinationToSessionsMap.mapSessionToDestination(sessionId, destination)

        // assert
        assertFalse(destinationToSessionsMap.isEmpty())
    }

    @Test
    fun unmapSessionFromDestination() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"

        // act
        assertTrue(destinationToSessionsMap.allSessionsForDestinationAreClosed(destination))
        destinationToSessionsMap.mapSessionToDestination(sessionId, destination)
        assertFalse(destinationToSessionsMap.allSessionsForDestinationAreClosed(destination))
        destinationToSessionsMap.unmapSessionFromDestination(sessionId, destination)

        // assert
        assertTrue(destinationToSessionsMap.allSessionsForDestinationAreClosed(destination))
    }

    @Test
    fun unmapSessionFromDestination_shouldDeleteDestinationsWithNoSessions() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"

        assertTrue(destinationToSessionsMap.isEmpty())
        destinationToSessionsMap.mapSessionToDestination(sessionId, destination)
        assertFalse(destinationToSessionsMap.isEmpty())

        // act
        destinationToSessionsMap.unmapSessionFromDestination(sessionId, destination)

        // assert
        assertTrue(destinationToSessionsMap.isEmpty())
    }

    @Test
    fun allSessionsForDestinationAreClosed() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"

        // act && assert
        assertTrue(destinationToSessionsMap.allSessionsForDestinationAreClosed(destination))
        destinationToSessionsMap.mapSessionToDestination(sessionId, destination)
        assertFalse(destinationToSessionsMap.allSessionsForDestinationAreClosed(destination))
        destinationToSessionsMap.unmapSessionFromDestination(sessionId, destination)
        assertTrue(destinationToSessionsMap.allSessionsForDestinationAreClosed(destination))
        assertTrue(destinationToSessionsMap.isEmpty())
    }

    @Test
    fun isNewDestination_shouldBeTrue_ifItIs() {
        // arrange
        val destination = "/topic/anything"

        // act && assert
        assertTrue(destinationToSessionsMap.isNewDestination(destination))
    }

    @Test
    fun isNewDestination_shouldBeFalse_ifItIsNot() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"

        // act
        destinationToSessionsMap.mapSessionToDestination(sessionId, destination)

        // assert
        assertFalse(destinationToSessionsMap.isNewDestination(destination))
    }
}
