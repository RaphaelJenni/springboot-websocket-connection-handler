package ch.rjenni.examscheduler.application

import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class WebSocketMessagingHelper(
    private val messagingTemplate: SimpMessagingTemplate
) {
    companion object {
        private val SESSION_ID_REGEX = Regex("^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}\$")
    }

    fun convertAndSendToSession(sessionId: String, destination: String, payload: Any) {
        val sanitizedDestination = destination.removePrefix(messagingTemplate.userDestinationPrefix)

        if (!sessionId.matches(SESSION_ID_REGEX)) throw IllegalArgumentException("Provided session id is not valid: $sessionId")

        val headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE)
        headerAccessor.sessionId = sessionId
        headerAccessor.setLeaveMutable(true)

        messagingTemplate.convertAndSendToUser(
            sessionId,
            sanitizedDestination,
            payload,
            headerAccessor.messageHeaders
        )
    }

    fun convertAndSendToUser(username: String, destination: String, payload: Any) {
        val sanitizedDestination = destination.removePrefix(messagingTemplate.userDestinationPrefix)

        if (username.matches(SESSION_ID_REGEX)) throw IllegalArgumentException("Provided username is a session id: $username - use convertAndSendToSession")


        messagingTemplate.convertAndSendToUser(
            username,
            sanitizedDestination,
            payload
        )
    }


}
