# DarkReader - Complete Functional Requirements

## 1. UI & Visual Theme
- Completely dark interface throughout the entire application.
- Backgrounds: near-black / black (`#0D0D0D`, `#1A1A1A`, `#1E1E1E`). No unnecessary white backgrounds.
- Cyan borders and cyan highlights (`#00BCD4`).
- Cyan accent color across buttons, indicators, and active states.
- Dark dialogs, popups, and menus.
- Clean and lightweight design without distracting animations.

## 2. File Access & Storage
- Modern Android Storage Access Framework (SAF).
- No deprecated storage permissions for normal document access.
- Support `ACTION_OPEN_DOCUMENT` and `ACTION_OPEN_DOCUMENT_TREE`.
- Persistable URI permissions via `takePersistableUriPermission()`.
- Access and remember user-selected folders across app restarts.
- Support file formats: `.pdf`, `.cbz`, `.jpg`, `.jpeg`, `.png`.
- Graceful error handling for missing, moved, or restricted files without crashing.

## 3. File Browser
- Folder navigation with breadcrumb display.
- Folder selection & ability to pick a new folder anytime.
- Real-time search by filename.
- Sorting options: Name (A-Z, Z-A), Date Modified (newest, oldest), Size, and Type. Persist user sorting preference.
- Views: Grid view and List view modes, with user toggle and saved preference.
- File actions: Open, Rename, Delete, Share, Add/Remove Favourite.
- Meaningful thumbnails for images, PDFs, CBZ, and folder icons.

## 4. PDF Reader
- Fast native rendering using Android `PdfRenderer`.
- Memory-efficient lazy loading: support large files (up to 470 MB / 100+ pages) without out-of-memory errors.
- Navigation: swipe, previous/next buttons, page number indicator (`Page X of Y`), direct jump-to-page dialog.
- Zoom support: pinch-to-zoom, double-tap zoom, fit-to-screen, and panning.
- Page thumbnails navigation & direct jump.
- Proper thread-safe synchronization of `PdfRenderer`.
- Resource cleanup on activity destroy.

## 5. CBZ Reader
- Read ZIP archives containing images (JPG, JPEG, PNG).
- Memory-efficient caching: extract/cache pages on-demand rather than loading entire multi-hundred MB ZIP into RAM.
- Navigation: swipe, previous/next buttons, page indicator, jump to page.
- Zoom support: pinch-to-zoom and pan.
- Bookmarks support.

## 6. External File Opening (Intent Filters)
- Support Android's "Open With", "Share", and file associations for PDF, CBZ, and images.
- Correct MIME types: `application/pdf`, `application/x-cbz`, `application/vnd.comicbook+zip`, `application/zip`, `image/*`.
- Support `content://` and `file://` schemes.
- Auto-open received document directly into viewer with persistent tab registration.

## 7. Settings
- Direct accessibility from UI (Tools screen and Home toolbar).
- Working settings: Default sort order, default view mode, safe PIN management, biometric toggle, bookmark export/import shortcuts, about info.
- Fully themed in dark style.

## 8. Favourites
- Favourite files and folders.
- Lightweight metadata storage in Room database (no file duplication).
- Dedicated Favourites screen with sorting and direct opening.
- Removing a favourite must never delete the underlying file.

## 9. Bookmarks & Backup
- Page-level bookmarks storing document URI, document name, page number, label, timestamp.
- No duplicate copies of documents.
- In-reader controls: toggle bookmark on current page, jump to previous bookmark, jump to next bookmark.
- Global bookmarks list in dedicated tab.
- Bookmark Export: export bookmarks metadata as JSON.
- Bookmark Import: import and merge bookmarks from JSON without duplicates.

## 10. Multi-Document Tabs
- Multi-document tab manager with card thumbnails and filenames.
- Active tab highlighted with cyan border.
- Close tab, close other tabs, close all tabs.
- Drag-and-drop tab reordering with saved positions.
- Closing a tab never deletes the file.

## 11. PDF Page Editor & Page Import
- Page thumbnail grid with multi-selection.
- Rearrange pages via drag-and-drop.
- Delete selected pages.
- Rotate pages (90°, 180°, 270°).
- Add JPG/PNG images as new pages.
- Import pages from another selected PDF into the current document at a chosen position.
- Real PDF generation using PDFBox.

## 12. CBZ Page Editor
- Thumbnail grid with selection.
- Rearrange pages, delete pages, rotate images, insert new images.
- Rebuild physical CBZ ZIP archive with sequential naming so other comic readers preserve page order.

## 13. PDF Merging
- Select multiple PDF files.
- Reorder files in the list via drag-and-drop or position buttons.
- Merge into a single valid PDF file.

## 14. Document Conversions
- PDF → JPG / PNG (all pages or selected pages).
- CBZ → JPG / PNG (all pages or selected pages).
- JPG / PNG → PDF (with page reordering).
- JPG / PNG → CBZ (with page reordering).

## 15. Save Behavior
- On document modification (PDF / CBZ editing):
  - Prompt user: *"Save changes to the original file?"*
  - **ALLOW / YES**: overwrite original file via SAF output stream.
  - **DON'T ALLOW / NO**: save as new copy (e.g. `<filename>_modified.<ext>`).

## 16. Private Safe
- Sandboxed private storage isolated from normal gallery/browsers.
- PIN protection (minimum 4 digits) with SHA-256 hash.
- Biometric unlock (fingerprint).
- Move files into safe, open safe files, and delete/remove from safe.

## 17. Packaging & Final Delivery
- Single, universal, installable APK file (`DarkReader.apk`).
- Debug keystore signed for immediate sideloading on modern Android devices.
