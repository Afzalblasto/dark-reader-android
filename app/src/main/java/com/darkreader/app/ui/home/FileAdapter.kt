package com.darkreader.app.ui.home

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.darkreader.app.R
import com.darkreader.app.databinding.ItemFileBinding
import com.darkreader.app.databinding.ItemFileGridBinding
import com.darkreader.app.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileAdapter(
    private val context: Context,
    private var isGridView: Boolean,
    private val listener: FileClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var files: List<DocumentFile> = emptyList()
    private var favouritePaths: Set<String> = emptySet()
    private val thumbnailScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(2))

    interface FileClickListener {
        fun onItemClick(file: DocumentFile)
        fun onItemLongClick(file: DocumentFile)
        fun onToggleFavourite(file: DocumentFile)
    }

    fun submitList(newFiles: List<DocumentFile>) {
        files = newFiles
        notifyDataSetChanged()
    }

    fun setFavourites(favourites: Set<String>) {
        favouritePaths = favourites
        notifyDataSetChanged()
    }

    fun setViewMode(gridView: Boolean) {
        isGridView = gridView
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = if (isGridView) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 1) {
            GridViewHolder(ItemFileGridBinding.inflate(inflater, parent, false))
        } else {
            ListViewHolder(ItemFileBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position !in files.indices) return
        val file = files[position]
        if (holder is ListViewHolder) holder.bind(file) else (holder as GridViewHolder).bind(file)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is ListViewHolder -> holder.recycle()
            is GridViewHolder -> holder.recycle()
        }
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        thumbnailScope.cancel()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemCount(): Int = files.size

    inner class ListViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        private var thumbnailJob: Job? = null
        private var bindToken = 0L

        fun bind(file: DocumentFile) {
            thumbnailJob?.cancel()
            val token = ++bindToken
            val name = file.name ?: "Unknown"
            binding.fileName.text = name
            updateFavIcon(binding.favIcon, favouritePaths.contains(file.uri.toString()))
            binding.favIcon.setOnClickListener { listener.onToggleFavourite(file) }

            if (file.isDirectory) {
                binding.thumbnail.setImageResource(R.drawable.ic_folder)
                binding.thumbnail.setColorFilter(Color.parseColor("#00BCD4"))
                binding.fileInfo.text = "Folder"
            } else {
                binding.fileInfo.text = "${FileUtils.formatFileSize(file.length())} • ${FileUtils.formatDate(file.lastModified())}"
                loadThumbnail(file, binding.thumbnail, 96, 96) { job -> thumbnailJob = job }
            }

            binding.root.setOnClickListener { listener.onItemClick(file) }
            binding.root.setOnLongClickListener { listener.onItemLongClick(file); true }
        }

        fun recycle() {
            bindToken++
            thumbnailJob?.cancel()
            thumbnailJob = null
            binding.thumbnail.setImageDrawable(null)
            Glide.with(binding.thumbnail).clear(binding.thumbnail)
        }
    }

    inner class GridViewHolder(private val binding: ItemFileGridBinding) : RecyclerView.ViewHolder(binding.root) {
        private var thumbnailJob: Job? = null
        private var bindToken = 0L

        fun bind(file: DocumentFile) {
            thumbnailJob?.cancel()
            val token = ++bindToken
            val name = file.name ?: "Unknown"
            binding.fileName.text = name
            updateFavIcon(binding.favIcon, favouritePaths.contains(file.uri.toString()))
            binding.favIcon.setOnClickListener { listener.onToggleFavourite(file) }

            if (file.isDirectory) {
                binding.thumbnail.setImageResource(R.drawable.ic_folder)
                binding.thumbnail.setColorFilter(Color.parseColor("#00BCD4"))
            } else {
                loadThumbnail(file, binding.thumbnail, 220, 220) { job -> thumbnailJob = job }
            }

            binding.root.setOnClickListener { listener.onItemClick(file) }
            binding.root.setOnLongClickListener { listener.onItemLongClick(file); true }
        }

        fun recycle() {
            bindToken++
            thumbnailJob?.cancel()
            thumbnailJob = null
            binding.thumbnail.setImageDrawable(null)
            Glide.with(binding.thumbnail).clear(binding.thumbnail)
        }
    }

    private fun updateFavIcon(imageView: ImageView, isFav: Boolean) {
        imageView.setImageResource(if (isFav) R.drawable.ic_star_filled else R.drawable.ic_star)
        imageView.setColorFilter(Color.parseColor(if (isFav) "#00BCD4" else "#9E9E9E"))
    }

    private fun loadThumbnail(
        file: DocumentFile,
        imageView: ImageView,
        width: Int,
        height: Int,
        onJob: (Job) -> Unit
    ) {
        val name = file.name ?: ""
        val token = "${file.uri}:$width:$height:${System.nanoTime()}"
        imageView.tag = token
        imageView.clearColorFilter()

        when {
            FileUtils.isPdfFile(name) -> {
                imageView.setImageResource(R.drawable.ic_pdf)
                imageView.setColorFilter(Color.parseColor("#00BCD4"))
                val job = thumbnailScope.launch {
                    val bmp = FileUtils.getFileThumbnail(context, file, width, height)
                    withContext(Dispatchers.Main) {
                        if (imageView.tag == token && bmp != null && imageView.isAttachedToWindow) {
                            imageView.clearColorFilter()
                            imageView.setImageBitmap(bmp)
                        } else {
                            bmp?.recycle()
                        }
                    }
                }
                onJob(job)
            }
            FileUtils.isCbzFile(name) -> {
                imageView.setImageResource(R.drawable.ic_cbz)
                imageView.setColorFilter(Color.parseColor("#4DD0E1"))
                val job = thumbnailScope.launch {
                    val bmp = FileUtils.getFileThumbnail(context, file, width, height)
                    withContext(Dispatchers.Main) {
                        if (imageView.tag == token && bmp != null && imageView.isAttachedToWindow) {
                            imageView.clearColorFilter()
                            imageView.setImageBitmap(bmp)
                        } else {
                            bmp?.recycle()
                        }
                    }
                }
                onJob(job)
            }
            FileUtils.isImageFile(name) -> {
                Glide.with(imageView).load(file.uri).placeholder(R.drawable.ic_image).into(imageView)
            }
            else -> {
                imageView.setImageResource(R.drawable.ic_file)
                imageView.setColorFilter(Color.parseColor("#9E9E9E"))
            }
        }
    }
}
