package com.pocket4cut.core.util

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File

object FileUris {
    fun contentUriForFile(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

