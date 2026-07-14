package com.tvpremium.launcher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.tvpremium.launcher.models.AppModel
import java.util.Locale

object AppLoader {

    const val CATEGORY_GAMES = "Juegos"
    const val CATEGORY_APPS = "Aplicaciones"
    const val CATEGORY_TOOLS = "Herramientas"
    const val CATEGORY_SYSTEM = "Sistema"

    fun loadInstalledApps(context: Context): List<AppModel> {
        val pm = context.packageManager
        val apps = mutableListOf<AppModel>()
        val uniquePackages = mutableSetOf<String>()

        // 1. Query TV Leanback Launcher apps (Crucial for Android TV Boxes)
        val tvIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvActivities = pm.queryIntentActivities(tvIntent, 0)
        for (resolveInfo in tvActivities) {
            val packageName = resolveInfo.activityInfo.packageName
            // Exclude our own launcher
            if (packageName != context.packageName && uniquePackages.add(packageName)) {
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val category = categorizeApp(resolveInfo.activityInfo.applicationInfo, label, packageName)
                apps.add(AppModel(label, packageName, icon, category))
            }
        }

        // 2. Query Standard Mobile Launcher apps (Many TV boxes install standard android apps)
        val mobileIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val mobileActivities = pm.queryIntentActivities(mobileIntent, 0)
        for (resolveInfo in mobileActivities) {
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName != context.packageName && uniquePackages.add(packageName)) {
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val category = categorizeApp(resolveInfo.activityInfo.applicationInfo, label, packageName)
                apps.add(AppModel(label, packageName, icon, category))
            }
        }

        // Sort alphabetically by name
        return apps.sortedWith { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
    }

    private fun categorizeApp(appInfo: ApplicationInfo, label: String, packageName: String): String {
        val labelLower = label.lowercase(Locale.ROOT)
        val packageLower = packageName.lowercase(Locale.ROOT)

        // Rule A: Check official Android category (API 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return CATEGORY_GAMES
                ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_MAPS -> return CATEGORY_TOOLS
                // Audio/Video/Social/News/Maps/etc are Apps
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE, ApplicationInfo.CATEGORY_SOCIAL,
                ApplicationInfo.CATEGORY_NEWS -> return CATEGORY_APPS
            }
        }

        // Rule B: Match package and label keywords for Games
        if (packageLower.contains("game") || packageLower.contains("arcade") || 
            packageLower.contains("play") || packageLower.contains("retro") || 
            packageLower.contains("emulator") || packageLower.contains("sports") ||
            labelLower.contains("juego") || labelLower.contains("game") || 
            labelLower.contains("retro") || labelLower.contains("play") ||
            labelLower.contains("mario") || labelLower.contains("minecraft") ||
            labelLower.contains("fifa") || labelLower.contains("racing")
        ) {
            return CATEGORY_GAMES
        }

        // Rule C: Match package and label keywords for System apps
        if (packageLower.contains("android.settings") || packageLower.contains("systemui") || 
            packageLower.contains("providers") || packageLower.contains("packageinstaller") || 
            packageLower.contains("updater") || packageLower.contains("launcher") || 
            packageLower.contains("tv.settings") || packageLower.contains("com.android.") ||
            labelLower.contains("ajustes") || labelLower.contains("settings") || 
            labelLower.contains("configurac") || labelLower.contains("sistema") || 
            labelLower.contains("actualiz") || labelLower.contains("updater") ||
            labelLower.contains("archivo") || labelLower.contains("launcher") ||
            labelLower.contains("home")
        ) {
            // Some file browsers might be Tools, let's keep specific file browsers in Tools
            if (labelLower.contains("archivo") || labelLower.contains("explorer") || labelLower.contains("manager")) {
                return CATEGORY_TOOLS
            }
            return CATEGORY_SYSTEM
        }

        // Rule D: Match package and label keywords for Tools
        if (packageLower.contains("tool") || packageLower.contains("cleaner") || 
            packageLower.contains("filemanager") || packageLower.contains("explorer") || 
            packageLower.contains("browser") || packageLower.contains("chrome") || 
            packageLower.contains("firefox") || packageLower.contains("opera") || 
            packageLower.contains("speedtest") || packageLower.contains("vpn") || 
            packageLower.contains("dns") || packageLower.contains("calc") || 
            packageLower.contains("keyboard") || packageLower.contains("remote") || 
            packageLower.contains("terminal") || packageLower.contains("download") ||
            labelLower.contains("explorer") || labelLower.contains("browser") || 
            labelLower.contains("navegador") || labelLower.contains("herramienta") || 
            labelLower.contains("calc") || labelLower.contains("vpn") || 
            labelLower.contains("descarga") || labelLower.contains("ayuda") ||
            labelLower.contains("speedtest") || labelLower.contains("remote")
        ) {
            return CATEGORY_TOOLS
        }

        // Default to Apps
        return CATEGORY_APPS
    }
}
