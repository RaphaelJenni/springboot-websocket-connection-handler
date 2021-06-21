package ch.rjenni.examscheduler.helpers

import org.springframework.boot.test.context.TestComponent
import org.springframework.context.event.EventListener
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

@TestComponent
class WebSocketSubscriptionTestHelper {
    val subscriptionToIdMap = ConcurrentHashMap<String, String>()
    var receivedSubscriptionsCountDownLatch = CountDownLatch(1)

    @EventListener
    fun handleSessionSubscribed(event: SessionSubscribeEvent) {
        subscriptionToIdMap[event.message.headers["simpSubscriptionId"] as String] =
            event.message.headers["simpSessionId"] as String
        receivedSubscriptionsCountDownLatch.countDown()
    }
}
