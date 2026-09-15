package com.darkreader.app.ui.tools

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.darkreader.app.databinding.FragmentToolsBinding
import com.darkreader.app.ui.convert.*
import com.darkreader.app.ui.safe.SafeActivity
import com.darkreader.app.ui.settings.SettingsActivity

class ToolsFragment : Fragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val exportBookmarksLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            com.darkreader.app.util.BookmarkExporter.exportToFile(requireContext(), it)
            android.widget.Toast.makeText(requireContext(), "Bookmarks exported successfully", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private val importBookmarksLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val count = com.darkreader.app.util.BookmarkExporter.importFromFile(requireContext(), it)
            android.widget.Toast.makeText(requireContext(), "$count bookmarks imported", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardSafe.setOnClickListener { startActivity(Intent(requireContext(), SafeActivity::class.java)) }
        binding.cardMergePdf.setOnClickListener { startActivity(Intent(requireContext(), MergePdfActivity::class.java)) }
        binding.cardImageToPdf.setOnClickListener { startActivity(Intent(requireContext(), ImageToPdfActivity::class.java)) }
        binding.cardImageToCbz.setOnClickListener { startActivity(Intent(requireContext(), ImageToCbzActivity::class.java)) }
        binding.cardPdfToImage.setOnClickListener { startActivity(Intent(requireContext(), PdfToImageActivity::class.java)) }
        binding.cardCbzToImage.setOnClickListener { startActivity(Intent(requireContext(), CbzToImageActivity::class.java)) }
        binding.cardExportBookmarks.setOnClickListener { exportBookmarksLauncher.launch("DarkReader_Bookmarks.json") }
        binding.cardImportBookmarks.setOnClickListener { importBookmarksLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
        binding.cardSettings.setOnClickListener { startActivity(Intent(requireContext(), SettingsActivity::class.java)) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
