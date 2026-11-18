package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.SupplyBill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object BillExportManager {

    private fun sanitizedFileBase(bill: SupplyBill): String {
        val inv = bill.invoiceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "draft" }
        val party = bill.receiverName.trim().take(18).replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return if (party.isNotBlank()) "SupplyBill_${inv}_${party}" else "SupplyBill_${inv}"
    }

    fun suggestedPdfFileName(bill: SupplyBill): String = "${sanitizedFileBase(bill)}.pdf"

    fun suggestedPngFileName(bill: SupplyBill): String = "${sanitizedFileBase(bill)}.png"

    suspend fun createSharePdfFile(context: Context, bill: SupplyBill): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val outFile = File(exportDir, suggestedPdfFileName(bill))
                FileOutputStream(outFile).use { fos ->
                    BillDocumentRenderer.writePdf(bill, fos)
                }
                outFile
            }
        }

    suspend fun createSharePngFile(context: Context, bill: SupplyBill): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val outFile = File(exportDir, suggestedPngFileName(bill))
                val bitmap = BillDocumentRenderer.renderBitmap(bill)
                try {
                    FileOutputStream(outFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                } finally {
                    bitmap.recycle()
                }
                outFile
            }
        }

    fun launchShareIntent(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Saves PDF directly to Downloads/SupplyBill on Android 10+ (API 29+) using MediaStore
     * without requiring any storage permissions.
     */
    suspend fun savePdfToMediaStore(context: Context, bill: SupplyBill): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    throw UnsupportedOperationException("Use SAF launcher on Android 9 and below")
                }
                val fileName = "${sanitizedFileBase(bill)}_${System.currentTimeMillis() % 10000}.pdf"
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SupplyBill")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create PDF entry in Downloads.")
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        BillDocumentRenderer.writePdf(bill, out)
                    } ?: error("Failed to open output stream for PDF.")
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
                "Downloads/SupplyBill/$fileName"
            }
        }

    /**
     * Saves PNG directly to Pictures/SupplyBill on Android 10+ (API 29+) using MediaStore
     * without requiring any storage permissions, making it immediately visible in Gallery.
     */
    suspend fun savePngToMediaStore(context: Context, bill: SupplyBill): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    throw UnsupportedOperationException("Use SAF launcher on Android 9 and below")
                }
                val fileName = "${sanitizedFileBase(bill)}_${System.currentTimeMillis() % 10000}.png"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SupplyBill")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create PNG entry in Pictures.")
                val bitmap = BillDocumentRenderer.renderBitmap(bill)
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: error("Failed to open output stream for PNG.")
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                } finally {
                    bitmap.recycle()
                }
                "Pictures/SupplyBill/$fileName"
            }
        }

    suspend fun writePdfToUri(context: Context, uri: Uri, bill: SupplyBill): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    BillDocumentRenderer.writePdf(bill, out)
                } ?: error("Failed to open destination for PDF.")
            }
        }

    suspend fun writePngToUri(context: Context, uri: Uri, bill: SupplyBill): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = BillDocumentRenderer.renderBitmap(bill)
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: error("Failed to open destination for PNG.")
                } finally {
                    bitmap.recycle()
                }
                Unit
            }
        }
}
