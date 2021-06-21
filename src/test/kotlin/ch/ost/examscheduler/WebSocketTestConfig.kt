package ch.rjenni.examscheduler

import ch.rjenni.examscheduler.interfaces.ws.WebSocketErrorHandler
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry


@Configuration
@EnableWebSocketMessageBroker
class WebSocketTestConfig : AbstractSecurityWebSocketMessageBrokerConfigurer() {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic", "/queue")
        config.setApplicationDestinationPrefixes("/app")
        config.setUserDestinationPrefix("/user")

    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*")
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS()
        registry.setErrorHandler(WebSocketErrorHandler())
    }

    override fun configureInbound(messages: MessageSecurityMetadataSourceRegistry) {
        messages.anyMessage().permitAll()
    }

    override fun sameOriginDisabled(): Boolean {
        return true
    }
}
