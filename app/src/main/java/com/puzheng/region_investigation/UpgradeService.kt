package com.puzheng.region_investigation

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.store.VersionUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.regex.Pattern


class UpgradeService : IntentService("UPGRADE_SERVICE") {
    override fun onHandleIntent(intent: Intent?) {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = manager.activeNetworkInfo
        if (info == null || !info.isAvailable) {
            return
        }
        try {
            val response = OkHttpClient().newCall(
                    Request.Builder()
                            .url(Uri.parse(ConfigStore.with(this).backend).buildUpon()
                                    .appendEncodedPath("app/latest-version").build().toString())
                            .build()).execute()
            if (!response.isSuccessful) {
                Logger.e(response.message())
            }
            val versionRegex = Pattern.compile("\\d+\\.\\d+.\\d+$", Pattern.CASE_INSENSITIVE).toRegex()
            val root = JSONObject(response.body().string())
            val latestVersion = root.getString("version")
            if (latestVersion.matches(versionRegex)) {
                Logger.i("current version: ${BuildConfig.VERSION_NAME} latest version: $latestVersion")
                if (VersionUtil.compare(BuildConfig.VERSION_NAME, root.getString("version")) == -1) {
                    Logger.v("should update to $latestVersion")

                    MyApplication.currentActivity.runOnUiThread {
                        ConfirmUpgradeDialogFragment.newInstance(BuildConfig.VERSION_NAME, latestVersion)
                                .show(MyApplication.currentActivity.supportFragmentManager, "")
                    }
                }
            }
        } catch (e: java.net.ConnectException) {
            e.printStackTrace()
        }
    }
}
