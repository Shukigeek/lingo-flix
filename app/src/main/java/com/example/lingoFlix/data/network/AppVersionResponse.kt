package com.example.lingoFlix.data.network

import com.google.gson.annotations.SerializedName

/** Mirrors the backend's AppVersionOut schema (GET /api/v1/app/version). */
data class AppVersionResponse(
    @SerializedName("latest_version_code") val latestVersionCode: Int,
    @SerializedName("latest_version_name") val latestVersionName: String,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("update_notes") val updateNotes: String,
    @SerializedName("force_update") val forceUpdate: Boolean,
)
