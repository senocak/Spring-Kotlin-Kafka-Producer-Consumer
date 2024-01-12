package com.github.senocak.config

import com.github.senocak.security.JwtAuthenticationEntryPoint
import com.github.senocak.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val unauthorizedHandler: JwtAuthenticationEntryPoint,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    @Bean
    fun securityFilterChainDSL(http: HttpSecurity): SecurityFilterChain {
        http {
            cors {}
            csrf { disable() }
            exceptionHandling { authenticationEntryPoint = unauthorizedHandler }
            authorizeRequests {
                authorize("/api/v1/auth/**", permitAll)
                authorize("/api/v1/swagger/**", permitAll)
                authorize("/actuator**/**", permitAll)
                authorize("/swagger**/**", permitAll)
                //authorize(PathRequest.toH2Console(), permitAll)
                authorize(matches = anyRequest, access = authenticated)
            }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            headers { frameOptions { disable() } }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(filter = jwtAuthenticationFilter)
        }
        return http.build()
    }
}
