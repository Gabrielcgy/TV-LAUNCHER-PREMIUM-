package com.tvpremium.launcher.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tvpremium.launcher.BuildConfig
import com.tvpremium.launcher.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val activity: Activity) {

    // Default GitHub Raw URL as reference (can be customized by user)
    private var updateUrl = "https://raw.githubusercontent.com/kb0528256/tv-premium-launcher/main/update.json"

    fun setUpdateUrl(url: String) {
        this.updateUrl = url
    }

    /**
     * Checks for updates and displays the dialog if a newer version is found.
     * This method is designed to be fully non-blocking and safe.
     */
    fun checkForUpdates(onUpdateChecked: (Boolean) -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlConnection = URL(updateUrl).openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 8000
                urlConnection.readTimeout = 8000
                urlConnection.requestMethod = "GET"

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonString = urlConnection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(jsonString)

                    val serverVersionCode = jsonObject.optInt("versionCode", 0)
                    val serverVersionName = jsonObject.optString("versionName", "1.0")
                    val updateMessage = jsonObject.optString("updateMessage", "Nueva versión disponible con mejoras de rendimiento.")
                    val apkUrl = jsonObject.optString("apkUrl", "")

                    val currentVersionCode = getAppVersionCode(activity)

                    if (serverVersionCode > currentVersionCode && apkUrl.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(serverVersionName, updateMessage, apkUrl)
                            onUpdateChecked(true)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onUpdateChecked(false)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onUpdateChecked(false)
                    }
                }
                urlConnection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onUpdateChecked(false)
                }
            }
        }
    }

    private fun getAppVersionCode(context: Context): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    private fun getAppVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    private fun showUpdateDialog(newVersionName: String, updateMessage: String, apkUrl: String) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_update, null)
        
        val tvCurrentVersion: TextView = dialogView.findViewById(R.id.tv_current_version)
        val tvNewVersion: TextView = dialogView.findViewById(R.id.tv_new_version)
        val tvUpdateMessage: TextView = dialogView.findViewById(R.id.tv_update_message)
        val layoutProgress: LinearLayout = dialogView.findViewById(R.id.layout_progress)
        val layoutButtons: LinearLayout = dialogView.findViewById(R.id.layout_buttons)
        val tvPercentage: TextView = dialogView.findViewById(R.id.tv_progress_percentage)
        val progressBar: LinearProgressIndicator = dialogView.findViewById(R.id.progress_bar)
        val btnLater: Button = dialogView.findViewById(R.id.btn_later)
        val btnDownload: Button = dialogView.findViewById(R.id.btn_download)

        tvCurrentVersion.text = getAppVersionName(activity)
        tvNewVersion.text = newVersionName
        tvUpdateMessage.text = updateMessage

        val dialog = MaterialAlertDialogBuilder(activity, R.style.Theme_MyApplication)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnLater.setOnClickListener {
            dialog.dismiss()
        }

        btnDownload.setOnClickListener {
            // Initiate APK download on background thread with real-time progress callback
            layoutButtons.visibility = View.GONE
            layoutProgress.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                val apkFile = File(activity.getExternalFilesDir(null), "tv_premium_launcher_update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                try {
                    val url = URL(apkUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.connect()

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw Exception("Server returned HTTP " + connection.responseCode)
                    }

                    val fileLength = connection.contentLength
                    val input = connection.inputStream
                    val output = FileOutputStream(apkFile)

                    val data = ByteArray(4096)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count.toLong()
                        if (fileLength > 0) {
                            val progress = (total * 100 / fileLength).toInt()
                            withContext(Dispatchers.Main) {
                                progressBar.progress = progress
                                tvPercentage.text = "$progress%"
                            }
                        }
                        output.write(data, 0, count)
                    }

                    output.flush()
                    output.close()
                    input.close()
                    connection.disconnect()

                    // Download completed successfully, trigger APK installation
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        installApk(apkFile)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
                        layoutButtons.visibility = View.VISIBLE
                        layoutProgress.visibility = View.GONE
                    }
                }
            }
        }

        dialog.show()
    }

    private fun installApk(file: File) {
        try {
            val context: Context = activity
            val intent = Intent(Intent.ACTION_VIEW)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider to safely share APK file with package installer on Android 7.0+
                val apkUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                @Suppress("DEPRECATION")
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Error al instalar APK: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
