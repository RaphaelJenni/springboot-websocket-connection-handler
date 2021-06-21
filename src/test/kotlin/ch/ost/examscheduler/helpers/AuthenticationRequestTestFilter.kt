package ch.rjenni.examscheduler.helpers

import org.springframework.boot.test.context.TestComponent
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@TestComponent
class AuthenticationRequestTestFilter : OncePerRequestFilter() {
    companion object {
        private const val TEST_AUTHENTICATION_HEADER = "testAuthenticate"

        fun authenticatedStompHeaders(): StompHeaders {
            val stompHeaders = StompHeaders()
            stompHeaders.add(TEST_AUTHENTICATION_HEADER, "TRUE")
            return stompHeaders
        }
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        setAuthenticationForContext(request)
        chain.doFilter(request, response)
    }

    private fun setAuthenticationForContext(request: HttpServletRequest) {
        val abstractAuthenticationToken = AuthenticationTokenCreator.create()

        abstractAuthenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = abstractAuthenticationToken
    }
}
