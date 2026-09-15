package com.darkreader.app.ui.viewer

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.darkreader.app.databinding.PageViewBinding
import com.darkreader.app.util.CbzUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CbzPageAdapter(
    private val context: Context,
    private val uri: Uri,
    private val pageCount: Int
) : RecyclerView.Adapter<CbzPageAdapter.PageViewHolder>() {

    private val renderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(2))
    @Volatile private var isClosed = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        PageViewHolder(PageViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) = holder.bind(position)

    override fun getItemCount(): Int = pageCount

    override fun onViewRecycled(holder: PageViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    fun close() {
        if (isClosed) return
        isClosed = true
        renderScope.cancel()
    }

    inner class PageViewHolder(private val binding: PageViewBinding) : RecyclerView.ViewHolder(binding.root) {
        private var renderJob: Job? = null
        private var bindToken = 0L

        fun bind(position: Int) {
            renderJob?.cancel()
            val token = ++bindToken
            binding.pageImage.resetZoom()
            binding.pageImage.tag = token
            binding.pageImage.setImageDrawable(null)

            renderJob = renderScope.launch {
                val measuredWidth = binding.root.width
                val width = (if (measuredWidth > 0) measuredWidth else binding.root.resources.displayMetrics.widthPixels).coerceIn(320, 1600)
                val bitmap = try {
                    CbzUtils.getPageBitmap(context, uri, position, width)
                } catch (_: Exception) {
                    null
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
