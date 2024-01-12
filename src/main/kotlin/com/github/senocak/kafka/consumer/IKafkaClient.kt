package com.github.senocak.kafka.consumer

import com.github.senocak.domain.dto.TpsInfo

interface IKafkaClient {
    fun init()
    fun destroy()
    fun processMessage(message: String)
    val tpsInfo: TpsInfo
}
