package com.puzheng.region_investigation

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.puzheng.region_investigation.store.EventType
import nl.komponents.kovenant.task
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.logging.Level

class UpgradeUtil private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = UpgradeUtil(context)
    }

    fun download(version: String, path: String, onProgress: (sent: Long, total: Long) -> Unit) = task {

        val cacheDir = File(Environment.getExternalStoragePublicDirectory(MyApplication.context.packageName),
                ".cache").apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val apkFile = File(cacheDir, version + ".apk")



        val response = OkHttpClient().newCall(
                Request.Builder()
                        .url(Uri.parse(ConfigStore.with(context).assetsBackend).buildUpon()
                                .appendEncodedPath(path)
                                .build().toString())
                        .build()).execute()
        if (response.code() == 200) {
            val inputStream = response.body().byteStream();
            val outputStream = FileOutputStream(apkFile)
            val buff = ByteArray(4096)
            var downloaded = 0L
            val total = response.body().contentLength()
            onProgress(downloaded, total)
            while (true) {
                val read = inputStream.read(buff)
                if(read == -1){
                    break;
                }
                //write buff
                downloaded += read;
                outputStream.write(buff, 0, read)
                onProgress(downloaded, total);
            }
            inputStream?.close()
            outputStream.close()
        }
        if (!response.isSuccessful) {
            throw IOException("Unexpected code " + response)
        }
        response.body().close()
        MyApplication.eventLogger.log(Level.INFO, "升级应用版本", JSONObject().apply {
            put("type", EventType.UPGRADE)
            put("version", version)
        })
        apkFile
    }
}
