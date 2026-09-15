package com.darkreader.app.ui.bookmarks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.darkreader.app.data.entity.BookmarkEntity
import com.darkreader.app.databinding.ItemBookmarkBinding

class BookmarkAdapter(
    private val bookmarks: List<BookmarkEntity>,
    private val onBookmarkClicked: (BookmarkEntity) -> Unit,
    private val onBookmarkDeleted: (BookmarkEntity) -> Unit,
    private val onBookmarkLongClicked: (BookmarkEntity) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    inner class BookmarkViewHolder(val binding: ItemBookmarkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bookmark: BookmarkEntity) {
            binding.documentName.text = bookmark.documentName
            binding.pageInfo.text = "Page ${bookmark.pageNumber}"
            binding.label.text = bookmark.label

            binding.root.setOnClickListener { onBookmarkClicked(bookmark) }
            binding.root.setOnLongClickListener { 
                onBookmarkLongClicked(bookmark)
                true
            }
            binding.btnDelete.setOnClickListener { onBookmarkDeleted(bookmark) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(bookmarks[position])
    }

    override fun getItemCount(): Int = bookmarks.size
}
