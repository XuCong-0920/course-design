package com.xucong.wordmemory.ui.deck

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xucong.wordmemory.R
import com.xucong.wordmemory.data.local.dao.DeckSummary
import com.xucong.wordmemory.databinding.ItemDeckBinding

/** 词库列表适配器；点击整张卡片回调给 Fragment 做导航。 */
class DeckAdapter(
    private val onClick: (DeckSummary) -> Unit,
) : ListAdapter<DeckSummary, DeckAdapter.DeckViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val binding = ItemDeckBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeckViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeckViewHolder(
        private val binding: ItemDeckBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeckSummary) {
            val context = binding.root.context
            binding.nameText.text = item.name
            binding.descriptionText.text = item.description
            binding.countText.text = context.getString(R.string.deck_word_count, item.wordCount)
            binding.dueText.text = context.getString(R.string.deck_due_count, item.dueCount)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DeckSummary>() {
        override fun areItemsTheSame(oldItem: DeckSummary, newItem: DeckSummary): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DeckSummary, newItem: DeckSummary): Boolean =
            oldItem == newItem
    }
}
