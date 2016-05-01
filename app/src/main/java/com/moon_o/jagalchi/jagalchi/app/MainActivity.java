package com.moon_o.jagalchi.jagalchi.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.moon_o.jagalchi.jagalchi.content.NotificationAction;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent service = new Intent(MainActivity.this, CaptureService.class);
        service.setAction(NotificationAction.START_ACTION.getString());
        startService(service);

        this.finish();
    }
}
