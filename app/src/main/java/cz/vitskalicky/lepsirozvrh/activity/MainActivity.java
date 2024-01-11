package cz.vitskalicky.lepsirozvrh.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.jaredrummler.cyanea.utils.ColorUtils;

import cz.vitskalicky.lepsirozvrh.BuildConfig;
import cz.vitskalicky.lepsirozvrh.MainApplication;
import cz.vitskalicky.lepsirozvrh.R;
import cz.vitskalicky.lepsirozvrh.SharedPrefs;
import cz.vitskalicky.lepsirozvrh.bakaAPI.login.Login;
import cz.vitskalicky.lepsirozvrh.fragment.RozvrhFragment;
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification;
import cz.vitskalicky.lepsirozvrh.theme.Theme;

public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_TIMER = TAG + "-timer";

    public static final String EXTRA_JUMP_TO_TODAY = MainActivity.class.getCanonicalName() + ".JUMP_TO_TODAY";
    private boolean showedNotiInfo = false;

    Context context = this;

    RozvrhFragment rFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLogin();

        rFragment = (RozvrhFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ColorUtils.darker(Theme.of(this).getCHeaderBg()));
        }
        // ask for permission to show notifications
        // TODO Do this properly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                            if (!isGranted) {
                                ((MainApplication) getApplication()).disableNotification();
                            }
                        }).launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SharedPrefs.getBooleanPreference(context, R.string.THEME_CHANGED, false)) {
            SharedPrefs.setBooleanPreference(context, R.string.THEME_CHANGED, false);
            recreate();
        }

        checkLogin();

        Intent intent = getIntent();
        boolean jumpToToday = intent.getBooleanExtra(EXTRA_JUMP_TO_TODAY, false);
        if (jumpToToday) {
            rFragment.jumpToWeek(0);
            rFragment.setCenterToCurrentLesson(true);
            intent.removeExtra(EXTRA_JUMP_TO_TODAY);
        } else {
            rFragment.setCenterToCurrentLesson(true);
        }
        boolean fromNotification = intent.getBooleanExtra(PermanentNotification.INSTANCE.getEXTRA_NOTIFICATION(), false);
        intent.removeExtra(PermanentNotification.INSTANCE.getEXTRA_NOTIFICATION());
        if (fromNotification && !showedNotiInfo) {
            PermanentNotification.INSTANCE.showInfoDialog(context, false);
            showedNotiInfo = true;
        }

    }

    private boolean finishing = false;
    public void checkLogin() {
        if (finishing){
            return;
        }
        Login login = ((MainApplication)getApplication()).getLogin();
        if (login.checkLogin(this) != null) {
            login.logout();
            finish();
            finishing = true;
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        ((MainApplication) getApplication()).pruneDatabaseAsync();
        rFragment.jumpToWeek(0);
        rFragment.setCenterToCurrentLesson(true);
        moveTaskToBack(true);
    }
}
