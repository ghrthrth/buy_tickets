package com.example.buy_tickets.ui.user_cabinet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.buy_tickets.databinding.FragmentUserCabinetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserCabinetFragment : Fragment() {
    private var _binding: FragmentUserCabinetBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference

    // Add admin email address (replace with your actual admin email)
    private val adminEmail = "admin@example.com"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserCabinetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Check user authentication
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.greetingText.text = "Добро пожаловать!\nАвторизуйтесь для доступа к личному кабинету"
            binding.userDetails.visibility = View.GONE
            return
        }

        // Get reference to user data
        userRef = database.getReference("users").child(currentUser.uid)

        // Load user data
        loadUserData()

        // Set up contact admin button
        binding.contactAdminButton.setOnClickListener {
            sendEmailToAdmin()
        }
    }

    private fun loadUserData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.userDetails.visibility = View.GONE

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBar.visibility = View.GONE
                binding.userDetails.visibility = View.VISIBLE

                if (snapshot.exists()) {
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val displayName = snapshot.child("displayName").getValue(String::class.java) ?: ""
                    val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false

                    val greeting = if (displayName.isNotEmpty()) {
                        "Добро пожаловать, $displayName!"
                    } else {
                        "Добро пожаловать!"
                    }

                    binding.greetingText.text = greeting
                    binding.userEmail.text = email

                    if (isAdmin) {
                        binding.adminBadge.visibility = View.VISIBLE
                        binding.contactAdminButton.visibility = View.GONE
                    } else {
                        binding.adminBadge.visibility = View.GONE
                        binding.contactAdminButton.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                binding.greetingText.text = "Ошибка загрузки данных"
                binding.userDetails.visibility = View.GONE
            }
        })
    }

    private fun sendEmailToAdmin() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(adminEmail))
            putExtra(Intent.EXTRA_SUBJECT, "Обращение в поддержку")
            putExtra(Intent.EXTRA_TEXT, "Уважаемая администрация,\n\n")
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Отправить письмо"))
        } catch (e: Exception) {
            // Handle case where no email app is available
            binding.greetingText.text = "Не найдено приложение для отправки email"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}