package com.dm6801.filemanager

import android.annotation.SuppressLint
import java.io.File

/*
https://www.iana.org/assignments/media-types/media-types.xhtml
https://en.wikipedia.org/wiki/List_of_file_signatures
https://www.digipres.org/formats/mime-types/
https://asecuritysite.com/forensics/magic
https://filesignatures.net/index.php?page=all
https://mimesniff.spec.whatwg.org/#introduction
https://www.garykessler.net/library/file_sigs.html
https://github.com/samuelneff/MimeTypeMap/blob/master/MimeTypeMap.cs
 */

private const val SIZE = 16

typealias Bytes = List<Byte?>

enum class MimeType(
    val mime: List<String>,
    val magic: List<Bytes> = emptyList(),
    val extensions: List<String> = emptyList()
) {
    JPG(
        mime = listOf("image/jpeg"),
        magic = listOf(
            bytes(0xFF, 0xD8, 0xFF, 0xDB),
            bytes(0xFF, 0xD8, 0xFF, 0xEE),
            bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01),
            bytes(0xFF, 0xD8, 0xFF, 0xE1, null, null, 0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
        ),
        extensions = listOf("jpeg", "jpe", "jif", "jfif", "jfi")
    ),
    PNG(
        mime = listOf("image/png"),
        magic = listOf(
            bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        )
    ),
    BMP(
        mime = listOf("image/bmp"),
        magic = listOf(
            bytes(0x42, 0x4D)
        )
    ),
    GIF(
        mime = listOf("image/gif"),
        magic = listOf(
            bytes(0x47, 0x49, 0x46, 0x38, 0x37, 0x61),
            bytes(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)
        )
    ),
    MP4(
        mime = listOf("video/mp4"),
        magic = listOf(
            bytes(null, null, null, null, 0x66, 0x74, 0x79, 0x70, 0x4D, 0x53, 0x4E, 0x56),
            bytes(null, null, null, null, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D)
        )
    ),
    MOV(
        mime = listOf("video/quicktime"),
        magic = listOf(
            bytes(null, null, null, null, 0x66, 0x74, 0x79, 0x70, 0x71, 0x74, 0x20, 0x20),
            bytes(null, null, null, null, 0x6D, 0x6F, 0x6F, 0x76)
        ),
        extensions = listOf("qt")
    ),
    MKV(
        mime = listOf("video/x-matroska"),
        magic = listOf(
            bytes(0x1A, 0x45, 0xDF, 0xA3)
        ),
        extensions = listOf("mka", "mks", "mk3d", "webm")
    ),
    MPG(
        mime = listOf("video/mpeg"),
        magic = listOf(
            bytes(0x00, 0x00, 0x01, 0xBA),
            bytes(0x00, 0x00, 0x01, 0xB3)
        )
    ),
    AVI(
        mime = listOf("video/x-msvideo"),
        magic = listOf(
            bytes(
                0x52, 0x49, 0x46, 0x46, null, null, null, null,
                0x41, 0x56, 0x49, 0x20, 0x4C, 0x49, 0x53, 0x54
            )
        )
    ),
    MP3(
        mime = listOf("audio/mpeg"),
        magic = listOf(
            bytes(0xFF, 0xFB),
            bytes(0xFF, 0xF3),
            bytes(0xFF, 0xF2),
            bytes(0x49, 0x44, 0x33)
        )
    ),
    WAV(
        mime = listOf(
            "audio/x-wav",
            "audio/wav"
        ),
        magic = listOf(
            bytes(0x52, 0x49, 0x46, 0x46, null, null, null, null, 0x57, 0x41, 0x56, 0x45)
        )
    ),
    FLAC(
        mime = listOf("audio/x-flac"),
        magic = listOf(
            bytes(0x66, 0x4C, 0x61, 0x43)
        )
    ),
    PDF(
        mime = listOf("application/pdf"),
        magic = listOf(
            bytes(0x25, 0x50, 0x44, 0x46)
        )
    ),
    RAR(
        mime = listOf("application/vnd.rar"),
        magic = listOf(
            bytes(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00),
            bytes(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00)
        )
    ),
    ZIP(
        mime = listOf("application/zip"),
        magic = listOf(
            bytes(0x50, 0x4B, 0x03, 0x04)
        )
    ),
    TAR(
        mime = listOf("application/x-tar"),
        magic = listOf(
            bytes(0x75, 0x73, 0x74, 0x61, 0x72, 0x00, 0x30, 0x30)
        )
    ),
    ELF(
        mime = listOf("application/x-elf"),
        magic = listOf(
            bytes(0x7F, 0x45, 0x4C, 0x46)
        )
    ),
    EXE(
        mime = listOf(
            "application/vnd.microsoft.portable-executable",
            "application/x-msdownload"
        ),
        magic = listOf(
            bytes(0x4D, 0x5A)
        )
    ),
    CSV(
        mime = listOf(
            "text/csv",
            "text/plain"
        ),
        magic = emptyList()
    ),
    TXT(
        mime = listOf("text/plain"),
        magic = emptyList()
    )
    ;

    val type: String get() = mime.firstOrNull() ?: ""

    @SuppressLint("DefaultLocale")
    companion object {
        fun analyze(path: String): String? {
            val bytes = readBytes(path)
            val ext = path.ext()
            if (ext != null) findEnum(ext)
                ?.takeIf { bytes?.matches(it.magic) == true }
                ?.let { return it.type }
            return values().asSequence().run {
                find { bytes?.matches(it.magic) == true }?.type
                    ?: find { it.extensions.contains(ext ?: return null) }?.type
            }
        }

        private fun findEnum(name: String): MimeType? {
            return try {
                valueOf(name.toUpperCase())
            } catch (_: Throwable) {
                null
            }
        }

        private fun String.ext(): String? {
            val separatorIndex = indexOfLast { it == '/' }.coerceAtLeast(0)
            val dotIndex = indexOf('.', separatorIndex)
            if (dotIndex == -1 || dotIndex + 1 >= length - 1) return null
            return substring(dotIndex + 1)
        }

        private fun readBytes(path: String): ByteArray? {
            val bytes = ByteArray(SIZE)
            val file = File(path)
            if (!file.exists()) return null
            try {
                if (file.inputStream().buffered().use { it.read(bytes, 0, SIZE) } != SIZE) {
                    val fileLength = file.length()
                    if (fileLength <= 0) return null
                    if (fileLength < SIZE)
                        file.inputStream().buffered().use { it.read(bytes, 0, fileLength.toInt()) }
                }
            } catch (_: Throwable) {
                return null
            }
            return if (bytes.isNotEmpty()) bytes
            else null
        }
    }
}

private fun ByteArray.matches(other: List<Bytes>): Boolean {
    return other.any { it.matches(this) }
}

private fun Bytes.matches(other: ByteArray): Boolean {
    for (i in 0 until size) {
        val char = get(i)
        if (char != other.getOrNull(i) && char != null) return false
    }
    return true
}

private fun bytes(vararg bytes: Int?): List<Byte?> {
    return bytes.map { it?.toByte() }
}