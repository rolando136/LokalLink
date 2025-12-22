package com.rolando.locallink.ui.screens


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.http.ContentType
import java.io.ByteArrayOutputStream

suspend fun uploadImageToSupabase(context: Context, uri: Uri): String =
    withContext(Dispatchers.IO) {
        try {
            val bucket = SupabaseClient.client.storage.from("posts")
            val fileName = "img_${System.currentTimeMillis()}.jpg"

            // 1. Read & Compress Image
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open image stream")

            val originalBitmap = BitmapFactory.decodeStream(stream)
            stream.close()

            // Resize if too big (Max 1024px width/height)
            val maxDimension = 1024
            val scale = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                if (ratio > 1) {
                    // Landscape: width = 1024
                    maxDimension.toFloat() / originalBitmap.width.toFloat()
                } else {
                    // Portrait: height = 1024
                    maxDimension.toFloat() / originalBitmap.height.toFloat()
                }
            } else {
                1f
            }

            val resizedBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            // Compress to JPEG 80% quality
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()

            Log.d("Upload", "Uploading $fileName (${bytes.size} bytes)...")

            // 2. Upload
            bucket.upload(
                path = fileName,
                data = bytes
            ) {
                upsert = false
                contentType = ContentType.Image.JPEG
            }

            // 3. Return Public URL
            bucket.publicUrl(fileName)

        } catch (e: Exception) {
            Log.e("UploadError", "Failed to upload image: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }