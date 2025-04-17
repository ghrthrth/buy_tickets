package com.example.buy_tickets

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.buy_tickets.databinding.ActivityAuthBinding
import com.example.buy_tickets.ui.user.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private val TAG = "AuthActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация Firebase Auth
        try {
            auth = FirebaseAuth.getInstance()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Firebase not initialized", e)
            Toast.makeText(this, "Ошибка инициализации Firebase", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Настройка Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Проверка авторизации
        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        binding.emailRegisterButton.setOnClickListener {
            startActivity(Intent(this, EmailRegisterActivity::class.java))
        }

        binding.emailLoginButton.setOnClickListener {
            startActivity(Intent(this, EmailLoginActivity::class.java))
        }

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
        binding.progressBar.visibility = View.VISIBLE
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e(TAG, "Ошибка входа (Код ${e.statusCode}): ${e.message}")
                showError("Ошибка входа. Попробуйте снова")
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onAuthSuccess(auth.currentUser)
                } else {
                    onAuthFailed(task.exception)
                }
                binding.progressBar.visibility = View.GONE
            }
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

    private fun onAuthFailed(exception: Exception?) {
        Log.w(TAG, "signInWithCredential:failure", exception)
        showError("Ошибка аутентификации: ${exception?.message}")
    }

    private fun onAuthSuccess(user: FirebaseUser?) {
        user?.let {
            Log.d(TAG, "signInWithCredential:success")

            checkIfUserIsAdmin(user) { isAdmin ->
                saveUserToDatabase(user, isAdmin)

                // Сохраняем данные пользователя в SharedPreferences
                val userPrefs = UserPreferences(this)
                userPrefs.saveUserData(user.uid, user.email ?: user.displayName ?: "Anonymous", isAdmin)

                startMainActivity()
            }
        }
    }

    private fun saveUserToDatabase(user: FirebaseUser, isAdmin: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        val userData = hashMapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "provider" to "google",
            "isAdmin" to isAdmin
        )

        usersRef.child(user.uid).setValue(userData)
            .addOnSuccessListener { Log.d(TAG, "User saved to database") }
            .addOnFailureListener { e -> Log.w(TAG, "Failed to save user", e) }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}