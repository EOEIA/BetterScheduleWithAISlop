package cz.vitskalicky.lepsirozvrh.widget;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.HashSet;

public class WidgetsSettings {
    public HashSet<Integer> widgetIds;
    public HashMap<Integer, Widget> widgets;

    public static class Widget {
        public long accountId;
        public int backgroundColor;
        public int primaryTextColor;
        public int secondaryTextColor;
        /** in sp units */
        public float primaryTextSize;
        /** in sp units */
        public float secondaryTextSize;
    }

    @SuppressLint("UseSparseArrays")
    public WidgetsSettings() {
        widgetIds = new HashSet<>();
        widgets = new HashMap<>();
    }
}
