package ch.rjenni.examscheduler.domain.collections

import java.util.concurrent.ConcurrentHashMap

internal class DestinationToSessionsMap {
    private val destinationToSessionsMap: ConcurrentHashMap<String, MutableSet<String>> = ConcurrentHashMap()

    fun isEmpty() = destinationToSessionsMap.isEmpty()

    fun allSessionsForDestinationAreClosed(destination: String): Boolean = getDestinationSessionsCount(destination) == 0

    fun isNewDestination(destination: String): Boolean {
        return !destinationToSessionsMap.containsKey(destination)
    }

    fun mapSessionToDestination(sessionId: String, destination: String) {
        destinationToSessionsMap.computeIfAbsent(destination) { HashSet() }.add(sessionId)
    }

    fun unmapSessionFromDestination(sessionId: String, destination: String) {
        val sessions = destinationToSessionsMap[destination]

        if (sessions != null) {
            sessions.remove(sessionId)

            if (sessions.isEmpty()) {
                destinationToSessionsMap.remove(destination)
            }
        }
    }

    private fun getDestinationSessionsCount(destination: String): Int = destinationToSessionsMap[destination]?.size ?: 0

    override fun toString(): String {
        return destinationToSessionsMap.toString()
    }

}
