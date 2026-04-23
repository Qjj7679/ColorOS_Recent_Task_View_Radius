package com.radius.optimization

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class RadiusConfigProvider : ContentProvider() {

    companion object {
        private const val PREFS_NAME = "radius_config"
        private const val KEY_DP = "recent_task_view_radius_dp"

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(RadiusConfig.AUTHORITY, RadiusConfig.PATH_DP, 1)
        }
    }

    private fun getDeviceProtectedContext() = context?.let { ctx ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            ctx.createDeviceProtectedStorageContext()
        } else ctx
    } ?: context

    private fun getCredentialProtectedContext() = context

    private fun migrateToDeviceProtectedIfNeeded(prefs: android.content.SharedPreferences) {
        if (prefs.contains(KEY_DP)) return
        val cePrefs = getCredentialProtectedContext()?.getSharedPreferences(PREFS_NAME, 0) ?: return
        if (!cePrefs.contains(KEY_DP)) return
        val migrated = cePrefs.getFloat(KEY_DP, RadiusConfig.DEFAULT_DP)
            .coerceIn(RadiusConfig.MIN_DP, RadiusConfig.MAX_DP)
        prefs.edit().putFloat(KEY_DP, migrated).commit()
    }

    private fun readDpOrNull(): Float? {
        val prefs = getDeviceProtectedContext()?.getSharedPreferences(PREFS_NAME, 0) ?: return null
        migrateToDeviceProtectedIfNeeded(prefs)
        if (!prefs.contains(KEY_DP)) return null
        return prefs.getFloat(KEY_DP, RadiusConfig.DEFAULT_DP)
            .coerceIn(RadiusConfig.MIN_DP, RadiusConfig.MAX_DP)
    }

    private fun writeDp(value: Float) {
        val safe = value.coerceIn(RadiusConfig.MIN_DP, RadiusConfig.MAX_DP)
        val prefs = getDeviceProtectedContext()?.getSharedPreferences(PREFS_NAME, 0) ?: return
        prefs.edit().putFloat(KEY_DP, safe).commit()
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (matcher.match(uri) != 1) return null
        val cursor = MatrixCursor(arrayOf("value"))
        readDpOrNull()?.let { cursor.addRow(arrayOf(it.toString())) }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (matcher.match(uri) != 1) return null
        val value = values?.getAsFloat("value")
            ?: values?.getAsString("value")?.toFloatOrNull()
            ?: RadiusConfig.DEFAULT_DP
        writeDp(value)
        context?.contentResolver?.notifyChange(uri, null)
        return uri
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (matcher.match(uri) != 1) return 0
        val value = values?.getAsFloat("value")
            ?: values?.getAsString("value")?.toFloatOrNull()
            ?: RadiusConfig.DEFAULT_DP
        writeDp(value)
        context?.contentResolver?.notifyChange(uri, null)
        return 1
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = if (matcher.match(uri) == 1)
        "vnd.android.cursor.item/vnd.${RadiusConfig.AUTHORITY}.${RadiusConfig.PATH_DP}"
    else null
}
