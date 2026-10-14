package com.cosmos.unreddit.ui.feed

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.databinding.FragmentFeedEditBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Creates or edits a custom feed: its name and its subreddits. */
@AndroidEntryPoint
class FeedEditDialogFragment : DialogFragment(), DialogInterface.OnShowListener {

    @Inject
    lateinit var repository: PostListRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var _binding: FragmentFeedEditBinding? = null
    private val binding get() = _binding!!

    private val feedId: Int?
        get() = arguments?.getInt(KEY_FEED_ID, NO_FEED)?.takeIf { it != NO_FEED }

    private val subreddits = mutableListOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentFeedEditBinding.inflate(requireActivity().layoutInflater)

        savedInstanceState?.getStringArrayList(KEY_SUBREDDITS)?.let { subreddits.addAll(it) }
        initView(savedInstanceState == null)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (feedId == null) R.string.feed_new else R.string.feed_edit)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .apply {
                if (feedId != null) setNeutralButton(R.string.feed_delete, null)
            }
            .create()
            .apply { setOnShowListener(this@FeedEditDialogFragment) }
    }

    private fun initView(firstTime: Boolean) {
        binding.run {
            layoutSubreddit.setEndIconOnClickListener { addSubreddit() }
            inputSubreddit.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addSubreddit()
                    true
                } else {
                    false
                }
            }
        }

        if (!firstTime) {
            renderSubreddits()
            return
        }

        arguments?.getString(KEY_SUBREDDIT)?.let { subreddits.add(it) }
        val id = feedId
        if (id == null) {
            renderSubreddits()
        } else {
            lifecycleScope.launch {
                repository.getFeed(id).firstOrNull()?.let { feed ->
                    binding.inputName.setText(feed.feed.name)
                    subreddits.clear()
                    subreddits.addAll(feed.names)
                }
                renderSubreddits()
            }
        }
    }

    override fun onShow(dialog: DialogInterface?) {
        (dialog as? AlertDialog)?.run {
            // Keep the buttons above the keyboard
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { save() }
            getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener { dismiss() }
            getButton(DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener { confirmDelete() }
        }
    }

    private fun addSubreddit() {
        val name = normalize(binding.inputSubreddit.text.toString())
        when {
            name.isEmpty() -> return
            !SUBREDDIT.matches(name) -> {
                binding.layoutSubreddit.error = getString(R.string.feed_subreddit_invalid)
                return
            }
            subreddits.none { it.equals(name, ignoreCase = true) } -> subreddits.add(name)
        }
        binding.layoutSubreddit.error = null
        binding.inputSubreddit.text = null
        renderSubreddits()
    }

    private fun renderSubreddits() {
        binding.groupSubreddits.removeAllViews()
        subreddits.forEach { name ->
            binding.groupSubreddits.addView(
                Chip(requireContext()).apply {
                    text = getString(R.string.subreddit_prefixed, name)
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        subreddits.remove(name)
                        renderSubreddits()
                    }
                }
            )
        }
    }

    private fun save() {
        // A subreddit still in the field counts
        if (binding.inputSubreddit.text?.isNotBlank() == true) addSubreddit()
        if (binding.layoutSubreddit.error != null) return

        val name = binding.inputName.text.toString().trim()
        if (name.isEmpty()) {
            binding.layoutName.error = getString(R.string.feed_name_empty)
            return
        }

        lifecycleScope.launch {
            val profileId = repository.getProfile(preferencesRepository.getCurrentProfile().first()).id
            val id = repository.saveFeed(feedId, name, subreddits.toList(), profileId)
            setFragmentResult(REQUEST_KEY_FEED, bundleOf(KEY_FEED_ID to id))
            dismiss()
        }
    }

    private fun confirmDelete() {
        val id = feedId ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.feed_delete)
            .setMessage(getString(R.string.feed_delete_message, binding.inputName.text))
            .setPositiveButton(R.string.feed_delete) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteFeed(id)
                    setFragmentResult(REQUEST_KEY_FEED, bundleOf(KEY_DELETED to id))
                    dismiss()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_SUBREDDITS, ArrayList(subreddits))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "FeedEditDialogFragment"

        const val REQUEST_KEY_FEED = "REQUEST_KEY_FEED"
        const val KEY_FEED_ID = "KEY_FEED_ID"
        const val KEY_DELETED = "KEY_DELETED"
        private const val KEY_SUBREDDIT = "KEY_SUBREDDIT"
        private const val KEY_SUBREDDITS = "KEY_SUBREDDITS"
        private const val NO_FEED = -1

        private val SUBREDDIT = Regex("^[A-Za-z0-9_]{2,21}$")

        /** `r/name`, `/r/name/` or a reddit.com link to just `name`. */
        fun normalize(input: String): String {
            return input.trim()
                .substringAfter("reddit.com", input.trim())
                .trim('/')
                .removePrefix("r/")
                .substringBefore('/')
                .trim()
        }

        /** Opens the editor for [feedId], or for a new feed (optionally with [subreddit]). */
        fun show(fragmentManager: FragmentManager, feedId: Int? = null, subreddit: String? = null) {
            FeedEditDialogFragment().apply {
                arguments = bundleOf(
                    KEY_FEED_ID to (feedId ?: NO_FEED),
                    KEY_SUBREDDIT to subreddit
                )
            }.show(fragmentManager, TAG)
        }
    }
}
