package com.offlineupi.ui.scan

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.offlineupi.R
import com.offlineupi.databinding.ActivityQrScannerBinding
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult

class QrScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private var torchOn = false
    private var didReturnResult = false

    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            val text = result?.text.orEmpty()
            if (didReturnResult || text.isBlank()) {
                return
            }
            didReturnResult = true
            setResult(
                RESULT_OK,
                Intent().putExtra(EXTRA_QR_TEXT, text)
            )
            finish()
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            // No-op.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySafeAreaInsets()
        binding.scannerView.setStatusText("")
        binding.scannerView.decodeContinuous(barcodeCallback)
        binding.buttonCloseScan.setOnClickListener { finish() }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            binding.buttonToggleTorch.setOnClickListener { toggleTorch() }
            updateTorchLabel()
        } else {
            binding.buttonToggleTorch.isEnabled = false
            binding.buttonToggleTorch.text = getString(R.string.flash_not_supported)
        }
    }

    private fun applySafeAreaInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scannerRoot) { _, insets ->
            val statusInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            val topLayoutParams = binding.topControls.layoutParams as android.widget.FrameLayout.LayoutParams
            topLayoutParams.topMargin = statusInset + dpToPx(12)
            binding.topControls.layoutParams = topLayoutParams

            val bottomLayoutParams = binding.bottomHintContainer.layoutParams as android.widget.FrameLayout.LayoutParams
            bottomLayoutParams.bottomMargin = navInset + dpToPx(12)
            binding.bottomHintContainer.layoutParams = bottomLayoutParams
            insets
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        binding.scannerView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.scannerView.pause()
    }

    private fun toggleTorch() {
        torchOn = !torchOn
        if (torchOn) {
            binding.scannerView.setTorchOn()
        } else {
            binding.scannerView.setTorchOff()
        }
        updateTorchLabel()
    }

    private fun updateTorchLabel() {
        binding.buttonToggleTorch.text = if (torchOn) {
            getString(R.string.flash_off)
        } else {
            getString(R.string.flash_on)
        }
    }

    companion object {
        const val EXTRA_QR_TEXT = "EXTRA_QR_TEXT"
    }
}
