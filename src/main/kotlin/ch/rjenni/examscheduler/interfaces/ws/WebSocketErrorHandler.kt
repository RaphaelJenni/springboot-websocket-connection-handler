package ch.rjenni.examscheduler.interfaces.ws

import ch.rjenni.examscheduler.interfaces.ws.dtos.AuthenticationTokenInvalidWebSocketError
import ch.rjenni.examscheduler.interfaces.ws.dtos.WebSocketError
import org.springframework.messaging.Message
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler

class WebSocketErrorHandler : StompSubProtocolErrorHandler() {
    override fun handleInternal(
        errorHeaderAccessor: StompHeaderAccessor,
        errorPayload: ByteArray,
        cause: Throwable?,
        clientHeaderAccessor: StompHeaderAccessor?
    ): Message<ByteArray> {
        when (cause) {
            is MessageDeliveryException -> {
                if (cause.cause is AccessDeniedException) {
                    errorHeaderAccessor.message = AuthenticationTokenInvalidWebSocketError().toString()
                }
            }
            else -> {
                errorHeaderAccessor.message = WebSocketError(message = errorHeaderAccessor.message).toString()
            }
        }
        return MessageBuilder.createMessage(errorPayload, errorHeaderAccessor.messageHeaders)
    }
}
