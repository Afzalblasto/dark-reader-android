# DarkReader - Changelog

## [Initial Generation]
- Set up Android Gradle Plugin 8.5.2 with Kotlin 2.0.0 and KSP.
- Configured compileSdk 37 and local Android SDK platform directory junction.
- Implemented Room database schema with 5 tables (`bookmarks`, `favourites`, `tabs`, `safe_files`, `settings`).
- Created core Dark theme resources with Cyan accent color.
- Implemented basic ViewPager2 readers for PDF and CBZ.
- Implemented Private Safe with PIN hash and Biometric prompt.
- Implemented Bookmark JSON export and import.
- Resolved Gradle JVM 25 incompatibility by using Eclipse Temurin JDK 21.0.12.1.
- Resolved missing preferences array resources (`arrays.xml`).
- Corrected PDFBox Android Maven coordinate to `com.tom-roush:pdfbox-android:2.0.27.0`.
- Built initial release APK (`14.2 MB`).

## [Master Repair Session - In Progress]
- Audited entire project codebase across all 48 source files, manifests, and layouts.
- Identified root causes for file browsing failure, invisible list items, `PdfRenderer` thread crashes, CBZ performance bottlenecks, and missing Settings access.
- Created `PROJECT_STATUS.md`, `REQUIREMENTS.md`, `TODO.md`, `TEST_PLAN.md`.
- Scheduled repairs for file access, viewer synchronization, pinch-to-zoom, page editor, and converters.

## [2026-09-05] Foundational access and Settings repair
- Removed legacy external-storage mode from the application configuration; normal browsing remains SAF-based.
- Root folder URIs are now persisted only when the provider grants persistent read/write access and the selected tree can be read; stale saved roots are discarded safely.
- Incoming `ACTION_VIEW` / `ACTION_SEND` URIs retain persistable grants when the source provider offers them.
- Fixed Settings/Home preference-key type collision that could throw `ClassCastException` after changing sort or view mode; added legacy preference migration.
- Prevented stale PDF/CBZ background renders from being assigned to recycled page views and limited CBZ extracted-page cache to 12 pages.
- Repaired pre-existing Kotlin compile blockers in Page Editor's save-dialog title and Home's Material sort-dialog API call.
- Built and package-inspected the single release APK at `F:\New folder (4)\pdf\DarkReader.apk`.

## [2026-09-05] Stability and adaptive layout repair
- Fixed view-lifecycle misuse in Home, Tabs, Bookmarks, and Favourites that could update null view bindings after a configuration change or rapid navigation.
- Cancel reader render work during teardown and prevent stale page render assignments.
- Reworked CBZ thumbnail decoding to avoid full-page byte-array allocations.
- Replaced fixed Home/Tab grid spans with width-derived spans and added tablet resource qualifiers.
- Compacted PDF/CBZ reader chrome so the document receives more vertical space.
- Built the updated universal release APK (`14,971,744` bytes; SHA-256 `EF7588B390AE14753550B426987B6159471E670F24762C20D2DC45A4F30A6040`).

## 2026-09-10 - Direct source repair pass
- Hardened PDF reader lifecycle and URI opening; page-change callback is explicitly registered/unregistered and document resources are closed deterministically.
- Limited PDF/CBZ rendering concurrency and ViewPager offscreen pages to reduce memory pressure.
- Bound PDF/CBZ page render jobs to RecyclerView holder lifetime so recycled pages cannot receive stale bitmaps.
- PDF/CBZ page render width now uses the measured reader viewport when available and is capped to avoid excessive bitmap allocation on high-resolution tablets.
- Hardened CBZ viewer bookmark loading and lifecycle cleanup.
- Reworked file and tab thumbnail loading to use bounded adapter-owned coroutine scopes and cancellation on recycling/detachment, reducing thumbnail-related memory pressure and stale UI updates.
- Made reader chrome more compact and document-focused while retaining the dark/cyan design.
- Added dynamic grid span recalculation after RecyclerView measurement for Home and Tabs.
- Replaced the reader image reset/zoom implementation with a viewport-aware fit-to-page transform that recalculates after size changes.
- Settings PIN persistence now uses the fragment view lifecycle scope rather than an unmanaged coroutine.

Device-level crash reproduction was not possible in this environment; the remaining requirement is to install the resulting APK on the user's phone and capture Logcat if the crash persists.
