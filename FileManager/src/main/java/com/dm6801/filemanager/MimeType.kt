package com.dm6801.filemanager

import java.io.File

private const val SIZE = 12

typealias Bytes = List<Byte?>

enum class MimeType(vararg val types: Pair<String, List<Bytes>>) {
    PNG("image/png" to listOf(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))),
    JPG(
        "image/jpeg" to listOf(
            bytes(0xFF, 0xD8, 0xFF, 0xDB),
            bytes(0xFF, 0xD8, 0xFF, 0xEE),
            bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01),
            bytes(0xFF, 0xD8, 0xFF, 0xE1, null, null, 0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
        )
    ),
    GIF(
        "image/gif" to listOf(
            bytes(0x47, 0x49, 0x46, 0x38, 0x37, 0x61),
            bytes(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)
        )
    )
    ;

    companion object {
        fun analyze(path: String): String? {
            val bytes = ByteArray(SIZE)
            try {
                if (File(path).inputStream().buffered().use { it.read(bytes, 0, SIZE) } != SIZE)
                    return null
            } catch (_: Throwable) {
                return null
            }

            return values().asSequence()
                .map { it.types.toList() }
                .flatten()
                .find { pair -> pair.second.any { it.matches(bytes) } }?.first
        }
    }
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