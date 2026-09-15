package com.darkreader.app.ui.favourites

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.darkreader.app.R
import com.darkreader.app.data.entity.FavouriteEntity
import com.darkreader.app.databinding.ItemFavouriteBinding
import com.darkreader.app.util.FileUtils

class FavouriteAdapter(
    private val favourites: List<FavouriteEntity>,
    private val onFavouriteClicked: (FavouriteEntity) -> Unit,
    private val onFavouriteRemoved: (FavouriteEntity) -> Unit
) : RecyclerView.Adapter<FavouriteAdapter.FavouriteViewHolder>() {

    inner class FavouriteViewHolder(val binding: ItemFavouriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(favourite: FavouriteEntity) {
            binding.fileName.text = favourite.fileName
            binding.fileInfo.text = if (favourite.isDirectory) "Folder" else favourite.fileType.uppercase()
            binding.thumbnail.clearColorFilter()

            when {
                favourite.isDirectory -> {
                    binding.thumbnail.setImageResource(R.drawable.ic_folder)
                    binding.thumbnail.setColorFilter(Color.parseColor("#00BCD4"))
                }
                FileUtils.isPdfFile(favourite.fileName) -> {
                    binding.thumbnail.setImageResource(R.drawable.ic_pdf)
                    binding.thumbnail.setColorFilter(Color.parseColor("#00BCD4"))
                }
                FileUtils.isCbzFile(favourite.fileName) -> {
                    binding.thumbnail.setImageResource(R.drawable.ic_cbz)
                    binding.thumbnail.setColorFilter(Color.parseColor("#4DD0E1"))
                }
                FileUtils.isImageFile(favourite.fileName) -> {
                    try {
                        Glide.with(binding.root)
                            .load(Uri.parse(favourite.filePath))
                            .placeholder(R.drawable.ic_image)
                            .into(binding.thumbnail)
                    } catch (e: Exception) {
                        binding.thumbnail.setImageResource(R.drawable.ic_image)
                    }
                }
                else -> {
                    binding.thumbnail.setImageResource(R.drawable.ic_file)
                }
            }

            binding.root.setOnClickListener { onFavouriteClicked(favourite) }
            binding.btnRemove.setOnClickListener { onFavouriteRemoved(favourite) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = ItemFavouriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavouriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        holder.bind(favourites[position])
    }

    override fun getItemCount(): Int = favourites.size
}
