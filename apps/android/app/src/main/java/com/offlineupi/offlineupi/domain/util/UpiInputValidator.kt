package com.offlineupi.domain.util

import com.offlineupi.domain.model.PaymentRequest

class UpiInputValidator {

    fun validate(vpa: String, amount: String, note: String?): Result<PaymentRequest> {
        val normalizedPayee = vpa.trim().lowercase()
        if (!isValidVpaOrMobile(normalizedPayee)) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID or mobile number"))
        }

        val normalizedAmount = amount.trim()
        if (!isValidAmount(normalizedAmount)) {
            return Result.failure(IllegalArgumentException("Enter an amount greater than 0"))
        }

        return Result.success(
            PaymentRequest(
                vpa = normalizedPayee,
                amount = normalizedAmount,
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    private fun isValidVpaOrMobile(payee: String): Boolean {
        val upiRegex = Regex("^[a-zA-Z0-9._-]{2,256}@[a-zA-Z]{2,64}$")
        val mobileRegex = Regex("^[6-9][0-9]{9}$")
        return upiRegex.matches(payee) || mobileRegex.matches(payee)
    }

    private fun isValidAmount(amount: String): Boolean {
        val parsed = amount.toDoubleOrNull() ?: return false
        return parsed > 0
    }
}
