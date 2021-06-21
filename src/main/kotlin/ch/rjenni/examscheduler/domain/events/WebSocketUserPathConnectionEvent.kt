package ch.rjenni.examscheduler.domain.events

import java.security.Principal

abstract class WebSocketUserPathConnectionEvent(
    source: Any,
    val userPrincipal: Principal,
    sessionId: String,
    simpPath: String,
) : WebSocketPathConnectionEvent(source, sessionId, simpPath)
