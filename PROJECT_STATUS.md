# DarkReader - Project Status

## 1. Existing Architecture
- **Platform**: Android (minSdk 26, targetSdk 37, compileSdk 37).
- **Language / Toolchain**: Kotlin 2.0.0, KSP 2.0.0-1.0.24, Gradle 8.14.2, Temurin OpenJDK 21.0.12.1.
- **UI Toolkit**: Material Design Components with Android Views and ViewBinding (No Jetpack Compose).
- **Architecture Pattern**: MVVM with Room Database, Coroutines, ViewModels, and Repository pattern.
- **Themes & Styling**: Pure Dark theme (`#0D0D0D`, `#1A1A1A`, `#1E1E1E`), Cyan accents (`#00BCD4`), Cyan borders, and dark dialogs.

## 2. Dependencies
- **Core**: `androidx.core:core-ktx:1.13.1`, `androidx.appcompat:appcompat:1.7.0`, `com.google.android.material:material:1.12.0`, `androidx.constraintlayout:constraintlayout:2.1.4`
- **Architecture**: `androidx.lifecycle:lifecycle-*-ktx:2.8.4`
- **Database**: `androidx.room:room-runtime:2.6.1`, `androidx.room:room-ktx:2.6.1` with KSP compiler
- **Storage & Documents**: `androidx.documentfile:documentfile:1.0.1`
- **PDF Engine**: Android native `PdfRenderer` for rendering; `com.tom-roush:pdfbox-android:2.0.27.0` for PDF manipulation (merge, rearrange, rotate, insert)
- **CBZ Engine**: Java NIO `ZipInputStream` / `ZipOutputStream`
- **Image Engine**: `com.github.bumptech.glide:glide:4.16.0`
- **Security & Biometrics**: `androidx.biometric:biometric:1.1.0`, `androidx.security:security-crypto:1.1.0-alpha06`
- **JSON Serialization**: `com.google.code.gson:gson:2.11.0`
- **Preferences**: `androidx.preference:preference-ktx:1.2.1`

## 3. Storage & Database Architecture
- **SAF (Storage Access Framework)**: Folder selection via `ACTION_OPEN_DOCUMENT_TREE`, file selection via `ACTION_OPEN_DOCUMENT`. Persistable URI permissions using `takePersistableUriPermission`.
- **Database**: Room database `darkreader.db` with 5 entities:
  1. `bookmarks`: Document URI, display name, page number, label, timestamp.
  2. `favourites`: File path/URI, file name, file type, directory flag, timestamp.
  3. `tabs`: Document URI, file name, file type, last page, tab position, opened timestamp, active flag.
  4. `safe_files`: Original name, internal UUID filename, file type, file size, timestamp.
  5. `settings`: Key-value settings table.

## 4. Feature Audit & Status Matrix

| Feature Area | Implementation Status | Health / Issues |
| :--- | :--- | :--- |
| **Navigation** | Implemented | 5 tabs working (Home, Tabs, Bookmarks, Favourites, Tools). |
| **File Browser** | Repaired; device verification pending | Uses SAF tree URIs. The repair now stores a root only after a persistable read grant succeeds and removes stale roots on restart. |
| **PDF Viewer** | Partially working; device verification pending | Uses synchronized native `PdfRenderer` page rendering. Recycled ViewPager rows now reject stale background bitmaps. Non-seekable external providers still need device coverage. |
| **CBZ Viewer** | Partially working; device verification pending | Reads ZIP entries lazily and uses a 12-page on-disk cache window. It must still be exercised against valid, corrupt, and large CBZs. |
| **External Opening** | Repaired in source; device verification pending | `ACTION_VIEW` / `ACTION_SEND` are handled and offered persistable URI grants are retained. |
| **Settings** | Repaired in source; device verification pending | The original Settings/Home key type collision could crash Home after a setting changed. Settings now use separate UI keys and map values to Home's native types; legacy values are normalized. |
| **Tabs Management** | Implemented | Add tab, switch tab, close tab, close others, close all, reorder tabs with drag-and-drop. |
| **Bookmarks** | Implemented | Toggle bookmark, list bookmarks, jump to bookmark, prev/next bookmark. |
| **Bookmark JSON Backup** | Implemented | Full JSON export and import with deduplication via SAF. |
| **Favourites** | Implemented | Add/remove favourites, list favourites, open favourite. |
| **Private Safe** | Implemented | PIN authentication + AndroidX Biometric prompt, internal sandboxed file storage. |
| **Page Editor** | Stub | `PageEditorActivity` was a placeholder skeleton. Needs full page thumbnail grid, reorder, delete, rotate, insert. |
| **PDF Merge** | Stub | `MergePdfActivity` was an empty skeleton. Needs file selector, reordering, and PDFBox merge. |
| **Conversions** | Stubs | `ImageToPdfActivity`, `ImageToCbzActivity`, `PdfToImageActivity`, `CbzToImageActivity` were placeholders. |
| **Save Behavior** | Incomplete | Requires "Save changes to original file?" dialog with ALLOW / DON'T ALLOW (`_modified` file). |

