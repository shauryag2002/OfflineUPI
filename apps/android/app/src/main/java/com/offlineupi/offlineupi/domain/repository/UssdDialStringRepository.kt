package com.offlineupi.domain.repository

import com.offlineupi.domain.model.PaymentRequest

interface UssdDialStringRepository {
    fun buildDialString(request: PaymentRequest): String
}
