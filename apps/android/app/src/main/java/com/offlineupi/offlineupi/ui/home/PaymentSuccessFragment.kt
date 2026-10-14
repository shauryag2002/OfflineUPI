package com.offlineupi.ui.home

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.offlineupi.R
import com.offlineupi.databinding.FragmentPaymentSuccessBinding

class PaymentSuccessFragment : Fragment() {

    private var _binding: FragmentPaymentSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentSuccessBinding.inflate(inflater, container, false)

        val args = requireArguments()
        binding.textAmount.text = args.getString(ARG_AMOUNT).orEmpty()
        binding.textPayeeName.text = args.getString(ARG_PAYEE_LABEL).orEmpty()
        binding.textVpaMobile.text = args.getString(ARG_VPA_OR_MOBILE).orEmpty()
        binding.textNote.text = args.getString(ARG_NOTE).orEmpty()
        binding.textTime.text = args.getString(ARG_TIMESTAMP).orEmpty()
        binding.textReferenceId.text = args.getString(ARG_REFERENCE_ID).orEmpty()
        binding.textChannel.text = args.getString(ARG_CHANNEL).orEmpty()

        applySafeAreaInsets()

        binding.buttonShareReceipt.setOnClickListener {
            shareReceipt()
        }

        binding.buttonNewPayment.setOnClickListener {
            findNavController().popBackStack(R.id.navigation_home, false)
        }

        binding.buttonDone.setOnClickListener {
            findNavController().popBackStack(R.id.navigation_home, false)
        }

        if (savedInstanceState == null) {
            playSuccessFeedback()
        }

        return binding.root
    }

    private fun applySafeAreaInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.successScroll.setPadding(
                binding.successScroll.paddingLeft,
                statusInsets.top,
                binding.successScroll.paddingRight,
                binding.successScroll.paddingBottom
            )

            binding.bottomActionBar.setPadding(
                binding.bottomActionBar.paddingLeft,
                binding.bottomActionBar.paddingTop,
                binding.bottomActionBar.paddingRight,
                navInsets.bottom + 12
            )
            insets
        }
    }

    private fun shareReceipt() {
        val shareText = getString(
            R.string.share_receipt_template,
            binding.textAmount.text,
            binding.textPayeeName.text,
            binding.textVpaMobile.text,
            binding.textReferenceId.text,
            binding.textTime.text,
            binding.textChannel.text
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_receipt_subject))
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.button_share_receipt)))
    }

    private fun playSuccessFeedback() {
        binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        binding.root.postDelayed({
            toneGenerator.release()
        }, 300)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_AMOUNT = "arg_amount"
        const val ARG_PAYEE_LABEL = "arg_payee_label"
        const val ARG_VPA_OR_MOBILE = "arg_vpa_mobile"
        const val ARG_NOTE = "arg_note"
        const val ARG_TIMESTAMP = "arg_timestamp"
        const val ARG_REFERENCE_ID = "arg_reference_id"
        const val ARG_CHANNEL = "arg_channel"
    }
}
