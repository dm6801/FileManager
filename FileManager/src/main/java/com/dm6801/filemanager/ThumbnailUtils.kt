package com.dm6801.filemanager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import java.io.File

object ThumbnailUtils {

    fun extractThumbnail(file: File, mimeType: String, size: Size): Bitmap? {
        return when {
            mimeType.startsWith("image") -> extractImageThumbnail(file, size)
            mimeType.startsWith("video") -> extractVideoThumbnail(file, size)
            mimeType.startsWith("audio") -> extractAudioThumbnail(file, size)
            else -> null
        }
    }

    fun extractImageThumbnail(file: File, size: Size): Bitmap? {
        return try {
            file.inputStream().use {
                val options = BitmapFactory.Options()
                BitmapFactory.decodeStream(it, null, options)
                    ?.downscale(`in` = Size(options.outWidth, options.outHeight), `out` = size)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun extractVideoThumbnail(file: File, size: Size): Bitmap? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            val embedded = mmr.embeddedPicture
            if (embedded?.isNotEmpty() == true)
                BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                    ?.let {
                        return it.downscale(
                            `in` = Size(it.width, it.height),
                            `out` = size
                        )
                    }
            val width =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toIntOrNull()
                    ?: return null
            val height =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toIntOrNull()
                    ?: return null
            val duration =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLongOrNull()
                    ?: return null

            mmr.getFrameAtTime(duration / 2, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.downscale(`in` = Size(width, height), `out` = size)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                mmr.close()
        }
    }

    fun extractAudioThumbnail(file: File, size: Size): Bitmap? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            val embedded = mmr.embeddedPicture
            if (embedded?.isNotEmpty() == true)
                BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                    ?.let {
                        return it.downscale(
                            `in` = Size(it.width, it.height),
                            `out` = size
                        )
                    }
            else null
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                mmr.close()
        }
    }

    fun Bitmap.downscale(
        `in`: Size,
        `out`: Size,
        bilinear: Boolean = false,
        aspectRatio: Boolean = true
    ): Bitmap? {
        val (inWidth, inHeight) = `in`.width to `in`.height
        val (outWidth, outHeight) = `out`.width to `out`.height
        return try {
            if (inWidth > outWidth || inHeight > outHeight) {
                if (aspectRatio) when {
                    inWidth > inHeight ->
                        Bitmap.createScaledBitmap(
                            this,
                            outWidth,
                            (inHeight / (inWidth / outWidth.toFloat())).toInt(),
                            bilinear
                        )?.also { recycle() }
                    else ->
                        Bitmap.createScaledBitmap(
                            this,
                            (inWidth / (inHeight / outHeight.toFloat())).toInt(),
                            outHeight,
                            bilinear
                        )?.also { recycle() }
                }
                else Bitmap.createScaledBitmap(this, outWidth, outHeight, bilinear)
                    ?.also { recycle() }
            } else this
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}