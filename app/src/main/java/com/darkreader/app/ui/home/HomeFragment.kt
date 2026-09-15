package com.darkreader.app.ui.home

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.doOnLayout
import com.darkreader.app.R
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.FavouriteEntity
import com.darkreader.app.databinding.FragmentHomeBinding
import com.darkreader.app.ui.main.MainActivity
import com.darkreader.app.ui.settings.SettingsActivity
import com.darkreader.app.util.Constants
import com.darkreader.app.util.FileUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fileAdapter: FileAdapter
    private lateinit var prefs: SharedPreferences
    private var currentDirectory: DocumentFile? = null
    private var rootDirectory: DocumentFile? = null
    private val folderStack = mutableListOf<DocumentFile>()
    private var allFiles = listOf<DocumentFile>()

    private var isGridView = false
    private var currentSortOrder = Constants.SORT_NAME_ASC
    private val favouriteDao by lazy { AppDatabase.getInstance(requireContext()).favouriteDao() }

    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val flags = (result.data?.flags ?: 0) and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                try {
                    if (flags == 0 || result.data?.flags?.and(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) == 0) {
                        throw SecurityException("The document provider did not grant persistent access")
                    }
                    requireContext().contentResolver.takePersistableUriPermission(uri, flags)
                    if (DocumentFile.fromTreeUri(requireContext(), uri)?.exists() != true) {
                        throw SecurityException("The selected folder cannot be read")
                    }
                    prefs.edit().putString("root_uri", uri.toString()).apply()
                    loadRoot(uri)
                } catch (e: SecurityException) {
                    Toast.makeText(requireContext(), "Folder access was not granted. Please choose the folder again.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        isGridView = readGridViewPreference()
        currentSortOrder = readSortOrderPreference()

        setupRecyclerView()
        setupListeners()

        val rootUriStr = prefs.getString("root_uri", null)
        if (rootUriStr == null || !hasPersistedReadPermission(Uri.parse(rootUriStr))) {
            if (rootUriStr != null) prefs.edit().remove("root_uri").apply()
            showEmptyState(true, "Select a folder to browse documents")
        } else {
            try {
                loadRoot(Uri.parse(rootUriStr))
            } catch (e: Exception) {
                e.printStackTrace()
                showEmptyState(true, "Could not open saved folder. Please select again.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshFavourites()
    }

    private fun setupListeners() {
        binding.btnChooseFolder.setOnClickListener { promptSelectRoot() }
        binding.btnSelectFolder.setOnClickListener { promptSelectRoot() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.btnViewMode.setOnClickListener {
            isGridView = !isGridView
            prefs.edit().putBoolean(Constants.PREF_VIEW_MODE, isGridView).apply()
            binding.btnViewMode.setImageResource(if (isGridView) R.drawable.ic_grid else R.drawable.ic_list)
            fileAdapter.setViewMode(isGridView)
            binding.recyclerFiles.layoutManager = if (isGridView) GridLayoutManager(context, 1) else LinearLayoutManager(context)
            binding.recyclerFiles.doOnLayout {
                if (isGridView) {
                    (binding.recyclerFiles.layoutManager as? GridLayoutManager)?.spanCount = gridSpanCount()
                }
            }
        }
        binding.btnViewMode.setImageResource(if (isGridView) R.drawable.ic_grid else R.drawable.ic_list)

        binding.btnSort.setOnClickListener { showSortDialog() }

        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndSortFiles(s?.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.fabAdd.setOnClickListener { showCreateFolderDialog() }
    }

    private fun promptSelectRoot() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        openDocumentTreeLauncher.launch(intent)
    }

    private fun hasPersistedReadPermission(uri: Uri): Boolean =
        requireContext().contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }

    // Older builds incorrectly used these keys for String ListPreference values.
    // Read and normalize them so upgrading cannot crash the home screen.
    private fun readSortOrderPreference(): Int = try {
        prefs.getInt(Constants.PREF_SORT_ORDER, Constants.SORT_NAME_ASC)
    } catch (_: ClassCastException) {
        val normalized = sortOrderFromValue(prefs.getString(Constants.PREF_SORT_ORDER, "name_asc"))
        prefs.edit().putInt(Constants.PREF_SORT_ORDER, normalized).apply()
        normalized
    }

    private fun readGridViewPreference(): Boolean = try {
        prefs.getBoolean(Constants.PREF_VIEW_MODE, false)
    } catch (_: ClassCastException) {
        val normalized = prefs.getString(Constants.PREF_VIEW_MODE, "list") == "grid"
        prefs.edit().putBoolean(Constants.PREF_VIEW_MODE, normalized).apply()
        normalized
    }

    private fun sortOrderFromValue(value: String?): Int = when (value) {
        "name_desc" -> Constants.SORT_NAME_DESC
        "date_new" -> Constants.SORT_DATE_NEW
        "date_old" -> Constants.SORT_DATE_OLD
        "size_large" -> Constants.SORT_SIZE_LARGE
        "size_small" -> Constants.SORT_SIZE_SMALL
        "type" -> Constants.SORT_TYPE
        else -> Constants.SORT_NAME_ASC
    }

    private fun loadRoot(uri: Uri) {
        val root = DocumentFile.fromTreeUri(requireContext(), uri)
        if (root != null && root.exists()) {
            rootDirectory = root
            currentDirectory = root
            folderStack.clear()
            loadCurrentDirectory()
        } else {
            showEmptyState(true, "Saved folder inaccessible. Please choose folder.")
        }
    }

    private fun loadCurrentDirectory() {
        val dir = currentDirectory ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.breadcrumb.text = dir.name ?: "Root"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val files = try {
                dir.listFiles().filter { file ->
                    file.isDirectory || FileUtils.isSupportedFile(file.name ?: "")
                }
            } catch (e: Exception) {
                emptyList()
            }
            allFiles = files
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                filterAndSortFiles(binding.searchEdit.text?.toString())
                refreshFavourites()
            }
        }
    }

    private fun filterAndSortFiles(query: String?) {
        var filtered = if (!query.isNullOrBlank()) {
            allFiles.filter { it.name?.contains(query, ignoreCase = true) == true }
        } else {
            allFiles
        }

        // Sort: folders first, then files sorted according to currentSortOrder
        val folders = filtered.filter { it.isDirectory }.sortedBy { (it.name ?: "").lowercase() }
        val nonFolders = filtered.filter { !it.isDirectory }

        val sortedNonFolders = when (currentSortOrder) {
            Constants.SORT_NAME_ASC -> nonFolders.sortedBy { (it.name ?: "").lowercase() }
            Constants.SORT_NAME_DESC -> nonFolders.sortedByDescending { (it.name ?: "").lowercase() }
            Constants.SORT_DATE_NEW -> nonFolders.sortedByDescending { it.lastModified() }
            Constants.SORT_DATE_OLD -> nonFolders.sortedBy { it.lastModified() }
            Constants.SORT_SIZE_LARGE -> nonFolders.sortedByDescending { it.length() }
            Constants.SORT_SIZE_SMALL -> nonFolders.sortedBy { it.length() }
            Constants.SORT_TYPE -> nonFolders.sortedBy { FileUtils.getFileExtension(it.name ?: "") }
            else -> nonFolders.sortedBy { (it.name ?: "").lowercase() }
        }

        val result = folders + sortedNonFolders
        fileAdapter.submitList(result)
        showEmptyState(result.isEmpty(), if (allFiles.isEmpty()) "Folder is empty" else "No matching files found")
    }

    private fun showEmptyState(show: Boolean, message: String) {
        binding.emptyContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.emptyText.text = message
        binding.recyclerFiles.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(requireContext(), isGridView, object : FileAdapter.FileClickListener {
            override fun onItemClick(file: DocumentFile) {
                if (file.isDirectory) {
                    currentDirectory?.let { folderStack.add(it) }
                    currentDirectory = file
                    loadCurrentDirectory()
                } else {
                    (activity as? MainActivity)?.openDocument(
                        file.uri,
                        file.name ?: "Document",
                        file.type ?: FileUtils.getMimeType(file.name ?: "")
                    )
                }
            }

            override fun onItemLongClick(file: DocumentFile) {
                showFileContextMenu(file)
            }

            override fun onToggleFavourite(file: DocumentFile) {
                toggleFavourite(file)
            }
        })
        binding.recyclerFiles.layoutManager = if (isGridView) GridLayoutManager(context, 1) else LinearLayoutManager(context)
        binding.recyclerFiles.adapter = fileAdapter
        binding.recyclerFiles.doOnLayout {
            if (isGridView) {
                (binding.recyclerFiles.layoutManager as? GridLayoutManager)?.spanCount = gridSpanCount()
            }
        }
    }

    private fun refreshFavourites() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val favs = favouriteDao.getAllSync()
            val paths = favs.map { it.filePath }.toSet()
            withContext(Dispatchers.Main) {
                fileAdapter.setFavourites(paths)
            }
        }
    }

    private fun toggleFavourite(file: DocumentFile) {
        val path = file.uri.toString()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val isFav = favouriteDao.isFavourite(path)
            if (isFav) {
                favouriteDao.deleteByPath(path)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Removed from favourites", Toast.LENGTH_SHORT).show()
                }
            } else {
                val entity = FavouriteEntity(
                    filePath = path,
                    fileName = file.name ?: "Unknown",
                    fileType = if (file.isDirectory) "folder" else FileUtils.getFileExtension(file.name ?: ""),
                    isDirectory = file.isDirectory,
                    addedAt = System.currentTimeMillis()
                )
                favouriteDao.insert(entity)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Added to favourites", Toast.LENGTH_SHORT).show()
                }
            }
            refreshFavourites()
        }
    }

    private fun showFileContextMenu(file: DocumentFile) {
        val name = file.name ?: "File"
        val isFav = favouriteDao.isFavourite(file.uri.toString())
        val options = mutableListOf(
            "Open",
            "Rename",
            "Delete",
            "Share",
            if (isFav) "Remove from Favourites" else "Add to Favourites"
        )

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_DarkReader_Dialog)
            .setTitle(name)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        if (file.isDirectory) {
                            currentDirectory?.let { folderStack.add(it) }
                            currentDirectory = file
                            loadCurrentDirectory()
                        } else {
                            (activity as? MainActivity)?.openDocument(file.uri, name, file.type ?: "*/*")
                        }
                    }
                    1 -> showRenameDialog(file)
                    2 -> showDeleteDialog(file)
                    3 -> shareFile(file)
                    4 -> toggleFavourite(file)
                }
            }
            .show()
    }

    private fun showRenameDialog(file: DocumentFile) {
        val input = EditText(requireContext()).apply {
            setText(file.name)
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_DarkReader_Dialog)
            .setTitle("Rename")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != file.name) {
                    try {
                        val success = file.renameTo(newName)
                        if (success) {
                            Toast.makeText(requireContext(), "Renamed successfully", Toast.LENGTH_SHORT).show()
                            loadCurrentDirectory()
                        } else {
                            Toast.makeText(requireContext(), "Rename failed", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(file: DocumentFile) {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_DarkReader_Dialog)
            .setTitle("Delete ${file.name}?")
            .setMessage("Are you sure you want to permanently delete this file?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    val deleted = file.delete()
                    if (deleted) {
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                        loadCurrentDirectory()
                    } else {
                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareFile(file: DocumentFile) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = file.type ?: FileUtils.getMimeType(file.name ?: "")
                putExtra(Intent.EXTRA_STREAM, file.uri)
                clipData = android.content.ClipData.newRawUri("document", file.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCreateFolderDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Folder Name"
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_DarkReader_Dialog)
            .setTitle("New Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val created = currentDirectory?.createDirectory(name)
                    if (created != null) {
                        Toast.makeText(requireContext(), "Folder created", Toast.LENGTH_SHORT).show()
                        loadCurrentDirectory()
                    } else {
                        Toast.makeText(requireContext(), "Could not create folder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf(
            "Name (A to Z)",
            "Name (Z to A)",
            "Date (Newest First)",
            "Date (Oldest First)",
            "Size (Largest First)",
            "Size (Smallest First)",
            "Type"
        )
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_DarkReader_Dialog)
            .setTitle("Sort By")
            .setSingleChoiceItems(options, currentSortOrder) { dialog, which ->
                currentSortOrder = which
                prefs.edit().putInt(Constants.PREF_SORT_ORDER, currentSortOrder).apply()
                filterAndSortFiles(binding.searchEdit.text?.toString())
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Grid density follows the actual available width instead of a phone-only fixed span count. */
    private fun gridSpanCount(): Int {
        val minItemWidth = resources.getDimensionPixelSize(R.dimen.grid_min_item_width)
        val availableWidth = binding.recyclerFiles.width.takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        return (availableWidth / minItemWidth).coerceAtLeast(2)
    }

    fun navigateUp(): Boolean {
        if (folderStack.isNotEmpty()) {
            currentDirectory = folderStack.removeAt(folderStack.size - 1)
            loadCurrentDirectory()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
