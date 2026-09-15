package com.darkreader.app.ui.safe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.darkreader.app.R
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.SafeFileEntity
import com.darkreader.app.data.entity.SettingEntity
import com.darkreader.app.databinding.ActivitySafeBinding
import com.darkreader.app.databinding.DialogSafePinBinding
import com.darkreader.app.ui.viewer.CbzViewerActivity
import com.darkreader.app.ui.viewer.ImageViewerActivity
import com.darkreader.app.ui.viewer.PdfViewerActivity
import com.darkreader.app.util.FileUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class SafeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySafeBinding
    private lateinit var adapter: SafeFileAdapter
    private val safeFiles = mutableListOf<SafeFileEntity>()
    private val safeDao by lazy { AppDatabase.getInstance(this).safeFileDao() }
    private val settingDao by lazy { AppDatabase.getInstance(this).settingDao() }

    private val addFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importFileToSafe(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySafeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = getString(R.string.private_safe)
        binding.toolbar.setNavigationIcon(R.drawable.ic_back)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()

        binding.fabAdd.setOnClickListener {
            addFileLauncher.launch(arrayOf("application/pdf", "application/x-cbz", "image/*", "*/*"))
        }

        checkAuth()
    }

    private fun setupRecyclerView() {
        adapter = SafeFileAdapter(
            safeFiles = safeFiles,
            onFileClicked = { file -> openSafeFile(file) },
            onFileLongClicked = { file -> confirmDelete(file) }
        )
        binding.recyclerSafe.layoutManager = LinearLayoutManager(this)
        binding.recyclerSafe.adapter = adapter
    }

    private fun checkAuth() {
        lifecycleScope.launch(Dispatchers.IO) {
            val savedHash = settingDao.get("safe_pin_hash")
            withContext(Dispatchers.Main) {
                if (savedHash == null) {
                    showSetPinDialog()
                } else {
                    showPinDialog(savedHash)
                }
            }
        }
    }

    private fun showSetPinDialog() {
        val dialogBinding = DialogSafePinBinding.inflate(LayoutInflater.from(this))
        dialogBinding.pinTitle.text = getString(R.string.set_pin)
        dialogBinding.btnBiometric.visibility = View.GONE

        MaterialAlertDialogBuilder(this, R.style.Theme_DarkReader_Dialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                val pin = dialogBinding.editPin.text.toString()
                if (pin.length >= 4) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        settingDao.insert(SettingEntity("safe_pin_hash", hashPin(pin)))
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SafeActivity, "PIN set successfully", Toast.LENGTH_SHORT).show()
                            loadSafeFiles()
                        }
                    }
                } else {
                    Toast.makeText(this, getString(R.string.pin_too_short), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .show()
    }

    private fun showPinDialog(savedHash: String) {
        val dialogBinding = DialogSafePinBinding.inflate(LayoutInflater.from(this))
        dialogBinding.pinTitle.text = getString(R.string.enter_pin)

        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_DarkReader_Dialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                val pin = dialogBinding.editPin.text.toString()
                if (hashPin(pin) == savedHash) {
                    loadSafeFiles()
                } else {
                    Toast.makeText(this, getString(R.string.wrong_pin), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .create()

        dialogBinding.btnBiometric.setOnClickListener {
            dialog.dismiss()
            showBiometricPrompt()
        }

        dialog.show()
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                loadSafeFiles()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                checkAuth()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_auth))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun loadSafeFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            val files = safeDao.getAllSync()
            withContext(Dispatchers.Main) {
                safeFiles.clear()
                safeFiles.addAll(files)
                adapter.notifyDataSetChanged()
                binding.emptyText.visibility = if (safeFiles.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun importFileToSafe(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = FileUtils.getUriFileName(this@SafeActivity, uri) ?: "file"
                val fileType = FileUtils.getFileType(fileName)
                val safeDir = File(filesDir, "safe").apply { mkdirs() }
                val storedName = UUID.randomUUID().toString() + "." + FileUtils.getFileExtension(fileName)
                val destFile = File(safeDir, storedName)

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val safeEntity = SafeFileEntity(
                    originalName = fileName,
                    storedName = storedName,
                    fileType = fileType,
                    fileSize = destFile.length(),
                    addedAt = System.currentTimeMillis()
                )
                safeDao.insert(safeEntity)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SafeActivity, "Added to Safe", Toast.LENGTH_SHORT).show()
                    loadSafeFiles()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openSafeFile(file: SafeFileEntity) {
        val safeFile = File(File(filesDir, "safe"), file.storedName)
        if (!safeFile.exists()) return

        val contentUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            safeFile
        )

        val intent = when {
            FileUtils.isPdfFile(file.originalName) -> Intent(this, PdfViewerActivity::class.java)
            FileUtils.isCbzFile(file.originalName) -> Intent(this, CbzViewerActivity::class.java)
            FileUtils.isImageFile(file.originalName) -> Intent(this, ImageViewerActivity::class.java)
            else -> null
        }

        intent?.let {
            it.putExtra("uri", contentUri.toString())
            it.putExtra("name", file.originalName)
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(it)
        }
    }

    private fun confirmDelete(file: SafeFileEntity) {
        MaterialAlertDialogBuilder(this, R.style.Theme_DarkReader_Dialog)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_confirm, file.originalName))
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    safeDao.delete(file)
                    File(File(filesDir, "safe"), file.storedName).delete()
                    loadSafeFiles()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
