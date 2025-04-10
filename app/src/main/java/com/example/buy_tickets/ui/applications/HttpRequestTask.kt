package com.example.buy_tickets.ui.applications

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class HttpRequestTask(
    private val mContext: Context,
    private val urls: String,
    private val params: Map<String, String>
) : AsyncTask<String, Void, String>() {

    @SuppressLint("StaticFieldLeak")
    override fun doInBackground(vararg doInBackgroundParams: String): String? {
        try {
            val json = JSONObject()
            for ((key, value) in params) {
                json.put(key, value)
            }

            val jsonData = json.toString()
            val requestBody = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                jsonData
            )

            val request = Request.Builder()
                .url(urls)
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    private fun requestBodyToString(requestBody: RequestBody): String {
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        return buffer.readUtf8()
    }

    override fun onPostExecute(result: String?) {
        if (result != null) {
            Toast.makeText(mContext, "Ошибка: $result", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(mContext, "Данные успешно записаны", Toast.LENGTH_SHORT).show()
        }
    }
}