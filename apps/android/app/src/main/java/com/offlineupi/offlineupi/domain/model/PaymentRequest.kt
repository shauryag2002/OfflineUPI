package com.offlineupi.domain.model

data class PaymentRequest(
    val vpa: String,
    val amount: String,
    val note: String?
)
