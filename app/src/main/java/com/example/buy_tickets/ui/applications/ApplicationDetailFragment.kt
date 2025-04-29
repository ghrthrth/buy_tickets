package com.example.buy_tickets.ui.applications

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.buy_tickets.R
import com.example.buy_tickets.ui.gallery.ProductDetailFragment.Companion.TAG
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import okhttp3.OkHttpClient
import java.io.IOException

class ApplicationDetailFragment(
    private val mContext: Context,
    private val user_id: String,
    private val service_id: String,
    private val title: String,
    private val name: String,
    private val surname: String,
    private val phone: String,
    private val date: String,
    private val time: String,
    private val email: String
) : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_application_detail, container, false)

        try {
            // Fill fields with application information
            val sendDataButton = view.findViewById<Button>(R.id.button_appointment)

            sendDataButton.setOnClickListener {
                sendDeleteRequest()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}", e)
        }

        return view
    }

    interface OnApplicationDeletedListener {
        fun onApplicationDeleted()
    }

    private var deletionListener: OnApplicationDeletedListener? = null

    fun setDeletionListener(listener: OnApplicationDeletedListener) {
        this.deletionListener = listener
    }
    private fun sendDeleteRequest() {
        val url = "https://decadances.ru/buy_tickets/api/add_application/delete.php"
        Log.d(TAG, "Preparing request to: $url")

        val client = OkHttpClient()
        val formBody = okhttp3.FormBody.Builder()
            .add("user_id", user_id)
            .add("service_id", service_id)
            .add("title", title)
            .add("name", name)
            .add("surname", surname)
            .add("phone", phone)
            .add("date", date)
            .add("time", time)
            .add("email", email)
            .build()

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Ошибка соединения: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseString = response.body?.string() ?: "empty response"
                Log.d(TAG, "Server response: $responseString")
                activity?.runOnUiThread {
                    if (response.isSuccessful && responseString.contains("success")) {
                        Toast.makeText(context, "Заявка удалена", Toast.LENGTH_SHORT).show()
                        deletionListener?.onApplicationDeleted() // Уведомляем слушателя
                        dismiss()
// Успешное удаление заявки
                        activity?.runOnUiThread {
                            if (response.isSuccessful && responseString.contains("success")) {
                                Toast.makeText(context, "Заявка удалена", Toast.LENGTH_SHORT).show()
                                // Обновление списка после удаления заявки
                                } else {
                                Toast.makeText(context, "Ошибка при удалении заявки", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        Toast.makeText(context, "Ошибка при удалении заявки", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

}