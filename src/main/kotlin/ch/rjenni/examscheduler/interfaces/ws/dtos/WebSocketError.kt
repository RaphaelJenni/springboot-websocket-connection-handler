package ch.rjenni.examscheduler.interfaces.ws.dtos

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

open class WebSocketError(
    val statusCode: Int = 500,
    val status: String = "Internal Server Error",
    val message: String?
) {
    val type: String = "WebSocketError"

    override fun toString(): String {
        return jacksonObjectMapper().writeValueAsString(this)
    }
}
