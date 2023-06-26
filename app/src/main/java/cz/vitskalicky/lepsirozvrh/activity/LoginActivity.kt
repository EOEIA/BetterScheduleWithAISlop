package cz.vitskalicky.lepsirozvrh.activity

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.model.AccountRepository.LoginResult
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolInfo
import cz.vitskalicky.lepsirozvrh.theme.Theme
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    lateinit var tilUsername: TextInputLayout
    lateinit var tilPassword: TextInputLayout
    lateinit var tilURL: TextInputLayout
    lateinit var etUrl: EditText
    lateinit var bChooseSchool: Button
    lateinit var bLogin: Button
    lateinit var progressBar: ProgressBar
    lateinit var twMessage: TextView

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        tilUsername = findViewById(R.id.textInputLayoutName)
        tilPassword = findViewById(R.id.textInputLayoutPassword)
        tilURL = findViewById(R.id.textInputLayoutURL)
        etUrl = findViewById(R.id.textEditURL)
        bChooseSchool = findViewById(R.id.buttonSchoolList)
        bLogin = findViewById(R.id.buttonLogin)
        progressBar = findViewById(R.id.progressBar)
        twMessage = findViewById(R.id.textViewMessage)
        tilUsername.isErrorEnabled = true
        tilPassword.isErrorEnabled = true
        tilURL.isErrorEnabled = true
        progressBar.visibility = View.GONE
        twMessage.text = ""
        twMessage.setTextColor(Theme.of(this).cError)


        if (tilUsername.editText!!.text.toString().isEmpty()) tilUsername.editText!!.setText(SharedPrefs.getString(this, SharedPrefs.USERNAME))
        if (viewModel.schoolInfo == null){
            viewModel.schoolInfo = SchoolInfo(
                    SharedPrefs.getString(this, SharedPrefs.SCHOOL_ID),
                    SharedPrefs.getString(this, SharedPrefs.SCHOOL_NAME).ifBlank { SharedPrefs.getString(this, SharedPrefs.URL) }.ifBlank { getString(R.string.no_school_selected) },
                    SharedPrefs.getString(this, SharedPrefs.URL)
            )
        }
        (applicationContext as MainApplication).accountRepository.logout()
        val chooseSchoolListener = View.OnClickListener { v: View? ->
            val intent = Intent(this, SchoolsListActivity::class.java)
            startActivityForResult(intent, REQUEST_PICK_SCHOOL)
        }
        bChooseSchool.setOnClickListener(chooseSchoolListener)
        tilURL.setOnClickListener(chooseSchoolListener)
        etUrl.setOnClickListener(chooseSchoolListener)
        bLogin.setOnClickListener {
            logIn()
        }

        tilURL.editText!!.setText(viewModel.schoolInfo?.name ?: "")

        tilUsername.editText!!.addTextChangedListener{
            tilUsername.error = null
            tilPassword.error = null
        }
        tilPassword.editText!!.addTextChangedListener{
            tilUsername.error = null
            tilPassword.error = null
        }

        if (BuildConfig.DEBUG) {
            bChooseSchool.setOnLongClickListener {
                lifecycleScope.launch {
                    (application as MainApplication).schoolsDb.schoolDAO().nukeTable()
                }
                Toast.makeText(this, "Schools list cleared", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }


    private fun clearErrors(){
        tilUsername.error = null
        tilPassword.error = null
        tilURL.error = null
    }

    override fun onResume() {
        super.onResume()
        updateChooseSchoolButtonText()
    }

    fun logIn() {
        bLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE
        twMessage.text = ""

        val url: String = viewModel.schoolInfo?.url ?: ""

        clearErrors()

        if (url.isBlank()) {
            tilURL.error = getText(R.string.enter_school)
            bLogin.isEnabled = true
            progressBar.visibility = View.GONE
            return
        }
        if (tilUsername.editText!!.text.toString().isBlank()) {
            tilUsername.error = getText(R.string.enter_username)
            bLogin.isEnabled = true
            progressBar.visibility = View.GONE
            return
        }
        if (tilPassword.editText!!.text.toString().isBlank()) {
            tilPassword.error = getText(R.string.enter_password)
            bLogin.isEnabled = true
            progressBar.visibility = View.GONE
            return
        }
        lifecycleScope.launch {
            val result = (applicationContext as MainApplication).accountRepository.firstLogin(url, tilUsername.editText!!.text.toString(), tilPassword.editText!!.text.toString(), viewModel.isManualUrl)

            if (result == LoginResult.SUCCESS) {
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
                return@launch
            }
            if (result == LoginResult.WRONG_LOGIN) {
                tilUsername.error = getText(R.string.invalid_login)
                tilPassword.error = getText(R.string.invalid_login)
            }
            if (result == LoginResult.UNREACHABLE) {
                //check internet connection and decide on message
                if (KotlinUtils.isOnline()){
                    if (viewModel.isManualUrl){
                        tilURL.error = getText(R.string.unreachable)
                    }else{
                        tilURL.error = getText(R.string.school_unreachable)
                    }
                }else {
                    twMessage.setText(R.string.no_internet)
                }
            }
            if (result == LoginResult.UNEXPECTED_RESPONSE) {
                tilURL.error = getText(R.string.unexpected_response)
            }
            /*if (code === Login.ROZVRH_DISABLED) {
                tilURL!!.error = " "
                twMessage!!.setText(R.string.schedule_disabled)
            }*/

            bLogin.isEnabled = true
            progressBar.visibility = View.GONE
        }
    }

    fun updateChooseSchoolButtonText() {
        if(viewModel.schoolInfo?.url.isNullOrBlank()){
            bChooseSchool.setText(R.string.choose_school)
        }else{
            bChooseSchool.setText(R.string.change_school)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_SCHOOL && resultCode == SchoolsListActivity.RESULT_OK && data != null) {
            val url = data.getStringExtra(SchoolsListActivity.EXTRA_URL)
            val name = data.getStringExtra(SchoolsListActivity.EXTRA_NAME)
            val id = data.getStringExtra(SchoolsListActivity.EXTRA_ID)
            val isManualUrl = data.getBooleanExtra(SchoolsListActivity.EXTRA_IS_MANUAL, false)
            if (url != null && name != null && id != null) {
                viewModel.schoolInfo = SchoolInfo(id, name, url)
                viewModel.isManualUrl = isManualUrl
                tilURL.editText?.setText(viewModel.schoolInfo?.name)

                SharedPrefs.setString(this, SharedPrefs.SCHOOL_ID, id )
                SharedPrefs.setString(this, SharedPrefs.SCHOOL_NAME, name )
                SharedPrefs.setString(this, SharedPrefs.URL, url )

                clearErrors()
            } else {
                Log.e(TAG, "No extra containing url (extra key: " + SchoolsListActivity.EXTRA_URL + ")")
            }
        }
        updateChooseSchoolButtonText()
    }

    companion object {
        val TAG = LoginActivity::class.java.simpleName

        /**
         * when you want this activity to log out as soon as it starts, pass this in intent extras.
         */
        const val LOGOUT = "logout"
        const val REQUEST_PICK_SCHOOL = 64585 //random number
    }
}

class LoginViewModel(val app: Application): AndroidViewModel(app){

    var schoolInfo: SchoolInfo? = null
    var isManualUrl = true

}