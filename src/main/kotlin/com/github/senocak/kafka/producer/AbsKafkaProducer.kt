package com.github.senocak.kafka.producer

import com.github.senocak.util.logger
import com.google.common.util.concurrent.JdkFutureAdapters
import com.google.common.util.concurrent.ListenableFuture
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value

abstract class AbsKafkaProducer<K, V> {
    private val log: Logger by logger()
    private lateinit var producer: KafkaProducer<K, V>

    @Value("\${spring.application.name}")
    var applicationName: K? = null

    protected fun init() {
        log.info("initializing KafkaProducer: Topic Name: $topic")
        Properties()
            .also { it: Properties ->
                it[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokerList
                it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = keySerializer
                it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = valueSerializer
                it[ProducerConfig.ACKS_CONFIG] = "1"
                it[ProducerConfig.RETRIES_CONFIG] = "3"
                it[ProducerConfig.LINGER_MS_CONFIG] = 5
            }
            .apply { producer = KafkaProducer(this) }
            log.info("initialized KafkaProducer: Topic Name: $topic")
    }

    protected fun destroy() {
        log.info("Closing Kafka Producer for topic: $topic")
        producer.close()
        log.info("Closing Kafka Producer for topic: $topic finished")
    }

    fun produce(msg: V) {
        val producerRecord = ProducerRecord<K, V>(topic, applicationName, msg)
        producerRecord.headers().add(RecordHeader("from", applicationName.toString().toByteArray()))
        val future: Future<RecordMetadata> = producer.send(producerRecord)
        val listenable: ListenableFuture<RecordMetadata> = JdkFutureAdapters.listenInPoolThread(future)
        val runnable = object: Runnable {
            override fun run () {
                log.info("Kafka message produced. message: $producerRecord")
            }
        }
        listenable.addListener(runnable, Executors.newSingleThreadExecutor())
        future.get()
        producer.flush()
        /*
        val callback = object: Callback {
            override fun onCompletion(metadata: RecordMetadata?, exception: Exception?){
                log.info("Kafka message produced. message: $producerRecord, metadata: $metadata, exception: $exception")
            }
        }
        producer.send(producerRecord, callback)
        */
    }

    protected open val topic: String
        get() = "default-topic"
    protected open val valueSerializer: Class<StringSerializer> = StringSerializer::class.java
    protected open val keySerializer: Class<StringSerializer> = StringSerializer::class.java
    //"org.apache.kafka.common.serialization.StringSerializer"
    protected abstract val brokerList: String
}
