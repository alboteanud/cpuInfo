package com.alboteanu.cpuinfo_3.ui.dashboard

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text
}