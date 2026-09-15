package com.darkreader.app.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.darkreader.app.R
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }
}
