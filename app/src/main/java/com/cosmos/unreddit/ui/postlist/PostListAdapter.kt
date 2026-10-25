package com.cosmos.unreddit.ui.postlist

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.model.preferences.UiPreferences.PostLayout
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.ui.common.widget.RedditView
import com.cosmos.unreddit.util.ClickableMovementMethod

class PostListAdapter(
    private val repository: PostListRepository,
    private val postClickListener: PostClickListener,
    private val onLinkClickListener: RedditView.OnLinkClickListener? = null
) : PagingDataAdapter<PostEntity, RecyclerView.ViewHolder>(POST_COMPARATOR) {

    interface PostClickListener {
        fun onClick(post: PostEntity)

        fun onLongClick(post: PostEntity)

        fun onMenuClick(post: PostEntity)

        fun onImageClick(post: PostEntity)

        fun onVideoClick(post: PostEntity)

        fun onLinkClick(post: PostEntity)

        fun onSaveClick(post: PostEntity)
    }

    interface Listener {
        fun onClick(position: Int, isLong: Boolean = false)

        fun onMediaClick(position: Int)

        fun onMenuClick(position: Int)

        fun onSaveClick(position: Int)
    }

    private val clickableMovementMethod = ClickableMovementMethod(
        object : ClickableMovementMethod.OnClickListener {
            override fun onLinkClick(link: String) {
                onLinkClickListener?.onLinkClick(link)
            }

            override fun onLinkLongClick(link: String) {
                onLinkClickListener?.onLinkLongClick(link)
            }

            override fun onClick() {
                // ignore
            }

            override fun onLongClick() {
                // ignore
            }
        }
    )

    var contentPreferences: ContentPreferences = ContentPreferences(
        showNsfw = false,
        showNsfwPreview = false,
        showSpoilerPreview = false
    )
        set(value) {
            if (field.showNsfwPreview != value.showNsfwPreview ||
                field.showSpoilerPreview != value.showSpoilerPreview
            ) {
                field = value
                notifyDataSetChanged()
            }
        }

    var postLayout: PostLayout = PostLayout.CARD
        set(value) {
            if (field != value) {
                field = value
                // Every row changes its view type
                notifyDataSetChanged()
            }
        }

    private val listener = object : Listener {
        override fun onClick(position: Int, isLong: Boolean) {
            getItem(position)?.let {
                if (isLong) {
                    postClickListener.onLongClick(it)
                } else {
                    setPostSeen(position, it)
                    postClickListener.onClick(it)
                }
            }
        }

        override fun onMediaClick(position: Int) {
            getItem(position)?.let {
                setPostSeen(position, it)
                when (it.type) {
                    PostType.IMAGE -> postClickListener.onImageClick(it)
                    PostType.LINK -> postClickListener.onLinkClick(it)
                    PostType.VIDEO -> postClickListener.onVideoClick(it)
                    else -> {
                        // ignore
                    }
                }
            }
        }

        override fun onMenuClick(position: Int) {
            getItem(position)?.let {
                postClickListener.onMenuClick(it)
            }
        }

        override fun onSaveClick(position: Int) {
            getItem(position)?.let {
                postClickListener.onSaveClick(it)
                it.saved = !it.saved
                notifyItemChanged(position, it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PostViewHolder.create(parent, viewType, listener, clickableMovementMethod)
            ?: throw IllegalArgumentException("Unknown type $viewType")
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.let { PostViewHolder.getViewType(it, postLayout) } ?: -1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        (holder as PostViewHolder).bind(item, contentPreferences)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = getItem(position) ?: return
            (holder as? PostViewHolder)?.update(item)
        }
    }

    private fun setPostSeen(position: Int, post: PostEntity) {
        post.seen = true
        notifyItemChanged(position, post)
    }

    companion object {
        private val POST_COMPARATOR = object : DiffUtil.ItemCallback<PostEntity>() {
            override fun areItemsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: PostEntity, newItem: PostEntity): Any {
                return newItem
            }
        }
    }
}
