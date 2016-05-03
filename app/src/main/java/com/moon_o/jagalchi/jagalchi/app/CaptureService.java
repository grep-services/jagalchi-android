package com.moon_o.jagalchi.jagalchi.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by mucha on 16. 4. 21.
 */
public class CaptureService extends Service implements ScreenshotListener{

    private Tracker tracker;

    private static final int NOTIFICATION_ID = 1;
    private static final int CAPTURE_LIMIT = 10;
    private ScreenshotObserver observer;
    private ImageCombineUtil imageCombineUtil;
    private Map<String, PendingIntent> pendingMap = new HashMap<>();
    private int captureCount;
    private List<String> capturedUriArray = new ArrayList<>();


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

            showMessage(getResources().getString(R.string.init_app));
            showComplexNotification(getResources().getString(R.string.init_app));

        } else if(action.equals(NotificationAction.STOP_ACTION.getString())) {
            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_run),
                    getResources().getString(R.string.ga_action_run_stop))
                    .build());
            recycle(false);
            stopForeground(true);
            stopSelf();

        } else if(action.equals(NotificationAction.RESET_ACTION.getString())) {

            if (captureCount == 0) {
                showMessage(getResources().getString(R.string.capture_no_content));
                return START_STICKY;
            }
            if(!imageCombineUtil.isExecutable()) {
                showMessage(getResources().getString(R.string.file_writing));
                return START_STICKY;
            }

            if (!recycle(false)) {
                showMessage(getResources().getString(R.string.reset_fail));
            } else {
                captureCount = 0;

                tracker.send(new HitBuilders.EventBuilder(
                        getResources().getString(R.string.ga_category_action),
                        getResources().getString(R.string.ga_action_capture_delete))
                        .build());

                showMessage(getResources().getString(R.string.reset_success));
            }

            showComplexNotification(null);
            notificationManager.notify(NOTIFICATION_ID, notification);

        } else if(action.equals(NotificationAction.SAVE_ACTION.getString())) {

            if(captureCount == 0) {
                showMessage(getResources().getString(R.string.capture_no_content));
                return START_STICKY;
            }
            if(!imageCombineUtil.isExecutable()) {
                showMessage(getResources().getString(R.string.file_writing));
              return START_STICKY;
            }

            if (!imageCombineUtil.mediaStoreInsertImage(
                    this.getContentResolver(),
                    imageCombineUtil.getImagePathArray().get(imageCombineUtil.getImagePathArray().size()-1),
                    imageCombineUtil.getName())) {
                showMessage(getResources().getString(R.string.save_fail));
            }
            else {
                tracker.send(new HitBuilders.EventBuilder(
                        getResources().getString(R.string.ga_category_action),
                        getResources().getString(R.string.ga_action_capture_save))
                        .build());

                showMessage(getResources().getString(R.string.save_success));

                recycle(true);
                showComplexNotification(null);
                notificationManager.notify(NOTIFICATION_ID, notification);
            }

        } else if(action.equals(NotificationAction.GALLERY_ACTION.getString())) {

            if (captureCount == 0) {
                showMessage(getResources().getString(R.string.capture_no_content));
                return START_STICKY;
            }

            if (!imageCombineUtil.isExecutable()) {
                showMessage(getResources().getString(R.string.file_writing));
                return START_STICKY;
            }

            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_action),
                    getResources().getString(R.string.ga_action_capture_view)).build());

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse("file://" + imageCombineUtil.getImagePathArray().get(imageCombineUtil.getImagePathArray().size()-1)), "image/*");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);

        } else if(action.equals(NotificationAction.LIMIT_ACTION.getString())) {
            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_action),
                    getResources().getString(R.string.ga_action_capture_limit))
                    .build());

            showMessage(getResources().getString(R.string.capture_limit));
            showComplexNotification(null);
            notificationManager.notify(NOTIFICATION_ID, notification);

        } else if(action.equals(NotificationAction.EXCEPTION_ACTION.getString())) {

            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_action),
                    getResources().getString(R.string.ga_action_capture_oom_capture))
                    .build());

            recycle(false);

            showMessage(getResources().getString(R.string.exception_out_of_memory));
            showComplexNotification(getResources().getString(R.string.exception_out_of_memory));
            notificationManager.notify(NOTIFICATION_ID, notification);

        }

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        observer.stop();
        if(captureCount != 0) {
            recycle(false);
            showMessage(getResources().getString(R.string.exit_reset_app));
        } else
            showMessage(getResources().getString(R.string.exit_app));

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

        if(captureCount == 0) {
            captureCount++;
            capturedUriArray.add(uri.getPath());
            new ImageCombineProcessor(imageCombineUtil.pathCreat(), null, uri.getPath(), true).execute();
        } else if(captureCount < CAPTURE_LIMIT) {
            captureCount++;
            capturedUriArray.add(uri.getPath());
            String combinedPath = imageCombineUtil.getImagePathArray().get(imageCombineUtil.getImagePathArray().size()-1);
            new ImageCombineProcessor(imageCombineUtil.pathCreat(), combinedPath, uri.getPath(), false).execute();
        } else {
            try {
                File file = new File(uri.getPath());
                if(file.exists()) {
                    imageCombineUtil.mediaStoreDeleteImage(this.getContentResolver(), uri.getPath());
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                }
                pendingMap.get(NotificationAction.LIMIT_ACTION.getString()).send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }



        showComplexNotification(getResources().getString(R.string.capture_success));
        notificationManager.notify(NOTIFICATION_ID, notification);
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
                } else {
                    if(captureCount != 0)
                        recycle(false);
                    showMessage(getResources().getString(R.string.exception_occur));
                    showComplexNotification(null);
                    notificationManager.notify(NOTIFICATION_ID, notification);
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

        return view;
    }

    private RemoteViews testBuileNotificationItemMap(Map<String, PendingIntent> pendingMap) {
        if (pendingMap.isEmpty())
            throw new NullPointerException("PendingIntent elements is null");

        RemoteViews view = new RemoteViews(getPackageName(), R.layout.notificationsosul_layout);

        view.setOnClickPendingIntent(R.id.noti_8, pendingMap.get(NotificationAction.STOP_ACTION.getString()));
        view.setOnClickPendingIntent(R.id.noti_7, pendingMap.get(NotificationAction.RESET_ACTION.getString()));
        view.setOnClickPendingIntent(R.id.noti_6, pendingMap.get(NotificationAction.SAVE_ACTION.getString()));

        view.setTextViewText(R.id.noti_3, captureCount+"");

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

        Intent limitIntent = new Intent(this, CaptureService.class);
        limitIntent.setAction(NotificationAction.LIMIT_ACTION.getString());
        PendingIntent limitPending = PendingIntent.getService(this, 0, limitIntent, 0);

        pendingMap.put(NotificationAction.GALLERY_ACTION.getString(), galleryPending);
        pendingMap.put(NotificationAction.STOP_ACTION.getString(), stopPending);
        pendingMap.put(NotificationAction.RESET_ACTION.getString(), resetPending);
        pendingMap.put(NotificationAction.SAVE_ACTION.getString(), savePending);
        pendingMap.put(NotificationAction.EXCEPTION_ACTION.getString(), exceptionPending);
        pendingMap.put(NotificationAction.LIMIT_ACTION.getString(), limitPending);
    }

    private void showComplexNotification(String tickerText) {
        if(pendingMap.isEmpty())
            setPendingIntent();

        if(notificationManager == null)
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationView = null;
        notificationView = testBuileNotificationItemMap(pendingMap);
//                buildNotificationItem(pendingMap);

        notification = null;
        notification = new NotificationCompat.Builder(this)
                .setTicker(tickerText)
//                .setContent(notificationView)
                .setContentIntent(pendingMap.get(NotificationAction.GALLERY_ACTION.getString()))
                .setSmallIcon(R.mipmap.icon)
                .setNumber(captureCount)
//                .setDefaults(Notification.DEFAULT_VIBRATE
//                        | Notification.DEFAULT_LIGHTS)
                .setPriority(getHeadsUpCheckSdk())
                .build();
        notification.bigContentView = notificationView;

        startForeground(NOTIFICATION_ID, notification);
    }



    public void showMessage(CharSequence message) {
        closeDialog();
        ToastWrapper.showText(getApplicationContext(), message);
    }

    public void closeDialog() {
        this.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    private boolean recycle(boolean newAction) {
        try {
            Iterator imagePathIter;
            if(!newAction) {
                imagePathIter = imageCombineUtil.getImagePathArray().iterator();
                while (imagePathIter.hasNext()) {
                    String deletePath = (String) imagePathIter.next();
                    imageCombineUtil.fileDelete(deletePath);
                }
            } else {
                for(int i = 0; i < imageCombineUtil.getImagePathArray().size()-1; i++) {
                    imageCombineUtil.fileDelete(imageCombineUtil.getImagePathArray().get(i));
                }
            }

            Iterator capturedPathIter = capturedUriArray.iterator();

            while (capturedPathIter.hasNext()) {
                String deletePath = (String) capturedPathIter.next();
                imageCombineUtil.mediaStoreDeleteImage(this.getContentResolver(), deletePath);
            }

            captureCount = 0;
            imageCombineUtil.getImagePathArray().clear();
            capturedUriArray.clear();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private int getHeadsUpCheckSdk() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return Notification.PRIORITY_MAX;
        else
            return Notification.PRIORITY_DEFAULT;
    }


}
