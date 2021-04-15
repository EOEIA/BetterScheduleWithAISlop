package cz.vitskalicky.lepsirozvrh.activity;

import android.content.Intent;
import android.os.Bundle;

import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity;

import cz.vitskalicky.lepsirozvrh.R;
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolsListFragment;
import kotlin.Unit;

public class SchoolsListActivity extends BaseActivity {
    private static final String EXTRA_BASE = SchoolsListActivity.class.getCanonicalName() + ".extra";
    public static final String EXTRA_URL = EXTRA_BASE + ".url";
    public static final String EXTRA_NAME = EXTRA_BASE + ".name";
    public static final String EXTRA_ID = EXTRA_BASE + ".id";
    public static final String EXTRA_IS_MANUAL = EXTRA_BASE + ".is_manual";
    public static final int RESULT_OK = 0;
    public static final int RESULT_CANCEL = 1;
    SchoolsListFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schools);

        fragment = (SchoolsListFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentSchools);
        fragment.setOnItemClickListener((schoolInfo, isManualUrl) -> {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_URL, schoolInfo.getUrl());
            intent.putExtra(EXTRA_NAME, schoolInfo.getName());
            intent.putExtra(EXTRA_ID, schoolInfo.getId());
            intent.putExtra(EXTRA_IS_MANUAL, isManualUrl);
            setResult(RESULT_OK, intent);
            finish();
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCEL);
        super.onBackPressed();
    }
}
