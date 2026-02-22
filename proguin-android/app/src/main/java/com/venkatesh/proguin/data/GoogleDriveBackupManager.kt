package com.venkatesh.proguin.data

import android.content.Context
import android.content.Intent

/**
 * Option A (Offline): Google Drive backup is disabled.
 * This stub keeps the project compiling without Google dependencies.
 */
class GoogleDriveBackupManager(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences("proguin_drive", Context.MODE_PRIVATE)

    fun savedEmail(): String = prefs.getString("email", "").orEmpty()

    fun clearSavedEmail() {
        prefs.edit().remove("email").apply()
    }

    fun lastSignedInAccount(): Any? = null

    fun signInIntent(): Intent = Intent()

    fun rememberAccount(account: Any?) { /* no-op */ }

    suspend fun backupNow(account: Any?): Result<Unit> =
        Result.failure(IllegalStateException("Google Drive backup is disabled (Offline mode)."))

    suspend fun restoreNow(account: Any?): Result<Unit> =
        Result.failure(IllegalStateException("Google Drive restore is disabled (Offline mode)."))

    fun signOut(onDone: () -> Unit) {
        clearSavedEmail()
        onDone()
    }
}