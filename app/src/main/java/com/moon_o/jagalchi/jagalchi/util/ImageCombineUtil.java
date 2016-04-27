package com.moon_o.jagalchi.jagalchi.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mucha on 16. 4. 27.
 */
public class ImageCombineUtil {
    private static ImageCombineUtil _instance;

    private final String BASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/TenToOne/";
    public final String MEDIA_EXTERNAL_PATH = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPath();
    private String name;
    private String path;
    private String combinedPath;

    private ImageCombineUtil() {}

    public synchronized static ImageCombineUtil getInstance() {
        if(_instance == null)
            _instance = new ImageCombineUtil();
        return _instance;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String beforePath) {
        this.path = beforePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCombinedPath() {
        return combinedPath;
    }

    public void setCombinedPath(String combinedPath) {
        this.combinedPath = combinedPath;
    }

    public String pathCreat() {
        setName("TenToOne_"+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".jpeg");
        setPath(BASE_PATH + name);
        return path;
    }

    public void fileCreate(File folder, File file) {
        if(folder != null && !folder.exists())
            folder.mkdir();

        if(file != null && !file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean fileDelete(String path) {
        try {
            new File(path).delete();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void bitmapFileWrite(String path, Bitmap bitmap) {
        File backFile = new File(path);
        fileCreate(new File(BASE_PATH), backFile);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(backFile, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bitmap.recycle();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean mediaStoreInsertImage(ContentResolver contentResolver, String imagePath, String imageName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, imageName);
            values.put(MediaStore.Images.Media.DESCRIPTION, "Created by TenToOne");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.ImageColumns.DATA, imagePath);
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            return true;
        }
    }

    public boolean mediaStoreDeleteImage(ContentResolver contentResolver, String path) {
        try {
            String[] retCol = {MediaStore.Images.Media._ID};
            Cursor cur = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    retCol,
                    MediaStore.MediaColumns.DATA + "='" +path+"'",
                    null,
                    null);
            if(cur.getCount() == 0)
                throw new NullPointerException("ImageData is not exist");

            cur.moveToFirst();
            int id = cur.getInt(cur.getColumnIndex(MediaStore.MediaColumns._ID));
            cur.close();

            Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            contentResolver.delete(uri, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            return true;
        }
    }

    public void bitmapCombine(String combinedPath, String combinePath, Bitmap backupBitmap, Bitmap capturedBitmap) {
        setCombinedPath(combinedPath);
        fileCreate(null, new File(combinePath));
        Bitmap backupCopyBitmap, capturedCopyBitmap, combineBitmap;
        backupCopyBitmap = backupBitmap.copy(Bitmap.Config.ARGB_8888, true);
        capturedCopyBitmap = capturedBitmap.copy(Bitmap.Config.ARGB_8888, true);

        int w;
        if(backupCopyBitmap.getWidth() > capturedCopyBitmap.getWidth())
            w = backupCopyBitmap.getWidth();
        else
            w = capturedCopyBitmap.getWidth();

        int h = backupCopyBitmap.getHeight() + capturedCopyBitmap.getHeight();


        combineBitmap = Bitmap.createScaledBitmap(backupCopyBitmap, w, h, true);

        Canvas canvas = new Canvas(combineBitmap);
        canvas.drawBitmap(backupCopyBitmap, 0, 0, null);
        canvas.drawBitmap(capturedCopyBitmap, 0, backupCopyBitmap.getHeight(), null);

        bitmapFileWrite(combinePath, combineBitmap);

        backupCopyBitmap.recycle();
        capturedCopyBitmap.recycle();
        backupBitmap.recycle();
        capturedBitmap.recycle();
        combineBitmap.recycle();
    }


}
