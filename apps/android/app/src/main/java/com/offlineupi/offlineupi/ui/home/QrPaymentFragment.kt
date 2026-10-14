package com.offlineupi.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.offlineupi.R
import com.offlineupi.databinding.FragmentQrPaymentBinding
import com.offlineupi.ui.scan.QrScannerActivity
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

class QrPaymentFragment : Fragment() {

    private var _binding: FragmentQrPaymentBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchQrScanner()
        } else {
            showMessage(getString(R.string.permission_camera_required))
        }
    }

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scanned = result.data?.getStringExtra(QrScannerActivity.EXTRA_QR_TEXT)
        if (result.resultCode != Activity.RESULT_OK || scanned.isNullOrBlank()) {
            homeViewModel.markScanCancelled()
            showMessage(getString(R.string.scan_failed_try_gallery))
            return@registerForActivityResult
        }
        homeViewModel.processScannedQr(scanned)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            showMessage(getString(R.string.gallery_no_selection))
            return@registerForActivityResult
        }

        val qrPayload = decodeQrFromImage(uri)
        if (qrPayload.isNullOrBlank()) {
            showMessage(getString(R.string.gallery_qr_not_found))
            return@registerForActivityResult
        }

        homeViewModel.processScannedQr(qrPayload)
        showMessage(getString(R.string.gallery_qr_loaded))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrPaymentBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(requireParentFragment())[HomeViewModel::class.java]

        setupUi()
        observeState()
        return binding.root
    }

    private fun setupUi() {
        binding.editTextQrFallbackAmount.addTextChangedListener(simpleTextWatcher {
            binding.inputLayoutQrAmount.error = null
        })

        binding.buttonScanQr.setOnClickListener {
            homeViewModel.markScanning()
            if (hasCameraPermission()) {
                launchQrScanner()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.buttonUploadGallery.setOnClickListener {
            homeViewModel.markScanning()
            galleryLauncher.launch("image/*")
        }

        binding.buttonPayScannedQr.setOnClickListener {
            val parsedAmount = homeViewModel.uiState.value?.parsedTransaction?.amount
            val fallbackAmount = binding.editTextQrFallbackAmount.text?.toString().orEmpty().trim()

            if (parsedAmount.isNullOrBlank() && fallbackAmount.isBlank()) {
                binding.inputLayoutQrAmount.error = getString(R.string.inline_error_amount_required_qr)
                return@setOnClickListener
            }

            homeViewModel.payFromParsedQr(fallbackAmount)
        }
    }

    private fun observeState() {
        homeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            val parsed = state.parsedTransaction
            binding.groupParsedQr.visibility = if (parsed == null) View.GONE else View.VISIBLE
            binding.textParsedVpa.text = parsed?.vpa ?: ""
            binding.textParsedName.text = parsed?.payeeName ?: getString(R.string.not_available)
            binding.textParsedAmount.text = parsed?.amount ?: getString(R.string.not_available)
        }
    }

    private fun simpleTextWatcher(onChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                onChanged()
            }
        }
    }

    private fun launchQrScanner() {
        scanLauncher.launch(Intent(requireContext(), QrScannerActivity::class.java))
    }

    private fun decodeQrFromImage(uri: Uri): String? {
        val bitmap = requireContext().contentResolver.openInputStream(uri).use { inputStream ->
            if (inputStream == null) {
                return null
            }
            BitmapFactory.decodeStream(inputStream)
        } ?: return null

        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return null
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        return try {
            MultiFormatReader().decode(binaryBitmap).text
        } catch (_: NotFoundException) {
            null
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showMessage(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        val topInset = ViewCompat.getRootWindowInsets(binding.root)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top ?: 0
        val extraMargin = resources.displayMetrics.density.times(12).toInt()

        when (val params = snackbar.view.layoutParams) {
            is FrameLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = topInset + extraMargin
                snackbar.view.layoutParams = params
            }

            is CoordinatorLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = topInset + extraMargin
                snackbar.view.layoutParams = params
            }
        }

        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
