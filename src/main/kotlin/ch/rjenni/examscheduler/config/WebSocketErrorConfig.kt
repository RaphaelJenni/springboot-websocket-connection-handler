package ch.rjenni.examscheduler.config

import ch.rjenni.examscheduler.interfaces.ws.WebSocketErrorHandler
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // interceptor config must be before spring security
class WebSocketErrorConfig : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.setErrorHandler(WebSocketErrorHandler())
    }
}
