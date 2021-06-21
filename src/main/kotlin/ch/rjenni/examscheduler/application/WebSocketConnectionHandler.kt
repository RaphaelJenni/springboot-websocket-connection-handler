package ch.rjenni.examscheduler.application

import ch.rjenni.examscheduler.application.exceptions.UnauthorizedException
import ch.rjenni.examscheduler.domain.collections.DestinationToSessionsMap
import ch.rjenni.examscheduler.domain.collections.SessionToDestinationsMap
import ch.rjenni.examscheduler.domain.collections.UsernameToDestinationsMap
import ch.rjenni.examscheduler.domain.collections.UsernameToSessionsMap
import ch.rjenni.examscheduler.domain.events.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.security.Principal
import java.util.function.Consumer

@Service
internal class WebSocketConnectionHandler(
    private val applicationEventPublisher: ApplicationEventPublisher

) {
    private val usernameToSessionsMap: UsernameToSessionsMap = UsernameToSessionsMap()
    private val usernameToUserDestinationsMap: UsernameToDestinationsMap = UsernameToDestinationsMap()

    private val sessionToPublicDestinationsMap: SessionToDestinationsMap = SessionToDestinationsMap()
    private val publicDestinationToSessionsMap: DestinationToSessionsMap = DestinationToSessionsMap()


    @EventListener
    fun handleSessionSubscribed(event: SessionSubscribeEvent) {
        val destinationPath = event.message.headers["simpDestination"] as String

        if (isUserDestination(destinationPath)) {
            handleUserDestinationSubscribed(event)
        } else {
            handlePublicDestinationSubscribed(event)
        }
    }

    @EventListener
    fun handleSessionUnsubscribed(event: SessionUnsubscribeEvent) {
        val sessionId = event.message.headers["simpSessionId"] as String
        val userPrincipal = event.user ?: event.message.headers["simpUser"] as Principal?

        handleSessionUnsubscribedOrDisconnected(sessionId, userPrincipal)
    }

    @EventListener
    fun handleSessionDisconnected(event: SessionDisconnectEvent) {
        val sessionId = event.message.headers["simpSessionId"] as String
        val userPrincipal = event.user ?: event.message.headers["simpUser"] as Principal?

        handleSessionUnsubscribedOrDisconnected(sessionId, userPrincipal)
    }

    private fun handleSessionUnsubscribedOrDisconnected(sessionId: String, userPrincipal: Principal?) {
        if (userPrincipal != null) {
            handleUserSessionDisconnected(sessionId, userPrincipal)
        }
        handlePublicSessionDisconnected(sessionId)
    }

    private fun isUserDestination(destinationPath: String) = destinationPath.startsWith("/user")

    private fun handleUserDestinationSubscribed(event: SessionSubscribeEvent) {
        val sessionId = event.message.headers["simpSessionId"] as String
        val destinationPath = event.message.headers["simpDestination"] as String
        val userPrincipal = event.user
            ?: event.message.headers["simpUser"] as Principal?
            ?: throw UnauthorizedException("User principal is not set - user is not authenticated!")


        val username = userPrincipal.name

        usernameToSessionsMap.mapSessionToUser(sessionId, username)

        if (usernameToUserDestinationsMap.isNewDestinationForUser(destinationPath, username)) {
            val wsEvent = WebSocketNewUserPathConnectionEvent(this, userPrincipal, sessionId, destinationPath)
            applicationEventPublisher.publishEvent(wsEvent)
            usernameToUserDestinationsMap.mapDestinationToUser(destinationPath, username)
        } else {
            val wsEvent =
                WebSocketExistingUserPathConnectionEvent(this, userPrincipal, sessionId, destinationPath)
            applicationEventPublisher.publishEvent(wsEvent)
        }
    }

    private fun handlePublicDestinationSubscribed(event: SessionSubscribeEvent) {
        val sessionId = event.message.headers["simpSessionId"] as String
        val destinationPath = event.message.headers["simpDestination"] as String

        val wsEvent = if (publicDestinationToSessionsMap.isNewDestination(destinationPath)) {
            WebSocketNewPathConnectionEvent(this, sessionId, destinationPath)
        } else {
            WebSocketExistingPathConnectionEvent(this, sessionId, destinationPath)
        }
        publicDestinationToSessionsMap.mapSessionToDestination(sessionId, destinationPath)
        sessionToPublicDestinationsMap.mapDestinationToSession(destinationPath, sessionId)
        applicationEventPublisher.publishEvent(wsEvent)
    }

    private fun handlePublicSessionDisconnected(sessionId: String) {
        sessionToPublicDestinationsMap.clearSessionAndReturnDestinations(sessionId).forEach(Consumer { destination ->
            handlePublicDestinationSessionDisconnected(sessionId, destination)
        })
    }

    private fun handlePublicDestinationSessionDisconnected(sessionId: String, destinationPath: String) {
        publicDestinationToSessionsMap.unmapSessionFromDestination(sessionId, destinationPath)

        if (publicDestinationToSessionsMap.allSessionsForDestinationAreClosed(destinationPath)) {
            val wsEvent = WebSocketPathConnectionClosedEvent(this, sessionId, destinationPath)
            applicationEventPublisher.publishEvent(wsEvent)
        }
    }

    private fun handleUserSessionDisconnected(
        sessionId: String,
        userPrincipal: Principal,
    ) {
        val username = userPrincipal.name

        usernameToSessionsMap.unmapSessionFromUser(sessionId, username)
        if (usernameToSessionsMap.allSessionsForUserAreClosed(username)) {
            usernameToUserDestinationsMap.clearUserAndReturnDestinations(username).forEach(Consumer { destination ->
                val wsEvent = WebSocketUserPathConnectionClosedEvent(this, userPrincipal, sessionId, destination)
                applicationEventPublisher.publishEvent(wsEvent)
            })
        }
    }
}
