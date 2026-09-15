# DarkReader - Functional Test Plan

This document outlines the test cases and verification steps for each feature in DarkReader.

---

## 1. Storage & File Browser Tests
- [ ] **TC-STORE-01: First Run Folder Selection**
  - Launch app on clean state.
  - Verify prompt or UI invites user to select a folder.
  - Select an external folder containing mixed files (`.pdf`, `.cbz`, `.jpg`, `.png`, and subfolders).
  - Verify directory contents display with filenames, sizes, and proper icons.
- [ ] **TC-STORE-02: Folder Navigation**
  - Tap a subfolder: verify navigation updates breadcrumb and loads children.
  - Tap Back button: verify returns to parent folder.
  - At root level, tap Back: verify app does not crash.
- [ ] **TC-STORE-03: Search Filtering**
  - Type query into search bar.
  - Verify list updates in real time, matching only filenames containing the query.
- [ ] **TC-STORE-04: Sorting**
  - Test sorting: Name A-Z, Name Z-A, Date Newest, Date Oldest, Size, Type.
  - Verify items reorder correctly and preference is remembered after app restart.
- [ ] **TC-STORE-05: Grid / List View Toggle**
  - Tap view mode button: verify view toggles between multi-column grid and single-column list.
- [ ] **TC-STORE-06: File Context Actions**
  - Long-press a file: test Rename, Delete, Share, and Add/Remove from Favourites.

---

## 2. PDF Reader Tests
- [ ] **TC-PDF-01: Document Loading**
  - Tap a PDF in the file browser.
  - Verify `PdfViewerActivity` opens with filename and page 1 rendered.
- [ ] **TC-PDF-02: Navigation & Swiping**
  - Swipe horizontally between pages.
  - Verify no crashes from concurrent `PdfRenderer` usage.
  - Test Next Page and Prev Page buttons.
  - Verify page indicator (`Page X of Y`) updates accurately.
- [ ] **TC-PDF-03: Jump to Page**
  - Tap page number: enter a target page (e.g. page 15).
  - Verify viewer jumps directly to that page.
- [ ] **TC-PDF-04: Pinch-to-Zoom**
  - Pinch to zoom on page: verify smooth scale-up and pan around page.
  - Double-tap: verify zoom reset / fit-to-screen.
- [ ] **TC-PDF-05: Bookmarks in Reader**
  - Tap bookmark button: verify icon turns cyan.
  - Move to another page and tap next/prev bookmark buttons: verify jumps to bookmarked page.

---

## 3. CBZ Reader Tests
- [ ] **TC-CBZ-01: Opening CBZ Archive**
  - Tap a `.cbz` file in the file browser.
  - Verify `CbzViewerActivity` opens and page 1 displays.
- [ ] **TC-CBZ-02: Page Navigation & Swiping**
  - Swipe forward and backward through comic pages.
  - Verify fast caching without repeatedly parsing the ZIP stream.
- [ ] **TC-CBZ-03: Zoom & Jump**
  - Test pinch-to-zoom on comic page.
  - Test jump to page dialog.

---

## 4. Multi-Document Tabs Tests
- [ ] **TC-TAB-01: Tab Auto-Creation**
  - Open a PDF, then return and open a CBZ.
  - Switch to Tabs bottom navigation.
  - Verify both documents appear as cards with titles and previews.
- [ ] **TC-TAB-02: Active Tab Indication**
  - Verify the currently active tab has a distinct cyan border.
- [ ] **TC-TAB-03: Tab Reordering**
  - Long press and drag a tab card to a new position.
  - Verify position updates in database.
- [ ] **TC-TAB-04: Tab Close Actions**
  - Test individual tab close (X button).
  - Test "Close Other Tabs" and "Close All Tabs".
  - Verify documents themselves are not deleted.

---

## 5. Favourites & Bookmarks Tests
- [ ] **TC-FAV-01: Add/Remove Favourite**
  - Star a file from browser or long-press context menu.
  - Go to Favourites tab: verify file appears.
  - Tap favourite item: verify document opens directly.
  - Remove favourite: verify removed from list without deleting file.
- [ ] **TC-BM-01: Global Bookmarks & Jump**
  - Go to Bookmarks tab: verify all created bookmarks appear grouped by document.
  - Tap a bookmark: verify document opens directly to bookmarked page.
