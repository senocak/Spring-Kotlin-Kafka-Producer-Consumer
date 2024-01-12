package com.github.senocak.domain.dto

import java.util.Date

class TpsInfo {
    var maxTps = 0
    var lastScheduledTime: Long = 0
    var currentProcessedTxnCount = 0
}

enum class KafkaAction(val messageId: String) {
    MAKE_BUCKET("make_bucket"),
    DELETE_BUCKET("delete_bucket"),
}

data class KafkaMessageTemplate(
    val action: KafkaAction,
    val value: String
) {
    var createdTime: Long = Date().time
}