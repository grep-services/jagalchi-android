package com.moon_o.jagalchi.tentoone.util;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by mucha on 16. 4. 29.
 */
public class AnalyticsApplication extends Application {
    private Tracker _tracker;
    private static final String PROPERTY_ID = "UA-77288963-1";
/*
**  taiyou@grep.services ->  UA-77287157-1
*   admin@grep.services ->   UA-77288963-1
 */

    synchronized public Tracker getDefaultTracker() {
        if (_tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            _tracker = analytics.newTracker(PROPERTY_ID);
        }
        return _tracker;
    }
}
