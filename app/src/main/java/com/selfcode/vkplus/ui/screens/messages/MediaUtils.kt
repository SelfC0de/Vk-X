package com.selfcode.vkplus.ui.screens.messages

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

fun downloadFile(context: Context, url: String, filename: String, mimeType: String = "*/*") {
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                when {
                    mimeType.startsWith("image") -> Environment.DIRECTORY_PICTURES
                    mimeType.startsWith("video") -> Environment.DIRECTORY_MOVIES
                    mimeType.startsWith("audio") -> Environment.DIRECTORY_MUSIC
                    else -> Environment.DIRECTORY_DOWNLOADS
                },
                filename
            )
            .setMimeType(mimeType)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        dm.enqueue(req)
        Toast.makeText(context, "Загрузка: $filename", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun mimeFromExt(ext: String): String = when (ext.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png"         -> "image/png"
    "gif"         -> "image/gif"
    "webp"        -> "image/webp"
    "mp4"         -> "video/mp4"
    "avi"         -> "video/x-msvideo"
    "mov"         -> "video/quicktime"
    "mp3"         -> "audio/mpeg"
    "ogg"         -> "audio/ogg"
    "pdf"         -> "application/pdf"
    "doc","docx"  -> "application/msword"
    "xls","xlsx"  -> "application/vnd.ms-excel"
    "zip"         -> "application/zip"
    "rar"         -> "application/x-rar-compressed"
    else          -> "*/*"
}
