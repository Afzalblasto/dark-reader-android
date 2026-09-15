package com.darkreader.app.ui.bookmarks

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.BookmarkEntity
import com.darkreader.app.databinding.FragmentBookmarksBinding
import com.darkreader.app.ui.main.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookmarkAdapter: BookmarkAdapter
    private var bookmarksList = mutableListOf<BookmarkEntity>()
    private val bookmarkDao by lazy { AppDatabase.getInstance(requireContext()).bookmarkDao() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadBookmarks()
    }

    override fun onResume() {
        super.onResume()
        loadBookmarks()
    }

    private fun setupRecyclerView() {
        bookmarkAdapter = BookmarkAdapter(
            bookmarks = bookmarksList,
            onBookmarkClicked = { bookmark -> openBookmark(bookmark) },
            onBookmarkDeleted = { bookmark -> deleteBookmark(bookmark) },
            onBookmarkLongClicked = { bookmark -> editBookmark(bookmark) }
        )
        binding.recyclerBookmarks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBookmarks.adapter = bookmarkAdapter
    }

    private fun loadBookmarks() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bookmarks = bookmarkDao.getAllSync()
            withContext(Dispatchers.Main) {
                bookmarksList.clear()
                bookmarksList.addAll(bookmarks)
                bookmarkAdapter.notifyDataSetChanged()
                binding.emptyText.visibility = if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun openBookmark(bookmark: BookmarkEntity) {
        val pageIdx = (bookmark.pageNumber - 1).coerceAtLeast(0)
        (activity as? MainActivity)?.openDocument(
            Uri.parse(bookmark.documentUri),
            bookmark.documentName,
            "application/pdf",
            pageIdx
        )
    }

    private fun deleteBookmark(bookmark: BookmarkEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            bookmarkDao.delete(bookmark)
            withContext(Dispatchers.Main) {
                val index = bookmarksList.indexOf(bookmark)
                if (index != -1) {
                    bookmarksList.removeAt(index)
                    bookmarkAdapter.notifyItemRemoved(index)
                    binding.emptyText.visibility = if (bookmarksList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun editBookmark(bookmark: BookmarkEntity) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
