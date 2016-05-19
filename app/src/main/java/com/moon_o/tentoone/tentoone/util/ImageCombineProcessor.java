package com.moon_o.tentoone.tentoone.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.moon_o.tentoone.tentoone.app.CaptureService;

import java.io.File;

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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDensity = DisplayMetrics.DENSITY_HIGH;
        imageCombineUtil.setExecutable(false);

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

            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        imageCombineUtil.setExecutable(true);
        super.onPostExecute(aVoid);
    }
}
