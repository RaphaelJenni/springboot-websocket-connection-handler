package ch.rjenni.examscheduler.application

import ch.rjenni.examscheduler.helpers.AuthenticationRequestTestFilter
import ch.rjenni.examscheduler.helpers.AuthenticationTokenCreator
import ch.rjenni.examscheduler.helpers.WebSocketSubscriptionTestHelper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AuthenticationRequestTestFilter::class, WebSocketSubscriptionTestHelper::class)
internal class WebSocketMessagingHelperTest {

    @LocalServerPort
    private var port: Int? = null

    @Autowired
    private lateinit var webSocketMessagingHelper: WebSocketMessagingHelper

    @Autowired
    private lateinit var subscriptionTestHelper: WebSocketSubscriptionTestHelper

    private var session1: StompSession? = null
    private var session2: StompSession? = null

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
        val connectedCountDownLatch = CountDownLatch(2)

        val webSocketStompClient1 = WebSocketStompClient(StandardWebSocketClient())
        webSocketStompClient1.messageConverter = StringMessageConverter()
        session1 = webSocketStompClient1
            .connect(String.format("ws://localhost:%d/ws", port), object : StompSessionHandlerAdapter() {
                override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
                    connectedCountDownLatch.countDown()
                    super.afterConnected(session, connectedHeaders)
                }

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
            .get(2, TimeUnit.SECONDS)


        val webSocketStompClient2 = WebSocketStompClient(StandardWebSocketClient())
        webSocketStompClient2.messageConverter = StringMessageConverter()
        session2 = webSocketStompClient2
            .connect(String.format("ws://localhost:%d/ws", port), object : StompSessionHandlerAdapter() {
                override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
                    connectedCountDownLatch.countDown()
                    super.afterConnected(session, connectedHeaders)
                }

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
            .get(2, TimeUnit.SECONDS)

        assertTrue(connectedCountDownLatch.await(2, TimeUnit.SECONDS))
    }

    @AfterEach
    fun tearDown() {
        if (session1?.isConnected == true) {
            session1?.disconnect()
        }
        if (session2?.isConnected == true) {
            session2?.disconnect()
        }
    }

    @Test
    fun convertAndSendToSession_shouldSendToSessionSubscription() {
        // arrange
        val countDownLatch = CountDownLatch(1)
        val destinationPath = "/user/topic/test/1/"

        subscriptionTestHelper.receivedSubscriptionsCountDownLatch = CountDownLatch(1)

        val subscription1 = session1!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {
            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                countDownLatch.countDown()
                super.handleFrame(headers, payload)
            }
        })
        assertTrue(subscriptionTestHelper.receivedSubscriptionsCountDownLatch.await(1, TimeUnit.SECONDS))
        val sessionId = subscriptionTestHelper.subscriptionToIdMap[subscription1.subscriptionId]

        subscriptionTestHelper.receivedSubscriptionsCountDownLatch = CountDownLatch(1)

        session2!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {
            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                throw AssertionError("Session2 should not be triggered!")
            }
        })

        // act
        assertTrue(subscriptionTestHelper.receivedSubscriptionsCountDownLatch.await(1, TimeUnit.SECONDS))
        assertNotNull(sessionId)

        webSocketMessagingHelper.convertAndSendToSession(sessionId!!, destinationPath, "Message")

        // assert
        assertTrue(countDownLatch.await(2, TimeUnit.SECONDS))
    }

    @Test
    fun convertAndSendToSession_shouldThrow_ifSessionIdIsNotValid() {
        // arrange
        val destinationPath = "/user/topic/test/2/"

        // act & assert
        assertThrows<IllegalArgumentException> {
            webSocketMessagingHelper.convertAndSendToSession("Invalid-SessionId", destinationPath, "Message")
        }
    }


    @Test
    fun convertAndSendToUser_shouldSendToAllSessionSubscriptionsOfUser() {
        // arrange
        val messageReceivedCountDownLatch = CountDownLatch(2)
        val destinationPath = "/user/topic/test/2/"

        subscriptionTestHelper.receivedSubscriptionsCountDownLatch = CountDownLatch(2)

        session1!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {
            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                messageReceivedCountDownLatch.countDown()
                super.handleFrame(headers, payload)
            }
        })

        session2!!.subscribe(destinationPath, object : StompSessionHandlerAdapter() {
            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                messageReceivedCountDownLatch.countDown()
                super.handleFrame(headers, payload)
            }
        })

        assertTrue(subscriptionTestHelper.receivedSubscriptionsCountDownLatch.await(1, TimeUnit.SECONDS))

        // act
        webSocketMessagingHelper.convertAndSendToUser(AuthenticationTokenCreator.username, destinationPath, "Message")

        // assert
        assertTrue(messageReceivedCountDownLatch.await(2, TimeUnit.SECONDS))
    }

    @Test
    fun convertAndSendToUser_shouldThrow_ifUsernameIsSessionId() {
        // arrange
        val destinationPath = "/user/topic/test/2/"

        // act & assert
        assertThrows<IllegalArgumentException> {
            webSocketMessagingHelper.convertAndSendToUser(session1!!.sessionId, destinationPath, "Message")
        }
    }

}
