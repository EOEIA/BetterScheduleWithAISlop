package cz.vitskalicky.lepsirozvrh.activity

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.SharedPrefs

class WelcomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val checkBox = findViewById<CheckBox>(R.id.checkBoxSendCrashReports)
        val twPrivacyPolicy = findViewById<TextView>(R.id.textViewPrivacyPolicy)
        buttonStart.setOnClickListener { v: View? ->
            val sendReports = checkBox.isChecked
            SharedPrefs.setInt(this, SharedPrefs.LAST_VERSION_SEEN, BuildConfig.VERSION_CODE)
            SharedPrefs.setBoolean(this, getString(R.string.PREFS_SEND_CRASH_REPORTS), sendReports)
            if (application is MainApplication) {
                if (sendReports) {
                    (application as MainApplication).enableSentry()
                } else {
                    (application as MainApplication).diableSentry()
                }
            }
            (application as MainApplication).accountRepository.checkLogin(this)
            finish()
        }

        //add link to privacy policy
        val ppText: String = getString(R.string.privacy_policy)
        val ppLink: String = getString(R.string.PRIVACY_POLICY_LINK)
        twPrivacyPolicy.text = SpannableStringBuilder()
                .append(ppText)
                .apply { setSpan(URLSpan(ppLink), 0, ppText.length, 0) }
        twPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance();

        /*
         * Hide "send crash reports" checkbox on debug builds, because bug reports are allowed on
         * official release builds only. (see build.gradle)
         */if (!BuildConfig.ALLOW_SENTRY) {
            checkBox.visibility = View.GONE
        }
    }
}