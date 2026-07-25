package com.example.busetaescolarapp.utils

import android.content.Context
import android.content.pm.PackageManager

object MapsKeyProvider {
    /** Lee la misma API Key que ya usa el Maps SDK desde el AndroidManifest (meta-data), sin duplicarla. */
    fun getApiKey(context: Context): String {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        return appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    }
}
