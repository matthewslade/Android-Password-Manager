package com.sladematthew.apm

import android.content.Context
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class DrivePasswords(private val context: Context) {
     var drive: Drive? = null
    suspend fun backup(string: String) {
        withContext(Dispatchers.IO){
            val file = File(context.cacheDir, Constants.Misc.LOCAL_FILENAME)
            val fileOutput = FileOutputStream(file)
            fileOutput.use { it.write(string.toByteArray()) }
            val fileG = com.google.api.services.drive.model.File().apply {
                name = Constants.Misc.LOCAL_FILENAME
            }
            val mediaContent = FileContent("application/json",file)
            drive?.files()?.create(fileG,mediaContent)?.execute()
        }
    }

    suspend fun restore(): String? {
        val byteArray = ByteArrayOutputStream()
        return drive
            ?.files()
            ?.list()
            ?.setSpaces("drive")
            ?.setFields("*")
            ?.execute()
            ?.files
            ?.find {
                it.name == Constants.Misc.LOCAL_FILENAME
            }?.id?.let {
                drive?.files()?.get(it)?.executeMediaAndDownloadTo(byteArray)
                byteArray.toString()
        }
    }
}