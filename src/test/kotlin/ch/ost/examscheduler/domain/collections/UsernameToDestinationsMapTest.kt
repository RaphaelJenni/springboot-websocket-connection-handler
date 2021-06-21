package ch.rjenni.examscheduler.domain.collections

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UsernameToDestinationsMapTest {

    private lateinit var usernameToDestinationsMap: UsernameToDestinationsMap

    @BeforeEach
    fun beforeEach() {
        usernameToDestinationsMap = UsernameToDestinationsMap()
    }

    @Test
    fun mapDestinationToUser_shouldCreateNewEntryInMap() {
        // arrange
        val username = "username1"
        val destination = "/topic/anything"

        assertTrue(usernameToDestinationsMap.isEmpty())

        // act
        usernameToDestinationsMap.mapDestinationToUser(username, destination)

        // assert
        assertFalse(usernameToDestinationsMap.isEmpty())
    }

    @Test
    fun clearUserAndReturnDestinations_shouldReturn1Destination_ifOnly1() {
        // arrange
        val username = "username1"
        val destination = "/topic/anything"
        usernameToDestinationsMap.mapDestinationToUser(destination, username)

        // act
        val destinations = usernameToDestinationsMap.clearUserAndReturnDestinations(username)

        // assert
        assertEquals(1, destinations.size)
    }

    @Test
    fun clearUserAndReturnDestinations_shouldReturn2Destinations_if2() {
        // arrange
        val username = "username1"
        val destination = "/topic/anything"
        usernameToDestinationsMap.mapDestinationToUser(destination, username)
        usernameToDestinationsMap.mapDestinationToUser(destination + destination, username)

        // act
        val destinations = usernameToDestinationsMap.clearUserAndReturnDestinations(username)

        // assert
        assertEquals(2, destinations.size)
    }

    @Test
    fun clearUserAndReturnDestinations_shouldReturnAnEmptySet_ifNotPresent() {
        // arrange
        val username = "username1"

        // act
        val destinations = usernameToDestinationsMap.clearUserAndReturnDestinations(username)

        // assert
        assertNotNull(destinations)
        assertEquals(0, destinations.size)
    }

    @Test
    fun isNewDestination_shouldBeTrue_ifItIs() {
        // arrange
        val username = "username1"
        val destination = "/topic/anything"

        // act && assert
        assertTrue(usernameToDestinationsMap.isNewDestinationForUser(destination, username))
    }

    @Test
    fun isNewDestination_shouldBeFalse_ifItIsNot() {
        // arrange
        val username = "username1"
        val destination = "/topic/anything"

        // act
        usernameToDestinationsMap.mapDestinationToUser(destination, username)

        // assert
        assertFalse(usernameToDestinationsMap.isNewDestinationForUser(destination, username))
    }
}
