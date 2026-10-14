package com.offlineupi.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.offlineupi.R
import com.offlineupi.databinding.FragmentUpiIdPaymentBinding

class UpiIdPaymentFragment : Fragment() {

    private var _binding: FragmentUpiIdPaymentBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var recentPayeeStore: RecentPayeeStore
    private lateinit var recentPayeeAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpiIdPaymentBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(requireParentFragment())[HomeViewModel::class.java]
        recentPayeeStore = RecentPayeeStore(requireContext())

        setupRecentPayeeSuggestions()
        setupUi()
        return binding.root
    }

    private fun setupUi() {
        binding.editTextUpiId.addTextChangedListener(simpleTextWatcher {
            binding.inputLayoutUpiId.error = null
        })
        binding.editTextAmount.addTextChangedListener(simpleTextWatcher {
            binding.inputLayoutAmount.error = null
        })

        binding.editTextUpiId.setOnClickListener {
            binding.editTextUpiId.showDropDown()
        }

        binding.editTextUpiId.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                binding.root.postDelayed({
                    binding.root.smoothScrollTo(0, binding.inputLayoutUpiId.top)
                }, 150)
            }
        }

        binding.editTextAmount.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                binding.root.postDelayed({
                    binding.root.smoothScrollTo(0, binding.inputLayoutAmount.top)
                }, 150)
            }
        }

        binding.editTextNote.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                binding.root.postDelayed({
                    binding.root.smoothScrollTo(0, binding.inputLayoutNote.top)
                }, 150)
            }
        }

        binding.buttonPayWithUpiId.setOnClickListener {
            val vpa = binding.editTextUpiId.text?.toString().orEmpty()
            val amount = binding.editTextAmount.text?.toString().orEmpty()
            val note = binding.editTextNote.text?.toString()

            var isValid = true
            if (vpa.isBlank()) {
                binding.inputLayoutUpiId.error = getString(R.string.inline_error_upi_required)
                isValid = false
            }
            if (amount.isBlank()) {
                binding.inputLayoutAmount.error = getString(R.string.inline_error_amount_required)
                isValid = false
            }

            if (!isValid) {
                return@setOnClickListener
            }

            homeViewModel.processManualPayment(vpa = vpa, amount = amount, note = note)
        }
    }

    private fun setupRecentPayeeSuggestions() {
        recentPayeeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        binding.editTextUpiId.setAdapter(recentPayeeAdapter)
        binding.editTextUpiId.post {
            binding.editTextUpiId.dropDownWidth = binding.inputLayoutUpiId.width
        }
        refreshRecentPayeeSuggestions()
    }

    private fun refreshRecentPayeeSuggestions() {
        val recent = recentPayeeStore.getRecentPayees()
        recentPayeeAdapter.clear()
        recentPayeeAdapter.addAll(recent)
        recentPayeeAdapter.notifyDataSetChanged()
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

    override fun onResume() {
        super.onResume()
        refreshRecentPayeeSuggestions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
