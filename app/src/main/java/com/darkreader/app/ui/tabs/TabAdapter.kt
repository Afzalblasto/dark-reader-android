package com.darkreader.app.ui.tabs

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.darkreader.app.R
import com.darkreader.app.data.entity.TabEntity
import com.darkreader.app.databinding.ItemTabBinding
import com.darkreader.app.util.CbzUtils
import com.darkreader.app.util.FileUtils
import com.darkreader.app.util.PdfUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TabAdapter(
    private val tabs: List<TabEntity>,
    private val onTabClicked: (TabEntity) -> Unit,
    private val onTabClosed: (TabEntity) -> Unit
) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

    private val thumbnailScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(2))

    inner class TabViewHolder(val binding: ItemTabBinding) : RecyclerView.ViewHolder(binding.root) {
        private var thumbnailJob: Job? = null
        private var bindToken = 0L

        fun bind(tab: TabEntity) {
            thumbnailJob?.cancel()
            val token = ++bindToken
            val context = binding.root.context
            binding.tabName.text = tab.fileName

            if (tab.isActive) {
                binding.root.strokeWidth = 3
                binding.root.strokeColor = ContextCompat.getColor(context, R.color.colorAccent)
            } else {
                binding.root.strokeWidth = 0
            }

            val uri = Uri.parse(tab.documentUri)
            when {
                FileUtils.isPdfFile(tab.fileName) -> {
                    binding.thumbnail.setImageResource(R.drawable.ic_pdf)
                    thumbnailJob = thumbnailScope.launch {
                        val bmp = PdfUtils.renderPage(context, uri, tab.lastPage.coerceAtLeast(0), 160)
                        withContext(Dispatchers.Main) {
                            if (bindToken == token && binding.root.isAttachedToWindow && bmp != null) {
                                binding.thumbnail.setImageBitmap(bmp)
                            } else {
                                bmp?.recycle()
                            }
                        }
                    }
                }
                FileUtils.isCbzFile(tab.fileName) -> {
                    binding.thumbnail.setImageResource(R.drawable.ic_cbz)
                    thumbnailJob = thumbnailScope.launch {
                        val bmp = runCatching {
                            CbzUtils.getPageBitmap(context, uri, tab.lastPage.coerceAtLeast(0), 160)
                        }.getOrNull()
                        withContext(Dispatchers.Main) {
                            if (bindToken == token && binding.root.isAttachedToWindow && bmp != null) {
                                binding.thumbnail.setImageBitmap(bmp)
                            } else {
                                bmp?.recycle()
                            }
                        }
                    }
                }
                FileUtils.isImageFile(tab.fileName) -> {
                    Glide.with(binding.thumbnail).load(uri).placeholder(R.drawable.ic_image).into(binding.thumbnail)
                }
                else -> binding.thumbnail.setImageResource(R.drawable.ic_file)
            }

            binding.root.setOnClickListener { onTabClicked(tab) }
            binding.btnClose.setOnClickListener { onTabClosed(tab) }
        }

        fun recycle() {
            bindToken++
            thumbnailJob?.cancel()
            thumbnailJob = null
            binding.thumbnail.setImageDrawable(null)
            Glide.with(binding.thumbnail).clear(binding.thumbnail)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder =
        TabViewHolder(ItemTabBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        if (position in tabs.indices) holder.bind(tabs[position])
    }

    override fun onViewRecycled(holder: TabViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        thumbnailScope.cancel()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemCount(): Int = tabs.size
}
