package com.darkreader.app.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.darkreader.app.R
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.TabEntity
import com.darkreader.app.databinding.ActivityMainBinding
import com.darkreader.app.ui.bookmarks.BookmarksFragment
import com.darkreader.app.ui.favourites.FavouritesFragment
import com.darkreader.app.ui.home.HomeFragment
import com.darkreader.app.ui.tabs.TabsFragment
import com.darkreader.app.ui.tools.ToolsFragment
import com.darkreader.app.ui.viewer.CbzViewerActivity
import com.darkreader.app.ui.viewer.ImageViewerActivity
import com.darkreader.app.ui.viewer.PdfViewerActivity
import com.darkreader.app.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val homeFragment = HomeFragment()
    private val tabsFragment = TabsFragment()
    private val bookmarksFragment = BookmarksFragment()
    private val favouritesFragment = FavouritesFragment()
    private val toolsFragment = ToolsFragment()

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, toolsFragment, "5").hide(toolsFragment)
            add(R.id.fragmentContainer, favouritesFragment, "4").hide(favouritesFragment)
            add(R.id.fragmentContainer, bookmarksFragment, "3").hide(bookmarksFragment)
            add(R.id.fragmentContainer, tabsFragment, "2").hide(tabsFragment)
            add(R.id.fragmentContainer, homeFragment, "1")
            commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(homeFragment)
                R.id.nav_tabs -> switchFragment(tabsFragment)
                R.id.nav_bookmarks -> switchFragment(bookmarksFragment)
                R.id.nav_favourites -> switchFragment(favouritesFragment)
                R.id.nav_tools -> switchFragment(toolsFragment)
                else -> false
            }
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeFragment != homeFragment) {
                    binding.bottomNav.selectedItemId = R.id.nav_home
                } else {
                    if (!homeFragment.navigateUp()) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val uri: Uri? = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)

        if ((Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action) && uri != null) {
            val flags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0 && flags != 0) {
                runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
            }
            val fileName = FileUtils.getUriFileName(this, uri) ?: "Document"
            val mimeType = intent.type ?: FileUtils.getMimeType(fileName)
            openDocument(uri, fileName, mimeType)
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }

    fun openDocument(uri: Uri, fileName: String, fileType: String, startPage: Int = 0) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(this@MainActivity)
                val tab = TabEntity(
                    documentUri = uri.toString(),
                    fileName = fileName,
                    fileType = fileType,
                    lastPage = startPage,
                    openedAt = System.currentTimeMillis(),
                    isActive = true
                )
                db.tabDao().insert(tab)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val intent = when {
            FileUtils.isPdfFile(fileName) || fileType.contains("pdf", ignoreCase = true) -> {
                Intent(this, PdfViewerActivity::class.java)
            }
            FileUtils.isCbzFile(fileName) || fileType.contains("zip", ignoreCase = true) || fileType.contains("cbz", ignoreCase = true) -> {
                Intent(this, CbzViewerActivity::class.java)
            }
            FileUtils.isImageFile(fileName) || fileType.startsWith("image/") -> {
                Intent(this, ImageViewerActivity::class.java)
            }
            else -> Intent(this, PdfViewerActivity::class.java)
        }

        intent.apply {
            putExtra("uri", uri.toString())
            putExtra("name", fileName)
            putExtra("page", startPage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        startActivity(intent)
    }
}
