package com.darkreader.app.ui.editor

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.darkreader.app.R
import com.darkreader.app.databinding.ItemPageThumbnailBinding
import com.darkreader.app.util.CbzUtils
import com.darkreader.app.util.PdfUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

data class PageEditorItem(
    val id: Long,
    val originalIndex: Int,
    var rotation: Int = 0,
    var imageUri: Uri? = null,
    var externalPdfUri: Uri? = null,
    var externalPdfPageIndex: Int = 0,
    var label: String = ""
)

class PageThumbnailAdapter(
    private val context: Context,
    private val documentUri: Uri,
    private val isPdf: Boolean,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<PageThumbnailAdapter.PageViewHolder>() {

    val items = mutableListOf<PageEditorItem>()
    val selectedIds = mutableSetOf<Long>()

    fun setItems(newItems: List<PageEditorItem>) {
        items.clear()
        items.addAll(newItems)
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getSelectedItems(): List<PageEditorItem> {
        return items.filter { selectedIds.contains(it.id) }
    }

    fun deleteSelected(): Int {
        val count = selectedIds.size
        items.removeAll { selectedIds.contains(it.id) }
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
        return count
    }

    fun rotateSelected(): Int {
        var count = 0
        for (item in items) {
            if (selectedIds.contains(item.id)) {
                item.rotation = (item.rotation + 90) % 360
                count++
            }
        }
        notifyDataSetChanged()
        return count
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        notifyItemChanged(fromPosition)
        notifyItemChanged(toPosition)
    }

    fun addImagePages(uris: List<Uri>, insertAt: Int = items.size) {
        val targetIndex = insertAt.coerceIn(0, items.size)
        var nextId = System.currentTimeMillis()
        val newItems = uris.map { uri ->
            PageEditorItem(
                id = nextId++,
                originalIndex = -1,
                imageUri = uri,
                label = "Image"
            )
        }
        items.addAll(targetIndex, newItems)
        notifyDataSetChanged()
    }

    fun addExternalPdfPages(pdfUri: Uri, pageIndices: List<Int>, insertAt: Int = items.size) {
        val targetIndex = insertAt.coerceIn(0, items.size)
        var nextId = System.currentTimeMillis()
        val newItems = pageIndices.map { pageIdx ->
            PageEditorItem(
                id = nextId++,
                originalIndex = -1,
                externalPdfUri = pdfUri,
                externalPdfPageIndex = pageIdx,
                label = "P.${pageIdx + 1}"
            )
        }
        items.addAll(targetIndex, newItems)
        notifyDataSetChanged()
    }

    inner class PageViewHolder(val binding: ItemPageThumbnailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item = items[position]
            binding.pageNumber.text = "${position + 1}"
            binding.checkBox.isChecked = selectedIds.contains(item.id)
            binding.pageThumbnail.rotation = item.rotation.toFloat()

            // Load thumbnail
            loadThumbnail(item, binding)

            binding.root.setOnClickListener {
                if (selectedIds.contains(item.id)) {
                    selectedIds.remove(item.id)
                } else {
                    selectedIds.add(item.id)
                }
                notifyItemChanged(position)
                onSelectionChanged(selectedIds.size)
            }

            binding.checkBox.setOnClickListener {
                if (selectedIds.contains(item.id)) {
                    selectedIds.remove(item.id)
                } else {
                    selectedIds.add(item.id)
                }
                notifyItemChanged(position)
                onSelectionChanged(selectedIds.size)
            }
        }

        private fun loadThumbnail(item: PageEditorItem, binding: ItemPageThumbnailBinding) {
            binding.pageThumbnail.setImageResource(R.drawable.bg_tool_card)
            when {
                item.imageUri != null -> {
                    Glide.with(context).load(item.imageUri).into(binding.pageThumbnail)
                }
                item.externalPdfUri != null -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val bmp = PdfUtils.renderPage(context, item.externalPdfUri!!, item.externalPdfPageIndex, 200)
                        withContext(Dispatchers.Main) {
                            if (bmp != null) binding.pageThumbnail.setImageBitmap(bmp)
                        }
                    }
                }
                isPdf && item.originalIndex >= 0 -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val bmp = PdfUtils.renderPage(context, documentUri, item.originalIndex, 200)
                        withContext(Dispatchers.Main) {
                            if (bmp != null) binding.pageThumbnail.setImageBitmap(bmp)
                        }
                    }
                }
                !isPdf && item.originalIndex >= 0 -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val bmp = CbzUtils.getPageBitmap(context, documentUri, item.originalIndex, 200)
                        withContext(Dispatchers.Main) {
                            if (bmp != null) binding.pageThumbnail.setImageBitmap(bmp)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.size
}
