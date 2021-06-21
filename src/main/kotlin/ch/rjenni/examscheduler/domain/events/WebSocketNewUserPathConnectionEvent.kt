package ch.rjenni.examscheduler.domain.events

import java.security.Principal

class WebSocketNewUserPathConnectionEvent(
    source: Any,
    userPrincipal: Principal,
    sessionId: String,
    simpPath: String,
) : WebSocketUserPathConnectionEvent(source, userPrincipal, sessionId, simpPath)
