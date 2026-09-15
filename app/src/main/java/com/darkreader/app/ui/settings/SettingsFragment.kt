package com.darkreader.app.ui.settings

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.lifecycle.lifecycleScope
import com.darkreader.app.R
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.SettingEntity
import com.darkreader.app.util.BookmarkExporter
import com.darkreader.app.util.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class SettingsFragment : PreferenceFragmentCompat() {

    private val exportBookmarksLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            BookmarkExporter.exportToFile(requireContext(), it)
            Toast.makeText(requireContext(), "Bookmarks exported successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private val importBookmarksLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val count = BookmarkExporter.importFromFile(requireContext(), it)
            Toast.makeText(requireContext(), "$count bookmarks imported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("settings_sort_order")?.setOnPreferenceChangeListener { _, value ->
            val sortOrder = when (value as? String) {
                "name_desc" -> Constants.SORT_NAME_DESC
                "date_new" -> Constants.SORT_DATE_NEW
                "date_old" -> Constants.SORT_DATE_OLD
                "size_large" -> Constants.SORT_SIZE_LARGE
                "size_small" -> Constants.SORT_SIZE_SMALL
                "type" -> Constants.SORT_TYPE
                else -> Constants.SORT_NAME_ASC
            }
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit().putInt(Constants.PREF_SORT_ORDER, sortOrder).apply()
            true
        }
        findPreference<ListPreference>("settings_view_mode")?.setOnPreferenceChangeListener { _, value ->
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit().putBoolean(Constants.PREF_VIEW_MODE, value == "grid").apply()
            true
        }

        findPreference<Preference>("safe_pin")?.setOnPreferenceClickListener {
            showChangePinDialog()
            true
        }

        findPreference<Preference>("export_bookmarks")?.setOnPreferenceClickListener {
            exportBookmarksLauncher.launch("DarkReader_Bookmarks.json")
            true
        }

        findPreference<Preference>("import_bookmarks")?.setOnPreferenceClickListener {
            importBookmarksLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            true
        }

        findPreference<Preference>("about_version")?.summary = "1.0.0 (DarkReader Universal)"
    }

    private fun showChangePinDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter new 4-6 digit PIN"
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_DarkReader_Dialog)
            .setTitle("Change Safe PIN")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val pin = input.text.toString().trim()
                if (pin.length >= 4) {
                    val hash = hashPin(pin)
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val db = AppDatabase.getInstance(requireContext())
                        db.settingDao().insert(SettingEntity(Constants.PREF_SAFE_PIN_HASH, hash))
                        withContext(Dispatchers.Main) {
                            if (isAdded) Toast.makeText(requireContext(), "Safe PIN updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
