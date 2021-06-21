package ch.rjenni.examscheduler.domain.events

open class WebSocketNewPathConnectionEvent(source: Any, sessionId: String, simpPath: String) :
    WebSocketPathConnectionEvent(source, sessionId, simpPath)
