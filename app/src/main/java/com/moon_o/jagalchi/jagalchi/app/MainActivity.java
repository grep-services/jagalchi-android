package com.moon_o.jagalchi.jagalchi.app;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.moon_o.jagalchi.R;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotListener;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotObserver;

public class MainActivity extends AppCompatActivity implements ScreenshotListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ScreenshotObserver so = new ScreenshotObserver(this);
        so.start();

    }

    @Override
    public void onScreenshotTaken(Uri uri) {

    }

}
