package com.moon_o.jagalchi.jagalchi.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.moon_o.jagalchi.R;
import com.moon_o.jagalchi.jagalchi.content.NotificationAction;
import com.moon_o.jagalchi.jagalchi.util.AnalyticsApplication;
import com.moon_o.jagalchi.jagalchi.util.ImageCombineProcessor;
import com.moon_o.jagalchi.jagalchi.util.ImageCombineUtil;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotListener;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotObserver;
import com.moon_o.jagalchi.jagalchi.util.ToastWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by mucha on 16. 4. 21.
 */
public class CaptureService extends Service implements ScreenshotListener{

    private Tracker tracker;

    private static final int NOTIFICATION_ID = 1;
    private ScreenshotObserver observer;
    private ImageCombineUtil imageCombineUtil;
    private Map<String, PendingIntent> pendingMap = new HashMap<>();
    private int captureCount;

    private RemoteViews notificationView;
    private Notification notification;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        tracker = ((AnalyticsApplication)getApplication()).getDefaultTracker();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if(action.equals(NotificationAction.START_ACTION.getString())) {
            init();
            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_run),
                    getResources().getString(R.string.ga_action_run_start))
                    .build());

            closeDialog();
            ToastWrapper.showText(this, getResources().getString(R.string.init_app));
            showComplexNotification(getResources().getString(R.string.init_app));

        } else if(action.equals(NotificationAction.STOP_ACTION.getString())) {
            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_run),
                    getResources().getString(R.string.ga_action_run_stop))
                    .build());

            stopForeground(true);
            stopSelf();

        } else if(action.equals(NotificationAction.RESET_ACTION.getString())) {

            if (captureCount == 0) {
                closeDialog();
                ToastWrapper.makeText(getApplicationContext(), R.string.capture_no_content).show();
                return START_STICKY;
            }
            if (!imageCombineUtil.fileDelete(imageCombineUtil.getPath())) {
                ToastWrapper.makeText(getApplicationContext(), R.string.reset_fail).show();
            } else {
                captureCount = 0;

                tracker.send(new HitBuilders.EventBuilder(
                        getResources().getString(R.string.ga_category_action),
                        getResources().getString(R.string.ga_action_capture_delete))
                        .build());

                ToastWrapper.makeText(getApplicationContext(), R.string.reset_success).show();
            }

            closeDialog();
            showComplexNotification(null);
            notificationManager.notify(NOTIFICATION_ID, notification);

        } else if(action.equals(NotificationAction.SAVE_ACTION.getString())) {

            if(captureCount == 0) {
                closeDialog();
                ToastWrapper.makeText(getApplicationContext(), R.string.capture_no_content).show();
                return START_STICKY;
            }

            if (!imageCombineUtil.mediaStoreInsertImage(
                    getContentResolver(),
                    imageCombineUtil.getPath(),
                    imageCombineUtil.getName())) {
                closeDialog();
                ToastWrapper.makeText(getApplicationContext(), R.string.save_fail).show();
            }
            else {
                tracker.send(new HitBuilders.EventBuilder(
                        getResources().getString(R.string.ga_category_action),
                        getResources().getString(R.string.ga_action_capture_save))
                        .build());

                closeDialog();
                ToastWrapper.makeText(getApplicationContext(), R.string.save_success).show();
            }
        } else if(action.equals(NotificationAction.EXCEPTION_ACTION.getString())) {

            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_action),
                    getResources().getString(R.string.ga_action_capture_oom_capture))
                    .build());

            recycle();

            closeDialog();
            ToastWrapper.showText(getApplicationContext(), getResources().getString(R.string.exception_out_of_memory));
            showComplexNotification(getResources().getString(R.string.exception_out_of_memory));
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else if(action.equals(NotificationAction.GALLERY_ACTION.getString())) {

            if (captureCount == 0) {
                closeDialog();
                ToastWrapper.makeText(getApplicationContext(), R.string.capture_no_content).show();
                return START_STICKY;
            }

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse("file://" + imageCombineUtil.getPath()), "image/*");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
//        공유 기능 제외
//        else if(action.equals(NotificationAction.SHARE_ACTION.getString())) {
//            if(captureCount == 0) {
//                ToastWrapper.makeText(getApplicationContext(), R.string.capture_no_content).show();
//                notiSlideUp();
//                return START_STICKY;
//            }
//
//            Uri screenshotUri = Uri.parse("file://"+imageCombineUtil.getPath());
//            File file = new File(screenshotUri.getPath());
//            Bitmap bitmap = decodeFile(file);
//            imageCombineUtil.bitmapFileWrite(file.getPath(), bitmap);
//
////            Log.e("SHARE", imageCombineUtil.getPath());
//            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
//            sharingIntent.setType("image/jpeg");
//            sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(file.getPath()));
//            Intent chooserintent = Intent.createChooser(sharingIntent, "Share Image");
//            chooserintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(chooserintent);
//
//            showComplexNotification(getResources().getString(R.string.share_string));
//            notificationManager.notify(NOTIFICATION_ID, notification);
//            notiSlideUp();
//        }

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        observer.stop();
        if(captureCount != 0) {
            recycle();
            closeDialog();
            ToastWrapper.showText(this, getResources().getString(R.string.exit_reset_app));
        } else {
            closeDialog();
            ToastWrapper.showText(this, getResources().getString(R.string.exit_app));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onScreenshotTaken(Uri uri) {
        tracker.send(new HitBuilders.EventBuilder(
                getResources().getString(R.string.ga_category_action),
                getResources().getString(R.string.ga_action_capture_capturing))
                .build());

        captureCount++;

        switch(captureCount) {
            case 1:
                new ImageCombineProcessor(imageCombineUtil.pathCreat(), null, uri.getPath(), true).execute();
                break;
            default:
                String combinedPath = imageCombineUtil.getPath();
                new ImageCombineProcessor(imageCombineUtil.pathCreat(), combinedPath, uri.getPath(), false).execute();
                break;
        }

        showComplexNotification(getResources().getString(R.string.share_pressbtn_pless));
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void closeDialog() {
        this.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    private void init() {
        int timeout_hour = 600;
        tracker.setScreenName(getClass().getSimpleName());
        tracker.setSessionTimeout(timeout_hour * 30);
        tracker.send(new HitBuilders.ScreenViewBuilder()
            .setNewSession()
            .build());

        imageCombineUtil = ImageCombineUtil.getInstance();
        observer =  new ScreenshotObserver(this);
        observer.start();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Throwable t = throwable.getCause();
                if(t instanceof OutOfMemoryError) {
                    try {
                        pendingMap.get(NotificationAction.EXCEPTION_ACTION.getString()).send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

    }

    private RemoteViews buildNotificationItem(Map<String, PendingIntent> pendingMap) {
        if (pendingMap.isEmpty())
            throw new NullPointerException("PendingIntent elements is null");

        RemoteViews view = new RemoteViews(getPackageName(), R.layout.notification_layout);

        view.setOnClickPendingIntent(R.id.noti_close_image, pendingMap.get(NotificationAction.STOP_ACTION.getString()));
        view.setOnClickPendingIntent(R.id.noti_delete_image, pendingMap.get(NotificationAction.RESET_ACTION.getString()));
        view.setOnClickPendingIntent(R.id.noti_save_image, pendingMap.get(NotificationAction.SAVE_ACTION.getString()));

        view.setImageViewResource(R.id.noti_close_image, R.mipmap.close);
        view.setImageViewResource(R.id.noti_delete_image, R.mipmap.delete);
        view.setImageViewResource(R.id.noti_save_image, R.mipmap.save);
        view.setTextViewText(R.id.noti_capture_count_text, captureCount+getResources().getString(R.string.capture_count));

//        view.setOnClickPendingIntent(R.id.notiExit, pendingMap.get(NotificationAction.STOP_ACTION.getString()));
//        view.setOnClickPendingIntent(R.id.notiReset, pendingMap.get(NotificationAction.RESET_ACTION.getString()));
//        view.setOnClickPendingIntent(R.id.notiShare, pendingMap.get(NotificationAction.SAVE_ACTION.getString()));
//
//        view.setImageViewResource(R.id.notiExit, android.R.drawable.ic_menu_close_clear_cancel);
//        view.setImageViewResource(R.id.notiReset, android.R.drawable.ic_menu_delete);
//        view.setImageViewResource(R.id.notiShare, android.R.drawable.ic_menu_save);
//        view.setTextViewText(R.id.notiText, captureCount+"");
        return view;
    }

    private void setPendingIntent() {

        Intent galleryIntent = new Intent(this, CaptureService.class);
        galleryIntent.setAction(NotificationAction.GALLERY_ACTION.getString());
        PendingIntent galleryPending = PendingIntent.getService(this, 0, galleryIntent, 0);

        Intent stopIntent = new Intent(this, CaptureService.class);
        stopIntent.setAction(NotificationAction.STOP_ACTION.getString());
        PendingIntent stopPending = PendingIntent.getService(this, 0, stopIntent, 0);

        Intent resetIntent = new Intent(this, CaptureService.class);
        resetIntent.setAction(NotificationAction.RESET_ACTION.getString());
        PendingIntent resetPending = PendingIntent.getService(this, 0 , resetIntent, 0);

        Intent saveIntent = new Intent(this, CaptureService.class);
        saveIntent.setAction(NotificationAction.SAVE_ACTION.getString());
        PendingIntent savePending = PendingIntent.getService(this, 0, saveIntent, 0);

        Intent exceptionIntent = new Intent(this, CaptureService.class);
        exceptionIntent.setAction(NotificationAction.EXCEPTION_ACTION.getString());
        PendingIntent exceptionPending = PendingIntent.getService(this, 0, exceptionIntent, 0);

//        Intent shareIntent = new Intent(this, CaptureService.class);
//        shareIntent.setAction(NotificationAction.SHARE_ACTION.getString());
//        PendingIntent sharePending = PendingIntent.getService(this, 0, shareIntent, 0);

        pendingMap.put(NotificationAction.GALLERY_ACTION.getString(), galleryPending);
        pendingMap.put(NotificationAction.STOP_ACTION.getString(), stopPending);
        pendingMap.put(NotificationAction.RESET_ACTION.getString(), resetPending);
        pendingMap.put(NotificationAction.SAVE_ACTION.getString(), savePending);
        pendingMap.put(NotificationAction.EXCEPTION_ACTION.getString(), exceptionPending);
    }

    private void showComplexNotification(String tickerText) {
        if(pendingMap.isEmpty())
            setPendingIntent();

        if(notificationManager == null)
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationView = null;
        notificationView = buildNotificationItem(pendingMap);

        notification = null;
        notification = new NotificationCompat.Builder(this)
                .setTicker(tickerText)
                .setContent(notificationView)
                .setContentIntent(pendingMap.get(NotificationAction.GALLERY_ACTION.getString()))
                .setSmallIcon(R.mipmap.notification)
                .setNumber(captureCount)
                .setDefaults(Notification.DEFAULT_VIBRATE
                        | Notification.DEFAULT_LIGHTS)
                .setPriority(setCheckSdk())
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void recycle() {
        imageCombineUtil.fileDelete(imageCombineUtil.getPath());
        imageCombineUtil.fileDelete(imageCombineUtil.getCombinedPath());
        captureCount = 0;
    }

    private int setCheckSdk() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return Notification.PRIORITY_MAX;
        else
            return Notification.PRIORITY_DEFAULT;
    }


}
