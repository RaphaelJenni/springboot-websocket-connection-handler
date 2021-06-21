package ch.rjenni.examscheduler.domain.collections

import java.util.concurrent.ConcurrentHashMap

internal class SessionToDestinationsMap {
    private val usernameToDestinationsMap: ConcurrentHashMap<String, MutableSet<String>> = ConcurrentHashMap()

    fun isEmpty() = usernameToDestinationsMap.isEmpty()

    fun clearSessionAndReturnDestinations(sessionId: String) = usernameToDestinationsMap.remove(sessionId) ?: HashSet()

    fun mapDestinationToSession(destination: String, sessionId: String) {
        usernameToDestinationsMap.computeIfAbsent(sessionId) { HashSet() }.add(destination)
    }

    override fun toString(): String {
        return usernameToDestinationsMap.toString()
    }
}
