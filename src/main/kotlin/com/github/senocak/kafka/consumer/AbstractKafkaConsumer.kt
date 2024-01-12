package com.github.senocak.kafka.consumer

import com.github.senocak.domain.dto.TpsInfo
import com.github.senocak.util.logger
import java.util.Properties
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value

abstract class AbstractKafkaConsumer : IKafkaClient {
    protected val log: Logger by logger()
    protected var messageExecutor: ExecutorService? = Executors.newFixedThreadPool(N_THREADS)
    private var msgConsumerList: MutableList<KafkaMsgConsumer> = ArrayList<KafkaMsgConsumer>()
    private lateinit var consumer: KafkaConsumer<String, String>

    override val tpsInfo: TpsInfo = TpsInfo()

    @Value("\${spring.kafka.bootstrap-servers}")
    lateinit var bootstrapServers: String

    @Value("\${spring.application.name}")
    lateinit var applicationName: String

    override fun init() {
        tpsInfo.maxTps = maxTps
        try {
            if (messageExecutor == null || messageExecutor!!.isShutdown || messageExecutor!!.isTerminated) {
                messageExecutor = Executors.newFixedThreadPool(N_THREADS)
            }
            log.info("Consumer Topic: $topicName starting")
            for (threadNumber: Int in 1 until consumerThreadCount + 1) {
                val properties: Properties = Properties()
                    .apply {
                        this[ConsumerConfig.GROUP_ID_CONFIG] = applicationName
                        this[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
                        this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
                        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
                        this[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = "$commitInterval"
                        this[ConsumerConfig.FETCH_MAX_BYTES_CONFIG] = "$fetchMessageMaxBytes"
                        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
                        this[ConsumerConfig.GROUP_INSTANCE_ID_CONFIG] = "SchedulerCoordinator${UUID.randomUUID()}"
                        this[ConsumerConfig.CLIENT_ID_CONFIG] = "$applicationName-$topicName-$threadNumber"
                    }
                    .apply { log.info("Create Consumer Config >> Properties object:$this") }

                consumer = KafkaConsumer(properties)
                consumer.subscribe(listOf(element = topicName))

                val messageConsumer = KafkaMsgConsumer(consumer = consumer, threadNumber = threadNumber, owner = this, applicationName = applicationName)

                "${javaClass.simpleName}-$topicName-$threadNumber"
                    .apply { Thread(messageConsumer::run, this).start() }
                    .apply { log.info("Consumer started for topic name:$topicName with thread: $this") }
                msgConsumerList.add(element = messageConsumer)
            }
        } catch (e: Exception) {
            log.error("Exception occurred while preparing and starting Kafka consumer. Ex: ${e.message}")
        }
    }

    override fun destroy() {
        log.info("Shutting down: ${javaClass.simpleName}")
        msgConsumerList.forEach(KafkaMsgConsumer::stop)
        msgConsumerList.clear()
        messageExecutor?.shutdown()
        try {
            //Max time=15 min for wait of termination of executor
            log.info("Waiting for termination of executor for max:$maxTimeOfTermination second. Will close earlier if all jobs was completed.",)
            messageExecutor?.awaitTermination(maxTimeOfTermination, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            log.error("Interrupted Exception was occurred while awaiting termination of stopping MessageExecutor in Message Consumer. Ex:${e.message}")
            Thread.currentThread().interrupt()
        }
        messageExecutor = null
        log.info("Stopping finished ${javaClass.simpleName}")
    }
    abstract val topicName: String
    protected open val consumerThreadCount: Int = 2
    protected open val maxTps: Int = 1000
    protected open val maxTimeOfTermination: Long = 900
    protected open val commitInterval: Int = 100
    protected open val fetchMessageMaxBytes: Long = 10485760
    //protected open val sessionTimeout: String = "5000"
    //protected open val syncTime: String = "2000"

    companion object {
        private const val N_THREADS = 10
    }
}
