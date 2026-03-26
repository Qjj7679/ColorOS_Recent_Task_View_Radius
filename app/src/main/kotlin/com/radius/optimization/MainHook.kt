package com.radius.optimization

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method

object MainHook : YukiBaseHooker() {

    private const val TARGET_DIMEN_NAME = "recent_task_view_radius"
    private const val TARGET_PACKAGE = "com.android.launcher"
    private var cachedTargetDimenId = 0
    @Volatile
    private var cachedReplacementDp: Float? = null

    override fun onHook() {
        loadApp(name = TARGET_PACKAGE) {
            Resources::class.java.method {
                name = "getDimensionPixelSize"
                param(Int::class.java)
            }.hook {
                after {
                    val requestId = args(0).int()
                    val res = instance as? Resources ?: return@after

                    if (cachedTargetDimenId == 0) {
                        cachedTargetDimenId = res.getIdentifier(TARGET_DIMEN_NAME, "dimen", TARGET_PACKAGE)
                    }
                    if (cachedTargetDimenId == 0 || requestId != cachedTargetDimenId) return@after

                    val replacementDp = cachedReplacementDp ?: runCatching {
                        val appContext = currentAppContext ?: return@runCatching RadiusConfig.DEFAULT_DP
                        appContext.contentResolver.query(RadiusConfig.CONTENT_URI, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                cursor.getString(0)?.toFloatOrNull()?.coerceIn(
                                    RadiusConfig.MIN_DP,
                                    RadiusConfig.MAX_DP
                                ) ?: RadiusConfig.DEFAULT_DP
                            } else RadiusConfig.DEFAULT_DP
                        } ?: RadiusConfig.DEFAULT_DP
                    }.getOrDefault(RadiusConfig.DEFAULT_DP).also { cachedReplacementDp = it }

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

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            val appGlobalsClass = Class.forName("android.app.AppGlobals")
            appGlobalsClass.getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
}
