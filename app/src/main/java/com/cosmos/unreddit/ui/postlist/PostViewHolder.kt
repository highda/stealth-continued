package com.cosmos.unreddit.ui.postlist

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.model.preferences.UiPreferences.PostLayout
import com.cosmos.unreddit.databinding.IncludePostFlairsBinding
import com.cosmos.unreddit.databinding.IncludePostInfoBinding
import com.cosmos.unreddit.databinding.IncludePostMetricsBinding
import com.cosmos.unreddit.databinding.ItemPostCompactBinding
import com.cosmos.unreddit.databinding.ItemPostImageBinding
import com.cosmos.unreddit.databinding.ItemPostLinkBinding
import com.cosmos.unreddit.databinding.ItemPostTextBinding
import com.cosmos.unreddit.ui.common.widget.AwardView
import com.cosmos.unreddit.ui.common.widget.CheckableImageView
import com.cosmos.unreddit.util.ClickableMovementMethod
import com.cosmos.unreddit.util.extension.authorLabel
import com.cosmos.unreddit.util.extension.load
import com.cosmos.unreddit.util.extension.setInfo
import com.cosmos.unreddit.util.extension.setRatio

/**
 * Base of the post list rows. The card layouts share [IncludePostMetricsBinding] for their action
 * row; the compact layout has its own one-line stats, so [postMetricsBinding] is null for it.
 */
