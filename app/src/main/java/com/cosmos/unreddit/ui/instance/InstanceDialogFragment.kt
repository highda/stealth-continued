package com.cosmos.unreddit.ui.instance

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.remote.instance.Instance
import com.cosmos.unreddit.data.remote.instance.InstanceManager
import com.cosmos.unreddit.databinding.FragmentInstanceBinding
import com.cosmos.unreddit.di.NetworkModule.InstanceOkHttp
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Edits the Teddit API instance: address and optional HTTP Basic credentials. In setup mode
 * (no instance configured yet) it cannot be dismissed without saving.
 */
@AndroidEntryPoint
class InstanceDialogFragment : DialogFragment(), DialogInterface.OnShowListener {

    @Inject
    lateinit var instanceManager: InstanceManager

    @Inject
    @InstanceOkHttp
    lateinit var okHttpClient: OkHttpClient

    private var _binding: FragmentInstanceBinding? = null
    private val binding get() = _binding!!

    private val isSetup: Boolean
        get() = arguments?.getBoolean(KEY_SETUP) ?: false

    private var testJob: Job? = null

    /** Set when the last test failed, so that the next Save saves without testing. */
    private var saveAnyway = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentInstanceBinding.inflate(requireActivity().layoutInflater)
        isCancelable = !isSetup

        initView(savedInstanceState)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isSetup) R.string.instance_setup_title else R.string.instance_dialog_title)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNeutralButton(R.string.instance_test, null)
            .apply {
                if (!isSetup) setNegativeButton(R.string.dialog_cancel, null)
            }
            .create()
            .apply {
                setCanceledOnTouchOutside(!isSetup)
                setOnShowListener(this@InstanceDialogFragment)
            }
    }

    private fun initView(savedInstanceState: Bundle?) {
        val current = instanceManager.current

        binding.run {
            textSetup.isVisible = isSetup

            if (savedInstanceState == null && current != null) {
                inputUrl.setText(current.url.toString().removeSuffix("/"))
                inputUsername.setText(current.username)
            }

            if (current?.hasCredentials == true) {
                layoutPassword.hint = getString(R.string.instance_password_keep_hint)
            }

            inputUrl.doAfterTextChanged { onInputChanged() }
            inputUsername.doAfterTextChanged { onInputChanged() }
            inputPassword.doAfterTextChanged { onInputChanged() }
        }

        updateHttpWarning()
    }

    override fun onShow(dialog: DialogInterface?) {
        (dialog as? AlertDialog)?.run {
            getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { save() }
            getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener { test(saveAfter = false) }
            getButton(DialogInterface.BUTTON_NEGATIVE)?.setOnClickListener { dismiss() }
        }
    }

    private fun onInputChanged() {
        binding.layoutUrl.error = null
        saveAnyway = false
        updatePositiveButton()
        updateHttpWarning()
    }

    private fun updatePositiveButton() {
        (dialog as? AlertDialog)?.getButton(DialogInterface.BUTTON_POSITIVE)?.setText(
            if (saveAnyway) R.string.instance_save_anyway else R.string.save
        )
    }

    private fun updateHttpWarning() {
        val url = Instance.parseUrl(binding.inputUrl.text.toString())
        val hasUser = binding.inputUsername.text.toString().isNotBlank()
        if (url?.scheme == "http" && hasUser) {
            showStatus(getString(R.string.instance_http_warning), error = true)
        } else if (binding.textStatus.text == getString(R.string.instance_http_warning)) {
            binding.textStatus.isVisible = false
        }
    }

    /** The instance as entered, or null (with an error shown) if the address is invalid. */
    private fun readInstance(): Instance? {
        val input = binding.inputUrl.text.toString()
        val url: HttpUrl? = Instance.parseUrl(input)
        if (url == null) {
            binding.layoutUrl.error = getString(
                if (input.isBlank()) R.string.instance_url_empty_error
                else R.string.instance_url_invalid_error
            )
            return null
        }
        val username = binding.inputUsername.text.toString().trim()
        val typed = binding.inputPassword.text.toString()
        // An empty password field keeps the saved password for the same user
        val current = instanceManager.current
        val password = if (typed.isEmpty() && current != null && username == current.username) {
            current.password
        } else {
            typed
        }
        return Instance(url, username, password)
    }

    private fun save() {
        if (saveAnyway) {
            readInstance()?.let { persist(it) }
        } else {
            test(saveAfter = true)
        }
    }

    private fun test(saveAfter: Boolean) {
        val instance = readInstance() ?: return
        testJob?.cancel()
        testJob = lifecycleScope.launch {
            setButtonsEnabled(false)
            showStatus(getString(R.string.instance_testing), error = false)

            val result = InstanceTester(okHttpClient).test(instance)
            val message = when (result) {
                is InstanceTester.Result.Ok -> getString(R.string.instance_test_ok)
                is InstanceTester.Result.Unauthorized -> getString(R.string.instance_test_unauthorized)
                is InstanceTester.Result.HttpError -> {
                    getString(R.string.instance_test_http_error, result.code)
                }
                is InstanceTester.Result.NetworkError -> {
                    getString(R.string.instance_test_failed, result.message)
                }
            }
            val ok = result is InstanceTester.Result.Ok
            showStatus(message, error = !ok)
            setButtonsEnabled(true)

            if (ok && saveAfter) {
                persist(instance)
            } else if (!ok) {
                saveAnyway = true
                updatePositiveButton()
            }
        }
    }

    private fun persist(instance: Instance) {
        val typed = binding.inputPassword.text.toString()
        lifecycleScope.launch {
            instanceManager.save(
                instance.url,
                instance.username,
                // null keeps the stored password when the field was left empty
                if (typed.isEmpty() && instance.password.isNotEmpty()) null else typed
            )
            setFragmentResult(REQUEST_KEY_INSTANCE, bundleOf(KEY_SETUP to isSetup))
            dismiss()
        }
    }

    private fun showStatus(message: String, error: Boolean) {
        binding.textStatus.run {
            text = message
            isVisible = true
            setTextColor(
                MaterialColors.getColor(
                    this,
                    if (error) R.attr.colorError else R.attr.colorPrimary
                )
            )
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        (dialog as? AlertDialog)?.run {
            getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = enabled
            getButton(DialogInterface.BUTTON_NEUTRAL)?.isEnabled = enabled
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "InstanceDialogFragment"

        const val REQUEST_KEY_INSTANCE = "REQUEST_KEY_INSTANCE"
        const val KEY_SETUP = "KEY_SETUP"

        fun show(fragmentManager: FragmentManager, setup: Boolean = false) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            InstanceDialogFragment().apply {
                arguments = bundleOf(KEY_SETUP to setup)
            }.show(fragmentManager, TAG)
        }
    }
}
