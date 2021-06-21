package ch.rjenni.examscheduler.domain.collections

import java.util.concurrent.ConcurrentHashMap

internal class UsernameToDestinationsMap {
    private val usernameToDestinationsMap: ConcurrentHashMap<String, MutableSet<String>> = ConcurrentHashMap()

    fun isEmpty() = usernameToDestinationsMap.isEmpty()

    fun clearUserAndReturnDestinations(username: String) = usernameToDestinationsMap.remove(username) ?: HashSet()

    fun isNewDestinationForUser(destination: String, username: String): Boolean {
        return !getPathsForUser(username).contains(destination)
    }

    fun mapDestinationToUser(destination: String, username: String) {
        usernameToDestinationsMap.computeIfAbsent(username) { HashSet() }.add(destination)
    }

    private fun getPathsForUser(username: String) = usernameToDestinationsMap[username] ?: HashSet()

    override fun toString(): String {
        return usernameToDestinationsMap.toString()
    }
}
