package com.example.buy_tickets.ui.user_cabinet

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

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Проверка авторизации пользователя
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.greetingText.text = "Добро пожаловать!\nАвторизуйтесь для доступа к личному кабинету"
            binding.userDetails.visibility = View.GONE
            return
        }

        // Получение ссылки на данные пользователя
        userRef = database.getReference("users").child(currentUser.uid)

        // Загрузка данных пользователя
        loadUserData()
    }

    private fun loadUserData() {
        // Показываем ProgressBar во время загрузки
        binding.progressBar.visibility = View.VISIBLE
        binding.userDetails.visibility = View.GONE

        // Слушатель для однократного получения данных
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBar.visibility = View.GONE
                binding.userDetails.visibility = View.VISIBLE

                if (snapshot.exists()) {
                    // Получение данных пользователя
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val displayName = snapshot.child("displayName").getValue(String::class.java) ?: ""
                    val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false

                    // Формируем приветствие
                    val greeting = if (displayName.isNotEmpty()) {
                        "Добро пожаловать, $displayName!"
                    } else {
                        "Добро пожаловать!"
                    }

                    // Обновление UI
                    binding.greetingText.text = greeting
                    binding.userEmail.text = email

                    if (isAdmin) {
                        binding.adminBadge.visibility = View.VISIBLE
                    } else {
                        binding.adminBadge.visibility = View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}