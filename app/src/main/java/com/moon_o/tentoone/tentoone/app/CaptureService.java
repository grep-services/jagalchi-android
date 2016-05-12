package com.moon_o.tentoone.tentoone.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.moon_o.tentoone.R;
import com.moon_o.tentoone.tentoone.content.NotificationAction;
import com.moon_o.tentoone.tentoone.util.AnalyticsApplication;
import com.moon_o.tentoone.tentoone.util.ImageCombineProcessor;
import com.moon_o.tentoone.tentoone.util.ImageCombineUtil;
import com.moon_o.tentoone.tentoone.util.ScreenshotListener;
import com.moon_o.tentoone.tentoone.util.ScreenshotObserver;
import com.moon_o.tentoone.tentoone.util.ToastWrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

            showDefaultNotification(getResources().getString(R.string.init_app), getResources().getString(R.string.capture_do), getResources().getString(R.string.init_app));

        } else if(action.equals(NotificationAction.STOP_ACTION.getString())) {
            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_run),
                    getResources().getString(R.string.ga_action_run_stop))
                    .build());

            if(captureCount != 0)
                recycle(false);

            stopForeground(true);
            stopSelf();

        } else if(action.equals(NotificationAction.RESET_ACTION.getString())) {

            if (captureCount == 0)
                return START_STICKY;

            if (!imageCombineUtil.isExecutable()) {
                showMessage(getResources().getString(R.string.file_writing));
                return START_STICKY;
            }

            if (!recycle(true)) {
                captureCount = 0;

                tracker.send(new HitBuilders.EventBuilder(
                        getResources().getString(R.string.ga_category_action),
                        getResources().getString(R.string.ga_action_capture_delete))
                        .build());
            }

            showDefaultNotification(getResources().getString(R.string.init_app), getResources().getString(R.string.capture_do), getResources().getString(R.string.init_app));

        } else if(action.equals(NotificationAction.UNDO_ACTION.getString())) {

            if(captureCount == 0)
                return START_STICKY;

            if(captureCount == 1) {
                captureCount--;
                showDefaultNotification(getResources().getString(R.string.init_app), getResources().getString(R.string.capture_do), getResources().getString(R.string.init_app));
                return START_STICKY;
            }

            String undoPath = imageCombineUtil.getImagePathArray().get(imageCombineUtil.getImagePathArray().size()-1);
            imageCombineUtil.fileDelete(undoPath);
            imageCombineUtil.getImagePathArray().remove(undoPath);

            File file = new File(capturedUriArray.get(capturedUriArray.size()-1));
            if(file.exists()) {
                //file write 되자마자 삭제하는 경우 안되기 때문에 1초정도 멈춘다음 삭제해야됨!
                try {
                    Thread.sleep(1000);
                    imageCombineUtil.mediaStoreDeleteImage(this.getContentResolver(), file.getCanonicalPath());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE));
                capturedUriArray.remove(capturedUriArray.get(capturedUriArray.size()-1));
            }

            captureCount--;

            showDefaultNotification(null, null, null);

        } else if(action.equals(NotificationAction.SAVE_ACTION.getString())) {

            if(captureCount == 0)
                return START_STICKY;

            if(!imageCombineUtil.isExecutable()) {
                showMessage(getResources().getString(R.string.file_writing));
                return START_STICKY;
            }

            String path = imageCombineUtil.getImagePathArray().get(imageCombineUtil.getImagePathArray().size()-1);

            if (imageCombineUtil.mediaStoreInsertImage(
                    this.getContentResolver(),
                    path,
                    imageCombineUtil.getName())) {
                tracker.send(new HitBuilders.EventBuilder(
                        getResources().getString(R.string.ga_category_action),
                        getResources().getString(R.string.ga_action_capture_save))
                        .build());

                recycle(true);

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse("file://" + path), "image/*");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);

                showDefaultNotification(getResources().getString(R.string.init_app), getResources().getString(R.string.capture_do), getResources().getString(R.string.init_app));

            }


        } else if(action.equals(NotificationAction.GALLERY_ACTION.getString())) {

            if (captureCount == 0)
                return START_STICKY;

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

            showDefaultNotification(null, null, null);

        } else if(action.equals(NotificationAction.EXCEPTION_ACTION.getString())) {

            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_action),
                    getResources().getString(R.string.ga_action_capture_over_capture))
                    .build());

            recycle(false);

            showDefaultNotification(null, null, null);

        }

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        observer.stop();

        if(captureCount != 0)
            recycle(false);
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
            tracker.send(new HitBuilders.EventBuilder(
                    getResources().getString(R.string.ga_category_action),
                    getResources().getString(R.string.ga_action_capture_over_capture))
                    .build());

            try {
                File file = new File(uri.getPath());
                if(file.exists()) {
                    //file write 되자마자 삭제하는 경우 안되기 때문에 1초정도 멈춘다음 삭제해야됨!
                    try {
                        Thread.sleep(1000);
                        imageCombineUtil.mediaStoreDeleteImage(this.getContentResolver(), file.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                }

                pendingMap.get(NotificationAction.LIMIT_ACTION.getString()).send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }

        showDefaultNotification(null, null, null);
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

                    showDefaultNotification(null, null, null);
                }
            }
        });

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

        Intent undoIntent = new Intent(this, CaptureService.class);
        undoIntent.setAction(NotificationAction.UNDO_ACTION.getString());
        PendingIntent undoPending = PendingIntent.getService(this, 0, undoIntent, 0);

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
        pendingMap.put(NotificationAction.UNDO_ACTION.getString(), undoPending);
        pendingMap.put(NotificationAction.SAVE_ACTION.getString(), savePending);
        pendingMap.put(NotificationAction.EXCEPTION_ACTION.getString(), exceptionPending);
        pendingMap.put(NotificationAction.LIMIT_ACTION.getString(), limitPending);
    }

    private void showDefaultNotification(CharSequence tickerText, CharSequence contentTitle, CharSequence contentText) {
        Resources resource = getResources();
        if(contentTitle == null)
            contentTitle = captureCount+resource.getString(R.string.capture_count_text);
        if(contentText == null)
            contentText = resource.getString(R.string.capture_continue);

        if(pendingMap.isEmpty())
            setPendingIntent();

        if(notificationManager == null)
            notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        notification = null;

        notification = new NotificationCompat.Builder(this)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setShowWhen(false)
                .setLargeIcon(BitmapFactory.decodeResource(resource, R.mipmap.icon))
//                .setColor(ContextCompat.getColor(this, R.color.text))
                .setSmallIcon(R.drawable.small_icon_image)
                .setContentIntent(pendingMap.get(NotificationAction.GALLERY_ACTION.getString()))
                .setPriority(getHeadsUpCheckSdk())
                .addAction(R.drawable.undo_image, resource.getString(R.string.undo_description), pendingMap.get(NotificationAction.UNDO_ACTION.getString()))
                .addAction(R.drawable.reset_image, resource.getString(R.string.reset_description), pendingMap.get(NotificationAction.RESET_ACTION.getString()))
                .addAction(R.drawable.save_image, resource.getString(R.string.save_description), pendingMap.get(NotificationAction.SAVE_ACTION.getString()))
                .setDeleteIntent(test(this, NOTIFICATION_ID))
                .build();

        startForeground(NOTIFICATION_ID, notification);

    }

    public void showMessage(CharSequence message) {
//        this.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        ToastWrapper.showText(this, message);
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

    private PendingIntent test(Context context, int notificationId) {
        Intent intent = new Intent(context, CaptureService.class);
        intent.putExtra("com.my.app.notificationid", NOTIFICATION_ID);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                notificationId, intent, 0);
        return pendingIntent;
    }

    private int getHeadsUpCheckSdk() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return Notification.PRIORITY_MAX;
        else
            return Notification.PRIORITY_DEFAULT;
    }


}
