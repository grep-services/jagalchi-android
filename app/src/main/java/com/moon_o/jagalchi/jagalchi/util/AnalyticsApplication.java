package com.moon_o.jagalchi.jagalchi.util;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by mucha on 16. 4. 29.
 */
public class AnalyticsApplication extends Application {
    private Tracker _tracker;
    private static final String PROPERTY_ID = "NONE";

    synchronized public Tracker getDefaultTracker() {
        if (_tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            _tracker = analytics.newTracker(PROPERTY_ID);
        }
        return _tracker;
    }
}
