package com.example.buy_tickets.ui.applications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ApplicationsViewModel {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text
}