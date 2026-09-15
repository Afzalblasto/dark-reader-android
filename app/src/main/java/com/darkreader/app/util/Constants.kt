package com.darkreader.app.util

object Constants {
    const val PREF_SORT_ORDER = "sort_order"
    const val PREF_VIEW_MODE = "view_mode"
    const val PREF_SAFE_PIN_HASH = "safe_pin_hash"
    const val PREF_SAFE_ENABLED = "safe_enabled"
    const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
    
    const val SORT_NAME_ASC = 0
    const val SORT_NAME_DESC = 1
    const val SORT_DATE_NEW = 2
    const val SORT_DATE_OLD = 3
    const val SORT_SIZE_LARGE = 4
    const val SORT_SIZE_SMALL = 5
    const val SORT_TYPE = 6
    
    const val VIEW_LIST = 0
    const val VIEW_GRID = 1
    
    val SUPPORTED_EXTENSIONS = setOf("pdf", "cbz", "jpg", "jpeg", "png")
    
    const val FILE_TYPE_PDF = "pdf"
    const val FILE_TYPE_CBZ = "cbz"
    const val FILE_TYPE_JPG = "jpg"
    const val FILE_TYPE_PNG = "png"
    const val FILE_TYPE_FOLDER = "folder"
    const val FILE_TYPE_OTHER = "other"
    
    const val SAFE_DIR_NAME = ".safe"
    
    const val REQUEST_CODE_OPEN_PDF = 1001
    const val REQUEST_CODE_OPEN_CBZ = 1002
}