- [ ] **TC-BM-02: Bookmark JSON Backup**
  - Tap Export Bookmarks: save JSON via SAF file picker.
  - Clear bookmarks or test on another device.
  - Tap Import Bookmarks: select exported JSON.
  - Verify bookmarks restore with 100% fidelity.

---

## 6. Private Safe Tests
- [ ] **TC-SAFE-01: PIN Setup & Auth**
  - First open: set a 4+ digit PIN.
  - Exit and reopen: verify PIN challenge dialog appears.
  - Enter wrong PIN: verify access denied.
  - Enter correct PIN: verify safe files load.
- [ ] **TC-SAFE-02: Adding & Opening Files**
  - Tap FAB (+): import a document into the safe.
  - Verify file copies to sandboxed internal storage and displays in safe list.
  - Tap to open: verify opens in reader via FileProvider.

---

## 7. Page Editor & Manipulation Tests
- [ ] **TC-EDIT-01: Page Grid & Selection**
  - From reader, tap thumbnail grid button.
  - Verify thumbnail grid of all pages displays.
  - Tap pages to select.
- [ ] **TC-EDIT-02: Delete & Rotate**
  - Select pages and tap Delete: verify pages removed from preview list.
  - Select pages and tap Rotate: verify pages rotate 90 degrees.
- [ ] **TC-EDIT-03: Reorder Pages**
  - Drag and drop page thumbnails to change order.
- [ ] **TC-EDIT-04: Insert Pages**
  - Tap "Add Images": pick JPG/PNG files and verify inserted at target position.
  - Tap "Add from PDF": pick external PDF, select pages, and verify inserted.
- [ ] **TC-EDIT-05: Save Prompt & Non-destructive Option**
  - Tap Save: verify prompt *"Save changes to original file? Allow / Don't Allow"*.
  - Test "Allow": original file replaced.
  - Test "Don't Allow": saved as `<name>_modified.pdf`.

---

## 8. Converters & Merging Tests
- [ ] **TC-CONV-01: PDF Merge**
  - Select 2+ PDFs, reorder, tap Merge: verify unified PDF created.
- [ ] **TC-CONV-02: Image to PDF / Image to CBZ**
  - Select multiple images, reorder, tap Create: verify valid PDF/CBZ created.
- [ ] **TC-CONV-03: Document to Images**
  - Export PDF/CBZ pages to JPG/PNG image files in storage.

---

## 9. External Opening & Intent Tests
- [ ] **TC-EXT-01: Android "Open With" File Association**
  - Open PDF or CBZ from external file manager or browser.
  - Select DarkReader.
  - Verify DarkReader launches directly into reader with document opened and URI permission preserved.

## 10. Settings regression tests
- [ ] **TC-SET-01: Open Settings** — Launch Home, tap the Settings icon, verify the dark Settings screen opens and toolbar Back returns to Home.
- [ ] **TC-SET-02: Preference types** — Change sort order and view mode, return Home, restart the app, and verify the selected ordering/view are applied without a crash.
- [ ] **TC-SET-03: Legacy upgrade** — Install over a build that used the former Settings keys, then open Home and Settings; verify neither crashes and Home retains a valid view/sort preference.

## 11. Stability regression flow
- [ ] Launch → browse a large folder → scroll → PDF → Back → second PDF → Back → CBZ → Back → image → Back; repeat five times with logcat attached. Verify no fatal exception or binding/lifecycle warning.
- [ ] Background/foreground the app while a folder and while a reader page is loading. Verify no stale view update or crash.
- [ ] Rotate reader and Home in both directions. Verify the app does not close and content reloads safely.

## 12. Responsive UI checklist
- [ ] **Phone portrait:** Home, file browser, PDF/CBZ/image readers, tabs, favourites, bookmarks, Settings, Safe, editor, and conversion screens have no clipping or oversized chrome.
- [ ] **Phone landscape:** Reader page remains centered and proportional; filename ellipsizes; controls remain visible and tappable.
- [ ] **Tablet portrait/landscape:** File and tab grids gain appropriate columns; screens use width without stretching phone cards; reader remains document-first.
- [ ] **Accessibility:** Test 1.0x and increased system font scale; long filenames remain ellipsized and do not overlap controls.
