package com.example.telestock.ui.create_services;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CreateServicesViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public CreateServicesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
