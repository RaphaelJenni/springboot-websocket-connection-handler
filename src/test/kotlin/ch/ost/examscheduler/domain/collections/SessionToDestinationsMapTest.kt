package ch.rjenni.examscheduler.domain.collections

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SessionToDestinationsMapTest {
    private lateinit var sessionToDestinationsMap: SessionToDestinationsMap

    @BeforeEach
    fun beforeEach() {
        sessionToDestinationsMap = SessionToDestinationsMap()
    }

    @Test
    fun mapDestinationToSession_shouldCreateNewEntryInMap() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"

        assertTrue(sessionToDestinationsMap.isEmpty())

        // act
        sessionToDestinationsMap.mapDestinationToSession(destination, sessionId)

        // assert
        assertFalse(sessionToDestinationsMap.isEmpty())
    }

    @Test
    fun clearSessionAndReturnDestinations_shouldReturn1Destination_ifOnly1() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"
        sessionToDestinationsMap.mapDestinationToSession(destination, sessionId)

        // act
        val destinations = sessionToDestinationsMap.clearSessionAndReturnDestinations(sessionId)

        // assert
        assertEquals(1, destinations.size)
    }

    @Test
    fun clearSessionAndReturnDestinations_shouldReturn2Destinations_if2() {
        // arrange
        val sessionId = "sessionId-123"
        val destination = "/topic/anything"
        sessionToDestinationsMap.mapDestinationToSession(destination, sessionId)
        sessionToDestinationsMap.mapDestinationToSession(destination + destination, sessionId)

        // act
        val destinations = sessionToDestinationsMap.clearSessionAndReturnDestinations(sessionId)

        // assert
        assertEquals(2, destinations.size)
    }

    @Test
    fun clearSessionAndReturnDestinations_shouldReturnAnEmptySet_ifNotPresent() {
        // arrange
        val sessionId = "sessionId-123"

        // act
        val destinations = sessionToDestinationsMap.clearSessionAndReturnDestinations(sessionId)

        // assert
        assertNotNull(destinations)
        assertEquals(0, destinations.size)
    }
}
