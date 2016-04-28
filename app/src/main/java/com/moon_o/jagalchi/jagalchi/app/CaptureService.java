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

import com.moon_o.jagalchi.R;
import com.moon_o.jagalchi.jagalchi.content.NotificationAction;
import com.moon_o.jagalchi.jagalchi.util.ImageCombineProcessor;
import com.moon_o.jagalchi.jagalchi.util.ImageCombineUtil;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotListener;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotObserver;
import com.moon_o.jagalchi.jagalchi.util.ToastWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mucha on 16. 4. 21.
 */
public class CaptureService extends Service implements ScreenshotListener{

    private static final int NOTIFICATION_ID = 1;
    private ScreenshotObserver observer;
    private ImageCombineUtil imageCombineUtil;
    private Map<String, PendingIntent> pendingMap = new HashMap<>();
    private int captureCount;

    private RemoteViews notificationView;
    private Notification notification;
    private NotificationManager notificationManager;

    private Intent notiSlideIntent;

    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if(action.equals(NotificationAction.START_ACTION.getString())) {
            notiSlideUp();
            ToastWrapper.showText(this, getResources().getString(R.string.init_string));
            init();

        } else if(action.equals(NotificationAction.STOP_ACTION.getString())) {
            stopForeground(true);
            stopSelf();

        } else if(action.equals(NotificationAction.RESET_ACTION.getString())) {

            if (captureCount == 0) {
                ToastWrapper.makeText(getApplicationContext(), R.string.capture_no_content).show();
                notiSlideUp();
                return START_STICKY;
            }
            if (!imageCombineUtil.fileDelete(imageCombineUtil.getPath())) {
                ToastWrapper.makeText(getApplicationContext(), R.string.reset_fail).show();
            } else {
                captureCount = 0;
                ToastWrapper.makeText(getApplicationContext(), R.string.reset_success).show();
            }

            showComplexNotification(null);
            notificationManager.notify(NOTIFICATION_ID, notification);
            notiSlideUp();

        } else if(action.equals(NotificationAction.SAVE_ACTION.getString())) {

            if(captureCount == 0) {
                ToastWrapper.makeText(getApplicationContext(), R.string.capture_no_content).show();
                notiSlideUp();
                return START_STICKY;
            }

            if (!imageCombineUtil.mediaStoreInsertImage(
                    getContentResolver(),
                    imageCombineUtil.getPath(),
                    imageCombineUtil.getName()))
                ToastWrapper.makeText(getApplicationContext(), R.string.save_fail).show();
            else {
                ToastWrapper.makeText(getApplicationContext(), R.string.save_success).show();
            }

            notiSlideUp();

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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onScreenshotTaken(Uri uri) {
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
        notiSlideUp();
    }

    public void notiSlideUp() {
        notiSlideIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        this.sendBroadcast(notiSlideIntent);
    }

    private void init() {
        imageCombineUtil = ImageCombineUtil.getInstance();
        observer =  new ScreenshotObserver(this);
        observer.start();

        showComplexNotification(getResources().getString(R.string.init_string));
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
        Intent notifinationIntent = new Intent(this, MainActivity.class);
        notifinationIntent.setAction(NotificationAction.MAIN_ACTION.getString());
        notifinationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent mainPending = PendingIntent.getActivity(this, 0, notifinationIntent, 0);

        Intent stopIntent = new Intent(this, CaptureService.class);
        stopIntent.setAction(NotificationAction.STOP_ACTION.getString());
        PendingIntent stopPending = PendingIntent.getService(this, 0, stopIntent, 0);

        Intent resetIntent = new Intent(this, CaptureService.class);
        resetIntent.setAction(NotificationAction.RESET_ACTION.getString());
        PendingIntent resetPending = PendingIntent.getService(this, 0 , resetIntent, 0);

        Intent saveIntent = new Intent(this, CaptureService.class);
        saveIntent.setAction(NotificationAction.SAVE_ACTION.getString());
        PendingIntent savePending = PendingIntent.getService(this, 0, saveIntent, 0);

//        Intent shareIntent = new Intent(this, CaptureService.class);
//        shareIntent.setAction(NotificationAction.SHARE_ACTION.getString());
//        PendingIntent sharePending = PendingIntent.getService(this, 0, shareIntent, 0);

        pendingMap.put(NotificationAction.MAIN_ACTION.getString(), mainPending);
        pendingMap.put(NotificationAction.STOP_ACTION.getString(), stopPending);
        pendingMap.put(NotificationAction.RESET_ACTION.getString(), resetPending);
        pendingMap.put(NotificationAction.SAVE_ACTION.getString(), savePending);
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
//                .setContent(notificationView)
                .setContentIntent(pendingMap.get(NotificationAction.MAIN_ACTION.getString()))
                .setSmallIcon(R.mipmap.notification)
                .setNumber(captureCount)
                .setDefaults(Notification.DEFAULT_VIBRATE
                        | Notification.DEFAULT_LIGHTS)
                .setPriority(setPriority())
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private int setPriority() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return Notification.PRIORITY_HIGH;
        else
            return Notification.PRIORITY_DEFAULT;
    }


}
