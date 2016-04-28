package com.moon_o.jagalchi.jagalchi.util;

import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Created by mucha on 16. 4. 27.
 */
public class ImageCombineProcessor extends AsyncTask<Void, Integer, Void> {
    private String combinePath;
    private String combinedPath;
    private String capturedPath;
    private boolean first = false;
    private ImageCombineUtil imageCombineUtil = ImageCombineUtil.getInstance();
    private WindowManager wm;

    public ImageCombineProcessor(String combinePath, String combinedPath, String capturedPath, boolean first) {
        super();
        this.combinePath = combinePath;
        this.combinedPath = combinedPath;
        this.capturedPath = capturedPath;
        this.first = first;
        this.wm = wm;
    }

    public boolean isFirst() {
        return first;
    }

    @Override
    protected Void doInBackground(Void... Voids) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDensity = DisplayMetrics.DENSITY_HIGH;
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

}
