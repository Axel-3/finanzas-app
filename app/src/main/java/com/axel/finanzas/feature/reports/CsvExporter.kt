package com.axel.finanzas.feature.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.format.DateTimeFormatter

/**
 * Escribe el CSV a un archivo temporal en caché y abre el selector de
 * "compartir" de Android (el usuario decide si lo guarda en Drive, lo
 * manda por correo, etc.) — más simple y flexible que pedir permisos de
 * almacenamiento para escribir directo a Descargas.
 */
fun shareCsv(context: Context, csvContent: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val fileName = "finanzas_${DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now())}.csv"
    val file = File(dir, fileName)
    file.writeText(csvContent)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Exportar movimientos"))
}
