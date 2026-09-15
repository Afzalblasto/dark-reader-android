# DarkReader - Repair & Implementation TODO List

## Priority 1: Storage, File Access & File Browser Repair
- [ ] In `HomeFragment.kt`:
  - [ ] Add explicit "Select Root Folder" button when no folder is selected or when user wants to change folder.
  - [x] Safely require and validate a persistable tree permission; do not save inaccessible roots.
  - [ ] Implement filename search filtering in `searchEdit`.
  - [ ] Implement sorting by Name (A-Z, Z-A), Date (newest, oldest), Size, and Type.
  - [ ] Hook up `btnViewMode` properly and persist preference.
  - [ ] Implement long-press context menu on items: Open, Rename, Delete, Share, Add to Favourites.
  - [ ] Hook up `fabAdd` to create new folders via `DocumentFile.createDirectory()`.
- [ ] In `FileAdapter.kt`:
  - [ ] Fix blank list items: bind filename, formatted file size, last modified date, and icons.
  - [ ] Generate real asynchronous thumbnails for PDFs, CBZ, and images.
  - [ ] Display favourite star indicator on items.
- [ ] In `MainActivity.kt`:
  - [ ] Propagate `Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION` when launching viewers and editors.
  - [ ] Add intent filters in `AndroidManifest.xml` for CBZ and image MIME types.
  - [ ] In `MainActivity.onCreate()` and `onNewIntent()`, check incoming intent for `ACTION_VIEW` and `ACTION_SEND` to open documents directly.

## Priority 2: Viewer Enhancements (Pinch-to-Zoom & Thread Safety)
- [ ] Create `ZoomableImageView.kt` (custom ImageView with ScaleGestureDetector and Matrix pan/zoom) to enable pinch-to-zoom across PDF, CBZ, and image viewers.
- [ ] In `PdfPageAdapter.kt`:
  - [ ] Add thread synchronization (`synchronized(lock)`) around `PdfRenderer.openPage()`, render, and `page.close()`.
  - [ ] Support zoom inside `page_view.xml`.
- [ ] In `PdfViewerActivity.kt`:
  - [ ] Hook up `btnThumbnails` to open `PageEditorActivity`.
- [ ] In `CbzUtils.kt`:
  - [ ] Implement efficient session caching for CBZ files: extract page entries to disk cache on-demand rather than re-reading the entire ZIP stream twice per page.
- [ ] In `CbzViewerActivity.kt`:
  - [ ] Hook up `btnThumbnails` to open `PageEditorActivity`.

## Priority 3: Settings Access & Navigation Repair
- [ ] Add Settings action button in `HomeFragment` toolbar so Settings is accessible immediately from the home screen.
- [ ] In `SettingsActivity.kt`, hook up toolbar navigation icon click to `finish()`.
- [x] Separate Settings ListPreference keys from Home's typed keys and normalize legacy values.
- [ ] Verify on a physical device: sort order, view mode, safe PIN, biometric toggle, and export/import bookmarks.

## Priority 4: Complete Feature Activities (Page Editor & Converters)
- [ ] In `PageEditorActivity.kt` and `PageThumbnailAdapter.kt`:
  - [ ] Load and display thumbnails for PDF or CBZ.
  - [ ] Implement page reordering via ItemTouchHelper drag-and-drop.
  - [ ] Implement page deletion.
  - [ ] Implement page rotation (90°, 180°, 270°).
  - [ ] Implement inserting JPG/PNG image pages.
  - [ ] Implement importing pages from another PDF.
  - [ ] Implement Save Behavior dialog: "Save changes to original file? ALLOW / DON'T ALLOW".
- [ ] In `MergePdfActivity.kt`:
  - [ ] Implement file picker for multiple PDFs.
  - [ ] Implement drag-and-drop reordering.
  - [ ] Implement merge using `PdfUtils.mergePdfs`.
- [ ] In `ImageToPdfActivity.kt`:
  - [ ] Pick multiple images, reorder, and compile into PDF.
- [ ] In `ImageToCbzActivity.kt`:
  - [ ] Pick multiple images, reorder, and pack into CBZ ZIP archive.
- [ ] In `PdfToImageActivity.kt`:
  - [ ] Export whole document or selected pages to JPG / PNG images in storage.
- [ ] In `CbzToImageActivity.kt`:
  - [ ] Export comic pages to JPG / PNG images.

## Priority 5: Verification & Packaging
- [x] Release build with Gradle (`:app:assembleRelease`).
- [x] Generate release APK `DarkReader.apk`.
- [ ] Install and execute the device test plan; no connected Android device/emulator was available in this workspace.
- [ ] Reproduce the former close/crash flow with logcat attached on a device and retain the first fatal stack trace if one remains.
- [ ] Exercise portrait, landscape, split-screen/foldable resize, and tablet configurations from the responsive UI checklist.
- [ ] Run functional test suite and verify APK structure.
