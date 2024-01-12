package com.github.senocak.kafka.consumer

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.senocak.domain.dto.KafkaMessageTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Service

@Service
class BucketCreateResponseConsumer: AbstractKafkaConsumer(), SmartLifecycle {
    private var running: Boolean = false
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Value("\${spring.kafka.consumer.topic.bucket-create}")
    override lateinit var topicName: String
    override val consumerThreadCount: Int = 1

    override fun start(): Unit = super.init().also { running = true }
    override fun stop(): Unit = super.destroy().run { running = false }
    override fun isRunning(): Boolean = running

    override fun processMessage(message: String) {
        // messageExecutor?.execute {}
        try {
            val kafkaMessageTemplate: KafkaMessageTemplate = objectMapper.readValue(message, KafkaMessageTemplate::class.java)
            log.info("Consumed message from Topic: $topicName. Message: $kafkaMessageTemplate, Thread:${Thread.currentThread().name}")
        } catch (jpe: JsonParseException) {
            log.error("JsonParseException while consuming message: $message, Ex: ${jpe.message}")
        }
    }
}
