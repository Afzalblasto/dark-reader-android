package com.darkreader.app.ui.safe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.darkreader.app.data.entity.SafeFileEntity
import com.darkreader.app.databinding.ItemSafeFileBinding
import com.darkreader.app.util.FileUtils

class SafeFileAdapter(
    private val safeFiles: List<SafeFileEntity>,
    private val onFileClicked: (SafeFileEntity) -> Unit,
    private val onFileLongClicked: (SafeFileEntity) -> Unit
) : RecyclerView.Adapter<SafeFileAdapter.SafeFileViewHolder>() {

    inner class SafeFileViewHolder(val binding: ItemSafeFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: SafeFileEntity) {
            binding.fileName.text = file.originalName
            binding.fileInfo.text = "${file.fileType.uppercase()} • ${FileUtils.formatFileSize(file.fileSize)}"

            binding.root.setOnClickListener { onFileClicked(file) }
            binding.root.setOnLongClickListener { 
                onFileLongClicked(file)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SafeFileViewHolder {
        val binding = ItemSafeFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SafeFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SafeFileViewHolder, position: Int) {
        holder.bind(safeFiles[position])
    }

    override fun getItemCount(): Int = safeFiles.size
}
