package com.tvpremium.launcher.models

import android.graphics.drawable.Drawable

data class AppModel(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val category: String
)
