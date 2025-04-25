package com.example.buy_tickets

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.buy_tickets.databinding.ActivityEmailLoginBinding
import com.example.buy_tickets.ui.user.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class EmailLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty()) {
            binding.emailEditText.error = "Введите email"
            binding.emailEditText.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Введите корректный email"
            binding.emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Введите пароль"
            binding.passwordEditText.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkIfUserIsAdmin(user) { isAdmin ->
                            saveUserToPreferences(user, isAdmin)
                            saveUserToDatabase(user, isAdmin)
                            Toast.makeText(this, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show()
                            startMainActivity()
                        }
                    }
                } else {
                    Toast.makeText(this, "Ошибка входа: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun saveUserToPreferences(user: FirebaseUser, isAdmin: Boolean) {
        val userPrefs = UserPreferences(this)
        userPrefs.saveUserData(user.uid, user.email ?: "Anonymous", isAdmin)
    }

    private fun saveUserToDatabase(user: FirebaseUser, isAdmin: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        val userData = hashMapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "provider" to "email",
            "isAdmin" to isAdmin
        )

        usersRef.child(user.uid).setValue(userData)
            .addOnSuccessListener { Log.d(TAG, "User saved to database") }
            .addOnFailureListener { e -> Log.w(TAG, "Failed to save user", e) }
    }

    private fun checkIfUserIsAdmin(user: FirebaseUser, callback: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(user.uid).child("isAdmin")

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isAdmin = task.result?.getValue(Boolean::class.java) ?: false
                callback(isAdmin)
            } else {
                Log.e(TAG, "Error checking admin status", task.exception)
                callback(false)
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "EmailLoginActivity"
    }
}