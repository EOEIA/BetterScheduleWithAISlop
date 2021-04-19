package cz.vitskalicky.lepsirozvrh.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolInfo
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolsListFragment

class SchoolsListActivity : BaseActivity(), UrlDialog.OnManualUrlListener {
    var fragment: SchoolsListFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schools)
        fragment = supportFragmentManager.findFragmentById(R.id.fragmentSchools) as SchoolsListFragment?
        fragment!!.setOnItemClickListener { schoolInfo: SchoolInfo ->
            sendResult(schoolInfo, false)
        }
        fragment!!.setOnManualUrlClickListener { enteredSoFar: String? ->
            UrlDialog().apply { initText = enteredSoFar ?: "" }.show(supportFragmentManager,"url_dialog");
        }
    }

    private fun sendResult(schoolInfo: SchoolInfo, manual: Boolean) {
        val intent = Intent()
        intent.putExtra(EXTRA_URL, schoolInfo.url)
        intent.putExtra(EXTRA_NAME, schoolInfo.name)
        intent.putExtra(EXTRA_ID, schoolInfo.id)
        intent.putExtra(EXTRA_IS_MANUAL, manual)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCEL)
        super.onBackPressed()
    }



    companion object {
        private val EXTRA_BASE = SchoolsListActivity::class.java.canonicalName + ".extra"
        val EXTRA_URL = EXTRA_BASE + ".url"
        val EXTRA_NAME = EXTRA_BASE + ".name"
        val EXTRA_ID = EXTRA_BASE + ".id"
        val EXTRA_IS_MANUAL = EXTRA_BASE + ".is_manual"
        const val RESULT_OK = 0
        const val RESULT_CANCEL = 1
    }

    override fun onManualUrlEntered(url: String) {
        sendResult(SchoolInfo("", url, url), true)
    }
}

class UrlDialog() : DialogFragment() {

    public interface OnManualUrlListener{
        fun onManualUrlEntered(url:String)
    }

    var initText: String = ""
    var editText: EditText? = null
    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (requireActivity() !is OnManualUrlListener){
            throw IllegalStateException("Activity needs to implement OnManualUrlListener")
        }
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.use_url_dialog_title)
        editText = EditText(context)
        editText!!.setHint(R.string.use_url_dialog_hint)
        editText!!.inputType = InputType.TYPE_TEXT_VARIATION_URI
        builder.setView(editText)

        //found on the internet. Should be good enough most times.
        val urlRegex = Regex("^((https?)://)?[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]\$")
        if (initText.matches(urlRegex)) {
            editText!!.setText(initText)
        }

        savedInstanceState?.getString("text")?.ifBlank { null }?.let { editText?.setText(it) }

        builder.setPositiveButton(R.string.ok) { dialogInterface: DialogInterface, button: Int ->
            val url: String = editText?.text.toString()
            (requireActivity() as OnManualUrlListener).onManualUrlEntered(url)
        }

        builder.setNegativeButton(R.string.cancel) { _, _ -> }

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("text", editText?.text?.toString())
        super.onSaveInstanceState(outState)
    }
}
