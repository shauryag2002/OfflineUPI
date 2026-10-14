package com.offlineupi.domain.usecase

import com.offlineupi.domain.model.PaymentRequest
import com.offlineupi.domain.repository.QrPayloadParserRepository
import com.offlineupi.domain.util.UpiInputValidator

class ProcessQrPaymentUseCase(
    private val qrPayloadParserRepository: QrPayloadParserRepository,
    private val validator: UpiInputValidator
) {
    operator fun invoke(rawPayload: String, fallbackAmount: String?): Result<PaymentRequest> {
        val transaction = qrPayloadParserRepository.parseQrPayload(rawPayload)
            ?: return Result.failure(IllegalArgumentException("Invalid UPI QR payload"))

        val amount = transaction.amount ?: fallbackAmount.orEmpty()
        return validator.validate(
            vpa = transaction.vpa,
            amount = amount,
            note = transaction.note
        )
    }
}
