package com.moon_o.tentoone.tentoone.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.moon_o.tentoone.tentoone.content.NotificationAction;
import com.moon_o.tentoone.tentoone.util.ImageCombineUtil;
import com.moon_o.tentoone.tentoone.util.ToastWrapper;

public class MainActivity extends AppCompatActivity{

    private static final int MY_PERMISSION_REQUEST_STORAGE = 123;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        verifyStoragePermissions(MainActivity.this);

        Intent service = new Intent(MainActivity.this, CaptureService.class);
        service.setAction(NotificationAction.START_ACTION.getString());
        startService(service);

        this.finish();
    }

    private void verifyStoragePermissions(Activity activity) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {

                if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            PERMISSIONS_STORAGE,
                            MY_PERMISSION_REQUEST_STORAGE);
                }
            }

//            if (!CaptureService.checkPermission(activity)) {
//                ActivityCompat.requestPermissions(
//                        activity,
//                        PERMISSIONS_STORAGE,
//                        MY_PERMISSION_REQUEST_STORAGE
//                );
//            }
        }
    }

}
