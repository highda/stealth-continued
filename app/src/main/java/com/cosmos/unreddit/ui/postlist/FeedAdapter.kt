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

/**
 * The home feed (subscriptions), Reddit's Popular and All, then the custom feeds, in the profile
 * drawer.
 */
class FeedAdapter(
    private val onClick: (FeedSelection) -> Unit,
    private val onLongClick: (FeedWithSubreddits) -> Unit
) : ListAdapter<FeedAdapter.Item, FeedAdapter.ViewHolder>(COMPARATOR) {

    /** [feed] is set for custom feeds only. */
    data class Item(val selection: FeedSelection, val feed: FeedWithSubreddits?, val selected: Boolean)

    fun submit(feeds: List<FeedWithSubreddits>, selected: FeedSelection) {
        val builtIn = listOf(FeedSelection.Home, FeedSelection.Reddit.POPULAR, FeedSelection.Reddit.ALL)
        submitList(
            builtIn.map { Item(it, null, it == selected) } +
                    feeds.map {
                        val selection = FeedSelection.Custom(it.feed.id)
                        Item(selection, it, selection == selected)
                    }
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

                when (item.selection) {
                    FeedSelection.Home -> {
                        feedIcon.setImageResource(R.drawable.ic_home)
                        feedName.setText(R.string.feed_home)
                        feedInfo.setText(R.string.feed_home_info)
                    }
                    FeedSelection.Reddit.POPULAR -> {
                        feedIcon.setImageResource(R.drawable.ic_trending)
                        feedName.setText(R.string.feed_popular)
                        feedInfo.setText(R.string.feed_popular_info)
                    }
                    FeedSelection.Reddit.ALL -> {
                        feedIcon.setImageResource(R.drawable.ic_globe)
                        feedName.setText(R.string.feed_all)
                        feedInfo.setText(R.string.feed_all_info)
                    }
                    is FeedSelection.Custom -> if (feed != null) {
                        feedAvatar.setText(feed.feed.name)
                        feedName.text = feed.feed.name
                        feedInfo.text = if (feed.subreddits.isEmpty()) {
                            context.getString(R.string.feed_empty)
                        } else {
                            feed.names.joinToString(", ")
                        }
                    }
                }

                cardFeed.strokeWidth = if (item.selected) {
                    context.resources.getDimensionPixelSize(R.dimen.feed_selected_stroke)
                } else {
                    0
                }

                cardFeed.setOnClickListener { onClick(item.selection) }
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
                return oldItem.selection == newItem.selection
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }
        }
    }
}
