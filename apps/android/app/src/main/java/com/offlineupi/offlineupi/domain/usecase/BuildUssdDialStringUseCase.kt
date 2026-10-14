package com.offlineupi.domain.usecase

import com.offlineupi.domain.model.PaymentRequest
import com.offlineupi.domain.repository.UssdDialStringRepository

class BuildUssdDialStringUseCase(
    private val ussdDialStringRepository: UssdDialStringRepository
) {
    operator fun invoke(request: PaymentRequest): String {
        return ussdDialStringRepository.buildDialString(request)
    }
}
