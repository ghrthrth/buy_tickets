package com.example.buy_tickets.ui.user

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_IS_ADMIN = "is_admin" // Новый ключ
    }

    fun saveUserData(userId: String, userLogin: String, isAdmin: Boolean = false) {
        with(sharedPref.edit()) {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_LOGIN, userLogin)
            putBoolean(KEY_IS_ADMIN, isAdmin) // Сохраняем флаг администратора
            apply()
        }
    }

    fun getUserId(): String? {
        return sharedPref.getString(KEY_USER_ID, null)
    }

    fun getUserLogin(): String? {
        return sharedPref.getString(KEY_USER_LOGIN, null)
    }

    // Новый метод для проверки, является ли пользователь администратором
    fun isAdmin(): Boolean {
        return sharedPref.getBoolean(KEY_IS_ADMIN, false)
    }

    // Новый метод для установки статуса администратора
    fun setAdmin(isAdmin: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(KEY_IS_ADMIN, isAdmin)
            apply()
        }
    }

    fun clearUserData() {
        with(sharedPref.edit()) {
            remove(KEY_USER_ID)
            remove(KEY_USER_LOGIN)
            remove(KEY_IS_ADMIN) // Удаляем и флаг администратора
            apply()
        }
    }
}