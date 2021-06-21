package ch.rjenni.examscheduler.domain.events

open class WebSocketExistingPathConnectionEvent(source: Any, sessionId: String, simpPath: String) :
    WebSocketPathConnectionEvent(source, sessionId, simpPath)
