package com.axel.finanzas.core.notifications

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledApp(val label: String, val packageName: String)

/**
 * Apps con ícono de lanzador instaladas en el teléfono (de ahí se elige
 * cuáles bancos escuchar). Usa `<queries>` del manifest en vez de
 * QUERY_ALL_PACKAGES para no pedir un permiso tan amplio.
 */
@Suppress("DEPRECATION")
fun getInstalledLaunchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    return resolveInfos
        .map { InstalledApp(label = it.loadLabel(pm).toString(), packageName = it.activityInfo.packageName) }
        .distinctBy { it.packageName }
        .filterNot { it.packageName == context.packageName }
        .sortedBy { it.label.lowercase() }
}
