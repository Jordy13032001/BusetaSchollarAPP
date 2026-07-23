package com.example.busetaescolarapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "SecureBusetaPrefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveUserSession(email: String, primaryRole: String, name: String, phone: String?, roles: List<String> = emptyList()) {
        sharedPreferences.edit().apply {
            putString("email", email)
            putString("role", primaryRole)
            putString("name", name)
            putString("phone", phone ?: "")
            putStringSet("roles", roles.toSet())
            putBoolean("isLoggedIn", true)
            apply()
        }
    }

    fun hasRole(role: String): Boolean {
        val roles = sharedPreferences.getStringSet("roles", emptySet())
        return roles?.contains(role) == true
    }

    fun getAvailableRoles(): Set<String> {
        return sharedPreferences.getStringSet("roles", emptySet()) ?: emptySet()
    }
    
    fun setCurrentRole(role: String) {
        sharedPreferences.edit().putString("role", role).apply()
    }

    fun getUserEmail(): String? = sharedPreferences.getString("email", null)
    fun getUserRole(): String? = sharedPreferences.getString("role", null)
    fun getUserName(): String? = sharedPreferences.getString("name", null)
    fun getUserPhone(): String? = sharedPreferences.getString("phone", null)
    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean("isLoggedIn", false)

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }
}
