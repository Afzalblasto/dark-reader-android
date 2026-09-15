package com.darkreader.app.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.darkreader.app.R
import com.darkreader.app.databinding.ActivityPageEditorBinding
import com.darkreader.app.util.CbzUtils
import com.darkreader.app.util.FileUtils
import com.darkreader.app.util.PdfUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PageEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPageEditorBinding
    private lateinit var adapter: PageThumbnailAdapter

    private var documentUriStr: String = ""
    private var documentName: String = ""
    private var isPdf: Boolean = true

    private var pendingSaveOriginal = false

    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            adapter.addImagePages(uris)
            Toast.makeText(this, "Added ${uris.size} image pages", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickExternalPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importPagesFromExternalPdf(uri)
        }
    }

    private val saveAsNewLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destUri ->
        if (destUri != null) {
            saveDocumentToUri(destUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPageEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        documentUriStr = intent.getStringExtra("uri") ?: ""
        documentName = intent.getStringExtra("name") ?: "Document"
        val type = intent.getStringExtra("type") ?: "pdf"
        isPdf = type.equals("pdf", ignoreCase = true) || FileUtils.isPdfFile(documentName)

        binding.toolbar.title = "Edit: $documentName"
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupButtons()
        loadPages()
    }

    private fun setupRecyclerView() {
        val docUri = Uri.parse(documentUriStr)
        adapter = PageThumbnailAdapter(this, docUri, isPdf) { count ->
            binding.selectionInfo.text = "$count selected"
        }

        binding.recyclerPages.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerPages.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(binding.recyclerPages)
    }

    private fun setupButtons() {
        binding.btnDelete.setOnClickListener {
            if (adapter.selectedIds.isEmpty()) {
                Toast.makeText(this, "Select pages to delete", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (adapter.selectedIds.size >= adapter.items.size) {
                Toast.makeText(this, "Cannot delete all pages", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val deleted = adapter.deleteSelected()
            Toast.makeText(this, "Deleted $deleted pages", Toast.LENGTH_SHORT).show()
        }

        binding.btnRotate.setOnClickListener {
            if (adapter.selectedIds.isEmpty()) {
                Toast.makeText(this, "Select pages to rotate", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val rotated = adapter.rotateSelected()
            Toast.makeText(this, "Rotated $rotated pages", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddImages.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        binding.btnAddFromPdf.setOnClickListener {
            pickExternalPdfLauncher.launch(arrayOf("application/pdf"))
        }

        binding.btnSave.setOnClickListener {
            showSaveConfirmationDialog()
        }
    }

    private fun loadPages() {
        val docUri = Uri.parse(documentUriStr)
        lifecycleScope.launch(Dispatchers.IO) {
            val pageCount = if (isPdf) {
                PdfUtils.getPageCount(this@PageEditorActivity, docUri)
            } else {
                CbzUtils.getPageCount(this@PageEditorActivity, docUri)
            }

            val items = (0 until pageCount).map { idx ->
                PageEditorItem(
                    id = idx.toLong(),
                    originalIndex = idx
                )
            }

            withContext(Dispatchers.Main) {
                adapter.setItems(items)
                if (items.isEmpty()) {
                    binding.selectionInfo.text = "No pages found"
                }
            }
        }
    }

    private fun importPagesFromExternalPdf(pdfUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = PdfUtils.getPageCount(this@PageEditorActivity, pdfUri)
            withContext(Dispatchers.Main) {
                if (count > 0) {
                    adapter.addExternalPdfPages(pdfUri, (0 until count).toList())
                    Toast.makeText(this@PageEditorActivity, "Imported $count pages from PDF", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PageEditorActivity, "No pages found in selected PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSaveConfirmationDialog() {
        MaterialAlertDialogBuilder(this, R.style.Theme_DarkReader_Dialog)
            .setTitle(R.string.save_changes)
            .setMessage("Do you want to overwrite the original document or save as a new file?")
            .setPositiveButton(R.string.allow) { _, _ ->
                saveDocumentToUri(Uri.parse(documentUriStr))
            }
            .setNegativeButton(R.string.dont_allow) { _, _ ->
                val newName = FileUtils.generateModifiedFileName(documentName)
                val mime = if (isPdf) "application/pdf" else "application/vnd.comicbook+zip"
                saveAsNewLauncher.launch(newName)
            }
            .show()
    }

    private fun saveDocumentToUri(targetUri: Uri) {
        val itemsToSave = adapter.items.toList()
        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                    if (isPdf) {
                        savePdf(itemsToSave, out)
                    } else {
                        saveCbz(itemsToSave, out)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PageEditorActivity, "Saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PageEditorActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun savePdf(items: List<PageEditorItem>, outputStream: OutputStream) {
        val targetDoc = PDDocument()
        val originalDoc = try {
            contentResolver.openInputStream(Uri.parse(documentUriStr))?.let { PDDocument.load(it) }
        } catch (e: Exception) {
            null
        }

        try {
            for (item in items) {
                when {
                    item.imageUri != null -> {
                        contentResolver.openInputStream(item.imageUri!!)?.use { imgStream ->
                            val bmp = BitmapFactory.decodeStream(imgStream)
                            if (bmp != null) {
                                val rotatedBmp = if (item.rotation != 0) {
                                    val matrix = Matrix().apply { postRotate(item.rotation.toFloat()) }
                                    Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                } else bmp

                                val page = PDPage(PDRectangle(rotatedBmp.width.toFloat(), rotatedBmp.height.toFloat()))
                                targetDoc.addPage(page)

                                val baos = ByteArrayOutputStream()
                                rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                                val pdImage = PDImageXObject.createFromByteArray(targetDoc, baos.toByteArray(), "img")

                                val cs = PDPageContentStream(targetDoc, page)
                                cs.drawImage(pdImage, 0f, 0f, page.mediaBox.width, page.mediaBox.height)
                                cs.close()
                            }
                        }
                    }
                    item.externalPdfUri != null -> {
                        contentResolver.openInputStream(item.externalPdfUri!!)?.use { extStream ->
                            val extDoc = PDDocument.load(extStream)
                            if (item.externalPdfPageIndex in 0 until extDoc.numberOfPages) {
                                val p = targetDoc.importPage(extDoc.getPage(item.externalPdfPageIndex))
                                p.rotation = (p.rotation + item.rotation) % 360
                            }
                            extDoc.close()
                        }
                    }
                    originalDoc != null && item.originalIndex in 0 until originalDoc.numberOfPages -> {
                        val p = targetDoc.importPage(originalDoc.getPage(item.originalIndex))
                        p.rotation = (p.rotation + item.rotation) % 360
                    }
                }
            }
            targetDoc.save(outputStream)
        } finally {
            targetDoc.close()
            originalDoc?.close()
        }
    }

    private fun saveCbz(items: List<PageEditorItem>, outputStream: OutputStream) {
        val zos = ZipOutputStream(outputStream)
        try {
            for ((index, item) in items.withIndex()) {
                val entryName = String.format("%03d.jpg", index + 1)
                val bmp: Bitmap? = when {
                    item.imageUri != null -> {
                        contentResolver.openInputStream(item.imageUri!!)?.use { BitmapFactory.decodeStream(it) }
                    }
                    item.originalIndex >= 0 -> {
                        CbzUtils.getPageBitmap(this, Uri.parse(documentUriStr), item.originalIndex, 0)
                    }
                    else -> null
                }

                if (bmp != null) {
                    val finalBmp = if (item.rotation != 0) {
                        val matrix = Matrix().apply { postRotate(item.rotation.toFloat()) }
                        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    } else bmp

                    val baos = ByteArrayOutputStream()
                    finalBmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                    val bytes = baos.toByteArray()

                    zos.putNextEntry(ZipEntry(entryName))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
        } finally {
            zos.close()
        }
    }
}
