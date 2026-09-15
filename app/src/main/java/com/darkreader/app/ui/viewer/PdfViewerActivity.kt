package com.darkreader.app.ui.viewer

import android.app.AlertDialog
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.darkreader.app.R
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.BookmarkEntity
import com.darkreader.app.databinding.ActivityPdfViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var pfd: ParcelFileDescriptor? = null
    private var pageAdapter: PdfPageAdapter? = null
    private var totalPages = 0
    private var currentUriStr: String = ""
    private var currentDocName: String = ""
    private var bookmarks = mutableListOf<Int>()
    private var pageCallback: ViewPager2.OnPageChangeCallback? = null
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUriStr = intent.getStringExtra("uri") ?: run {
            finishWithError("No PDF was supplied")
            return
        }
        currentDocName = intent.getStringExtra("name") ?: "Document.pdf"

        binding.fileName.text = currentDocName
        binding.btnBack.setOnClickListener { finish() }
        binding.btnThumbnails.setOnClickListener {
            val editIntent = android.content.Intent(this, com.darkreader.app.ui.editor.PageEditorActivity::class.java).apply {
                putExtra("uri", currentUriStr)
                putExtra("name", currentDocName)
                putExtra("type", "pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivity(editIntent)
        }

        setupControls()
        loadBookmarks()
        loadPdf()
    }

    private fun loadPdf() {
        lifecycleScope.launch(Dispatchers.IO) {
            var fdForRenderer: ParcelFileDescriptor? = null
            try {
                val uri = Uri.parse(currentUriStr)
                val countFd = contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IllegalStateException("The PDF could not be opened")

                val count = try {
                    PdfRenderer(countFd).use { it.pageCount }
                } finally {
                    runCatching { countFd.close() }
                }

                if (count <= 0) throw IllegalArgumentException("The PDF contains no pages")

                fdForRenderer = contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IllegalStateException("The PDF could not be opened again")

                val adapterFd = fdForRenderer
                withContext(Dispatchers.Main) {
                    if (destroyed || isFinishing || isChangingConfigurations) {
                        runCatching { adapterFd.close() }
                        return@withContext
                    }

                    pageAdapter?.close()
                    pageAdapter = null
                    pfd?.close()
                    pfd = adapterFd
                    fdForRenderer = null
                    totalPages = count

                    val adapter = PdfPageAdapter(this@PdfViewerActivity, adapterFd)
                    pageAdapter = adapter
                    binding.viewPager.adapter = adapter
                    binding.viewPager.offscreenPageLimit = 1

                    val initialPage = intent.getIntExtra("page", 0)
                        .coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                    binding.viewPager.setCurrentItem(initialPage, false)
                    updatePageIndicator(initialPage)
                    updateBookmarkIcon(initialPage)

                    pageCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
                    pageCallback = object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            updatePageIndicator(position)
                            updateBookmarkIcon(position)
                        }
                    }.also { binding.viewPager.registerOnPageChangeCallback(it) }
                }
            } catch (e: Exception) {
                runCatching { fdForRenderer?.close() }
                withContext(Dispatchers.Main) {
                    if (!destroyed) {
                        Toast.makeText(
                            this@PdfViewerActivity,
                            "Unable to open PDF: ${e.localizedMessage ?: "unsupported or inaccessible file"}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val curr = if (::binding.isInitialized) binding.viewPager.currentItem else 0
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val db = AppDatabase.getInstance(this@PdfViewerActivity)
                val tabs = db.tabDao().getAllSync()
                tabs.firstOrNull { it.documentUri == currentUriStr }?.let {
                    db.tabDao().update(it.copy(lastPage = curr))
                }
            }
        }
    }

    private fun loadBookmarks() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@PdfViewerActivity)
                val list = db.bookmarkDao().getByDocumentSync(currentUriStr)
                val loaded = list.map { it.pageNumber }.distinct().sorted()
                withContext(Dispatchers.Main) {
                    if (!destroyed) {
                        bookmarks = loaded.toMutableList()
                        updateBookmarkIcon(binding.viewPager.currentItem)
                    }
                }
            } catch (_: Exception) {
                // A bookmark database failure must not prevent reading the document.
            }
        }
    }

    private fun updatePageIndicator(position: Int) {
        if (totalPages <= 0) return
        val pageNum = (position + 1).coerceIn(1, totalPages)
        binding.pageIndicator.text = getString(R.string.page_format, pageNum, totalPages)
        binding.pageNumber.text = pageNum.toString()
    }

    private fun updateBookmarkIcon(position: Int) {
        if (!::binding.isInitialized) return
        binding.btnBookmark.setImageResource(
            if (bookmarks.contains(position)) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
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
            val wasBookmarked = bookmarks.contains(curr)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(this@PdfViewerActivity)
                    if (wasBookmarked) {
                        db.bookmarkDao().getByDocumentSync(currentUriStr)
                            .filter { it.pageNumber == curr }
                            .forEach { db.bookmarkDao().delete(it) }
                        bookmarks.remove(curr)
                    } else {
                        db.bookmarkDao().insert(
                            BookmarkEntity(
                                documentUri = currentUriStr,
                                documentName = currentDocName,
                                pageNumber = curr,
                                label = "Page ${curr + 1}",
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        if (!bookmarks.contains(curr)) bookmarks.add(curr)
                        bookmarks.sort()
                    }
                    withContext(Dispatchers.Main) {
                        if (!destroyed) {
                            updateBookmarkIcon(curr)
                            Toast.makeText(
                                this@PdfViewerActivity,
                                if (wasBookmarked) getString(R.string.bookmark_removed) else getString(R.string.bookmark_added),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (!destroyed) Toast.makeText(this@PdfViewerActivity, "Bookmark failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnPrevBookmark.setOnClickListener {
            val curr = binding.viewPager.currentItem
            bookmarks.filter { it < curr }.maxOrNull()?.let {
                binding.viewPager.currentItem = it
            } ?: Toast.makeText(this, "No previous bookmark", Toast.LENGTH_SHORT).show()
        }

        binding.btnNextBookmark.setOnClickListener {
            val curr = binding.viewPager.currentItem
            bookmarks.filter { it > curr }.minOrNull()?.let {
                binding.viewPager.currentItem = it
            } ?: Toast.makeText(this, "No next bookmark", Toast.LENGTH_SHORT).show()
        }

        binding.pageNumber.setOnClickListener { showJumpToPageDialog() }
    }

    private fun showJumpToPageDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "1 - $totalPages"
            setTextColor(resources.getColor(R.color.textPrimary, theme))
        }

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

    private fun finishWithError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        destroyed = true
        pageCallback?.let { runCatching { binding.viewPager.unregisterOnPageChangeCallback(it) } }
        pageCallback = null
        binding.viewPager.adapter = null
        pageAdapter?.close()
        pageAdapter = null
        runCatching { pfd?.close() }
        pfd = null
        super.onDestroy()
    }
}
