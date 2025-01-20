package cz.vitskalicky.lepsirozvrh;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.vitskalicky.lepsirozvrh.widget.WidgetsSettings;

//todo remove this class, use MainApplication
public class AppSingleton {
    private static final String TAG = AppSingleton.class.getSimpleName();

    private static AppSingleton instance;
    private static Context ctx;
    //private RequestQueue requestQueue;
    //private RozvrhAPI rozvrhAPI;
    private WidgetsSettings widgetsSettings;

    private AppSingleton(Context context) {
        ctx = context;
        //requestQueue = getRequestQueue();
    }

    public static synchronized AppSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new AppSingleton(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Update these widget settings and don't forget to {@link #saveWidgetsSettings()} afterwards.
     */
    public WidgetsSettings getWidgetsSettings() {
        if (widgetsSettings == null) {
            if (SharedPrefs.contains(ctx, PrefsConsts.WIDGETS_SETTINGS)) {
                ObjectMapper mapper = new ObjectMapper();

                try {
                    widgetsSettings = mapper.readValue(SharedPrefs.getString(ctx, PrefsConsts.WIDGETS_SETTINGS), WidgetsSettings.class);
                } catch (JsonProcessingException e) {
                    widgetsSettings = new WidgetsSettings();
                }
            } else {
                widgetsSettings = new WidgetsSettings();
            }
        }
        return widgetsSettings;

    }

    public void saveWidgetsSettings() {
        // if you ever change this logic, don't forget to change in migration code too
        if (widgetsSettings != null){
            ObjectMapper mapper = new ObjectMapper();

            try {
                String json = mapper.writeValueAsString(widgetsSettings);
                SharedPrefs.setString(ctx, PrefsConsts.WIDGETS_SETTINGS, json);
            } catch (JsonProcessingException e) {
                Log.e(TAG, "Failed to save widgets settings (widgets count: " + widgetsSettings.widgetIds.size() + ")");
            }
        }
    }
}

