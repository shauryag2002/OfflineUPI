package com.offlineupi.data.parser

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.offlineupi.domain.model.UpiTransaction
import com.offlineupi.domain.repository.QrPayloadParserRepository

class UpiQrParser : QrPayloadParserRepository {

    override fun parseQrPayload(rawPayload: String): UpiTransaction? {
        val normalizedPayload = normalizePayload(rawPayload) ?: return null
        val uri = runCatching { Uri.parse(normalizedPayload) }.getOrNull() ?: return null
        if (!uri.scheme.equals("upi", ignoreCase = true)) {
            return null
        }
        val hostOrPath = uri.host ?: uri.pathSegments.firstOrNull()
        if (!hostOrPath.equals("pay", ignoreCase = true)) {
            return null
        }

        val vpa = uri.getQueryParameter("pa")?.trim().orEmpty()
        if (vpa.isEmpty()) {
            return null
        }

        return UpiTransaction(
            vpa = vpa,
            amount = uri.getQueryParameter("am")?.trim(),
            note = uri.getQueryParameter("tn")?.trim(),
            payeeName = uri.getQueryParameter("pn")?.trim()
        )
    }

    private fun normalizePayload(rawPayload: String): String? {
        val trimmed = rawPayload.trim()
        if (trimmed.isBlank()) {
            return null
        }

        val decoded = runCatching {
            URLDecoder.decode(trimmed, StandardCharsets.UTF_8.name())
        }.getOrDefault(trimmed)

        val source = if (decoded.contains("upi://pay", ignoreCase = true)) decoded else trimmed
        val start = source.indexOf("upi://pay", ignoreCase = true)
        if (start < 0) {
            return null
        }

        val upiPart = source.substring(start).substringBefore(' ').substringBefore('\n').trim()
        return upiPart.replace("upi://pay/", "upi://pay")
    }
}
