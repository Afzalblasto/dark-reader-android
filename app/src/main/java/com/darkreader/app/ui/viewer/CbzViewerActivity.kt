package com.darkreader.app.ui.viewer

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.darkreader.app.R
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.BookmarkEntity
import com.darkreader.app.databinding.ActivityCbzViewerBinding
import com.darkreader.app.util.CbzUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CbzViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCbzViewerBinding
    private var totalPages = 0
    private var currentUriStr: String = ""
    private var currentDocName: String = ""
    private var bookmarks = mutableListOf<Int>()
    private var pageAdapter: CbzPageAdapter? = null
    private var pageCallback: ViewPager2.OnPageChangeCallback? = null
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCbzViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUriStr = intent.getStringExtra("uri") ?: return
        currentDocName = intent.getStringExtra("name") ?: "Comic.cbz"

        binding.fileName.text = currentDocName
        binding.btnBack.setOnClickListener { finish() }
        binding.btnThumbnails.setOnClickListener {
            val editIntent = android.content.Intent(this, com.darkreader.app.ui.editor.PageEditorActivity::class.java).apply {
                putExtra("uri", currentUriStr)
                putExtra("name", currentDocName)
                putExtra("type", "cbz")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivity(editIntent)
        }

        loadCbz()
        loadBookmarks()
        setupControls()
    }

    private fun loadCbz() {
        val uri = Uri.parse(currentUriStr)
        lifecycleScope.launch(Dispatchers.IO) {
            totalPages = CbzUtils.getPageCount(this@CbzViewerActivity, uri)
            withContext(Dispatchers.Main) {
                pageAdapter?.close()
                val adapter = CbzPageAdapter(this@CbzViewerActivity, uri, totalPages)
                pageAdapter = adapter
                binding.viewPager.adapter = adapter
                binding.viewPager.offscreenPageLimit = 1
                val initialPage = intent.getIntExtra("page", 0).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                binding.viewPager.setCurrentItem(initialPage, false)
                updatePageIndicator(initialPage)

                binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updatePageIndicator(position)
                        updateBookmarkIcon(position)
                    }
                })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val curr = binding.viewPager.currentItem
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CbzViewerActivity)
                val tabs = db.tabDao().getAllSync()
                val tab = tabs.find { it.documentUri == currentUriStr }
                if (tab != null) {
                    db.tabDao().update(tab.copy(lastPage = curr))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadBookmarks() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CbzViewerActivity)
                val list = db.bookmarkDao().getByDocumentSync(currentUriStr)
                val loaded = list.map { it.pageNumber }.distinct().sorted()
                withContext(Dispatchers.Main) {
                    if (!destroyed) {
                        bookmarks = loaded.toMutableList()
                        updateBookmarkIcon(binding.viewPager.currentItem)
                    }
                }
            } catch (_: Exception) {
                // Bookmark failures must not crash the reader.
            }
        }
    }

    private fun updatePageIndicator(position: Int) {
        val pageNum = position + 1
        binding.pageIndicator.text = getString(R.string.page_format, pageNum, totalPages)
        binding.pageNumber.text = "$pageNum"
    }

    private fun updateBookmarkIcon(position: Int) {
        val isBookmarked = bookmarks.contains(position)
        binding.btnBookmark.setImageResource(
            if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
        )
    }

    private fun setupControls() {
        binding.btnPrev.setOnClickListener {
            val curr = binding.viewPager.currentItem
            if (curr > 0) binding.viewPager.currentItem = curr - 1
        }

        binding.btnNext.setOnClickListener {
            val curr = binding.viewPager.currentItem
            if (curr < totalPages - 1) binding.viewPager.currentItem = curr + 1
        }

        binding.btnBookmark.setOnClickListener {
            val curr = binding.viewPager.currentItem
            val isBookmarked = bookmarks.contains(curr)
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@CbzViewerActivity)
                if (isBookmarked) {
                    val entity = db.bookmarkDao().getByDocumentSync(currentUriStr).find { it.pageNumber == curr }
                    if (entity != null) db.bookmarkDao().delete(entity)
                    bookmarks.remove(curr)
                } else {
                    val newBm = BookmarkEntity(
                        documentUri = currentUriStr,
                        documentName = currentDocName,
                        pageNumber = curr,
                        label = "Page ${curr + 1}",
                        createdAt = System.currentTimeMillis()
                    )
                    db.bookmarkDao().insert(newBm)
                    bookmarks.add(curr)
                }
                withContext(Dispatchers.Main) {
                    updateBookmarkIcon(curr)
                    Toast.makeText(
                        this@CbzViewerActivity,
                        if (isBookmarked) getString(R.string.bookmark_removed) else getString(R.string.bookmark_added),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnPrevBookmark.setOnClickListener {
            val curr = binding.viewPager.currentItem
            val prevBm = bookmarks.filter { it < curr }.maxOrNull()
            if (prevBm != null) {
                binding.viewPager.currentItem = prevBm
            } else {
                Toast.makeText(this, "No previous bookmark", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnNextBookmark.setOnClickListener {
            val curr = binding.viewPager.currentItem
            val nextBm = bookmarks.filter { it > curr }.minOrNull()
            if (nextBm != null) {
                binding.viewPager.currentItem = nextBm
            } else {
                Toast.makeText(this, "No next bookmark", Toast.LENGTH_SHORT).show()
            }
        }

        binding.pageNumber.setOnClickListener {
            showJumpToPageDialog()
        }
    }

    private fun showJumpToPageDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "1 - $totalPages"
        input.setTextColor(resources.getColor(R.color.textPrimary, theme))

        AlertDialog.Builder(this, R.style.Theme_DarkReader_Dialog)
            .setTitle(R.string.jump_to_page)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val target = input.text.toString().toIntOrNull()
                if (target != null && target in 1..totalPages) {
                    binding.viewPager.currentItem = target - 1
                } else {
                    Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        destroyed = true
        pageCallback?.let { runCatching { binding.viewPager.unregisterOnPageChangeCallback(it) } }
        pageCallback = null
        binding.viewPager.adapter = null
        pageAdapter?.close()
        pageAdapter = null
        CbzUtils.clearCache(this, Uri.parse(currentUriStr))
        super.onDestroy()
    }
}
