package com.moon_o.jagalchi.jagalchi.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.moon_o.jagalchi.R;
import com.moon_o.jagalchi.jagalchi.content.NotificationAction;
import com.moon_o.jagalchi.jagalchi.util.ImageCombineProcessor;
import com.moon_o.jagalchi.jagalchi.util.ImageCombineUtil;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotListener;
import com.moon_o.jagalchi.jagalchi.util.ScreenshotObserver;

/**
 * Created by mucha on 16. 4. 21.
 */
public class CaptureService extends Service implements ScreenshotListener{

    private static final String TAG = "CAPTURESERVICE";
    private static final int NOTIFICATION_ID = 1;
    private ScreenshotObserver observer;
    private ImageCombineUtil imageCombineUtil;
    private int captureCount;

    private RemoteViews notificationView;
    private Notification notification;
    private NotificationManager notificationManager;


    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if(action.equals(NotificationAction.START_ACTION.getString())) {
            init();
        } else if(action.equals(NotificationAction.STOP_ACTION.getString())) {
            stopForeground(true);
            stopSelf();

        } else if(action.equals(NotificationAction.RESET_ACTION.getString())) {
            showComplexNotification("리셋을 완료하였습니다.");
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else if(action.equals(NotificationAction.GALLERY_ACTION.getString())) {

            Toast.makeText(getApplicationContext(), "갤러리를 불러오는데 성공.", Toast.LENGTH_SHORT).show();

        } else if(action.equals(NotificationAction.SHARE_ACTION.getString())) {

            Log.e(TAG, "share event");
            Toast.makeText(this, "Share event", Toast.LENGTH_SHORT).show();
        }

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

    private void init() {
        imageCombineUtil = ImageCombineUtil.getInstance();
        observer =  new ScreenshotObserver(this);
        observer.start();

        showComplexNotification("10to1 - 스크린샷을 모아 공유해 보세요");
    }

    private void showComplexNotification(String tickerText) {

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

        Intent galleryIntent = new Intent(this, CaptureService.class);
        galleryIntent.setAction(NotificationAction.GALLERY_ACTION.getString());
        PendingIntent galleryPending = PendingIntent.getService(this, 0, galleryIntent, 0);

        Intent shareIntent = new Intent(this, CaptureService.class);
        shareIntent.setAction(NotificationAction.SHARE_ACTION.getString());
        PendingIntent sharePending = PendingIntent.getService(this, 0, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if(notificationManager == null && notificationView == null && notification == null) {

            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification_layout);
        }

        notification = null;

        notification = new NotificationCompat.Builder(this)
                .setTicker(tickerText)
                .setContent(notificationView)
                .setContentIntent(mainPending)
                .setSmallIcon(android.R.drawable.ic_input_get)
                .setNumber(captureCount)
                .build();

        notificationView.setOnClickPendingIntent(R.id.notiExit, stopPending);
        notificationView.setOnClickPendingIntent(R.id.notiReset, resetPending);
        notificationView.setOnClickPendingIntent(R.id.notiShare, sharePending);
        notificationView.setOnClickPendingIntent(R.id.notigallary, galleryPending);

        notificationView.setImageViewResource(R.id.notiExit, android.R.drawable.ic_menu_delete);
        notificationView.setImageViewResource(R.id.notiReset, android.R.drawable.ic_menu_crop);
        notificationView.setImageViewResource(R.id.notiShare, android.R.drawable.ic_menu_set_as);
        notificationView.setImageViewResource(R.id.notigallary, android.R.drawable.ic_menu_camera);
        notificationView.setTextViewText(R.id.notiText, captureCount+"");

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onScreenshotTaken(Uri uri) {
        captureCount++;

        switch(captureCount) {
            case 1:
                new ImageCombineProcessor(getContentResolver(), uri, true).execute();
                break;
            default:
                new ImageCombineProcessor(getContentResolver(), uri, false).execute();
                break;
        }


//        if(captureCount == 1)
//            imageCombineUtil.bitmapFileWrite(
//                    imageCombineUtil.pathCreat(),
//                    BitmapFactory.decodeFile(uri.getPath()));
//        else {
//            String path = imageCombineUtil.getPath();
//            imageCombineUtil.bitmapCombine(
//                    imageCombineUtil.pathCreat(),
//                    BitmapFactory.decodeFile(path),
//                    BitmapFactory.decodeFile(uri.getPath()));
//
//            File deleteBeforeFile = new File(path);
//            deleteBeforeFile.delete();
//        }

        showComplexNotification("공유하시려면 공유 아이콘을 눌러주세요");
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

}
