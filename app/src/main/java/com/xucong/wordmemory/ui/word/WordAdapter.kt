package com.xucong.wordmemory.ui.word

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xucong.wordmemory.data.local.entity.WordEntity
import com.xucong.wordmemory.databinding.ItemWordBinding

/** 单词列表适配器，点击进入单词详情。 */
class WordAdapter(
    private val onClick: (WordEntity) -> Unit,
) : ListAdapter<WordEntity, WordAdapter.WordViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WordViewHolder(
        private val binding: ItemWordBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WordEntity) {
            binding.termText.text = item.term
            binding.phoneticText.text = item.phonetic
            binding.meaningText.text = item.meaning
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<WordEntity>() {
        override fun areItemsTheSame(oldItem: WordEntity, newItem: WordEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: WordEntity, newItem: WordEntity): Boolean =
            oldItem == newItem
    }
}
