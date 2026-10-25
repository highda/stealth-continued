package com.cosmos.unreddit.util.extension

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.PosterType
import com.cosmos.unreddit.databinding.IncludePostInfoBinding
import com.cosmos.unreddit.databinding.IncludePostMetricsBinding

fun IncludePostMetricsBinding.setRatio(ratio: Int) {
    textPostRatio.setRatio(ratio)
}

fun TextView.setRatio(ratio: Int) {
    ratio.takeUnless { it == -1 }?.let {
        isVisible = true
        text = context.getString(R.string.post_ratio, it)
    } ?: run {
        isVisible = false
    }
}

/** Fills the post info line: `r/subreddit · u/author · date`. */
fun IncludePostInfoBinding.setInfo(
    subreddit: String?,
    author: String?,
    posterType: PosterType = PosterType.REGULAR
) {
    textSubreddit.text = subreddit
    textPostAuthor.run {
        text = authorLabel(context, author)
        // Only admins and moderators stand out, like on Reddit
        val color = if (posterType == PosterType.REGULAR) {
            R.color.text_color_secondary
        } else {
            posterType.color
        }
        setTextColor(ContextCompat.getColor(context, color))
    }
}

fun authorLabel(context: Context, author: String?): String? {
    return when {
        author.isNullOrEmpty() -> author
        author.startsWith("u/") -> author
        author.startsWith("/u/") -> author.removePrefix("/")
        else -> context.getString(R.string.post_author, author)
    }
}
