package com.offlineupi.ui.home

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.navigation.fragment.findNavController
import com.offlineupi.R
import com.offlineupi.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var recentPayeeStore: RecentPayeeStore
    private var pendingDialString: String? = null

    private val callPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val dialString = pendingDialString
        if (granted && !dialString.isNullOrBlank()) {
            val launched = launchDirectCall(dialString)
            homeViewModel.onDialCompleted(launched)
            if (launched) {
                pendingDialString = null
                binding.buttonOpenDialer.visibility = View.GONE
            }
        } else if (!dialString.isNullOrBlank()) {
            binding.buttonOpenDialer.visibility = View.VISIBLE
            showMessage(getString(R.string.call_permission_denied_use_manual))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        recentPayeeStore = RecentPayeeStore(requireContext())

        setupPager()
        setupKeyboardHandling()
        observeState()
        return binding.root
    }

    private fun setupKeyboardHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            // Smoothly collapse the top info banner when typing so input fields remain fully visible above the keyboard
            binding.appBarLayout.setExpanded(!imeVisible, true)
            insets
        }
    }

    private fun setupPager() {
        val pagerAdapter = HomePagerAdapter(this)
        binding.paymentPager.adapter = pagerAdapter

        TabLayoutMediator(binding.paymentTabs, binding.paymentPager) { tab, position ->
            tab.text = if (position == 0) getString(R.string.tab_scan_qr) else getString(R.string.tab_upi_id)
        }.attach()
    }

    private fun observeState() {
        homeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.textStatus.text = state.statusMessage
            binding.textError.text = state.errorMessage.orEmpty()
            binding.textError.visibility = if (state.errorMessage.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.progressProcessing.visibility = if (state.paymentState.name == "SCANNING" || state.paymentState.name == "DIALING") {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        binding.buttonOpenDialer.setOnClickListener {
            val dialString = pendingDialString
            if (dialString.isNullOrBlank()) {
                showMessage(getString(R.string.receipt_not_ready))
                return@setOnClickListener
            }

            val launched = launchManualDialer(dialString)
            homeViewModel.onDialCompleted(launched)
            if (launched) {
                pendingDialString = null
                binding.buttonOpenDialer.visibility = View.GONE
            }
        }

        binding.buttonViewSuccess.setOnClickListener {
            val receipt = homeViewModel.ensureReceiptAvailable()
            if (receipt == null) {
                showMessage(getString(R.string.receipt_not_ready))
                return@setOnClickListener
            }

            val args = Bundle().apply {
                putString(PaymentSuccessFragment.ARG_AMOUNT, receipt.amount)
                putString(PaymentSuccessFragment.ARG_PAYEE_LABEL, receipt.payeeLabel)
                putString(PaymentSuccessFragment.ARG_VPA_OR_MOBILE, receipt.vpaOrMobile)
                putString(PaymentSuccessFragment.ARG_NOTE, receipt.note)
                putString(PaymentSuccessFragment.ARG_TIMESTAMP, receipt.timestamp)
                putString(PaymentSuccessFragment.ARG_REFERENCE_ID, receipt.referenceId)
                putString(PaymentSuccessFragment.ARG_CHANNEL, receipt.channel)
            }
            findNavController().navigate(R.id.navigation_payment_success, args)
        }

        homeViewModel.dialRequest.observe(viewLifecycleOwner) { dialString ->
            if (dialString.isNullOrBlank()) {
                return@observe
            }
            pendingDialString = dialString
            attemptDirectCallOrRequestPermission(dialString)
            homeViewModel.consumeDialRequest()
        }

        homeViewModel.lastPreparedRequest.observe(viewLifecycleOwner) { request ->
            if (request == null) {
                return@observe
            }

            recentPayeeStore.savePayee(request.vpa)
            copyPayeeToClipboard(request.vpa)
            binding.buttonViewSuccess.visibility = View.VISIBLE
            homeViewModel.consumeLastPreparedRequest()
        }
    }

    private fun attemptDirectCallOrRequestPermission(dialString: String) {
        if (hasCallPermission()) {
            val launched = launchDirectCall(dialString)
            homeViewModel.onDialCompleted(launched)
            if (!launched) {
                binding.buttonOpenDialer.visibility = View.VISIBLE
                showMessage(getString(R.string.dialer_ready_manual))
            }
            return
        }

        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    private fun launchDirectCall(dialString: String): Boolean {
        val encodedUssd = Uri.encode(dialString)
        val numberUri = Uri.parse("tel:$encodedUssd")
        val callIntent = Intent(Intent.ACTION_CALL, numberUri)

        return try {
            startActivity(callIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun launchManualDialer(dialString: String): Boolean {
        val encodedUssd = Uri.encode(dialString)
        val numberUri = Uri.parse("tel:$encodedUssd")
        val dialIntent = Intent(Intent.ACTION_DIAL, numberUri)

        return try {
            startActivity(dialIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun copyPayeeToClipboard(payee: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("UPI_PAYEE", payee)
        clipboard.setPrimaryClip(clipData)
        showMessage(getString(R.string.payee_copied_for_carrier, payee))
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