abstract class PostViewHolder(
    itemView: View,
    private val postInfoBinding: IncludePostInfoBinding,
    private val postMetricsBinding: IncludePostMetricsBinding?,
    private val postFlairsBinding: IncludePostFlairsBinding,
    listener: PostListAdapter.Listener
) : RecyclerView.ViewHolder(itemView) {

    private val title = itemView.findViewById<TextView>(R.id.text_post_title)
    private val awards: AwardView? = itemView.findViewById(R.id.awards)
    private val saveButton = itemView.findViewById<CheckableImageView>(R.id.button_save)

    init {
        itemView.apply {
            setOnClickListener {
                listener.onClick(bindingAdapterPosition)
            }
            setOnLongClickListener {
                listener.onClick(bindingAdapterPosition, true)
                return@setOnLongClickListener true
            }
        }

        itemView.findViewById<View>(R.id.button_more).setOnClickListener {
            listener.onMenuClick(bindingAdapterPosition)
        }

        saveButton.setOnClickListener {
            listener.onSaveClick(bindingAdapterPosition)
        }
    }

    open fun bind(
        postEntity: PostEntity,
        contentPreferences: ContentPreferences
    ) {
        postMetricsBinding?.run {
            post = postEntity
            setRatio(postEntity.ratio)
        }
        postFlairsBinding.post = postEntity

        postInfoBinding.run {
            this.post = postEntity
            setInfo(postEntity.subreddit, postEntity.author, postEntity.posterType)
        }

        title.apply {
            text = postEntity.title
            setTextColor(ContextCompat.getColor(context, postEntity.textColor))
        }

        awards?.apply {
            if (postEntity.awards.isNotEmpty()) {
                visibility = View.VISIBLE
                setAwards(postEntity.awards, postEntity.totalAwards)
            } else {
                visibility = View.GONE
            }
        }

        when {
            postEntity.hasFlairs -> {
                postFlairsBinding.root.visibility = View.VISIBLE
                postFlairsBinding.postFlair.apply {
                    if (!postEntity.flair.isEmpty()) {
                        visibility = View.VISIBLE

                        setFlair(postEntity.flair)
                    } else {
                        visibility = View.GONE
                    }
                }
            }

            postEntity.showDomain -> {
                // The row still shows the domain of the link
                postFlairsBinding.root.visibility = View.VISIBLE
                postFlairsBinding.postFlair.visibility = View.GONE
            }

            else -> {
                postFlairsBinding.root.visibility = View.GONE
            }
        }

        when {
            postEntity.crosspost != null -> {
                postInfoBinding.groupCrosspost.isVisible = true
                postInfoBinding.textCrosspostSubreddit.text = postEntity.crosspost.subreddit
                postInfoBinding.textCrosspostAuthor.text = authorLabel(
                    itemView.context,
                    postEntity.crosspost.author
                )
            }

            postEntity.crosspostScrap != null -> {
                postInfoBinding.groupCrosspost.isVisible = true
                postInfoBinding.textCrosspostSubreddit.text = postEntity.crosspostScrap?.subreddit
                postInfoBinding.textCrosspostAuthor.text = authorLabel(
                    itemView.context,
                    postEntity.crosspostScrap?.author
                )
            }

            else -> postInfoBinding.groupCrosspost.isVisible = false
        }

        saveButton.isChecked = postEntity.saved
    }

    open fun update(post: PostEntity) {
        title.setTextColor(ContextCompat.getColor(title.context, post.textColor))
        saveButton.isChecked = post.saved
    }

    class ImagePostViewHolder(
        private val binding: ItemPostImageBinding,
        listener: PostListAdapter.Listener
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        binding.includePostMetrics,
        binding.includePostFlairs,
        listener
    ) {

        init {
            binding.imagePostPreview.setOnClickListener {
                listener.onMediaClick(bindingAdapterPosition)
            }
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            binding.imagePostPreview.load(
                postEntity.preview,
                !postEntity.shouldShowPreview(contentPreferences)
            ) {
                error(R.drawable.preview_image_fallback)
                fallback(R.drawable.preview_image_fallback)
            }

            binding.buttonTypeIndicator.apply {
                if (postEntity.isGallery) {
                    visibility = View.VISIBLE
                    setImageResource(R.drawable.ic_gallery)
                } else {
                    visibility = View.GONE
                }
            }
        }
    }

    class VideoPostViewHolder(
        private val binding: ItemPostImageBinding,
        listener: PostListAdapter.Listener
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        binding.includePostMetrics,
        binding.includePostFlairs,
        listener
    ) {

        init {
            binding.imagePostPreview.setOnClickListener {
                listener.onMediaClick(bindingAdapterPosition)
            }
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            binding.imagePostPreview.load(
                postEntity.preview,
                !postEntity.shouldShowPreview(contentPreferences)
            ) {
                error(R.drawable.preview_video_fallback)
                fallback(R.drawable.preview_video_fallback)
            }

            binding.buttonTypeIndicator.apply {
                visibility = View.VISIBLE
                setImageResource(R.drawable.ic_play)
            }
        }
    }

    class TextPostViewHolder(
        private val binding: ItemPostTextBinding,
        listener: PostListAdapter.Listener,
        clickableMovementMethod: ClickableMovementMethod
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        binding.includePostMetrics,
        binding.includePostFlairs,
        listener
    ) {

        init {
            // No listeners of its own: they would keep the row from getting taps
            // on plain text. Text with links passes them on through the movement method.
            binding.textPostSelf.movementMethod = clickableMovementMethod
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            val previewText = postEntity.previewText?.toPreview()

            binding.textPostSelf.apply {
                if (postEntity.shouldShowPreview(contentPreferences) && previewText != null) {
                    binding.textPostSelfCard.visibility = View.VISIBLE
                    setText(previewText, false)
                    setTextColor(ContextCompat.getColor(context, postEntity.previewTextColor))
                } else {
                    binding.textPostSelfCard.visibility = View.GONE
                }
            }
        }

        override fun update(post: PostEntity) {
            super.update(post)
            if (binding.textPostSelfCard.isVisible) {
                binding.textPostSelf.apply {
                    setTextColor(ContextCompat.getColor(context, post.previewTextColor))
                }
            }
        }
    }

    class LinkPostViewHolder(
        private val binding: ItemPostLinkBinding,
        listener: PostListAdapter.Listener
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        binding.includePostMetrics,
        binding.includePostFlairs,
        listener
    ) {

        init {
            binding.imagePostLinkPreview.setOnClickListener {
                listener.onMediaClick(bindingAdapterPosition)
            }
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            binding.imagePostLinkPreview.load(
                postEntity.preview,
                !postEntity.shouldShowPreview(contentPreferences)
            ) {
                error(R.drawable.preview_link_fallback)
                fallback(R.drawable.preview_link_fallback)
            }
        }
    }

    /** Dense row for every post type, with a thumbnail for media and link posts. */
    class CompactPostViewHolder(
        private val binding: ItemPostCompactBinding,
        listener: PostListAdapter.Listener
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        null,
        binding.includePostFlairs,
        listener
    ) {

        init {
            binding.imagePostPreview.setOnClickListener {
                listener.onMediaClick(bindingAdapterPosition)
            }
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            binding.textPostVote.text = postEntity.score
            binding.textPostComments.text = postEntity.commentsNumber
            binding.textPostRatio.setRatio(postEntity.ratio)

            val fallback = when (postEntity.type) {
                PostType.IMAGE -> R.drawable.preview_image_fallback
                PostType.VIDEO -> R.drawable.preview_video_fallback
                PostType.LINK -> R.drawable.preview_link_fallback
                else -> null
            }

            binding.imagePostPreview.isVisible = fallback != null
            if (fallback != null) {
                binding.imagePostPreview.load(
                    postEntity.preview,
                    !postEntity.shouldShowPreview(contentPreferences)
                ) {
                    error(fallback)
                    fallback(fallback)
                }
            }

            @DrawableRes val indicator: Int? = when {
                postEntity.type == PostType.VIDEO -> R.drawable.ic_play
                postEntity.type == PostType.LINK -> R.drawable.ic_link
                postEntity.isGallery -> R.drawable.ic_gallery
                else -> null
            }
            binding.imageTypeIndicator.apply {
                isVisible = indicator != null
                indicator?.let { setImageResource(it) }
            }
        }
    }

    companion object {
        /** View type of [post] in the given layout, to pick the matching view holder. */
        fun getViewType(post: PostEntity, postLayout: PostLayout): Int {
            return when (postLayout) {
                PostLayout.CARD -> post.type.value
                PostLayout.COMPACT -> COMPACT_TYPE
            }
        }

        /** Creates the view holder of a view type from [getViewType], or null for another type. */
        fun create(
            parent: ViewGroup,
            viewType: Int,
            listener: PostListAdapter.Listener,
            clickableMovementMethod: ClickableMovementMethod
        ): PostViewHolder? {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                PostType.TEXT.value -> TextPostViewHolder(
                    ItemPostTextBinding.inflate(inflater, parent, false),
                    listener,
                    clickableMovementMethod
                )
                PostType.IMAGE.value -> ImagePostViewHolder(
                    ItemPostImageBinding.inflate(inflater, parent, false),
                    listener
                )
                PostType.VIDEO.value -> VideoPostViewHolder(
                    ItemPostImageBinding.inflate(inflater, parent, false),
                    listener
                )
                PostType.LINK.value -> LinkPostViewHolder(
                    ItemPostLinkBinding.inflate(inflater, parent, false),
                    listener
                )
                COMPACT_TYPE -> CompactPostViewHolder(
                    ItemPostCompactBinding.inflate(inflater, parent, false),
                    listener
                )
                else -> null
            }
        }

        // Far from PostType values and from the other view types of the adapters
        private const val COMPACT_TYPE = 100
    }
}

/**
 * Joins the paragraphs of a text post into one, so that the few lines of the preview are not
 * spent on blank lines. Links and other spans are kept.
 */
private fun CharSequence.toPreview(): CharSequence {
    val builder = SpannableStringBuilder(this.trim())
    LINE_BREAKS.findAll(builder).toList().asReversed().forEach { match ->
        builder.replace(match.range.first, match.range.last + 1, " ")
    }
    return builder
}

private val LINE_BREAKS = Regex("\\s*\n\\s*")

private val PostEntity.isGallery: Boolean
    get() = mediaType == MediaType.REDDIT_GALLERY ||
            mediaType == MediaType.IMGUR_ALBUM ||
            mediaType == MediaType.IMGUR_GALLERY
