package ch.rjenni.examscheduler.domain.events

open class WebSocketPathConnectionClosedEvent(source: Any, sessionId: String, simpPath: String) :
    WebSocketPathConnectionEvent(source, sessionId, simpPath)
