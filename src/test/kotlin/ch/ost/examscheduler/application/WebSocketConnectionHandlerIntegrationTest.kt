package ch.rjenni.examscheduler.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import ch.rjenni.examscheduler.domain.collections.DestinationToSessionsMap
import ch.rjenni.examscheduler.domain.collections.SessionToDestinationsMap
import ch.rjenni.examscheduler.domain.collections.UsernameToDestinationsMap
import ch.rjenni.examscheduler.domain.collections.UsernameToSessionsMap
import ch.rjenni.examscheduler.domain.events.*
import ch.rjenni.examscheduler.helpers.AuthenticationRequestTestFilter
import ch.rjenni.examscheduler.helpers.AuthenticationRequestTestFilter.Companion.authenticatedStompHeaders
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.internal.verification.VerificationModeFactory.times
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.stereotype.Controller
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AuthenticationRequestTestFilter::class)
internal class WebSocketConnectionHandlerIntegrationTest {

    @LocalServerPort
    private var port: Int? = null

    @SpyBean
    private lateinit var testWebSocketControllerSpy: TestWebSocketController

    @SpyBean
    private lateinit var webSocketStompClientSpy: WebSocketConnectionHandler

    private var session: StompSession? = null

    @TestConfiguration
    @Order(Ordered.HIGHEST_PRECEDENCE + 99)
    class WebSecurityConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http
                .httpBasic().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().anyRequest().permitAll()
        }
    }

    @BeforeEach
    fun setup() {
        val webSocketStompClient = WebSocketStompClient(StandardWebSocketClient())
        val mappingJackson2MessageConverter = MappingJackson2MessageConverter()
        mappingJackson2MessageConverter.objectMapper = ObjectMapper()
            .registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())

        webSocketStompClient.messageConverter = mappingJackson2MessageConverter
        session = webSocketStompClient
            .connect(String.format("ws://localhost:%d/ws", port), object : StompSessionHandlerAdapter() {
                override fun handleException(
                    session: StompSession,
                    command: StompCommand?,
                    headers: StompHeaders,
                    payload: ByteArray,
                    exception: Throwable
                ) {
                    throw RuntimeException("Failure in WebSocket handling", exception)
                }
            })
            .get(1, TimeUnit.SECONDS)
    }

    @AfterEach
    fun tearDownAndPostVerification() {
        val usernameToSessionsMap =
            ReflectionTestUtils.getField(webSocketStompClientSpy, "usernameToSessionsMap") as UsernameToSessionsMap
        val usernameToUserDestinationsMap = ReflectionTestUtils.getField(
            webSocketStompClientSpy,
            "usernameToUserDestinationsMap"
        ) as UsernameToDestinationsMap
        val sessionToPublicDestinationsMap = ReflectionTestUtils.getField(
            webSocketStompClientSpy,
            "sessionToPublicDestinationsMap"
        ) as SessionToDestinationsMap
        val publicDestinationToSessionsMap = ReflectionTestUtils.getField(
            webSocketStompClientSpy,
            "publicDestinationToSessionsMap"
        ) as DestinationToSessionsMap

        if (!usernameToSessionsMap.isEmpty() ||
            !usernameToUserDestinationsMap.isEmpty() ||
            !sessionToPublicDestinationsMap.isEmpty() ||
            !publicDestinationToSessionsMap.isEmpty()
        ) {
            val countDownLatch = CountDownLatch(1)
            doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handlePathConnectionClosed(any())
            doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy)
                .handleUserPathConnectionClosed(any())

            if (session?.isConnected == true) {
                session?.disconnect()
            } else {
                countDownLatch.countDown()
            }

            assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        }

        assertTrue(usernameToSessionsMap.isEmpty(), "Not empty: $usernameToSessionsMap")
        assertTrue(usernameToUserDestinationsMap.isEmpty(), "Not empty: $usernameToUserDestinationsMap")
        assertTrue(sessionToPublicDestinationsMap.isEmpty(), "Not empty: $sessionToPublicDestinationsMap")
        assertTrue(publicDestinationToSessionsMap.isEmpty(), "Not empty: $publicDestinationToSessionsMap")
    }

    @Test
    fun handleNewPathConnection() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handleNewPathConnection(any())

        val destinationPath = "/topic/handleNewPathConnection/1/"

        // act
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handleNewPathConnection(any())
        verify(testWebSocketControllerSpy).handleNewPathConnection(argThat { event -> event.simpPath == destinationPath })
    }

    @Test
    fun handleNewUserPathConnection() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handleNewUserPathConnection(any())

        val destinationPath = "/user/topic/handleNewUserPathConnection/1/"
        val stompHeaders = authenticatedStompHeaders()
        stompHeaders.destination = destinationPath

        // act
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handleNewUserPathConnection(any())
        verify(testWebSocketControllerSpy).handleNewUserPathConnection(argThat { event -> event.simpPath == destinationPath })
    }

    @Test
    fun handleExistingPathConnection() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handleExistingPathConnection(any())

        val destinationPath = "/topic/handleExistingPathConnection/1/"
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})

        // act
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handleExistingPathConnection(any())
        verify(testWebSocketControllerSpy).handleNewPathConnection(argThat { event -> event.simpPath == destinationPath })
    }

    @Test
    fun handleExistingUserPathConnection() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy)
            .handleExistingUserPathConnection(any())

        val destinationPath = "/user/topic/handleExistingUserPathConnection/1/"
        val stompHeaders = authenticatedStompHeaders()
        stompHeaders.destination = destinationPath
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})

        // act
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handleExistingUserPathConnection(any())
        verify(testWebSocketControllerSpy).handleExistingUserPathConnection(argThat { event -> event.simpPath == destinationPath })
    }


    @Test
    fun handlePathConnectionClosed_dueTo_unsubscribe() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handlePathConnectionClosed(any())

        val destinationPath = "/topic/handlePathConnectionClosed_dueTo_unsubscribe/1/"
        val subscription = session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})

        // act
        subscription.unsubscribe()

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handlePathConnectionClosed(any())
        verify(testWebSocketControllerSpy).handlePathConnectionClosed(argThat { event -> event.simpPath == destinationPath })
    }

    @Test
    fun handleUserPathConnectionClosed_dueTo_unsubscribe() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handleUserPathConnectionClosed(any())

        val destinationPath = "/user/topic/handleUserPathConnectionClosed_dueTo_unsubscribe/1/"
        val stompHeaders = authenticatedStompHeaders()
        stompHeaders.destination = destinationPath
        val subscription = session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})

        // act
        subscription.unsubscribe()

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handleUserPathConnectionClosed(any())
        verify(testWebSocketControllerSpy).handleUserPathConnectionClosed(argThat { event -> event.simpPath == destinationPath })
    }

    @Test
    fun handlePathConnectionClosed_dueTo_disconnect_single() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handlePathConnectionClosed(any())

        val destinationPath = "/topic/handlePathConnectionClosed_dueTo_disconnect_single/1/"
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})

        // act
        session!!.disconnect()

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handlePathConnectionClosed(any())
        verify(testWebSocketControllerSpy).handlePathConnectionClosed(argThat { event -> event.simpPath == destinationPath })
    }

    @Test
    fun handlePathConnectionClosed_dueTo_disconnect_multiple() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handlePathConnectionClosed(any())

        val destinationPath = "/topic/handlePathConnectionClosed_dueTo_disconnect_multiple/1/"
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {})

        // act
        session!!.disconnect()

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handlePathConnectionClosed(any())
        verify(testWebSocketControllerSpy).handlePathConnectionClosed(argThat { event -> event.simpPath == destinationPath })
    }

    @Test
    fun handleUserPathConnectionClosed_dueTo_disconnect_single() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handleUserPathConnectionClosed(any())

        val destinationPath = "/user/topic/handleUserPathConnectionClosed_dueTo_disconnect_single/1/"
        val stompHeaders = authenticatedStompHeaders()
        stompHeaders.destination = destinationPath
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})

        // act
        session!!.disconnect(stompHeaders)

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handleUserPathConnectionClosed(any())
        verify(testWebSocketControllerSpy).handleUserPathConnectionClosed(argThat { event -> event.simpPath == destinationPath })
    }

    @Test
    fun handleUserPathConnectionClosed_dueTo_disconnect_multiple() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        doAnswer { countDownLatch.countDown() }.`when`(testWebSocketControllerSpy).handleUserPathConnectionClosed(any())

        val destinationPath = "/user/topic/handleUserPathConnectionClosed_dueTo_disconnect_multiple/1/"
        val stompHeaders = authenticatedStompHeaders()
        stompHeaders.destination = destinationPath
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})
        session!!.subscribe(stompHeaders, object : StompSessionHandlerAdapter() {})

        // act
        session!!.disconnect(stompHeaders)

        // assert
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        verify(testWebSocketControllerSpy, times(1)).handleUserPathConnectionClosed(any())
        verify(testWebSocketControllerSpy).handleUserPathConnectionClosed(argThat { event -> event.simpPath == destinationPath })
    }

    @Controller
    class TestWebSocketController {
        // Public endpoints
        @EventListener
        fun handleNewPathConnection(event: WebSocketNewPathConnectionEvent) {
        }

        @EventListener
        fun handleExistingPathConnection(event: WebSocketExistingPathConnectionEvent) {
        }

        @EventListener
        fun handlePathConnectionClosed(event: WebSocketPathConnectionClosedEvent) {
        }

        // User endpoints
        @EventListener
        fun handleNewUserPathConnection(eventUser: WebSocketNewUserPathConnectionEvent) {
        }

        @EventListener
        fun handleExistingUserPathConnection(event: WebSocketExistingUserPathConnectionEvent) {
        }

        @EventListener
        fun handleUserPathConnectionClosed(event: WebSocketUserPathConnectionClosedEvent) {
        }
    }


}
