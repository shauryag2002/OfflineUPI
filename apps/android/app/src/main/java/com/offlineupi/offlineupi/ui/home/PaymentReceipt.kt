package com.offlineupi.ui.home

data class PaymentReceipt(
    val amount: String,
    val payeeLabel: String,
    val vpaOrMobile: String,
    val note: String,
    val timestamp: String,
    val referenceId: String,
    val channel: String
)
