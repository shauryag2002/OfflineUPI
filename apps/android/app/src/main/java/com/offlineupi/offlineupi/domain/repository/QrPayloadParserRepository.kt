package com.offlineupi.domain.repository

import com.offlineupi.domain.model.UpiTransaction

interface QrPayloadParserRepository {
    fun parseQrPayload(rawPayload: String): UpiTransaction?
}
