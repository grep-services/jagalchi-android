package com.moon_o.jagalchi.jagalchi.util;

/**
 * Created by mucha on 16. 4. 21.
 */
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;

public class ScreenshotObserver extends FileObserver{
    public static final String PATH = Environment.getExternalStorageDirectory().toString() + "/Pictures/Screenshots/";

    private ScreenshotListener mListener;
    private String mLastTakenPath;

    Context context;

    public ScreenshotObserver(ScreenshotListener listener) {
        super(PATH, FileObserver.CLOSE_WRITE);
        mListener = listener;

    }

    public ScreenshotObserver() {
        super(PATH, FileObserver.CLOSE_WRITE);
    }

//    public void setWatcher(ScreenshotListener listener) {
//        mListener = listener;
//    }

    @Override
    public void onEvent(int event, String name) {


        if (name == null || event != FileObserver.CLOSE_WRITE) {
//            Log.e("ScreenshotObserver", "Application EXIT");
        }
//        else if (mLastTakenPath != null && name.equalsIgnoreCase(mLastTakenPath)) {
//            Log.e("ScreenshotObserver", "Not support same name");
//            Log.e("ScreenshotObserver", name);
//        }
        else {
            mLastTakenPath = name;
            File file = new File(PATH+name);
            mListener.onScreenshotTaken(Uri.fromFile(file));
        }

    }

    public void start() {
        super.startWatching();
    }

    public void stop() {
        super.stopWatching();
    }

}
