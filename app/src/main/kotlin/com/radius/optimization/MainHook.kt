package com.radius.optimization

import android.content.Context
import android.content.res.Resources
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method

object MainHook : YukiBaseHooker() {

    private val TARGET_DIMEN_NAMES = listOf(
        "recent_task_view_radius",
        "task_view_radius_20",
        "task_view_radius_22"
    )
    private const val TARGET_PACKAGE = "com.android.launcher"
    private const val TAG = "RTR_HOOK"
    private const val LOCAL_PREFS_NAME = "radius_hook_cache"
    private const val LOCAL_KEY_LAST_DP = "last_dp"

    private var cachedTargetDimenIds: Set<Int> = emptySet()
    @Volatile
    private var cachedReplacementDp: Float? = null
    @Volatile
    private var configObserverRegistered = false

    override fun onHook() {
        loadApp(name = TARGET_PACKAGE) {
            Resources::class.java.method {
                name = "getDimensionPixelSize"
                param(Int::class.java)
            }.hook {
                after {
                    val requestId = args(0).int()
                    val res = instance as? Resources ?: return@after

                    if (cachedTargetDimenIds.isEmpty()) {
                        cachedTargetDimenIds = TARGET_DIMEN_NAMES.mapNotNull { name ->
                            val id = res.getIdentifier(name, "dimen", TARGET_PACKAGE)
                            if (id != 0) id else null
                        }.toSet()
                    }
                    if (cachedTargetDimenIds.isEmpty() || requestId !in cachedTargetDimenIds) return@after

                    registerConfigObserverIfNeeded()

                    val replacementDp = cachedReplacementDp ?: resolveReplacementDp(currentAppContext).let { resolved ->
                        if (resolved.source != "default") {
                            cachedReplacementDp = resolved.dp
                        }
                        resolved.dp
                    }

                    val modifiedPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        replacementDp,
                        res.displayMetrics
                    ).toInt()

                    result = modifiedPx
                }
            }
        }
    }

    private data class ResolveResult(val dp: Float, val source: String)

    private fun resolveReplacementDp(appContext: Context?): ResolveResult {
        if (appContext == null) {
            Log.i(TAG, "resolve source=default reason=no_app_context dp=${RadiusConfig.DEFAULT_DP}")
            return ResolveResult(RadiusConfig.DEFAULT_DP, "default")
        }

        val providerDp = runCatching {
            appContext.contentResolver.query(RadiusConfig.CONTENT_URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.toFloatOrNull()?.coerceIn(
                        RadiusConfig.MIN_DP,
                        RadiusConfig.MAX_DP
                    )
                } else null
            }
        }.getOrNull()

        if (providerDp != null) {
            writeLocalBackupDp(appContext, providerDp)
            Log.i(TAG, "resolve source=provider dp=$providerDp")
            return ResolveResult(providerDp, "provider")
        }

        val localDp = readLocalBackupDp(appContext)
        if (localDp != null) {
            Log.i(TAG, "resolve source=local_backup dp=$localDp")
            return ResolveResult(localDp, "local_backup")
        }

        Log.i(TAG, "resolve source=default reason=no_provider_no_backup dp=${RadiusConfig.DEFAULT_DP}")
        return ResolveResult(RadiusConfig.DEFAULT_DP, "default")
    }

    private fun localPrefsContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
    }

    private fun readLocalBackupDp(context: Context): Float? {
        val prefs = localPrefsContext(context).getSharedPreferences(LOCAL_PREFS_NAME, 0)
        if (!prefs.contains(LOCAL_KEY_LAST_DP)) return null
        return prefs.getFloat(LOCAL_KEY_LAST_DP, RadiusConfig.DEFAULT_DP).coerceIn(
            RadiusConfig.MIN_DP,
            RadiusConfig.MAX_DP
        )
    }

    private fun writeLocalBackupDp(context: Context, dp: Float) {
        val safeDp = dp.coerceIn(RadiusConfig.MIN_DP, RadiusConfig.MAX_DP)
        localPrefsContext(context).getSharedPreferences(LOCAL_PREFS_NAME, 0)
            .edit()
            .putFloat(LOCAL_KEY_LAST_DP, safeDp)
            .commit()
    }

    private fun registerConfigObserverIfNeeded() {
        if (configObserverRegistered) return
        val appContext = currentAppContext ?: return
        runCatching {
            appContext.contentResolver.registerContentObserver(
                RadiusConfig.CONTENT_URI,
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        cachedReplacementDp = null
                    }
                }
            )
            configObserverRegistered = true
        }
    }

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            val appGlobalsClass = Class.forName("android.app.AppGlobals")
            appGlobalsClass.getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
}
