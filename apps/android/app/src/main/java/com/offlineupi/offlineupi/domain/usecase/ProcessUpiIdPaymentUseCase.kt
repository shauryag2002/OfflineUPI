package com.offlineupi.domain.usecase

import com.offlineupi.domain.model.PaymentRequest
import com.offlineupi.domain.util.UpiInputValidator

class ProcessUpiIdPaymentUseCase(
    private val validator: UpiInputValidator
) {
    operator fun invoke(vpa: String, amount: String, note: String?): Result<PaymentRequest> {
        return validator.validate(vpa = vpa, amount = amount, note = note)
    }
}
