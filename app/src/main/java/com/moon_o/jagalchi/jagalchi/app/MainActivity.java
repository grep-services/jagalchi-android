package com.moon_o.jagalchi.jagalchi.app;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.moon_o.jagalchi.R;
import com.moon_o.jagalchi.jagalchi.content.NotificationAction;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotListener;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotObserver;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        Intent service = new Intent(MainActivity.this, CaptureService.class);
        service.setAction(NotificationAction.START_ACTION.getString());
        startService(service);
//        Toast.makeText(getApplicationContext(), NotificationAction.START.getString(), Toast.LENGTH_SHORT).show();
    }


}
