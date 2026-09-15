package com.darkreader.app.ui.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.darkreader.app.databinding.PageViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfPageAdapter(
    private val context: Context,
    private val pfd: ParcelFileDescriptor
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    private val renderer = PdfRenderer(pfd)
    private val pageCount = renderer.pageCount
    private val renderLock = Any()
    private val renderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(2))
    @Volatile private var isClosed = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        PageViewHolder(PageViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = pageCount

    override fun onViewRecycled(holder: PageViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    fun close() {
        if (isClosed) return
        isClosed = true
        renderScope.cancel()
        synchronized(renderLock) {
            runCatching { renderer.close() }
        }
    }

    inner class PageViewHolder(private val binding: PageViewBinding) : RecyclerView.ViewHolder(binding.root) {
        private var renderJob: kotlinx.coroutines.Job? = null
        private var bindToken = 0L

        fun bind(position: Int) {
            renderJob?.cancel()
            val token = ++bindToken
            binding.pageImage.resetZoom()
            binding.pageImage.tag = token
            binding.pageImage.setImageDrawable(null)

            renderJob = renderScope.launch {
                val bitmap = synchronized(renderLock) {
                    try {
                        if (isClosed || position !in 0 until pageCount) return@synchronized null
                        val page = renderer.openPage(position)
                        try {
                            val measuredWidth = binding.root.width
                            val availableWidth = (if (measuredWidth > 0) measuredWidth else binding.root.resources.displayMetrics.widthPixels)
                                .coerceIn(320, 1600)
                            val width = availableWidth
                            val height = (width.toFloat() / page.width * page.height)
                                .toInt()
                                .coerceIn(1, 2400)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        } finally {
                            runCatching { page.close() }
                        }
                    } catch (_: Exception) {
                        null
                    }
                }

                withContext(Dispatchers.Main) {
                    if (!isClosed && binding.pageImage.tag == token && binding.root.isAttachedToWindow && bitmap != null) {
                        binding.pageImage.setImageBitmap(bitmap)
                    } else {
                        bitmap?.recycle()
                    }
                }
            }
        }

        fun recycle() {
            bindToken++
            renderJob?.cancel()
            renderJob = null
            binding.pageImage.tag = null
            binding.pageImage.setImageDrawable(null)
        }
    }
}
