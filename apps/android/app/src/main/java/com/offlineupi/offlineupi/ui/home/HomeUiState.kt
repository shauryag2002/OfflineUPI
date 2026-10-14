package com.offlineupi.ui.home

import com.offlineupi.domain.model.PaymentState
import com.offlineupi.domain.model.UpiTransaction

data class HomeUiState(
    val paymentState: PaymentState = PaymentState.IDLE,
    val statusMessage: String = "Ready for offline UPI payment",
    val parsedTransaction: UpiTransaction? = null,
    val errorMessage: String? = null
)