## 5. Build & Output
- **Build Command**:
  ```powershell
  $env:JAVA_HOME = "F:\New folder (4)\pdf\jdk21\jdk-21.0.12.1+1"
  $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
  & "F:\New folder (4)\pdf\gradle-latest\gradle-8.14.2\bin\gradle.bat" assembleRelease
  ```
- **Primary APK Deliverable**: `F:\New folder (4)\pdf\DarkReader.apk` (release build verified 2026-09-05; SHA-256 `EF7588B390AE14753550B426987B6159471E670F24762C20D2DC45A4F30A6040`).
- **Gradle Release Output**: `F:\New folder (4)\pdf\DarkReader\app\build\outputs\apk\release\app-release.apk`

## 6. Confirmed Root Causes (2026-09-05)
- Folder selection saved a URI even when `takePersistableUriPermission` failed, guaranteeing loss of access after process restart.
- Settings wrote string values to `sort_order` / `view_mode`, while Home read those exact keys as an integer / Boolean. This type collision raises `ClassCastException`.
- Page render jobs were not bound to a ViewPager holder instance, so a slow render could appear on the wrong recycled page; CBZ extraction had no cache-size limit.
- Full physical-device testing is not available in this workspace. Build/package checks are recorded separately from unperformed device checks.

## 7. Direct repair pass (2026-09-10)
- PDF/CBZ reader lifecycle hardened and render concurrency bounded.
- Reader controls compacted; page rendering now uses measured viewport width when available and has a safety cap.
- File/tab thumbnail jobs are bounded and cancelled on recycling.
- Home/Tabs grid spans are recalculated after measurement.
- Settings PIN save uses view lifecycle scope.
- This pass was source-level; no physical Android device is connected in this workspace, so runtime crash verification still requires installation and Logcat.

## 8. Verification completed
- `:app:assembleRelease` completed successfully after correcting two pre-existing Kotlin compile blockers in the Page Editor and Home sort dialog.
- The APK was inspected as a ZIP: it contains Android DEX payloads (`classes.dex`, `classes2.dex`, `classes3.dex`) and packaged runtime assets.
- Installation, launch, and all interactive tests remain device-dependent and are deliberately left unchecked in `TEST_PLAN.md`.

## 8. Stability and responsive UI repair (2026-09-05)
- **Probable closure root cause:** asynchronous Fragment work used the Fragment lifecycle but dereferenced view binding after `onDestroyView`. This can produce a binding NPE during rotation, resizing, or rapid navigation. Home, Tabs, Bookmarks, and Favourites now use `viewLifecycleOwner.lifecycleScope`.
- Reader page render jobs now have cancellable adapter-owned scopes; they are cancelled before the renderer/cache is released.
- CBZ thumbnails now decode sampled streams rather than reading a complete high-resolution ZIP entry into memory.
- File and tab grids derive span counts from their measured/available width, with tablet (`sw600dp`) item dimensions. Reader chrome is compacted while preserving 40–44dp controls and document-first viewport behavior.
- No Android device/emulator or runtime logcat stream is connected to this workspace, so device-flow reproduction, rotation, and memory stress testing remain required rather than claimed complete.
