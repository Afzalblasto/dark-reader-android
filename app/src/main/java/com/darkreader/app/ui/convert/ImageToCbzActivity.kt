package com.darkreader.app.ui.convert

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.darkreader.app.databinding.ActivityImageToCbzBinding
import com.darkreader.app.databinding.ItemImageSelectBinding
import com.darkreader.app.util.CbzUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class ImageToCbzActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageToCbzBinding
    private val imageUris = mutableListOf<Uri>()
    private lateinit var adapter: ImageSelectAdapter

    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            imageUris.addAll(uris)
            adapter.notifyDataSetChanged()
        }
    }

    private val saveCbzLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.comicbook+zip")
    ) { destUri ->
        if (destUri != null) {
            createCbz(destUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageToCbzBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ImageSelectAdapter()
        binding.recyclerPdfs.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerPdfs.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                Collections.swap(imageUris, from, to)
                adapter.notifyItemMoved(from, to)
                adapter.notifyItemChanged(from)
                adapter.notifyItemChanged(to)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(binding.recyclerPdfs)

        binding.fabAddImages.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        binding.btnCreate.setOnClickListener {
            if (imageUris.isEmpty()) {
                Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveCbzLauncher.launch("Comic.cbz")
        }
    }

    private fun createCbz(destUri: Uri) {
        Toast.makeText(this, "Creating CBZ...", Toast.LENGTH_SHORT).show()
        val urisToProcess = imageUris.toList()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(destUri)?.use { out ->
                    CbzUtils.imagesToCbz(this@ImageToCbzActivity, urisToProcess, out)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImageToCbzActivity, "CBZ created successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImageToCbzActivity, "Creation failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    inner class ImageSelectAdapter : RecyclerView.Adapter<ImageSelectAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemImageSelectBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemImageSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = imageUris[position]
            holder.b.numberBadge.text = "${position + 1}"
            Glide.with(this@ImageToCbzActivity)
                .load(uri)
                .into(holder.b.thumbnail)
        }

        override fun getItemCount(): Int = imageUris.size
    }
}
