package com.moon_o.tentoone.tentoone.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.moon_o.tentoone.tentoone.content.NotificationAction;

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
