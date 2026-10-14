package com.offlineupi.data.ussd

import com.offlineupi.domain.model.PaymentRequest
import com.offlineupi.domain.repository.UssdDialStringRepository

class UssdDialStringBuilder : UssdDialStringRepository {

    override fun buildDialString(request: PaymentRequest): String {
        // Most dialers/carriers only accept digits, '*' and '#' in USSD input.
        // VPAs contain letters/symbols, so embedding them causes "invalid number".
        // Start with *99# and let the user finish menu steps manually.
        return "*99#"
    }
}
