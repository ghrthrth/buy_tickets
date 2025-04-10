package com.example.buy_tickets

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.buy_tickets.databinding.ActivityMainBinding
import com.example.buy_tickets.ui.applications.ApplicationsFragment
import com.example.buy_tickets.ui.create_services.CreateServicesFragment
import com.example.buy_tickets.ui.gallery.GalleryFragment
import com.example.buy_tickets.ui.user.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragmentMap = mutableMapOf<Int, Fragment>()
    private var currentFragmentId: Int? = null
    private lateinit var fab: FloatingActionButton // Добавьте в объявления переменных

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверка авторизации
        if (FirebaseAuth.getInstance().currentUser == null) {
            redirectToAuth()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Проверка авторизации
        val auth = FirebaseAuth.getInstance()
        val userPrefs = UserPreferences(this)

        if (auth.currentUser == null || userPrefs.getUserId() == null) {
            redirectToAuth()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFragments()
        setupBottomNavigation()
        handleInitialFragment(savedInstanceState)

        // Инициализация FAB
        fab = findViewById(R.id.fabLogout)
        fab.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                logout()
            }
            .setNegativeButton("Отмена", null)
            .create()
            .show()
    }

    private fun logout() {
        // Анимация загрузки (опционально)
        fab.animate().rotationBy(360f).setDuration(500).start()

        // 1. Выход из Firebase
        FirebaseAuth.getInstance().signOut()

        // 2. Очищаем SharedPreferences
        UserPreferences(this).clearUserData()

        // 3. Выход из Google
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // 4. Перенаправление на экран авторизации
            redirectToAuth()
        }.addOnFailureListener {
            // Если выход из Google не удался, все равно перенаправляем
            redirectToAuth()
        }
    }

    private fun redirectToAuth() {
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupFragments() {
        fragmentMap.apply {
            put(R.id.nav_create_service, CreateServicesFragment())
            put(R.id.nav_gallery, GalleryFragment())
            put(R.id.nav_applications, ApplicationsFragment())
        }
    }

    private fun handleInitialFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            navigateToFragment(R.id.nav_gallery)
        } else {
            currentFragmentId = savedInstanceState.getInt(CURRENT_FRAGMENT_KEY, R.id.nav_gallery)
            binding.bottomNavigation.selectedItemId = currentFragmentId ?: R.id.nav_gallery
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (currentFragmentId != item.itemId) {
                navigateToFragment(item.itemId)
            }
            true
        }
    }

    private fun navigateToFragment(menuItemId: Int) {
        fragmentMap[menuItemId]?.let { fragment ->
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            currentFragmentId = menuItemId
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentFragmentId?.let {
            outState.putInt(CURRENT_FRAGMENT_KEY, it)
        }
    }

    companion object {
        private const val CURRENT_FRAGMENT_KEY = "current_fragment"
    }
}