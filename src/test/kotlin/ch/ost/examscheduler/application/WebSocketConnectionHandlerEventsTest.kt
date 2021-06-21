package ch.rjenni.examscheduler.application

import ch.rjenni.examscheduler.application.exceptions.UnauthorizedException
import ch.rjenni.examscheduler.domain.events.WebSocketNewPathConnectionEvent
import ch.rjenni.examscheduler.domain.events.WebSocketNewUserPathConnectionEvent
import ch.rjenni.examscheduler.domain.events.WebSocketPathConnectionClosedEvent
import ch.rjenni.examscheduler.domain.events.WebSocketUserPathConnectionClosedEvent
import ch.rjenni.examscheduler.helpers.AuthenticationTokenCreator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent


@RecordApplicationEvents
@SpringBootTest
internal class WebSocketConnectionHandlerEventsTest {

    @Autowired
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private lateinit var applicationEvents: ApplicationEvents

    @Autowired
    private lateinit var webSocketConnectionHandler: WebSocketConnectionHandler

    @TestConfiguration
    class WebSecurityConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http
                .httpBasic().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().anyRequest().permitAll()
        }
    }


    @Test
    fun handleSessionSubscribed_shouldTrigger_WebSocketNewPathConnectionEvent_ifNotAuthenticated_forPublicUrl() {
        // arrange
        val destinationPath = "/topic/anything"
        val sessionId = "123456-123-123-123"

        val headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        headers.destination = destinationPath
        headers.sessionId = sessionId

        // act
        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    headers.messageHeaders
                )
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketNewPathConnectionEvent::class.java)
            .filter { event: WebSocketNewPathConnectionEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }

    @Test
    fun handleSessionSubscribed_shouldTrigger_WebSocketNewPathConnectionEvent_ifAuthenticated_forPublicUrl() {
        // arrange
        val destinationPath = "/topic/anything"
        val sessionId = "123456-123-123-123"
        val principal = AuthenticationTokenCreator.create()

        val headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        headers.destination = destinationPath
        headers.sessionId = sessionId
        headers.user = principal

        // act
        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    headers.messageHeaders
                ),
                principal
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketNewPathConnectionEvent::class.java)
            .filter { event: WebSocketNewPathConnectionEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }

    @Test
    fun handleSessionSubscribed_shouldThrow_ifNotAuthenticated_forUserUrl() {
        // arrange
        val destinationPath = "/user/topic/anything"
        val sessionId = "123456-123-123-123"

        val headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        headers.destination = destinationPath
        headers.sessionId = sessionId

        // act & assert
        assertThrows<UnauthorizedException> {
            webSocketConnectionHandler.handleSessionSubscribed(
                SessionSubscribeEvent(
                    this, MessageBuilder.createMessage(
                        ByteArray(0),
                        headers.messageHeaders
                    )
                )
            )
        }

        val correctEvents = applicationEvents
            .stream(WebSocketNewUserPathConnectionEvent::class.java)
            .filter { eventUser: WebSocketNewUserPathConnectionEvent -> eventUser.simpPath == destinationPath }
            .count()

        assertEquals(0, correctEvents)
    }

    @Test
    fun handleSessionSubscribed_shouldTrigger_WebSocketNewUserPathConnectionEvent_ifAuthenticated_forUserUrl() {
        // arrange
        val destinationPath = "/user/topic/anything"
        val sessionId = "123456-123-123-123"
        val principal = AuthenticationTokenCreator.create()

        val headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        headers.destination = destinationPath
        headers.sessionId = sessionId
        headers.user = principal

        // act
        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    headers.messageHeaders
                ),
                principal
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketNewUserPathConnectionEvent::class.java)
            .filter { eventUser: WebSocketNewUserPathConnectionEvent -> eventUser.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }


    @Test
    fun handleSessionUnsubscribed_shouldTrigger_WebSocketPathConnectionClosedEvent_ifNotAuthenticated_forPublicUrl() {
        // arrange
        val destinationPath = "/topic/anything"
        val sessionId = "123456-123-123-123"

        val subscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders.destination = destinationPath
        subscriptionHeaders.sessionId = sessionId

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders.messageHeaders
                )
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId

        // act
        webSocketConnectionHandler.handleSessionUnsubscribed(
            SessionUnsubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                )
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketPathConnectionClosedEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }

    @Test
    fun handleSessionUnsubscribed_shouldTrigger_WebSocketPathConnectionClosedEvent_ifAuthenticated_forPublicUrl() {
        // arrange
        val destinationPath = "/topic/anything"
        val sessionId = "123456-123-123-123"
        val principal = AuthenticationTokenCreator.create()

        val subscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders.destination = destinationPath
        subscriptionHeaders.sessionId = sessionId
        subscriptionHeaders.user = principal

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders.messageHeaders
                ),
                principal
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId
        unsubscriptionHeaders.user = principal

        // act
        webSocketConnectionHandler.handleSessionUnsubscribed(
            SessionUnsubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                ),
                principal
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketPathConnectionClosedEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }


    @Test
    fun handleSessionUnsubscribed_shouldNotTrigger_WebSocketUserPathConnectionClosedEvent_ifNotAuthenticated_forUserUrl() {
        // arrange
        val destinationPath = "/user/topic/anything"
        val sessionId = "123456-123-123-123"
        val principal = AuthenticationTokenCreator.create()

        val subscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders.destination = destinationPath
        subscriptionHeaders.sessionId = sessionId
        subscriptionHeaders.user = principal

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders.messageHeaders
                ),
                principal
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId

        // act
        webSocketConnectionHandler.handleSessionUnsubscribed(
            SessionUnsubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                )
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketUserPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketUserPathConnectionClosedEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(0, correctEvents)
    }

    @Test
    fun handleSessionUnsubscribed_shouldTrigger_WebSocketUserPathConnectionClosedEvent_ifAuthenticated_forUserUrl() {
        // arrange
        val destinationPath = "/user/topic/anything"
        val sessionId = "123456-123-123-123"
        val principal = AuthenticationTokenCreator.create()

        val subscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders.destination = destinationPath
        subscriptionHeaders.sessionId = sessionId
        subscriptionHeaders.user = principal

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders.messageHeaders
                ),
                principal
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId
        unsubscriptionHeaders.user = principal

        // act
        webSocketConnectionHandler.handleSessionUnsubscribed(
            SessionUnsubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                ),
                principal
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketUserPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketUserPathConnectionClosedEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }

    @Test
    fun handleSessionDisconnected_shouldTrigger_WebSocketPathConnectionClosedEvent_ifNotAuthenticated_forPublicUrl() {
        // arrange
        val destinationPath = "/topic/anything"
        val sessionId = "123456-123-123-123"

        val subscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders.destination = destinationPath
        subscriptionHeaders.sessionId = sessionId

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders.messageHeaders
                )
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId

        // act
        webSocketConnectionHandler.handleSessionDisconnected(
            SessionDisconnectEvent(
                this,
                MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                ),
                sessionId,
                CloseStatus.NORMAL
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketPathConnectionClosedEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }

    @Test
    fun handleSessionDisconnected_shouldTrigger_WebSocketPathConnectionClosedEvent_ifAuthenticated_forPublicUrl() {
        // arrange
        val destinationPath = "/topic/anything"
        val sessionId = "123456-123-123-123"
        val principal = AuthenticationTokenCreator.create()

        val subscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders.destination = destinationPath
        subscriptionHeaders.sessionId = sessionId
        subscriptionHeaders.user = principal

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders.messageHeaders
                ),
                principal
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId
        unsubscriptionHeaders.user = principal

        // act
        webSocketConnectionHandler.handleSessionDisconnected(
            SessionDisconnectEvent(
                this,
                MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                ),
                sessionId,
                CloseStatus.NORMAL,
                principal
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketPathConnectionClosedEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }


    @Test
    fun handleSessionDisconnected_shouldNotTrigger_WebSocketUserPathConnectionClosedEvent_ifNotAuthenticated_forUserUrl() {
        // arrange
        val destinationPath = "/user/topic/anything"
        val sessionId = "123456-123-123-123"
        val principal = AuthenticationTokenCreator.create()

        val subscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders.destination = destinationPath
        subscriptionHeaders.sessionId = sessionId
        subscriptionHeaders.user = principal

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders.messageHeaders
                ),
                principal
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId

        // act
        webSocketConnectionHandler.handleSessionDisconnected(
            SessionDisconnectEvent(
                this,
                MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                ),
                sessionId,
                CloseStatus.NORMAL
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketUserPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketUserPathConnectionClosedEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(0, correctEvents)
    }

    @Test
    fun handleSessionDisconnected_shouldTrigger_WebSocketUserPathConnectionClosedEvent_ifAuthenticated_forUserUrl() {
        // arrange
        val destinationPath = "/user/topic/anything"
        val sessionId = "123456-123-123-123"
        val principal = AuthenticationTokenCreator.create()

        val subscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders.destination = destinationPath
        subscriptionHeaders.sessionId = sessionId
        subscriptionHeaders.user = principal

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders.messageHeaders
                ),
                principal
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId
        unsubscriptionHeaders.user = principal

        // act
        webSocketConnectionHandler.handleSessionDisconnected(
            SessionDisconnectEvent(
                this,
                MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                ),
                sessionId,
                CloseStatus.NORMAL,
                principal
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketUserPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketUserPathConnectionClosedEvent -> event.simpPath == destinationPath }
            .count()

        // assert
        assertEquals(1, correctEvents)
    }


    @Test
    fun handleSessionDisconnected_shouldTrigger_WebSocketPathConnectionClosedEvent_3Times_ifNotAuthenticated_for3PublicUrl() {
        // arrange
        val destinationPath1 = "/topic/anything1"
        val destinationPath2 = "/topic/anything2"
        val destinationPath3 = "/topic/anything3"
        val destinations = setOf(destinationPath1, destinationPath2, destinationPath3)
        val sessionId = "123456-123-123-123"

        val subscriptionHeaders1 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders1.destination = destinationPath1
        subscriptionHeaders1.sessionId = sessionId

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders1.messageHeaders
                )
            )
        )

        val subscriptionHeaders2 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders2.destination = destinationPath2
        subscriptionHeaders2.sessionId = sessionId

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders2.messageHeaders
                )
            )
        )

        val subscriptionHeaders3 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        subscriptionHeaders3.destination = destinationPath3
        subscriptionHeaders3.sessionId = sessionId

        webSocketConnectionHandler.handleSessionSubscribed(
            SessionSubscribeEvent(
                this, MessageBuilder.createMessage(
                    ByteArray(0),
                    subscriptionHeaders3.messageHeaders
                )
            )
        )

        val unsubscriptionHeaders = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        unsubscriptionHeaders.sessionId = sessionId

        // act
        webSocketConnectionHandler.handleSessionDisconnected(
            SessionDisconnectEvent(
                this,
                MessageBuilder.createMessage(
                    ByteArray(0),
                    unsubscriptionHeaders.messageHeaders
                ),
                sessionId,
                CloseStatus.NORMAL
            )
        )

        val correctEvents = applicationEvents
            .stream(WebSocketPathConnectionClosedEvent::class.java)
            .filter { event: WebSocketPathConnectionClosedEvent -> destinations.contains(event.simpPath) }
            .count()

        // assert
        assertEquals(3, correctEvents)
    }
}
