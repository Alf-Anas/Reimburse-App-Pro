package dev.geoit.android.reimburseapppro.utils

import android.content.Context
import android.os.Environment
import dev.geoit.android.reimburseapppro.R
import java.io.File

class GetReimburseDirectory {

    fun getPicturesDirectory(mCtx: Context): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + File.separator + mCtx.resources.getString(R.string.app_name) + File.separator
        )
    }

    fun getDownloadsDirectory(mCtx: Context, projectID: String, timeStamp: String): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + File.separator + mCtx.resources.getString(R.string.app_name) + File.separator + projectID + "_" + timeStamp + File.separator
        )
    }

}