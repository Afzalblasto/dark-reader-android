package com.darkreader.app.ui.convert

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkreader.app.databinding.ActivityMergePdfBinding
import com.darkreader.app.databinding.ItemMergePdfBinding
import com.darkreader.app.util.FileUtils
import com.darkreader.app.util.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

data class MergePdfItem(val uri: Uri, val name: String)

class MergePdfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMergePdfBinding
    private val pdfList = mutableListOf<MergePdfItem>()
    private lateinit var adapter: MergePdfAdapter

    private val pickPdfsLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            for (uri in uris) {
                val name = FileUtils.getUriFileName(this, uri) ?: "Document.pdf"
                pdfList.add(MergePdfItem(uri, name))
            }
            adapter.notifyDataSetChanged()
        }
    }

    private val saveMergedPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null) {
            mergePdfsToUri(destUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMergePdfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MergePdfAdapter()
        binding.recyclerPdfs.layoutManager = LinearLayoutManager(this)
        binding.recyclerPdfs.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                Collections.swap(pdfList, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(binding.recyclerPdfs)

        binding.fabAddPdf.setOnClickListener {
            pickPdfsLauncher.launch(arrayOf("application/pdf"))
        }

        binding.btnMerge.setOnClickListener {
            if (pdfList.size < 2) {
                Toast.makeText(this, "Please select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveMergedPdfLauncher.launch("Merged_Document.pdf")
        }
    }

    private fun mergePdfsToUri(destUri: Uri) {
        Toast.makeText(this, "Merging PDFs...", Toast.LENGTH_SHORT).show()
        val uris = pdfList.map { it.uri }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(destUri)?.use { out ->
                    PdfUtils.mergePdfs(this@MergePdfActivity, uris, out)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MergePdfActivity, "PDFs merged successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MergePdfActivity, "Merge failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    inner class MergePdfAdapter : RecyclerView.Adapter<MergePdfAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemMergePdfBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemMergePdfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = pdfList[position]
            holder.b.fileName.text = item.name
            holder.b.btnRemove.setOnClickListener {
                val idx = holder.bindingAdapterPosition
                if (idx in 0 until pdfList.size) {
                    pdfList.removeAt(idx)
                    notifyItemRemoved(idx)
                }
            }
        }

        override fun getItemCount(): Int = pdfList.size
    }
}
