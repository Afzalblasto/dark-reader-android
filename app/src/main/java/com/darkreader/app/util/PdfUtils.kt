package com.darkreader.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfUtils {

    fun init(context: Context) {
        PDFBoxResourceLoader.init(context)
    }

    fun getPageCount(context: Context, uri: Uri): Int {
        var document: PDDocument? = null
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                document = PDDocument.load(stream)
                document?.numberOfPages ?: 0
            } ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            document?.close()
        }
    }

    fun renderPage(context: Context, uri: Uri, pageIndex: Int, width: Int): Bitmap? {
        return try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(fd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                fd.close()
                return null
            }
            val page = renderer.openPage(pageIndex)
            val height = (width.toFloat() / page.width * page.height).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun mergePdfs(context: Context, inputUris: List<Uri>, outputStream: OutputStream) {
        val mergerUtility = PDFMergerUtility()
        try {
            val document = PDDocument()
            for (uri in inputUris) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val srcDoc = PDDocument.load(stream)
                    mergerUtility.appendDocument(document, srcDoc)
                    srcDoc.close()
                }
            }
            document.save(outputStream)
            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun rearrangePages(context: Context, inputUri: Uri, newOrder: List<Int>, outputStream: OutputStream) {
        var sourceDoc: PDDocument? = null
        val targetDoc = PDDocument()
        try {
            context.contentResolver.openInputStream(inputUri)?.use { stream ->
                sourceDoc = PDDocument.load(stream)
                for (pageIndex in newOrder) {
                    if (pageIndex >= 0 && pageIndex < (sourceDoc?.numberOfPages ?: 0)) {
                        val page = sourceDoc?.getPage(pageIndex)
                        if (page != null) {
                            targetDoc.importPage(page)
                        }
                    }
                }
                targetDoc.save(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            sourceDoc?.close()
            targetDoc.close()
        }
    }

    fun deletePages(context: Context, inputUri: Uri, pagesToDelete: Set<Int>, outputStream: OutputStream) {
        var sourceDoc: PDDocument? = null
        val targetDoc = PDDocument()
        try {
            context.contentResolver.openInputStream(inputUri)?.use { stream ->
                sourceDoc = PDDocument.load(stream)
                val totalPages = sourceDoc?.numberOfPages ?: 0
                for (i in 0 until totalPages) {
                    if (!pagesToDelete.contains(i)) {
                        val page = sourceDoc?.getPage(i)
                        if (page != null) {
                            targetDoc.importPage(page)
                        }
                    }
                }
                targetDoc.save(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            sourceDoc?.close()
            targetDoc.close()
        }
    }

    fun rotatePages(context: Context, inputUri: Uri, rotations: Map<Int, Int>, outputStream: OutputStream) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(inputUri)?.use { stream ->
                document = PDDocument.load(stream)
                for ((pageIndex, degrees) in rotations) {
                    if (pageIndex >= 0 && pageIndex < (document?.numberOfPages ?: 0)) {
                        val page = document?.getPage(pageIndex)
                        page?.rotation = (page?.rotation ?: 0) + degrees
                    }
                }
                document?.save(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document?.close()
        }
    }

    fun addImagePages(context: Context, pdfUri: Uri, imageUris: List<Uri>, position: Int, outputStream: OutputStream) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(pdfUri)?.use { stream ->
                document = PDDocument.load(stream)
                var currentPos = position
                if (currentPos < 0) currentPos = 0
                if (currentPos > (document?.numberOfPages ?: 0)) currentPos = document?.numberOfPages ?: 0
                
                for (imageUri in imageUris) {
                    context.contentResolver.openInputStream(imageUri)?.use { imgStream ->
                        val bitmap = BitmapFactory.decodeStream(imgStream)
                        if (bitmap != null) {
                            val page = PDPage()
                            document?.pages?.insertBefore(page, if (currentPos < (document?.numberOfPages ?: 0)) document?.getPage(currentPos) else null)
                            if (currentPos == (document?.numberOfPages ?: 0)) {
                                document?.addPage(page)
                            }
                            
                            val baos = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                            val pdImage = PDImageXObject.createFromByteArray(document, baos.toByteArray(), "image")
                            
                            val contentStream = PDPageContentStream(document, page)
                            contentStream.drawImage(pdImage, 0f, 0f, page.mediaBox.width, page.mediaBox.height)
                            contentStream.close()
                            
                            currentPos++
                        }
                    }
                }
                document?.save(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document?.close()
        }
    }

    fun importPagesFromPdf(context: Context, targetUri: Uri, sourceUri: Uri, pages: List<Int>, position: Int, outputStream: OutputStream) {
        var targetDoc: PDDocument? = null
        var sourceDoc: PDDocument? = null
        try {
            val targetStream = context.contentResolver.openInputStream(targetUri)
            targetDoc = if (targetStream != null) PDDocument.load(targetStream) else PDDocument()
            targetStream?.close()
            
            context.contentResolver.openInputStream(sourceUri)?.use { sStream ->
                sourceDoc = PDDocument.load(sStream)
                var currentPos = position
                if (currentPos < 0) currentPos = 0
                if (currentPos > (targetDoc.numberOfPages)) currentPos = targetDoc.numberOfPages
                
                for (pageIndex in pages) {
                    if (pageIndex >= 0 && pageIndex < sourceDoc!!.numberOfPages) {
                        val pageToImport = sourceDoc!!.getPage(pageIndex)
                        val importedPage = targetDoc.importPage(pageToImport)
                        if (currentPos < targetDoc.numberOfPages) {
                            targetDoc.pages.insertBefore(importedPage, targetDoc.getPage(currentPos))
                        } else {
                            targetDoc.addPage(importedPage)
                        }
                        currentPos++
                    }
                }
            }
            targetDoc.save(outputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            targetDoc?.close()
            sourceDoc?.close()
        }
    }

    fun pdfToImages(context: Context, pdfUri: Uri, pages: List<Int>, format: String, quality: Int, outputDir: File): List<File> {
        val resultFiles = mutableListOf<File>()
        if (!outputDir.exists()) outputDir.mkdirs()
        
        try {
            val fd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return resultFiles
            val renderer = PdfRenderer(fd)
            
            val compressFormat = if (format.lowercase() == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val ext = if (format.lowercase() == "png") "png" else "jpg"
            
            for (pageIndex in pages) {
                if (pageIndex >= 0 && pageIndex < renderer.pageCount) {
                    val page = renderer.openPage(pageIndex)
                    val width = page.width * 2 // Increase resolution for better output
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    
                    val file = File(outputDir, "page_${pageIndex + 1}.$ext")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(compressFormat, quality, out)
                    }
                    resultFiles.add(file)
                    page.close()
                }
            }
            renderer.close()
            fd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultFiles
    }

    fun imagesToPdf(context: Context, imageUris: List<Uri>, outputStream: OutputStream) {
        val document = PDDocument()
        try {
            for (uri in imageUris) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val page = PDPage()
                        document.addPage(page)
                        
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                        val pdImage = PDImageXObject.createFromByteArray(document, baos.toByteArray(), "image")
                        
                        val contentStream = PDPageContentStream(document, page)
                        contentStream.drawImage(pdImage, 0f, 0f, page.mediaBox.width, page.mediaBox.height)
                        contentStream.close()
                    }
                }
            }
            document.save(outputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }
    }
}
