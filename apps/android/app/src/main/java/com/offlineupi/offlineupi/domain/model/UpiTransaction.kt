package com.offlineupi.domain.model

data class UpiTransaction(
    val vpa: String,
    val amount: String?,
    val note: String?,
    val payeeName: String?
)
