package com.radius.optimization

import android.net.Uri

object RadiusConfig {
    const val AUTHORITY = "com.radius.optimization.config"
    const val PATH_DP = "radius_dp"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_DP")

    const val DEFAULT_DP = 26f
    const val MIN_DP = 16f
    const val MAX_DP = 130f
}
