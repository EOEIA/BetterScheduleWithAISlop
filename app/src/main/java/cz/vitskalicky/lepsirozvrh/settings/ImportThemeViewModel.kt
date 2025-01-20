package cz.vitskalicky.lepsirozvrh.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import cz.vitskalicky.lepsirozvrh.MainApplication

class ImportThemeViewModel(application: Application): AndroidViewModel(application) {
    val textFieldTextLD = MutableLiveData<String>("")
    var textFieldText: String
        get() = textFieldTextLD.value!!
        set(value) {
            textFieldTextLD.value = value
        };
}