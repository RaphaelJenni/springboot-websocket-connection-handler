package ch.rjenni.examscheduler.domain.collections

import java.util.concurrent.ConcurrentHashMap

internal class UsernameToSessionsMap {
    private val usernameToSessionsMap: ConcurrentHashMap<String, MutableSet<String>> = ConcurrentHashMap()

    fun isEmpty() = usernameToSessionsMap.isEmpty()

    fun allSessionsForUserAreClosed(user: String): Boolean = getUserSessionsCount(user) == 0

    fun mapSessionToUser(sessionId: String, user: String) {
        usernameToSessionsMap.computeIfAbsent(user) { HashSet() }.add(sessionId)
    }

    fun unmapSessionFromUser(sessionId: String, user: String) {
        val sessions = usernameToSessionsMap[user]

        if (sessions != null) {
            sessions.remove(sessionId)

            if (sessions.isEmpty()) {
                usernameToSessionsMap.remove(user)
            }
        }
    }

    private fun getUserSessionsCount(user: String): Int = usernameToSessionsMap[user]?.size ?: 0

    override fun toString(): String {
        return usernameToSessionsMap.toString()
    }
}
