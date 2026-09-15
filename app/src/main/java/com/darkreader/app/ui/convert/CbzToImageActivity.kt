package com.darkreader.app.ui.convert

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.darkreader.app.databinding.ActivityCbzToImageBinding
import com.darkreader.app.ui.editor.PageEditorItem
import com.darkreader.app.ui.editor.PageThumbnailAdapter
import com.darkreader.app.util.CbzUtils
import com.darkreader.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CbzToImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCbzToImageBinding
    private var documentUri: Uri? = null
    private var totalPages = 0
    private lateinit var pageAdapter: PageThumbnailAdapter

    private val pickCbzLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            documentUri = uri
            loadCbzDocument(uri)
        } else if (documentUri == null) {
            finish()
        }
    }

    private val pickDestinationFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            exportImagesToFolder(treeUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCbzToImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val formatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("JPG", "PNG"))
        binding.formatSpinner.adapter = formatAdapter

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.recyclerPages.visibility = if (checkedId == binding.selectedPages.id) View.VISIBLE else View.GONE
        }

        binding.btnExport.setOnClickListener {
            if (documentUri == null) {
                Toast.makeText(this, "No CBZ selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.selectedPages.isChecked && pageAdapter.selectedIds.isEmpty()) {
                Toast.makeText(this, "Select at least one page to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickDestinationFolderLauncher.launch(null)
        }

        val uriStr = intent.getStringExtra("uri")
        if (uriStr != null) {
            documentUri = Uri.parse(uriStr)
            loadCbzDocument(documentUri!!)
        } else {
            pickCbzLauncher.launch(arrayOf("application/x-cbz", "application/vnd.comicbook+zip", "application/zip", "application/octet-stream"))
        }
    }

    private fun loadCbzDocument(uri: Uri) {
        val name = FileUtils.getUriFileName(this, uri) ?: "Comic.cbz"
        binding.toolbar.title = "Export: $name"

        pageAdapter = PageThumbnailAdapter(this, uri, false) {}
        binding.recyclerPages.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerPages.adapter = pageAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            totalPages = CbzUtils.getPageCount(this@CbzToImageActivity, uri)
            val items = (0 until totalPages).map { idx ->
                PageEditorItem(id = idx.toLong(), originalIndex = idx)
            }
            withContext(Dispatchers.Main) {
                pageAdapter.setItems(items)
            }
        }
    }

    private fun exportImagesToFolder(treeUri: Uri) {
        val destFolder = DocumentFile.fromTreeUri(this, treeUri) ?: return
        val format = binding.formatSpinner.selectedItem.toString().lowercase()
        val isPng = format == "png"
        val mimeType = if (isPng) "image/png" else "image/jpeg"
        val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

        val pagesToExport = if (binding.allPages.isChecked) {
            (0 until totalPages).toList()
        } else {
            pageAdapter.getSelectedItems().map { it.originalIndex }
        }

        Toast.makeText(this, "Exporting ${pagesToExport.size} images...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                for ((idx, pageIndex) in pagesToExport.withIndex()) {
                    val bmp = CbzUtils.getPageBitmap(this@CbzToImageActivity, documentUri!!, pageIndex, 0)
                    if (bmp != null) {
                        val fileName = String.format("page_%03d.%s", idx + 1, format)
                        val targetFile = destFolder.createFile(mimeType, fileName)
                        if (targetFile != null) {
                            contentResolver.openOutputStream(targetFile.uri)?.use { out ->
                                bmp.compress(compressFormat, 90, out)
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CbzToImageActivity, "Export complete!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CbzToImageActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
