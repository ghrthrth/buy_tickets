package com.example.buy_tickets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.buy_tickets.databinding.ActivityMainBinding
import com.example.buy_tickets.ui.create_services.CreateServicesFragment
import com.example.buy_tickets.ui.gallery.GalleryFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragmentMap = mutableMapOf<Int, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFragments()
        setupBottomNavigation()

        // Установка начального фрагмента
        loadInitialFragment()
    }

    private fun setupFragments() {
        fragmentMap.apply {
            put(R.id.nav_create_service, CreateServicesFragment())
            put(R.id.nav_gallery, GalleryFragment())
        }
    }

    private fun loadInitialFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GalleryFragment())
            .commit()
        binding.bottomNavigation.selectedItemId = R.id.nav_gallery
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            fragmentMap[item.itemId]?.let { fragment ->
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            }
            true
        }
    }

}