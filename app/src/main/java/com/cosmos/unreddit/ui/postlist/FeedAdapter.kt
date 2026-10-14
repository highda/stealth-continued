package com.cosmos.unreddit.ui.postlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.db.FeedWithSubreddits
import com.cosmos.unreddit.databinding.ItemFeedBinding

/** The home feed (subscriptions) followed by the custom feeds, in the profile drawer. */
class FeedAdapter(
    private val onClick: (FeedWithSubreddits?) -> Unit,
    private val onLongClick: (FeedWithSubreddits) -> Unit
) : ListAdapter<FeedAdapter.Item, FeedAdapter.ViewHolder>(COMPARATOR) {

    data class Item(val feed: FeedWithSubreddits?, val selected: Boolean)

    fun submit(feeds: List<FeedWithSubreddits>, selectedId: Int?) {
        submitList(
            listOf(Item(null, selectedId == null)) +
                    feeds.map { Item(it, it.feed.id == selectedId) }
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemFeedBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFeedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            val context = binding.root.context
            val feed = item.feed

            binding.run {
                feedIcon.isVisible = feed == null
                feedAvatar.isVisible = feed != null

                if (feed == null) {
                    feedIcon.setImageResource(R.drawable.ic_home)
                    feedName.setText(R.string.feed_home)
                    feedInfo.setText(R.string.feed_home_info)
                } else {
                    feedAvatar.setText(feed.feed.name)
                    feedName.text = feed.feed.name
                    feedInfo.text = if (feed.subreddits.isEmpty()) {
                        context.getString(R.string.feed_empty)
                    } else {
                        feed.names.joinToString(", ")
                    }
                }

                cardFeed.strokeWidth = if (item.selected) {
                    context.resources.getDimensionPixelSize(R.dimen.feed_selected_stroke)
                } else {
                    0
                }

                cardFeed.setOnClickListener { onClick(feed) }
                cardFeed.setOnLongClickListener {
                    feed?.let { onLongClick(it) }
                    feed != null
                }
            }
        }
    }

    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.feed?.feed?.id == newItem.feed?.feed?.id
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }
        }
    }
}
