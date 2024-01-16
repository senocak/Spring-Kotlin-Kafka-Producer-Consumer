package com.github.senocak.domain

import com.github.senocak.util.logger
import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class UserListener(
    //var passwordEncoder: PasswordEncoder? = null
){

    private val log: Logger by logger()

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @PrePersist
    fun prePersist(user: User) {
        log.info("passwordEncoder: ${passwordEncoder.encode("asenocak")}")
        log.info("prePersist: $user")
    }

    @PostPersist
    @PostUpdate
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun afterPersist(user: User) {
        log.info("afterPersist: $user")
    }

    @PreUpdate
    fun preUpdate(user: User) {
        log.info("preUpdate: $user")
    }
}
