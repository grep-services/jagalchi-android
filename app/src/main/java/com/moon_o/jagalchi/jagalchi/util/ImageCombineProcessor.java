package com.moon_o.jagalchi.jagalchi.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;

/**
 * Created by mucha on 16. 4. 27.
 */
public class ImageCombineProcessor extends AsyncTask<Void, Integer, Boolean> {
    private ContentResolver contentResolver;
    private Uri uri;
    private boolean first = false;
    private ImageCombineUtil imageCombineUtil = ImageCombineUtil.getInstance();

    public ImageCombineProcessor(ContentResolver contentResolver, Uri uri, boolean first) {
        super();
        this.contentResolver = contentResolver;
        this.uri = uri;
        this.first = first;
    }

    public boolean isFirst() {
        return first;
    }

    @Override
    protected Boolean doInBackground(Void... Voids) {
        try {
            if (isFirst()) {
                imageCombineUtil.bitmapFileWrite(
                        imageCombineUtil.pathCreat(),
                        BitmapFactory.decodeFile(uri.getPath()));

                imageCombineUtil.mediaStoreInsertImage(
                        contentResolver,
                        imageCombineUtil.getPath(),
                        imageCombineUtil.getName());
            } else {
                String path = imageCombineUtil.getPath();
                String name = imageCombineUtil.getName();

                imageCombineUtil.bitmapCombine(
                        imageCombineUtil.pathCreat(),
                        BitmapFactory.decodeFile(path),
                        BitmapFactory.decodeFile(uri.getPath())
                );

                imageCombineUtil.mediaStoreInsertImage(
                        contentResolver,
                        imageCombineUtil.getPath(),
                        imageCombineUtil.getName());

                imageCombineUtil.mediaStoreDeleteImage(contentResolver, path);
                imageCombineUtil.fileDelete(path);
//                imageCombineUtil.mediaStoreDeleteImage(
//                        contentResolver,
//                        path);

            }
        } catch(Exception e) {
            return false;
        }
        return true;
    }

}
