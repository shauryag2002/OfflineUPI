package com.offlineupi.domain.model

enum class PaymentState {
    IDLE,
    SCANNING,
    CONFIRMING,
    DIALING,
    COMPLETED,
    ERROR
}
