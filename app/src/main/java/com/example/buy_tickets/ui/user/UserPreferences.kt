package com.example.buy_tickets.ui.user

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_LOGIN = "user_login"
    }

    fun saveUserData(userId: String, userLogin: String) {
        with(sharedPref.edit()) {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_LOGIN, userLogin)
            apply()
        }
    }

    fun getUserId(): String? {
        return sharedPref.getString(KEY_USER_ID, null)
    }

    fun getUserLogin(): String? {
        return sharedPref.getString(KEY_USER_LOGIN, null)
    }

    fun clearUserData() {
        with(sharedPref.edit()) {
            remove(KEY_USER_ID)
            remove(KEY_USER_LOGIN)
            apply()
        }
    }
}