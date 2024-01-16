package com.github.senocak.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.senocak.domain.dto.KafkaAction
import com.github.senocak.domain.dto.KafkaMessageTemplate
import com.github.senocak.kafka.producer.BucketCreateRequestKafkaProducer
import com.github.senocak.repository.RoleRepository
import com.github.senocak.repository.UserRepository
import com.github.senocak.util.RoleName
import com.github.senocak.util.logger
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class Listener(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val bucketCreateRequestKafkaProducer: BucketCreateRequestKafkaProducer
){
    private val log: Logger by logger()
    private val jacksonObjectMapper: ObjectMapper = jacksonObjectMapper()

    @Value("\${spring.jpa.hibernate.ddl-auto}")
    lateinit var ddl: String

    @EventListener(ApplicationReadyEvent::class)
    fun applicationReadyEvent(event: ApplicationReadyEvent) {
        log.info("Time: ${event.timeTaken.seconds}")
        if (ddl == "create" || ddl == "create-drop") {
            roleRepository.deleteAll()
            userRepository.deleteAll()

            val userRole: Role = roleRepository.save(Role().also { it.name = RoleName.ROLE_USER })
            val adminRole: Role = roleRepository.save(Role().also { it.name = RoleName.ROLE_ADMIN })
            val defaultPass: String = passwordEncoder.encode("asenocak")
            val user1: User = userRepository.save(User().also { it.name = "Anıl Şenocak"; it.email = "anil@senocak.com"; it.password = defaultPass; it.roles = arrayListOf(userRole, adminRole)})

            if (bucketCreateRequestKafkaProducer.running) {
                KafkaMessageTemplate(action = KafkaAction.MAKE_BUCKET, value = "${user1.id}")
                    .run { bucketCreateRequestKafkaProducer.produce(msg = jacksonObjectMapper.writeValueAsString(this)) }
            }
            log.info("[ApplicationReadyEvent]: db migrated.")
        }
    }
}
