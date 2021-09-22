package cz.vitskalicky.lepsirozvrh.settings

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.SharedPrefs
import cz.vitskalicky.lepsirozvrh.Utils.RecreateWithAnimationActivity
import cz.vitskalicky.lepsirozvrh.donations.Donations
import cz.vitskalicky.lepsirozvrh.theme.Theme
import cz.vitskalicky.lepsirozvrh.theme.ThemeData
import java.io.IOException

/**
 * A simple [Fragment] subclass.
 */
class ImportThemeFragment : Fragment() {
    private var donations: Donations? = null
    private var root: View? = null
    private var editTextData: EditText? = null
    private var preloadedString = ""
    private var buttonPaste: Button? = null
    private var buttonOK: Button? = null
    private var buttonClear: Button? = null
    private var tvInfo: TextView? = null
    fun init(donations: Donations?) {
        this.donations = donations
    }

    fun setString(preloadedString: String) {
        this.preloadedString = preloadedString
        if (editTextData != null) {
            editTextData!!.setText(preloadedString)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        if (root == null) {
            root = inflater.inflate(R.layout.fragment_import_theme, container, false)
            editTextData = root!!.findViewById(R.id.editTextData)
            buttonPaste = root!!.findViewById(R.id.buttonPaste)
            buttonOK = root!!.findViewById(R.id.buttonOK)
            buttonClear = root!!.findViewById(R.id.buttonClear)
            tvInfo = root!!.findViewById(R.id.textViewInfo)
        }

        //insert more themes link
        var text = getString(R.string.import_theme_detail)
        val linkStart: Int = text.indexOf("%1")
        if (linkStart >= 0){
            val link = getString(R.string.MORE_THEMES_LINK)
            text = text.format(link)
            tvInfo!!.text = SpannableStringBuilder()
                    .append(text)
                    .apply { setSpan(URLSpan(link), linkStart, linkStart + link.length, 0) }
        }
        tvInfo!!.movementMethod = LinkMovementMethod.getInstance()
        buttonPaste!!.setOnClickListener { v: View? ->
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.primaryClip != null && clipboard.primaryClip!!.itemCount > 0) {
                editTextData!!.setText(clipboard.primaryClip!!.getItemAt(0).text)
            }
        }
        buttonOK!!.setOnClickListener { v: View ->
            if (donations?.isSponsor == true){
                doImport()
            }else{
                donations?.showDialog()
            }
        }
        buttonClear!!.setOnClickListener { v: View? -> editTextData!!.setText("") }
        return root
    }

    override fun onResume() {
        super.onResume()
        if (!preloadedString.isEmpty()) {
            editTextData!!.setText(preloadedString)
            preloadedString = ""
        }
    }

    private fun doImport() {
        AsyncTask.execute {
            var td: ThemeData? = null
            val original = editTextData!!.text.toString().replace("\\s".toRegex(), "") //remove all whitespaces
            var input = original
            if (input.startsWith("https://vitskalicky.github.io/lepsi-rozvrh/motiv-info") || input.startsWith("https://vitskalicky.gitlab.io/lepsi-rozvrh/motiv-info")) {
                val uri = Uri.parse(input)
                input = uri.getQueryParameter("data") ?: ""
            } else if (input.startsWith("lepsi-rozvrh:motiv/")) {
                input = input.substring(19)
            }
            if (input == null || input.isEmpty()) {
                input = original
            }
            try {
                td = ThemeData.parseZipped(input)
            } catch (e: IOException) {
                //try to parse as json (not zipped)
                try {
                    td = ThemeData.parseJson(input)
                } catch (ex: IOException) {
                    //try to parse as base64, but not url-safe
                    input = input.replace('+', '-').replace('/', '_')
                    try {
                        td = ThemeData.parseZipped(input)
                    } catch (exc: IOException) {
                        //try fixing the data (find the magic number (+compression method - always same) of gzip: H4s)
                        try {
                            val index = input.indexOf("H4s")
                            if (index > -1) {
                                td = ThemeData.parseZipped(input.substring(index))
                            }
                        } catch (ignored: IOException) {
                        }
                    }
                }
            }
            val ftd = td
            Handler(Looper.getMainLooper()).post {
                if (ftd != null) {
                    Theme.of(context).themeData = ftd
                    SharedPrefs.setIntPreference(context, R.string.PREFS_DETAIL_LEVEL, 3)
                    SharedPrefs.setStringPreference(context, R.string.PREFS_APP_THEME, "4")
                    val activity: Activity? = activity
                    if (activity is RecreateWithAnimationActivity) {
                        (activity as RecreateWithAnimationActivity).recreateWithAnimation()
                    }
                } else {
                    val s = Snackbar.make(root!!, R.string.import_invalid, Snackbar.LENGTH_LONG)
                    s.show()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }
}