package com.pocket4cut.core.util

import android.content.res.Resources

object CollageExportMetrics {
    val preferredOutputWidth: Float
        get() {
            val dm = Resources.getSystem().displayMetrics
            var w = dm.widthPixels.toFloat()
            if (w < 320f) w = 1170f
            return w.coerceIn(720f, 2160f)
        }
}
