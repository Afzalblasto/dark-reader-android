package com.darkreader.app.ui.viewer

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.darkreader.app.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriStr = intent.getStringExtra("uri") ?: return
        val name = intent.getStringExtra("name") ?: "Image"
        
        binding.toolbar.title = name
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        Glide.with(this)
            .load(Uri.parse(uriStr))
            .into(binding.imageView)
    }
}
