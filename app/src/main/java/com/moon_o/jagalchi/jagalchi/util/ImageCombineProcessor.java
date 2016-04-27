package com.moon_o.jagalchi.jagalchi.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by mucha on 16. 4. 27.
 */
public class ImageCombineProcessor extends AsyncTask<Void, Integer, Void> {
    private Context context;
    private String combinePath;
    private String combinedPath;
    private String capturedPath;
    private boolean first = false;
    private ImageCombineUtil imageCombineUtil = ImageCombineUtil.getInstance();

    public ImageCombineProcessor(Context context, String combinePath, String combinedPath, String capturedPath, boolean first) {
        super();
        this.context = context;
        this.combinePath = combinePath;
        this.combinedPath = combinedPath;
        this.capturedPath = capturedPath;
        this.first = first;
    }

    public boolean isFirst() {
        return first;
    }

    @Override
    protected Void doInBackground(Void... Voids) {
        try {
            if (isFirst()) {
                imageCombineUtil.bitmapFileWrite(
                        combinePath,
                        BitmapFactory.decodeFile(capturedPath)
                );

            } else {

                imageCombineUtil.bitmapCombine(
                        combinedPath,
                        combinePath,
                        BitmapFactory.decodeFile(combinedPath),
                        BitmapFactory.decodeFile(capturedPath)
                );
                imageCombineUtil.fileDelete(combinedPath);

            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        ToastWrapper.showText(context, "캡처 성공하였습니다.");
        super.onPostExecute(aVoid);
    }
}
