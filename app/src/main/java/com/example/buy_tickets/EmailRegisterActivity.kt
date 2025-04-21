package com.example.buy_tickets

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.buy_tickets.databinding.ActivityEmailRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class EmailRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        if (name.isEmpty()) {
            binding.nameEditText.error = "Введите имя"
            binding.nameEditText.requestFocus()
            return
        }

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

        if (password.length < 6) {
            binding.passwordEditText.error = "Пароль должен быть не менее 6 символов"
            binding.passwordEditText.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.confirmPasswordEditText.error = "Пароли не совпадают"
            binding.confirmPasswordEditText.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                saveUserToDatabase(user, name, email, "email")
                                Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this, "Ошибка обновления профиля", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun saveUserToDatabase(user: FirebaseUser, name: String, email: String, provider: String) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        val userData = HashMap<String, Any>()
        userData["uid"] = user.uid
        userData["email"] = email
        userData["isAdmin"] = false
        userData["displayName"] = name
        userData["photoUrl"] = ""
        userData["provider"] = provider

        usersRef.child(user.uid).setValue(userData)
            .addOnSuccessListener { Log.d(TAG, "User data saved to database") }
            .addOnFailureListener { e -> Log.w(TAG, "Failed to save user data", e) }
    }

    companion object {
        private const val TAG = "EmailRegisterActivity"
    }
}