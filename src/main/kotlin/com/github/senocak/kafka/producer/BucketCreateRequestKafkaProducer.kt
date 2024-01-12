package com.github.senocak.kafka.producer

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Service

@Service
//@Scope(value = "prototype")
class BucketCreateRequestKafkaProducer: AbsKafkaProducer<String, String>(), SmartLifecycle {
    var running: Boolean = false

    @Value("\${spring.kafka.producer.topic.bucket-create}")
    override lateinit var topic: String

    @Value("\${spring.kafka.bootstrap-servers}")
    override lateinit var brokerList: String

    override fun start(): Unit = super.init().also { running = true }
    override fun stop(): Unit = super.destroy().also { running = false }
    override fun isRunning(): Boolean = running
}
