package com.example.buy_tickets.ui.create_services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class CreateServicesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is CreateServices Fragment"
    }
    val text: LiveData<String> = _text
}
