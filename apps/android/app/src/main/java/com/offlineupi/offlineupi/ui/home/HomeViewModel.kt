package com.offlineupi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.offlineupi.data.parser.UpiQrParser
import com.offlineupi.data.ussd.UssdDialStringBuilder
import com.offlineupi.domain.model.PaymentRequest
import com.offlineupi.domain.model.PaymentState
import com.offlineupi.domain.model.UpiTransaction
import com.offlineupi.domain.usecase.BuildUssdDialStringUseCase
import com.offlineupi.domain.usecase.ProcessUpiIdPaymentUseCase
import com.offlineupi.domain.util.UpiInputValidator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val validator = UpiInputValidator()
    private val qrParser = UpiQrParser()
    private val processUpiIdPaymentUseCase = ProcessUpiIdPaymentUseCase(validator)
    private val buildUssdDialStringUseCase = BuildUssdDialStringUseCase(UssdDialStringBuilder())

    private var pendingParsedQr: UpiTransaction? = null
    private var latestPreparedRequest: PaymentRequest? = null
    private var lastReceipt: PaymentReceipt? = null

    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    private val _dialRequest = MutableLiveData<String?>()
    val dialRequest: LiveData<String?> = _dialRequest

    private val _lastPreparedRequest = MutableLiveData<PaymentRequest?>()
    val lastPreparedRequest: LiveData<PaymentRequest?> = _lastPreparedRequest

    fun markScanning() {
        _uiState.value = _uiState.value?.copy(
            paymentState = PaymentState.SCANNING,
            statusMessage = "Scanning QR code..."
        )
    }

    fun markScanCancelled() {
        _uiState.value = _uiState.value?.copy(
            paymentState = PaymentState.IDLE,
            statusMessage = "Scan cancelled"
        )
    }

    fun processManualPayment(vpa: String, amount: String, note: String?) {
        _uiState.value = _uiState.value?.copy(
            paymentState = PaymentState.CONFIRMING,
            statusMessage = "Validating UPI ID payment...",
            errorMessage = null
        )

        processUpiIdPaymentUseCase(vpa, amount, note)
            .onSuccess { request ->
                queueDial(request)
            }
            .onFailure { error ->
                _uiState.value = _uiState.value?.copy(
                    paymentState = PaymentState.ERROR,
                    statusMessage = "Could not prepare payment",
                    errorMessage = error.message
                )
            }
    }

    fun processScannedQr(rawPayload: String) {
        val parsed = qrParser.parseQrPayload(rawPayload)
        if (parsed == null) {
            _uiState.value = _uiState.value?.copy(
                paymentState = PaymentState.ERROR,
                statusMessage = "QR parsing failed",
                errorMessage = "Invalid UPI QR payload",
                parsedTransaction = null
            )
            return
        }

        pendingParsedQr = parsed
        _uiState.value = _uiState.value?.copy(
            paymentState = PaymentState.CONFIRMING,
            statusMessage = if (parsed.amount.isNullOrBlank()) {
                "QR parsed. Enter amount and proceed."
            } else {
                "QR parsed. Confirm to dial *99#"
            },
            parsedTransaction = parsed,
            errorMessage = null
        )
    }

    fun payFromParsedQr(fallbackAmount: String?) {
        val parsed = pendingParsedQr
        if (parsed == null) {
            _uiState.value = _uiState.value?.copy(
                paymentState = PaymentState.ERROR,
                statusMessage = "No scanned payment found",
                errorMessage = "Scan a UPI QR before confirming"
            )
            return
        }

        val resolvedAmount = if (parsed.amount.isNullOrBlank()) fallbackAmount.orEmpty() else parsed.amount
        validator.validate(
            vpa = parsed.vpa,
            amount = resolvedAmount,
            note = parsed.note
        ).onSuccess { request ->
            queueDial(request)
        }.onFailure { error ->
            _uiState.value = _uiState.value?.copy(
                paymentState = PaymentState.ERROR,
                statusMessage = "Amount required for this QR",
                errorMessage = error.message
            )
        }
    }

    fun onDialCompleted(success: Boolean) {
        _uiState.value = if (success) {
            lastReceipt = buildLatestReceipt()
            _uiState.value?.copy(
                paymentState = PaymentState.COMPLETED,
                statusMessage = "Dialer opened with *99#. Complete the menu steps to finish payment.",
                errorMessage = null
            )
        } else {
            _uiState.value?.copy(
                paymentState = PaymentState.ERROR,
                statusMessage = "Failed to open dialer",
                errorMessage = "No compatible dialer found"
            )
        }
    }

    fun consumeDialRequest() {
        _dialRequest.value = null
    }

    fun consumeLastPreparedRequest() {
        _lastPreparedRequest.value = null
    }

    fun getLastReceipt(): PaymentReceipt? = lastReceipt

    fun ensureReceiptAvailable(): PaymentReceipt? {
        if (lastReceipt == null) {
            lastReceipt = buildLatestReceipt()
        }
        return lastReceipt
    }

    private fun queueDial(request: PaymentRequest) {
        val ussd = buildUssdDialStringUseCase(request)
        _dialRequest.value = ussd
        _lastPreparedRequest.value = request
        latestPreparedRequest = request
        _uiState.value = _uiState.value?.copy(
            paymentState = PaymentState.DIALING,
            statusMessage = "Opening dialer for *99# payment...",
            errorMessage = null
        )
    }

    private fun buildLatestReceipt(): PaymentReceipt? {
        val request = latestPreparedRequest ?: return null
        val payeeName = _uiState.value?.parsedTransaction?.payeeName?.takeIf { it.isNotBlank() }
        val label = payeeName ?: "UPI Payee"

        val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val reference = "TXN${System.currentTimeMillis().toString().takeLast(8)}"

        return PaymentReceipt(
            amount = "INR ${request.amount}",
            payeeLabel = label,
            vpaOrMobile = request.vpa,
            note = request.note ?: "No note",
            timestamp = timestamp,
            referenceId = reference,
            channel = "UPI 123PAY / *99#"
        )
    }
}