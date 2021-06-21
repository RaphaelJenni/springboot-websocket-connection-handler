package ch.rjenni.examscheduler.helpers

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

object AuthenticationTokenCreator {
    const val username = "TestUser"

    fun create(): AbstractAuthenticationToken {
        val grantedAuthorities = listOf<GrantedAuthority>()

        return UsernamePasswordAuthenticationToken(
            User
                .withUsername(username)
                .password("testPassword")
                .authorities(grantedAuthorities)
                .build(),
            null,
            grantedAuthorities
        )
    }
}
