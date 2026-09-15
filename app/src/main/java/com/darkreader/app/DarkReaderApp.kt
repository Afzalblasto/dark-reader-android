package com.darkreader.app

import android.app.Application
import com.darkreader.app.util.PdfUtils

class DarkReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox resource loader
        PdfUtils.init(this)
    }
}
